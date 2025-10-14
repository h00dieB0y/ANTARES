package student.imt.antares.construction;

import java.util.List;
import java.util.Random;
import student.imt.antares.problem.Variable;
import java.util.Optional;

public class VariableSelectors {

    // Smallest Domain First selector
    public static final VariableSelector SMALLEST_DOMAIN_FIRST = (problem, assignment, solver) -> problem.getVariables().stream()
            .filter(var -> !assignment.isAssigned(var))
            .min((v1, v2) -> Integer.compare(solver.getCurrentDomain(v1).size(), solver.getCurrentDomain(v2).size()));

    public static final VariableSelector RANDOM = (problem, assignment, solver) -> {
        List<Variable<?>> unassigned = problem.getVariables().stream()
            .filter(var -> !assignment.isAssigned(var))
            .toList();

        if (unassigned.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(unassigned.get(new Random().nextInt(unassigned.size())));
    };
}
