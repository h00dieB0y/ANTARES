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
 * Immutable pheromone matrix tracking trail strengths for variable-value pairs.
 * Used in ant colony optimization to guide search toward promising solutions.
 */
public final class PheromoneMatrix {
    private static final Logger logger = LoggerFactory.getLogger(PheromoneMatrix.class);

    private final Map<Trail<?>, Double> trails;

    private PheromoneMatrix(Map<Trail<?>, Double> trails) {
        this.trails = Map.copyOf(trails);
    }

    public static PheromoneMatrix initialize(Problem problem, double initialPheromone) {
        validatePositive(initialPheromone, "Initial pheromone");

        Map<Trail<?>, Double> initialTrails = new HashMap<>();
        for (Variable<?> var : problem.getVariables()) {
            addTrailsForVariable(var, initialTrails, initialPheromone);
        }

        logger.info("Initialized pheromone matrix: {} trails with value {}",
                   initialTrails.size(), initialPheromone);

        return new PheromoneMatrix(initialTrails);
    }

    private static <T> void addTrailsForVariable(Variable<T> var, Map<Trail<?>, Double> trails, double amount) {
        for (T value : var.domain()) {
            trails.put(new Trail<>(var, value), amount);
        }
    }

    public <T> double getAmount(Variable<T> variable, T value) {
        Trail<T> trail = new Trail<>(variable, value);
        return trails.getOrDefault(trail, 0.0);
    }

    public PheromoneMatrix evaporate(double evaporationRate) {
        validateRange(evaporationRate, "Evaporation rate");

        Map<Trail<?>, Double> newTrails = new HashMap<>();
        for (Map.Entry<Trail<?>, Double> entry : trails.entrySet()) {
            newTrails.put(entry.getKey(), entry.getValue() * (1 - evaporationRate));
        }
        return new PheromoneMatrix(newTrails);
    }

    public PheromoneMatrix deposit(Assignment assignment, double amount) {
        validatePositive(amount, "Deposit amount");

        Map<Trail<?>, Double> newTrails = new HashMap<>(trails);
        depositOnTrails(newTrails, assignment, amount);
        return new PheromoneMatrix(newTrails);
    }

    public PheromoneMatrix depositMultiple(List<Assignment> assignments, Function<Assignment, Double> amountFunction) {
        Map<Trail<?>, Double> newTrails = new HashMap<>(trails);

        for (Assignment assignment : assignments) {
            double amount = amountFunction.apply(assignment);
            validatePositive(amount, "Deposit amount");
            depositOnTrails(newTrails, assignment, amount);
        }

        return new PheromoneMatrix(newTrails);
    }

    public PheromoneMatrix clamp(double minPheromone, double maxPheromone) {
        if (minPheromone < 0 || maxPheromone < minPheromone) {
            throw new IllegalArgumentException(
                "Invalid bounds: min=" + minPheromone + ", max=" + maxPheromone);
        }

        Map<Trail<?>, Double> clampedTrails = new HashMap<>();
        for (Map.Entry<Trail<?>, Double> entry : trails.entrySet()) {
            double clamped = Math.min(maxPheromone, Math.max(minPheromone, entry.getValue()));
            clampedTrails.put(entry.getKey(), clamped);
        }

        return new PheromoneMatrix(clampedTrails);
    }

    private void depositOnTrails(Map<Trail<?>, Double> trails, Assignment assignment, double amount) {
        for (Variable<?> var : assignment.getAssignedVariables()) {
            depositForVariable(var, trails, assignment, amount);
        }
    }

    private <T> void depositForVariable(Variable<T> var, Map<Trail<?>, Double> trails,
                                        Assignment assignment, double amount) {
        assignment.getValue(var).ifPresent(value -> {
            Trail<T> trail = new Trail<>(var, value);
            trails.merge(trail, amount, Double::sum);
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
        return trails.equals(that.trails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trails);
    }

    @Override
    public String toString() {
        return "PheromoneMatrix{" + trails + "}";
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
