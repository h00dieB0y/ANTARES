package student.imt.antares.colony;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.construction.AssignmentConstructor;
import student.imt.antares.construction.ValueSelector;
import student.imt.antares.construction.VariableSelector;
import student.imt.antares.pheromone.PheromoneUpdater;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.solver.CSPSolver;

/**
 * Ant colony that iteratively constructs and improves CSP solutions.
 * Each ant builds a solution guided by pheromone trails, and the colony
 * updates pheromones based on solution quality.
 */
public class Colony {
    private static final Logger logger = LoggerFactory.getLogger(Colony.class);

    private final ACOParameters parameters;
    private PheromoneMatrix pheromones;
    private Assignment bestAssignment;

    private Colony(ACOParameters parameters, PheromoneMatrix initialPheromones) {
        this.parameters = Objects.requireNonNull(parameters, "Parameters cannot be null");
        this.pheromones = Objects.requireNonNull(initialPheromones, "Initial pheromones cannot be null");
        this.bestAssignment = Assignment.empty();
    }

    public static Colony create(Problem problem, ACOParameters parameters) {
        Objects.requireNonNull(problem, "Problem cannot be null");
        Objects.requireNonNull(parameters, "Parameters cannot be null");

        PheromoneMatrix initialPheromones = PheromoneMatrix.initialize(problem, parameters.tauMax());
        return new Colony(parameters, initialPheromones);
    }

    public Assignment solve(Problem problem,
                            AssignmentConstructor constructor,
                            VariableSelector variableSelector,
                            ValueSelector valueSelector,
                            PheromoneUpdater pheromoneUpdater,
                            CSPSolver solver,
                            int maxCycles) {

        Objects.requireNonNull(problem, "Problem cannot be null");
        Objects.requireNonNull(constructor, "Assignment constructor cannot be null");
        Objects.requireNonNull(variableSelector, "Variable selector cannot be null");
        Objects.requireNonNull(valueSelector, "Value selector cannot be null");
        Objects.requireNonNull(pheromoneUpdater, "Pheromone updater cannot be null");
        Objects.requireNonNull(solver, "CSP solver cannot be null");

        if (maxCycles <= 0) {
            throw new IllegalArgumentException("Max cycles must be positive");
        }

        logger.info("Starting ACO: {} cycles, {} ants/cycle, problem size: {}",
                   maxCycles, parameters.numberOfAnts(), problem.size());
        logger.debug("Parameters: {}", parameters);

        for (int cycle = 0; cycle < maxCycles; cycle++) {
            Assignment cycleBest = executeCycle(problem, constructor, variableSelector,
                                                valueSelector, pheromoneUpdater, solver);

            // Return immediately if valid solution found
            if (problem.isSolution(cycleBest)) {
                logger.info("Valid solution found at cycle {}: {}/{} variables assigned",
                           cycle, cycleBest.size(), problem.size());
                return cycleBest;
            }
        }

        logger.warn("Max cycles reached without complete solution. Best: {}/{} variables",
                   bestAssignment.size(), problem.size());
        return bestAssignment;
    }

    private Assignment executeCycle(Problem problem,
                                    AssignmentConstructor constructor,
                                    VariableSelector variableSelector,
                                    ValueSelector valueSelector,
                                    PheromoneUpdater pheromoneUpdater,
                                    CSPSolver solver) {

        List<Assignment> cycleAssignments = new ArrayList<>();
        Assignment cycleBest = Assignment.empty();

        logger.debug("Starting cycle with {} ants", parameters.numberOfAnts());

        // Each ant constructs a solution
        for (int ant = 0; ant < parameters.numberOfAnts(); ant++) {
            Assignment assignment = constructor.construct(problem, pheromones, parameters,
                                                         variableSelector, valueSelector, solver);

            if (assignment.size() > 0) {
                cycleAssignments.add(assignment.snapshot());
                logger.trace("Ant {} constructed assignment with {}/{} variables",
                           ant, assignment.size(), problem.size());

                // Track best in this cycle
                if (assignment.size() > cycleBest.size()) {
                    cycleBest = assignment.snapshot();
                    logger.debug("New cycle best: {}/{} variables", cycleBest.size(), problem.size());
                }

                // Track global best
                if (assignment.size() > bestAssignment.size()) {
                    bestAssignment = assignment.snapshot();
                    logger.info("New global best: {}/{} variables", bestAssignment.size(), problem.size());
                }
            } else {
                logger.trace("Ant {} failed to construct assignment", ant);
            }
        }

        // Update pheromones based on all assignments in this cycle
        logger.debug("Updating pheromones with {} assignments", cycleAssignments.size());
        pheromones = pheromoneUpdater.update(pheromones, cycleAssignments, bestAssignment, parameters);

        return cycleBest;
    }

    public ACOParameters getParameters() {
        return parameters;
    }

    public PheromoneMatrix getPheromones() {
        return pheromones;
    }

    public Assignment getBestAssignment() {
        return bestAssignment;
    }
}