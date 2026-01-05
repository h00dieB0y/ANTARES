package student.imt.antares.examples;

import org.chocosolver.parser.SetUpException;
import org.chocosolver.parser.xcsp.XCSP;
import org.chocosolver.solver.*;
import org.chocosolver.solver.search.strategy.selectors.variables.AntiFirstFail;
import org.chocosolver.solver.variables.IntVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.solver.AntColonyMetaHeuristicSolver;

public class XCSPTest {
    
    private static final Logger logger = LoggerFactory.getLogger(XCSPTest.class);
    private static final String DEFAULT_PATH = "/Users/manneemilekitsoukou/Downloads/CarSequencing/CarSequencing-m1-jcr/CarSequencing-16-81.xml";
    
    private static class Config {
        String path = DEFAULT_PATH;
        double alpha;
        double beta;
        double rho;
        double tauMin;
        double tauMax;
        int numberOfAnts;
        int maxCycles = 1000;
        long seed = System.currentTimeMillis();

        Config(ACOParameters defaults) {
            this.alpha = defaults.alpha();
            this.beta = defaults.beta();
            this.rho = defaults.rho();
            this.tauMin = defaults.tauMin();
            this.tauMax = defaults.tauMax();
            this.numberOfAnts = defaults.numberOfAnts();
        }
    }
    
    public static void main(String[] args) throws SetUpException {
        Config config = parseArguments(args);
        XCSP xcspParser = setupParser(config.path);
        
        if (xcspParser != null) {
            ACOParameters acoParams = new ACOParameters(
                config.alpha,
                config.beta,
                config.rho,
                config.tauMin,
                config.tauMax,
                config.numberOfAnts
            );

            runSolver(xcspParser, acoParams, config.seed, config.path, config.maxCycles);
        }
    }
    
    private static Config parseArguments(String[] args) {
        Config config = new Config(ACOParameters.withDefaults());
        
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            int increment = 1;
            
            if (i + 1 < args.length) {
                String value = args[i + 1];
                boolean recognized = true;
                
                switch (arg) {
                    case "-path" -> config.path = value;
                    case "-alpha" -> config.alpha = Double.parseDouble(value);
                    case "-beta" -> config.beta = Double.parseDouble(value);
                    case "-rho" -> config.rho = Double.parseDouble(value);
                    case "-tauMin" -> config.tauMin = Double.parseDouble(value);
                    case "-tauMax" -> config.tauMax = Double.parseDouble(value);
                    case "-ants" -> config.numberOfAnts = Integer.parseInt(value);
                    case "-maxCycles" -> config.maxCycles = Integer.parseInt(value);
                    case "-seed" -> config.seed = Long.parseLong(value);
                    default -> recognized = false;
                }
                
                if (recognized) {
                    increment = 2;
                } else {
                    logger.warn("Unknown argument: {}", arg);
                }
            } else {
                logger.warn("Unknown argument: {}", arg);
            }
            
            i += increment;
        }
        
        return config;
    }
    
    private static XCSP setupParser(String path) throws SetUpException {
        XCSP xcspParser = new XCSP();
        if (xcspParser.setUp(path)) {
            xcspParser.createSolver();
            xcspParser.buildModel();
            return xcspParser;
        }
        return null;
    }
    
    private static void runSolver(XCSP xcspParser, ACOParameters acoParams, long seed, String path, int maxCycles) {
        Model model = xcspParser.getModel();

        IntVar[] allVars = model.retrieveIntVars(true);

        AntColonyMetaHeuristicSolver acoStrategy = new AntColonyMetaHeuristicSolver(
            allVars,
            new AntiFirstFail(model),
            acoParams,
            seed
        );

        Solver solver = model.getSolver();
        solver.setSearch(acoStrategy);

        // Calculate total restarts needed: numberOfAnts × maxCycles
        int totalRestarts = acoParams.numberOfAnts() * maxCycles;
        solver.limitRestart(totalRestarts);

        long startTime = System.currentTimeMillis();
        solver.solve();
        long endTime = System.currentTimeMillis();

        printResults(acoStrategy, seed, path, acoParams, endTime - startTime);
    }
    
    private static void printResults(AntColonyMetaHeuristicSolver acoStrategy, long seed, 
                                     String path, ACOParameters acoParams, long runtimeMs) {
        acoStrategy.printProgressCSV();
        
        String problemName = extractProblemName(path);
        int totalCycles = acoStrategy.getProgressHistory().size();
        
        acoStrategy.printSummaryCSV(seed, problemName, acoParams, runtimeMs, totalCycles);
    }
    
    private static String extractProblemName(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        return path.substring(lastSlash + 1, lastDot);
    }
}
