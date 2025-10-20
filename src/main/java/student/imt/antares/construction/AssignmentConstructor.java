package student.imt.antares.construction;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;
import student.imt.antares.solver.BasicCSPSolver;

/**
 * Constructs CSP assignments using ant colony optimization.
 * Iteratively selects variables and values guided by pheromone trails.
 */
public class AssignmentConstructor {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentConstructor.class);

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
        solver.reset(); // Reset solver state for this ant

        int step = 0;
        while (!assignment.isComplete(problem.size())) {
            var nextVariable = variableSelector.selectNext(problem, assignment, solver);

            if (nextVariable.isEmpty()) {
                logger.debug("No variable selected at step {} - construction failed", step);
                return assignment;
            }


            assignment = processVariable(nextVariable.get(), assignment, pheromones,
                                        parameters, valueSelector, solver);

            if (solver.hasFailed()) {
                logger.debug("Construction failed at step {} - {}/{} variables assigned",
                           step, assignment.size(), problem.size());
                return assignment;
            }

            step++;
        }

        logger.debug("Construction complete: {}/{} variables assigned", assignment.size(), problem.size());
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

    /**
     * Automatically assigns all singleton variables (variables with domain size 1)
     * after constraint propagation.
     * <p>
     * Implements Ant-CP algorithm line 9. Singleton assignment is deterministic
     * and doesn't require ant guidance, so it's performed immediately to reduce
     * the search space.
     * </p>
     */
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

    /**
     * Assigns a single singleton variable to its only possible value.
     */
    private Assignment assignSingleton(Variable variable, Assignment assignment, BasicCSPSolver solver) {
        Set<Integer> domain = solver.getCurrentDomain(variable);

        if (domain.size() != 1) {
            logger.warn("Variable {} is not a singleton (domain size: {})", variable.name(), domain.size());
            return assignment;
        }

        Integer value = domain.iterator().next();

        Assignment newAssignment = assignment.assign(variable, value);
        solver.propagate(newAssignment);

        return newAssignment;
    }
}
