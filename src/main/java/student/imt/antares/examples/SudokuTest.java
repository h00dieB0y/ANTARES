package student.imt.antares.examples;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.Colony;
import student.imt.antares.construction.AssignmentConstructor;
import student.imt.antares.construction.ProbabilisticSelection;
import student.imt.antares.construction.VariableSelectors;
import student.imt.antares.pheromone.MaxMinUpdate;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.solver.BasicCSPSolver;

public class SudokuTest {

    public static void main(String[] args) {
        testEasySudoku();
        testMediumSudoku();
        testHardSudoku();
    }

    private static void testEasySudoku() {
        int[][] puzzle = SudokuProblem.createEasyPuzzle();
        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithACO(problem, 500);
        printResult(problem, solution);
    }

    private static void testMediumSudoku() {
        int[][] puzzle = SudokuProblem.createMediumPuzzle();
        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithACO(problem, 1000);
        printResult(problem, solution);
    }

    private static void testHardSudoku() {
        int[][] puzzle = SudokuProblem.createHardPuzzle();
        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithACO(problem, 2000);
        printResult(problem, solution);
    }

    private static Assignment solveWithACO(Problem problem, int maxCycles) {
        ACOParameters params = new ACOParameters(
            2.0,
            0,
            0.01,
            0.01,
            10.0,
            30
        );

        Colony colony = Colony.create(problem, params);

        AssignmentConstructor constructor = new AssignmentConstructor();
        ProbabilisticSelection valueSelector = new ProbabilisticSelection(42);
        MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
        BasicCSPSolver solver = new BasicCSPSolver(problem);

        Assignment solution = colony.solve(problem, constructor, VariableSelectors.SMALLEST_DOMAIN_FIRST,
                                          valueSelector, pheromoneUpdater, solver, maxCycles);

        return solution;
    }

    private static void printResult(Problem problem, Assignment solution) {
    }
}
