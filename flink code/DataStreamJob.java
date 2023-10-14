package org.myorg.wordCount;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.util.Collector;
import java.util.StringTokenizer;

public class DataStreamJob {

	public static void main(String[] args) throws Exception {

		//ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		//env.setRuntimeMode(RuntimeExecutionMode.BATCH);

		String inputPath;
		String outputPath;
		int numOfTasks;

		if (args.length >= 3) {
			inputPath = args[0];
			outputPath = args[1];
			numOfTasks = Integer.parseInt(args[2]);
		} else {
			inputPath = "/shared_folder/book_double.txt";
			outputPath = "/shared_folder/output_double_A.txt";
			numOfTasks = 1;
		}

       // System.out.print(numOfTasks);
        env.setParallelism(numOfTasks);

		DataStream<String> input = env.readTextFile(inputPath); //.rebalance();
		//DataSet<String> input = env.readTextFile(inputPath);


		/*DataSet<Tuple2<String, Integer>> wordCounts = input
				.flatMap(new Tokenizer())
				.groupBy(0)
				.sum(1).setParallelism(numOfTasks);*/

		DataStream<Tuple2<String, Integer>> wordCounts = input
				.flatMap(new Tokenizer())
				.keyBy(value -> String.valueOf(value.f0.length()))
				.process(new LogKeyByDataTaskSlotDistribution()).setParallelism(numOfTasks);

		DataStream<Tuple2<String, Integer>> summedStream = wordCounts
				.keyBy(tuple -> tuple.f0)
				.sum(1);

		//wordCounts.addSink(new PrintSinkFunction<>());
        //wordCounts.print();
		//wordCounts.writeAsText(outputPath, FileSystem.WriteMode.OVERWRITE);
		//wordCounts.writeAsCsv(outputPath, FileSystem.WriteMode.OVERWRITE).setParallelism(1);
		//wordCounts.writeAsText("/mnt/s/apache-flink/quickstart/src/textFiles/output.txt", FileSystem.WriteMode.OVERWRITE);

		env.execute("WordCount Batch Job");
	}

	public static final class Tokenizer extends RichFlatMapFunction<String, Tuple2<String, Integer>> {
		@Override
		public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
            StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");

            while (tokenizer.hasMoreTokens()) {
				String word = tokenizer.nextToken();
				if (word.length() > 0) {
					System.out.println("Tokenizer Key: " + word + ", Task Slot: " + getRuntimeContext().getIndexOfThisSubtask());
					out.collect(new Tuple2<>(word, 1));
				}
			}
		}
	}
	public static class LogKeyByDataTaskSlotDistribution extends ProcessFunction<Tuple2<String, Integer>, Tuple2<String, Integer>> {
		@Override
		public void processElement(Tuple2<String, Integer> value, Context ctx, Collector<Tuple2<String, Integer>> out) {
			System.out.println("LogKeyByDataTaskSlotDistribution Key: " + value.f0 + ", Task Slot: " + getRuntimeContext().getIndexOfThisSubtask());
			out.collect(value);
		}
	}
}
