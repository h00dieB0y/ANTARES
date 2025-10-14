package student.imt.antares.examples;

import student.imt.antares.colony.*;
import student.imt.antares.construction.*;
import student.imt.antares.pheromone.*;
import student.imt.antares.problem.*;
import student.imt.antares.solver.*;
import student.imt.antares.construction.VariableSelectors;

public class TSPTest {
    public static void main(String[] args) {
        System.out.println("=== TSP Solver with ACO ===");
        double[][] distances = TSPProblem.createSampleInstance();
        Problem problem = TSPProblem.create(distances);

        Assignment solution = solveWithACO(problem, 1000);

        printResult(problem, solution, distances);
    }

    private static Assignment solveWithACO(Problem problem, int maxCycles) {
        ACOParameters params = new ACOParameters(
            2.0,   // alpha
            2.0,   // beta (ajuste selon heuristique)
            0.1,   // rho
            0.01,  // tauMin
            10.0,  // tauMax
            20     // numberOfAnts
        );
        Colony colony = Colony.create(problem, params);
        AssignmentConstructor constructor = new AssignmentConstructor();
        ProbabilisticSelection valueSelector = new ProbabilisticSelection(1234);
        MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
        BasicCSPSolver solver = new BasicCSPSolver(problem);

        System.out.println("Solving...");
        Assignment solution = colony.solve(problem, constructor, VariableSelectors.RANDOM,
            valueSelector, pheromoneUpdater, solver, maxCycles);
        return solution;
    }

    private static void printResult(Problem problem, Assignment solution, double[][] distances) {
        System.out.println("Valid: " + problem.isSolution(solution));
        if (solution.isComplete(problem.size())) {
            TSPProblem.printTour(solution);
            double len = TSPProblem.tourLength(distances, solution);
            System.out.println("Tour length: " + len);
        } else {
            System.out.println("No complete solution found!");
        }
    }
}
