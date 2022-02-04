package utils;

import enums.LogLevel;

/**
 * For logging engine
 */
public interface ILogger {

    public void logging(String log, LogLevel logLevel);
}
