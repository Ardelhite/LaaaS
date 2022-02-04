package utils;

import enums.LogLevel;

public class LogHeader {
    static public String logHeader(Class<?> onLoggingClass, LogLevel logLevel) {
        return "[" + onLoggingClass.getName() + " - " + logLevel.logLebel() + "]";
    }

    static public String logHeader(String location, LogLevel logLevel) {
        return "[" + location + " - " + logLevel.logLebel() + "]";
    }
}
