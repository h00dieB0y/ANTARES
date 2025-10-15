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
public class ProbabilisticSelection implements ValueSelector {
    
    private final Random random;

    public ProbabilisticSelection() {
        this.random = new Random();
    }

    public ProbabilisticSelection(long seed) {
        this.random = new Random(seed);
    } 

    @Override
    public <T> Optional<T> select(Variable<T> variable, Set<T> domain, PheromoneMatrix pheromones,
            ACOParameters params) {

        if (domain == null || domain.isEmpty()) {
            return Optional.empty();
        }

        if (domain.size() == 1) {
            return Optional.of(domain.iterator().next());
        }

        Map<T, Double> probabilities = calculateProbabilities(variable, domain, pheromones, params);

        return rouletteWheelSelection(probabilities);
    }


    private <T> Map<T, Double> calculateProbabilities(Variable<T> variable, Set<T> domain, PheromoneMatrix pheromones, ACOParameters params) {
        double alpha = params.alpha();
        double beta = params.beta();

        double sum = domain.stream()
                .mapToDouble(value -> {
                    double tau = pheromones.getAmount(variable, value);
                    double eta = 1.0;
                    return Math.pow(tau, alpha) * Math.pow(eta, beta);
                })
                .sum();

        return domain.stream()
                .collect(Collectors.toMap(
                    value -> value,
                    value -> {
                        double tau = pheromones.getAmount(variable, value);
                        double eta = 1.0;
                        return (Math.pow(tau, alpha) * Math.pow(eta, beta)) / sum;
                    }
                ));

    }

    private <T> Optional<T> rouletteWheelSelection(Map<T, Double> probabilities) {
        if (probabilities.isEmpty()) {
            return Optional.empty();
        }

        double rand = random.nextDouble();
        double cumulativeProb = 0.0;

        for (Map.Entry<T, Double> entry : probabilities.entrySet()) {
            cumulativeProb += entry.getValue();
            if (rand <= cumulativeProb) {
                return Optional.of(entry.getKey());
            }
        }

        // Fallback: return last element if rounding errors occur
        return Optional.of(probabilities.keySet().iterator().next());
    }
}