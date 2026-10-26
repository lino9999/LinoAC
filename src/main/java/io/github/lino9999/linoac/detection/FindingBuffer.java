package io.github.lino9999.linoac.detection;

public final class FindingBuffer {
    private FindingBuffer() {}

    public static Result advance(double current, double maximum, Mode mode) {
        return advance(current, maximum, mode, 0);
    }

    public static Result advance(double current, double maximum, Mode mode, double decay) {
        if (!Double.isFinite(current) || current < 0 || !Double.isFinite(maximum) || maximum < 0 || mode == null || !Double.isFinite(decay) || decay < 0) {
            throw new IllegalArgumentException("Invalid finding buffer state.");
        }
        if (mode == Mode.DIRECT) return new Result(current, true);
        boolean strict = mode == Mode.ABOVE_THRESHOLD || mode == Mode.ABOVE_WITH_DECAY;
        double next = Math.min(strict ? 10000 : maximum, current + 1);
        boolean ready = strict ? next > maximum : next >= maximum;
        return new Result(mode == Mode.ABOVE_WITH_DECAY && !ready ? Math.max(0, next - decay) : next, ready);
    }

    public enum Mode { AT_THRESHOLD, ABOVE_THRESHOLD, ABOVE_WITH_DECAY, DIRECT }
    public static Result advanceOrDirect(double current, double maximum, boolean immediate) {
        Result result = advance(current, maximum, Mode.ABOVE_THRESHOLD);
        return new Result(result.amount(), result.ready() || immediate);
    }

    public static double submitted(Result result, double multiplier) {
        if (result == null || !Double.isFinite(result.amount()) || result.amount() < 0 || !Double.isFinite(multiplier) || multiplier < 0 || multiplier > 1) throw new IllegalArgumentException("Invalid finding buffer multiplier.");
        return result.ready() ? result.amount() * multiplier : result.amount();
    }

    public record Result(double amount, boolean ready) {}

    public static double accumulated(double current) {
        if (!Double.isFinite(current) || current < 0) throw new IllegalArgumentException("Invalid finding buffer state.");
        return Math.min(10000, current + 1);
    }

    public static double decayed(double current, double amount) {
        if (!Double.isFinite(current) || current < 0 || !Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid finding decay.");
        return Math.max(0, current - amount);
    }
}
