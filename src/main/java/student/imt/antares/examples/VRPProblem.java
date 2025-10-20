package student.imt.antares.examples;

import java.util.*;
import student.imt.antares.problem.*;

public class VRPProblem {

    // Représente une affectation d'un client à un véhicule
    public static class ClientAssignment {
        public final int client;
        public final int vehicle;
        public ClientAssignment(int client, int vehicle) {
            this.client = client;
            this.vehicle = vehicle;
        }
        @Override public String toString() { return "Client" + client + "-Veh" + vehicle; }
    }

    // Contrainte pour la capacité maximale d'un véhicule
    private static class CapacityConstraint implements Constraint {
        private final int vehicle;
        private final int maxLoad;
        private final int[] demands; // demande pour chaque client
        private final List<Variable> variables; // une par client

        public CapacityConstraint(int vehicle, int maxLoad, int[] demands, List<Variable> variables) {
            this.vehicle = vehicle;
            this.maxLoad = maxLoad;
            this.demands = demands;
            this.variables = variables;
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            int total = 0;
            for (int i = 0; i < variables.size(); i++) {
                Optional<Integer> valOpt = assignment.getValue(variables.get(i));
                if (valOpt.isPresent() && (valOpt.get() == vehicle)) {
                    total += demands[i];
                }
            }
            return total <= maxLoad;
        }
        @Override public Set<Variable> getInvolvedVariables() { return new HashSet<>(variables); }
        @Override public String toString() { return "Veh" + vehicle + "-MaxCap(" + maxLoad + ")"; }
    }

    // Chaque client doit être affecté à un véhicule (contrainte d'affectation obligatoire)
    private static class MustServeConstraint implements Constraint {
        private final Variable clientVar;
        public MustServeConstraint(Variable clientVar) { this.clientVar = clientVar; }
        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            return assignment.getValue(clientVar).isPresent();
        }
        @Override public Set<Variable> getInvolvedVariables() { return Set.of(clientVar); }
        @Override public String toString() { return "ClientServed(" + clientVar.name() + ")"; }
    }

    // Modélisation du problème
    public static Problem create(double[][] distances, int[] demands, int[] vehicleCaps) {
        int nClients = distances.length - 1; // index 0 = dépôt
        int nVehicles = vehicleCaps.length;

        // les variables = un choix de véhicule pour chaque client
        List<Variable> clientVars = new ArrayList<>();
        for (int c = 1; c <= nClients; c++) { // clients = 1..n
            Set<Integer> domain = new HashSet<>();
            for (int v = 0; v < nVehicles; v++) domain.add(v);
            clientVars.add(new Variable("Client" + c, domain));
        }

        // Contraintes : capacité pour chaque véhicule
        List<Constraint> constraints = new ArrayList<>();
        for (int v = 0; v < nVehicles; v++) {
            constraints.add(new CapacityConstraint(v, vehicleCaps[v], demands, clientVars));
        }
        // Chaque client doit être affecté à un véhicule
        for (Variable var : clientVars) constraints.add(new MustServeConstraint(var));

        return new Problem(clientVars, constraints);
    }

    // Calcul du coût total (simplifié : tournée = dépôt → clients → dépôt, pas d’ordre optimisé)
    public static double totalDistance(double[][] dist, Assignment assign, int nVehicles) {
        double sum = 0;
        for (int v = 0; v < nVehicles; v++) {
            List<Integer> route = new ArrayList<>();
            for (Variable var : assign.getAssignedVariables()) {
                int cli = Integer.parseInt(var.name().substring(6)); // "ClientX"
                Optional<Integer> vehicleOpt = assign.getValue(var);
                if (vehicleOpt.isPresent() && vehicleOpt.get() == v) {
                    route.add(cli);
                }
            }
            // Dépôt à premier client
            if (!route.isEmpty()) {
                sum += dist[0][route.get(0)];
                for (int i = 1; i < route.size(); i++) sum += dist[route.get(i-1)][route.get(i)];
                sum += dist[route.get(route.size()-1)][0]; // retour dépôt
            }
        }
        return sum;
    }

    // Matrice de distances, demandes et capacités - exemple pour 5 clients, 2 véhicules
    public static double[][] sampleDistances() {
        return new double[][] {
            {0, 10, 8, 7, 6, 12}, // depot
            {10, 0, 4, 2, 8, 6},
            {8, 4, 0, 3, 7, 9},
            {7, 2, 3, 0, 2, 5},
            {6, 8, 7, 2, 0, 4},
            {12, 6, 9, 5, 4, 0}
        };
    }
    public static int[] sampleDemands() { return new int[]{2, 5, 4, 2, 3}; }
    public static int[] sampleCaps() { return new int[]{7, 10}; }
}
