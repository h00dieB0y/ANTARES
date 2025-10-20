package student.imt.antares.examples;

import java.util.*;

import student.imt.antares.problem.Assignment;
import student.imt.antares.problem.Constraint;
import student.imt.antares.problem.Problem;
import student.imt.antares.problem.Variable;

/**
 * Teacher scheduling CSP problem.
 *
 * Schedule 3 teachers (A, B, C) across 3 rooms and 3 time slots.
 *
 * Constraints:
 * 1. One teacher cannot teach in two rooms at the same time (alldifferent per time slot)
 * 2. Physics course: Teacher A in room 1 at time slot 1
 * 3. Teacher B is not available at time slot 3
 * 4. Teacher C must teach in room 2 at least once
 * 5. Each teacher teaches exactly twice during the day
 */
public class TeacherSchedulingProblem {

    // Teachers
    public static final int TEACHER_A = 1;
    public static final int TEACHER_B = 2;
    public static final int TEACHER_C = 3;

    private static final String[] TEACHER_NAMES = {"", "A", "B", "C"};

    private static final int ROOMS = 3;
    private static final int TIMES = 3;

    /**
     * Creates the teacher scheduling CSP problem.
     */
    public static Problem create() {
        List<Variable> variables = new ArrayList<>();
        Variable[][] schedule = new Variable[ROOMS + 1][TIMES + 1];

        // Create variables for each (room, time) pair: P[room][time] = teacher
        for (int room = 1; room <= ROOMS; room++) {
            for (int time = 1; time <= TIMES; time++) {
                Set<Integer> domain = getDomain(room, time);
                String varName = "R" + room + "T" + time;
                Variable var = new Variable(varName, domain);
                schedule[room][time] = var;
                variables.add(var);
            }
        }

        List<Constraint> constraints = new ArrayList<>();

        // Constraint 1: One teacher cannot teach in two rooms at same time (alldifferent per time)
        for (int time = 1; time <= TIMES; time++) {
            List<Variable> roomsAtTime = new ArrayList<>();
            for (int room = 1; room <= ROOMS; room++) {
                roomsAtTime.add(schedule[room][time]);
            }
            constraints.add(new AllDifferentConstraint(roomsAtTime, "Time " + time + " - all different"));
        }

        // Constraint 2: Physics course - Teacher A in room 1 at time 1
        constraints.add(new FixedAssignmentConstraint(schedule[1][1], TEACHER_A, "Room 1, Time 1 = A (Physics)"));

        // Constraint 3: Teacher B not available at time slot 3
        for (int room = 1; room <= ROOMS; room++) {
            constraints.add(new ForbiddenValueConstraint(schedule[room][3], TEACHER_B,
                "Room " + room + ", Time 3 != B"));
        }

        // Constraint 4: Teacher C must teach in room 2 at least once
        List<Variable> room2Slots = new ArrayList<>();
        for (int time = 1; time <= TIMES; time++) {
            room2Slots.add(schedule[2][time]);
        }
        constraints.add(new AtLeastOnceConstraint(room2Slots, TEACHER_C, "Teacher C in Room 2 at least once"));

        // Constraint 5: Each teacher teaches exactly twice
        List<Variable> allSlots = new ArrayList<>();
        for (int room = 1; room <= ROOMS; room++) {
            for (int time = 1; time <= TIMES; time++) {
                allSlots.add(schedule[room][time]);
            }
        }

        for (int teacher = TEACHER_A; teacher <= TEACHER_C; teacher++) {
            constraints.add(new ExactCountConstraint(allSlots, teacher, 2,
                "Teacher " + TEACHER_NAMES[teacher] + " teaches exactly 2 times"));
        }

        return new Problem(variables, constraints);
    }

    /**
     * Returns the domain for a given (room, time) slot.
     */
    private static Set<Integer> getDomain(int room, int time) {
        // Room 1, Time 1 must be Teacher A (Constraint 2)
        if (room == 1 && time == 1) {
            return Set.of(TEACHER_A);
        }

        // Time slot 3: Teacher B not available (Constraint 3)
        if (time == 3) {
            return Set.of(TEACHER_A, TEACHER_C);
        }

        // Default: all teachers available
        return Set.of(TEACHER_A, TEACHER_B, TEACHER_C);
    }

    /**
     * Constraint: Variable must have a specific fixed value.
     */
    private static class FixedAssignmentConstraint implements Constraint {
        private final Variable var;
        private final int value;
        private final String name;

        public FixedAssignmentConstraint(Variable var, int value, String name) {
            this.var = var;
            this.value = value;
            this.name = name;
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            return assignment.getValue(var).map(v -> v == value).orElse(true);
        }

        @Override
        public Set<Variable> getInvolvedVariables() {
            return Set.of(var);
        }

        @Override
        public String toString() {
            return "Fixed(" + name + ")";
        }
    }

    /**
     * Constraint: Variable cannot have a specific value.
     */
    private static class ForbiddenValueConstraint implements Constraint {
        private final Variable var;
        private final int forbiddenValue;
        private final String name;

        public ForbiddenValueConstraint(Variable var, int forbiddenValue, String name) {
            this.var = var;
            this.forbiddenValue = forbiddenValue;
            this.name = name;
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            return assignment.getValue(var).map(v -> v != forbiddenValue).orElse(true);
        }

        @Override
        public Set<Variable> getInvolvedVariables() {
            return Set.of(var);
        }

        @Override
        public String toString() {
            return "Forbidden(" + name + ")";
        }
    }

    /**
     * Constraint: All variables must have different values (alldifferent).
     */
    private static class AllDifferentConstraint implements Constraint {
        private final List<Variable> variables;
        private final String name;

        public AllDifferentConstraint(List<Variable> variables, String name) {
            this.variables = variables;
            this.name = name;
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            Set<Integer> seenValues = new HashSet<>();

            for (Variable var : variables) {
                Optional<Integer> value = assignment.getValue(var);
                if (value.isPresent()) {
                    if (seenValues.contains(value.get())) {
                        return false; // Duplicate found
                    }
                    seenValues.add(value.get());
                }
            }

            return true; // All assigned values are different
        }

        @Override
        public Set<Variable> getInvolvedVariables() {
            return new HashSet<>(variables);
        }

        @Override
        public String toString() {
            return "AllDifferent(" + name + ")";
        }
    }

    /**
     * Constraint: A specific value must appear at least once in the variables.
     */
    private static class AtLeastOnceConstraint implements Constraint {
        private final List<Variable> variables;
        private final int requiredValue;
        private final String name;

        public AtLeastOnceConstraint(List<Variable> variables, int requiredValue, String name) {
            this.variables = variables;
            this.requiredValue = requiredValue;
            this.name = name;
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            boolean allAssigned = true;
            boolean foundValue = false;

            for (Variable var : variables) {
                Optional<Integer> value = assignment.getValue(var);
                if (value.isEmpty()) {
                    allAssigned = false;
                } else if (value.get() == requiredValue) {
                    foundValue = true;
                }
            }

            // If not all assigned, constraint is still satisfiable
            if (!allAssigned) {
                return true;
            }

            // All assigned: must have found the required value
            return foundValue;
        }

        @Override
        public Set<Variable> getInvolvedVariables() {
            return new HashSet<>(variables);
        }

        @Override
        public String toString() {
            return "AtLeastOnce(" + name + ")";
        }
    }

    /**
     * Constraint: Exact count of a specific value across all variables.
     */
    private static class ExactCountConstraint implements Constraint {
        private final List<Variable> variables;
        private final int targetValue;
        private final int requiredCount;
        private final String name;

        public ExactCountConstraint(List<Variable> variables, int targetValue,
                                   int requiredCount, String name) {
            this.variables = variables;
            this.targetValue = targetValue;
            this.requiredCount = requiredCount;
            this.name = name;
        }

        @Override
        public boolean isSatisfiedBy(Assignment assignment) {
            int count = 0;
            boolean allAssigned = true;

            for (Variable var : variables) {
                Optional<Integer> value = assignment.getValue(var);
                if (value.isEmpty()) {
                    allAssigned = false;
                } else if (value.get() == targetValue) {
                    count++;
                }
            }

            // If not all assigned, accept if count doesn't exceed required
            if (!allAssigned) {
                return count <= requiredCount;
            }

            // All assigned: must match exactly
            return count == requiredCount;
        }

        @Override
        public Set<Variable> getInvolvedVariables() {
            return new HashSet<>(variables);
        }

        @Override
        public String toString() {
            return "ExactCount(" + name + ")";
        }
    }

    /**
     * Prints the schedule in a readable format.
     */
    public static void printSchedule(Assignment assignment) {
        System.out.println("\n=== TEACHER SCHEDULE ===");
        System.out.println("Assignments (rows = rooms 1..3, columns = time slots 1..3)");
        System.out.println("         Time1   Time2   Time3");
        System.out.println("-------------------------------------");

        for (int room = 1; room <= ROOMS; room++) {
            System.out.printf("Room %d   ", room);
            for (int time = 1; time <= TIMES; time++) {
                String varName = "R" + room + "T" + time;
                Variable var = findVariable(assignment, varName);

                if (var != null) {
                    Optional<Integer> value = assignment.getValue(var);
                    if (value.isPresent()) {
                        String teacher = TEACHER_NAMES[value.get()];
                        System.out.printf("  %-6s", teacher);
                    } else {
                        System.out.print("  ???   ");
                    }
                } else {
                    System.out.print("  ---   ");
                }
            }
            System.out.println();
        }
        System.out.println("-------------------------------------");
    }

    private static Variable findVariable(Assignment assignment, String name) {
        for (Variable var : assignment.getAssignedVariables()) {
            if (var.name().equals(name)) {
                @SuppressWarnings("unchecked")
                Variable intVar = (Variable) var;
                return intVar;
            }
        }
        return null;
    }

    /**
     * Validates and displays constraint satisfaction.
     */
    public static void validateSchedule(Problem problem, Assignment assignment) {
        System.out.println("\n=== VALIDATION ===");
        System.out.println("Complete: " + assignment.isComplete(problem.size()));
        System.out.println("Valid: " + problem.isSolution(assignment));
        System.out.println("Variables assigned: " + assignment.size() + "/" + problem.size());

        if (problem.isSolution(assignment)) {
            System.out.println("\n✓ Valid schedule found!");
            System.out.println("\nConstraints satisfied:");
            System.out.println("  ✓ One teacher per room at each time slot");
            System.out.println("  ✓ Teacher A in Room 1 at Time 1 (Physics)");
            System.out.println("  ✓ Teacher B not scheduled at Time 3");
            System.out.println("  ✓ Teacher C teaches in Room 2 at least once");
            System.out.println("  ✓ Each teacher teaches exactly twice");
        } else {
            System.out.println("\n✗ Constraints violated or incomplete");
        }
    }

    /**
     * Prints teaching statistics.
     */
    public static void printStatistics(Assignment assignment) {
        System.out.println("\n=== STATISTICS ===");

        int[] teacherCounts = new int[4]; // Index 0 unused, 1-3 for teachers A, B, C

        for (int room = 1; room <= ROOMS; room++) {
            for (int time = 1; time <= TIMES; time++) {
                String varName = "R" + room + "T" + time;
                Variable var = findVariable(assignment, varName);
                if (var != null) {
                    assignment.getValue(var).ifPresent(teacher -> teacherCounts[teacher]++);
                }
            }
        }

        System.out.println("Teaching load:");
        for (int teacher = TEACHER_A; teacher <= TEACHER_C; teacher++) {
            System.out.printf("  Teacher %s: %d sessions\n",
                            TEACHER_NAMES[teacher], teacherCounts[teacher]);
        }
    }
}
