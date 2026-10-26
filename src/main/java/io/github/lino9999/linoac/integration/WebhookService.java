package io.github.lino9999.linoac.integration;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.api.LinoWebhookPunishmentEvent;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WebhookService implements AutoCloseable {
    private final LinoAC plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(32), action -> {
                Thread thread = new Thread(action, "LinoAC-webhook");
                thread.setDaemon(true);
                return thread;
            });
    private final Map<String, Long> nextSend = new HashMap<>();
    private boolean closed;

    public WebhookService(LinoAC plugin) { this.plugin = plugin; }

    public void send(String kind, String message) {
        submit(kind, message, Map.of(), false);
    }

    public void send(String kind, String message, Map<String, String> context) { submit(kind, message, Map.copyOf(context), false); }

    public boolean sendTest(String kind, String message) { return submit(kind, message, Map.of(), true); }

    private boolean submit(String kind, String message, Map<String, String> context, boolean manualTest) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Webhook notifications must be submitted on the server main thread.");
        if (!kind.equals("alerts") && !kind.equals("punishments")) throw new IllegalArgumentException("Unknown webhook notification kind.");
        if (closed || !plugin.settings().enabled("webhook.enabled") || !plugin.settings().enabled("webhook." + kind)) return false;
        URI uri;
        try {
            String endpoint = plugin.settings().text(kind.equals("alerts") ? "webhook.alert-url" : "webhook.punishment-url");
            if (endpoint.isBlank()) endpoint = plugin.settings().text("webhook.url");
            uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) return false;
        } catch (IllegalArgumentException e) { return false; }
        if (kind.equals("punishments")) {
            LinoWebhookPunishmentEvent event = new LinoWebhookPunishmentEvent(message);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
        }
        long now = System.nanoTime();
        if (!manualTest && now < nextSend.getOrDefault(kind, 0L)) return false;
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload(kind, message, context)))
                .build();
        try {
            executor.execute(() -> {
                try {
                    int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                    if (status < 200 || status > 299) plugin.getLogger().fine("Webhook returned HTTP " + status);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (Exception e) { plugin.getLogger().fine("Webhook delivery failed."); }
            });
            if (!manualTest) nextSend.put(kind, now + TimeUnit.SECONDS.toNanos(2));
            return true;
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            plugin.getLogger().fine("Webhook queue is full.");
            return false;
        }
    }

    private String payload(String kind, String message, Map<String, String> context) {
        String prefix = kind.equals("alerts") ? "webhook.alert-" : "webhook.punishment-";
        Map<String, String> values = new LinkedHashMap<>(context);
        values.put("message", message);
        values.putIfAbsent("server-name", plugin.settings().text("settings.server-name"));
        String username = plugin.settings().text(prefix + "username");
        if (username.isBlank()) username = plugin.settings().text("webhook.username");
        String avatar = plugin.settings().text(prefix + "avatar-url");
        if (avatar.isBlank()) avatar = plugin.settings().text("webhook.avatar-url");
        boolean embed = plugin.settings().enabled("webhook.embed");
        String configuredContent = plugin.settings().text(prefix + "content");
        String content = embed ? plugin.actions().render(configuredContent, values) : message;
        StringBuilder body = new StringBuilder("{\"content\":").append(json(limit(content, 1900))).append(",\"allowed_mentions\":{\"parse\":[]}");
        if (!username.isBlank()) body.append(",\"username\":").append(json(limit(plugin.actions().render(username, values), 80)));
        if (!avatar.isBlank()) body.append(",\"avatar_url\":").append(json(plugin.actions().render(avatar, values)));
        if (embed) {
            String title = plugin.settings().text(prefix + "title");
            if (title.isBlank()) title = kind.equals("alerts") ? "LinoAC Alert" : "LinoAC Punishment";
            String description = plugin.settings().text(prefix + "description");
            if (description.isBlank()) description = "%message%";
            int color = kind.equals("alerts") ? 2592638 : 15158332;
            String configuredColor = plugin.settings().text(prefix + "color");
            if (!configuredColor.isBlank()) {
                try { color = configuredColor.startsWith("#") ? Integer.parseInt(configuredColor.substring(1), 16) : Integer.parseInt(configuredColor); }
                catch (NumberFormatException ignored) { color = kind.equals("alerts") ? 2592638 : 15158332; }
            }
            color = Math.max(0, Math.min(16777215, color));
            body.append(",\"embeds\":[{\"title\":").append(json(limit(plugin.actions().render(title, values), 256)))
                    .append(",\"description\":").append(json(limit(plugin.actions().render(description, values), 1900)))
                    .append(",\"color\":").append(color).append(",\"timestamp\":").append(json(Instant.now().toString()));
            String thumbnail = plugin.settings().text(prefix + "thumbnail-url");
            if (!thumbnail.isBlank()) body.append(",\"thumbnail\":{\"url\":").append(json(plugin.actions().render(thumbnail, values))).append('}');
            String footer = plugin.settings().text("webhook.footer");
            if (!footer.isBlank()) body.append(",\"footer\":{\"text\":").append(json(limit(plugin.actions().render(footer, values), 2048))).append('}');
            if (plugin.settings().enabled("webhook.include-context")) {
                List<String> fields = new ArrayList<>();
                field(fields, "Player", context.get("player"));
                field(fields, "Check", context.get("check"));
                field(fields, "Client", context.get("client-version"));
                field(fields, "Reported brand", context.get("client-brand"));
                if (context.containsKey("ping") && context.containsKey("tps")) field(fields, "Ping / TPS", context.get("ping") + "ms / " + context.get("tps"));
                if (context.keySet().containsAll(List.of("world", "x", "y", "z"))) field(fields, "Recorded location", context.get("world") + " " + context.get("x") + ", " + context.get("y") + ", " + context.get("z"));
                if (!fields.isEmpty()) body.append(",\"fields\":[").append(String.join(",", fields)).append(']');
            }
            body.append("}]");
        }
        return body.append('}').toString();
    }

    private void field(List<String> fields, String name, String value) {
        if (value != null && !value.isBlank() && !value.equalsIgnoreCase("unknown")) {
            fields.add("{\"name\":" + json(name) + ",\"value\":" + json(limit(value, 1024)) + ",\"inline\":true}");
        }
    }

    private String limit(String value, int maximum) { return value.length() <= maximum ? value : value.substring(0, maximum); }

    private String json(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c == '\\' || c == '\"') result.append('\\').append(c);
            else if (c < 32) result.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
        client.close();
    }
}
