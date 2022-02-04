package awsutil.dynamodb.tabledefinition;

import java.util.function.Predicate;

/**
 * Generator of random value by specific digits
 * @param <T>
 */
public interface IRandomValueGenerator<T> extends Iterable<T>{

    public T generateRandomValue(Integer digits, Predicate<T> isMatchedGeneratingConditions);
}
