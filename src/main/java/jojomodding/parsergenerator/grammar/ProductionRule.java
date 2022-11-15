package jojomodding.parsergenerator.grammar;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import jojomodding.parsergenerator.parsed.AbstractSyntax;
import jojomodding.parsergenerator.utils.Utils;

/**
 * The right hand side of a production rule.
 * @param items The list of production items. May be empty to represent an epsilon-rule.
 */
public record ProductionRule<T>(List<ProductionItem<T>> items, BiFunction<NonTerminal<T>, List<String>, String> formatter) {

    public ProductionRule(List<ProductionItem<T>> items) {
        this(items, (element, children) -> "(" + element.name() + ":=" + Utils.formatWord(children, Function.identity()) + ")");
    }

    @SafeVarargs
    public ProductionRule(BiFunction<NonTerminal<T>, List<String>, String> formatter, ProductionItem<T>... items) {
        this(List.of(items), formatter);
    }

    /**
     * Construct a new production rule for a vararg list of items
     * @param item the new items
     * @return The corresponding production rule
     */
    @SafeVarargs
    public static <T> ProductionRule<T> of(ProductionItem<T>... item) {
        return new ProductionRule<T>(List.of(item));
    }

    public static <T> ProductionRule<T> empty() {
        return new ProductionRule<>(List.of());
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
        return Utils.formatWord(items, ProductionItem::format);
    }
}
