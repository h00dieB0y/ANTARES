package student.imt.antares.solver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Constraint;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

public class BasicCSPSolver {

    private final Problem problem;
    private Map<Variable, Set<Integer>> currentDomains;
    private boolean failed;

    public BasicCSPSolver(Problem problem) {
        this.problem = Objects.requireNonNull(problem, "Problem cannot be null");
        this.currentDomains = new HashMap<>();
        this.failed = false;
        reset();
    }

    public void reset() {
        currentDomains.clear();
        failed = false;

        for (Variable problemVar : problem.getVariables()) {
            currentDomains.put(problemVar, Set.copyOf(problemVar.domain()));
        }
    }

    public boolean propagate(Assignment assignment) {
        if (failed) {
            return false;
        }

        if (!problem.isConsistent(assignment)) {
            failed = true;
            return false;
        }

        for (Constraint constraint : problem.getConstraints()) {
            if (!propagateConstraint(constraint, assignment)) {
                failed = true;
                return false;
            }
        }

        return true;
    }

    public Set<Integer> getCurrentDomain(Variable variable) {
        return currentDomains.getOrDefault(variable, Set.of());
    }

    public boolean hasFailed() {
        return failed;
    }

    public List<Variable> getSingletonVariables() {
        return currentDomains.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .map(Map.Entry::getKey)
                .toList();
    }

    private boolean propagateConstraint(Constraint constraint, Assignment assignment) {
        Set<Variable> involvedVars = constraint.getInvolvedVariables();
        Set<Variable> assignedVars = involvedVars.stream()
                .filter(assignment::isAssigned)
                .collect(Collectors.toSet());

        if (assignedVars.size() == involvedVars.size()) {
            return constraint.isSatisfiedBy(assignment);
        }

        for (Variable involvedVar : involvedVars) {
            if (!assignment.isAssigned(involvedVar) &&
                    !reduceDomain(involvedVar, constraint, assignment)) {
                return false;
            }
        }

        return true;
    }

    private boolean reduceDomain(Variable variable, Constraint constraint, Assignment assignment) {
        Set<Integer> currentDomain = getCurrentDomain(variable);

        Set<Integer> newDomain = currentDomain.stream()
                .filter(value -> {
                    assignment.assign(variable, value);
                    boolean satisfied = constraint.isSatisfiedBy(assignment);
                    assignment.unassign(variable);
                    return satisfied;
                })
                .collect(Collectors.toSet());

        currentDomains.put(variable, newDomain);

        return !newDomain.isEmpty();
    }
}
