package student.imt.antares.examples;

import java.util.*;
import student.imt.antares.problem.*;

public class TSPProblem {

    private record TourConstraint(List<Variable> cities) implements Constraint {
            private TourConstraint(List<Variable> cities) {
                this.cities = Objects.requireNonNull(cities);
            }

        @Override
            public boolean isSatisfiedBy(Assignment assignment) {
                Set<Object> seen = new HashSet<>();
                for (Variable v : cities) {
                    var val = assignment.getValue(v);
                    if (val.isEmpty()) continue;
                    Object value = val.get();
                    if (!(value instanceof Integer)) {
                        throw new IllegalArgumentException(
                            "TSP tour constraint expects Integer positions, got: " + value.getClass().getSimpleName());
                    }
                    if (seen.contains(value)) return false;
                    seen.add(value);
                }
                return true;
            }

        @Override
            public Set<Variable> getInvolvedVariables() {
                return new HashSet<>(cities);
            }

        @Override
        public String toString() {
            return "AllCitiesOnceTour";
        }
        }

    public static Problem create(double[][] distances) {
        int n = distances.length;
        List<Variable> variables = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Set<Integer> domain = new HashSet<>();
            for (int pos = 0; pos < n; pos++) domain.add(pos);
            variables.add(new Variable("City" + i, domain));
        }
        Constraint allUnique = new TourConstraint(variables);
        List<Constraint> constraints = List.of(allUnique);
        return new Problem(variables, constraints);
    }

    public static double tourLength(double[][] distances, Assignment assignment) {
        int n = distances.length;
        int[] tour = new int[n];
        for (Variable var : assignment.getAssignedVariables()) {
            int city = Integer.parseInt(var.name().substring(4));
            var value = assignment.getValue(var);
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Assignment incomplete");
            }
            int pos = value.get();
            tour[pos] = city;
        }
        double sum = 0;
        for (int i = 0; i < n; i++) {
            int from = tour[i];
            int to = tour[(i+1)%n];
            sum += distances[from][to];
        }
        return sum;
    }

    public static void printTour(Assignment assignment) {
    }

    public static double[][] createSampleInstance() {
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
