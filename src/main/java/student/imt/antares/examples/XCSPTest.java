package student.imt.antares.examples;

import org.chocosolver.parser.SetUpException;
import org.chocosolver.parser.xcsp.XCSP;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.variables.AntiFirstFail;
import org.chocosolver.solver.variables.IntVar;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.solver.AntColonyMetaHeuristicSolver;

public class XCSPTest {
    public static void main(String[] args) throws SetUpException {
        XCSP xcspExample = new XCSP();
        xcspExample.setUp(new String[]{"C:\\Users\\anasa\\Downloads\\CarSequencing\\CarSequencing\\CarSequencing-m1-gagne\\CarSequencing-200-01.xml\\CarSequencing-200-01.xml", "-lvl","INFO"});
        xcspExample.createSolver();
        xcspExample.buildModel();
        xcspExample.configureSearch();
        Model model = xcspExample.getModel();
        // ACO parameters (same as standalone ANTARES for fair comparison)
        ACOParameters acoParams = new ACOParameters(
            2.0,   // alpha (pheromone importance)
            0.0,   // beta (heuristic importance, 0 = pure pheromone)
            0.01,  // rho (evaporation rate)
            0.01,  // tauMin
            1.0,  // tauMax
            10    // numberOfAnts per cycle
        );

        // Create ACO metaheuristic strategy
        IntVar[] allVars = model.retrieveIntVars(true);
        AntColonyMetaHeuristicSolver acoStrategy = new AntColonyMetaHeuristicSolver(
            allVars,
            new AntiFirstFail(model),
            acoParams
        );

        Solver solver = model.getSolver();
        solver.limitTime("30s");
        solver.setSearch(acoStrategy); 
        // declare ACO    
        xcspExample.solve();
    }
}
