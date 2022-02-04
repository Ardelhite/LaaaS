package awsutil.dynamodb.tabledefinition;

import com.amazonaws.services.dynamodbv2.model.*;
import lombok.AllArgsConstructor;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class GlobalSecondlyIndexStructure {
    public String indexName;
    // SimpleEntry<Field name, keyType>
    public List<AbstractMap.SimpleEntry<String, KeyType>> keyAndAttributes;
    Long readCapacity;
    Long writeCapacity;

    public GlobalSecondaryIndex toKeySchemeElement() {
        if(isOnlyHashKey()) {
            AbstractMap.SimpleEntry<String, KeyType> gsi = this.keyAndAttributes.get(0);
            // GSI has only Hash key
            return new GlobalSecondaryIndex().withIndexName(this.indexName).withKeySchema(
                    new KeySchemaElement().withAttributeName(gsi.getKey()).withKeyType(gsi.getValue()))
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(this.readCapacity)
                            .withWriteCapacityUnits(this.writeCapacity)
                    );

        } else if(hasSortKey()) {
            // GSI has hash and sort key
            List<AbstractMap.SimpleEntry<String, KeyType>> sortedList =
                    this.keyAndAttributes.stream().sorted(Comparator.comparing(AbstractMap.SimpleEntry::getValue)).collect(Collectors.toList());
            return new GlobalSecondaryIndex().withIndexName(this.indexName).withKeySchema(
                    new KeySchemaElement().withAttributeName(sortedList.get(0).getKey()).withKeyType(sortedList.get(0).getValue()),
                    new KeySchemaElement().withAttributeName(sortedList.get(1).getKey()).withKeyType(sortedList.get(1).getValue()))
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(this.readCapacity)
                            .withWriteCapacityUnits(this.writeCapacity)
                    );
        }
        return null;
    }

    public void addNewKey(String fieldName, KeyType keyType) {
        this.keyAndAttributes.add(new AbstractMap.SimpleEntry<>(fieldName, keyType));
    }

    public boolean isOnlyHashKey() {
        return this.keyAndAttributes.size() == 1 && this.keyAndAttributes.get(0).getValue() == KeyType.HASH;
    }

    public boolean hasSortKey() {
        return this.keyAndAttributes.size() == 2 &&
                this.keyAndAttributes.get(0).getValue() != this.keyAndAttributes.get(1).getValue();
    }
}
