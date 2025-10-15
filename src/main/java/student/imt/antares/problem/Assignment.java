package student.imt.antares.problem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable mapping from variables to their assigned values.
 * Supports partial assignments during search and complete solutions.
 *
 * <h2>Performance Design</h2>
 * Mutates in-place to avoid allocations during search. Use {@link #snapshot()}
 * when storing assignments that must remain independent.
 *
 * <h2>Type Safety Design</h2>
 * Internally uses {@code Map<Variable<?>, Object>} which loses the type correlation
 * between {@code Variable<T>} and its value of type {@code T}. This design trade-off:
 * <ul>
 *   <li><b>Allows</b> heterogeneous variables (Integer, String, etc.) in one assignment</li>
 *   <li><b>Maintains</b> type safety at the public API level via generic methods</li>
 *   <li><b>Requires</b> unchecked cast in {@link #getValue(Variable)} (safe due to API constraints)</li>
 *   <li><b>Avoids</b> complex type token patterns that would add significant overhead</li>
 * </ul>
 *
 * <h3>Why the unchecked cast is safe:</h3>
 * The only way to insert values is via {@link #assign(Variable, Object)} which enforces
 * that a {@code Variable<T>} can only be assigned a value of type {@code T}. Therefore,
 * when retrieving via {@link #getValue(Variable)}, the cast to {@code T} is guaranteed
 * to succeed (assuming no external mutation of the internal map, which is prevented by
 * encapsulation).
 *
 * @see Variable
 */
public final class Assignment {
    /**
     * Internal storage using Object values to allow heterogeneous variable types.
     * Type safety is maintained through the generic public API methods.
     */
    private final Map<Variable<?>, Object> assignments;

    private Assignment(Map<Variable<?>, Object> assignments) {
        this.assignments = assignments;
    }

    public static Assignment empty() {
        return new Assignment(new HashMap<>());
    }

    public static Assignment of(Map<Variable<?>, Object> assignments) {
        return new Assignment(new HashMap<>(assignments));
    }

    public <T> Assignment assign(Variable<T> variable, T value) {
        assignments.put(variable, value);
        return this;
    }

    public Assignment unassign(Variable<?> variable) {
        assignments.remove(variable);
        return this;
    }

    /**
     * Creates an independent copy of this assignment.
     * Use when storing assignments that must not be affected by future mutations.
     */
    public Assignment snapshot() {
        return new Assignment(new HashMap<>(assignments));
    }

    /**
     * Retrieves the value assigned to a variable.
     *
     * @param variable the variable to look up
     * @param <T> the type of the variable's domain
     * @return Optional containing the assigned value, or empty if unassigned
     *
     * @implNote Contains an unchecked cast from Object to T, which is safe because
     * {@link #assign(Variable, Object)} enforces type consistency at insertion time.
     */
    public <T> Optional<T> getValue(Variable<T> variable) {
        @SuppressWarnings("unchecked") // Safe: assign() enforces Variable<T> -> T relationship
        T value = (T) assignments.get(variable);
        return Optional.ofNullable(value);
    }

    public boolean isAssigned(Variable<?> variable) {
        return assignments.containsKey(variable);
    }

    public Set<Variable<?>> getAssignedVariables() {
        return assignments.keySet();
    }

    public int size() {
        return assignments.size();
    }

    public boolean isComplete(int totalVariables) {
        return size() == totalVariables;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Assignment that = (Assignment) obj;
        return assignments.equals(that.assignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignments);
    }

    @Override
    public String toString() {
        return "Assignment{" + assignments + "}";
    }
}
