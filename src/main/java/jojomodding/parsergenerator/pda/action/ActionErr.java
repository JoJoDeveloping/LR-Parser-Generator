package jojomodding.parsergenerator.pda.action;

/**
 * Describes the PDA action of erroring.
 * @param <T>
 */
public record ActionErr<T>() implements Action<T> {

    @Override
    public String toString() {
        return "error";
    }
}
