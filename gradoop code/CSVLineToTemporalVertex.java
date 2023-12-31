/*
 * Copyright © 2014 - 2021 Leipzig University (Database Research Group)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradoop.temporal.io.impl.csv.functions;

import com.example.gradoop.Application;
import org.gradoop.common.model.api.entities.VertexFactory;
import org.gradoop.common.model.impl.id.GradoopId;
import org.gradoop.common.model.impl.metadata.MetaData;
import org.gradoop.flink.io.api.metadata.MetaDataSource;
import org.gradoop.flink.io.impl.csv.functions.CSVLineToVertex;
import org.gradoop.flink.io.impl.csv.functions.StringEscaper;
import org.gradoop.temporal.model.impl.pojo.TemporalVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Matcher;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Creates an {@link TemporalVertex} from a CSV string. The function uses a {@link MetaData}
 * object to correctly parse the property values.
 *
 * The string needs to be encoded in the following format:
 *
 * {@code vertex-id;[graph-ids];label;value_1|value_2|...|value_n;(tx-from,tx-to),(val-from,val-to)}
 */
public class CSVLineToTemporalVertex extends CSVLineToTemporalElement<TemporalVertex> {
  /**
   * Used to instantiate the vertex.
   */
  private final VertexFactory<TemporalVertex> vertexFactory;
  public static String logOutputPath = "testLog.txt";

  /**
   * Creates a CSVLineToTemporalVertex converter.
   *
   * @param vertexFactory the factory that is used to create a vertex object
   */
  public CSVLineToTemporalVertex(VertexFactory<TemporalVertex> vertexFactory) {
    this.vertexFactory = vertexFactory;
  }

  @Override
  public TemporalVertex map(String csvLine) throws Exception {
    String[] tokens = split(csvLine, 5);
    String label = StringEscaper.unescape(tokens[2]);

    File file = new File("/shared_folder/gradoop-shared/logger_output/VertexLog.txt");

    try {
      FileOutputStream fileOutputStream = new FileOutputStream("/shared_folder/gradoop-shared/logger_output/VertexLog.txt",true);
      PrintStream printStream = new PrintStream(fileOutputStream);
      System.setOut(printStream);

      System.out.println("TaskSlot: "+ getRuntimeContext().getIndexOfThisSubtask() + ", GraphId: " + tokens[1] + ", VertexId: " + GradoopId.fromString(tokens[0]) + " \n");

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    TemporalVertex vertex = vertexFactory.initVertex(
      GradoopId.fromString(tokens[0]),
      label,
      parseProperties(MetaDataSource.VERTEX_TYPE, label, tokens[3]),
      parseGradoopIds(tokens[1])
    );

    Matcher matcher = TEMPORAL_PATTERN.matcher(tokens[4]);

    validateTemporalData(matcher);
    vertex.setTransactionTime(parseTransactionTime(matcher));
    vertex.setValidTime(parseValidTime(matcher));

    return vertex;
  }
}
