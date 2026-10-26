package io.github.lino9999.linoac.integration;

import java.lang.reflect.Method;
import java.util.UUID;

public final class BedrockSupport {
    private Object api;
    private Method isFloodgatePlayer;

    public BedrockSupport() {
        try {
            Class<?> type = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            api = type.getMethod("getInstance").invoke(null);
            isFloodgatePlayer = type.getMethod("isFloodgatePlayer", UUID.class);
        } catch (ReflectiveOperationException ignored) { api = null; }
    }

    public boolean isBedrock(UUID id) {
        if (id.getMostSignificantBits() == 0) return true;
        if (api == null) return false;
        try { return (boolean) isFloodgatePlayer.invoke(api, id); }
        catch (ReflectiveOperationException e) { return false; }
    }
}
