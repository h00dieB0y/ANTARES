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
 * Test runner for teacher scheduling problem.
 */
public class TeacherSchedulingTest {

    public static void main(String[] args) {
        System.out.println("=== Teacher Scheduling with ACO ===\n");

        // Create the teacher scheduling CSP
        Problem problem = TeacherSchedulingProblem.create();

        System.out.println("Problem size: " + problem.size() + " variables");
        System.out.println("(3 rooms × 3 time slots = 9 assignments)\n");

        System.out.println("Constraints:");
        System.out.println("  1. One teacher cannot teach in two rooms at same time");
        System.out.println("  2. Teacher A in Room 1 at Time 1 (Physics)");
        System.out.println("  3. Teacher B not available at Time 3");
        System.out.println("  4. Teacher C must teach in Room 2 at least once");
        System.out.println("  5. Each teacher teaches exactly twice\n");

        // Configure ACO parameters
        ACOParameters params = new ACOParameters(
            1.5,    // alpha: lower pheromone importance for more exploration
            0.0,    // beta: no heuristic
            0.1,    // rho: 10% evaporation rate for faster adaptation
            0.01,   // tauMin
            10.0,   // tauMax
            50      // numberOfAnts: more ants for better exploration
        );

        System.out.println("ACO Parameters:");
        System.out.println("  Alpha (pheromone): " + params.alpha());
        System.out.println("  Ants per cycle: " + params.numberOfAnts());

        // Initialize colony
        Colony colony = Colony.create(problem, params);

        AssignmentConstructor constructor = new AssignmentConstructor();
        ProbabilisticSelection valueSelector = new ProbabilisticSelection(42);
        MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
        BasicCSPSolver solver = new BasicCSPSolver(problem);

        int maxCycles = 5000;
        System.out.println("\nSolving with max " + maxCycles + " cycles...\n");

        long startTime = System.currentTimeMillis();

        Assignment solution = colony.solve(
            problem,
            constructor,
            VariableSelectors.SMALLEST_DOMAIN_FIRST,
            valueSelector,
            pheromoneUpdater,
            solver,
            maxCycles
        );

        long endTime = System.currentTimeMillis();

        System.out.println("\nTime: " + (endTime - startTime) + " ms");

        // Display and validate results
        TeacherSchedulingProblem.printSchedule(solution);
        TeacherSchedulingProblem.printStatistics(solution);
        TeacherSchedulingProblem.validateSchedule(problem, solution);
    }
}
