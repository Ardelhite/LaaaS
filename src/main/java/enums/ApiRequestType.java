package enums;

/**
 * HttpMethodType Enum
 */
public enum ApiRequestType {
    POST("POST"), GET("GET"), PUT("PUT"), DELETE("DELETE"), CONNECT("CONNECT"), OPTIONS("OPTIONS"),
    TRACE("TRACE"), PATCH("PATCH");

    private final String requestType;

    ApiRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String reqType() { return requestType; }
}
