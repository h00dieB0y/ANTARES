package student.imt.antares.examples;

import student.imt.antares.colony.*;
import student.imt.antares.construction.*;
import student.imt.antares.pheromone.*;
import student.imt.antares.problem.*;
import student.imt.antares.solver.*;
import student.imt.antares.construction.VariableSelectors;

public class TSPTest {
    public static void main(String[] args) {
        double[][] distances = TSPProblem.createSampleInstance();
        Problem problem = TSPProblem.create(distances);
        Assignment solution = solveWithACO(problem, 1000);
        printResult(problem, solution, distances);
    }

    private static Assignment solveWithACO(Problem problem, int maxCycles) {
        ACOParameters params = new ACOParameters(
            2.0,
            2.0,
            0.1,
            0.01,
            10.0,
            20
        );
        Colony colony = Colony.create(problem, params);
        AssignmentConstructor constructor = new AssignmentConstructor();
        ProbabilisticSelection valueSelector = new ProbabilisticSelection(1234);
        MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
        BasicCSPSolver solver = new BasicCSPSolver(problem);

        Assignment solution = colony.solve(problem, constructor, VariableSelectors.RANDOM,
            valueSelector, pheromoneUpdater, solver, maxCycles);
        return solution;
    }

    private static void printResult(Problem problem, Assignment solution, double[][] distances) {
    }
}
