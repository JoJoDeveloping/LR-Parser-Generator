package jojomodding.parsergenerator.grammar;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The right hand side of a production rule.
 * @param items The list of production items. May be empty to represent an epsilon-rule.
 */
public record ProductionRule<T>(List<ProductionItem<T>> items) {

    /**
     * Construct a new production rule for a vararg list of items
     * @param item the new items
     * @return The corresponding production rule
     */
    @SafeVarargs
    public static <T> ProductionRule<T> of(ProductionItem<T>... item) {
        return new ProductionRule<T>(List.of(item));
    }

    /**
     * Check if this is well-formed under a given grammar, which it is iff all contained items are well-formed.
     * @param grammar The grammar this needs to be well-formed under.
     * @return true if this is well-formed, false otherwise.
     */
    public boolean isWellFormed(Grammar<T> grammar) {
        return items.stream().allMatch(x -> x.isWellFormed(grammar));
    }

    @Override
    public String toString() {
        if(items.isEmpty()) {
            return "ε";
        } else {
            return items.stream().map(ProductionItem::format).collect(Collectors.joining(" "));
        }
    }
}
