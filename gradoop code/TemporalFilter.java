package com.example.gradoop;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.GroupReduceOperator;
import org.gradoop.common.model.api.entities.Element;
import org.gradoop.flink.model.api.functions.TransformationFunction;
import org.gradoop.flink.util.GradoopFlinkConfig;
import org.gradoop.temporal.io.impl.csv.TemporalCSVDataSink;
import org.gradoop.temporal.io.impl.csv.TemporalCSVDataSource;
import org.gradoop.temporal.model.impl.TemporalGraph;
import org.gradoop.temporal.model.impl.functions.predicates.ContainedIn;
import org.gradoop.temporal.model.impl.functions.predicates.Overlaps;
import org.gradoop.temporal.model.impl.pojo.TemporalVertex;
import org.gradoop.temporal.util.TemporalGradoopConfig;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TemporalFilter {
    public static void main(String[] args) throws Exception {

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        String inputPath;
        String outputPath;
        int numOfTasks;
        int intervalLength;
        long numVertices;

        if (args.length < 4) {
            throw new IOException("Not enough input parameters!");
        }

        inputPath = args[0];
        outputPath = args[1];
        numOfTasks = Integer.parseInt(args[2]);
        intervalLength = Integer.parseInt(args[3]);
        numVertices = Integer.parseInt(args[4]);

        env.setParallelism(numOfTasks);

        TemporalGradoopConfig temporalGradoopConfig = TemporalGradoopConfig.createConfig(env);

        TemporalCSVDataSource dataSource = new TemporalCSVDataSource(inputPath, temporalGradoopConfig);

        TemporalGraph temporalGraph = dataSource.getTemporalGraph();

        List<TemporalVertex> selectedVertices = new ArrayList<>();

        // Filter temporalGraph by time intervals and select one vertex from each subgraph
        for (long i = 0; i < numVertices; i += intervalLength) {
//            TemporalGraph snapshot = temporalGraph.snapshot(new ContainedIn(i, i + intervalLength - 1));
//
//            if(snapshot.getVertices().count() > 0) {
//                TemporalVertex selectedVertex = snapshot.getVertices().collect().get(0);
//
//                selectedVertices.add(selectedVertex);
//            }
            Optional.ofNullable(temporalGraph.snapshot(new ContainedIn(i, i + intervalLength - 1))
                            .getVertices().collect().get(0))
                    .ifPresent(selectedVertices::add);

//            snapshot.writeTo(new TemporalCSVDataSink(outputPath + "_" + i, temporalGradoopConfig), true);
//            env.execute("Gradoop TPGM - Loop Iteration");
        }

        // Create a new graph with selected vertices
        TemporalGraph newTemporalGraph = temporalGraph.vertexInducedSubgraph(new CustomFilterFunction(selectedVertices));

        // Write the new graph to output
        env.setParallelism(1);
        newTemporalGraph.writeTo(new TemporalCSVDataSink(outputPath + "_subgraph", temporalGradoopConfig), true);

        // finally execute
        env.execute("Gradoop TPGM - Filter Application");
    }

    private static class CustomFilterFunction implements FilterFunction<TemporalVertex> {
        List<TemporalVertex> selectedVertices;

        public CustomFilterFunction(List<TemporalVertex> selectedVertices) {
            this.selectedVertices = selectedVertices;
        }

        @Override
        public boolean filter(TemporalVertex vertex) throws Exception {

            return selectedVertices.contains(vertex);
        }
    }
}
