package student.imt.antares.examples;

import java.util.*;
import org.slf4j.Logger;

import student.imt.antares.problem.*;

/**
 * Sudoku CSP problem for 9x9 grids.
 * Each cell is a variable with domain 1-9, subject to:
 * - Row constraints: all different in each row
 * - Column constraints: all different in each column
 * - Box constraints: all different in each 3x3 box
 */
public class SudokuProblem {
    
    private SudokuProblem() {
        // Private constructor to hide the implicit public one
    }

    /**
     * AllDifferent constraint: ensures all variables have different values.
     */
    private static class AllDifferentConstraint implements Constraint {
        private final Set<Variable> variables;
        private final String name;

        public AllDifferentConstraint(Set<Variable> variables, String name) {
            this.variables = Objects.requireNonNull(variables, "Variables cannot be null");
            this.name = Objects.requireNonNull(name, "Name cannot be null");

            if (variables.size() != 9) {
                throw new IllegalArgumentException("Sudoku constraint must have exactly 9 variables");
            }
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            Set<Integer> assignedValues = new HashSet<>();

            for (Variable cellVar : variables) {
                var value = assignment.getValue(cellVar);

                if (value.isEmpty()) {
                    continue;
                }

                if (assignedValues.contains(value.get())) {
                    return false;
                }

                assignedValues.add(value.get());
            }

            return true;
        }

        @Override
        public Set<Variable> getInvolvedVariables() {
            return new HashSet<>(variables);
        }

        @Override
        public String toString() {
            return "AllDifferent(" + name + ")";
        }
    }

    /**
     * Creates a Sudoku problem from an initial grid.
     *
     * @param initialGrid 9x9 array where 0 represents empty cell, 1-9 are given values
     * @return Sudoku problem with variables and constraints
     */
    public static Problem create(int[][] initialGrid) {
        validateGrid(initialGrid);

        Variable[][] cells = createVariables(initialGrid);
        List<Variable> allVariables = flattenCells(cells);
        List<Constraint> constraints = createAllConstraints(cells);

        return new Problem(allVariables, constraints);
    }

    /**
     * Creates a 9x9 grid of variables from the initial puzzle configuration.
     * Each cell becomes a variable with a domain of either a single fixed value (for given cells)
     * or all possible values 1-9 (for empty cells).
     *
     * @param initialGrid the initial puzzle grid where 0 represents empty cells
     * @return 9x9 array of variables representing the Sudoku grid
     */
    private static Variable[][] createVariables(int[][] initialGrid) {
        Variable[][] cells = new Variable[9][9];

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int given = initialGrid[row][col];
                Set<Integer> domain = (given != 0)
                        ? Set.of(given)
                        : Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

                String cellName = "R" + row + "C" + col;
                cells[row][col] = new Variable(cellName, domain);
            }
        }

        return cells;
    }

    /**
     * Flattens the 2D grid of variables into a single list.
     * This is needed for the Problem constructor which expects a flat collection of variables.
     *
     * @param cells 9x9 grid of variables
     * @return list containing all 81 variables in row-major order
     */
    private static List<Variable> flattenCells(Variable[][] cells) {
        List<Variable> allVariables = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                allVariables.add(cells[row][col]);
            }
        }
        return allVariables;
    }

    /**
     * Creates all Sudoku constraints (rows, columns, and 3x3 boxes).
     * A standard 9x9 Sudoku has 27 AllDifferent constraints: 9 rows, 9 columns, and 9 boxes.
     *
     * @param cells 9x9 grid of variables
     * @return list of all 27 Sudoku constraints
     */
    private static List<Constraint> createAllConstraints(Variable[][] cells) {
        List<Constraint> constraints = new ArrayList<>();
        constraints.addAll(createRowConstraints(cells));
        constraints.addAll(createColumnConstraints(cells));
        constraints.addAll(createBoxConstraints(cells));
        return constraints;
    }

    /**
     * Creates AllDifferent constraints for each row.
     * Each of the 9 rows must contain distinct values from 1-9.
     *
     * @param cells 9x9 grid of variables
     * @return list of 9 row constraints
     */
    private static List<Constraint> createRowConstraints(Variable[][] cells) {
        List<Constraint> constraints = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            Set<Variable> rowVars = new HashSet<>();
            for (int col = 0; col < 9; col++) {
                rowVars.add(cells[row][col]);
            }
            constraints.add(new AllDifferentConstraint(rowVars, "Row" + row));
        }
        return constraints;
    }

    /**
     * Creates AllDifferent constraints for each column.
     * Each of the 9 columns must contain distinct values from 1-9.
     *
     * @param cells 9x9 grid of variables
     * @return list of 9 column constraints
     */
    private static List<Constraint> createColumnConstraints(Variable[][] cells) {
        List<Constraint> constraints = new ArrayList<>();
        for (int col = 0; col < 9; col++) {
            Set<Variable> colVars = new HashSet<>();
            for (int row = 0; row < 9; row++) {
                colVars.add(cells[row][col]);
            }
            constraints.add(new AllDifferentConstraint(colVars, "Col" + col));
        }
        return constraints;
    }

    /**
     * Creates AllDifferent constraints for each 3x3 box.
     * Each of the 9 boxes must contain distinct values from 1-9.
     * Boxes are numbered left-to-right, top-to-bottom.
     *
     * @param cells 9x9 grid of variables
     * @return list of 9 box constraints
     */
    private static List<Constraint> createBoxConstraints(Variable[][] cells) {
        List<Constraint> constraints = new ArrayList<>();
        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                Set<Variable> boxVars = new HashSet<>();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int row = boxRow * 3 + i;
                        int col = boxCol * 3 + j;
                        boxVars.add(cells[row][col]);
                    }
                }
                constraints.add(new AllDifferentConstraint(boxVars, "Box" + boxRow + boxCol));
            }
        }
        return constraints;
    }

    /**
     * Creates an easy Sudoku puzzle.
     */
    public static int[][] createEasyPuzzle() {
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

    /**
     * Creates a medium difficulty Sudoku puzzle.
     */
    public static int[][] createMediumPuzzle() {
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

    /**
     * Creates a hard Sudoku puzzle.
     */
    public static int[][] createHardPuzzle() {
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

    /**
     * Creates a custom hard Sudoku puzzle from image.
     */
    public static int[][] createCustomHardPuzzle() {
        return new int[][]{
            {9, 0, 0, 0, 2, 0, 0, 1, 0},
            {0, 0, 6, 8, 0, 0, 0, 7, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 5, 0, 3},
            {0, 0, 9, 0, 0, 5, 0, 0, 1},
            {0, 0, 4, 1, 0, 0, 0, 8, 0},
            {3, 0, 0, 5, 0, 0, 0, 0, 0},
            {7, 0, 0, 6, 0, 0, 0, 9, 0},
            {2, 0, 0, 0, 0, 8, 7, 3, 0}
        };
    }

    private static void validateGrid(int[][] grid) {
        if (grid == null || grid.length != 9) {
            throw new IllegalArgumentException("Grid must be 9x9");
        }

        for (int row = 0; row < 9; row++) {
            if (grid[row] == null || grid[row].length != 9) {
                throw new IllegalArgumentException("Grid must be 9x9");
            }

            for (int col = 0; col < 9; col++) {
                int value = grid[row][col];
                if (value < 0 || value > 9) {
                    throw new IllegalArgumentException("Grid values must be 0-9");
                }
            }
        }
    }

    public static void printGrid(int[][] grid) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(SudokuProblem.class);
        for (int row = 0; row < 9; row++) {
            if (row % 3 == 0 && row != 0) {
                logger.info("------+-------+------");
            }
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < 9; col++) {
                if (col % 3 == 0 && col != 0) {
                    line.append("| ");
                }
                line.append(grid[row][col]).append(" ");
            }
            if (logger.isInfoEnabled()) {
                logger.info(line.toString());
            }
        }
    }

    public static int[][] assignmentToGrid(Assignment assignment) {
        int[][] grid = new int[9][9];

        for (Variable cellVar : assignment.getAssignedVariables()) {
            String name = cellVar.name();
            int row = Character.getNumericValue(name.charAt(1));
            int col = Character.getNumericValue(name.charAt(3));

            assignment.getValue(cellVar).ifPresent(value -> 
                grid[row][col] = value
            );
        }

        return grid;
    }
}
