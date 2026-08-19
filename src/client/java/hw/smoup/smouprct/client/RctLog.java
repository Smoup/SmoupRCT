package hw.smoup.smouprct.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RctLog {

    private static final Logger LOG = LoggerFactory.getLogger("SmoupRCT");

    private static RctConfig config;

    private RctLog() {
    }

    public static void init(RctConfig cfg) {
        config = cfg;
    }

    public static void detail(String message, Object... args) {
        if (config != null && config.debugLog) {
            LOG.info(message, args);
        }
    }

    public static void warn(String message, Object... args) {
        LOG.warn(message, args);
    }
}
