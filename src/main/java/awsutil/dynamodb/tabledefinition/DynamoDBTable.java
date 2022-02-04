package awsutil.dynamodb.tabledefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to data model used as DynamoDB table
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamoDBTable {
    // Name of table name
    public String tableName() default "";
    // read capacity unit for table
    public long readCapacityUnit() default 10L;
    // write capacity unit for table
    public long writeCapacityUnit() default 5L;
}
