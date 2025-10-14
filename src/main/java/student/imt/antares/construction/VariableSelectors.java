package student.imt.antares.construction;

public class VariableSelectors {
    // Smallest Domain First selector
    public static final VariableSelector SMALLEST_DOMAIN_FIRST = (problem, assignment, solver) -> problem.getVariables().stream()
            .filter(var -> !assignment.isAssigned(var))
            .min((v1, v2) -> Integer.compare(solver.getCurrentDomain(v1).size(), solver.getCurrentDomain(v2).size()));
}
