package student.imt.antares.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.*;
import student.imt.antares.construction.*;
import student.imt.antares.pheromone.*;
import student.imt.antares.problem.*;
import student.imt.antares.solver.*;

import java.util.*;

public class VRPTest {
    
    private static final Logger logger = LoggerFactory.getLogger(VRPTest.class);
    public static void main(String[] args) {
        double[][] dist = VRPProblem.sampleDistances();
        int[] demands = VRPProblem.sampleDemands();
        int[] caps = VRPProblem.sampleCaps();
        Problem problem = VRPProblem.create(dist, demands, caps);
        Assignment solution = solveWithACO(problem, 1000);
        printResult(problem, solution, dist, caps.length);
    }

    private static Assignment solveWithACO(Problem problem, int maxCycles) {
        ACOParameters params = new ACOParameters(
            2.0, 2.0, 0.1, 0.01, 10.0, 20
        );
        Colony colony = Colony.create(problem, params);
        AssignmentConstructor constructor = new AssignmentConstructor();
        ProbabilisticSelection valueSelector = new ProbabilisticSelection(1234);
        MaxMinUpdate pheromoneUpdater = new MaxMinUpdate();
        BasicCSPSolver solver = new BasicCSPSolver(problem);

        return colony.solve(problem, constructor, VariableSelectors.RANDOM,
            valueSelector, pheromoneUpdater, solver, maxCycles);
    }

    private static void printResult(Problem problem, Assignment solution, double[][] dist, int nVehicles) {
        if (solution == null) {
            logger.info("No solution found!");
            return;
        }

        if (!problem.isConsistent(solution)) {
            logger.warn("Solution is inconsistent!");
            return;
        }

        logger.info("Solution found!");
        logger.info("Vehicle Routes:");
        logger.info("===============");
        
        // Group clients by vehicle
        Map<Integer, List<Integer>> vehicleRoutes = new HashMap<>();
        for (int v = 0; v < nVehicles; v++) {
            vehicleRoutes.put(v, new ArrayList<>());
        }
        
        for (Variable clientVar : solution.getAssignedVariables()) {
            solution.getValue(clientVar).ifPresent(vehicleId -> {
                String clientName = clientVar.name();
                int clientId = Integer.parseInt(clientName.replace("Client", ""));
                vehicleRoutes.get(vehicleId).add(clientId);
            });
        }
        
        // Print routes for each vehicle
        double totalDistance = 0.0;
        for (int v = 0; v < nVehicles; v++) {
            List<Integer> route = vehicleRoutes.get(v);
            if (route.isEmpty()) {
                logger.info("Vehicle {}: No clients assigned", v);
                continue;
            }
            
            StringBuilder routeStr = new StringBuilder();
            routeStr.append("Vehicle ").append(v).append(": Depot");
            
            double vehicleDistance = 0.0;
            int currentLocation = 0; // depot
            
            for (int client : route) {
                routeStr.append(" -> ").append(client);
                vehicleDistance += dist[currentLocation][client];
                currentLocation = client;
            }
            
            // Return to depot
            routeStr.append(" -> Depot");
            vehicleDistance += dist[currentLocation][0];
            
            routeStr.append(String.format(" (Distance: %.2f)", vehicleDistance));
            
            if (logger.isInfoEnabled()) {
                logger.info(routeStr.toString());
            }
            
            totalDistance += vehicleDistance;
        }
        
        logger.info("");
        if (logger.isInfoEnabled()) {
            logger.info("Total Distance: {}", String.format("%.2f", totalDistance));
        }
    }
}
