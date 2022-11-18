package jojomodding.parsergenerator.converter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.pda.PushDownAutomaton;
import jojomodding.parsergenerator.pda.action.Action;
import jojomodding.parsergenerator.pda.action.ActionAccept;
import jojomodding.parsergenerator.pda.action.ActionReduce;
import jojomodding.parsergenerator.pda.action.ActionShift;
import jojomodding.parsergenerator.utils.MutablePair;
import jojomodding.parsergenerator.utils.Utils;

/**
 * This class converts a grammar into an LR(n) grammar. Also supports LA(k)LR(n-k) and SLR(n)
 */
public class ParserGenerator<T> {

    /**
     * This parser generator is for an LR(lrn) grammar, or an LA(lak)LR(lrn-lak) grammar.
     */
    private final int lrn_maybezero;
    private final int lrn;
    /**
     * This parser generator is for an LR(lrn) grammar, or an LA(lak)LR(lrn-lak) grammar.
     * If lr
     */
    private final int lak;
    /**
     * The grammar to convert. This grammar is always reduced and extended.
     */
    private final Grammar<T> grammar;
    /**
     * The First_n() set, for each grammar. Note that if n==0, we pretend n==1. Before use, call computeFirstFollow()
     */
    private final Map<NonTerminal<T>, Set<List<T>>> first = new HashMap<>();
    /**
     * The First_n() set, for each grammar. Note that if n==0, we pretend n==1. Before use, call computeFirstFollow()
     */
    private final Map<NonTerminal<T>, Set<List<T>>> follow = new HashMap<>();

    /**
     * Constructs a new parser generator.
     *
     * If lak == -1, then lrn must be 0, and we will construct the SLR(0) parser.
     * If lak == lrn == n, then we construct the LR(n) parser
     * If lak == 0, lrn == n, then we construct the LALR(n) parser.
     * In general, for lak <= lrn, we construct the LA(lak)LR(lrn-lak) parser.
     *
     * @param grammar the grammar for which a PDA is to be generated.
     * @param lrn       The lookahead size.
     */
    public ParserGenerator(Grammar<T> grammar, int lrn, int lak) {
        grammar.reduce();
        grammar.extend();
        this.grammar = grammar;
        this.lrn_maybezero = lrn;
        this.lrn = Integer.max(1, lrn);
        this.lak = lak;
        if ((lak == -1 && lrn != 0) || !(lak <= lrn && lak >= -1)) {
            throw new IllegalArgumentException("Invalid LA LR combination!");
        }
        computeFirstFollow();
    }

    private String kind() {
        if (lak == lrn_maybezero) {
            return "LR(" + lrn_maybezero + ")";
        } else if (lak == -1) {
            return "SLR(" + lrn_maybezero + ")";
        } else if (lak == 0) {
            return "LALR(" + lrn_maybezero + ")";
        } else {
            return "LA(" + lak + ")LR(" + (lrn_maybezero - lak) + ")";
        }
    }

    /**
     * Builds the PDA, which has lookahead n, or 1 if n==0. It does so by constructing the LR(n) automaton, or a variation thereof.
     *
     * @return The PDA for this grammar.
     * @throws IllegalArgumentException If the grammar is not of correct kind.
     */
    public PushDownAutomaton<T> build() {
        var transitions = buildDFA();
        Map<CharacteristicState<T>, Integer> labeling = new HashMap<>();
        CharacteristicState<T> initial = null, error = new CharacteristicState<>(lak);
        labeling.put(error, -1);
        for (var k : transitions.keySet()) {
            if (k.getAll().stream().anyMatch(x -> x.from().equals(grammar.getInitial()) && x.before().items().isEmpty())) {
                initial = k;
            }
        }
        labeling.put(initial, 0);
        int i = 1;
        for (var k : transitions.keySet()) {
            if (!labeling.containsKey(k)) {
                labeling.put(k, i);
                i++;
            }
        }
        var errorID = labeling.get(error);
        System.out.println("Initial: " + labeling.get(initial));
        List<Entry<CharacteristicState<T>, Integer>> toSort = new ArrayList<>(labeling.entrySet());
        toSort.sort(Comparator.comparingInt(Entry::getValue));
        boolean hasConflicts = false;
        System.out.println("GoTo table:");
        for (Entry<CharacteristicState<T>, Integer> e : toSort) {
            System.out.println("  State: " + e.getValue() + " " + e.getKey());
            for (var to : transitions.get(e.getKey()).entrySet()) {
                var target = labeling.get(to.getValue());
                if (target.equals(errorID)) {
                    continue;
                }
                System.out.println("    under " + to.getKey() + " -> " + target);
            }
            if (!isStateAdequate(e.getKey(), "" + e.getValue())) {
                hasConflicts = true;
            }
        }
        System.out.println("Action table:");
        List<Map<List<T>, Action<T>>> actionTable = new ArrayList<>(i);
        List<Map<ProductionItem<T>, Integer>> gotoTable = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            actionTable.add(new HashMap<>());
            gotoTable.add(new HashMap<>());
        }
        for (Entry<CharacteristicState<T>, Integer> e : toSort) {
            System.out.println("  State: " + e.getValue() + " " + e.getKey());
            if (e.getKey().getAll().isEmpty()) {
                continue;
            }
            Map<ProductionItem<T>, Integer> gotoEntry = gotoTable.get(e.getValue());
            Map<List<T>, Action<T>> actionEntry = actionTable.get(e.getValue());
            BiConsumer<List<T>, Action<T>> addActionEntry = (a,b) -> {
                System.out.println("    upon " + Utils.formatWord(a, Objects::toString, true) + " -> " + b.toString());
                actionEntry.put(a, b);
            };
            for (var to : transitions.get(e.getKey()).entrySet()) {
                if (to.getValue().getAll().isEmpty()) {
                    continue;
                }
                gotoEntry.put(to.getKey(), labeling.get(to.getValue()));
            }
            for (var state : e.getKey().getAll()) {
                if (state.firstAfterDot().isEmpty()) {
                    if (!state.from().equals(grammar.getInitial())) {
                        for (var la : lookaheadFor(state, false)) {
                            addActionEntry.accept(la, new ActionReduce<>(state.from(), state.before()));
                        }
                    } else if (state.from().equals(grammar.getInitial()) && state.lookahead().equals(List.of())) {
                        addActionEntry.accept(List.of(), new ActionAccept<>());
                    }
                } else if (state.isShift()) {
                    for (List<T> lookahead : lookaheadFor(state, false)) {
                        addActionEntry.accept(lookahead, new ActionShift<>());
                    }
                }
            }
        }
        if (hasConflicts) {
            throw new IllegalArgumentException("Grammar is not " + kind() + "!");
        } else {
            System.out.println("Grammar is " + kind() + "!");
        }
        return new PushDownAutomaton<>(grammar, lrn, actionTable, gotoTable);
    }

    /**
     * Given a PDA item X -> a . b | c, computes First(b) ++ c, capped at length n. In other words, compute all possible lookaheads under which this
     * rule is applicable. If lak == -1, we consider c = Follow(X)
     *
     * @param s                  The production item, i.e. X -> a . b | C
     * @param duringConstruction
     * @return The set of next possible parsed strings, up to length n.
     */
    private Set<List<T>> lookaheadFor(ProductionRuleItem<T> s, boolean duringConstruction) {
        if (duringConstruction && lrn_maybezero == 0) {
            return Set.of(List.of());
        }
        Set<List<T>> firsts = Set.of(List.of());
        for (var nt : s.after().items()) {
            firsts = appendFirsts(firsts, nt);
        }
        Stream<List<T>> k;
        if (lak == -1) {
            k = follow.get(s.from()).stream();
        } else if (lrn_maybezero == 0) {
            k = Stream.concat(Stream.of(List.of()), grammar.getTerminals().stream().map(List::of));
        } else {
            k = Stream.of(s.lookahead());
        }
        return appendAll(firsts, () -> k);
    }

    /**
     * This checks whether a state in the power-set automaton for the LR(n) automaton is adequate. A state is not adequate if it has a Shift-Shift- or
     * a Shift-Reduce-Conflict.
     *
     * @param state     The state
     * @param stateName The state name, used to print conflicts.
     * @return True iff there is no conflict, otherwise false.
     */
    private boolean isStateAdequate(CharacteristicState<T> state, String stateName) {
        List<ProductionRuleItem<T>> lst = new ArrayList<>(state.getAll());
        boolean adequate = true;
        for (int i = lst.size() - 1; i >= 0; i--) {
            var state1 = lst.get(i);
            for (int j = 0; j < lst.size(); j++) {
                var state2 = lst.get(j);
                if (i < j && state1.isReduce() && state2.isReduce() && !state1.equals(state2)) {
                    if (lookaheadFor(state1, false).stream().anyMatch(lookaheadFor(state2, false)::contains)) {
                        System.out.println("    Reduce-Reduce-Conflict in " + stateName + ": " + state1 + " vs " + state2);
                        adequate = false;
                    }
                }
                var state2FA = state2.firstAfterDot();
                if (state1.isReduce() && state2FA.isPresent() && state2FA.get() instanceof Terminal<T> t) {
                    if (lookaheadFor(state1, false).stream().anyMatch(lookaheadFor(state2, false)::contains)) {
                        System.out.println("    Shift-Reduce-Conflict in " + stateName + ": " + state2 + " vs " + state1);
                        adequate = false;
                    }
                }
            }
        }
        return adequate;
    }

    /**
     * Computes the epsilon-closure of a state in the LR(n) automaton, in order to quickly constructs its power-set automaton.
     *
     * @param items the state, without epsilon transitions
     * @return The epsilon-closure of items.
     */
    private CharacteristicState<T> closure(Set<ProductionRuleItem<T>> items) {
        var res = new CharacteristicState<T>(lak);
        items.forEach(res::addCompacting);
        while (true) {
            boolean change = false;
            for (var item : Set.copyOf(res.getAll())) {
                if (item.firstAfterDot().isEmpty()) {
                    continue;
                }
                if (item.firstAfterDot().get() instanceof NonTerminal<T> nt) {
                    Set<List<T>> followed = lookaheadFor(item.advanceOne(), true);
                    for (var prod : grammar.getProductionRules().get(nt)) {
                        for (var lookahead : followed) {
                            change |= res.addCompacting(new ProductionRuleItem<>(nt, new ProductionRule<>(List.of(), prod.formatter()), prod, lookahead));
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
     *
     * @return The LR(n) DFA.
     */
    private Map<CharacteristicState<T>, Map<ProductionItem<T>, CharacteristicState<T>>> buildDFA() {
        Map<CharacteristicState<T>, Map<ProductionItem<T>, CharacteristicState<T>>> result = new HashMap<>();
        Map<CharacteristicState<T>, CharacteristicState<T>> uniqueify = new HashMap<>();
        var startstate = closure(Set.of(new ProductionRuleItem<>(grammar.getInitial(), new ProductionRule<>(List.of(), grammar.getInitialProductionRule().formatter()), grammar.getInitialProductionRule(), List.of())));
        uniqueify.put(startstate, startstate);
        result.put(new CharacteristicState<>(lak), Map.of());
        outer:
        while (true) {
            for (var state : uniqueify.values()) {
                var map1 = result.get(state);
                if (map1 != null) continue;
                map1 = new HashMap<>();
                result.put(state, map1);
                Map<ProductionItem<T>, Set<ProductionRuleItem<T>>> byInitial = new HashMap<>();
                for (var item : state.getAll()) {
                    var head = item.firstAfterDot();
                    if (head.isEmpty()) continue;
                    byInitial.computeIfAbsent(head.get(), $ -> new HashSet<>()).add(item.advanceOne());
                }
                boolean change = false;
                for (var e : byInitial.entrySet()) {
                    var it = e.getKey();
                    var start = e.getValue();
                    var lfp = closure(start);
                    var uniq = uniqueify.get(lfp);
                    if (uniq == null) {
                        uniqueify.put(lfp, lfp);
                        map1.put(it, lfp);
                        change = true;
                    } else {
                        map1.put(it, uniq);
                        boolean uniqStale = false;
                        for (var x : lfp.getAll()) {
                            uniqStale |= uniq.addCompacting(x);
                        }
                        change |= uniqStale;
                        if (uniqStale) {
                            result.remove(uniq);
                        }
                    }
                }
                if(change)
                    continue outer;
            }
            break;
        }
        return result;
    }

    /**
     * Given a set of words W over U, and V over X, construct {k : w ++ v | w in W, v in V}. In other words, this is the set of all words in W,
     * concatenated to each word in V.
     *
     * @param w the set W
     * @param u the set V
     * @return the set of truncated concatenations.
     */
    private <U> Set<List<U>> appendAll(Set<List<U>> w, Supplier<Stream<List<U>>> u) {
        return w.stream().flatMap(x -> u.get().map(y -> Utils.concatLimit(lrn, x, y))).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Concatenates to each string in w each string in the First() set of item.
     *
     * @param w    the set w
     * @param item the item
     * @return a set, containing for each element of w and each string in First(item), the concatenation of these two, truncated at n.
     */
    private Set<List<T>> appendFirsts(Set<List<T>> w, ProductionItem<T> item) {
        if (item instanceof NonTerminal<T> nt) {
            return appendAll(w, () -> first.get(nt).stream());
        } else if (item instanceof Terminal<T> t) {
            return appendAll(w, () -> Stream.of(List.of(t.terminal())));
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
        follow.get(grammar.getInitial()).add(List.of());
        while (true) {
            boolean change = false;
            for (var T : grammar.getNonTerminals()) {
                for (var rule : grammar.getProductionRules().get(T)) {
                    Set<List<T>> partialFirst = Set.of(List.of());
                    List<MutablePair<NonTerminal<T>, Set<List<T>>>> partialFollows = new ArrayList<>();
                    for (var item : rule.items()) {
                        partialFirst = appendFirsts(partialFirst, item);
                        partialFollows.forEach(x -> x.setSecond(appendFirsts(x.getSecond(), item)));
                        if (item instanceof NonTerminal<T> nt) {
                            partialFollows.add(new MutablePair<>(nt, Set.of(List.of())));
                        }
                    }
                    partialFollows.forEach(w -> w.setSecond(
                            w.getSecond().stream().flatMap(x -> follow.get(T).stream().map(y -> Utils.concatLimit(lrn, x, y)))
                                    .collect(Collectors.toUnmodifiableSet())));
                    change |= first.get(T).addAll(partialFirst);
                    for (var e : partialFollows) {
                        change |= follow.get(e.getFirst()).addAll(e.getSecond());
                    }
                }
            }
            if (!change) {
                break;
            }
        }
    }

}
