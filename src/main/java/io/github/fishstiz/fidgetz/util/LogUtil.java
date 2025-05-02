package io.github.fishstiz.fidgetz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
    public static final Logger LOGGER = LoggerFactory.getLogger("fidgetz");

    private LogUtil() {
    }

    public static void logUnsupported(String message) {
        try {
            throw new UnsupportedOperationException(message);
        } catch (UnsupportedOperationException e) {
            if (!e.getMessage().isEmpty()) {
                LOGGER.error("Unsupported operation: ", e);
            } else {
                LOGGER.error("Unsupported operation. ", e);
            }
        }
    }

    public static void logUnsupported() {
        logUnsupported("");
    }
}
