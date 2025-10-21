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
import student.imt.antares.problem.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * How to use this class ?
 * <pre><code>
 *  Model model = new Model();
 *  // declare variables and constraints
 *  // ...
 * AntColonyMetaHeuristic ac = new AntColonyMetaHeuristic( parameters );
 * model.getSolver().setSearch(ac);
 * model.getSolver().solve();
 * </code></pre>
 *
 * @author Charles Prud'homme
 */

public class AntColonyMetaHeuristicSolver extends AbstractStrategy<IntVar>
            implements IMonitorRestart, IMonitorSolution, IMonitorContradiction {

    private final Model model;
    private final VariableSelector<IntVar> variableSelector;
    private final ACOParameters acoParameters;
    private final PheromoneMatrix pheromoneMatrix;
    private final MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
    private final ProbabilisticSelection valueSelector;
    private final Map<Variable<Integer>, IntVar> acoVariables;
    private final List<Assignment> assignmentList = new ArrayList<>();
    private Assignment bestAssignment = Assignment.empty();
    private Assignment currentAssignment = Assignment.empty();
    private int antsUsed = 0;

    public AntColonyMetaHeuristicSolver(IntVar[] variables,
                                  VariableSelector<IntVar> variableSelector,
                                  ACOParameters acoParameters,
                                  ProbabilisticSelection valueSelector
    ) {
        super(variables);
        this.model = variables[0].getModel();
        this.acoVariables = getACOVariables();
        this.variableSelector = variableSelector;
        this.acoParameters = acoParameters;
        this.pheromoneMatrix = PheromoneMatrix.initialize(new ArrayList<>(acoVariables.keySet()),
                                                          acoParameters.tauMax());
        this.valueSelector = valueSelector;

        // connection with the model/solver
        model.getSolver().plugMonitor(this);

        // restart on every solution
        model.getSolver().setRestartOnSolutions();

        // restart every 100 failures
        model.getSolver().addRestarter(new Restarter(
                new MonotonicCutoff(100),
                new FailCounter(model, 1),
                Integer.MAX_VALUE, false));
    }

    private Map<Variable<Integer>, IntVar> getACOVariables() {
        Map<Variable<Integer>, IntVar> acoVars = new HashMap<>();
        for (IntVar var : vars) {
            Set<Integer> domain = IntStream.rangeClosed(var.getLB(), var.getUB()).boxed().collect(Collectors.toSet());
            Variable<Integer> problemVar = new Variable<>(var.getName(), domain);
            acoVars.put(problemVar, var);
        }
        return acoVars;
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
        // make a decision
        return computeDecision(
                variableSelector.getVariable(vars)
        );
    }

    // syntaxix sugar
    private Decision<IntVar> buildEqualsDecision(IntVar variable, int value) {
        return model.getSolver().getDecisionPath().makeIntDecision(variable, DecisionOperatorFactory.makeIntEq(), value);
    }

    @Override
    public Decision<IntVar> computeDecision(IntVar variable) {
        if (variable == null || variable.isInstantiated()) {
            return null;
        }
        // call the value selector
        int value = selectValue(variable);

        return buildEqualsDecision(variable, value);
    }

    private int selectValue(IntVar variable) {

        // select the value here
        Variable<Integer> problemVar = acoVariables.entrySet().stream()
                .filter(entry -> entry.getValue().equals(variable))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Variable not found in ACO variables mapping"));

        Set<Integer> currentDomain = IntStream.rangeClosed(variable.getLB(), variable.getUB())
                .filter(variable::contains)
                .boxed()
                .collect(Collectors.toSet());

        int value = valueSelector.select(
                problemVar,
                currentDomain,
                pheromoneMatrix,
                acoParameters
        ).orElseThrow(() -> new IllegalStateException("No value selected for variable " + variable.getName()));

        currentAssignment.assign(problemVar, value);
        return value;
    }

    @Override
    public void onContradiction(ContradictionException cex) {
        // things to do on a failure
        System.out.println("Contradiction encountered: " + cex.getMessage());
        antsUsed++;
        assignmentList.add(currentAssignment.snapshot());
        if(currentAssignment.size() >= bestAssignment.size()) {
            bestAssignment = currentAssignment.snapshot();
        }
        currentAssignment = Assignment.empty();
    }

    @Override
    public void beforeRestart() {
        // things to do before restarting
        model.getSolver().reset();
        if(antsUsed >= acoParameters.numberOfAnts()) {
            antsUsed = 0;
            pheromoneUpdater.update(
                    pheromoneMatrix,
                    assignmentList,
                    bestAssignment,
                    acoParameters
            );
        }
    }

    @Override
    public void afterRestart() {
        // things to do after the restart
        if(antsUsed >= acoParameters.numberOfAnts()) {
            bestAssignment = Assignment.empty();
            assignmentList.clear();
        }
        currentAssignment = Assignment.empty();

    }

    @Override
    public void onSolution() {
        // things to do on a solution, if needed
        assignmentList.add(currentAssignment.snapshot());
        if(currentAssignment.size() > bestAssignment.size()) {
            bestAssignment = currentAssignment.snapshot();
        }
    }
}

