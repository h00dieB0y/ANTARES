package student.imt.antares.problem;

import java.util.Set;

/**
 * Validates assignments and enforces relationships between variables.
 */
public interface Constraint {

    boolean isSatisfiedBy(Assignment assignment);

    Set<Variable<?>> getInvolvedVariables();
}
