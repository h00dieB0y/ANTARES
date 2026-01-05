package student.imt.antares.construction;

import java.util.List;
import java.util.Random;
import student.imt.antares.problem.Variable;
import java.util.Optional;

/**
 * Pre-configured variable selection strategies for CSP construction.
 * <p>
 * Variable ordering heuristics significantly impact search performance.
 * These strategies implement common approaches from the CSP literature.
 * </p>
 *
 * @since 1.0
 */
public class VariableSelectors {

    private static final Random RANDOM_INSTANCE = new Random();

    private VariableSelectors() {
        // Prevent instantiation
    }
    
    /**
     * Selects the unassigned variable with the smallest remaining domain (most constrained first).
     * <p>
     * This "fail-first" principle detects failures early by prioritizing variables
     * most likely to fail. Widely used in CSP solving due to strong empirical performance.
     * </p>
     */
    public static final VariableSelector SMALLEST_DOMAIN_FIRST = (problem, assignment, solver) -> problem.getVariables().stream()
            .filter(variable -> !assignment.isAssigned(variable))
            .min((v1, v2) -> Integer.compare(solver.getCurrentDomain(v1).size(), solver.getCurrentDomain(v2).size()));

    /**
     * Selects an unassigned variable uniformly at random.
     * <p>
     * Useful as a baseline or when no domain knowledge suggests a better ordering.
     * </p>
     */
    public static final VariableSelector RANDOM = (problem, assignment, solver) -> {
        List<Variable> unassigned = problem.getVariables().stream()
            .filter(variable -> !assignment.isAssigned(variable))
            .toList();

        if (unassigned.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(unassigned.get(RANDOM_INSTANCE.nextInt(unassigned.size())));
    };
}
