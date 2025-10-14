package student.imt.antares.construction;

import java.util.Optional;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;
import student.imt.antares.solver.CSPSolver;

@FunctionalInterface
public interface VariableSelector {
    Optional<Variable<?>> selectNext(Problem problem, Assignment assignment, CSPSolver solver);
}
