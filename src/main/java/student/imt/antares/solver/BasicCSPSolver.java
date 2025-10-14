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
 */
public class BasicCSPSolver implements CSPSolver {
    private static final Logger logger = LoggerFactory.getLogger(BasicCSPSolver.class);

    private final Problem problem;
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

        // Initialize current domains with full domains
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

        // Check if assignment is consistent with constraints
        if (!problem.isConsistent(assignment)) {
            logger.debug("Assignment inconsistent with constraints");
            failed = true;
            return false;
        }

        // Reduce domains based on constraints (forward checking)
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

    @Override
    public <T> Set<T> getCurrentDomain(Variable<T> variable) {
        @SuppressWarnings("unchecked")
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

        // If all variables in constraint are assigned, just check satisfaction
        if (assignedVars.size() == involvedVars.size()) {
            return constraint.isSatisfiedBy(assignment);
        }

        // Forward checking: reduce domains of unassigned variables
        for (Variable<?> var : involvedVars) {
            if (!assignment.isAssigned(var)) {
                if (!reduceDomain(var, constraint, assignment)) {
                    return false;
                }
            }
        }

        return true;
    }

    private <T> boolean reduceDomain(Variable<T> variable, Constraint constraint, Assignment assignment) {
        Set<T> currentDomain = getCurrentDomain(variable);

        // Filter out values that would violate the constraint
        Set<T> newDomain = currentDomain.stream()
                .filter(value -> {
                    // CRITICAL: Must snapshot to avoid mutating the real assignment during testing
                    Assignment testAssignment = assignment.snapshot();
                    testAssignment.assign(variable, value);
                    return constraint.isSatisfiedBy(testAssignment);
                })
                .collect(Collectors.toSet());

        // Update domain
        currentDomains.put(variable, newDomain);

        // Domain wipeout = failure
        return !newDomain.isEmpty();
    }
}
