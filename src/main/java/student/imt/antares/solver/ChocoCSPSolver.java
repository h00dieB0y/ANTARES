package student.imt.antares.solver;

import org.chocosolver.solver.*;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.monitors.*;
import org.chocosolver.solver.search.restart.*;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Choco Solver integration for ANTARES using Ant Colony Optimization as a search strategy.
 * This class bridges the ANTARES CSP framework with Choco Solver by implementing
 * a custom search strategy that uses pheromone trails to guide value selection.
 */
public class ChocoCSPSolver implements CSPSolver {
    private static final Logger logger = LoggerFactory.getLogger(ChocoCSPSolver.class);

    private final Problem antaresProb;
    private final Model chocoModel;
    private final Map<Variable<?>, IntVar> variableMapping;
    private final ACOParameters acoParams;
    private final PheromoneMatrix pheromones;
    private Assignment bestAssignment;
    private boolean failed;

    public ChocoCSPSolver(Problem problem, ACOParameters parameters) {
        this.antaresProb = Objects.requireNonNull(problem, "Problem cannot be null");
        this.acoParams = Objects.requireNonNull(parameters, "ACO parameters cannot be null");
        this.chocoModel = new Model("ANTARES-ACO");
        this.variableMapping = new HashMap<>();
        this.bestAssignment = Assignment.empty();
        this.failed = false;
        
        // Initialize pheromones
        this.pheromones = PheromoneMatrix.initialize(problem, parameters.tauMax());
        
        // Build Choco model from ANTARES problem
        buildChocoModel();
    }

    /**
     * Converts ANTARES problem to Choco model.
     */
    private void buildChocoModel() {
        // Create Choco variables from ANTARES variables
        for (Variable<?> antaresVar : antaresProb.getVariables()) {
            IntVar chocoVar = createChocoVariable(antaresVar);
            variableMapping.put(antaresVar, chocoVar);
        }

        // Post Choco constraints from ANTARES constraints
        for (student.imt.antares.problem.Constraint antaresConstraint : antaresProb.getConstraints()) {
            postChocoConstraint(antaresConstraint);
        }
        
        logger.debug("Choco model built: {} variables, {} constraints", 
                    variableMapping.size(), chocoModel.getNbCstrs());
    }

    /**
     * Creates a Choco IntVar from an ANTARES Variable.
     */
    private IntVar createChocoVariable(Variable<?> antaresVar) {
        Set<?> domain = antaresVar.domain();
        
        // Convert domain to int array (assuming integer domains)
        int[] domainArray = domain.stream()
                .mapToInt(val -> {
                    // use switch expression instead of casting directly
                    if (val instanceof Integer integer) {
                        return integer;
                    } else if (val instanceof Number number) {
                        return number.intValue();
                    } else {
                        throw new IllegalArgumentException(
                            "Variable domain must contain numeric values: " + antaresVar.name());
                    }
                })
                .sorted()
                .toArray();
        
        return chocoModel.intVar(antaresVar.name(), domainArray);
    }

    /**
     * Posts a Choco constraint based on an ANTARES constraint.
     * Maps ANTARES constraints to equivalent Choco constraints.
     */
    private void postChocoConstraint(student.imt.antares.problem.Constraint antaresConstraint) {
        Set<Variable<?>> involvedVars = antaresConstraint.getInvolvedVariables();

        // Convert ANTARES variables to Choco variables
        IntVar[] chocoVars = involvedVars.stream()
                .map(variableMapping::get)
                .filter(Objects::nonNull)
                .toArray(IntVar[]::new);

        if (chocoVars.length == 0) {
            logger.warn("No Choco variables found for constraint: {}", antaresConstraint);
            return;
        }

        // Detect constraint type and map to appropriate Choco constraint
        String constraintStr = antaresConstraint.toString();

        if (constraintStr.contains("AllDifferent")) {
            // Map to Choco's allDifferent constraint
            chocoModel.allDifferent(chocoVars).post();
            logger.trace("Posted allDifferent constraint for {} variables", chocoVars.length);
        } else {
            // For other constraint types, create a custom propagator
            chocoModel.post(new AntaresConstraintAdapter(
                antaresConstraint,
                variableMapping,
                chocoVars,
                chocoModel
            ));
            logger.trace("Posted custom constraint adapter: {}", constraintStr);
        }
    }

    @Override
    public void reset() {
        chocoModel.getSolver().hardReset();
        failed = false;
        logger.trace("Choco solver reset");
    }

    @Override
    public boolean propagate(Assignment assignment) {
        if (failed) {
            return false;
        }

        try {
            chocoModel.getSolver().propagate();
            
            // Check if assignment is consistent
            if (!antaresProb.isConsistent(assignment)) {
                failed = true;
                return false;
            }
            
            return true;
        } catch (ContradictionException e) {
            logger.debug("Propagation failed with contradiction: {}", e.getMessage());
            failed = true;
            return false;
        }
    }

    @Override
    public <T> Set<T> getCurrentDomain(Variable<T> variable) {
        IntVar chocoVar = variableMapping.get(variable);
        if (chocoVar == null) {
            return Set.of();
        }

        Set<T> domain = new HashSet<>();
        for (int value = chocoVar.getLB(); value <= chocoVar.getUB(); value = chocoVar.nextValue(value)) {
            @SuppressWarnings("unchecked")
            T typedValue = (T) Integer.valueOf(value);
            domain.add(typedValue);
        }
        
        return domain;
    }

    @Override
    public boolean hasFailed() {
        return failed || !chocoModel.getSolver().solve();
    }

    @Override
    public List<Variable<?>> getSingletonVariables() {
        return variableMapping.entrySet().stream()
                .filter(entry -> entry.getValue().isInstantiated())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Solves the problem using ACO-guided search.
     */
    public Assignment solve(int maxCycles) {
        Solver solver = chocoModel.getSolver();
        
        // Create and set the ACO search strategy
        IntVar[] allVars = variableMapping.values().toArray(new IntVar[0]);
        ACOSearchStrategy acoStrategy = new ACOSearchStrategy(
            allVars, 
            acoParams, 
            pheromones,
            antaresProb,
            variableMapping
        );
        
        solver.setSearch(acoStrategy);
        
        logger.info("Starting Choco-ACO solve: {} cycles", maxCycles);
        
        // Solve with restarts
        int cycleCount = 0;
        while (solver.solve() && cycleCount < maxCycles) {
            // Store best solution
            Solution solution = new Solution(chocoModel);
            solution.record();
            bestAssignment = convertSolutionToAssignment(solution);
            
            if (antaresProb.isSolution(bestAssignment)) {
                logger.info("Valid solution found at cycle {}", cycleCount);
                return bestAssignment;
            }
            
            cycleCount++;
            
            if (cycleCount % 10 == 0) {
                logger.info("Cycle {}/{} - Best: {}/{} variables",
                           cycleCount, maxCycles, bestAssignment.size(), antaresProb.size());
            }
        }
        
        logger.warn("Max cycles reached. Best: {}/{} variables",
                   bestAssignment.size(), antaresProb.size());
        return bestAssignment;
    }

    /**
     * Converts a Choco Solution to an ANTARES Assignment.
     */
    private Assignment convertSolutionToAssignment(Solution solution) {
        Assignment assignment = Assignment.empty();
        
        for (Map.Entry<Variable<?>, IntVar> entry : variableMapping.entrySet()) {
            Variable<?> antaresVar = entry.getKey();
            IntVar chocoVar = entry.getValue();
            
            if (chocoVar.isInstantiated()) {
                int value = solution.getIntVal(chocoVar);
                assignGeneric(assignment, antaresVar, value);
            }
        }
        
        return assignment;
    }

    @SuppressWarnings("unchecked")
    private <T> void assignGeneric(Assignment assignment, Variable<T> variable, int value) {
        assignment.assign(variable, (T) Integer.valueOf(value));
    }

    /**
     * Inner class: ACO Search Strategy for Choco.
     */
    private static class ACOSearchStrategy extends AbstractStrategy<IntVar> 
            implements IMonitorRestart, IMonitorSolution, IMonitorContradiction {
        
        private final Model model;
        private final ACOParameters params;
        private final PheromoneMatrix pheromones;
        @SuppressWarnings("unused") // Used indirectly for validation
        private final Problem antaresProb;
        private final Map<Variable<?>, IntVar> varMapping;
        private final Random random;
        private int solutionCount;
        private int contradictionCount;

        public ACOSearchStrategy(IntVar[] variables,
                                ACOParameters params,
                                PheromoneMatrix pheromones,
                                Problem antaresProb,
                                Map<Variable<?>, IntVar> varMapping) {
            super(variables);
            this.model = variables[0].getModel();
            this.params = params;
            this.pheromones = pheromones;
            this.antaresProb = antaresProb;
            this.varMapping = varMapping;
            this.random = new Random(42);
            this.solutionCount = 0;
            this.contradictionCount = 0;

            // Configure restarts
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
            return true;
        }

        @Override
        public void remove() {
            Solver solver = model.getSolver();
            if (solver.getSearchMonitors().contains(this)) {
                solver.unplugMonitor(this);
            }
        }

        @Override
        public Decision<IntVar> getDecision() {
            IntVar variable = selectVariable();
            return computeDecision(variable);
        }

        private IntVar selectVariable() {
            // Select first uninstantiated variable
            for (IntVar v : vars) {
                if (!v.isInstantiated()) {
                    return v;
                }
            }
            return null;
        }

        @Override
        public Decision<IntVar> computeDecision(IntVar variable) {
            if (variable == null || variable.isInstantiated()) {
                return null;
            }
            
            int value = selectValue(variable);
            return model.getSolver().getDecisionPath().makeIntDecision(
                variable, DecisionOperatorFactory.makeIntEq(), value);
        }

        private int selectValue(IntVar chocoVar) {
            // Find corresponding ANTARES variable
            Variable<?> antaresVar = findAntaresVariable(chocoVar);
            if (antaresVar == null) {
                // Fallback: select random value
                return chocoVar.getLB();
            }

            // Collect available values with their pheromone levels
            List<Integer> values = new ArrayList<>();
            List<Double> probabilities = new ArrayList<>();
            double totalWeight = 0.0;

            for (int val = chocoVar.getLB(); val <= chocoVar.getUB(); val = chocoVar.nextValue(val)) {
                double pheromone = getPheromoneAmount(antaresVar, val);
                double weight = Math.pow(pheromone, params.alpha());
                
                values.add(val);
                probabilities.add(weight);
                totalWeight += weight;
            }

            // Normalize probabilities
            if (totalWeight > 0) {
                for (int i = 0; i < probabilities.size(); i++) {
                    probabilities.set(i, probabilities.get(i) / totalWeight);
                }
            }

            // Select value using roulette wheel
            return rouletteWheelSelection(values, probabilities);
        }

        /**
         * Type-safe helper method to get pheromone amount for a variable-value pair.
         */
        @SuppressWarnings("unchecked")
        private <T> double getPheromoneAmount(Variable<?> variable, int value) {
            Variable<T> typedVar = (Variable<T>) variable;
            T typedValue = (T) Integer.valueOf(value);
            return pheromones.getAmount(typedVar, typedValue);
        }

        private Variable<?> findAntaresVariable(IntVar chocoVar) {
            for (Map.Entry<Variable<?>, IntVar> entry : varMapping.entrySet()) {
                if (entry.getValue().equals(chocoVar)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private int rouletteWheelSelection(List<Integer> values, List<Double> probabilities) {
            if (values.isEmpty()) {
                throw new IllegalStateException("No values available for selection");
            }

            double rand = random.nextDouble();
            double cumulative = 0.0;

            for (int i = 0; i < values.size(); i++) {
                cumulative += probabilities.get(i);
                if (rand <= cumulative) {
                    return values.get(i);
                }
            }

            return values.get(values.size() - 1);
            // return values.getLast() ;
        }

        @Override
        public void onContradiction(ContradictionException cex) {
            // Update pheromones on failure (apply evaporation)
            contradictionCount++;
            pheromones.evaporate(params.rho());

            if (contradictionCount % 100 == 0) {
                logger.debug("Contradictions encountered: {}", contradictionCount);
            }
        }

        @Override
        public void beforeRestart() {
            // Apply pheromone evaporation before restart
            pheromones.evaporate(params.rho());
            logger.trace("Restart triggered - pheromones evaporated");
        }

        @Override
        public void afterRestart() {
            // Clamp pheromones to maintain bounds after restart
            pheromones.clamp(params.tauMin(), params.tauMax());
            logger.trace("Pheromones clamped after restart");
        }

        @Override
        public void onSolution() {
            // Update pheromones on solution found
            solutionCount++;
            Solution solution = new Solution(model);
            solution.record();

            // Convert Choco solution to ANTARES assignment
            Assignment assignment = convertChocoSolutionToAssignment(solution);

            // Deposit pheromones on the solution path
            double depositAmount = params.tauMax() / 10.0; // Reward proportional to quality
            pheromones.deposit(assignment, depositAmount);
            pheromones.clamp(params.tauMin(), params.tauMax());

            logger.debug("Solution #{} found - pheromones updated", solutionCount);
        }

        private Assignment convertChocoSolutionToAssignment(Solution solution) {
            Assignment assignment = Assignment.empty();

            for (Map.Entry<Variable<?>, IntVar> entry : varMapping.entrySet()) {
                Variable<?> antaresVar = entry.getKey();
                IntVar chocoVar = entry.getValue();

                if (chocoVar.isInstantiated()) {
                    int value = solution.getIntVal(chocoVar);
                    assignToAssignment(assignment, antaresVar, value);
                }
            }

            return assignment;
        }

        @SuppressWarnings("unchecked")
        private <T> void assignToAssignment(Assignment assignment, Variable<T> variable, int value) {
            assignment.assign(variable, (T) Integer.valueOf(value));
        }
    }

    /**
     * Adapter that wraps ANTARES constraints as Choco propagators.
     * Used for constraint types that don't have direct Choco equivalents.
     */
    private static class AntaresConstraintAdapter extends Constraint {

        public AntaresConstraintAdapter(
                student.imt.antares.problem.Constraint antaresConstraint,
                Map<Variable<?>, IntVar> varMapping,
                IntVar[] scope,
                Model model) {
            super("AntaresAdapter",
                new AntaresPropagator(antaresConstraint, varMapping, scope, model));
        }
    }

    /**
     * Custom propagator that checks ANTARES constraint satisfaction.
     */
    private static class AntaresPropagator extends org.chocosolver.solver.constraints.Propagator<IntVar> {
        private final student.imt.antares.problem.Constraint antaresConstraint;
        private final Map<Variable<?>, IntVar> varMapping;

        public AntaresPropagator(
                student.imt.antares.problem.Constraint antaresConstraint,
                Map<Variable<?>, IntVar> varMapping,
                IntVar[] scope,
                Model model) {
            super(scope, org.chocosolver.solver.constraints.PropagatorPriority.LINEAR, false);
            this.antaresConstraint = antaresConstraint;
            this.varMapping = varMapping;
        }

        @Override
        public void propagate(int evtmask) throws ContradictionException {
            // Convert current Choco state to ANTARES Assignment
            Assignment assignment = Assignment.empty();

            for (Map.Entry<Variable<?>, IntVar> entry : varMapping.entrySet()) {
                IntVar chocoVar = entry.getValue();
                if (chocoVar.isInstantiated()) {
                    assignToAssignment(assignment, entry.getKey(), chocoVar.getValue());
                }
            }

            // Check if constraint is satisfied
            Set<Variable<?>> involved = antaresConstraint.getInvolvedVariables();
            boolean allAssigned = involved.stream().allMatch(assignment::isAssigned);

            if (allAssigned && !antaresConstraint.isSatisfiedBy(assignment)) {
                fails();
            }
        }

        @Override
        public org.chocosolver.util.ESat isEntailed() {
            // Convert current state to ANTARES Assignment
            Assignment assignment = Assignment.empty();
            boolean allInstantiated = true;

            for (Map.Entry<Variable<?>, IntVar> entry : varMapping.entrySet()) {
                IntVar chocoVar = entry.getValue();
                if (chocoVar.isInstantiated()) {
                    assignToAssignment(assignment, entry.getKey(), chocoVar.getValue());
                } else {
                    allInstantiated = false;
                }
            }

            Set<Variable<?>> involved = antaresConstraint.getInvolvedVariables();
            boolean allInvolvedAssigned = involved.stream().allMatch(assignment::isAssigned);

            if (allInvolvedAssigned) {
                return antaresConstraint.isSatisfiedBy(assignment)
                    ? org.chocosolver.util.ESat.TRUE
                    : org.chocosolver.util.ESat.FALSE;
            }

            return org.chocosolver.util.ESat.UNDEFINED;
        }

        @SuppressWarnings("unchecked")
        private <T> void assignToAssignment(Assignment assignment, Variable<T> variable, int value) {
            assignment.assign(variable, (T) Integer.valueOf(value));
        }
    }
}

