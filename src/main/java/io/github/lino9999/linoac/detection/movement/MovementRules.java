package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import io.github.lino9999.linoac.detection.Statistics;

import java.util.Set;

public final class MovementRules implements RuleModule {
    private static final Set<String> IDS = Set.of(
            "antilevitation-a", "boatfly-a", "boatfly-b", "boatfly-c",
            "elytra-a", "elytra-b", "elytra-c", "elytra-f", "elytra-g", "elytra-i", "elytra-k", "elytra-l", "elytra-m", "elytra-n",
            "entityflight-a", "entityflight-b", "entityspeed-a", "fastclimb-a",
            "flight-a", "flight-b", "flight-c", "flight-d", "flight-e", "flight-f",
            "jesus-a", "jesus-b", "jesus-c", "jesus-d", "jesus-e", "jump-a", "jump-b", "jump-c",
            "motion-a", "motion-b", "motion-c", "motion-e", "nosaddle-a", "noslow-a", "noslow-b", "noslow-c",
            "speed-a", "speed-b", "speed-c", "speed-d", "speed-e", "sprint-a", "step-a", "step-c", "strafe-a", "vclip-a", "wallclimb-a");

    public Set<String> checkIds() { return IDS; }

    public void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        if (o.state("teleport-grace") || o.state("creative")) return;
        if (o.kind() == Observation.Kind.RELEASE && o.state("dig:RELEASE_USE_ITEM") && o.has("placements-since-move")) {
            sink.sample("noslow-a", o.state("legacy-server") && o.state("legacy-client") && o.value("placements-since-move") > 0,
                    "release-after-placement-before-movement");
        }
        if (o.kind() == Observation.Kind.PACKET && o.has("elytra-toggle-interval")) {
            double interval = o.value("elytra-toggle-interval");
            sink.sample("elytra-f", interval >= 0 && interval < 110, "toggle-interval-ms=" + interval);
        }
        if (o.kind() == Observation.Kind.TICK && o.state("vehicle") && !o.state("movement-grace") && !o.state("allow-flight")) {
            vehicles(o, memory, sink);
            return;
        }
        if (o.kind() != Observation.Kind.MOVE || !o.hasAll("dy", "previous-dy", "horizontal", "previous-horizontal", "air-ticks")) return;
        if (o.state("movement-grace") || o.state("allow-flight")) return;
        if (o.state("vehicle")) {
            return;
        }
        if (o.state("gliding")) {
            gliding(o, memory, sink);
            return;
        }
        slowSurfaces(o, sink);
        if (o.state("liquid") || o.state("liquid-below")) water(o, memory, sink);
        if (o.value("levitation-effect") > 0) {
            levitation(o, memory, sink);
            return;
        }
        memory.set("movement.levitation-start", o.tick());
        memory.set("movement.levitation-level", 0);
        if (o.state("climbing")) {
            sink.sample("fastclimb-a", o.value("dy") > .25, "vertical-speed=" + o.value("dy"));
            return;
        }
        if (o.state("liquid") || o.state("environment-special") || o.state("slow-falling")) return;
        ordinary(o, memory, sink);
    }

    private void ordinary(Observation o, DetectionMemory memory, FindingSink sink) {
        double dy = o.value("dy"), previousDy = o.value("previous-dy");
        double horizontal = o.value("horizontal"), previousHorizontal = o.value("previous-horizontal");
        double air = o.value("air-ticks"), groundTicks = o.value("ground-ticks");
        boolean ground = o.state("ground"), previousGround = o.state("previous-ground");
        double scale = Math.max(.1, o.value("movement-speed", .1) / .1);
        double jump = Math.max(0, o.value("jump-strength", .42)) + o.value("jump-effect") * .1;
        double gravity = Math.max(.001, o.value("gravity", .08));
        double expectedDy = (previousDy - gravity) * .98;
        double error = dy - expectedDy;
        boolean airPhysics = !ground && !previousGround && air > 3 && !o.state("head-blocked");
        double groundLimit = .2975 * scale + .045;
        double airLimit = Math.max(.3625 * scale, previousHorizontal * .91 + .045 * scale) + .025;
        double friction = previousGround ? .546 : .91;
        double acceleration = Math.max(0, horizontal - previousHorizontal * friction);

        sink.sample("speed-a", ground && previousGround && horizontal > .25 && acceleration > .14 * scale + .035,
                "friction-residual=" + acceleration);
        if (!o.state("entity-cram")) {
            sink.sample("speed-b", ground && previousGround && groundTicks > 3 && horizontal > groundLimit,
                    "ground-speed=" + horizontal + " limit=" + groundLimit);
            sink.sample("speed-c", !ground && !previousGround && air > 2 && horizontal > airLimit,
                    "air-speed=" + horizontal + " limit=" + airLimit);
            sink.sample("speed-d", ground && !previousGround && horizontal > groundLimit + .175,
                    "landing-speed=" + horizontal);
        }
        if (o.hasAll("dx", "dz", "previous-dx", "previous-dz")) {
            double vectorInput = Math.hypot(o.value("dx") - o.value("previous-dx") * friction,
                    o.value("dz") - o.value("previous-dz") * friction);
            sink.sample("speed-e", ground && previousGround && vectorInput > .15 * scale + .045,
                    "ground-input=" + vectorInput);
            sink.sample("strafe-a", airPhysics && vectorInput > .026 * scale + .025,
                    "air-input=" + vectorInput);
        }

        sink.sample("flight-a", airPhysics && Math.abs(dy) <= 1.25 && error > .055,
                "vertical-prediction-residual=" + error);
        if (airPhysics && !o.state("client-ground")) {
            double accumulated = Math.max(0, memory.get("movement.client-prediction-drift") + error - .005);
            memory.set("movement.client-prediction-drift", accumulated);
            sink.sample("flight-b", air > 8 && accumulated > .24, "client-trajectory-drift=" + accumulated);
            if (accumulated > .3) memory.set("movement.client-prediction-drift", .15);
        } else memory.set("movement.client-prediction-drift", 0);
        sink.sample("flight-c", airPhysics && air > 12 && dy > .08 && dy >= previousDy - .005,
                "unsupported-ascension=" + dy);
        boolean shallowFall = airPhysics && air > 20 && dy < -.02 && dy > -.5 && dy - previousDy > -.005;
        sink.sample("flight-d", repeated(o, memory, "shallow-fall", shallowFall) >= 4, "fall-acceleration=" + (dy - previousDy));
        sink.sample("flight-e", airPhysics && air > 20 && Math.abs(dy) < .005 && Math.abs(dy - previousDy) < .005,
                "hover-air-ticks=" + air);
        double closest = Math.min(Math.abs(error), Math.abs(dy));
        if (o.has("vertical-velocity") && o.value("velocity-age-ticks", 100) < 4) closest = Math.min(closest, Math.abs(dy - o.value("vertical-velocity")));
        if (previousGround) closest = Math.min(closest, Math.abs(dy - jump));
        sink.sample("flight-f", airPhysics && Math.abs(dy) <= 1.25 && dy > expectedDy && closest > .085,
                "closest-vertical-model-error=" + closest);

        boolean takeoff = previousGround && !ground && dy > .1 && !o.state("head-blocked");
        sink.sample("jump-a", takeoff && Math.abs(dy - jump) > .075, "takeoff-motion=" + dy + " expected=" + jump);
        if (ground) {
            memory.set("movement.jump-height", 0);
            memory.set("movement.jump-active", 0);
        } else {
            if (takeoff) {
                memory.set("movement.jump-active", 1);
                memory.set("movement.jump-height", 0);
            }
            if (memory.get("movement.jump-active") == 1) {
                double height = memory.increment("movement.jump-height", dy);
                sink.sample("jump-b", height > maximumJumpHeight(jump, gravity) + .2,
                        "jump-height=" + height);
            }
        }
        sink.sample("jump-c", !ground && air == 2 && !o.state("head-blocked") && previousDy > .1
                        && Math.abs(dy - (jump - gravity) * .98) > .075,
                "second-jump-motion=" + dy);
        sink.sample("step-a", ground && previousGround && dy > Math.max(.6, o.value("step-height", .6)) + .05,
                "grounded-rise=" + dy);
        sink.sample("step-c", previousGround && ground && dy < -1 && previousDy >= -.08,
                "instant-grounded-descent=" + dy);
        sink.sample("vclip-a", ground && previousGround && Math.abs(dy) > 2.5,
                "grounded-vertical-displacement=" + dy);

        boolean repeatedMotion = airPhysics && Math.abs(dy) > .01 && Math.abs(dy - previousDy) < 1e-10 && Math.abs(dy - .11760000228882461) > .001;
        sink.sample("motion-a", repeated(o, memory, "constant-motion", repeatedMotion) >= 4, "constant-y=" + dy);
        boolean inverseMotion = airPhysics && Math.abs(dy) > .01 && Math.abs(dy + previousDy) < 1e-10;
        sink.sample("motion-b", repeated(o, memory, "inverse-motion", inverseMotion) >= 3, "inverse-y=" + dy);
        double jumpDifference = Math.abs(dy - .41999998688697815);
        sink.sample("motion-c", repeated(o, memory, "rounded-jump", airPhysics && jumpDifference > 0 && jumpDifference < 1e-5) >= 4,
                "rounded-jump-difference=" + jumpDifference);
        sink.sample("motion-e", repeated(o, memory, "decimal-motion", airPhysics && Math.abs(dy) > .11 && quantized(dy, .1)) >= 4,
                "decimal-y=" + dy);
        boolean wallRise = airPhysics && o.state("horizontal-collision") && dy > .1 && dy <= .73 && Math.abs(dy - previousDy) < .005;
        sink.sample("wallclimb-a", repeated(o, memory, "wall-rise", wallRise) >= 3, "wall-vertical-motion=" + dy);
        if (o.hasAll("yaw", "dx", "dz")) {
            double direction = Math.toDegrees(Math.atan2(-o.value("dx"), o.value("dz")));
            double angle = Math.abs(Statistics.wrappedDegrees(direction - o.value("yaw")));
            sink.sample("sprint-a", o.state("sprinting") && horizontal > .25 && angle > 100,
                    "sprint-movement-angle=" + angle);
        }
    }

    private void water(Observation o, DetectionMemory memory, FindingSink sink) {
        if (o.state("swimming") || o.state("dolphins-grace") || o.state("bubble-column")) return;
        double dy = o.value("dy"), lastDy = o.value("previous-dy"), horizontal = o.value("horizontal");
        boolean surface = o.state("liquid-below") && !o.state("liquid") && !o.state("ground");
        sink.sample("jesus-a", repeated(o, memory, "liquid-surface", surface && horizontal > .1 && Math.abs(dy) < .005) >= 4,
                "unsupported-liquid-surface=" + horizontal);
        sink.sample("jesus-b", o.state("liquid") && dy > .16 && o.value("jump-effect") == 0,
                "liquid-upward-motion=" + dy);
        sink.sample("jesus-c", repeated(o, memory, "liquid-fixed-height", o.state("liquid") && Math.abs(dy) < .001 && horizontal > .1) >= 8,
                "fixed-liquid-height=" + dy);
        sink.sample("jesus-d", surface && dy > .3 && lastDy <= 0 && !o.state("previous-ground"),
                "unsupported-liquid-jump=" + dy);
        double scale = Math.max(1, o.value("movement-speed", .1) / .1);
        sink.sample("jesus-e", surface && horizontal > .3 * scale + .05 && o.value("air-ticks") > 5,
                "liquid-surface-speed=" + horizontal);
    }

    private void slowSurfaces(Observation o, FindingSink sink) {
        double horizontal = o.value("horizontal");
        double scale = Math.max(1, o.value("movement-speed", .1) / .1);
        sink.sample("noslow-b", o.state("soul-sand") && o.state("ground") && o.value("soul-speed-level") == 0 && horizontal > .2 * scale,
                "soul-sand-speed=" + horizontal);
        sink.sample("noslow-c", o.state("web") && horizontal > .09 * scale,
                "web-speed=" + horizontal);
    }

    private void levitation(Observation o, DetectionMemory memory, FindingSink sink) {
        double level = o.value("levitation-effect");
        if (memory.get("movement.levitation-level") != level) {
            memory.set("movement.levitation-level", level);
            memory.set("movement.levitation-start", o.tick());
        }
        boolean mature = o.tick() - memory.get("movement.levitation-start") > 25;
        sink.sample("antilevitation-a", mature && !o.state("head-blocked") && !o.state("ground") && o.value("dy") <= 0,
                "levitation-level=" + level + " vertical-motion=" + o.value("dy"));
    }

    private void vehicles(Observation o, DetectionMemory memory, FindingSink sink) {
        if (!o.hasAll("vehicle-dy", "vehicle-horizontal", "vehicle-air-ticks") || o.state("vehicle-special")) return;
        double dy = o.value("vehicle-dy"), horizontal = o.value("vehicle-horizontal"), air = o.value("vehicle-air-ticks");
        boolean unsupported = !o.state("vehicle-ground") && !o.state("vehicle-liquid");
        if (o.state("boat")) {
            sink.sample("boatfly-a", unsupported && air > 8 && dy > .01, "boat-rise=" + dy);
            sink.sample("boatfly-b", unsupported && air > 5 && horizontal > .45 && dy > -.05,
                    "airborne-boat-horizontal=" + horizontal);
            if (o.has("previous-vehicle-dy")) {
                boolean hovering = unsupported && air > 12 && dy > -.05 && Math.abs(dy - o.value("previous-vehicle-dy")) < .025;
                sink.sample("boatfly-c", repeated(o, memory, "boat-hover", hovering) >= 4, "boat-motion=" + dy);
            }
            return;
        }
        double jumpTicks = Math.ceil(Math.max(.5, o.value("vehicle-jump-strength", 1)) / .08) + 6;
        sink.sample("entityflight-a", unsupported && air > jumpTicks && dy > .01, "mounted-rise=" + dy);
        if (o.has("previous-vehicle-dy")) {
            boolean hovering = unsupported && air > 20 && Math.abs(dy) < .4 && Math.abs(dy - o.value("previous-vehicle-dy")) < .005;
            sink.sample("entityflight-b", repeated(o, memory, "mounted-hover", hovering) >= 4, "mounted-motion=" + dy);
        }
        if (o.has("vehicle-speed")) {
            double speed = o.value("vehicle-speed");
            double limit = speed * 2.25 + .1 + (unsupported ? .175 : 0);
            sink.sample("entityspeed-a", horizontal > limit && o.value("velocity-age-ticks", 100) > 10,
                    "mounted-speed=" + horizontal + " limit=" + limit);
            sink.sample("nosaddle-a", o.state("horse") && !o.state("saddled") && o.state("vehicle-steered")
                            && horizontal > speed * 2.5 + .1,
                    "unsaddled-controlled-speed=" + horizontal);
        }
    }

    private void gliding(Observation o, DetectionMemory memory, FindingSink sink) {
        if (o.state("firework-boost") || o.state("liquid") || o.state("environment-special")) return;
        double dy = o.value("dy"), lastDy = o.value("previous-dy");
        double horizontal = o.value("horizontal"), lastHorizontal = o.value("previous-horizontal");
        double acceleration = horizontal - lastHorizontal, ticks = o.value("glide-ticks");
        sink.sample("elytra-a", horizontal > o.value("elytra-speed-limit", 4.25), "glide-speed=" + horizontal);
        boolean constant = ticks > 5 && ticks < 400 && horizontal > 1 && Math.abs(acceleration) < 1e-10 && Math.abs(dy - lastDy) < 1e-10;
        sink.sample("elytra-b", repeated(o, memory, "glide-fixed-motion", constant) >= 4, "fixed-glide-speed=" + horizontal);
        sink.sample("elytra-c", ticks > 10 && dy < -.6 && acceleration > .035,
                "steep-descent-acceleration=" + acceleration);
        sink.sample("elytra-g", ticks > 10 && dy > -.5 && horizontal > .65 && acceleration > .031,
                "shallow-glide-acceleration=" + acceleration);
        sink.sample("elytra-i", repeated(o, memory, "glide-decimal-tenth", ticks > 5 && dy > .1 && quantized(dy, .1)) >= 4,
                "glide-decimal-y=" + dy);
        sink.sample("elytra-k", ticks > 5 && dy > 0 && horizontal > .3 && acceleration > .005,
                "ascending-glide-acceleration=" + acceleration);
        if (o.has("pitch")) sink.sample("elytra-l", ticks > 5 && dy > 1.45 + o.value("jump-effect") * .125 && o.value("pitch") > -20,
                "glide-ascension=" + dy + " pitch=" + o.value("pitch"));
        sink.sample("elytra-m", repeated(o, memory, "glide-decimal-hundredth", ticks > 5 && dy > .01 && quantized(dy, .01)) >= 4,
                "glide-hundredth-y=" + dy);
        double scale = Math.max(1, o.value("movement-speed", .1) / .1);
        if (!o.state("entity-cram")) sink.sample("elytra-n", o.state("ground") && o.state("previous-ground") && horizontal > .2975 * scale + .15,
                "grounded-glide-speed=" + horizontal);
    }

    private int repeated(Observation o, DetectionMemory memory, String key, boolean suspicious) {
        String prefix = "movement.streak." + key;
        double previousTick = memory.get(prefix + ".tick", -1);
        double count = suspicious ? previousTick == o.tick() - 1 ? memory.get(prefix) + 1 : 1 : 0;
        memory.set(prefix, count);
        memory.set(prefix + ".tick", o.tick());
        return (int) count;
    }

    private boolean quantized(double value, double step) {
        return Math.abs(value - Math.rint(value / step) * step) < 1e-8;
    }

    private double maximumJumpHeight(double motion, double gravity) {
        double height = 0;
        for (int i = 0; i < 128 && motion > 0; i++) {
            height += motion;
            motion = (motion - gravity) * .98;
        }
        return height;
    }
}
