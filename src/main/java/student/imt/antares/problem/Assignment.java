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
