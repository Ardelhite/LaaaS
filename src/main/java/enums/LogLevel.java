package enums;

public enum LogLevel {
    INFO("INFO"), WARN("WARN"), ERROR("ERROR");

    private final String logLevel;

    LogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String logLebel() { return logLevel; }
}
