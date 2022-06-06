package awsutil.dynamodb.tabledefinition;

import java.util.ArrayList;
import java.util.Collection;

/**
 * List for DDB record without duplicated record
 */
public class DdbRecordCollection extends ArrayList<IGenericDynamoDbTable> {
    @Override
    public boolean add(IGenericDynamoDbTable newRecord) {
        if (newRecord != null && ! newRecord.isExistsInList(this)) {
            return super.add(newRecord);
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends IGenericDynamoDbTable> records) {
        if (records != null) {
            for (IGenericDynamoDbTable record: records) {
                this.add(record);
            }
        } else return false;
        return this.size() != 0;
    }

    @Override
    public boolean addAll(int index, Collection<? extends IGenericDynamoDbTable> records) {
        // Delegate process when specified invalid index
        if (index < 1 || index > this.size() - 1) this.addAll(records);

        DdbRecordCollection head = new DdbRecordCollection();
        DdbRecordCollection tail = new DdbRecordCollection();
        // Separate this to head and tail
        for (int current = 0; current < this.size(); current++) {
            if (current < index - 1) head.add(this.get(current));
            else tail.add(this.get(current));
        }
        this.clear();
        // Joining lists
        this.addAll(head);
        this.addAll(records);
        this.addAll(tail);

        return this.size() != 0;
    }
}
