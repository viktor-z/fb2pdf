package com.fb2pdf.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopy;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopyConfigKeys;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopyCreatorMapper;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopyMapper;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopyReducer;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.common.distance.ManhattanDistanceMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MeanShiftCanopyDriver {
  
  private static final Logger log = LoggerFactory.getLogger(MeanShiftCanopyDriver.class);
  
  private MeanShiftCanopyDriver() {}
  
  public static void main(String[] args) {
    
      String input = args[0];
      String output = args[1];
      String measureClassName = CosineDistanceMeasure.class.getName();
      double t1 = 0.08;
      double t2 = 0.02;
      double convergenceDelta = 0.001;
      String canopyOut = "/tmp/" + UUID.randomUUID().toString();
      createCanopyFromVectors(input, canopyOut);
      runJob(canopyOut, output, canopyOut + File.separator + MeanShiftCanopyConfigKeys.CONTROL_PATH_KEY,
        measureClassName, t1, t2, convergenceDelta);
  }
  
  /**
   * Run the job
   * 
   * @param input
   *          the input pathname String
   * @param output
   *          the output pathname String
   * @param control
   *          the control path
   * @param measureClassName
   *          the DistanceMeasure class name
   * @param t1
   *          the T1 distance threshold
   * @param t2
   *          the T2 distance threshold
   * @param convergenceDelta
   *          the double convergence criteria
   */
  public static void runJob(String input,
                            String output,
                            String control,
                            String measureClassName,
                            double t1,
                            double t2,
                            double convergenceDelta) {
    
    Configurable client = new JobClient();
    JobConf conf = new JobConf(MeanShiftCanopyDriver.class);
    
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(MeanShiftCanopy.class);
    
    FileInputFormat.setInputPaths(conf, new Path(input));
    Path outPath = new Path(output);
    FileOutputFormat.setOutputPath(conf, outPath);
    
    conf.setMapperClass(MeanShiftCanopyMapper.class);
    conf.setReducerClass(MeanShiftCanopyReducer.class);
    conf.setNumReduceTasks(1);
    conf.setInputFormat(SequenceFileInputFormat.class);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    conf.set(MeanShiftCanopyConfigKeys.DISTANCE_MEASURE_KEY, measureClassName);
    conf.set(MeanShiftCanopyConfigKeys.CLUSTER_CONVERGENCE_KEY, String.valueOf(convergenceDelta));
    conf.set(MeanShiftCanopyConfigKeys.T1_KEY, String.valueOf(t1));
    conf.set(MeanShiftCanopyConfigKeys.T2_KEY, String.valueOf(t2));
    conf.set(MeanShiftCanopyConfigKeys.CONTROL_PATH_KEY, control);
    
    client.setConf(conf);
    try {
      JobClient.runJob(conf);
    } catch (IOException e) {
      log.warn(e.toString(), e);
    }
  }
  
  /**
   * Run the job
   * 
   * @param input
   *          the input pathname String
   * @param output
   *          the output pathname String
   */
  public static void createCanopyFromVectors(String input, String output) {
    
    Configurable client = new JobClient();
    JobConf conf = new JobConf(MeanShiftCanopyDriver.class);
    
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(MeanShiftCanopy.class);
    
    FileInputFormat.setInputPaths(conf, new Path(input));
    Path outPath = new Path(output);
    FileOutputFormat.setOutputPath(conf, outPath);
    
    conf.setMapperClass(MeanShiftCanopyCreatorMapper.class);
    conf.setNumReduceTasks(0);
    conf.setInputFormat(SequenceFileInputFormat.class);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    
    client.setConf(conf);
    try {
      JobClient.runJob(conf);
    } catch (IOException e) {
      log.warn(e.toString(), e);
    }
  }
}

