package jojomodding.parsergenerator.parsed;

import java.util.LinkedList;

/**
 * A leaf in the ASt, representing a single token.
 * @param token the token.
 * @param <T> the type of the token.
 */
public record AbstractSyntaxToken<T>(T token) implements AbstractSyntax<T> {

    @Override
    public String toString() {
        return "t(" + token + ")";
    }

}
