package student.imt.antares.examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Constraint;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

/**
 * Sudoku CSP problem for 9x9 grids.
 * Each cell is a variable with domain 1-9, subject to:
 * - Row constraints: all different in each row
 * - Column constraints: all different in each column
 * - Box constraints: all different in each 3x3 box
 */
public class SudokuProblem {

    /**
     * AllDifferent constraint: ensures all variables have different values.
     */
    private static class AllDifferentConstraint implements Constraint {
        private final Set<Variable<Integer>> variables;
        private final String name;

        public AllDifferentConstraint(Set<Variable<Integer>> variables, String name) {
            this.variables = Objects.requireNonNull(variables, "Variables cannot be null");
            this.name = Objects.requireNonNull(name, "Name cannot be null");

            if (variables.size() != 9) {
                throw new IllegalArgumentException("Sudoku constraint must have exactly 9 variables");
            }
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            Set<Integer> assignedValues = new HashSet<>();

            for (Variable<Integer> var : variables) {
                var value = assignment.getValue(var);

                if (value.isEmpty()) {
                    // Unassigned variables don't violate constraint yet
                    continue;
                }

                // Check for duplicate
                if (assignedValues.contains(value.get())) {
                    return false;
                }

                assignedValues.add(value.get());
            }

            return true;
        }

        @Override
        public Set<Variable<?>> getInvolvedVariables() {
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

        // Create variables: one per cell
        Variable<Integer>[][] cells = new Variable[9][9];
        List<Variable<?>> allVariables = new ArrayList<>();

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int given = initialGrid[row][col];
                Set<Integer> domain;

                if (given != 0) {
                    // Given value: single value domain
                    domain = Set.of(given);
                } else {
                    // Empty cell: all values possible
                    domain = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
                }

                String cellName = "R" + row + "C" + col;
                cells[row][col] = new Variable<>(cellName, domain);
                allVariables.add(cells[row][col]);
            }
        }

        // Create constraints
        List<Constraint> constraints = new ArrayList<>();

        // Row constraints
        for (int row = 0; row < 9; row++) {
            Set<Variable<Integer>> rowVars = new HashSet<>();
            for (int col = 0; col < 9; col++) {
                rowVars.add(cells[row][col]);
            }
            constraints.add(new AllDifferentConstraint(rowVars, "Row" + row));
        }

        // Column constraints
        for (int col = 0; col < 9; col++) {
            Set<Variable<Integer>> colVars = new HashSet<>();
            for (int row = 0; row < 9; row++) {
                colVars.add(cells[row][col]);
            }
            constraints.add(new AllDifferentConstraint(colVars, "Col" + col));
        }

        // Box constraints (3x3)
        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                Set<Variable<Integer>> boxVars = new HashSet<>();
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

        return new Problem(allVariables, constraints);
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

    /**
     * Prints a Sudoku grid in readable format.
     */
    public static void printGrid(int[][] grid) {
        for (int row = 0; row < 9; row++) {
            if (row % 3 == 0 && row != 0) {
                System.out.println("------+-------+------");
            }

            for (int col = 0; col < 9; col++) {
                if (col % 3 == 0 && col != 0) {
                    System.out.print("| ");
                }

                int value = grid[row][col];
                System.out.print(value == 0 ? ". " : value + " ");
            }
            System.out.println();
        }
    }

    /**
     * Converts an assignment to a 9x9 grid for display.
     */
    public static int[][] assignmentToGrid(Assignment assignment) {
        int[][] grid = new int[9][9];

        for (Variable<?> var : assignment.getAssignedVariables()) {
            String name = var.name();
            int row = Character.getNumericValue(name.charAt(1));
            int col = Character.getNumericValue(name.charAt(3));

            assignment.getValue(var).ifPresent(value -> {
                if (!(value instanceof Integer)) {
                    throw new IllegalArgumentException(
                        "Expected Integer value for Sudoku cell " + name + ", got: " + value.getClass().getSimpleName());
                }
                grid[row][col] = (Integer) value;
            });
        }

        return grid;
    }
}
