package io.github.lino9999.linoac.integration;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProxyPacketCodec {
    private ProxyPacketCodec() {}

    public static byte[] encode(String operation, String payload, int maximumBytes) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(payload, "payload");
        int operationBytes = modifiedUtfLength(operation, maximumBytes);
        int payloadBytes = modifiedUtfLength(payload, maximumBytes);
        if (maximumBytes < 4 || operationBytes > 65535 || payloadBytes > 65535
                || (long) operationBytes + payloadBytes + 4 > maximumBytes) {
            throw new IllegalArgumentException("Proxy message exceeds the plugin message limit.");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(operationBytes + payloadBytes + 4);
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeUTF(operation);
            output.writeUTF(payload);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException failure) {
            throw new IllegalArgumentException("Unable to encode proxy message.", failure);
        }
    }

    private static int modifiedUtfLength(String text, int maximumBytes) {
        int size = 0;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            size += value >= 1 && value <= 127 ? 1 : value > 2047 ? 3 : 2;
            if (size > maximumBytes || size > 65535) return size;
        }
        return size;
    }

    public static Optional<String> remoteCommand(String template) {
        Objects.requireNonNull(template, "template");
        String command = template.stripLeading();
        if (!command.regionMatches(true, 0, "Bungee:", 0, 7)) return Optional.empty();
        command = command.substring(7);
        if (command.indexOf('\r') >= 0 || command.indexOf('\n') >= 0 || command.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid proxy command.");
        }
        command = command.trim();
        if (command.isEmpty()) throw new IllegalArgumentException("Invalid proxy command.");
        return Optional.of(command);
    }

    public static String alertPayload(AlertFields fields, String separator) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(separator, "separator");
        if (separator.isEmpty()) throw new IllegalArgumentException("Proxy alert separator cannot be empty.");
        return String.join(separator, List.of(fields.checkName(), fields.checkType(), fields.violations(), fields.playerName(),
                fields.maximumViolations(), fields.clientVersion(), fields.tps(), fields.ping(), fields.description(),
                fields.detail(), fields.experimentalMarker(), fields.severityColor()));
    }

    public static String punishmentPayload(String command, PunishmentFields fields, String separator) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(separator, "separator");
        if (separator.isEmpty()) throw new IllegalArgumentException("Proxy punishment separator cannot be empty.");
        return String.join(separator, List.of(command, fields.checkName(), fields.checkType(), fields.violations(),
                fields.playerName(), fields.maximumViolations()));
    }

    public record AlertFields(String checkName, String checkType, String violations, String playerName,
                              String maximumViolations, String clientVersion, String tps, String ping,
                              String description, String detail, String experimentalMarker, String severityColor) {}

    public record PunishmentFields(String checkName, String checkType, String violations, String playerName,
                                   String maximumViolations) {}
}
