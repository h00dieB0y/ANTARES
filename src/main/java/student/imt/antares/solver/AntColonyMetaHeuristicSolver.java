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
import org.chocosolver.util.criteria.Criterion;

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
    private final int maxCycles;

    private final PheromoneMatrix pheromones;
    private final ProbabilisticSelection valueSelector;
    private final MaxMinUpdate pheromoneUpdater;

    private int currentAnt;
    private int currentCycle;

    private List<Assignment> cycleSolutions;

    private Assignment bestOverall;
    private boolean solutionFoundByCurrentAnt;
    private SolutionProgress bestProgress;
    private List<SolutionProgress> progressHistory;

    // Track the deepest assignment reached by current ant
    private Assignment currentAntBestAssignment;
    private int currentAntMaxDepth;

    public AntColonyMetaHeuristicSolver(IntVar[] variables,
            VariableSelector<IntVar> variableSelector,
            ACOParameters parameters,
            int maxCycles) {
        this(variables, variableSelector, parameters, maxCycles, System.currentTimeMillis());
    }

    public AntColonyMetaHeuristicSolver(IntVar[] variables,
            VariableSelector<IntVar> variableSelector,
            ACOParameters parameters,
            int maxCycles,
            long seed) {
        super(variables);
        this.model = variables[0].getModel();
        this.variableSelector = variableSelector;
        this.parameters = parameters;
        this.maxCycles = maxCycles;

        this.adapter = new IntVarAdapter(variables);

        this.pheromones = PheromoneMatrix.initialize(adapter.getProblem(), parameters.tauMax());
        this.valueSelector = new ProbabilisticSelection(seed);
        this.pheromoneUpdater = new MaxMinUpdate();

        this.currentAnt = 0;
        this.currentCycle = 0;
        this.cycleSolutions = new ArrayList<>();
        this.bestOverall = Assignment.empty();
        this.solutionFoundByCurrentAnt = false;
        this.bestProgress = new SolutionProgress(0, 0, Assignment.empty());
        this.progressHistory = new ArrayList<>();
        this.currentAntBestAssignment = Assignment.empty();
        this.currentAntMaxDepth = 0;

        // Each ant gets a fixed budget: 5 failures per variable
        int failureLimit = vars.length * 5;
        model.getSolver().addRestarter(new Restarter(
                new GeometricalCutoff(failureLimit, 1.00001),  // Scale factor 1.0 = constant cutoff
                new FailCounter(model, failureLimit),
                Integer.MAX_VALUE,
                false));
        
        model.getSolver().limitSearch(
            () -> currentCycle >= maxCycles
        );
    }

    public AntColonyMetaHeuristicSolver(IntVar[] variables,
            VariableSelector<IntVar> variableSelector,
            ACOParameters parameters,
            long seed) {
        this(variables, variableSelector, parameters, 250, seed);
    }

    public AntColonyMetaHeuristicSolver(IntVar[] variables,
            VariableSelector<IntVar> variableSelector,
            ACOParameters parameters) {
        this(variables, variableSelector, parameters, 250, System.currentTimeMillis());
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
        IntVar selectedVar = variableSelector.getVariable(vars);
        return computeDecision(selectedVar);
    }

    private Decision<IntVar> buildEqualsDecision(IntVar variable, int value) {
        return model.getSolver().getDecisionPath().makeIntDecision(variable, DecisionOperatorFactory.makeIntEq(),
                value);
    }

    @Override
    public Decision<IntVar> computeDecision(IntVar intVar) {
        // Always track current depth before making a decision
        updateCurrentAntBestAssignment();

        if (intVar == null || intVar.isInstantiated()) {
            return null;
        }

        Optional<Integer> selectedValue = selectValue(intVar);

        if (selectedValue.isEmpty()) {
            return null;
        }

        int value = selectedValue.get();

        return buildEqualsDecision(intVar, value);
    }

    private void updateCurrentAntBestAssignment() {
        int currentDepth = 0;
        for (IntVar var : vars) {
            if (var.isInstantiated()) {
                currentDepth++;
            }
        }

        if (currentDepth > currentAntMaxDepth) {
            currentAntMaxDepth = currentDepth;
            currentAntBestAssignment = captureCurrentAssignment();
        }
    }

    private Optional<Integer> selectValue(IntVar intVar) {
        Variable variable = adapter.toVariable(intVar);
        if (variable == null) {
            return Optional.empty();
        }

        Set<Integer> domain = adapter.getCurrentDomain(intVar);

        return valueSelector.select(variable, domain, pheromones, parameters);
    }

    private Assignment captureCurrentAssignment() {
        Assignment assignment = Assignment.empty();

        for (IntVar var : vars) {
            if (var.isInstantiated()) {
                Variable variable = adapter.toVariable(var);
                if (variable != null) {
                    assignment = assignment.assign(variable, var.getValue());
                }
            }
        }

        return assignment;
    }

    @Override
    public void onSolution() {
        this.solutionFoundByCurrentAnt = true;
        Assignment assignment = captureCurrentAssignment();
        cycleSolutions.add(assignment);

        if (assignment.size() >= bestOverall.size()) {
            bestOverall = assignment.snapshot();
            bestProgress = new SolutionProgress(currentCycle, assignment.size(), bestOverall);

            // If complete solution found, update pheromones immediately
            // because search will stop and afterRestart() won't be called
            if (assignment.size() == adapter.getProblem().size()) {
                currentAnt++;  // Count this successful ant

                performCycleUpdate();
                progressHistory.add(bestProgress);

                // Note: For CSP, Choco stops here. No more ants/cycles will run.
            }
        }
    }

    @Override
    public void afterRestart() {
        // Use the deepest assignment reached by this ant (if not already captured in onSolution)
        if (!this.solutionFoundByCurrentAnt) {
            // Use the deepest assignment tracked during search
            Assignment antAssignment = currentAntBestAssignment;
            if (antAssignment.size() > 0) {
                cycleSolutions.add(antAssignment);

                if (antAssignment.size() > bestOverall.size()) {
                    bestOverall = antAssignment.snapshot();
                    bestProgress = new SolutionProgress(currentCycle, antAssignment.size(), bestOverall);
                }
            }
        }

        currentAnt++;

        if (currentAnt >= parameters.numberOfAnts()) {
            performCycleUpdate();
            progressHistory.add(new SolutionProgress(currentCycle, bestOverall.size(), bestOverall));
            currentAnt = 0;
            currentCycle++;
        }

        // Reset tracking for next ant
        this.solutionFoundByCurrentAnt = false;
        this.currentAntBestAssignment = Assignment.empty();
        this.currentAntMaxDepth = 0;
    }

    @Override
    public void onContradiction(ContradictionException cex) {
        // Contradictions are handled naturally by Choco's backtracking
        // No action needed here - assignment capture happens in afterRestart()
    }

    private void performCycleUpdate() {
        if (cycleSolutions.isEmpty()) {
            cycleSolutions.clear();
            return;
        }

        pheromoneUpdater.update(pheromones, cycleSolutions, bestOverall, parameters);

        cycleSolutions.clear();
    }

    public SolutionProgress getBestProgress() {
        return bestProgress;
    }

    public List<SolutionProgress> getProgressHistory() {
        return List.copyOf(progressHistory);
    }

    public void printProgressCSV(long seed, String problemName, ACOParameters params) {
        int totalVars = adapter.getProblem().size();
        for (var progress : progressHistory) {
            double completionRate = (double) progress.assignedVariables() / totalVars;
            System.out.printf(java.util.Locale.US, "%d,%s,%.2f,%.2f,%.2f,%d,%d,%d,%d,%.4f%n",
                    seed,
                    problemName,
                    params.alpha(),
                    params.beta(),
                    params.rho(),
                    params.numberOfAnts(),
                    progress.cycle(),
                    progress.assignedVariables(),
                    totalVars,
                    completionRate);
        }
    }

    public void printSummaryCSV(long seed, String problemName, ACOParameters params,
            long runtimeMs, int totalCycles) {
        int totalVars = adapter.getProblem().size();
        int finalAssigned = bestProgress.assignedVariables();
        int bestCycle = bestProgress.cycle();
        boolean success = (finalAssigned == totalVars);

        System.err.printf(java.util.Locale.US, "%d,%s,%d,%.2f,%.2f,%.2f,%.4f,%.2f,%d,%d,%d,%b,%d,%d%n",
                seed,
                problemName,
                totalVars,
                params.alpha(),
                params.beta(),
                params.rho(),
                params.tauMin(),
                params.tauMax(),
                params.numberOfAnts(),
                bestCycle,
                finalAssigned,
                success,
                totalCycles,
                runtimeMs);
    }

    public record SolutionProgress(int cycle, int assignedVariables, Assignment assignment) {
        @Override
        public String toString() {
            return String.format("Cycle %d: %d variables assigned", cycle, assignedVariables);
        }
    }

    private class IntVarAdapter {

        private final Map<IntVar, Variable> intVarToVariable;
        private final Map<Variable, IntVar> variableToIntVar;
        private final Problem problem;
        final IntVar[] chocoVars;

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
