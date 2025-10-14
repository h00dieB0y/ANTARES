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
import student.imt.antares.solver.CSPSolver;

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
                                ValueSelector valueSelector,
                                CSPSolver solver) {

        Objects.requireNonNull(problem, "Problem cannot be null");
        Objects.requireNonNull(pheromones, "Pheromone matrix cannot be null");
        Objects.requireNonNull(parameters, "ACO parameters cannot be null");
        Objects.requireNonNull(variableSelector, "Variable selector cannot be null");
        Objects.requireNonNull(valueSelector, "Value selector cannot be null");
        Objects.requireNonNull(solver, "CSP solver cannot be null");

        logger.trace("Starting assignment construction for problem size {}", problem.size());

        Assignment assignment = Assignment.empty();
        solver.reset();

        int step = 0;
        while (!assignment.isComplete(problem.size())) {
            var nextVariable = variableSelector.selectNext(problem, assignment, solver);

            if (nextVariable.isEmpty()) {
                logger.debug("No variable selected at step {} - construction failed", step);
                return assignment;
            }

            logger.trace("Step {}: Selected variable {}", step, nextVariable.get().name());

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

    private <T> Assignment processVariable(Variable<T> variable,
                                           Assignment assignment,
                                           PheromoneMatrix pheromones,
                                           ACOParameters parameters,
                                           ValueSelector valueSelector,
                                           CSPSolver solver) {
        Set<T> domain = solver.getCurrentDomain(variable);

        logger.trace("Processing variable {} with domain size {}", variable.name(), domain.size());

        if (domain.isEmpty()) {
            logger.trace("Domain empty for variable {}", variable.name());
            return assignment;
        }

        var selectedValue = valueSelector.select(variable, domain, pheromones, parameters);

        if (selectedValue.isEmpty()) {
            logger.trace("No value selected for variable {}", variable.name());
            return assignment;
        }

        logger.trace("Selected value {} for variable {}", selectedValue.get(), variable.name());

        Assignment newAssignment = assignment.assign(variable, selectedValue.get());

        if (!solver.propagate(newAssignment)) {
            logger.trace("Propagation failed after assigning {} = {}", variable.name(), selectedValue.get());
            return newAssignment;
        }

        // Auto-assign singleton variables after propagation (Ant-CP algorithm line 9)
        return assignSingletons(newAssignment, solver);
    }

    /**
     * Automatically assigns all singleton variables (variables with domain size 1)
     * after constraint propagation, as specified in Ant-CP algorithm.
     */
    private Assignment assignSingletons(Assignment assignment, CSPSolver solver) {
        Assignment current = assignment;

        while (true) {
            var singletons = solver.getSingletonVariables();

            // Filter out already assigned singletons
            final Assignment currentAssignmentForFilter = current;
            var unassignedSingletons = singletons.stream()
                    .filter(var -> !currentAssignmentForFilter.isAssigned(var))
                    .toList();

            if (unassignedSingletons.isEmpty()) {
                break; // No more singletons to assign
            }

            logger.trace("Auto-assigning {} singleton variables", unassignedSingletons.size());

            // Assign each singleton
            for (Variable<?> singletonVar : unassignedSingletons) {
                current = assignSingleton(singletonVar, current, solver);

                if (solver.hasFailed()) {
                    logger.trace("Propagation failed during singleton assignment");
                    return current;
                }
            }
        }

        return current;
    }

    /**
     * Assigns a single singleton variable to its only possible value.
     */
    private <T> Assignment assignSingleton(Variable<T> variable, Assignment assignment, CSPSolver solver) {
        Set<T> domain = solver.getCurrentDomain(variable);

        if (domain.size() != 1) {
            logger.warn("Variable {} is not a singleton (domain size: {})", variable.name(), domain.size());
            return assignment;
        }

        T value = domain.iterator().next();
        logger.trace("Auto-assigning singleton: {} = {}", variable.name(), value);

        Assignment newAssignment = assignment.assign(variable, value);
        solver.propagate(newAssignment);

        return newAssignment;
    }
}
