package awsutil.dynamodb.exceptions;

import awsutil.dynamodb.tabledefinition.TableRelation;
import enums.LogLevel;
import utils.LogHeader;

import java.util.List;

/**
 * Throw this exception when find circular reference between each table by @ExternalRelation
 */
public class ExistsCircularReferenceException extends Exception {

    private final List<TableRelation> parentClass;
    private final Class<?> currentClass;
    private final Integer rankOfChildClass;

    public ExistsCircularReferenceException(List<TableRelation> parentClass, Class<?> currentClass, Integer rankOfCurrentClass) {
        this.parentClass = parentClass;
        this.currentClass = currentClass;
        this.rankOfChildClass = rankOfCurrentClass;
    }

    @Override
    public String getMessage() {
        return "[" + LogHeader.logHeader("Evaluate relation", LogLevel.ERROR) +
                "] Circular reference has be found as " + currentClass.getName() + "@" + rankOfChildClass + " to "
                + parentClass.stream().reduce(
                        "<Parents>",
                (mes, relation) -> mes + " " + relation.parentTables + "@" + relation.rankOfTable,
                (smes, str) -> smes + str
        );
    }
}
