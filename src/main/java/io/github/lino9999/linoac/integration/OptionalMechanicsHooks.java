package io.github.lino9999.linoac.integration;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

public final class OptionalMechanicsHooks implements Listener {
    private final Plugin owner;
    private final BooleanSupplier enabled;
    private final Set<UUID> berserkPlayers = new HashSet<>();
    private Plugin mcmmo;
    private boolean connected;

    public OptionalMechanicsHooks(Plugin owner, BooleanSupplier enabled) {
        this.owner = owner;
        this.enabled = enabled;
    }

    public boolean register() {
        if (connected) return true;
        if (!enabled.getAsBoolean()) return false;
        Plugin optional = Bukkit.getPluginManager().getPlugin("mcMMO");
        if (optional == null || !optional.isEnabled()) return false;
        try {
            ClassLoader loader = optional.getClass().getClassLoader();
            AbilityEvent activation = event(loader, "com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent");
            AbilityEvent deactivation = event(loader, "com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent");
            connect(activation, true);
            connect(deactivation, false);
            Bukkit.getPluginManager().registerEvents(this, owner);
            mcmmo = optional;
            connected = true;
            return true;
        } catch (ReflectiveOperationException | LinkageError failure) {
            owner.getLogger().log(Level.FINE, "Unable to connect to the mcMMO ability event API.", failure);
            return false;
        }
    }

    public boolean berserk(UUID player) {
        return connected && enabled.getAsBoolean() && mcmmo != null && mcmmo.isEnabled() && berserkPlayers.contains(player);
    }

    public boolean active(UUID player) { return berserk(player); }

    @EventHandler
    public void quit(PlayerQuitEvent event) { berserkPlayers.remove(event.getPlayer().getUniqueId()); }

    @EventHandler
    public void disabled(PluginDisableEvent event) {
        if (event.getPlugin() == mcmmo || event.getPlugin() == owner) {
            berserkPlayers.clear();
            connected = false;
        }
    }

    private AbilityEvent event(ClassLoader loader, String name) throws ReflectiveOperationException {
        Class<?> type = Class.forName(name, false, loader);
        if (!Event.class.isAssignableFrom(type)) throw new ReflectiveOperationException("Ability type is not a Bukkit event");
        Method player = type.getMethod("getPlayer");
        Method ability = type.getMethod("getAbility");
        if (!Player.class.isAssignableFrom(player.getReturnType()) || !ability.getReturnType().isEnum()) throw new ReflectiveOperationException("Unsupported ability event accessors");
        return new AbilityEvent(type.asSubclass(Event.class), player, ability);
    }

    private void connect(AbilityEvent access, boolean active) {
        Bukkit.getPluginManager().registerEvent(access.type(), this, EventPriority.MONITOR, (listener, event) -> {
            try {
                Object ability = access.ability().invoke(event);
                if (!(ability instanceof Enum<?> value) || !value.name().equals("BERSERK")) return;
                Object subject = access.player().invoke(event);
                if (!(subject instanceof Player player)) return;
                Runnable update = () -> {
                    if (!connected || !enabled.getAsBoolean() || mcmmo == null || !mcmmo.isEnabled()) {
                        berserkPlayers.clear();
                        return;
                    }
                    if (active) berserkPlayers.add(player.getUniqueId());
                    else berserkPlayers.remove(player.getUniqueId());
                };
                if (Bukkit.isPrimaryThread()) update.run();
                else Bukkit.getScheduler().runTask(owner, update);
            } catch (ReflectiveOperationException failure) {
                owner.getLogger().log(Level.FINE, "Unable to read the mcMMO ability event.", failure);
            }
        }, owner, true);
    }

    private record AbilityEvent(Class<? extends Event> type, Method player, Method ability) { }
}
