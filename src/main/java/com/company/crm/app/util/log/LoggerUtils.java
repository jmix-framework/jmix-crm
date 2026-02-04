package com.company.crm.app.util.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public final class LoggerUtils {

    public static void setLevel(Class<?> clazz, Level level) {
        logger(clazz).setLevel(level);
    }

    public static void runWithLevel(Class<?> clazz, Level level, Runnable runnable) {
        var logger = logger(clazz);
        Level currentLevel = logger.getLevel();
        logger.setLevel(level);

        try {
            runnable.run();
        } finally {
            logger.setLevel(currentLevel);
        }
    }

    private static Logger logger(Class<?> clazz) {
        return (Logger) LoggerFactory.getLogger(clazz);
    }

    private LoggerUtils() {
    }
}