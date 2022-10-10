package jojomodding.parsergenerator.converter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.TerminalOrEnd;

/**
 * An LR(n) item.
 * These look like [from -> before _ after | lookahead], and represent a partially parsed production rule.
 * The core of such an item refers to that item with the lookahead being the empty string.
 * Instead of thinking of before and after as distinct, it is better to think of the position of the separator, which is also called "dot".
 * @param from A non-terminal
 * @param before The string of parsed grammar items.
 * @param after The string of grammar items yet to be parsed.
 * @param lookahead The lookahead, an element of Follow(from)
 */
public record ProductionRuleItem<T>(NonTerminal<T> from, ProductionRule<T> before, ProductionRule<T> after, List<TerminalOrEnd<T>> lookahead) {

    /**
     * Check if this item is well-formed, which it is if its core resembles a valid production item.
     * @param grammar
     * @return
     */
    public boolean isWellFormed(Grammar<T> grammar) {
        return grammar.hasNonTerminal(from) && before().isWellFormed(grammar) && after().isWellFormed(grammar);
    }

    public String toString() {
        return "[" + from().name() + " -> " +
                before.items().stream().map(ProductionItem::format).collect(Collectors.joining(" "))
                + "_" +
                after.items().stream().map(ProductionItem::format).collect(Collectors.joining(" "))
                + " | " + lookahead.stream().map(TerminalOrEnd::format).collect(Collectors.joining(" ")) + "]";
    }

    /**
     * Returns the next grammar item to be parsed, or None if none such item exists.
     * @return The next item after the dot.
     */
    public Optional<ProductionItem<T>> firstAfterDot() {
        return after.items().stream().findFirst();
    }

    /**
     * Moves the dot one grammar item forward.
     * @return The production rule with the dot shifted.
     */
    public ProductionRuleItem<T> advanceOne() {
        if(after.items().isEmpty()) throw new IllegalArgumentException();
        var before = new LinkedList<>(before().items());
        var after = new LinkedList<>(after().items());
        before.addLast(after.removeFirst());
        return new ProductionRuleItem<>(from, new ProductionRule<>(before), new ProductionRule<>(after), lookahead);
    }

    /**
     * Check if this item allows a reduce action in the PDA.
     * This is the case iff the dot is at the very end.
     * @return True iff the dot is at the very end, otherwise false.
     */
    public boolean isReduce() {
        return after().items().isEmpty();
    }
}
