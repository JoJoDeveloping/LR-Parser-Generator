package jojomodding.parsergenerator.pda;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.parsed.AbstractSyntax;
import jojomodding.parsergenerator.parsed.AbstractSyntaxToken;
import jojomodding.parsergenerator.parsed.AbstractSyntaxTree;
import jojomodding.parsergenerator.pda.action.Action;
import jojomodding.parsergenerator.pda.action.ActionAccept;
import jojomodding.parsergenerator.pda.action.ActionErr;
import jojomodding.parsergenerator.pda.action.ActionReduce;
import jojomodding.parsergenerator.pda.action.ActionShift;
import jojomodding.parsergenerator.utils.Utils;

/**
 * A push down automaton, that can accept strings over T
 * @param <T> the type of strings over which to accept
 */
public class PushDownAutomaton<T> {

    /**
     * The given grammar.
     */
    private final Grammar<T> grammar;
    /**
     * How many chars of lookahead to use.
     */
    private final int lookahead;
    /**
     * The action table. Contains one action for each string of length lookahead.
     * Also must contain action for shorter strings, as long as they end with the EOF marker.
     */
    private final ArrayList<Map<List<T>, Action<T>>> actionTable;
    /**
     * The goto table, encoding the transition of the characteristic LR(n) automaton.
     */
    private final ArrayList<Map<ProductionItem<T>, Integer>> gotoTable;

    /**
     * Create a new PDA.
     * @param grammar the underlying grammar.
     * @param lookahead how much lookahead to use.
     * @param actionTable the action table.
     * @param gotoTable the goto table.
     */
    public PushDownAutomaton(Grammar<T> grammar, int lookahead, List<Map<List<T>, Action<T>>> actionTable,
            List<Map<ProductionItem<T>, Integer>> gotoTable) {
        this.grammar = grammar;
        this.lookahead = lookahead;
        this.actionTable = new ArrayList<>(actionTable);
        this.gotoTable = new ArrayList<>(gotoTable);
        if (lookahead <= 0 || actionTable.size() != gotoTable.size()) {
            throw new IllegalArgumentException("Malformed PDA");
        }
    }


    /**
     * Run the PDA on the given input.
     * @param input the input
     * @return the parsed result
     * @throws IllegalArgumentException if the input is not in the language
     */
    public AbstractSyntax<T> run(final List<T> input) {
        var inputList = new LinkedList<>(input);
        LinkedList<Integer> stack = new LinkedList<>();
        LinkedList<AbstractSyntax<T>> dataStack = new LinkedList<>();
        stack.push(0);
        while (true) {
            var lookahead = Utils.concatLimit(this.lookahead, inputList, List.of());
            int current = stack.getFirst();
            var nextAction = actionTable.get(current).get(lookahead);
            if (nextAction instanceof ActionShift<T>) {
                if (inputList.isEmpty()) {
                    throw new IllegalStateException("Can not shift on EOF!");
                }
                stack.push(gotoTable.get(current).get(new Terminal<>(inputList.peek())));
                dataStack.push(new AbstractSyntaxToken<>(inputList.peek()));
                inputList.pop();
            } else if (nextAction instanceof ActionReduce<T> red) {
                int nums = red.to().items().size();
                LinkedList<AbstractSyntax<T>> subSyntax = new LinkedList<>();
                for (int i = 0; i < nums; i++) {
                    stack.pop();
                    subSyntax.push(dataStack.pop());
                }
                current = stack.getFirst();
                stack.push(gotoTable.get(current).get(red.from()));
                dataStack.push(new AbstractSyntaxTree<>(grammar, red.from(), red.to(), subSyntax));
            } else if (nextAction instanceof ActionAccept<T>) {
                if (dataStack.size() != 1) {
                    throw new IllegalStateException("Unexpected end of input!");
                }
                return dataStack.pop();
            } else if (nextAction instanceof ActionErr<T> || nextAction == null) {
                throw new IllegalArgumentException();
            }
        }
    }
}
