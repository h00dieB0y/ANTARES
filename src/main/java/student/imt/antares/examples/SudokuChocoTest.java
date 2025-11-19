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

public class SudokuChocoTest {

    public static void main(String[] args) {
        testHardSudoku();
    }

    private static void testEasySudoku() {
        int[][] puzzle = createEasyPuzzle();
        solveWithChocoACO(puzzle, 500);
    }

    private static void testMediumSudoku() {
        int[][] puzzle = createMediumPuzzle();
        solveWithChocoACO(puzzle, 1000);
    }

    private static void testHardSudoku() {
        int[][] puzzle = createHardPuzzle();
        solveWithChocoACO(puzzle, 2000);
    }

    private static void solveWithChocoACO(int[][] puzzle, int maxRestarts) {
        Model model = new Model("Sudoku");

        // Create 9x9 grid of IntVars with domains 1-9
        IntVar[][] grid = new IntVar[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (puzzle[row][col] != 0) {
                    grid[row][col] = model.intVar("cell_" + row + "_" + col, puzzle[row][col]);
                } else {
                    grid[row][col] = model.intVar("cell_" + row + "_" + col, 1, 9);
                }
            }
        }
        for (int row = 0; row < 9; row++) {
            model.allDifferent(grid[row]).post();
        }

        for (int col = 0; col < 9; col++) {
            IntVar[] column = new IntVar[9];
            for (int row = 0; row < 9; row++) {
                column[row] = grid[row][col];
            }
            model.allDifferent(column).post();
        }

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

        ACOParameters acoParams = new ACOParameters(
            2.0,
            0.0,
            0.01,
            0.01,
            1.0,
            30
        );
        IntVar[] allVars = model.retrieveIntVars(true);
        AntColonyMetaHeuristicSolver acoStrategy = new AntColonyMetaHeuristicSolver(
            allVars,
            new AntiFirstFail(model),
            acoParams
        );

        Solver solver = model.getSolver();
        solver.setSearch(acoStrategy);
        solver.showShortStatistics();

        solver.limitRestart(maxRestarts);

        solver.solve();
    }

    private static void printSolution(IntVar[][] grid) {
    }

    private static void printGrid(int[][] grid) {
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
