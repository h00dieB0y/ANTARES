# Choco Solver Integration for ANTARES

## Overview

The `ChocoCSPSolver` class integrates Choco Solver with the ANTARES ACO framework, combining the power of constraint propagation with ant colony optimization for solving CSPs.

## Implementation Summary

### What Was Fixed

1. **Critical Errors Fixed:**
   - Fixed `pheromones.get()` → `pheromones.getAmount()` method call
   - Added type-safe helper method `getPheromoneAmount()` to handle generic type conversions
   - Fixed `Solution.getIntVal()` null check (returns primitive int, not Integer)
   - Fixed `.toList()` compatibility issues with older Java versions

2. **Empty Methods Completed:**
   - `onContradiction()`: Applies pheromone evaporation on constraint failures
   - `beforeRestart()`: Evaporates pheromones before each restart
   - `afterRestart()`: Clamps pheromone levels to maintain MIN-MAX bounds
   - `onSolution()`: Deposits pheromones on successful solution paths
   - `selectValue()`: Implements ACO-based probabilistic value selection
   - `getPheromoneAmount()`: Type-safe pheromone retrieval

3. **Key Features Implemented:**
   - ACO-guided search strategy for Choco Solver
   - Pheromone-based value selection using roulette wheel
   - Automatic pheromone updates (evaporation + deposition)
   - Restart strategy with pheromone management
   - Bridge between ANTARES and Choco constraint models

## Usage

### Basic Example

```java

```

### Running the Sudoku Example

```bash
# Run the Choco-based Sudoku test
mvn exec:java -Dexec.mainClass="student.imt.antares.examples.SudokuChocoTest"
```

## Architecture

### Class Hierarchy

```
ChocoCSPSolver (implements CSPSolver)
├── ACOSearchStrategy (extends AbstractStrategy<IntVar>)
│   ├── Implements IMonitorRestart
│   ├── Implements IMonitorSolution
│   └── Implements IMonitorContradiction
└── AntaresConstraintWrapper (extends Constraint)
```

### How It Works

1. **Model Building:**
   - Converts ANTARES variables to Choco IntVar variables
   - Wraps ANTARES constraints as Choco constraints
   - Initializes pheromone matrix

2. **Search Strategy:**
   - Uses ACO-guided variable and value selection
   - Selects values probabilistically based on pheromone levels
   - Applies roulette wheel selection weighted by pheromone^alpha

3. **Pheromone Management:**
   - **On Contradiction:** Evaporates pheromones (discourages bad paths)
   - **On Solution:** Deposits pheromones on successful paths (reinforces good paths)
   - **On Restart:** Evaporates and clamps pheromones to maintain bounds

4. **Restart Strategy:**
   - Restarts on every solution found
   - Monotonic cutoff with 100 failure threshold
   - Unlimited restarts within max cycles

## ACO Parameters Guide

| Parameter | Description | Typical Range | Effect |
|-----------|-------------|---------------|--------|
| `alpha` | Pheromone importance | 1.0 - 5.0 | Higher = more exploitation of known paths |
| `beta` | Heuristic importance | 0.0 - 5.0 | Use 0.0 when no heuristic available |
| `rho` | Evaporation rate | 0.01 - 0.1 | Lower = more exploration, slower convergence |
| `tauMin` | Min pheromone | > 0 | Prevents path starvation |
| `tauMax` | Max pheromone | > tauMin | Prevents premature convergence |
| `numberOfAnts` | Colony size | 10 - 50 | More ants = more diversity but slower |

## Comparison with BasicCSPSolver

| Feature | BasicCSPSolver | ChocoCSPSolver |
|---------|----------------|----------------|
| Constraint Propagation | Basic forward checking | Advanced Choco propagation |
| Search Strategy | Manual ACO implementation | Integrated ACO + Choco |
| Performance | Good for small problems | Better for complex CSPs |
| Scalability | Limited | High (leverages Choco) |
| Restart Support | No | Yes (with pheromone management) |

## Extending the Implementation

### Adding Custom Constraint Mapping

Currently, constraints are wrapped in `AntaresConstraintWrapper`. To improve performance, you can map specific ANTARES constraint types to native Choco constraints:

```java
private void postChocoConstraint(student.imt.antares.problem.Constraint antaresConstraint) {
    // Example: Map AllDifferent constraints
    if (antaresConstraint instanceof AllDifferentConstraint) {
        Set<Variable<?>> involved = antaresConstraint.getInvolvedVariables();
        IntVar[] chocoVars = involved.stream()
            .map(variableMapping::get)
            .toArray(IntVar[]::new);
        chocoModel.allDifferent(chocoVars).post();
    } else {
        // Fallback to wrapper
        chocoModel.post(new AntaresConstraintWrapper(antaresConstraint, variableMapping));
    }
}
```

### Custom Variable Selection

You can modify `selectVariable()` in `ACOSearchStrategy` to use different heuristics:

```java
private IntVar selectVariable() {
    // Example: Select variable with smallest domain
    IntVar best = null;
    int minDomain = Integer.MAX_VALUE;
    for (IntVar v : vars) {
        if (!v.isInstantiated() && v.getDomainSize() < minDomain) {
            best = v;
            minDomain = v.getDomainSize();
        }
    }
    return best;
}
```

## Troubleshooting

### Issue: Solver doesn't find solutions
- **Try:** Increase `maxCycles` or adjust `numberOfAnts`
- **Try:** Lower `rho` for more exploration
- **Try:** Increase `tauMax` for stronger pheromone signals

### Issue: Solutions found but not optimal
- **Try:** Increase `alpha` for more exploitation
- **Try:** Add heuristic information (increase `beta`)
- **Try:** Implement custom constraint mapping

### Issue: Slow performance
- **Try:** Reduce `numberOfAnts`
- **Try:** Implement native Choco constraint mappings
- **Try:** Use problem-specific variable selection heuristics

## References

- [Choco Solver Documentation](https://choco-solver.org/)
- MAX-MIN Ant System (MMAS) algorithm
- ANTARES ACO Framework

## License

Same as the ANTARES project.

