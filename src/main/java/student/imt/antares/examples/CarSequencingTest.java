package student.imt.antares.examples;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.solver.AntColonyMetaHeuristicSolver;

public class CarSequencingTest {

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printUsage();
            return;
        }

        CarSequencing.Data selectedData = CarSequencing.Data.P16_81;
        long seed = System.currentTimeMillis();

        // ACO Parameters (defaults from paper)
        double alpha = 1.0;
        double beta = 2.0;
        double rho = 0.1;
        double tauMin = 0.01;
        double tauMax = 4.0;
        int nbAnts = 5;
        int maxCycles = 50;  // Max cycles (default: 500 from paper)

        // Parse arguments
        int argIndex = 0;

        // Arg 0: Instance name
        if (args.length > argIndex) {
            try {
                selectedData = CarSequencing.Data.valueOf(args[argIndex]);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid data name. Available: " +
                    java.util.Arrays.toString(CarSequencing.Data.values()));
                return;
            }
        }
        argIndex++;

        // Arg 1: Seed
        if (args.length > argIndex) {
            try {
                seed = Long.parseLong(args[argIndex]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid seed. Using default.");
            }
        }
        argIndex++;

        // Arg 2: alpha
        if (args.length > argIndex) {
            try {
                alpha = Double.parseDouble(args[argIndex]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid alpha. Using default: " + alpha);
            }
        }
        argIndex++;

        // Arg 3: beta
        if (args.length > argIndex) {
            try {
                beta = Double.parseDouble(args[argIndex]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid beta. Using default: " + beta);
            }
        }
        argIndex++;

        // Arg 4: rho
        if (args.length > argIndex) {
            try {
                rho = Double.parseDouble(args[argIndex]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid rho. Using default: " + rho);
            }
        }
        argIndex++;

        // Arg 5: nbAnts
        if (args.length > argIndex) {
            try {
                nbAnts = Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid nbAnts. Using default: " + nbAnts);
            }
        }
        argIndex++;

        // Arg 6: maxCycles
        if (args.length > argIndex) {
            try {
                maxCycles = Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid maxCycles. Using default: " + maxCycles);
            }
        }

        // Create and build the car sequencing problem
        CarSequencing problem = new CarSequencing();
        problem.data = selectedData;
        problem.buildModel();

        Model model = problem.model;

        // Create ACO parameters with specified values
        ACOParameters acoParams = new ACOParameters(alpha, beta, rho, tauMin, tauMax, nbAnts);

        // Create ACO solver with FirstFail variable selector
        AntColonyMetaHeuristicSolver acoSolver = new AntColonyMetaHeuristicSolver(
            problem.cars,
            new FirstFail(model),
            acoParams,
            maxCycles,
            seed
        );

        // Configure search with ACO strategy
        Solver solver = model.getSolver();
        solver.setSearch(acoSolver);

        // Solve
        long startTime = System.currentTimeMillis();
        solver.solve();
        long endTime = System.currentTimeMillis();

        // Get results
        AntColonyMetaHeuristicSolver.SolutionProgress bestProgress = acoSolver.getBestProgress();
        long runtimeMs = endTime - startTime;

        // Calculate total cycles run (number of entries in progress history)
        int totalCycles = acoSolver.getProgressHistory().size();

        // Print summary CSV on stderr
        acoSolver.printSummaryCSV(seed, selectedData.name(), acoParams, runtimeMs, totalCycles);

        // Print progress CSV on stdout
        acoSolver.printProgressCSV(seed, selectedData.name(), acoParams);
    }

    private static void printUsage() {
        System.out.println("Usage: CarSequencingTest [instance] [seed] [alpha] [beta] [rho] [nbAnts] [maxCycles]");
        System.out.println();
        System.out.println("Arguments (all optional):");
        System.out.println("  instance   : Car sequencing instance (default: P16_81)");
        System.out.println("               Available: " + java.util.Arrays.toString(CarSequencing.Data.values()));
        System.out.println("  seed       : Random seed (default: current time)");
        System.out.println("  alpha      : Pheromone importance (default: 1.0)");
        System.out.println("  beta       : Heuristic importance (default: 2.0)");
        System.out.println("  rho        : Evaporation rate (default: 0.1)");
        System.out.println("  nbAnts     : Number of ants per cycle (default: 5)");
        System.out.println("  maxCycles  : Maximum number of cycles (default: 500)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  CarSequencingTest P16_81 12345");
        System.out.println("  CarSequencingTest P16_81 12345 2.0 0.0 0.01 30 500");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  CSV summary on stderr");
        System.out.println("  Progress history on stdout");
    }
}
