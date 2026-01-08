#!/bin/bash

# Simple experiment script for paper figures
# Figure 1: Influence of alpha
# Figure 2: Influence of rho

RESULTS_DIR="experiments_results"
mkdir -p "${RESULTS_DIR}"

# Instances from paper
INSTANCES=("P16_81" "P26_82" "P41_66" "P6_76")

# 10 seeds for reproducibility
SEEDS=(12345 23456 34567 45678 56789 67890 78901 89012 90123 101234)

# Build JAR if needed
if [ ! -f "target/antares-1.0-SNAPSHOT.jar" ]; then
    echo "Building JAR..."
    mvn clean package -Dmaven.test.skip=true
fi

# Experiment: Influence of pheromone importance (alpha) - Figure 1
# Fixed: beta=0, rho=0.01, nbAnts=30, maxCycles=500
test_pheromone_importance() {
    echo "Testing influence of pheromone importance (alpha) - Figure 1..."
    ALPHAS=(0 1 2 3)
    BETA=0
    RHO=0.01
    NB_ANTS=30
    MAX_CYCLES=50

    SUMMARY_CSV="${RESULTS_DIR}/alpha_influence_summary.csv"
    PROGRESS_CSV="${RESULTS_DIR}/alpha_influence_progress.csv"

    # Create CSV headers
    echo "seed,problem,total_vars,alpha,beta,rho,tau_min,tau_max,num_ants,best_cycle,final_assigned,success,total_cycles,runtime_ms" > "${SUMMARY_CSV}"
    echo "seed,problem,alpha,beta,rho,num_ants,cycle,assigned_variables,total_variables,completion_rate" > "${PROGRESS_CSV}"

    for instance in "${INSTANCES[@]}"; do
        for seed in "${SEEDS[@]}"; do
            for alpha in "${ALPHAS[@]}"; do
                echo "Running ${instance} seed=${seed} alpha=${alpha}..."
                java -jar target/antares-1.0-SNAPSHOT.jar \
                    "${instance}" "${seed}" "${alpha}" "${BETA}" "${RHO}" "${NB_ANTS}" "${MAX_CYCLES}" \
                    >> "${PROGRESS_CSV}" 2>> "${SUMMARY_CSV}"
            done
        done
    done

    echo "Results saved to:"
    echo "  Summary:  ${SUMMARY_CSV}"
    echo "  Progress: ${PROGRESS_CSV}"
}

# Experiment: Influence of evaporation rate (rho) - Figure 2
# Fixed: alpha=2, beta=0, nbAnts=30, maxCycles=500
test_evaporation_rate() {
    echo "Testing influence of evaporation rate (rho) - Figure 2..."
    ALPHA=2
    BETA=0
    RHOS=(0.01 0.02 0.03)
    NB_ANTS=30
    MAX_CYCLES=50

    SUMMARY_CSV="${RESULTS_DIR}/rho_influence_summary.csv"
    PROGRESS_CSV="${RESULTS_DIR}/rho_influence_progress.csv"

    # Create CSV headers
    echo "seed,problem,total_vars,alpha,beta,rho,tau_min,tau_max,num_ants,best_cycle,final_assigned,success,total_cycles,runtime_ms" > "${SUMMARY_CSV}"
    echo "seed,problem,alpha,beta,rho,num_ants,cycle,assigned_variables,total_variables,completion_rate" > "${PROGRESS_CSV}"

    for instance in "${INSTANCES[@]}"; do
        for seed in "${SEEDS[@]}"; do
            for rho in "${RHOS[@]}"; do
                echo "Running ${instance} seed=${seed} rho=${rho}..."
                java -jar target/antares-1.0-SNAPSHOT.jar \
                    "${instance}" "${seed}" "${ALPHA}" "${BETA}" "${rho}" "${NB_ANTS}" "${MAX_CYCLES}" \
                    >> "${PROGRESS_CSV}" 2>> "${SUMMARY_CSV}"
            done
        done
    done

    echo "Results saved to:"
    echo "  Summary:  ${SUMMARY_CSV}"
    echo "  Progress: ${PROGRESS_CSV}"
}

# Main
case "${1:-all}" in
    alpha)
        test_pheromone_importance
        ;;
    rho)
        test_evaporation_rate
        ;;
    all)
        test_pheromone_importance
        test_evaporation_rate
        ;;
    *)
        echo "Usage: $0 [alpha|rho|all]"
        echo "  alpha - Test influence of pheromone importance (Figure 1)"
        echo "  rho   - Test influence of evaporation rate (Figure 2)"
        echo "  all   - Run both experiments from paper (default)"
        exit 1
        ;;
esac

echo "Done!"
