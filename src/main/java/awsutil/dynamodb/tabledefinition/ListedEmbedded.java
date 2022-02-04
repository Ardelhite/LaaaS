package awsutil.dynamodb.tabledefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ListedEmbedded {
    public Class<? extends IGenericDynamoDbTable> embeddedClass();
}
