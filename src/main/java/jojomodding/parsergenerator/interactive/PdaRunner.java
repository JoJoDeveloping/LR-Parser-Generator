package jojomodding.parsergenerator.interactive;

import static jojomodding.parsergenerator.Main.anbn;
import static jojomodding.parsergenerator.Main.ex22;
import static jojomodding.parsergenerator.Main.ex33;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import jojomodding.parsergenerator.converter.ParserGenerator;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.parsed.AbstractSyntaxTree;
import jojomodding.parsergenerator.utils.Utils;

public class PdaRunner {

    public static void main(String[] args) {
//        example(ex22(), "cdeecddcaaccd", 1);
//        example(anbn(), "aaabbb", 0);
        example(ex33(), "abbbbbccd", -1, 0, true);
    }
//

    public static void example(Grammar<Character> g, String s, int lak, int lrn, boolean rightmost) {
        var pda = new InteractiveNondeterministicPDA<>(g);
        List<Character> input = s.chars().mapToObj(x-> (char) x).toList();
//        pda.run(input);
//        new Scanner(System.in).nextLine();
        System.out.println();
        var pg = new ParserGenerator<>(g, lrn, lak).build();
        AbstractSyntaxTree<Character> ast = (AbstractSyntaxTree<Character>) pg.run(input);
        System.out.println();
        if (rightmost) {
            var input2 = new LinkedList<>(input);
            var prefix = new LinkedList<ProductionItem<Character>>();
            ast.printRMD(input2, prefix, "");
            System.out.println("Input: " + Utils.formatWord(input2, Objects::toString));
            System.out.println("Prefix: " + Utils.formatWord(prefix, ProductionItem::format));
            System.out.println("Accept");
        } else {
            ast.printRun(new LinkedList<>(input), "");
        }
    }

}


