package awsutil.dynamodb.tabledefinition;

import com.amazonaws.services.dynamodbv2.model.KeyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Like as external key, Create new table and relation data
 * Each table are related as 1:many
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalRelation {
    // Destination class
    public Class<? extends IGenericDynamoDbTable> relationTo();
    // Index type of relation
    public ERelationKeyType relationKeyType();
    // for GSI
    public KeyType gsiKeyType() default KeyType.HASH;
    // for GSI and LSI
    public String indexName() default "";
}
