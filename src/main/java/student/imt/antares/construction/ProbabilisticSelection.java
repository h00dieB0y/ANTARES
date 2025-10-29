package student.imt.antares.construction;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Variable;

/**
 * Probabilistic value selection strategy using pheromone-weighted roulette wheel.
 * <p>
 * Selects values based on the ACO probability formula:
 * P(value) ∝ τ^α * η^β, where τ is pheromone level and η is heuristic information.
 * </p>
 * <p>
 * This implementation uses uniform heuristic (η = 1.0) as CSP problems typically
 * lack domain-specific value-ordering heuristics. Selection is proportional to
 * pheromone strength alone when beta = 0.
 * </p>
 *
 * @since 1.0
 */
public class ProbabilisticSelection {

    private final Random random;

    public ProbabilisticSelection() {
        this.random = new Random(25072001);
    }

    public ProbabilisticSelection(long seed) {
        this.random = new Random(seed);
    }

    public Optional<Integer> select(Variable variable, Set<Integer> domain, PheromoneMatrix pheromones,
            ACOParameters params) {

        if (domain == null || domain.isEmpty()) {
            return Optional.empty();
        }

        if (domain.size() == 1) {
            return Optional.of(domain.iterator().next());
        }

        return rouletteWheelSelection(variable, domain, pheromones, params);
    }

    /**
     * Performs fitness-proportional selection using weights without normalization.
     * <p>
     * More efficient than pre-computing all probabilities:
     * 1. Calculate sum of all weights (τ^α * η^β)
     * 2. Pick random value in [0, sum)
     * 3. Iterate through values, accumulating weights until threshold is reached
     * </p>
     */
    private Optional<Integer> rouletteWheelSelection(Variable variable, Set<Integer> domain,
            PheromoneMatrix pheromones, ACOParameters params) {

        double alpha = params.alpha();
        double beta = params.beta();

        // First pass: calculate sum of all weights
        double sumWeights = 0.0;
        for (Integer value : domain) {
            double tau = pheromones.getAmount(variable, value);
            double eta = 1.0;
            double weight = Math.pow(tau, alpha) * Math.pow(eta, beta);
            sumWeights += weight;
        }

        if (sumWeights == 0.0) {
            throw new IllegalStateException(
                "All pheromone weights are zero for variable: " + variable.name()
            );
        }

        // Second pass: select value using random threshold
        double randomThreshold = random.nextDouble() * sumWeights;
        double cumulativeWeight = 0.0;

        for (Integer value : domain) {
            double tau = pheromones.getAmount(variable, value);
            double eta = 1.0;
            double weight = Math.pow(tau, alpha) * Math.pow(eta, beta);
            cumulativeWeight += weight;

            if (randomThreshold <= cumulativeWeight) {
                return Optional.of(value);
            }
        }

        // This should never happen as randomThreshold < sumWeights
        throw new IllegalStateException(
            "Roulette wheel selection failed: threshold=" + randomThreshold +
            ", final cumulative=" + cumulativeWeight
        );
    }
}