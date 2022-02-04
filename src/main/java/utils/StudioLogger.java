package utils;

import enums.ColorCode;
import enums.LogLevel;

/**
 * Output log into stdout
 */
public class StudioLogger implements ILogger {
    @Override
    public void logging(String log, LogLevel logLevel) {
        switch (logLevel) {
            case INFO:
                System.out.println(log);
                break;
            case WARN:
                System.out.println(ColorCode.YELLOW.code() + log + ColorCode.DEFAULT.code());
                break;
            case ERROR:
                System.out.println(ColorCode.RED.code() + log + ColorCode.DEFAULT.code());
                break;
        }
    }
}
