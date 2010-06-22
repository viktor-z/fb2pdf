package com.fb2pdf.hadoop.cluster;

import java.io.IOException;
import java.util.UUID;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MeanShiftCanopyDriver {
  
  private static final Logger LOG = LoggerFactory.getLogger(MeanShiftCanopyDriver.class);
  
  private MeanShiftCanopyDriver() {}
  
  public static void main(String[] args) {
    
	  if(args.length != 3){
		  System.err
			.println("Usage MeanShiftCanopyDriver <t2> <input> <out>");
	  }
      String input = args[1];
      String output = args[2];
      String measureClassName = CosineDistanceMeasure.class.getName();
      double t2 = 0.02;
      try{
    	  t2 = Double.parseDouble(args[0]);
      }
      catch(NumberFormatException e){
    	  LOG.error(e.toString());
      }
      if(t2 > 0.5) t2 = 0.5;
      double t1 = t2 * 2;
      double convergenceDelta = 0.001;
      Path canopyOut = createCanopyFromVectors(input);
      runJob(canopyOut, output, measureClassName, t1, t2, convergenceDelta);
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
  public static void runJob(Path input,
                            String output,
                            String measureClassName,
                            double t1,
                            double t2,
                            double convergenceDelta) {
    
    Configurable client = new JobClient();
    JobConf conf = new JobConf(MeanShiftCanopyDriver.class);
    
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(MeanShiftCanopy.class);
    conf.setJobName("MeanShiftCanopyCluster");
    
    FileInputFormat.setInputPaths(conf, input);
    Path outPath = new Path(output);
    Path controlPath = new Path(input.getParent(), UUID.randomUUID().toString());
    FileOutputFormat.setOutputPath(conf, outPath);
    conf.setMapperClass(MeanShiftCanopyMapper.class);
    conf.setReducerClass(MeanShiftCanopyReducer.class);
    conf.setNumReduceTasks(1);
    conf.setInputFormat(SequenceFileInputFormat.class);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    conf.setBoolean("mapred.output.compress", true);
    conf.setClass("mapred.output.compression.codec", GzipCodec.class,  CompressionCodec.class);
    conf.set(MeanShiftCanopyConfigKeys.DISTANCE_MEASURE_KEY, measureClassName);
    conf.set(MeanShiftCanopyConfigKeys.CLUSTER_CONVERGENCE_KEY, String.valueOf(convergenceDelta));
    conf.set(MeanShiftCanopyConfigKeys.T1_KEY, String.valueOf(t1));
    conf.set(MeanShiftCanopyConfigKeys.T2_KEY, String.valueOf(t2));
    conf.set(MeanShiftCanopyConfigKeys.CONTROL_PATH_KEY, controlPath.toString());
    client.setConf(conf);
    try {
      JobClient.runJob(conf);
      input.getFileSystem(conf).delete(input.getParent(), true);
    } catch (IOException e) {
    	LOG.warn(e.toString(), e);
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
  public static Path createCanopyFromVectors(String input) {
    
    Configurable client = new JobClient();
    JobConf conf = new JobConf(MeanShiftCanopyDriver.class);
    conf.setJobName("CreateCanopyFromVectors");
    
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(MeanShiftCanopy.class);
    
    FileInputFormat.setInputPaths(conf, new Path(input));
    Path tmpDir = new Path(new Path(conf.get("hadoop.tmp.dir")), UUID.randomUUID().toString());
    Path outPath = new Path(tmpDir, UUID.randomUUID().toString());
    FileOutputFormat.setOutputPath(conf, outPath);
    
    conf.setMapperClass(MeanShiftCanopyCreatorMapper.class);
    conf.setNumReduceTasks(0);
    conf.setInputFormat(SequenceFileInputFormat.class);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    
    client.setConf(conf);
    try {
      JobClient.runJob(conf);
      return outPath;
    } catch (IOException e) {
      LOG.warn(e.toString(), e);
    }
    return null;
  }
}

