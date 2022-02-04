package utils;

import enums.LogLevel;

/**
 * For use logging
 */
public class Logger {

    static public void logging(ILogger engine, String message, LogLevel logLevel) {
        engine.logging(message, logLevel);
    }
}
