package hw.smoup.smouprct.client;

import java.util.List;

public final class TimingTuner {

    private static final int WINDOW = 10;
    private static final double BUMP_FACTOR = 1.25;

    private static final int MAX_SETTLE = 40;
    private static final int MAX_STABLE = 20;
    private static final int MAX_MENU_TIMEOUT = 300;
    private static final int MAX_MENU_RETRY = 100;
    private static final int MAX_OPERATION_TIMEOUT = 20 * 60;

    private TimingTuner() {
    }

    public static void recordOperation(RctConfig config, int hubTravel, int menuOpen,
                                       int contentSettle, int screenChange) {
        if (!config.autoTuneTimings) return;

        Timings measured = config.timings;
        keepLast(measured.hubTravel, hubTravel);
        keepLast(measured.menuOpen, menuOpen);
        keepLast(measured.contentSettle, contentSettle);
        keepLast(measured.screenChange, screenChange);

        if (measured.samples() >= WINDOW) retune(config);
        config.save();
    }

    public static void onSlowScreen(RctConfig config) {
        if (!config.autoTuneTimings) return;

        int settle = bumped(config.menuSettleTicks, MAX_SETTLE);
        int stable = bumped(config.contentStableTicks, MAX_STABLE);
        if (settle == config.menuSettleTicks && stable == config.contentStableTicks) return;

        RctLog.detail("Экран медленный: menuSettleTicks {} -> {}, contentStableTicks {} -> {}",
                config.menuSettleTicks, settle, config.contentStableTicks, stable);
        config.menuSettleTicks = settle;
        config.contentStableTicks = stable;
        config.save();
    }

    public static void onMenuTimeout(RctConfig config) {
        if (!config.autoTuneTimings) return;

        int timeout = bumped(config.menuOpenTimeoutTicks, MAX_MENU_TIMEOUT);
        int retry = bumped(config.menuRetryTicks, MAX_MENU_RETRY);
        if (timeout == config.menuOpenTimeoutTicks && retry == config.menuRetryTicks) return;

        RctLog.detail("Меню не дождалось: menuOpenTimeoutTicks {} -> {}, menuRetryTicks {} -> {}",
                config.menuOpenTimeoutTicks, timeout, config.menuRetryTicks, retry);
        config.menuOpenTimeoutTicks = timeout;
        config.menuRetryTicks = retry;
        config.save();
    }

    public static void onOperationTimeout(RctConfig config) {
        if (!config.autoTuneTimings) return;

        int timeout = bumped(config.operationTimeoutTicks, MAX_OPERATION_TIMEOUT);
        if (timeout == config.operationTimeoutTicks) return;

        RctLog.detail("Операция не уложилась: operationTimeoutTicks {} -> {}",
                config.operationTimeoutTicks, timeout);
        config.operationTimeoutTicks = timeout;
        config.save();
    }

    private static void retune(RctConfig config) {
        Timings measured = config.timings;
        StringBuilder changes = new StringBuilder();

        if (!measured.hubTravel.isEmpty()) {
            config.hubArriveFallbackTicks = onlyGrow(changes, "hubArriveFallbackTicks",
                    config.hubArriveFallbackTicks, withHeadroom(max(measured.hubTravel), 1.5, 10), 30, 200);
        }
        if (!measured.menuOpen.isEmpty()) {
            config.menuOpenTimeoutTicks = onlyGrow(changes, "menuOpenTimeoutTicks",
                    config.menuOpenTimeoutTicks, withHeadroom(max(measured.menuOpen), 2.0, 20), 60, MAX_MENU_TIMEOUT);
            config.menuRetryTicks = onlyGrow(changes, "menuRetryTicks",
                    config.menuRetryTicks, withHeadroom(max(measured.menuOpen), 1.5, 5), 10, MAX_MENU_RETRY);
        }
        if (!measured.screenChange.isEmpty()) {
            config.screenChangeTimeoutTicks = onlyGrow(changes, "screenChangeTimeoutTicks",
                    config.screenChangeTimeoutTicks, withHeadroom(max(measured.screenChange), 2.0, 10), 30, 200);
        }
        if (!measured.contentSettle.isEmpty()) {
            config.menuSettleTicks = set(changes, "menuSettleTicks",
                    config.menuSettleTicks, max(measured.contentSettle), 2, MAX_SETTLE);
        }

        if (!changes.isEmpty()) {
            RctLog.detail("Подстроил тайминги по {} заходам:{}", WINDOW, changes);
        }
    }

    private static void keepLast(List<Integer> samples, int value) {
        if (value < 0) return;
        samples.add(value);
        while (samples.size() > WINDOW) {
            samples.remove(0);
        }
    }

    private static int onlyGrow(StringBuilder changes, String name, int current, int wanted,
                                int floor, int ceiling) {
        return set(changes, name, current, Math.max(current, wanted), floor, ceiling);
    }

    private static int set(StringBuilder changes, String name, int current, int wanted,
                           int floor, int ceiling) {
        int value = Math.max(floor, Math.min(ceiling, wanted));
        if (value == current) return current;

        changes.append("\n  ").append(name).append(": ").append(current).append(" -> ").append(value);
        return value;
    }

    private static int bumped(int current, int ceiling) {
        int raised = Math.max((int) Math.ceil(current * BUMP_FACTOR), current + 1);
        return Math.min(raised, ceiling);
    }

    private static int withHeadroom(int observed, double factor, int extra) {
        return (int) Math.ceil(observed * factor) + extra;
    }

    private static int max(List<Integer> samples) {
        int max = 0;
        for (int value : samples) {
            max = Math.max(max, value);
        }
        return max;
    }
}
