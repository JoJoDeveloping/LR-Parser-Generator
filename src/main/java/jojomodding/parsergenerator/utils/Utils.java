package jojomodding.parsergenerator.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Utils {

    /**
     * Generate a fresh name, from a given prefix.
     *
     * For example, if "a", "a0", and "a1" are already in use, generate a2.
     * @param prefix the prefix ("a" in the above example)
     * @param isTaken predicate whether the string is available
     * @return a new string that is available, starting with prefix, with a suffix made out of digits.
     */
    public static String freshName(String prefix, Predicate<String> isTaken) {
        String s = prefix;
        int i = 0;
        while (isTaken.test(s)) {
            s = prefix + i;
            i++;
        }
        return s;
    }

    /**
     * Concatenate two lists so that the result is not longer than length.
     * In other words, compute length : (first ++ second)
     * @param length the maximal length of the result
     * @param first the first component
     * @param last the second component
     * @return the longest prefix of (first ++ second) that has a length <= length
     * @param <T> the type of list entries
     */
    public static <T> List<T> concatLimit(int length, List<T> first, List<T> last) {
        ArrayList<T> res = new ArrayList<>(length);
        for (T t : first) {
            if (res.size() >= length) return res;
            res.add(t);
        }
        for (T t : last) {
            if (res.size() >= length) return res;
            res.add(t);
        }
        return res;
    }

}
