package io.github.lino9999.linoac.check;

import java.util.List;

public record CheckPolicy(boolean enabled, double buffer, double decay, int maxViolations,
                          boolean setback, boolean punishable, List<String> commands,
                          double bufferMultiplier, int minimumSetbackViolations, int alertInterval,
                          int minimumAlertViolations, int maximumPing, double minimumTps,
                          int webhookInterval, boolean broadcastPunishment, int shuffleMinimum,
                          int shuffleInterval, int rotateMinimum, int rotateInterval) {}
