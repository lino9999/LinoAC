package io.github.lino9999.linoac.detection;

import java.util.Set;

public interface RuleModule {
    Set<String> checkIds();
    void evaluate(Observation observation, DetectionMemory memory, FindingSink sink);

    @FunctionalInterface
    interface FindingSink {
        void sample(String checkId, boolean suspicious, String detail);
        default void direct(String checkId, String detail) { sample(checkId, true, detail); }
        default void reset(String checkId, String detail) { sample(checkId, false, detail); }
        default void decay(String checkId, double amount, String detail) { sample(checkId, false, detail); }
        default void sampleAbove(String checkId, boolean suspicious, String detail) { sample(checkId, suspicious, detail); }
        default void sampleAboveThenDecay(String checkId, String detail) { sampleAbove(checkId, true, detail); }
        default void sampleAboveOrDirect(String checkId, boolean immediate, String detail) { if (immediate) sample(checkId, true, detail); else sampleAbove(checkId, true, detail); }
    }
}
