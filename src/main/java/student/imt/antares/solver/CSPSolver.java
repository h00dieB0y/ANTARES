package student.imt.antares.solver;

import java.util.List;
import java.util.Set;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Variable;

public interface CSPSolver {
    void reset();
    boolean propagate(Assignment assignment);
    <T> Set<T> getCurrentDomain(Variable<T> variable);
    boolean hasFailed();
    List<Variable<?>> getSingletonVariables();
}
