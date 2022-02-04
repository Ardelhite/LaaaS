package awsutil.dynamodb.tabledefinition;

import com.amazonaws.services.dynamodbv2.model.KeyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set field that be set as Global secondary index
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalSI {
    public String indexName();
    public KeyType keyType();
    public long readCapacity() default 5L;
    public long writeCapacity() default 2L;
}
