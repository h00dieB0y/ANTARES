package student.imt.antares.problem;

import java.util.Set;

/**
 * Represents a variable in a Constraint Satisfaction Problem (CSP).
 * <p>
 * Each variable has a unique name and a domain of possible integer values.
 * Variables are immutable value objects with identity based solely on their name,
 * allowing different instances with the same name to be considered equal.
 * </p>
 * <p>
 * This implementation is monomorphized to Integer domains, reflecting the actual
 * usage across all problem types in the framework (Sudoku, TSP, VRP, Scheduling).
 * </p>
 *
 * @param name unique identifier for this variable (cannot be null or blank)
 * @param domain set of possible integer values this variable can take (immutable, non-empty)
 *
 * @since 1.0
 */
public record Variable(String name, Set<Integer> domain) {
    public Variable {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Variable name cannot be null or empty");
        }
        if (domain == null || domain.isEmpty()) {
            throw new IllegalArgumentException("Variable domain cannot be null or empty");
        }
        domain = Set.copyOf(domain);
    }
}
