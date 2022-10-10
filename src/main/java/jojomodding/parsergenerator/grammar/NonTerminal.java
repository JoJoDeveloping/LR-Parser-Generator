package jojomodding.parsergenerator.grammar;

/**
 * The production item representing a non-terminal.
 * @param name The name of the non-terminal.
 */
public record NonTerminal<T> (String name) implements ProductionItem<T> {

    @Override
    public boolean isWellFormed(Grammar<T> grammar) {
        return grammar.hasNonTerminal(this);
    }

    public static <T> NonTerminal<T> n(String s) {
        return new NonTerminal<>(s);
    }

    @Override
    public String toString() {
        return "n(" + name + ")";
    }

    @Override
    public String format() {
        return name;
    }
}
