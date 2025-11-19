package student.imt.antares.solver;

import org.chocosolver.solver.*;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.monitors.*;
import org.chocosolver.solver.search.restart.*;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.construction.ProbabilisticSelection;
import student.imt.antares.pheromone.MaxMinUpdate;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

import java.util.*;

public class AntColonyMetaHeuristicSolver extends AbstractStrategy<IntVar>
        implements IMonitorRestart, IMonitorSolution, IMonitorContradiction {

    private final Model model;
    private final VariableSelector<IntVar> variableSelector;
    private final IntVarAdapter adapter;
    private final ACOParameters parameters;

    private final PheromoneMatrix pheromones;
    private final ProbabilisticSelection valueSelector;
    private final MaxMinUpdate pheromoneUpdater;

    private int currentAnt;
    private int currentCycle;

    private List<ChocoDecision> currentPath;

    private List<Assignment> cycleSolutions;

    private Assignment bestOverall;
    private boolean solutionFoundByCurrentAnt;
    public AntColonyMetaHeuristicSolver(IntVar[] variables,
            VariableSelector<IntVar> variableSelector,
            ACOParameters parameters) {
        super(variables);
        this.model = variables[0].getModel();
        this.variableSelector = variableSelector;
        this.parameters = parameters;

        this.adapter = new IntVarAdapter(variables);

        this.pheromones = PheromoneMatrix.initialize(adapter.getProblem(), parameters.tauMax());
        this.valueSelector = new ProbabilisticSelection();
        this.pheromoneUpdater = new MaxMinUpdate();

        this.currentAnt = 0;
        this.currentCycle = 0;
        this.currentPath = new ArrayList<>();
        this.cycleSolutions = new ArrayList<>();
        this.bestOverall = Assignment.empty();
        this.solutionFoundByCurrentAnt = false;

        model.getSolver().setRestartOnSolutions();
        model.getSolver().addRestarter(new Restarter(
                new MonotonicCutoff(100),
                new FailCounter(model, 1),
                Integer.MAX_VALUE, false));
    }

    @Override
    public boolean init() {
        Solver solver = model.getSolver();
        if (!solver.getSearchMonitors().contains(this)) {
            solver.plugMonitor(this);
        }
        return variableSelector.init();
    }

    @Override
    public void remove() {
        Solver solver = model.getSolver();
        if (solver.getSearchMonitors().contains(this)) {
            solver.unplugMonitor(this);
        }
        variableSelector.remove();
    }

    @Override
    public Decision<IntVar> getDecision() {
        return computeDecision(
                variableSelector.getVariable(vars));
    }

    private Decision<IntVar> buildEqualsDecision(IntVar variable, int value) {
        return model.getSolver().getDecisionPath().makeIntDecision(variable, DecisionOperatorFactory.makeIntEq(),
                value);
    }

    @Override
    public Decision<IntVar> computeDecision(IntVar intVar) {
        if (intVar == null || intVar.isInstantiated()) {
            return null;
        }

        Optional<Integer> selectedValue = selectValue(intVar);

        if (selectedValue.isEmpty()) {
            return null;
        }

        int value = selectedValue.get();

        currentPath.add(new ChocoDecision(intVar, value));

        return buildEqualsDecision(intVar, value);
    }

    private Optional<Integer> selectValue(IntVar intVar) {
        Variable var = adapter.toVariable(intVar);
        if (var == null) {
            return Optional.empty();
        }

        Set<Integer> domain = adapter.getCurrentDomain(intVar);

        return valueSelector.select(var, domain, pheromones, parameters);
    }

    @Override
    public void onSolution() {
        this.solutionFoundByCurrentAnt = true;
        Assignment assignment = adapter.toAssignment(currentPath);
        cycleSolutions.add(assignment);

        if (assignment.size() >= bestOverall.size()) {
            bestOverall = assignment.snapshot();
        }
    }

    @Override
    public void beforeRestart() {
    }

    @Override
    public void afterRestart() {
        currentAnt++;

        if (currentAnt >= parameters.numberOfAnts()) {
            performCycleUpdate();
            currentAnt = 0;
            currentCycle++;
        }

        currentPath.clear();
        this.solutionFoundByCurrentAnt = false;
    }

    @Override
    public void onContradiction(ContradictionException cex) {
        if (!this.solutionFoundByCurrentAnt && !currentPath.isEmpty()) {
            Assignment partialAssignment = adapter.toAssignment(currentPath);
            cycleSolutions.add(partialAssignment);

            if (partialAssignment.size() > bestOverall.size()) {
                bestOverall = partialAssignment.snapshot();
            }
        }
    }

    private void performCycleUpdate() {
        if (cycleSolutions.isEmpty()) {
            cycleSolutions.clear();
            return;
        }

        pheromoneUpdater.update(pheromones, cycleSolutions, bestOverall, parameters);

        cycleSolutions.clear();
    }

    private record ChocoDecision(IntVar variable, int value) {
        public ChocoDecision {
            Objects.requireNonNull(variable, "Variable cannot be null");
        }

        @Override
        public String toString() {
            return String.format("%s=%d", variable.getName(), value);
        }
    }

    private class IntVarAdapter {

        private final Map<IntVar, Variable> intVarToVariable;
        private final Map<Variable, IntVar> variableToIntVar;
        private final Problem problem;
        private final IntVar[] chocoVars;

        public IntVarAdapter(IntVar[] chocoVars) {
            this.chocoVars = chocoVars;
            this.intVarToVariable = new HashMap<>();
            this.variableToIntVar = new HashMap<>();

            List<Variable> antaresVars = new ArrayList<>();
            for (IntVar intVar : chocoVars) {
                Set<Integer> domain = extractDomain(intVar);
                Variable variable = new Variable(intVar.getName(), domain);

                intVarToVariable.put(intVar, variable);
                variableToIntVar.put(variable, intVar);
                antaresVars.add(variable);
            }

            this.problem = new Problem(antaresVars, List.of());
        }

        public Variable toVariable(IntVar intVar) {
            return intVarToVariable.get(intVar);
        }

        public IntVar toIntVar(Variable variable) {
            return variableToIntVar.get(variable);
        }

        public Problem getProblem() {
            return problem;
        }

        public Assignment toAssignment(List<ChocoDecision> decisions) {
            Assignment assignment = Assignment.empty();
            for (ChocoDecision decision : decisions) {
                Variable variable = toVariable(decision.variable());
                if (variable != null) {
                    assignment = assignment.assign(variable, decision.value());
                }
            }
            return assignment;
        }

        private Set<Integer> extractDomain(IntVar intVar) {
            Set<Integer> domain = new HashSet<>();
            int lb = intVar.getLB();
            int ub = intVar.getUB();
            for (int value = lb; value <= ub; value++) {
                if (intVar.contains(value)) {
                    domain.add(value);
                }
            }
            return domain;
        }

        public Set<Integer> getCurrentDomain(IntVar intVar) {
            return extractDomain(intVar);
        }
    }
}
