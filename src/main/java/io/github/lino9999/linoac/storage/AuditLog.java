package io.github.lino9999.linoac.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class AuditLog implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Path directory;
    private final ThreadPoolExecutor worker;
    private volatile int retention;
    private long lastWarning;
    private LocalDate lastDate;
    private volatile long maximumBytes = 32L * 1024 * 1024;
    private volatile boolean violationsEnabled = true, punishmentsEnabled = true;

    public AuditLog(JavaPlugin plugin, int retention) throws IOException {
        this.plugin = plugin;
        this.retention = retention;
        directory = plugin.getDataFolder().toPath().resolve("logs");
        Files.createDirectories(directory);
        worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4096), r -> {
            Thread thread = new Thread(r, "LinoAC-audit");
            thread.setDaemon(true);
            return thread;
        });
        submit(this::prune);
    }

    public void retention(int days) { retention = days; submit(this::prune); }

    public void configure(boolean violations, boolean punishments, long bytes) {
        violationsEnabled = violations;
        punishmentsEnabled = punishments;
        maximumBytes = bytes;
    }

    public void write(String kind, UUID id, String name, String text) {
        if (!kind.equals("violations") && !kind.equals("punishments")) throw new IllegalArgumentException("Unknown audit channel.");
        if (kind.equals("violations") ? !violationsEnabled : !punishmentsEnabled) return;
        String line = Instant.now() + "\t" + id + "\t" + clean(name) + "\t" + clean(text) + System.lineSeparator();
        LocalDate date = LocalDate.now();
        submit(() -> {
            if (!date.equals(lastDate)) {
                lastDate = date;
                prune();
            }
            Path path = directory.resolve(date + "-" + kind + ".log");
            try {
                byte[] encoded = line.getBytes(StandardCharsets.UTF_8);
                long existing = Files.exists(path) ? Files.size(path) : 0;
                long capacity = maximumBytes;
                if (existing > capacity || encoded.length > capacity - existing) {
                    warning("Daily " + kind + " log reached its configured size limit; further entries will be dropped today.");
                    return;
                }
                Files.write(path, encoded, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                warning("Unable to write audit log: " + e.getMessage());
            }
        });
    }

    public void read(String kind, String name, int page, int limit, Consumer<List<String>> callback) {
        readPage(kind, name, page, limit, result -> callback.accept(result.lines()));
    }

    public void readPage(String kind, String name, int page, int limit, Consumer<LogPage> callback) {
        if (page < 1) throw new IllegalArgumentException("Log page must be positive.");
        if (!kind.equals("violations") && !kind.equals("punishments")) throw new IllegalArgumentException("Unknown audit channel.");
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid recent log limit.");
        java.util.Objects.requireNonNull(callback);
        boolean accepted = submit(() -> {
            try {
                callback.accept(LogPage.of(RecentLogReader.read(directory, kind, name, limit), page));
            } catch (IOException e) {
                warning("Unable to read audit log: " + e.getMessage());
                callback.accept(LogPage.unavailable(page));
            }
        });
        if (!accepted) callback.accept(LogPage.unavailable(page));
    }

    private void prune() {
        LocalDate cutoff = LocalDate.now().minusDays(retention);
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(p -> p.getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}-(violations|punishments)\\.log"))
                    .forEach(path -> {
                        if (LocalDate.parse(path.getFileName().toString().substring(0, 10)).isBefore(cutoff)) {
                            try { Files.deleteIfExists(path); } catch (IOException e) { warning("Unable to remove expired log: " + path); }
                        }
                    });
        } catch (IOException e) { warning("Unable to prune audit logs: " + e.getMessage()); }
    }

    private boolean submit(Runnable action) {
        try { worker.execute(action); return true; }
        catch (java.util.concurrent.RejectedExecutionException e) { warning("Audit queue is full or shutting down."); return false; }
    }

    private synchronized void warning(String text) {
        long now = System.nanoTime();
        if (now - lastWarning > TimeUnit.SECONDS.toNanos(30)) {
            lastWarning = now;
            plugin.getLogger().warning(text);
        }
    }

    private String clean(String value) { return value.replaceAll("[\\r\\n\\t\\p{Cntrl}]", " "); }

    public void close() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Audit shutdown timeout; pending records may be lost.");
                worker.shutdownNow();
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); worker.shutdownNow(); }
    }
}
