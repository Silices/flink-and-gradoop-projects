#!/bin/bash

declare -i NUM_RUNS=20
FLINK_BIN_PATH="./bin/flink"
FLINK_JOB_JAR="/shared_folder/wordCount-0.1_datastream_process_custom_keyby_no_rebalance.jar"
INPUT_DIR="/shared_folder/input_files"
OUTPUT_DIR="/shared_folder/output_files"
declare -i PARALLELISM=2
OUTPUT_FILE="/shared_folder/average_times.txt"

# Clear the output file if it already exists
truncate -s 0 $OUTPUT_FILE

for INPUT_FILE in $INPUT_DIR/*.txt; do
  [ -e "$INPUT_FILE" ] || continue

  # Get the input file's basename without the extension
  input_basename="$(basename "$INPUT_FILE" .txt)"
  OUTPUT_FILE_PREFIX="${OUTPUT_DIR}/${input_basename}/"

  declare -i total_time=0
  declare -i i=1
  while [ $i -le $NUM_RUNS ]; do
    output=$($FLINK_BIN_PATH run -p $PARALLELISM $FLINK_JOB_JAR $INPUT_FILE "${OUTPUT_FILE_PREFIX}_${i}.txt" $PARALLELISM)
    job_runtime=$(echo "$output" | grep -oP "Job Runtime: \K\d+")
    total_time=$((total_time + job_runtime))
   
    i=$((i + 1))
  done

  average_time=$((total_time / NUM_RUNS))
  echo "Average job execution time for $INPUT_FILE: $average_time ms" | tee -a $OUTPUT_FILE
done
