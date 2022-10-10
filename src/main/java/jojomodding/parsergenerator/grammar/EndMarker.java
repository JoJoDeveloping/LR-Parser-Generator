package jojomodding.parsergenerator.grammar;

/**
 * The EOF marker, representing the end of input.
 */
public record EndMarker<T>() implements ExtendedProductionItem<T>, TerminalOrEnd<T> {

    @Override
    public boolean isWellFormed(Grammar<T> t) {
        return true;
    }

    @Override
    public String format() {
        return "EOF";
    }
}
