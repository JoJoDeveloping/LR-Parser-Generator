package jojomodding.parsergenerator.interactive;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import jojomodding.parsergenerator.converter.ProductionRuleItem;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.utils.Utils;

public class InteractiveNondeterministicPDA<T> {

    private Grammar<T> grammar;

    private final Scanner sc = new Scanner(System.in);

    public InteractiveNondeterministicPDA(Grammar<T> grammar) {
        grammar.extend();
        this.grammar = grammar;
    }

    public boolean run(List<T> input) {
        LinkedList<T> inp = new LinkedList<>(input);
        LinkedList<ProductionRuleItem<T>> stack = new LinkedList<>();
        stack.push(new ProductionRuleItem<>(grammar.getInitial(), ProductionRule.empty(), grammar.getInitialProductionRule(), List.of()));
        while (!stack.isEmpty()) {
            System.out.println("Stack: " + stack.stream().map(ProductionRuleItem::formatWihtoutLookahead).collect(Collectors.joining(" ")));
            System.out.println("Input: " + Utils.formatWord(inp, Object::toString, true));
            var top = stack.getLast();
            var first = top.firstAfterDot();
            if (first.isEmpty()) {
                readOption(List.of("Reduce " + top.formatWihtoutLookahead()));
                stack.removeLast();
                if (!stack.isEmpty()) {
                    var k = stack.removeLast();
                    stack.addLast(k.advanceOne());
                }
            } else {
                var ffirst = first.get();
                if (ffirst instanceof Terminal<T> t) {
                    if (inp.isEmpty() || !inp.removeFirst().equals(t.terminal())) {
                        System.err.println("Want to shift " + t.format() + " but can not!");
                        return false;
                    }
                    readOption(List.of("Shift " + t.format()));
                    stack.removeLast();
                    stack.addLast(top.advanceOne());
                } else if (ffirst instanceof NonTerminal<T> nt) {
                    var v1 = new ArrayList<>(grammar.getProductionRules().getOrDefault(nt, Set.of()));
                    var v = v1.stream().map(x -> new ProductionRuleItem<T>(nt, ProductionRule.empty(), x, List.of())).collect(Collectors.toList());
                    if (v.isEmpty()) {
                        System.err.println("We can not expand " + nt.name() + ", there are no production rules!");
                        return false;
                    } else {
                        int i = readOption(v.stream().map(x -> "Expand " + nt.name() + " to " + x.formatWihtoutLookahead()).collect(Collectors.toList()));
                        stack.addLast(v.get(i));
                    }
                }
            }
        }
        if (inp.isEmpty()) {
            readOption(List.of("Accept, as the input is read completely"));
            System.out.println("Accepted!");
            return true;
        } else {
            System.err.println("Input remaining after reducing initial item!");
            return false;
        }
    }

    private int readOption(List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            if (i != 0) System.out.print(";   ");
            System.out.print((i+1) + ": " + options.get(i));
        }
        System.out.println();
        if (options.size() == 1) {
            sc.nextLine();
            return 0;
        }
        while (true) {
            var v = sc.nextLine();
            if (v.isBlank() && options.size() == 1) {
                return 0;
            }
            int i;
            try {
                i = Integer.parseInt(v.trim());
                if (i <= 0 || i > options.size()) {
                    throw new NumberFormatException();
                }
                return i-1;
            } catch (NumberFormatException e) {
                System.err.println("Invalid option " + v);
            }
        }
    }

}
