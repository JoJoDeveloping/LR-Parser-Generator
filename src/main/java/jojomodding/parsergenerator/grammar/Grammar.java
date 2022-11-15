package jojomodding.parsergenerator.grammar;

import static jojomodding.parsergenerator.grammar.ProductionRule.of;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jojomodding.parsergenerator.utils.Utils;

/**
 * Represents a context-free grammar.
 *
 * @param <T> the type of characters the grammar is over.
 */
public class Grammar<T> {

    /**
     * The set of defined non-terminals.
     */
    private Set<NonTerminal<T>> nonTerminals;

    /**
     * The set of production rules. More specifically, for each non-terminal, the set of RHS of production rules.
     */
    private final Map<NonTerminal<T>, Set<ProductionRule<T>>> productionRules;

    /**
     * The total set of terminals appearing in this grammar.
     */
    private final Set<T> terminals;

    /**
     * The inital rule of this grammar.
     */
    private NonTerminal<T> initial;

    /**
     * Creates a new grammar.
     *
     * @param nonTerminals The total set of non-terminals that will ever appear in this grammar.
     * @param initial      The initial non-terminal. Must be in the above set.
     */
    public Grammar(Collection<NonTerminal<T>> nonTerminals, NonTerminal<T> initial) {
        this.nonTerminals = new HashSet<>(nonTerminals);
        productionRules = new HashMap<>();
        this.initial = initial;
        if (!this.nonTerminals.contains(initial)) {
            throw new IllegalArgumentException("Initial state must be contained!");
        }
        for (var T : nonTerminals) {
            productionRules.put(T, new HashSet<>());
        }
        terminals = new HashSet<>();
    }

    /**
     * Creates a new grammar.
     *
     * @param nonTerminals The total set of non-terminals that will ever appear in this grammar, given as strings, representing the name.
     * @param initial      The name of the initial non-terminal. Must be in the above set.
     */
    public Grammar(Collection<String> nonTerminals, String initial) {
        this(nonTerminals.stream().map(NonTerminal<T>::new).collect(Collectors.toUnmodifiableSet()), new NonTerminal<>(initial));
    }

    /**
     * Check if a non-terminal is defined by this grammar.
     *
     * @param t the non-terminal.
     * @return true iff this grammar defines t, false otherwise.
     */
    public boolean hasNonTerminal(NonTerminal<T> t) {
        return nonTerminals.contains(t);
    }

    /**
     * Gets all production rules. The set is immutable and is not updated when the grammar changes.
     *
     * @return The map, representing production rules.
     */
    public Map<NonTerminal<T>, Set<ProductionRule<T>>> getProductionRules() {
        return Map.copyOf(productionRules);
    }

    /**
     * Gets the set of all non-terminals. The set is immutable and is not updated when the grammar changes.
     *
     * @return The set of all non-terminals.
     */
    public Set<NonTerminal<T>> getNonTerminals() {
        return Set.copyOf(nonTerminals);
    }

    /**
     * Adds a new production rule T -> to
     *
     * @param T  the LHS
     * @param to the RHS
     */
    public void addProduction(NonTerminal<T> T, ProductionRule<T> to) {
        if (!nonTerminals.contains(T) || !to.isWellFormed(this)) {
            throw new IllegalArgumentException("Invalid production rule!");
        }
        this.productionRules.get(T).add(to);
        to.items().stream().flatMap(x -> x instanceof Terminal<T> tt ? Stream.of(tt.terminal()) : Stream.empty()).forEach(
                terminals::add);
    }

    /**
     * Adds a new production rule from -> to
     *
     * @param from the LHS
     * @param to   the RHS
     */
    public void addProduction(String from, ProductionRule<T> to) {
        NonTerminal<T> T = new NonTerminal<>(from);
        addProduction(T, to);
    }

    /**
     * Adds a new production rule from -> to
     *
     * @param from the LHS
     * @param to   the RHS
     */
    @SafeVarargs
    public final void addProduction(String from, ProductionItem<T>... to) {
        NonTerminal<T> T = new NonTerminal<>(from);
        addProduction(T, new ProductionRule<>(List.of(to)));
    }

    /**
     * Checks whether this grammar is extended. An extended grammar has an initial non-terminal S' that only has one production rule S' -> S for some
     * other non-terminal S
     *
     * @return true if this grammar is extended, otherwise false
     */
    private boolean isExtended() {
        if (productionRules.get(initial).size() != 1) {
            return false;
        }
        var any = productionRules.get(initial).stream().findAny().orElseThrow();
        if (any.items().size() != 1) {
            return false;
        }
        return any.items().get(0) instanceof NonTerminal<T>;
    }

    /**
     * Ensures that this grammar is extended.
     * @see Grammar::isExtended
     */
    public void extend() {
        if (isExtended()) {
            return;
        }
        String name = Utils.freshName("Start", x -> nonTerminals.contains(new NonTerminal<T>(x)));
        var S = new NonTerminal<T>(name);
        nonTerminals.add(S);
        productionRules.put(S, new HashSet<>());
        addProduction(S, of(initial));
        initial = S;
    }

    /**
     * Computes the productive non-terminals.
     * A non-terminal is productive if it can generate a string.
     * @param considered the set of non-terminals which are to be included in the analysis.
     * @return A set containing all productive non-terminals also contained in considered.
     */
    private Set<NonTerminal<T>> computeProductiveNonterminals(Set<NonTerminal<T>> considered) {
        Set<NonTerminal<T>> productive = new HashSet<>();
        boolean change;
        do {
            change = false;
            outer:
            for (NonTerminal<T> T : considered) {
                if (productive.contains(T)) {
                    continue;
                }
                for (var prod : productionRules.get(T)) {
                    if (prod.items().stream().allMatch(x ->
                            x instanceof Terminal<T> || (x instanceof NonTerminal<T> nt && productive.contains(nt))
                    )) {
                        change |= productive.add(T);
                        continue outer;
                    }
                }
            }
        } while (change);
        return productive;
    }

    /**
     * Computes the reachable non-terminals.
     * These are all non-terminals that can be generated when starting at the initial state.
     * @return The set of all reachable non-terminals.
     */
    private Set<NonTerminal<T>> computeReachableNonterminals() {
        Set<NonTerminal<T>> reachable = new HashSet<>();
        reachable.add(initial);
        boolean change;
        do {
            change = false;
            for (NonTerminal<T> T : Set.copyOf(reachable)) {
                for (var prod : productionRules.get(T)) {
                    for (var elem : prod.items()) {
                        if (elem instanceof NonTerminal<T> nbr) {
                            change |= reachable.add(nbr);
                        }
                    }
                }
            }
        } while (change);
        return reachable;
    }

    /**
     * Ensures that this grammar is reduced.
     * A grammar is reduced if all states are productive and reachable.
     * This method remove all states that are not such.
     * @throws IllegalArgumentException if the grammar is empty.
     */
    public void reduce() {
        Set<NonTerminal<T>> productiveAndReachable = new HashSet<>(nonTerminals);
        while (true) {
            var reachable = computeReachableNonterminals();
            boolean change = false;
            change |= productiveAndReachable.retainAll(reachable);
            var productive = computeProductiveNonterminals(productiveAndReachable);
            change |= productiveAndReachable.retainAll(productive);
            if (!change) {
                break;
            }
        }
        if (productiveAndReachable.isEmpty() || !productiveAndReachable.contains(initial)) {
            throw new IllegalArgumentException("Your Grammar must contain at least one word!");
        }
        this.nonTerminals.retainAll(productiveAndReachable);
        this.productionRules.keySet().retainAll(productiveAndReachable);
    }

    /**
     * Check if this grammar has the production rule from -> to.
     * @param from the LHS
     * @param to the RHS
     * @return if from -> to is a production rule in this grammar.
     */
    public boolean hasProductionRule(NonTerminal<T> from, ProductionRule<T> to) {
        if (!hasNonTerminal(from) || !to.isWellFormed(this)) {
            return false;
        }
        return productionRules.get(from).contains(to);
    }

    @Override
    public String toString() {
        String s = "initial: " + initial.name() + "\n";
        for (NonTerminal<T> nt : nonTerminals) {
            int len = nt.name().length();
            boolean first = true;
            for (var rule : productionRules.get(nt)) {
                s += (first ? nt.name() + " -> " : " ".repeat(len) + "  | ") + rule.toString() + "\n";
                first = false;
            }
        }
        return s;
    }

    /**
     * Gets the initial non-terminal symbol.
     * @return the initial non-temrinal symbol.
     */
    public NonTerminal<T> getInitial() {
        return initial;
    }

    /**
     * Gets the set of all terminals in this grammar.
     * This set never shrinks, even if all production rules mentioning a non-terminal are removed.
     * @return The set of all terminals used in this grammar.
     */
    public Set<T> getTerminals() {
        return terminals;
    }

    /**
     * Gets all production items used in this grammar,
     * that is, all terminals and non-terminals.
     * @return
     */
    public Set<ProductionItem<T>> getProductionItems() {
        Set<ProductionItem<T>> pi = new HashSet<>();
        terminals.forEach(x -> pi.add(new Terminal<>(x)));
        pi.addAll(nonTerminals);
        return pi;
    }

    /**
     * Gets the initial production rule, for an extended grammar.
     * An extended grammar has exactly one production rule
     * @return the initial production rule.
     * @throws IllegalArgumentException if the grammar is not extended.
     */
    public ProductionRule<T> getInitialProductionRule() {
        if (!isExtended())
            throw new IllegalArgumentException("Grammar must be extended!");
        return productionRules.get(initial).stream().findAny().orElseThrow();
    }
}
