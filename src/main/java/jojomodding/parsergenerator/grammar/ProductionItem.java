package jojomodding.parsergenerator.grammar;

/**
 * A production item. Either a terminal, or a non-terminal symbol.
 */
public interface ProductionItem<T> {

    /**
     * Check if this is well-formed under a given grammar. In particular, non-terminals must be defined by the grammar.
     * @param grammar the grammar that this must be well-formed under.
     * @return true if this is well-formed under the given grammar, false otherwise.
     */
    boolean isWellFormed(Grammar<T> grammar);

    /**
     * Renders this item into a string.
     * @return this item, rendered into a string.
     */
    String format();

}
