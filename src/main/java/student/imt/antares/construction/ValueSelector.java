package student.imt.antares.construction;

import java.util.Optional;
import java.util.Set;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Variable;

public interface ValueSelector {
    <T> Optional<T> select(Variable<T> variable, Set<T> domain, PheromoneMatrix pheromones, ACOParameters params);
}
