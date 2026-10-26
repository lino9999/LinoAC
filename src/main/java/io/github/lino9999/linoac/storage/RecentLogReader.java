package io.github.lino9999.linoac.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecentLogReader {
    private RecentLogReader() {}

    public static List<String> read(Path directory, String channel, String name, int limit) throws IOException {
        if (!channel.equals("violations") && !channel.equals("punishments")) throw new IllegalArgumentException("Unknown audit channel.");
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid recent log limit.");
        List<String> latest = new ArrayList<>(limit);
        try (var paths = Files.list(directory)) {
            List<Path> files = paths.filter(path -> path.getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}-" + channel + "\\.log"))
                    .sorted(Comparator.comparing(Path::toString).reversed()).toList();
            for (Path file : files) {
                int remaining = limit - latest.size();
                ArrayDeque<String> tail = new ArrayDeque<>();
                try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
                    lines.forEach(line -> {
                        String[] fields = line.split("\t", 4);
                        if (fields.length != 4 || !fields[2].equalsIgnoreCase(name)) return;
                        tail.addLast(line);
                        if (tail.size() > remaining) tail.removeFirst();
                    });
                }
                while (!tail.isEmpty()) latest.add(tail.removeLast());
                if (latest.size() == limit) break;
            }
        } catch (java.io.UncheckedIOException failure) { throw failure.getCause(); }
        return List.copyOf(latest);
    }
}
