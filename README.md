# ANTARES

**Ant Algorithm for Trail-based Exploration & Resolution Systems**

An implementation of the Ant-CP algorithm for solving Constraint Satisfaction Problems (CSP) using Ant Colony Optimization (ACO).

---

## Overview

ANTARES implements the MAX-MIN Ant System (MMAS) approach to CSP solving, as described in the research literature on ant colony optimization for constraint satisfaction. The implementation follows the Ant-CP algorithm, combining pheromone-guided search with constraint propagation techniques to efficiently explore the solution space.

### Key Features

- **Faithful Research Implementation**: Adheres to the Ant-CP algorithm specification from ACO research papers
- **Domain-Driven Design**: Clean separation between CSP domain model, ACO components, and solution construction
- **Extensible Architecture**: Strategy pattern-based design allows easy experimentation with different selection heuristics and pheromone update strategies
- **Type-Safe**: Leverages Java 21 features including records, generics, and pattern matching

---

## Algorithm

The implementation follows the **Ant-CP algorithm** for solving CSPs:

1. **Initialization**: Pheromone trails set to τ_max
2. **Iterative Construction**: Each ant builds a solution by:
   - Selecting variables using smallest-domain-first heuristic
   - Choosing values probabilistically based on pheromone levels: p(X,v) ∝ [τ(X,v)]^α
   - Propagating constraints with automatic singleton assignment
3. **Pheromone Update**: MAX-MIN strategy with evaporation and bounded trails [τ_min, τ_max]
4. **Termination**: Returns best solution found or stops after max cycles

---

## Architecture

### Core Components

```
antares/
├── problem/              # CSP domain model (DDD)
│   ├── Variable<T>       # CSP variable with domain
│   ├── Constraint        # Constraint interface
│   ├── Assignment        # Variable-value mappings
│   └── Problem           # CSP aggregate root
│
├── colony/               # ACO coordination
│   ├── Colony            # Main ACO orchestrator
│   ├── ACOParameters     # Algorithm hyperparameters
│   └── PheromoneMatrix   # Pheromone trail storage
│
├── construction/         # Solution construction
│   ├── AssignmentConstructor  # Ant-CP implementation
│   ├── VariableSelector       # Variable ordering strategies
│   └── ValueSelector          # Value selection strategies
│
├── pheromone/           # Pheromone management
│   ├── PheromoneUpdater # Update strategy interface
│   └── MaxMinUpdate     # MAX-MIN Ant System
│
├── solver/              # Constraint propagation
│   ├── CSPSolver        # Solver interface
│   └── BasicCSPSolver   # Arc consistency propagation
│
└── examples/            # Applications
    └── SudokuTest       # Demonstration application
```

### Design Patterns

- **Strategy Pattern**: Variable selection, value selection, pheromone updates
- **Domain-Driven Design**: Clear bounded contexts and aggregate roots
- **Immutability**: Value objects (Variable, ACOParameters) for thread safety
- **Fluent API**: Mutable updates with method chaining for performance

---

## Requirements

- **Java**: 21 or higher
- **Maven**: 3.x
- **Memory**: Minimum 512MB heap (recommended: 1GB for large problems)

---

## Installation & Usage

### Build

```bash
mvn clean compile
```

### Run Example

```bash
mvn exec:java
```

### Configure Main Class

Edit `pom.xml` to change the main class:

```xml
<mainClass>student.imt.antares.examples.SudokuTest</mainClass>
```

### Example Usage

```java
// Create CSP problem
Problem problem = /* define your CSP */;

// Configure ACO parameters
ACOParameters params = new ACOParameters(
    2.0,    // alpha: pheromone importance
    0.0,    // beta: heuristic importance (0 for no heuristic)
    0.01,   // rho: evaporation rate (1% per cycle)
    0.01,   // tauMin: minimum pheromone level
    10.0,   // tauMax: maximum pheromone level
    30      // numberOfAnts: ants per cycle
);

// Initialize colony and solve
Colony colony = Colony.create(problem, params);
Assignment solution = colony.solve(
    problem,
    new AssignmentConstructor(),
    VariableSelectors.SMALLEST_DOMAIN_FIRST,
    new ProbabilisticSelection(),
    new MaxMinUpdate(),
    new BasicCSPSolver(problem),
    2000  // max cycles
);

// Validate solution
boolean isValid = problem.isSolution(solution);
```

---

## Configuration & Tuning

### ACO Parameters

The algorithm's behavior is controlled by `ACOParameters`:

| Parameter | Symbol | Description | Typical Range | Default Value |
|-----------|--------|-------------|---------------|---------------|
| `alpha` | α | Pheromone importance | 1.0 - 5.0 | 2.0 |
| `beta` | β | Heuristic importance | 0.0 - 5.0 | 0.0 |
| `rho` | ρ | Evaporation rate | 0.01 - 0.1 | 0.01 |
| `tauMin` | τ_min | Min pheromone | 0.001 - 0.1 | 0.01 |
| `tauMax` | τ_max | Max pheromone | 1.0 - 100.0 | 10.0 |
| `numberOfAnts` | m | Colony size | 10 - 100 | 30 |

### Tuning Guidelines

- **Increase α**: Stronger exploitation of learned knowledge
- **Increase β**: Stronger use of problem-specific heuristics (when available)
- **Decrease ρ**: Slower forgetting, more exploration
- **Narrow [τ_min, τ_max]**: Prevents premature convergence
- **More ants**: Better exploration, but slower cycles

---

## Logging

Uses SLF4J with slf4j-simple backend. Configure logging level in `src/main/resources/simplelogger.properties`:

```properties
org.slf4j.simpleLogger.defaultLogLevel=INFO
org.slf4j.simpleLogger.log.student.imt.antares.colony.Colony=DEBUG
```

Log levels:
- `INFO`: High-level progress (cycle updates, solution found)
- `DEBUG`: Detailed algorithm state (best assignments, pheromone updates)
- `TRACE`: Fine-grained details (individual ant steps, value selection)

---

## Testing

```bash
# Compile and run tests
mvn test

# Run example application
mvn exec:java
```

---

## Authors & Contributors

**Primary Authors:**
- **Manneemi Lekitsoukou** ([H00dieB0y](https://github.com/H00dieB0y))
- **Jean-Baptiste Zinobienne** (Angry-Jay) - <jinozebian@gmail.com>
- **Anas Alaoui** (Ariazoox) - <anas.alaoui2002@gmail.com>

**Institution**: IMT (Institut Mines-Télécom)

---

## License

This project is part of academic work at IMT. Educational and research use permitted.

---

For questions, issues, or contributions, please refer to the project repository or contact the authors.
