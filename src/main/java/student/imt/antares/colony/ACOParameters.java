package student.imt.antares.colony;

/**
 * ACO algorithm parameters for pheromone update and ant behavior.
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

    public static ACOParameters withDefaults() {
        return new ACOParameters(
                2.0,  // alpha - strong pheromone importance
                0.0,  // beta - no heuristic
                0.01, // rho - slow evaporation (1%)
                0.01, // tauMin - minimum bound
                10.0, // tauMax - maximum bound
                30    // numberOfAnts - standard colony size
        );
    }

    @Override
    public String toString() {
        return String.format(
                "ACOParameters{α=%.2f, β=%.2f, ρ=%.3f, τ∈[%.3f,%.2f], ants=%d}",
                alpha, beta, rho, tauMin, tauMax, numberOfAnts);
    }
}
