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

public class TeacherSchedulingTest {

    public static void main(String[] args) {
        Problem problem = TeacherSchedulingProblem.create();

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

        int maxCycles = 5000;

        Assignment solution = colony.solve(
            problem,
            constructor,
            VariableSelectors.SMALLEST_DOMAIN_FIRST,
            valueSelector,
            pheromoneUpdater,
            solver,
            maxCycles
        );
    }
}
