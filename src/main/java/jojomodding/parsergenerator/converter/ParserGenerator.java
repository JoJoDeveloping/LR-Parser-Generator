package jojomodding.parsergenerator.converter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jojomodding.parsergenerator.grammar.EndMarker;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.grammar.TerminalOrEnd;
import jojomodding.parsergenerator.pda.PushDownAutomaton;
import jojomodding.parsergenerator.pda.action.Action;
import jojomodding.parsergenerator.pda.action.ActionAccept;
import jojomodding.parsergenerator.pda.action.ActionReduce;
import jojomodding.parsergenerator.pda.action.ActionShift;
import jojomodding.parsergenerator.utils.Utils;

/**
 * This class converts a grammar into an LR(n) parser.
 */
public class ParserGenerator<T> {

    /**
     * The n. If n == 0, this value is 1 instead.
     */
    private final int n;
    /**
     * The actual n. May be 0.
     */
    private final int n_actual;
    /**
     * The grammar to convert. This grammar is always reduced and extended.
     */
    private final Grammar<T> grammar;
    /**
     * The First_n() set, for each grammar. Note that if n==0, we pretend n==1.
     * Before use, call computeFirstFollow()
     */
    private final Map<NonTerminal<T>, Set<List<T>>> first = new HashMap<>();
    /**
     * The First_n() set, for each grammar. Note that if n==0, we pretend n==1.
     * Before use, call computeFirstFollow()
     */
    private final Map<NonTerminal<T>, Set<List<TerminalOrEnd<T>>>> follow = new HashMap<>();

    /**
     * Constructs a new parser generator.
     * @param grammar the grammar for which a PDA is to be generated.
     * @param n The lookahead size.
     */
    public ParserGenerator(Grammar<T> grammar, int n) {
        grammar.reduce();
        grammar.extend();
        this.grammar = grammar;
        this.n_actual = n;
        this.n = Math.max(n, 1);
        computeFirstFollow();
    }

    /**
     * Builds the PDA, which has lookahead n, or 1 if n==0.
     * It does so by constructing the LR(n_actual) automaton.
     * @return The PDA for this grammar.
     * @throws IllegalArgumentException If the grammar is not LR(n_actual)
     */
    public PushDownAutomaton<T> build() {
        var transitions = buildDFA();
        Map<Set<ProductionRuleItem<T>>, Integer> labeling = new HashMap<>();
        Map<Integer, Set<ProductionRuleItem<T>>> reverseLabelling = new HashMap<>();
        Set<ProductionRuleItem<T>> initial = null, error = Set.of();
        labeling.put(error, -1);
        reverseLabelling.put(-1, error);
        for (var k : transitions.keySet()) {
            if (k.stream().anyMatch(x -> x.from().equals(grammar.getInitial()) && x.before().items().isEmpty())) {
                initial = k;
            }
        }
        labeling.put(initial, 0);
        reverseLabelling.put(0, initial);
        int i = 1;
        for (var k : transitions.keySet()) {
            if (!labeling.containsKey(k)) {
                labeling.put(k, i);
                reverseLabelling.put(i, k);
                i++;
            }
        }
        var errorID = labeling.get(error);
        System.out.println("Initial: " + labeling.get(initial));
        System.out.println("Error: " + labeling.get(error));
        List<Entry<Set<ProductionRuleItem<T>>, Integer>> toSort = new ArrayList<>(labeling.entrySet());
        toSort.sort(Comparator.comparingInt(Entry::getValue));
        boolean hasConflicts = false;
        for (Entry<Set<ProductionRuleItem<T>>, Integer> e : toSort) {
            System.out.println("State: " + e.getValue() + " " + e.getKey());
            for (var to : transitions.get(e.getKey()).entrySet()) {
                var target = labeling.get(to.getValue());
                if (target.equals(errorID)) {
                    continue;
                }
                System.out.println("  under " + to.getKey() + " -> " + target);
            }
            if (!isStateAdequate(e.getKey(), "" + e.getValue())) {
                hasConflicts = true;
            }
        }
        if (hasConflicts) {
            throw new IllegalArgumentException("Grammar is not LR(" + n_actual + ")!");
        } else {
            System.out.println("Grammar is LR(" + n_actual + ")");
        }
        List<Map<List<TerminalOrEnd<T>>, Action<T>>> actionTable = new ArrayList<>(i);
        List<Map<ProductionItem<T>, Integer>> gotoTable = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            actionTable.add(new HashMap<>());
            gotoTable.add(new HashMap<>());
        }
        for (Entry<Set<ProductionRuleItem<T>>, Integer> e : labeling.entrySet()) {
            if (e.getKey().isEmpty()) {
                continue;
            }
            Map<ProductionItem<T>, Integer> gotoEntry = gotoTable.get(e.getValue());
            Map<List<TerminalOrEnd<T>>, Action<T>> actionEntry = actionTable.get(e.getValue());
            for (var to : transitions.get(e.getKey()).entrySet()) {
                if (to.getValue().isEmpty()) {
                    continue;
                }
                gotoEntry.put(to.getKey(), labeling.get(to.getValue()));
            }
            for (var state : e.getKey()) {
                if (state.firstAfterDot().isEmpty()) {
                    if (!state.from().equals(grammar.getInitial())) {
                        if (n_actual == 0) {
                            for (var x : follow.get(state.from())) {
                                actionEntry.put(x, new ActionReduce<>(state.from(), state.before()));
                            }
                        } else {
                            actionEntry.put(state.lookahead(), new ActionReduce<>(state.from(), state.before()));
                        }
                    } else if (state.from().equals(grammar.getInitial()) && (n_actual == 0 || state.lookahead().equals(List.of(new EndMarker<>())))) {
                        actionEntry.put(List.of(new EndMarker<>()), new ActionAccept<>());
                    }
                } else if (state.firstAfterDot().get() instanceof Terminal<T> t) {
                    for (List<TerminalOrEnd<T>> lookahead : lookaheadFor(state)) {
                        actionEntry.put(lookahead, new ActionShift<>());
                    }
                }
            }
        }

        return new PushDownAutomaton<T>(grammar, n, actionTable, gotoTable);
    }

    /**
     * Given a PDA item X -> a . b | c, computes First(b) ++ c, capped at length n.
     * In other words, compute all possible lookaheads under which this rule is applicable.
     * If n==0, we consider that c = Follow(X)
     * @param s The production item, i.e. X -> a . b | C
     * @return The set of next possible parsed strings, up to length n.
     */
    private Set<List<TerminalOrEnd<T>>> lookaheadFor(ProductionRuleItem<T> s) {
        Set<List<TerminalOrEnd<T>>> firsts = Set.of(List.of());
        for (var nt : s.after().items()) {
            firsts = appendFirsts(firsts, Terminal<T>::new, nt);
        }
        return n_actual == 0 ? firsts : appendAll(firsts, Function.identity(), () -> Stream.of(s.lookahead()));
    }

    /**
     * This checks whether a state in the power-set automaton for the LR(n) automaton is adequate.
     * A state is not adequate if it has a Shift-Shift- or a Shift-Reduce-Conflict.
     * For n==0, we do not look at the lookahead, but instead consider the follow sets, like in @see lookaheadFor
     * @param state The state
     * @param stateName The state name, used to print conflicts.
     * @return True iff there is no conflict, otherwise false.
     */
    private boolean isStateAdequate(Set<ProductionRuleItem<T>> state, String stateName) {
        List<ProductionRuleItem<T>> lst = new ArrayList<>(state);
        boolean adequate = true;
        for (int i = lst.size() - 1; i >= 0; i--) {
            var state1 = lst.get(i);
            for (int j = 0; j < lst.size(); j++) {
                var state2 = lst.get(j);
                if (i < j && state1.isReduce() && state2.isReduce()
                        && !state1.equals(state2)) {
                    if (n_actual == 0 ? follow.get(state1.from()).stream().anyMatch(follow.get(state2.from())::contains)
                            : state1.lookahead().equals(state2.lookahead())) {
                        System.out.println("Reduce-Reduce-Conflict in " + stateName + ": " + state1 + " vs " + state2);
                        adequate = false;
                    }
                }
                var state2FA = state2.firstAfterDot();
                if (state1.isReduce() && state2FA.isPresent() && state2FA.get() instanceof Terminal<T> t) {
                    if (n_actual == 0 ? follow.get(state1.from()).contains(t)
                            : lookaheadFor(state2).contains(state1.lookahead())) {
                        System.out.println("Shift-Reduce-Conflict in " + stateName + ": " + state2 + " vs " + state1);
                        adequate = false;
                    }
                }
            }
        }
        return adequate;
    }

    /**
     * Computes the epsilon-closure of a state in the LR(n) automaton, in order to quickly constructs its power-set automaton.
     * @param items the state, without epsilon transitions
     * @return The epsilon-closure of items.
     */
    private Set<ProductionRuleItem<T>> closure(Set<ProductionRuleItem<T>> items) {
        var res = new HashSet<>(items);
        while (true) {
            boolean change = false;
            for (var item : Set.copyOf(res)) {
                if (item.firstAfterDot().isEmpty()) {
                    continue;
                }
                if (item.firstAfterDot().get() instanceof NonTerminal<T> nt) {
                    Set<List<TerminalOrEnd<T>>> followed = Set.of(List.of());
                    if (n_actual > 0) {
                        followed = lookaheadFor(item.advanceOne());
                    }
                    for (var prod : grammar.getProductionRules().get(nt)) {
                        for (var lookahead : followed) {
                            change |= res.add(new ProductionRuleItem<>(nt, new ProductionRule<>(List.of()), prod, lookahead));
                        }
                    }
                }
            }
            if (!change) {
                break;
            }
        }
        return res;
    }

    /**
     * Builds the LR(n) DFA.
     * @return The LR(n) DFA.
     */
    private Map<Set<ProductionRuleItem<T>>, Map<ProductionItem<T>, Set<ProductionRuleItem<T>>>> buildDFA() {
        Map<Set<ProductionRuleItem<T>>, Map<ProductionItem<T>, Set<ProductionRuleItem<T>>>> result = new HashMap<>();
        Set<Set<ProductionRuleItem<T>>> states = new HashSet<>();
        states.add(closure(Set.of(new ProductionRuleItem<>(grammar.getInitial(), new ProductionRule<>(List.of()), grammar.getInitialProductionRule(),
                n_actual > 0 ? List.of(new EndMarker<>()) : List.of()))));
        outer:
        while (true) {
            for (var state : states) {
                for (var it : grammar.getProductionItems()) {
                    var map1 = result.computeIfAbsent(state, k -> new HashMap<>());
                    if (map1.containsKey(it)) {
                        continue;
                    }
                    var start = new HashSet<ProductionRuleItem<T>>();
                    for (var item : state) {
                        var head = item.firstAfterDot();
                        if (head.isPresent() && head.get().equals(it)) {
                            start.add(item.advanceOne());
                        }
                    }
                    var lfp = closure(start);
                    map1.put(it, lfp);
                    if (states.add(lfp)) {
                        continue outer;
                    }
                }
            }
            break;
        }
        return result;
    }

    /**
     * Given a set of words W over U, and V over X, construct {k : w ++ inj@v | w in W, v in V}.
     * For instance, if inj is the identity, this is the set of all words in W, concatenated to each word in V.
     * Since the type of characters in the words in V might differ, the function inj is applied to each character.
     * @param w the set W
     * @param inj the injection X -> U
     * @param u the set V
     * @return the set of truncated concatenations.
     * @param <U> the type of characters of the strings in W
     * @param <X> the type of characters of the strings in V
     */
    private <U, X> Set<List<U>> appendAll(Set<List<U>> w, Function<X, U> inj, Supplier<Stream<List<X>>> u) {
        return w.stream().flatMap(x -> u.get().map(y -> Utils.concatLimit(n, x, y.stream().map(inj).toList())))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Concatenates to each string in w each string in the First() set of item.
     * @param w the set w
     * @param inj a function to translate characters from T to U
     * @param item the item
     * @return a set, containing for each element of w and each string in First(item), the concatenation of these two, truncated at n.
     * @param <U> the type of strings in w.
     */
    private <U> Set<List<U>> appendFirsts(Set<List<U>> w, Function<T, U> inj, ProductionItem<T> item) {
        if (item instanceof NonTerminal<T> nt) {
            return appendAll(w, inj, () -> first.get(nt).stream());
        } else if (item instanceof Terminal<T> t) {
            return appendAll(w, inj, () -> Stream.of(List.of(t.terminal())));
        } else {
            throw new RuntimeException();
        }
    }

    /**
     * Computes the First_n() and Follow_n() sets of each non-terminal.
     */
    public void computeFirstFollow() {
        for (var T : grammar.getNonTerminals()) {
            first.put(T, new HashSet<>());
            follow.put(T, new HashSet<>());
        }
        follow.get(grammar.getInitial()).add(List.of(new EndMarker<>()));
        while (true) {
            boolean change = false;
            for (var T : grammar.getNonTerminals()) {
                for (var rule : grammar.getProductionRules().get(T)) {
                    Set<List<T>> partialFirst = Set.of(List.of());
                    Map<NonTerminal<T>, Set<List<TerminalOrEnd<T>>>> partialFollows = new HashMap<>();
                    for (var item : rule.items()) {
                        partialFirst = appendFirsts(partialFirst, Function.identity(), item);
                        partialFollows.entrySet().forEach(x -> x.setValue(appendFirsts(x.getValue(), Terminal<T>::new, item)));
                        if (item instanceof NonTerminal<T> nt) {
                            partialFollows.computeIfAbsent(nt, $ -> new HashSet<>()).add(List.of());
                        }
                    }
                    partialFollows.entrySet().forEach(w -> w.setValue(w.getValue().stream()
                            .flatMap(x -> follow.get(T).stream().map(y -> Utils.concatLimit(n, x, y)))
                            .collect(Collectors.toUnmodifiableSet())));
                    change |= first.get(T).addAll(partialFirst);
                    for (var e : partialFollows.entrySet()) {
                        change |= follow.get(e.getKey()).addAll(e.getValue());
                    }
                }
            }
            if (!change) {
                break;
            }
        }
    }

}