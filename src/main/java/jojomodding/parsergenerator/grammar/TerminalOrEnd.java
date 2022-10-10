package jojomodding.parsergenerator.grammar;

/**
 * Either a terminal, or the EOF marker.
 */
public interface TerminalOrEnd<T> extends ProductionItem<T>{

    static <T> TerminalOrEnd<T> ofNullable(T t) {
        if (t == null) return new EndMarker<>();
        return new Terminal<>(t);
    }
}
