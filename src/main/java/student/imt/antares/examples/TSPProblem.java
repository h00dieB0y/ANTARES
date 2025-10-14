package student.imt.antares.examples;

import java.util.*;
import student.imt.antares.problem.*;

public class TSPProblem {

    private record TourConstraint(List<Variable<Integer>> cities) implements Constraint {
            private TourConstraint(List<Variable<Integer>> cities) {
                this.cities = Objects.requireNonNull(cities);
            }

        @Override
            public boolean isSatisfiedBy(Assignment assignment) {
                Set<Integer> seen = new HashSet<>();
                for (Variable<Integer> v : cities) {
                    var val = assignment.getValue(v);
                    if (val.isEmpty()) continue;
                    if (seen.contains(val.get())) return false;
                    seen.add(val.get());
                }
                return true;
            }

        @Override
            public Set<Variable<?>> getInvolvedVariables() {
                return new HashSet<>(cities);
            }

        @Override
        public String toString() {
            return "AllCitiesOnceTour";
        }
        }

    public static Problem create(double[][] distances) {
        int n = distances.length;
        List<Variable<?>> variables = new ArrayList<>();
        // Each city is assigned a unique position in the tour (Permutation CSP)
        for (int i = 0; i < n; i++) {
            Set<Integer> domain = new HashSet<>();
            for (int pos = 0; pos < n; pos++) domain.add(pos);
            variables.add(new Variable<>("City" + i, domain));
        }
        // One constraint: all positions in the tour are unique (each visited once)
        Constraint allUnique = new TourConstraint((List)variables);
        List<Constraint> constraints = List.of(allUnique);
        // You peux ajouter plus de contraintes selon besoins (Ex: boucle fermée)
        return new Problem(variables, constraints);
    }

    public static double tourLength(double[][] distances, Assignment assignment) {
        int n = distances.length;
        int[] tour = new int[n];
        for (Variable<?> var : assignment.getAssignedVariables()) {
            int city = Integer.parseInt(var.name().substring(4)); // "CityX"
            var value = assignment.getValue(var);
            if( value.isEmpty() )
                throw new IllegalArgumentException("Assignment incomplete");
            int pos = (int) value.get();
            tour[pos] = city;
        }
        double sum = 0;
        for (int i = 0; i < n; i++) {
            int from = tour[i];
            int to = tour[(i+1)%n]; // boucle fermée
            sum += distances[from][to];
        }
        return sum;
    }

    public static void printTour(Assignment assignment) {
        System.out.print("Tour: ");
        // Affichage ordre de passage
        assignment.getAssignedVariables().stream()
            .sorted(Comparator.comparingInt(v -> (Integer)assignment.getValue(v).get()))
            .forEach(v -> System.out.print(v.name() + " "));
        System.out.println();
    }

    public static double[][] createSampleInstance() {
        // Exemple TSP 4 villes
        return new double[][] {
        { 0, 29, 20, 21, 16, 31, 100, 12, 4, 31 },
        { 29, 0, 15, 29, 28, 40, 72, 21, 29, 41 },
        { 20, 15, 0, 15, 14, 25, 81, 9, 23, 27 },
        { 21, 29, 15, 0, 4, 12, 92, 12, 25, 13 },
        { 16, 28, 14, 4, 0, 16, 94, 9, 20, 16 },
        { 31, 40, 25, 12, 16, 0, 95, 24, 36, 3 },
        { 100, 72, 81, 92, 94, 95, 0, 90, 101, 99 },
        { 12, 21, 9, 12, 9, 24, 90, 0, 15, 25 },
        { 4, 29, 23, 25, 20, 36, 101, 15, 0, 35 },
        { 31, 41, 27, 13, 16, 3, 99, 25, 35, 0 }
    };
    }
}
