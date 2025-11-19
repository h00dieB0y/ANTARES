package student.imt.antares.construction;

import java.util.Objects;
import java.util.Set;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;
import student.imt.antares.solver.BasicCSPSolver;

public class AssignmentConstructor {

    public Assignment construct(Problem problem,
                                PheromoneMatrix pheromones,
                                ACOParameters parameters,
                                VariableSelector variableSelector,
                                ProbabilisticSelection valueSelector,
                                BasicCSPSolver solver) {

        Objects.requireNonNull(problem, "Problem cannot be null");
        Objects.requireNonNull(pheromones, "Pheromone matrix cannot be null");
        Objects.requireNonNull(parameters, "ACO parameters cannot be null");
        Objects.requireNonNull(variableSelector, "Variable selector cannot be null");
        Objects.requireNonNull(valueSelector, "Value selector cannot be null");
        Objects.requireNonNull(solver, "CSP solver cannot be null");

        Assignment assignment = Assignment.empty();
        solver.reset();

        while (!assignment.isComplete(problem.size())) {
            var nextVariable = variableSelector.selectNext(problem, assignment, solver);

            if (nextVariable.isEmpty()) {
                return assignment;
            }

            assignment = processVariable(nextVariable.get(), assignment, pheromones,
                                        parameters, valueSelector, solver);

            if (solver.hasFailed()) {
                return assignment;
            }
        }

        return assignment;
    }

    private Assignment processVariable(Variable variable,
                                           Assignment assignment,
                                           PheromoneMatrix pheromones,
                                           ACOParameters parameters,
                                           ProbabilisticSelection valueSelector,
                                           BasicCSPSolver solver) {
        Set<Integer> domain = solver.getCurrentDomain(variable);

        if (domain.isEmpty()) {
            return assignment;
        }

        var selectedValue = valueSelector.select(variable, domain, pheromones, parameters);

        if (selectedValue.isEmpty()) {
            return assignment;
        }

        Assignment newAssignment = assignment.assign(variable, selectedValue.get());

        if (!solver.propagate(newAssignment)) {
            return newAssignment;
        }

        return assignSingletons(newAssignment, solver);
    }

    private Assignment assignSingletons(Assignment assignment, BasicCSPSolver solver) {
        Assignment current = assignment;

        while (true) {
            var singletons = solver.getSingletonVariables();

            final Assignment currentAssignmentForFilter = current;
            var unassignedSingletons = singletons.stream()
                    .filter(variable -> !currentAssignmentForFilter.isAssigned(variable))
                    .toList();

            if (unassignedSingletons.isEmpty()) {
                break;
            }

            for (Variable singletonVar : unassignedSingletons) {
                current = assignSingleton(singletonVar, current, solver);

                if (solver.hasFailed()) {
                    return current;
                }
            }
        }

        return current;
    }

    private Assignment assignSingleton(Variable variable, Assignment assignment, BasicCSPSolver solver) {
        Set<Integer> domain = solver.getCurrentDomain(variable);

        if (domain.size() != 1) {
            return assignment;
        }

        Integer value = domain.iterator().next();

        Assignment newAssignment = assignment.assign(variable, value);
        solver.propagate(newAssignment);

        return newAssignment;
    }
}
