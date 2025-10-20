package student.imt.antares.examples;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.solver.ChocoCSPSolver;

/**
 * Test Sudoku solving with Choco Solver + ACO integration.
 *
 * This demonstrates how to use the ChocoCSPSolver class which bridges
 * ANTARES's ACO framework with Choco Solver's constraint propagation.
 */
public class SudokuChocoTest {

    public static void main(String[] args) {
        System.out.println("=== Sudoku Solver with Choco + ACO ===\n");

        testEasySudoku();
        testMediumSudoku();
        testHardSudoku();
    }

    private static void testEasySudoku() {
        System.out.println("--- Easy Sudoku (Choco) ---");
        int[][] puzzle = SudokuProblem.createEasyPuzzle();

        System.out.println("Initial puzzle:");
        SudokuProblem.printGrid(puzzle);
        System.out.println();

        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithChocoACO(problem, 100);

        printResult(problem, solution);
        System.out.println();
    }

    private static void testMediumSudoku() {
        System.out.println("--- Medium Sudoku (Choco) ---");
        int[][] puzzle = SudokuProblem.createMediumPuzzle();

        System.out.println("Initial puzzle:");
        SudokuProblem.printGrid(puzzle);
        System.out.println();

        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithChocoACO(problem, 200);

        printResult(problem, solution);
        System.out.println();
    }

    private static void testHardSudoku() {
        System.out.println("--- Hard Sudoku (Choco) ---");
        int[][] puzzle = SudokuProblem.createHardPuzzle();

        System.out.println("Initial puzzle:");
        SudokuProblem.printGrid(puzzle);
        System.out.println();

        Problem problem = SudokuProblem.create(puzzle);
        Assignment solution = solveWithChocoACO(problem, 500);

        printResult(problem, solution);
        System.out.println();
    }

    private static Assignment solveWithChocoACO(Problem problem, int maxCycles) {
        // Configure ACO parameters
        ACOParameters params = new ACOParameters(
            2.0,    // alpha - pheromone importance
            0.0,    // beta - heuristic importance (0 for pure ACO)
            0.01,   // rho - evaporation rate
            0.01,   // tauMin - minimum pheromone
            10.0,   // tauMax - maximum pheromone
            30      // number of ants
        );

        System.out.println("Creating Choco solver with ACO strategy...");
        ChocoCSPSolver solver = new ChocoCSPSolver(problem, params);

        System.out.println("Solving with " + maxCycles + " cycles...");
        long startTime = System.currentTimeMillis();

        Assignment solution = solver.solve(maxCycles);

        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms\n");

        return solution;
    }

    private static void printResult(Problem problem, Assignment solution) {
        if (problem.isSolution(solution)) {
            System.out.println("✓ VALID SOLUTION FOUND!");
            System.out.println("Solution:");
            SudokuProblem.printGrid(SudokuProblem.assignmentToGrid(solution));
        } else if (solution.size() > 0) {
            System.out.println("⚠ Partial solution (" + solution.size() + "/" + problem.size() + " variables)");
            System.out.println("Solution:");
            SudokuProblem.printGrid(SudokuProblem.assignmentToGrid(solution));
        } else {
            System.out.println("✗ No solution found");
        }
    }
}

