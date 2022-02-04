package awsutil.dynamodb.exceptions;

import java.util.AbstractMap;

public class DuplicatedSortKeyException extends Exception {

    private final String currentSortKeyName;
    private final String newSortKeyName;

    public DuplicatedSortKeyException(String currentSortKeyName, String newSortKeyName) {
        this.currentSortKeyName = currentSortKeyName;
        this.newSortKeyName = newSortKeyName;
    }

    public AbstractMap.SimpleEntry<String, String> getDuplicatedSortKeys() {
        return new AbstractMap.SimpleEntry<String, String>(this.currentSortKeyName, this.newSortKeyName);
    }

    @Override
    public String getMessage() {
        return "Sort key is already set > (Current) " + this.currentSortKeyName+ "\n(New) " + this.newSortKeyName;
    }
}
