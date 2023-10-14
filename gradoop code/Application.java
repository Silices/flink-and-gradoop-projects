package com.example.gradoop;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.gradoop.common.model.api.entities.Vertex;
import org.gradoop.flink.io.impl.csv.CSVDataSource;
import org.gradoop.flink.io.impl.dot.DOTDataSink;
import org.gradoop.flink.model.api.functions.KeyFunction;
import org.gradoop.flink.model.impl.epgm.LogicalGraph;
import org.gradoop.flink.util.GradoopFlinkConfig;
import org.gradoop.flink.io.impl.csv.CSVDataSource;
import org.gradoop.temporal.io.api.TemporalDataSource;
import org.gradoop.temporal.io.impl.csv.TemporalCSVDataSink;
import org.gradoop.temporal.io.impl.csv.TemporalCSVDataSource;
import org.gradoop.temporal.io.impl.csv.functions.CSVLineToTemporalEdge;
import org.gradoop.temporal.io.impl.csv.functions.CSVLineToTemporalGraphHead;
import org.gradoop.temporal.io.impl.csv.functions.CSVLineToTemporalVertex;
import org.gradoop.temporal.model.impl.TemporalGraph;
import org.gradoop.temporal.model.impl.TemporalGraphCollection;
import org.gradoop.temporal.model.impl.TemporalGraphCollectionFactory;
import org.gradoop.temporal.util.TemporalGradoopConfig;

import java.nio.file.Path;
import java.nio.file.Paths;


public class Application {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        String inputPath;
        String outputPath;
        int numOfTasks;

        if (args.length >= 3) {
            inputPath = args[0];
            outputPath = args[1];
            numOfTasks = Integer.parseInt(args[2]);
        } else {
            inputPath = "/shared_folder/gradoop-shared/ldbc_sample";
            outputPath = "/shared_folder/gradoop-shared/output.dot";
            numOfTasks = 1;
        }

        env.setParallelism(numOfTasks);

        TemporalGradoopConfig temporalGradoopConfig = TemporalGradoopConfig.createConfig(env);

        TemporalCSVDataSource dataSource = new TemporalCSVDataSource(inputPath, temporalGradoopConfig);

        TemporalGraph temporalGraph = dataSource.getTemporalGraph();
        temporalGraph.writeTo(new TemporalCSVDataSink(outputPath, temporalGradoopConfig));

        // finally execute
        env.execute("Gradoop TPGM App");
    }
}
