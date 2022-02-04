package samples;

import awsutil.dynamodb.tabledefinition.DynamoDBTable;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import awsutil.dynamodb.tabledefinition.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(
        tableName = "test-boolean-table"
)
public class BooleanTable implements IGenericDynamoDbTable {
    @PartitionKey
    public String id = "";

    public Boolean isField = true;

    public ArrayList<Boolean> booleanList = new ArrayList<>();
}
