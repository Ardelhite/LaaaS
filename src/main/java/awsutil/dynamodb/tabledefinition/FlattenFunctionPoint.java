package awsutil.dynamodb.tabledefinition;

import lombok.AllArgsConstructor;

import java.util.AbstractMap;
import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public class FlattenFunctionPoint<T, R> {
    // Coordinates ot putting function
    public List<AbstractMap.SimpleEntry<Integer, Integer>> correspondenceCoordinates;
    // be put function on plane
    public Function<T, R> biMappedFunction;
}
