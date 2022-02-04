package awsutil.dynamodb.tabledefinition;

import java.util.function.Predicate;

/**
 * Random value generator
 * @param <T> Returning type
 */
public abstract class AbsAutoValueGenerator<T> {

    /**
     * Generate engine
     */
    IRandomValueGenerator<T> generator;

    /**
     * Function for evaluation generated value
     */
    Predicate<T> isMatchedGeneratingConditions;


    public AbsAutoValueGenerator(IRandomValueGenerator<T> generator, Predicate<T> isMatchedGeneratingConditions) {
        this.generator = generator;
        this.isMatchedGeneratingConditions = isMatchedGeneratingConditions;
    }

    public T generateRandomValue(Integer digits){
        return this.generator.generateRandomValue(digits, this.isMatchedGeneratingConditions);
    }
}
