package jojomodding.parsergenerator.parsed;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jojomodding.parsergenerator.converter.ProductionRuleItem;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.utils.Utils;

/**
 * An abstract syntax tree inner node, representing an expanded production rule.
 */
public class AbstractSyntaxTree<T> implements AbstractSyntax<T>{

    /**
     * The grammar this was generated from.
     */
    private final Grammar<T> grammar;

    /**
     * The nonterminal / production rule LHS represented.
     */
    private final NonTerminal<T> element;

    /**
     * The production rule RHS that was expanded.
     */
    private final ProductionRule<T> generated;

    /**
     * The children the production rule expanded into.
     */
    private final List<AbstractSyntax<T>> children;

    /**
     * Construct a new AST node, for a production rule X -> aBc
     * @param grammar the grammar
     * @param element the production rule LHS, i.e. X
     * @param generated the production rule RHS, i.e. aBc
     * @param children the children, i.e. what aBc expanded into.
     * @throws IllegalArgumentException if the AST node is not well-formed according to the grammar.
     */
    public AbstractSyntaxTree(Grammar<T> grammar, NonTerminal<T> element, ProductionRule<T> generated, List<AbstractSyntax<T>> children) {
        this.grammar = grammar;
        this.element = element;
        this.generated = generated;
        this.children = children;
        if (!grammar.hasProductionRule(element, generated))
            throw new IllegalArgumentException("AST for nonexistent production rule!");
        Iterator<AbstractSyntax<T>> childrenIter = children.iterator();
        for (var item : generated.items()) {
            if (!childrenIter.hasNext())
                throw new IllegalArgumentException("Production rule requires child for " + item + ", but there is no more child!");
            var childAST = childrenIter.next();
            if (item instanceof NonTerminal<T> nt) {
                if (childAST instanceof AbstractSyntaxTree<T> ast) {
                    if (!ast.element.equals(nt)) {
                        throw new IllegalArgumentException(
                                "Production rule requires AST child for " + nt + ", but child has type " + ast.element + "!");
                    }
                } else throw new IllegalArgumentException("Production rule requires AST child for " + nt + ", but child is no AST!");
            } else if (item instanceof Terminal<T> t) {
                if (childAST instanceof AbstractSyntaxToken<T> token) {
                    if (!token.token().equals(t.terminal())) {
                        throw new IllegalArgumentException("Production rule requires token " + t + ", but got " + token + "!");
                    }
                } else throw new IllegalArgumentException("Production rule requires token for " + t + ", but child is no token!");
            }
        }
        if (childrenIter.hasNext())
            throw new IllegalArgumentException("There are more children/token than required!");
    }

    @Override
    public String toString() {
        return generated.formatter().apply(element, children.stream().map(Object::toString).collect(Collectors.toList()));
    }

    public void printRun(LinkedList<T> input, String statePrefix) {
        System.out.println("Input: " + Utils.formatWord(input, Objects::toString));
        var state = new ProductionRuleItem<T>(this.element, ProductionRule.empty(), this.generated, List.of());
        System.out.println("State: " + statePrefix + state.formatWihtoutLookahead());
        var subIter = this.children.iterator();
        while (!state.isReduce()) {
            var first = state.firstAfterDot().get();
            var child = subIter.next();
            if (first instanceof NonTerminal<T> nt) {
                AbstractSyntaxTree<T> childTree = (AbstractSyntaxTree<T>) child;
                System.out.println("Expand " + nt.name() + " to " + new ProductionRuleItem<T>(childTree.element, ProductionRule.empty(), childTree.generated, List.of()).formatWihtoutLookahead());
                System.out.println();
                childTree.printRun(input, statePrefix + state.formatWihtoutLookahead() + " ");
            } else if (first instanceof Terminal<T> t) {
                System.out.println("Shift " + t.format());
                input.removeFirst();
            }
            System.out.println();
            state = state.advanceOne();
            System.out.println("Input: " + Utils.formatWord(input, Objects::toString));
            System.out.println("State: " + statePrefix + state.formatWihtoutLookahead());
        }
        System.out.println("Reduce " + state.formatWihtoutLookahead());
    }

    public void printRMD(LinkedList<T> input, LinkedList<ProductionItem<T>> prefix, String statePrefix) {
        var state = new ProductionRuleItem<T>(this.element, ProductionRule.empty(), this.generated, List.of());
        var subIter = this.children.iterator();
        while (!state.isReduce()) {
            var first = state.firstAfterDot().get();
            var child = subIter.next();
            if (first instanceof NonTerminal<T> nt) {
                AbstractSyntaxTree<T> childTree = (AbstractSyntaxTree<T>) child;
                childTree.printRMD(input, prefix, statePrefix + state.formatWihtoutLookahead() + " ");
            } else if (first instanceof Terminal<T> t) {
                System.out.println("Input: " + Utils.formatWord(input, Objects::toString));
                System.out.println("Prefix: " + Utils.formatWord(prefix, ProductionItem::format));
                System.out.println("State: " + statePrefix + state.formatWihtoutLookahead());
                System.out.println("Shift " + t.format());
                System.out.println();
                input.removeFirst();
                prefix.addLast(first);
            }
            state = state.advanceOne();
        }
        System.out.println("Input: " + Utils.formatWord(input, Objects::toString));
        System.out.println("Prefix: " + Utils.formatWord(prefix, ProductionItem::format));
        System.out.println("State: " + statePrefix + state.formatWihtoutLookahead());
        System.out.println("Reduce " + state.formatWihtoutLookahead());
        System.out.println();
        for(var x : this.generated.items()) {
            prefix.removeLast();
        }
        prefix.addLast(this.element);
    }
}
