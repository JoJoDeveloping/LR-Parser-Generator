package jojomodding.parsergenerator.converter;

import java.util.HashSet;
import java.util.Set;
import jojomodding.parsergenerator.utils.Utils;

/**
 * A state in the characteristic deterministic LR automaton.
 */
public class CharacteristicState<T> {

    /**
     * The items in this state
     */
    private final Set<ProductionRuleItem<T>> items;
    /**
     * The items in this state, where all lookaheads are at most length lak
     */
    private final Set<ProductionRuleItem<T>> itemsUpTo;

    /**
     * The cutoff at which lookahead becomes irrelevant. Thus, the states described by this class correspond to an LA(lak) automaton
     */
    private final int lak;

    public CharacteristicState(int lak) {
        this.lak = lak;
        this.items = new HashSet<>();
        this.itemsUpTo = new HashSet<>();
    }

    /**
     * Adds a production item to this state.
     * @param x the new production item
     * @return Whether it was here before
     */
    public boolean addCompacting(ProductionRuleItem<T> x) {
        if (lak == -1) {
            itemsUpTo.add(x);
        } else {
            ProductionRuleItem<T> compacted = new ProductionRuleItem<>(x.from(), x.before(), x.after(), Utils.limit(lak, x.lookahead()));
            itemsUpTo.add(compacted);
        }
        return items.add(x);
    }

    /**
     * Gets all production item in this state.
     * All means all, even those that are identical if there lookahead was shortened.
     * @return all production items in this state
     */
    public Set<ProductionRuleItem<T>> getAll() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CharacteristicState<?> that = (CharacteristicState<?>) o;

        if (lak != that.lak) {
            return false;
        }
        return itemsUpTo.equals(that.itemsUpTo);
    }

    @Override
    public int hashCode() {
        int result = itemsUpTo.hashCode();
        result = 31 * result + lak;
        return result;
    }

    @Override
    public String toString() {
        return getAll().toString();
    }
}
