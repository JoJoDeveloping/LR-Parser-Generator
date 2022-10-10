package jojomodding.parsergenerator.pda.action;

/**
 * Describes the PDA action of accepting, i.e. finishing parsing.
 */
public record ActionAccept<T>() implements Action<T> {

}
