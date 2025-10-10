package student.imt.antares.problem;

import java.util.List;

/**
 * CSP with variables and constraints.
 * Enforces: at least one variable, at least one constraint.
 */
public class Problem {
    private final List<Variable<?>> variables;
    private final List<Constraint> constraints;

    public Problem(List<Variable<?>> variables, List<Constraint> constraints) {
        if (variables == null || variables.isEmpty()) {
            throw new IllegalArgumentException("Problem must have at least one variable");
        }
        if (constraints == null) {
            throw new IllegalArgumentException("Constraints cannot be null");
        }
        this.variables = List.copyOf(variables);
        this.constraints = List.copyOf(constraints);
    }

    public List<Variable<?>> getVariables() {
        return variables;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public int size() {
        return variables.size();
    }

    public boolean isConsistent(Assignment assignment) {
        return constraints.stream()
                .filter(constraint -> constraint.getInvolvedVariables().stream()
                        .allMatch(assignment::isAssigned))
                .allMatch(constraint -> constraint.isSatisfiedBy(assignment));
    }

    public boolean isSolution(Assignment assignment) {
        return assignment.isComplete(size()) && isConsistent(assignment);
    }
}
