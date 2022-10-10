package jojomodding.parsergenerator.pda.action;

import jojomodding.parsergenerator.grammar.NonTerminal;
import jojomodding.parsergenerator.grammar.ProductionRule;

/**
 * Describes the PDA action of reducing.
 * Reductions are always along a production rule like X -> aBc
 * @param from the production rule LHS, i.e. X in the above example
 * @param to the production rule RHS, i.e. aBc in the above example
 * @param <T> the type of strings the grammar is over
 */
public record ActionReduce<T>(NonTerminal<T> from, ProductionRule<T> to) implements Action<T> {

}
