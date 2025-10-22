package student.imt.antares.problem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable mapping from variables to their assigned integer values.
 * Supports partial assignments during search and complete solutions.
 *
 * @see Variable
 */
public final class Assignment {
    private final Map<Variable, Integer> assignments;

    private Assignment(Map<Variable, Integer> assignments) {
        this.assignments = assignments;
    }

    public static Assignment empty() {
        return new Assignment(new HashMap<>());
    }

    public static Assignment of(Map<Variable, Integer> assignments) {
        return new Assignment(new HashMap<>(assignments));
    }

    /**
     * Assigns an integer value to a variable, mutating this instance.
     *
     * @param variable the variable to assign
     * @param value the integer value to assign to the variable
     * @return this instance (for fluent method chaining)
     */
    public Assignment assign(Variable variable, Integer value) {
        assignments.put(variable, value);
        return this;
    }

    /**
     * Removes an assignment for a variable, mutating this instance.
     *
     * @param variable the variable to unassign
     * @return this instance (for fluent method chaining)
     */
    public Assignment unassign(Variable variable) {
        assignments.remove(variable);
        return this;
    }

    /**
     * Creates an independent copy of this assignment (defensive copy).
     * <p>
     * Use when storing assignments that must not be affected by future mutations,
     * such as when saving best solutions in Colony or caching intermediate states.
     * </p>
     *
     * @return a new Assignment instance with a copy of the current assignments
     */
    public Assignment snapshot() {
        return new Assignment(new HashMap<>(assignments));
    }

    /**
     * Retrieves the integer value assigned to a variable.
     *
     * @param variable the variable to look up
     * @return Optional containing the assigned integer value, or empty if unassigned
     */
    public Optional<Integer> getValue(Variable variable) {
        return Optional.ofNullable(assignments.get(variable));
    }

    public boolean isAssigned(Variable variable) {
        return assignments.containsKey(variable);
    }

    public Set<Variable> getAssignedVariables() {
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
