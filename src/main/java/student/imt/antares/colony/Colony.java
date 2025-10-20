package student.imt.antares.colony;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.construction.AssignmentConstructor;
import student.imt.antares.construction.ProbabilisticSelection;
import student.imt.antares.construction.VariableSelector;
import student.imt.antares.pheromone.MaxMinUpdate;
import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Problem;
import student.imt.antares.solver.BasicCSPSolver;

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
                            ProbabilisticSelection valueSelector,
                            MaxMinUpdate pheromoneUpdater,
                            BasicCSPSolver solver,
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
                                    ProbabilisticSelection valueSelector,
                                    MaxMinUpdate pheromoneUpdater,
                                    BasicCSPSolver solver) {

        List<Assignment> cycleAssignments = new ArrayList<>();
        Assignment cycleBest = Assignment.empty();

        // Each ant constructs a solution
        for (int ant = 0; ant < parameters.numberOfAnts(); ant++) {
            Assignment assignment = constructor.construct(problem, pheromones, parameters,
                                                         variableSelector, valueSelector, solver);

            if (assignment.size() > 0) {
                // Snapshot creates defensive copy - necessary because Assignment is mutable
                // and will be reused/modified in subsequent ant constructions
                cycleAssignments.add(assignment.snapshot());

                // Track best in this cycle
                if (assignment.size() > cycleBest.size()) {
                    // Snapshot to preserve this assignment state before future mutations
                    cycleBest = assignment.snapshot();
                }

                // Track global best
                if (assignment.size() > bestAssignment.size()) {
                    // Snapshot to preserve this best solution - it must not change
                    bestAssignment = assignment.snapshot();
                }
            }
        }

        // Update pheromones based on all assignments in this cycle
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