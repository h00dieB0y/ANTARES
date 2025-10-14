package student.imt.antares.pheromone;

import java.util.List;

import student.imt.antares.colony.ACOParameters;
import student.imt.antares.colony.PheromoneMatrix;
import student.imt.antares.problem.Assignment;

public interface PheromoneUpdater {
        PheromoneMatrix update(PheromoneMatrix currentPheromones,
                          List<Assignment> cycleAssignments,
                          Assignment bestOverall,
                          ACOParameters parameters);
}
