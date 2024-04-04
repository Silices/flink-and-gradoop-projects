#!/bin/bash

#/shared_folder/run_gradoop_multiple_times.sh

FLINK_BIN_PATH="./bin/flink"
FLINK_JOB_JAR="/shared_folder/gradoop-shared/gradoop-tpgm-app-1.0.jar"
FLINK_JOB_APP="com.example.gradoop.TemporalFilter"
INPUT_DIR="/shared_folder/gradoop-shared/csv_tpgm_graphs/"
OUTPUT_DIR="/shared_folder/gradoop-shared/csv_tpgm_graphs_output_sh"

OUTPUT_FILE="/shared_folder/gradoop-shared/execution_log.csv"
RUNTIME_OUTPUT_FILE="/shared_folder/gradoop-shared/runtimes_per_interval.csv"

#cp -r "$INPUT_DIR" "$OUTPUT_DIR" &

#wait

declare -i PARALLELISM=1
#declare -i INTERVAL=11
declare -i VERTEXNUM=128
declare -i NUM_RUNS=10 # 10
INTERVALS=(64 32 16 8 4 2 1) # 64 32 16 8 4 2 1

echo "Running"

for INTERVAL in "${INTERVALS[@]}"; do

  echo "Running INTERVAL jobs: $INTERVAL"
  for SUB_DIR in $INPUT_DIR/*/; do # für jede graphen-Kollektion
    if [ -d "$SUB_DIR" ]; then

      declare -i i=1
      declare -i total_time=0
      declare -i mes_total_time=0
      #declare -i squared_total_time=0
      run_times=()
      mes_run_times=()

      while [ $i -le $NUM_RUNS ]; do

        mes_start_time=$(date +%s%3N)
        
        output=$($FLINK_BIN_PATH run -p $PARALLELISM -c $FLINK_JOB_APP $FLINK_JOB_JAR $SUB_DIR $OUTPUT_DIR $PARALLELISM $INTERVAL $VERTEXNUM)  
        
        mes_end_time=$(date +%s%3N)
        mes_elapsed_time=$((mes_end_time - mes_start_time))
        mes_run_times+=("$mes_elapsed_time")
        mes_total_time=$((mes_total_time + mes_elapsed_time))
        
        job_runtime=$(echo "$output" | grep -oP "\K\d+(?= ms)")
        
        runtime_outputs=$job_runtime 
        echo "$PARALLELISM;$INTERVAL;$runtime_outputs" | tr '\n' ';' | tee -a $RUNTIME_OUTPUT_FILE
        echo "" | tee -a $RUNTIME_OUTPUT_FILE

        #/shared_folder/run_gradoop_multiple_times.sh

        run_time=0
        while read -r job_runtime; do
          run_time=$((run_time + job_runtime))
        done < <(echo "$output" | grep -oP "\K\d+(?= ms)")

        run_times+=("$run_time")
        

        #echo "output: $output" | tee -a $OUTPUT_FILE
      #   sum=0
      
      #  # Loop over each value in job_runtimes
      #   for runtime in $job_runtimes; do
      #     sum=$((sum + runtime))
      #   done

        total_time=$((total_time + run_time))
        #squared_total_time=$((squared_total_time + run_time*run_time))
        #echo "run_time: $run_time"     
        i=$((i + 1))
      done

      mes_average_time=$((mes_total_time / NUM_RUNS))

      for time in "${mes_run_times[@]}"; do
        mes_diff=$((time - mes_average_time))
        mes_squared_diff=$((mes_diff * mes_diff))
        mes_variance=$((mes_variance + mes_squared_diff))
      done
      mes_variance=$((mes_variance / NUM_RUNS))
      mes_standard_deviation=$(awk -v var="$mes_variance" 'BEGIN {print sqrt(var)}')

      #________________________________________________________________________________________________
      average_time=$((total_time / NUM_RUNS))

      for time in "${run_times[@]}"; do
        diff=$((time - average_time))
        squared_diff=$((diff * diff))
        variance=$((variance + squared_diff))
      done
      variance=$((variance / NUM_RUNS))

      #variance=$((squared_total_time / NUM_RUNS - (average_time)**2))
      standard_deviation=$(awk -v var="$variance" 'BEGIN {print sqrt(var)}')

      num_graphs=$(basename "$SUB_DIR")
      echo "$PARALLELISM;$INTERVAL;$num_graphs;$mes_average_time;$mes_standard_deviation;$average_time;$standard_deviation;" | tee -a $OUTPUT_FILE

      # echo "Average job execution time for $INPUT_FILE: $average_time ms" | tee -a $OUTPUT_FILE
      # echo "variance for $INPUT_FILE: $variance ms" | tee -a $OUTPUT_FILE
      # echo "Standard deviation of job execution time for $INPUT_FILE: $standard_deviation ms" | tee -a $OUTPUT_FILE
    fi
  done
done
echo "Stopping"

#/shared_folder/run_gradoop_multiple_times.sh