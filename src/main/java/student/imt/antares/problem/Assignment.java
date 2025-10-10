package student.imt.antares.problem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable mapping from variables to their assigned values.
 * Supports partial assignments during search and complete solutions.
 */
public final class Assignment {
    private final Map<Variable<?>, Object> assignments;

    private Assignment(Map<Variable<?>, Object> assignments) {
        this.assignments = Map.copyOf(assignments);
    }

    public static Assignment empty() {
        return new Assignment(Map.of());
    }

    public static Assignment of(Map<Variable<?>, Object> assignments) {
        return new Assignment(assignments);
    }

    public <T> Assignment assign(Variable<T> variable, T value) {
        Map<Variable<?>, Object> newAssignments = new HashMap<>(assignments);
        newAssignments.put(variable, value);
        return new Assignment(newAssignments);
    }

    public <T> Optional<T> getValue(Variable<T> variable) {
        @SuppressWarnings("unchecked")
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
