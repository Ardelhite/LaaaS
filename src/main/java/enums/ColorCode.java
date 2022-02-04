package enums;

/**
 * Color for standard output
 */
public enum ColorCode {
    RED("\u001b[00;31m"), GREEN("\u001b[00;32m"), YELLOW("\u001b[00;33m"), PURPLE("\u001b[00;34m"),
    PINK("\u001b[00;35m"), CYAN("\u001b[00;36m"), DEFAULT("\u001b[00m");

    private final String colorCode;

    ColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String code() {
        return this.colorCode;
    }
}
