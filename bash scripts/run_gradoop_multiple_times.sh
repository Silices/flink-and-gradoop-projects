#!/bin/bash

FLINK_BIN_PATH="./bin/flink"
FLINK_JOB_JAR="/shared_folder/gradoop-shared/gradoop-tpgm-app-1.0.jar"
FLINK_JOB_APP="com.example.gradoop.Application"
INPUT_DIR="/shared_folder/gradoop-shared/csv_tpgm_graphs/"
OUTPUT_DIR="/shared_folder/gradoop-shared/csv_tpgm_graphs_output"
VERT_LOG="VertexLog.txt"
GRAPH_LOG="GraphLog.txt"
EDGE_LOG="EdgeLog.txt"
LOGGER_DIR="/shared_folder/gradoop-shared/logger_output/"
LOG_DIR="/shared_folder/gradoop-shared/logs/"
declare -i PARALLELISM=8

echo "Running"

for SUB_DIR in $INPUT_DIR/*/; do
  if [ -d "$SUB_DIR" ]; then

        # Extract the subdirectory name
        SUBDIR_NAME=$(basename "$SUB_DIR")

        echo "Running Gradoop job for $SUBDIR_NAME"
        output=$($FLINK_BIN_PATH run -p $PARALLELISM -c $FLINK_JOB_APP $FLINK_JOB_JAR $SUB_DIR $OUTPUT_DIR $PARALLELISM)  
        
        # Clear the output directory for the next run
        rm -r "$OUTPUT_DIR"/*
        
        mv $LOGGER_DIR$VERT_LOG $LOG_DIR"/"$PARALLELISM"/"$SUBDIR_NAME"_"$VERT_LOG 
        mv $LOGGER_DIR$GRAPH_LOG $LOG_DIR"/"$PARALLELISM"/"$SUBDIR_NAME"_"$GRAPH_LOG 
        mv $LOGGER_DIR$EDGE_LOG $LOG_DIR"/"$PARALLELISM"/"$SUBDIR_NAME"_"$EDGE_LOG 
  fi
done

echo "Stopping"