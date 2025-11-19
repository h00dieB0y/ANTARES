package student.imt.antares.examples;

import org.chocosolver.parser.SetUpException;
import org.chocosolver.parser.xcsp.XCSP;
import org.chocosolver.solver.search.strategy.selectors.variables.AntiFirstFail;
import org.chocosolver.solver.variables.IntVar;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.solver.AntColonyMetaHeuristicSolver;

public class XCSPTest {
    public static void main(String[] args) throws SetUpException {
        String path = "/Users/manneemilekitsoukou/Downloads/GraphColoring/GraphColoring-m1-fixed/GraphColoring-qwhdec-o18-h120-1.xml";
        var defaultAcoParams = ACOParameters.withDefaults();
        double alpha = defaultAcoParams.alpha();
        double beta = defaultAcoParams.beta();
        double rho = defaultAcoParams.rho();
        double tauMin = defaultAcoParams.tauMin();
        double tauMax = defaultAcoParams.tauMax();
        int numberOfAnts = defaultAcoParams.numberOfAnts();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-path":
                    if (i + 1 < args.length) path = args[++i];
                    break;
                case "-alpha":
                    if (i + 1 < args.length) alpha = Double.parseDouble(args[++i]);
                    break;
                case "-beta":
                    if (i + 1 < args.length) beta = Double.parseDouble(args[++i]);
                    break;
                case "-rho":
                    if (i + 1 < args.length) rho = Double.parseDouble(args[++i]);
                    break;
                case "-tauMin":
                    if (i + 1 < args.length) tauMin = Double.parseDouble(args[++i]);
                    break;
                case "-tauMax":
                    if (i + 1 < args.length) tauMax = Double.parseDouble(args[++i]);
                    break;
                case "-ants":
                    if (i + 1 < args.length) numberOfAnts = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.out.println("Unknown argument: " + args[i]);
            }
        }

        XCSP xcspParser = new XCSP();
        if (xcspParser.setUp(path)) {
            xcspParser.createSolver();
            xcspParser.buildModel();
            xcspParser.configureSearch();

            var acoParams = new ACOParameters(
                    alpha,
                    beta,
                    rho,
                    tauMin,
                    tauMax,
                    numberOfAnts
            );

            var model = xcspParser.getModel();
            IntVar[] allVars = model.retrieveIntVars(true);
            var acoStrategy = new AntColonyMetaHeuristicSolver(
                    allVars,
                    new AntiFirstFail(model),
                    acoParams);

            var solver = model.getSolver();
            solver.limitTime("30s");
            solver.setSearch(acoStrategy);

            xcspParser.solve();
        }
    }
}
