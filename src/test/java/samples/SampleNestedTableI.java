package samples;

import awsutil.dynamodb.tabledefinition.Embedded;
import awsutil.dynamodb.tabledefinition.IGenericDynamoDbTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embedded
public class SampleNestedTableI implements IGenericDynamoDbTable {

    public String embField01;

    public ArrayList<String> nestedAListAttr;

    public SampleNestedTableI init(String embField01, ArrayList<String> nestedAListAttr) {
        this.embField01 = embField01;
        this.nestedAListAttr = nestedAListAttr;
        return this;
    }

}
