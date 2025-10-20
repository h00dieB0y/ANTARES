package student.imt.antares.solver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Constraint;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

/**
 * Basic CSP solver with forward checking and domain reduction.
 * Maintains current domains for each variable and propagates constraints.
 *
 * @see Assignment
 */
public class BasicCSPSolver {
    private static final Logger logger = LoggerFactory.getLogger(BasicCSPSolver.class);

    private final Problem problem;
    private Map<Variable, Set<Integer>> currentDomains;
    private boolean failed;

    public BasicCSPSolver(Problem problem) {
        this.problem = Objects.requireNonNull(problem, "Problem cannot be null");
        this.currentDomains = new HashMap<>();
        this.failed = false;
        reset();
    }

    public void reset() {
        currentDomains.clear();
        failed = false;

        for (Variable var : problem.getVariables()) {
            currentDomains.put(var, Set.copyOf(var.domain()));
        }
    }

    public boolean propagate(Assignment assignment) {
        if (failed) {
            return false;
        }


        if (!problem.isConsistent(assignment)) {
            logger.debug("Assignment inconsistent with constraints");
            failed = true;
            return false;
        }

        for (Constraint constraint : problem.getConstraints()) {
            if (!propagateConstraint(constraint, assignment)) {
                logger.debug("Constraint propagation failed: {}", constraint);
                failed = true;
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves the current reduced domain for a variable after constraint propagation.
     *
     * @param variable the variable to look up
     * @return the current integer domain (may be reduced from original), or empty set if unknown
     */
    public Set<Integer> getCurrentDomain(Variable variable) {
        return currentDomains.getOrDefault(variable, Set.of());
    }

    public boolean hasFailed() {
        return failed;
    }

    public List<Variable> getSingletonVariables() {
        return currentDomains.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean propagateConstraint(Constraint constraint, Assignment assignment) {
        Set<Variable> involvedVars = constraint.getInvolvedVariables();
        Set<Variable> assignedVars = involvedVars.stream()
                .filter(assignment::isAssigned)
                .collect(Collectors.toSet());

        if (assignedVars.size() == involvedVars.size()) {
            return constraint.isSatisfiedBy(assignment);
        }

        for (Variable var : involvedVars) {
            if (!assignment.isAssigned(var)) {
                if (!reduceDomain(var, constraint, assignment)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Reduces a variable's domain by filtering out values that violate a constraint.
     * <p>
     * <strong>Mutation Strategy:</strong> Uses a test-and-revert pattern for performance:
     * temporarily assigns each candidate value to the (mutable) assignment, checks constraint
     * satisfaction, then immediately removes the assignment. This avoids creating defensive
     * snapshots for each value test, significantly improving performance during forward checking.
     * </p>
     * <p>
     * The assignment is guaranteed to be in its original state after this method returns,
     * as each {@code assign()} is paired with {@code unassign()} within the lambda.
     * </p>
     *
     * @return {@code false} if domain becomes empty (wipeout detected), {@code true} otherwise
     */
    private boolean reduceDomain(Variable variable, Constraint constraint, Assignment assignment) {
        Set<Integer> currentDomain = getCurrentDomain(variable);

        Set<Integer> newDomain = currentDomain.stream()
                .filter(value -> {
                    // Temporarily mutate assignment to test this value
                    assignment.assign(variable, value);
                    boolean satisfied = constraint.isSatisfiedBy(assignment);
                    // Immediately revert mutation
                    assignment.unassign(variable);
                    return satisfied;
                })
                .collect(Collectors.toSet());

        currentDomains.put(variable, newDomain);

        return !newDomain.isEmpty();
    }
}
