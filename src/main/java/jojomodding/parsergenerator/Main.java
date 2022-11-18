package jojomodding.parsergenerator;

import static jojomodding.parsergenerator.grammar.NonTerminal.n;
import static jojomodding.parsergenerator.grammar.ProductionRule.of;
import static jojomodding.parsergenerator.grammar.Terminal.t;

import java.util.List;
import java.util.Map;
import jojomodding.parsergenerator.converter.ParserGenerator;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.ProductionRule;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.pda.PushDownAutomaton;
import jojomodding.parsergenerator.pda.action.Action;
import jojomodding.parsergenerator.pda.action.ActionAccept;
import jojomodding.parsergenerator.pda.action.ActionErr;
import jojomodding.parsergenerator.pda.action.ActionReduce;
import jojomodding.parsergenerator.pda.action.ActionShift;

public class Main {

    public static void main(String[] args) {
//        var pg = new ParserGenerator<>(anbn(), 0).build();
//        System.out.println(pg.run("aabb".chars().mapToObj(x -> (char) x).toList()));
//        pg = new ParserGenerator<>(TE(), 1).build();
//        System.out.println(pg.run("0*0+0*0+0+0+(0+0)".chars().mapToObj(x -> (char) x).toList()));
//        pg = new ParserGenerator<>(LR2(), 2).build();
//        System.out.println(pg.run("abaaabc".chars().mapToObj(x -> (char) x).toList()));
//        pg = new ParserGenerator<>(notLALR(), 1).build();
//        System.out.println(pg.run("aec".chars().mapToObj(x -> (char) x).toList()));
//        var pg = new ParserGenerator<>(ex22(), 1).build();
//        System.out.println(pg.run("cdeecddcaaccd".chars().mapToObj(x -> (char) x).toList()));
//        var pg = new ParserGenerator<>(sameAB2(), 1).build();
//        System.out.println(pg.run("aabbbbaa".chars().mapToObj(x -> (char) x).toList()));
        var pglalr = new ParserGenerator<>(anbn(), 1, 1).build();
        var pg = new ParserGenerator<>(anbn(), 1, 0).build();
        var pgslr = new ParserGenerator<>(anbn(), 0, -1).build();
//        new ParserGenerator<>(anbn(), 0).build();
    }

    public static Grammar<Character> TE() {
        Grammar<Character> ETF = new Grammar<>(List.of("E", "T", "F"), "E");
        ETF.addProduction("E", new ProductionRule<>((x,y) -> y.get(0), n("T")));
        ETF.addProduction("E", new ProductionRule<>((x,y) -> "(" + y.get(0) + "+" + y.get(2) + ")", n("E"), t('+'), n("T")));
        ETF.addProduction("T", new ProductionRule<>((x,y) -> y.get(0), n("F")));
        ETF.addProduction("T", new ProductionRule<>((x,y) -> "(" + y.get(0) + "*" + y.get(2) + ")", n("T"), t('*'), n("F")));
        ETF.addProduction("F", new ProductionRule<>((x,y) -> y.get(1), t('('), n("E"), t(')')));
        ETF.addProduction("F", new ProductionRule<>((x,y) -> "0", t('0')));
        return ETF;
    }

    public static Grammar<Character> ex22() {
        Grammar<Character> g = new Grammar<Character>(
                List.of(n("S"), n("A"), n("B"), n("C"), n("D"), n("H"), n("K")),
                n("S")
        );
        g.addProduction("S", n("K"), n("A"));
        g.addProduction("S", n("B"), n("K"));

        g.addProduction("A", t('a'), t('b'), n("A"));
        g.addProduction("A", n("B"), t('c'), n("H"));
        g.addProduction("A");

        g.addProduction("B", t('e'), n("B"), t('d'));
        g.addProduction("B", t('c'));

        g.addProduction("C", t('d'), n("A"), t('b'));
        g.addProduction("C", t('a'), t('a'));

        g.addProduction("D", n("S"));
        g.addProduction("D");

        g.addProduction("H", n("C"), n("D"));
        g.addProduction("K", t('c'), t('d'));

        return g;
    }

    public static Grammar<Character> ex33() {
        Grammar<Character> g = new Grammar<Character>(
                List.of(n("S'"), n("S"), n("A"), n("B"), n("C")),
                n("S")
        );
        g.addProduction("S'", n("S"));

        g.addProduction("S", n("A"), n("B"));
        g.addProduction("S", n("A"));

        g.addProduction("A", t('a'), n("C"), t('c'));

        g.addProduction("B", t('c'), t('d'));

        g.addProduction("C", t('b'), t('b'), n("C"));
        g.addProduction("C", t('b'));

        return g;
    }

    public static Grammar<Character> notLALR() {
        /*
          S → a E c
            → a F d
            → b F c
            → b E d
          E → e
          F → e
         */
        Grammar<Character> c = new Grammar<>(List.of("S", "E", "F"), "S");
        c.addProduction("S", t('a'), n("E"), t('c'));
        c.addProduction("S", t('a'), n("F"), t('d'));
        c.addProduction("S", t('b'), n("F"), t('c'));
        c.addProduction("S", t('b'), n("E"), t('d'));
        c.addProduction("E", t('e'));
        c.addProduction("F", t('e'));
        return c;
    }

    public static Grammar<Character> anbn() {
        Grammar<Character> anbn = new Grammar<>(List.of("S"), "S");
        anbn.addProduction("S", of());
        anbn.addProduction("S", of(t('a'), n("S"), t('b')));
        return anbn;
    }

    public static Grammar<Character> anbn_lr0() {
        Grammar<Character> anbn = new Grammar<>(List.of("S", "A", "E"), "S");
        anbn.addProduction("S", of(n("A")));

        anbn.addProduction("A", of(t('a'), n("A"), t('b')));
        anbn.addProduction("A", of(t('a'), t('b')));

        anbn.addProduction("E", of());
        return anbn;
    }

    public static Grammar<Character> LR2() {
        Grammar<Character> anbn = new Grammar<>(List.of("S", "R", "T"), "S");
        anbn.addProduction("S", of(n("R"), n("S")));
        anbn.addProduction("S", of());
        anbn.addProduction("R", of(t('a'), t('b'), n("T")));
        anbn.addProduction("T", of(t('a'), n("T")));
        anbn.addProduction("T", of(t('c')));
        anbn.addProduction("T", of());
        return anbn;
    }

    public static Grammar<Character> sameAB1() {
        Grammar<Character> anbn = new Grammar<>(List.of("Q0", "Q1", "Q2"), "Q0");
        anbn.addProduction("Q0", t('a'), n("Q1"), n("Q0"));
        anbn.addProduction("Q0", t('b'), n("Q2"), n("Q0"));
        anbn.addProduction("Q0");
        anbn.addProduction("Q1", t('a'), n("Q1"), n("Q1"));
        anbn.addProduction("Q1", t('b'));
        anbn.addProduction("Q2", t('b'), n("Q2"), n("Q2"));
        anbn.addProduction("Q2", t('a'));
        return anbn;
    }

    public static Grammar<Character> sameAB2() {
        Grammar<Character> anbn = new Grammar<>(List.of("S"), "S");
        anbn.addProduction("S", t('a'), n("S"), t('b'), n("S"));
        anbn.addProduction("S", t('b'), n("S"), t('a'), n("S"));
        anbn.addProduction("S");
        return anbn;
    }

    public static void runPDA() {
        List<Map<List<Character>, Action<Character>>> actionTable = List.of(
                Map.of(List.of('a'), new ActionShift<>(),
                        List.of('b'), new ActionErr<>(),
                        List.of(), new ActionReduce<>(n("S"), of())),
                Map.of(List.of('a'), new ActionShift<>(),
                        List.of('b'), new ActionReduce<>(n("S"), of()),
                        List.of(), new ActionErr<>()),
                Map.of(List.of('a'), new ActionErr<>(),
                        List.of('b'), new ActionShift<>(),
                        List.of(), new ActionErr<>()),
                Map.of(List.of('a'), new ActionErr<>(),
                        List.of('b'), new ActionReduce<>(n("S"), of(t('a'), n("S"), t('b'))),
                        List.of(), new ActionReduce<>(n("S"), of(t('a'), n("S"), t('b')))),
                Map.of(List.of('a'), new ActionErr<>(),
                        List.of('b'), new ActionErr<>(),
                        List.of(), new ActionAccept<>()));
        List<Map<ProductionItem<Character>, Integer>> goToTable = List.of(
                Map.of(t('a'), 1, n("S"), 4),
                Map.of(t('a'), 1, n("S"), 2),
                Map.of(t('b'), 3),
                Map.of(),
                Map.of()
        );
        PushDownAutomaton<Character> pda = new PushDownAutomaton<>(anbn(), 0, actionTable, goToTable);
        var foo = pda.run(List.of('a', 'a', 'b', 'b'));
        System.out.println(foo);
    }

}
