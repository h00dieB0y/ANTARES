package student.imt.antares.examples;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.variables.AntiFirstFail;
import org.chocosolver.solver.variables.IntVar;
import student.imt.antares.colony.ACOParameters;
import student.imt.antares.construction.ProbabilisticSelection;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;
import student.imt.antares.solver.AntColonyMetaHeuristicSolver;

/**
 * Test Sudoku solving with Choco Solver + ACO integration.
 *
 * This demonstrates how to use the ChocoCSPSolver class which bridges
 * ANTARES's ACO framework with Choco Solver's constraint propagation.
 */
public class SudokuChocoTest {

    public static void main(String[] args) {
        System.out.println("=== Sudoku Solver with Choco + ACO ===\n");

        //testEasySudoku();
        testMediumSudoku();
        //testHardSudoku();
    }


    private static void testEasySudoku() {
        System.out.println("--- Easy Sudoku (Choco + ACO) ---");
        int[][] puzzle = createEasyPuzzle();

        System.out.println("Initial puzzle:");
        printGrid(puzzle);
        System.out.println();

        solveWithChocoACO(puzzle, 500);
        System.out.println();
    }

    private static void testMediumSudoku() {
        System.out.println("--- Medium Sudoku (Choco + ACO) ---");
        int[][] puzzle = createMediumPuzzle();

        System.out.println("Initial puzzle:");
        printGrid(puzzle);
        System.out.println();

        solveWithChocoACO(puzzle, 1000);
        System.out.println();
    }

    private static void testHardSudoku() {
        System.out.println("--- Hard Sudoku (Choco + ACO) ---");
        int[][] puzzle = createHardPuzzle();

        System.out.println("Initial puzzle:");
        printGrid(puzzle);
        System.out.println();

        solveWithChocoACO(puzzle, 2000);
        System.out.println();
    }

    private static void solveWithChocoACO(int[][] puzzle, int maxRestarts) {
        Model model = new Model("Sudoku");

        // Create 9x9 grid of IntVars with domains 1-9
        IntVar[][] grid = new IntVar[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (puzzle[row][col] != 0) {
                    // Fixed cell from puzzle
                    grid[row][col] = model.intVar("cell_" + row + "_" + col, puzzle[row][col]);
                } else {
                    // Variable cell (domain 1-9)
                    grid[row][col] = model.intVar("cell_" + row + "_" + col, 1, 9);
                }
            }
        }

        // Constraints: AllDifferent for rows, columns, and 3x3 boxes
        // (Choco's allDifferent is much simpler than custom constraints!)

        // Row constraints
        for (int row = 0; row < 9; row++) {
            model.allDifferent(grid[row]).post();
        }

        // Column constraints
        for (int col = 0; col < 9; col++) {
            IntVar[] column = new IntVar[9];
            for (int row = 0; row < 9; row++) {
                column[row] = grid[row][col];
            }
            model.allDifferent(column).post();
        }

        // Box constraints (3x3 subgrids)
        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                IntVar[] box = new IntVar[9];
                int idx = 0;
                for (int row = boxRow * 3; row < boxRow * 3 + 3; row++) {
                    for (int col = boxCol * 3; col < boxCol * 3 + 3; col++) {
                        box[idx++] = grid[row][col];
                    }
                }
                model.allDifferent(box).post();
            }
        }

        // ACO parameters (same as standalone ANTARES for fair comparison)
        ACOParameters acoParams = new ACOParameters(
            2.0,   // alpha (pheromone importance)
            0.0,   // beta (heuristic importance, 0 = pure pheromone)
            0.01,  // rho (evaporation rate)
            0.01,  // tauMin
            1.0,  // tauMax
            30     // numberOfAnts per cycle
        );

        // Create ACO metaheuristic strategy
        IntVar[] allVars = model.retrieveIntVars(true);
        AntColonyMetaHeuristicSolver acoStrategy = new AntColonyMetaHeuristicSolver(
            allVars,
            new AntiFirstFail(model),
            acoParams
        );

        Solver solver = model.getSolver();
        solver.setSearch(acoStrategy); // Re-enable ACO strategy

        // Limit search to prevent infinite loops
        solver.limitRestart(maxRestarts);

        System.out.println("Solving with ACO (max " + maxRestarts + " restarts, 30 ants/cycle)...");
        long startTime = System.currentTimeMillis();

        if (solver.solve()) {
            long endTime = System.currentTimeMillis();
            System.out.println("Solution found!");
            System.out.println("Time: " + (endTime - startTime) + " ms");
            System.out.println("Restarts: " + solver.getRestartCount());
            System.out.println("Failures: " + solver.getFailCount());
            System.out.println();

            // Print solution
            printSolution(grid);
        } else {
            long endTime = System.currentTimeMillis();
            System.out.println("No solution found within restart limit");
            System.out.println("Time: " + (endTime - startTime) + " ms");
            System.out.println("Restarts: " + solver.getRestartCount());
            System.out.println("Failures: " + solver.getFailCount());
        }
    }

    private static void printSolution(IntVar[][] grid) {
        System.out.println("Solution:");
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                System.out.print(grid[row][col].getValue() + " ");
                if ((col + 1) % 3 == 0 && col < 8) System.out.print("| ");
            }
            System.out.println();
            if ((row + 1) % 3 == 0 && row < 8) {
                System.out.println("------+-------+------");
            }
        }
    }

    private static void printGrid(int[][] grid) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                char ch = grid[row][col] == 0 ? '.' : (char) ('0' + grid[row][col]);
                System.out.print(ch + " ");
                if ((col + 1) % 3 == 0 && col < 8) System.out.print("| ");
            }
            System.out.println();
            if ((row + 1) % 3 == 0 && row < 8) {
                System.out.println("------+-------+------");
            }
        }
    }


    private static int[][] createEasyPuzzle() {
        return new int[][]{
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };
    }

    private static int[][] createMediumPuzzle() {
        return new int[][]{
            {0, 0, 0, 6, 0, 0, 4, 0, 0},
            {7, 0, 0, 0, 0, 3, 6, 0, 0},
            {0, 0, 0, 0, 9, 1, 0, 8, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 5, 0, 1, 8, 0, 0, 0, 3},
            {0, 0, 0, 3, 0, 6, 0, 4, 5},
            {0, 4, 0, 2, 0, 0, 0, 6, 0},
            {9, 0, 3, 0, 0, 0, 0, 0, 0},
            {0, 2, 0, 0, 0, 0, 1, 0, 0}
        };
    }

    private static int[][] createHardPuzzle() {
        return new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 1, 2},
            {0, 0, 0, 0, 3, 5, 0, 0, 0},
            {0, 0, 0, 6, 0, 0, 0, 7, 0},
            {7, 0, 0, 0, 0, 0, 3, 0, 0},
            {0, 0, 0, 4, 0, 0, 8, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 2, 0, 0, 0, 0},
            {0, 8, 0, 0, 0, 0, 0, 4, 0},
            {0, 5, 0, 0, 0, 0, 6, 0, 0}
        };
    }
}
