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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.construction.ProbabilisticSelection;
import student.imt.antares.pheromone.MaxMinUpdate;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * How to use this class ?
 * 
 * <pre>
 * <code>
 *  Model model = new Model();
 *  // declare variables and constraints
 *  // ...
 * AntColonyMetaHeuristic ac = new AntColonyMetaHeuristic( parameters );
 * model.getSolver().setSearch(ac);
 * model.getSolver().solve();
 * </code>
 * </pre>
 *
 * @author Charles Prud'homme
 */

public class AntColonyMetaHeuristicSolver extends AbstractStrategy<IntVar>
        implements IMonitorRestart, IMonitorSolution, IMonitorContradiction {

    private static final Logger logger = LoggerFactory.getLogger(AntColonyMetaHeuristicSolver.class);

    private final Model model;
    private final VariableSelector<IntVar> variableSelector;
    private final IntVarAdapter adapter;
    private final ACOParameters parameters;

    // Reuse ANTARES core components (NO duplication!)
    private final PheromoneMatrix pheromones;
    private final ProbabilisticSelection valueSelector;
    private final MaxMinUpdate pheromoneUpdater;

    // Ant cycle tracking
    private int currentAnt;
    private int currentCycle;

    // Path tracking for current ant
    private List<ChocoDecision> currentPath;

    // Solutions collected in current cycle (as ANTARES Assignments)
    private List<Assignment> cycleSolutions;

    // Best solution found overall
    private Assignment bestOverall;
    private boolean solutionFoundByCurrentAnt;

    /**
     * Creates an ACO metaheuristic strategy for Choco.
     *
     * @param variables        all decision variables in the model
     * @param variableSelector strategy for selecting which variable to assign next
     * @param parameters       ACO parameters (alpha, beta, rho, tauMin, tauMax,
     *                         numberOfAnts)
     */
    public AntColonyMetaHeuristicSolver(IntVar[] variables,
            VariableSelector<IntVar> variableSelector,
            ACOParameters parameters) {
        super(variables);
        this.model = variables[0].getModel();
        this.variableSelector = variableSelector;
        this.parameters = parameters;

        // Create adapter to convert IntVar ↔ Variable
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

        // Configure Choco restart mechanism
        model.getSolver().setRestartOnSolutions();
        model.getSolver().addRestarter(new Restarter(
                new MonotonicCutoff(100),
                new FailCounter(model, 1),
                Integer.MAX_VALUE, false));

        logger.info("ACO initialized: {} ants/cycle, restart on first failure, {} variables",
                parameters.numberOfAnts(), variables.length);
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
                variableSelector.getVariable(vars));
    }

    // syntaxix sugar
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

        // Track this decision in the current ant's path
        currentPath.add(new ChocoDecision(intVar, value));

        return buildEqualsDecision(intVar, value);
    }

    private Optional<Integer> selectValue(IntVar intVar) {
        // Convert IntVar to ANTARES Variable
        Variable var = adapter.toVariable(intVar);
        if (var == null) {
            return Optional.empty();
        }

        // Get current domain from Choco (after propagation)
        Set<Integer> domain = adapter.getCurrentDomain(intVar);

        logger.debug("Ant {}: Selecting value for variable {} with domain {}", currentAnt, var.name(), domain);
        for (Integer value : domain) {
            double pheromone = pheromones.getAmount(var, value);
            //System.out.printf("  Value %d: Pheromone = %.3f \n", value, pheromone);
        }

        // Use ANTARES ProbabilisticSelection (reused, not duplicated!)
        Optional<Integer> selectedValue = valueSelector.select(var, domain, pheromones, parameters);

        if (selectedValue.isPresent()) {
            logger.debug("Ant {}: Selected value {} for variable {}", currentAnt, selectedValue.get(), var.name());
        } else {
            logger.debug("Ant {}: No value selected for variable {}", currentAnt, var.name());
        }

        return selectedValue;
    }

    @Override
    public void onSolution() {
        this.solutionFoundByCurrentAnt = true;
        Assignment assignment = adapter.toAssignment(currentPath);
        cycleSolutions.add(assignment);

        logger.debug("Ant {} found solution with {} decisions", currentAnt, assignment.size());

        // Track global best
        if (assignment.size() >= bestOverall.size()) {
            bestOverall = assignment.snapshot();
            logger.info("New best solution: {} variables assigned", bestOverall.size());
        }
    }

    @Override
    public void beforeRestart() {
        // Nothing to do before restart
    }

    @Override
    public void afterRestart() {
        currentAnt++;

        // If we've completed a cycle (N ants), perform pheromone update
        if (currentAnt >= parameters.numberOfAnts()) {
            performCycleUpdate();
            currentAnt = 0;
            currentCycle++;
        }

        // Clear path for new ant and reset flag for the new ant's run
        currentPath.clear();
        this.solutionFoundByCurrentAnt = false;
    }

    @Override
    public void onContradiction(ContradictionException cex) {
        // If the current ant did not find a complete solution and has a partial path,
        // capture it before the restart clears the path.
        if (!this.solutionFoundByCurrentAnt && !currentPath.isEmpty()) {
            Assignment partialAssignment = adapter.toAssignment(currentPath);
            cycleSolutions.add(partialAssignment);
            logger.debug("Ant {} ended with partial assignment of {} decisions due to contradiction", currentAnt,
                    partialAssignment.size());

            // Update global best if this partial assignment is better (more variables
            // assigned)
            if (partialAssignment.size() > bestOverall.size()) {
                bestOverall = partialAssignment.snapshot();
                logger.info("New best partial solution: {} variables assigned (from contradiction)",
                        bestOverall.size());
            }
        }
    }

    /**
     * Performs pheromone update after completing a cycle of N ants.
     * <p>
     * Reuses {@link MaxMinUpdate} from ANTARES core - NO duplication!
     * </p>
     */
    private void performCycleUpdate() {
        if (cycleSolutions.isEmpty()) {
            logger.debug("Cycle {} complete: no solutions found", currentCycle);
            cycleSolutions.clear();
            return;
        }

        logger.debug("Cycle {} complete: {} solutions found, updating pheromones",
                currentCycle, cycleSolutions.size());

        // Use ANTARES MaxMinUpdate (reused, not duplicated!)
        pheromoneUpdater.update(pheromones, cycleSolutions, bestOverall, parameters);

        // Clear for next cycle
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

        /**
         * Creates an adapter for a set of Choco variables.
         *
         * @param chocoVars the Choco IntVar array (only uninstantiated variables)
         */
        public IntVarAdapter(IntVar[] chocoVars) {
            this.chocoVars = chocoVars;
            this.intVarToVariable = new HashMap<>();
            this.variableToIntVar = new HashMap<>();

            // Create ANTARES Variable for each IntVar
            List<Variable> antaresVars = new ArrayList<>();
            for (IntVar intVar : chocoVars) {
                Set<Integer> domain = extractDomain(intVar);
                Variable variable = new Variable(intVar.getName(), domain);

                intVarToVariable.put(intVar, variable);
                variableToIntVar.put(variable, intVar);
                antaresVars.add(variable);
            }

            // Create a minimal Problem (no constraints needed, Choco handles propagation)
            this.problem = new Problem(antaresVars, List.of());
        }

        /**
         * Converts an IntVar to its corresponding ANTARES Variable.
         */
        public Variable toVariable(IntVar intVar) {
            return intVarToVariable.get(intVar);
        }

        /**
         * Converts an ANTARES Variable to its corresponding IntVar.
         */
        public IntVar toIntVar(Variable variable) {
            return variableToIntVar.get(variable);
        }

        /**
         * Returns the ANTARES Problem representation (for PheromoneMatrix
         * initialization).
         */
        public Problem getProblem() {
            return problem;
        }

        /**
         * Converts a list of Choco decisions to an ANTARES Assignment.
         */
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

        /**
         * Extracts the current domain of an IntVar as a Set<Integer>.
         */
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

        /**
         * Gets the current reduced domain of an IntVar (after propagation).
         */
        public Set<Integer> getCurrentDomain(IntVar intVar) {
            return extractDomain(intVar);
        }
    }
}
