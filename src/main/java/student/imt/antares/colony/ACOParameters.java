package student.imt.antares.colony;

/**
 * Configuration parameters for the Ant Colony Optimization (ACO) algorithm.
 * <p>
 * These parameters control pheromone influence, heuristic weighting,
 * evaporation rate, and pheromone bounds following the MAX-MIN Ant System (MMAS) approach.
 * </p>
 *
 * @param alpha pheromone importance weight (≥ 0). Higher values increase exploitation
 *              of existing pheromone trails. Typical range: [1.0, 5.0]
 * @param beta heuristic information importance weight (≥ 0). Higher values increase
 *             exploitation of problem-specific heuristics. Use 0.0 when no heuristic available
 * @param rho pheromone evaporation rate [0, 1]. Fraction of pheromone that evaporates
 *            each cycle. Low values (0.01-0.1) provide more exploration. Standard: 0.01 for CSP
 * @param tauMin minimum pheromone bound (> 0). Prevents starvation of trails
 * @param tauMax maximum pheromone bound (> tauMin). Prevents premature convergence
 * @param numberOfAnts size of the ant colony (> 0). More ants increase diversity but
 *                     require more computation per cycle
 *
 * @since 1.0
 */
public record ACOParameters(
        double alpha,
        double beta,
        double rho,
        double tauMin,
        double tauMax,
        int numberOfAnts) {

    public ACOParameters {
        if (alpha < 0 || beta < 0 ) {
            throw new IllegalArgumentException(
                    "Both alpha and beta must be positive ! (alpha = " + alpha + ", beta = " + beta + ")"
            );
        }
        if (rho < 0 || rho > 1) {
            throw new IllegalArgumentException(
                    "Rho must be in [0, 1]"
            );
        }
        if (tauMin <= 0 || tauMax <= 0) {
            throw new IllegalArgumentException(
                    "Both tauMin and tauMax must be greater than 0 ! (tauMin = " + tauMin + ", tauMax = " + tauMax + ")"
            );
        }
        if (tauMin >= tauMax) {
            throw new IllegalArgumentException(
                    "TauMin must be less than TauMax"
            );
        }
        if (numberOfAnts <= 0) {
            throw new IllegalArgumentException(
                    "Number of ants must be positive"
            );
        }
    }

    /**
     * Creates ACOParameters with research-validated defaults for CSP solving.
     * <p>
     * These defaults prioritize pheromone guidance with slow evaporation,
     * suitable for constraint satisfaction problems without domain-specific heuristics.
     * </p>
     *
     * @return default parameter configuration
     */
    public static ACOParameters withDefaults() {
        return new ACOParameters(
                2.0,
                0.0,
                0.01,
                0.01,
                10.0,
                30
        );
    }

    @Override
    public String toString() {
        return String.format(
                "ACOParameters{α=%.2f, β=%.2f, ρ=%.3f, τ∈[%.3f,%.2f], ants=%d}",
                alpha, beta, rho, tauMin, tauMax, numberOfAnts);
    }
}
