package awsutil.dynamodb.tabledefinition;

import java.util.HashSet;

/**
 * For evaluate relationship between each tables
 */
public class TableRelation {
    // Root table is 0 and incremented by children
    public Integer rankOfTable = 0;
    // Type of current table
    public Class<?> typeOfTable;
    // List of parent of this table
    public HashSet<Class<?>> parentTables = new HashSet<>();

    // Table ID is unique: ex.rootTableClass@0
    public String getTableId() {
        return typeOfTable + "@" + rankOfTable;
    }

    TableRelation(Integer rank, Class<?> typeOfTable) {
        this.rankOfTable = rank;
        this.typeOfTable = typeOfTable;
    }
}
