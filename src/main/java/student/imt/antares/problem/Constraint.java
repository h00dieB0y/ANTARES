package student.imt.antares.problem;

import java.util.Set;

/**
 * Enforces relationships and restrictions between CSP variables.
 * <p>
 * Constraints define the rules that valid solutions must satisfy.
 * They can involve one or more variables and are evaluated during
 * the search process to prune invalid partial assignments.
 * </p>
 *
 * @since 1.0
 */
public interface Constraint {

    /**
     * Checks whether this constraint is satisfied by the given assignment.
     * <p>
     * If not all involved variables are assigned, the constraint may return
     * {@code true} to indicate it's not yet violated (optimistic evaluation).
     * </p>
     *
     * @param assignment the current variable assignments to evaluate
     * @return {@code true} if the constraint is satisfied or not yet violated,
     *         {@code false} if the constraint is definitively violated
     */
    boolean isSatisfiedBy(Assignment assignment);

    /**
     * Returns all variables that participate in this constraint.
     * <p>
     * Used by solvers to determine which constraints need re-evaluation
     * after a variable assignment.
     * </p>
     *
     * @return set of variables involved in this constraint (never null or empty)
     */
    Set<Variable<?>> getInvolvedVariables();
}
