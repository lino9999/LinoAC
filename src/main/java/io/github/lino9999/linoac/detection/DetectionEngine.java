package io.github.lino9999.linoac.detection;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.detection.combat.CombatRules;
import io.github.lino9999.linoac.detection.movement.MovementRules;
import io.github.lino9999.linoac.detection.player.PlayerRules;
import io.github.lino9999.linoac.detection.world.WorldRules;
import org.bukkit.entity.Player;
import java.util.*;

public final class DetectionEngine {
    private final LinoAC plugin;
    private final List<RuleModule> modules = List.of(new CombatRules(), new MovementRules(), new PlayerRules(), new WorldRules());
    private final Map<UUID, DetectionMemory> memories = new HashMap<>();
    private final Set<String> checkIds;

    public DetectionEngine(LinoAC plugin) {
        this.plugin = plugin;
        Set<String> registered = new HashSet<>();
        for (RuleModule module : modules) for (String id : module.checkIds()) {
            if (!registered.add(id)) throw new IllegalStateException("Duplicate detection ID: " + id);
        }
        Set<String> expected = new HashSet<>();
        for (CheckType type : CheckType.values()) if (type.available()) expected.add(type.id());
        if (!registered.equals(expected)) {
            Set<String> missing = new HashSet<>(expected); missing.removeAll(registered);
            Set<String> extra = new HashSet<>(registered); extra.removeAll(expected);
            throw new IllegalStateException("Detection registry mismatch. Missing=" + missing + " extra=" + extra);
        }
        checkIds = Set.copyOf(registered);
    }

    public Set<String> checkIds() { return checkIds; }
    public void reset(UUID id) { memories.remove(id); }
    public void clear() { memories.clear(); }

    public void accept(Player player, Observation observation) {
        var data = plugin.data(player);
        if (!player.isOnline()) return;
        DetectionMemory memory = memories.computeIfAbsent(data.id, ignored -> new DetectionMemory());
        Map<String, Double> values = new HashMap<>(observation.values());
        for (String group : List.of("combat", "movement", "player", "autoclicker", "total", "scaffold")) {
            values.put("improbable-limit-" + group, plugin.settings().number("limits.improbable-" + group));
        }
        values.put("placement-limit", plugin.settings().number("checks.fastplace-a.max-blocks"));
        values.put("bridge-placement-limit", plugin.settings().number("limits.bridge-placements"));
        values.put("minimum-break-difference", plugin.settings().number("limits.minimum-break-difference"));
        values.put("source-elytra-a-max-speed", plugin.settings().number("limits.elytra-speed"));
        values.put("source-entity-collision-allowance-enabled", plugin.settings().enabled("settings.entity-collision") ? 1d : 0d);
        for (String id : List.of("speed-b", "speed-c", "speed-d", "entityspeed-a")) {
            values.put("source-" + id + "-min-difference", plugin.settings().number("checks." + id + ".min-difference"));
        }
        values.put("cps-limit", plugin.settings().number("checks.autoclicker-a.max-cps"));
        values.put("click-tail-weight-minimum", plugin.settings().number("checks.autoclicker-t.min-kurtosis"));
        values.put("timer-speed-limit", plugin.settings().number("checks.timer-a.max-speed"));
        values.put("timer-balance-limit", plugin.settings().number("checks.timer-d.max-balance"));
        values.put("source-hitbox-b-max-angle", plugin.settings().number("checks.hitbox-b.maximum-angle"));
        values.put("movement-ignore-liquid-motion", plugin.settings().enabled("movement-exemptions.liquid-residuals") ? 1d : 0d);
        values.put("movement-ignore-low-altitude", plugin.settings().enabled("movement-exemptions.low-altitude-translations") ? 1d : 0d);
        for (String id : List.of("flight-a", "flight-c", "flight-d", "flight-e")) {
            values.put("movement-ignore-ghost-blocks-" + id, plugin.settings().enabled("checks." + id + ".ignore-unconfirmed-ground") ? 1d : 0d);
        }
        Observation enriched = new Observation(observation.kind(), observation.tick(), observation.nanos(), values, observation.states());
        boolean corrupt = observation.state("nonfinite-position") || observation.state("nonfinite-rotation") || observation.state("outside-world") || observation.state("invalid-pitch");
        for (RuleModule module : modules) {
            if (observation.state("source-profile-incomplete") && !(module instanceof PlayerRules)) continue;
            if (corrupt && !(module instanceof PlayerRules)) continue;
            Observation input;
            if (module instanceof WorldRules) {
                var totals = data.historyTotals.snapshot();
                var worldValues = new HashMap<>(enriched.values());
                worldValues.put("violations-combat", (double) totals.combat()); worldValues.put("violations-movement", (double) totals.movement());
                worldValues.put("violations-player", (double) totals.player()); worldValues.put("violations-autoclicker", (double) totals.autoClicker());
                worldValues.put("violations-total", (double) totals.total()); worldValues.put("violations-scaffold", (double) totals.scaffold());
                input = new Observation(enriched.kind(), enriched.tick(), enriched.nanos(), worldValues, enriched.states());
            } else input = enriched;
            module.evaluate(input, memory, new RuleModule.FindingSink() {
                public void sample(String id, boolean suspicious, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) plugin.violations().sample(player, data, type, suspicious, detail);
                }
                public void direct(String id, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) plugin.violations().direct(player, data, type, detail);
                }
                public void reset(String id, String detail) {
                    CheckType type = CheckType.find(id).orElseThrow();
                    data.buffers.remove(type);
                }
                public void accumulate(String id, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) data.buffers.compute(type, (key, current) -> FindingBuffer.accumulated(current == null ? 0 : current));
                }
                public void sampleAbove(String id, boolean suspicious, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) plugin.violations().sampleAbove(player, data, type, suspicious, detail);
                }
                public void decay(String id, double amount, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) plugin.violations().reward(data, type, amount);
                }
                public void sampleAboveThenDecay(String id, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) plugin.violations().sampleAboveThenDecay(player, data, type, detail);
                }
                public void sampleAboveOrDirect(String id, boolean immediate, String detail) {
                    if (CheckExemptions.exempt(id, enriched)) return;
                    CheckType type = CheckType.find(id).orElseThrow();
                    if (type.available()) plugin.violations().sampleAboveOrDirect(player, data, type, immediate, detail);
                }
            });
        }
        if (!corrupt && observation.kind() == Observation.Kind.MOVE && observation.state("ground") && observation.state("client-ground")
                && !observation.state("movement-grace") && observation.hasAll("x", "y", "z")
                && data.buffers.entrySet().stream().noneMatch(entry -> entry.getKey().category().equals("movement") && entry.getValue() > 0)
                && data.lastFlags.entrySet().stream().noneMatch(entry -> entry.getKey().category().equals("movement") && entry.getValue() == plugin.tick())) {
            double x = observation.value("x"), y = observation.value("y"), z = observation.value("z");
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                    && player.getWorld().isChunkLoaded(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4)) {
                data.safe = new org.bukkit.Location(player.getWorld(), x, y, z,
                        (float) observation.value("yaw", player.getLocation().getYaw()), (float) observation.value("pitch", player.getLocation().getPitch()));
            }
        }
    }
}
