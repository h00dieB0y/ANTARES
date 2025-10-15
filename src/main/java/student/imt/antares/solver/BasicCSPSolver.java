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
 * @see CSPSolver
 * @see Assignment
 */
public class BasicCSPSolver implements CSPSolver {
    private static final Logger logger = LoggerFactory.getLogger(BasicCSPSolver.class);

    private final Problem problem;
    /**
     * Internal storage using wildcard types to allow heterogeneous variable domains.
     * Type safety is maintained through the generic public API methods.
     */
    private Map<Variable<?>, Set<?>> currentDomains;
    private boolean failed;

    public BasicCSPSolver(Problem problem) {
        this.problem = Objects.requireNonNull(problem, "Problem cannot be null");
        this.currentDomains = new HashMap<>();
        this.failed = false;
        reset();
    }

    @Override
    public void reset() {
        currentDomains.clear();
        failed = false;

        for (Variable<?> var : problem.getVariables()) {
            currentDomains.put(var, Set.copyOf(var.domain()));
        }
    }

    @Override
    public boolean propagate(Assignment assignment) {
        if (failed) {
            logger.trace("Propagation skipped - solver already failed");
            return false;
        }

        logger.trace("Propagating assignment with {} variables", assignment.size());

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

        logger.trace("Propagation successful");
        return true;
    }

    /**
     * Retrieves the current reduced domain for a variable after constraint propagation.
     *
     * @param variable the variable to look up
     * @param <T> the type of the variable's domain values
     * @return the current domain (may be reduced from original), or empty set if unknown
     *
     * @implNote Contains an unchecked cast from {@code Set<?>} to {@code Set<T>}, which is
     * safe because {@link #reset()} initializes domains from {@code variable.domain()}, and
     * {@link #reduceDomain(Variable, Constraint, Assignment)} maintains the type invariant.
     */
    @Override
    public <T> Set<T> getCurrentDomain(Variable<T> variable) {
        @SuppressWarnings("unchecked") // Safe: reset() initializes Variable<T> -> Set<T> mapping
        Set<T> domain = (Set<T>) currentDomains.getOrDefault(variable, Set.of());
        return domain;
    }

    @Override
    public boolean hasFailed() {
        return failed;
    }

    @Override
    public List<Variable<?>> getSingletonVariables() {
        return currentDomains.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean propagateConstraint(Constraint constraint, Assignment assignment) {
        Set<Variable<?>> involvedVars = constraint.getInvolvedVariables();
        Set<Variable<?>> assignedVars = involvedVars.stream()
                .filter(assignment::isAssigned)
                .collect(Collectors.toSet());

        if (assignedVars.size() == involvedVars.size()) {
            return constraint.isSatisfiedBy(assignment);
        }

        for (Variable<?> var : involvedVars) {
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
     * Uses optimistic testing: temporarily assigns each value, checks satisfaction,
     * then rolls back. This avoids creating snapshots for each test, significantly
     * improving performance during forward checking.
     * </p>
     *
     * @return {@code false} if domain becomes empty (wipeout detected), {@code true} otherwise
     */
    private <T> boolean reduceDomain(Variable<T> variable, Constraint constraint, Assignment assignment) {
        Set<T> currentDomain = getCurrentDomain(variable);

        Set<T> newDomain = currentDomain.stream()
                .filter(value -> {
                    assignment.assign(variable, value);
                    boolean satisfied = constraint.isSatisfiedBy(assignment);
                    assignment.unassign(variable);
                    return satisfied;
                })
                .collect(Collectors.toSet());

        currentDomains.put(variable, newDomain);

        return !newDomain.isEmpty();
    }
}
