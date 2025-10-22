package student.imt.antares.colony;

import java.util.*;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

/**
 * Mutable pheromone matrix tracking trail strengths for variable-value pairs.
 * Used in ant colony optimization to guide search toward promising solutions.
 * <p>
 * <strong>Mutability:</strong> Methods like {@link #evaporate}, {@link #deposit}, {@link #depositMultiple},
 * and {@link #clamp} modify the internal pheromone array in-place and return {@code this}
 * for fluent method chaining.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> This class is NOT thread-safe. Designed for single-threaded use only.
 * </p>
 */
public final class PheromoneMatrix {
    private static final Logger logger = LoggerFactory.getLogger(PheromoneMatrix.class);

    private final double[] pheromones;
    private final Map<Trail, Integer> trailToIndex;

    private PheromoneMatrix(double[] pheromones, Map<Trail, Integer> trailToIndex) {
        this.pheromones = pheromones;
        this.trailToIndex = trailToIndex;
    }

    public static PheromoneMatrix initialize(List<Variable<?>> variables, double initialPheromone) {
        validatePositive(initialPheromone, "Initial pheromone");

        Map<Trail, Integer> indexMap = new HashMap<>();
        int index = 0;

        for (Variable var : problem.getVariables()) {
            index = addTrailsForVariable(var, indexMap, index);
        }

        double[] pheromones = new double[index];
        Arrays.fill(pheromones, initialPheromone);

        return new PheromoneMatrix(pheromones, indexMap);
    }

    private static int addTrailsForVariable(Variable var, Map<Trail, Integer> indexMap, int startIndex) {
        int index = startIndex;
        for (Integer value : var.domain()) {
            indexMap.put(new Trail(var, value), index++);
        }
        return index;
    }

    public double getAmount(Variable variable, Integer value) {
        Trail trail = new Trail(variable, value);
        Integer index = trailToIndex.get(trail);
        return index != null ? pheromones[index] : 0.0;
    }

    /**
     * Evaporates pheromones by multiplying all values by (1 - evaporationRate).
     * Mutates this instance in-place.
     *
     * @param evaporationRate the fraction of pheromone to evaporate [0, 1]
     * @return this instance (for fluent method chaining)
     * @throws IllegalArgumentException if evaporationRate not in [0, 1]
     */
    public PheromoneMatrix evaporate(double evaporationRate) {
        if (evaporationRate < 0 || evaporationRate > 1) {
            throw new IllegalArgumentException("evaporation rate must be in [0, 1], got: " + evaporationRate);
        }
        double factor = 1.0 - evaporationRate;
        for (int i = 0; i < pheromones.length; i++) {
            pheromones[i] *= factor;
        }
        return this;
    }

    /**
     * Deposits pheromone on trails corresponding to assigned variable-value pairs.
     * Mutates this instance in-place.
     *
     * @param assignment the assignment whose trails receive pheromone
     * @param amount the amount of pheromone to add (must be positive)
     * @return this instance (for fluent method chaining)
     * @throws IllegalArgumentException if amount is not positive
     */
    public PheromoneMatrix deposit(Assignment assignment, double amount) {
        validatePositive(amount, "Deposit amount");

        depositOnTrails(assignment, amount);
        return this;
    }

    /**
     * Deposits pheromone for multiple assignments, calculating deposit amounts via a function.
     * Mutates this instance in-place.
     *
     * @param assignments the assignments whose trails receive pheromone
     * @param amountFunction function mapping each assignment to its deposit amount
     * @return this instance (for fluent method chaining)
     * @throws IllegalArgumentException if any calculated amount is not positive
     */
    public PheromoneMatrix depositMultiple(List<Assignment> assignments, Function<Assignment, Double> amountFunction) {
        for (Assignment assignment : assignments) {
            double amount = amountFunction.apply(assignment);
            validatePositive(amount, "Deposit amount");
            depositOnTrails(assignment, amount);
        }

        return this;
    }

    /**
     * Clamps all pheromone values to the range [minPheromone, maxPheromone].
     * Mutates this instance in-place.
     *
     * @param minPheromone minimum pheromone bound (must be non-negative)
     * @param maxPheromone maximum pheromone bound (must be >= minPheromone)
     * @return this instance (for fluent method chaining)
     * @throws IllegalArgumentException if bounds are invalid
     */
    public PheromoneMatrix clamp(double minPheromone, double maxPheromone) {
        if (minPheromone < 0 || maxPheromone < minPheromone) {
            throw new IllegalArgumentException(
                "Invalid bounds: min=" + minPheromone + ", max=" + maxPheromone);
        }

        for (int i = 0; i < pheromones.length; i++) {
            pheromones[i] = Math.clamp(pheromones[i], minPheromone, maxPheromone);
        }

        return this;
    }

    private void depositOnTrails(Assignment assignment, double amount) {
        for (Variable var : assignment.getAssignedVariables()) {
            assignment.getValue(var).ifPresent(value -> {
                Trail trail = new Trail(var, value);
                Integer index = trailToIndex.get(trail);
                if (index != null) {
                    pheromones[index] += amount;
                }
            });
        }
    }

    private static void validatePositive(double value, String paramName) {
        if (value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive, got: " + value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PheromoneMatrix that = (PheromoneMatrix) obj;
        return Arrays.equals(pheromones, that.pheromones) &&
               trailToIndex.equals(that.trailToIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(pheromones), trailToIndex);
    }

    @Override
    public String toString() {
        return "PheromoneMatrix{" + pheromones.length + " trails}";
    }

    private record Trail(Variable variable, Integer value) {
        public Trail {
            if (variable == null || value == null) {
                throw new IllegalArgumentException("Variable and value cannot be null");
            }
            if (!variable.domain().contains(value)) {
                throw new IllegalArgumentException(
                    "Value " + value + " not in domain of variable " + variable.name());
            }
        }

        @Override
        public String toString() {
            return String.format("(%s=%s)", variable.name(), value);
        }
    }
}
