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

/**
 * Test Sudoku solving with ACO.
 */
public class SudokuTest {

    public static void main(String[] args) {
        System.out.println("=== Sudoku Solver with ACO ===\n");

        testEasySudoku();
        testMediumSudoku();
        testHardSudoku();
    }

    private static void testEasySudoku() {
        System.out.println("--- Easy Sudoku ---");
        int[][] puzzle = SudokuProblem.createEasyPuzzle();

        System.out.println("Initial puzzle:");
        SudokuProblem.printGrid(puzzle);
        System.out.println();

        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithACO(problem, 500);

        printResult(problem, solution);
        System.out.println();
    }

    private static void testMediumSudoku() {
        System.out.println("--- Medium Sudoku ---");
        int[][] puzzle = SudokuProblem.createMediumPuzzle();

        System.out.println("Initial puzzle:");
        SudokuProblem.printGrid(puzzle);
        System.out.println();

        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithACO(problem, 1000);

        printResult(problem, solution);
        System.out.println();
    }

    private static void testHardSudoku() {
        System.out.println("--- Hard Sudoku ---");
        int[][] puzzle = SudokuProblem.createHardPuzzle();

        System.out.println("Initial puzzle:");
        SudokuProblem.printGrid(puzzle);
        System.out.println();

        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithACO(problem, 2000);

        printResult(problem, solution);
        System.out.println();
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

        System.out.println("Solving with " + maxCycles + " cycles, " + params.numberOfAnts() + " ants...");
        long startTime = System.currentTimeMillis();

        Assignment solution = colony.solve(problem, constructor, VariableSelectors.SMALLEST_DOMAIN_FIRST,
                                          valueSelector, pheromoneUpdater, solver, maxCycles);

        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms\n");

        return solution;
    }

    private static void printResult(Problem problem, Assignment solution) {
        System.out.println("Result:");
        System.out.println("  Variables: " + problem.size());
        System.out.println("  Assigned: " + solution.size());
        System.out.println("  Complete: " + solution.isComplete(problem.size()));
        System.out.println("  Valid: " + problem.isSolution(solution));

        if (solution.isComplete(problem.size())) {
            System.out.println("\nSolution:");
            int[][] grid = SudokuProblem.assignmentToGrid(solution);
            SudokuProblem.printGrid(grid);
        } else {
            System.out.println("  No complete solution found!");
        }
    }
}
