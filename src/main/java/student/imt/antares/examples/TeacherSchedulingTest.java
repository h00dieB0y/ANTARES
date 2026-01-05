package student.imt.antares.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.*;
import student.imt.antares.construction.*;
import student.imt.antares.pheromone.MaxMinUpdate;
import student.imt.antares.problem.*;
import student.imt.antares.solver.BasicCSPSolver;

public class TeacherSchedulingTest {

    private static final Logger logger = LoggerFactory.getLogger(TeacherSchedulingTest.class);

    public static void main(String[] args) {
        testTeacherScheduling();
    }

    private static void testTeacherScheduling() {
        Problem problem = TeacherSchedulingProblem.create();
        Assignment solution = solveWithACO(problem, 5000);
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

        return colony.solve(
            problem,
            constructor,
            VariableSelectors.SMALLEST_DOMAIN_FIRST,
            valueSelector,
            pheromoneUpdater,
            solver,
            maxCycles
        );
    }

    private static void printResult(Problem problem, Assignment solution) {
        if (solution == null) {
            logger.info("No solution found!");
            return;
        }

        if (!problem.isConsistent(solution)) {
            logger.warn("Solution is inconsistent!");
            return;
        }

        logger.info("Solution found!");
        TeacherSchedulingProblem.printSchedule(solution);
        logger.info("");
    }
}
