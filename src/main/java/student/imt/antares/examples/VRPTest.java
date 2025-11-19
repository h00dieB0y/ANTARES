package student.imt.antares.examples;

import student.imt.antares.colony.*;
import student.imt.antares.construction.*;
import student.imt.antares.pheromone.*;
import student.imt.antares.problem.*;
import student.imt.antares.solver.*;

public class VRPTest {
    public static void main(String[] args) {
        double[][] dist = VRPProblem.sampleDistances();
        int[] demands = VRPProblem.sampleDemands();
        int[] caps = VRPProblem.sampleCaps();
        Problem problem = VRPProblem.create(dist, demands, caps);
        Assignment solution = solveWithACO(problem, 1000);
        printResult(problem, solution, dist, caps.length);
    }

    private static Assignment solveWithACO(Problem problem, int maxCycles) {
        ACOParameters params = new ACOParameters(
            2.0, 2.0, 0.1, 0.01, 10.0, 20
        );
        Colony colony = Colony.create(problem, params);
        AssignmentConstructor constructor = new AssignmentConstructor();
        ProbabilisticSelection valueSelector = new ProbabilisticSelection(1234);
        MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
        BasicCSPSolver solver = new BasicCSPSolver(problem);

        return colony.solve(problem, constructor, VariableSelectors.RANDOM,
            valueSelector, pheromoneUpdater, solver, maxCycles);
    }

    private static void printResult(Problem problem, Assignment solution, double[][] dist, int nVehicles) {
    }
}
