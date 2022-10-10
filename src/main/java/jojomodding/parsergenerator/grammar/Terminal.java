package jojomodding.parsergenerator.grammar;

/**
 * The production item representing a terminal.
 * @param terminal the terminal.
 * @param <T> the type of the terminal.
 */
public record Terminal<T>(T terminal) implements ProductionItem<T>{

    @Override
    public boolean isWellFormed(Grammar<T> t) {
        return true;
    }

    public static <T> Terminal<T> t(T t) {
        return new Terminal<>(t);
    }

    @Override
    public String toString() {
        return "t(" + terminal.toString() + ")";
    }

    @Override
    public String format() {
        return "'" + terminal + "'";
    }
}
