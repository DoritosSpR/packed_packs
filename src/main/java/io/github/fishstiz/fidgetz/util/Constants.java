package io.github.fishstiz.fidgetz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    public static final String PROJECT_ID = "fidgetz";
    public static final Logger LOGGER = LoggerFactory.getLogger(PROJECT_ID);

    private Constants() {
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
