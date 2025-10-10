package student.imt.antares.problem;

import java.util.Set;

/**
 * CSP variable with a name and domain of possible values.
 * Identity is based on name only.
 */
public record Variable<T>(String name, Set<T> domain) {
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
