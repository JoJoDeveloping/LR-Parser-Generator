package jojomodding.parsergenerator;

import static jojomodding.parsergenerator.grammar.NonTerminal.n;
import static jojomodding.parsergenerator.grammar.ProductionRule.of;
import static jojomodding.parsergenerator.grammar.Terminal.t;

import java.util.List;
import java.util.Map;
import jojomodding.parsergenerator.converter.ParserGenerator;
import jojomodding.parsergenerator.grammar.Grammar;
import jojomodding.parsergenerator.grammar.ProductionItem;
import jojomodding.parsergenerator.grammar.Terminal;
import jojomodding.parsergenerator.pda.PushDownAutomaton;
import jojomodding.parsergenerator.pda.action.Action;
import jojomodding.parsergenerator.pda.action.ActionAccept;
import jojomodding.parsergenerator.pda.action.ActionErr;
import jojomodding.parsergenerator.pda.action.ActionReduce;
import jojomodding.parsergenerator.pda.action.ActionShift;

public class Main {

    public static void main(String[] args) {
        var pg = new ParserGenerator<>(anbn(), 0).build();
        System.out.println(pg.run("aabb".chars().mapToObj(x -> (char) x).toList()));
        pg = new ParserGenerator<>(TE(), 1).build();
        System.out.println(pg.run("0*0+0*0+0+0+(0+0)".chars().mapToObj(x -> (char) x).toList()));
    }

    public static Grammar<Character> TE() {
        Grammar<Character> ETF = new Grammar<>(List.of("E", "T", "F"), "E");
        ETF.addProduction("E", n("T"));
        ETF.addProduction("E", n("E"), t('+'), n("T"));
        ETF.addProduction("T", n("F"));
        ETF.addProduction("T", n("T"), t('*'), n("F"));
        ETF.addProduction("F", t('('), n("E"), t(')'));
        ETF.addProduction("F", t('0'));
        return ETF;
    }

    public static Grammar<Character> anbn() {
        Grammar<Character> anbn = new Grammar<>(List.of("S"), "S");
        anbn.addProduction("S", of());
        anbn.addProduction("S", of(t('a'), n("S"), t('b')));
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
