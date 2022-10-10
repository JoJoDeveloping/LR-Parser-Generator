package jojomodding.parsergenerator.parsed;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.Terminal;

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
        return "(" + this.element.name() + ":=" + this.children.stream().map(Objects::toString).collect(Collectors.joining(" ")) + ")";
    }
}
