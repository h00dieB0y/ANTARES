package student.imt.antares.examples;

import org.chocosolver.solver.Model;
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
        // Configure AntColonyMetaHeuristicSolver parameters
        ACOParameters acoParameters = ACOParameters.withDefaults();
        Model model = new Model("Sudoku ACO Choco Solver");
        IntVar[] chocoVars = new IntVar[problem.size()];

        for (int i = 0; i < problem.size(); i++) {
            chocoVars[i] = model.intVar("Var" + i, 1, 9);
        }
        // define constraints of sudoku with choco constrains directly with use of problem
        // AllDifferent constraints for rows, columns, and boxes
        for (int r = 0; r < 9; r++) {
            IntVar[] rowVars = new IntVar[9];
            IntVar[] colVars = new IntVar[9];
            IntVar[] boxVars = new IntVar[9];
            for (int c = 0; c < 9; c++) {
                rowVars[c] = chocoVars[r * 9 + c];
                colVars[c] = chocoVars[c * 9 + r];
                int boxRow = (r / 3) * 3 + c / 3;
                int boxCol = (r % 3) * 3 + c % 3;
                boxVars[c] = chocoVars[boxRow * 9 + boxCol];
            }
            model.allDifferent(rowVars).post();
            model.allDifferent(colVars).post();
            model.allDifferent(boxVars).post();
        }

         new AntColonyMetaHeuristicSolver(
                chocoVars,
                new AntiFirstFail(model),
                acoParameters,
                new ProbabilisticSelection()
        );

        if (model.getSolver().solve()) {
            Assignment assignment = Assignment.empty();
            for (int i = 0; i < chocoVars.length; i++) {
                assignment = assignment.assign((Variable<Integer>) problem.getVariables().get(chocoVars[i].getValue()), chocoVars[i].getValue());
            }
            return assignment;
        } else {
            return Assignment.empty();
        }
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
