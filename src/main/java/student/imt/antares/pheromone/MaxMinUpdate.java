package student.imt.antares.pheromone;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Assignment;

/**
 * Max-Min Ant System (MMAS) pheromone update strategy.
 * All best solutions from the cycle deposit pheromone (BestOfCycle), and pheromone levels
 * are clamped to [τ_min, τ_max] to avoid premature convergence.
 * Implements Ant-CP algorithm line 11: update using all max-size assignments.
 */
public class MaxMinUpdate implements PheromoneUpdater {
    private static final Logger logger = LoggerFactory.getLogger(MaxMinUpdate.class);

    @Override
    public PheromoneMatrix update(PheromoneMatrix currentPheromones,
                                  List<Assignment> cycleAssignments,
                                  Assignment bestOverall,
                                  ACOParameters parameters) {

        Objects.requireNonNull(currentPheromones, "Pheromone matrix cannot be null");
        Objects.requireNonNull(cycleAssignments, "Cycle assignments cannot be null");
        Objects.requireNonNull(bestOverall, "Best overall assignment cannot be null");
        Objects.requireNonNull(parameters, "ACO parameters cannot be null");

        logger.debug("Updating pheromones: {} assignments in cycle, best overall: {} vars",
                    cycleAssignments.size(), bestOverall.size());

        PheromoneMatrix afterEvaporation = currentPheromones.evaporate(parameters.rho());
        logger.trace("Evaporation complete with rate {}", parameters.rho());

        // Find ALL assignments with maximum size (BestOfCycle as per Ant-CP algorithm)
        List<Assignment> bestOfCycle = findBestAssignments(cycleAssignments);

        if (bestOfCycle.isEmpty()) {
            logger.debug("No valid assignments in cycle - no deposit");
            return afterEvaporation.clamp(parameters.tauMin(), parameters.tauMax());
        }

        logger.debug("BestOfCycle contains {} assignments with size {}",
                    bestOfCycle.size(), bestOfCycle.get(0).size());

        // Deposit from ALL best assignments (their Δτ contributions are summed)
        PheromoneMatrix afterDeposit = afterEvaporation.depositMultiple(
            bestOfCycle,
            assignment -> calculateDepositAmount(assignment, bestOverall)
        );

        logger.trace("Clamping pheromones to [{}, {}]", parameters.tauMin(), parameters.tauMax());
        return afterDeposit.clamp(parameters.tauMin(), parameters.tauMax());
    }

    /**
     * Finds all assignments with maximum size in the cycle (BestOfCycle).
     * According to Ant-CP: BestOfCycle = {A_i | |var(A_i)| is maximal}
     *
     * @param assignments All assignments from current cycle
     * @return List of all assignments with maximum size
     */
    private List<Assignment> findBestAssignments(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return List.of();
        }

        // Find maximum size
        int maxSize = assignments.stream()
                .mapToInt(Assignment::size)
                .max()
                .orElse(0);

        if (maxSize == 0) {
            return List.of();
        }

        // Collect ALL assignments with max size
        List<Assignment> bestOnes = assignments.stream()
                .filter(a -> a.size() == maxSize)
                .collect(Collectors.toList());

        logger.trace("Found {} assignments with max size {}", bestOnes.size(), maxSize);
        return bestOnes;
    }

    /**
     * Calculates Δτ (pheromone deposit amount) for an assignment.
     * According to Ant-CP: Δτ(A_k) = 1/(1 + |A_best| - |A_k|)
     * where A_best is the global best assignment since search start.
     *
     * @param assignment Assignment to calculate deposit for
     * @param bestOverall Best assignment found since search start
     * @return Δτ value for this assignment
     */
    private double calculateDepositAmount(Assignment assignment, Assignment bestOverall) {
        int gap = bestOverall.size() - assignment.size();

        // Defensive: should never be negative if Colony updates bestOverall correctly
        if (gap < 0) {
            logger.warn("Assignment size {} exceeds best overall {} - possible race condition",
                       assignment.size(), bestOverall.size());
            gap = 0; // Treat as equal to best
        }

        double delta = 1.0 / (1.0 + gap);
        logger.trace("Δτ = {:.4f} for assignment size {} (gap={})", delta, assignment.size(), gap);
        return delta;
    }
}
