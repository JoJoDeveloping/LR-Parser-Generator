package jojomodding.parsergenerator.interactive;

import static jojomodding.parsergenerator.Main.anbn;
import static jojomodding.parsergenerator.Main.ex22;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import jojomodding.parsergenerator.converter.ParserGenerator;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.parsed.AbstractSyntaxTree;

public class PdaRunner {

    public static void main(String[] args) {
//        example(ex22(), "cdeecddcaaccd", 1);
        example(anbn(), "aaabbb", 0);
    }
//

    public static void example(Grammar<Character> g, String s, int n) {
        var pda = new InteractiveNondeterministicPDA<>(g);
        List<Character> input = s.chars().mapToObj(x-> (char) x).toList();
        pda.run(input);
        new Scanner(System.in).nextLine();
        System.out.println();
        var pg = new ParserGenerator<>(g, n).build();
        AbstractSyntaxTree<Character> ast = (AbstractSyntaxTree<Character>) pg.run(input);
        System.out.println();
        ast.printRun(new LinkedList<>(input), "");
    }

}


