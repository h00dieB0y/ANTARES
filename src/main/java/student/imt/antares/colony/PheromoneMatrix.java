package student.imt.antares.colony;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

/**
 * Mutable pheromone matrix tracking trail strengths for variable-value pairs.
 * Used in ant colony optimization to guide search toward promising solutions.
 *
 * Performance: Uses flat double array for cache efficiency and in-place mutations.
 */
public final class PheromoneMatrix {
    private static final Logger logger = LoggerFactory.getLogger(PheromoneMatrix.class);

    private final double[] pheromones;
    private final Map<Trail<?>, Integer> trailToIndex;

    private PheromoneMatrix(double[] pheromones, Map<Trail<?>, Integer> trailToIndex) {
        this.pheromones = pheromones;
        this.trailToIndex = trailToIndex;
    }

    public static PheromoneMatrix initialize(Problem problem, double initialPheromone) {
        validatePositive(initialPheromone, "Initial pheromone");

        Map<Trail<?>, Integer> indexMap = new HashMap<>();
        int index = 0;

        for (Variable<?> var : problem.getVariables()) {
            index = addTrailsForVariable(var, indexMap, index);
        }

        double[] pheromones = new double[index];
        for (int i = 0; i < pheromones.length; i++) {
            pheromones[i] = initialPheromone;
        }

        logger.info("Initialized pheromone matrix: {} trails with value {}",
                   pheromones.length, initialPheromone);

        return new PheromoneMatrix(pheromones, indexMap);
    }

    private static <T> int addTrailsForVariable(Variable<T> var, Map<Trail<?>, Integer> indexMap, int startIndex) {
        int index = startIndex;
        for (T value : var.domain()) {
            indexMap.put(new Trail<>(var, value), index++);
        }
        return index;
    }

    public <T> double getAmount(Variable<T> variable, T value) {
        Trail<T> trail = new Trail<>(variable, value);
        Integer index = trailToIndex.get(trail);
        return index != null ? pheromones[index] : 0.0;
    }

    public PheromoneMatrix evaporate(double evaporationRate) {
        validateRange(evaporationRate, "Evaporation rate");

        double factor = 1.0 - evaporationRate;
        for (int i = 0; i < pheromones.length; i++) {
            pheromones[i] *= factor;
        }
        return this;
    }

    public PheromoneMatrix deposit(Assignment assignment, double amount) {
        validatePositive(amount, "Deposit amount");

        depositOnTrails(assignment, amount);
        return this;
    }

    public PheromoneMatrix depositMultiple(List<Assignment> assignments, Function<Assignment, Double> amountFunction) {
        for (Assignment assignment : assignments) {
            double amount = amountFunction.apply(assignment);
            validatePositive(amount, "Deposit amount");
            depositOnTrails(assignment, amount);
        }

        return this;
    }

    public PheromoneMatrix clamp(double minPheromone, double maxPheromone) {
        if (minPheromone < 0 || maxPheromone < minPheromone) {
            throw new IllegalArgumentException(
                "Invalid bounds: min=" + minPheromone + ", max=" + maxPheromone);
        }

        for (int i = 0; i < pheromones.length; i++) {
            pheromones[i] = Math.min(maxPheromone, Math.max(minPheromone, pheromones[i]));
        }

        return this;
    }

    private void depositOnTrails(Assignment assignment, double amount) {
        for (Variable<?> var : assignment.getAssignedVariables()) {
            depositForVariable(var, assignment, amount);
        }
    }

    private <T> void depositForVariable(Variable<T> var, Assignment assignment, double amount) {
        assignment.getValue(var).ifPresent(value -> {
            Trail<T> trail = new Trail<>(var, value);
            Integer index = trailToIndex.get(trail);
            if (index != null) {
                pheromones[index] += amount;
            }
        });
    }

    private static void validatePositive(double value, String paramName) {
        if (value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive, got: " + value);
        }
    }

    private static void validateRange(double value, String paramName) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(paramName + " must be in [0, 1], got: " + value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PheromoneMatrix that = (PheromoneMatrix) obj;
        return java.util.Arrays.equals(pheromones, that.pheromones) &&
               trailToIndex.equals(that.trailToIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(java.util.Arrays.hashCode(pheromones), trailToIndex);
    }

    @Override
    public String toString() {
        return "PheromoneMatrix{" + pheromones.length + " trails}";
    }

    private static record Trail<T>(Variable<T> var, T value) {
        public Trail {
            if (var == null || value == null) {
                throw new IllegalArgumentException("Variable and value cannot be null");
            }
            if (!var.domain().contains(value)) {
                throw new IllegalArgumentException(
                    "Value " + value + " not in domain of variable " + var.name());
            }
        }

        @Override
        public String toString() {
            return String.format("(%s=%s)", var.name(), value);
        }
    }
}
