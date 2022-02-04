package facade;

import enums.ApiRequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiGwEvents {
    public String basePath() default  "/";
    public ApiRequestType requestMethodType() default ApiRequestType.OPTIONS;
    public Class<?> requestPayloadDataModel() default Object.class;
}
