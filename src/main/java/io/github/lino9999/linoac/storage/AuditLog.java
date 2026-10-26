package io.github.lino9999.linoac.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
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
                if (Files.exists(path) && Files.size(path) + line.getBytes(StandardCharsets.UTF_8).length > maximumBytes) {
                    warning("Daily " + kind + " log reached its configured size limit; further entries will be dropped today.");
                    return;
                }
                Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                warning("Unable to write audit log: " + e.getMessage());
            }
        });
    }

    public void read(String kind, String name, int page, int limit, Consumer<List<String>> callback) {
        submit(() -> {
            ArrayDeque<String> lines = new ArrayDeque<>(limit);
            try (Stream<Path> paths = Files.list(directory)) {
                List<Path> files = paths.filter(p -> p.getFileName().toString().endsWith("-" + kind + ".log"))
                        .sorted(Comparator.comparing(Path::toString).reversed()).toList();
                int maximum = Math.min(limit, page * 8);
                for (Path file : files) {
                    ArrayDeque<String> perFile = new ArrayDeque<>();
                    try (Stream<String> source = Files.lines(file, StandardCharsets.UTF_8)) {
                        source.filter(line -> {
                            String[] parts = line.split("\t", 4);
                            return parts.length == 4 && parts[2].equalsIgnoreCase(name);
                        }).forEach(line -> {
                            perFile.addLast(line);
                            if (perFile.size() > maximum) perFile.removeFirst();
                        });
                    }
                    while (!perFile.isEmpty() && lines.size() < maximum) lines.addLast(perFile.removeLast());
                    if (lines.size() >= maximum) break;
                }
                List<String> all = new ArrayList<>(lines);
                int start = Math.min(all.size(), (page - 1) * 8);
                List<String> result = List.copyOf(all.subList(start, Math.min(all.size(), start + 8)));
                callback.accept(result);
            } catch (IOException e) {
                warning("Unable to read audit log: " + e.getMessage());
                callback.accept(List.of());
            }
        });
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

    private void submit(Runnable action) {
        try { worker.execute(action); }
        catch (java.util.concurrent.RejectedExecutionException e) { warning("Audit queue is full or shutting down."); }
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
