package student.imt.antares.problem;

import java.util.List;

/**
 * Represents a Constraint Satisfaction Problem (CSP).
 * A CSP consists of a set of variables and constraints that must be satisfied.
 * This class acts as the aggregate root for the CSP domain model, providing
 * operations to validate partial and complete assignments.
 * Invariants: Must have at least one variable. Constraints list cannot be null
 * (but can be empty for unconstrained problems).
 *
 * @since 1.0
 */
public class Problem {
    private final List<Variable> variables;
    private final List<Constraint> constraints;

    public Problem(List<Variable> variables, List<Constraint> constraints) {
        if (variables == null || variables.isEmpty()) {
            throw new IllegalArgumentException("Problem must have at least one variable");
        }
        if (constraints == null) {
            throw new IllegalArgumentException("Constraints cannot be null");
        }
        this.variables = List.copyOf(variables);
        this.constraints = List.copyOf(constraints);
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    /**
     * Returns the number of variables in this problem.
     *
     * @return the variable count
     */
    public int size() {
        return variables.size();
    }

    /**
     * Checks if an assignment satisfies all applicable constraints.
     * Only constraints whose involved variables are all assigned are evaluated.
     * Partial assignments are considered consistent if they don't violate
     * any fully-instantiated constraints.
     *
     * @param assignment the assignment to validate
     * @return {@code true} if no constraints are violated, {@code false} otherwise
     */
    public boolean isConsistent(Assignment assignment) {
        return constraints.stream()
                .filter(constraint -> constraint.getInvolvedVariables().stream()
                        .allMatch(assignment::isAssigned))
                .allMatch(constraint -> constraint.isSatisfiedBy(assignment));
    }

    /**
     * Determines whether an assignment is a valid complete solution.
     * <p>
     * A solution must assign all variables and satisfy all constraints.
     * </p>
     *
     * @param assignment the assignment to check
     * @return {@code true} if the assignment is complete and consistent
     */
    public boolean isSolution(Assignment assignment) {
        return assignment.isComplete(size()) && isConsistent(assignment);
    }
}
