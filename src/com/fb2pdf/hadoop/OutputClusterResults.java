package com.fb2pdf.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopy;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class OutputClusterResults extends Configured implements Tool {

	public static class OutputClusterResultsMapper extends MapReduceBase
			implements
			Mapper<Text, MeanShiftCanopy, Text, Text> {
		
		CosineDistanceMeasure measure;
		List<Path> vectorPaths = new ArrayList<Path>();
		FileSystem fs;
		Configuration conf;

		@Override
		public void map(Text key, MeanShiftCanopy canopy,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			Vector v1 = canopy.getCenter();
			for(Path vectorPath : vectorPaths){
				SequenceFile.Reader reader = new SequenceFile.Reader(fs, vectorPath, conf);
				VectorWritable v2 = new VectorWritable();
				Text vkey = new Text();
				while(reader.next(vkey, v2)){
					if(v2 != null){
						if(measure.distance(v1, v2.get()) < 0.02){
							output.collect(key, new Text(v2.get().getName()));
						}
					}
				}
			}
			
		}

		@Override
		public void configure(JobConf job) {
			conf = job;
			try{
				fs = FileSystem.get(job);
				int i = 0;
				String vectorPath;
				while((vectorPath = job.get("cluster.results.vector.files" + i)) != null){
					vectorPaths.add(new Path(vectorPath));
					i++;
				}
				measure = new CosineDistanceMeasure();
			}
			catch(IOException e){
				//TODO
			}
		}
	}

	@Override
	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), OutputClusterResults.class);
		conf.setJobName(OutputClusterResults.class.getName());

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		Path outPath = new Path(args[2]);
		FileOutputFormat.setOutputPath(conf, outPath);

		FileSystem fs = FileSystem.get(conf);
		Path[] vectorFiles = FileUtil.stat2Paths(fs
				.listStatus(new Path(args[1])));
		for (int i = 0; i < vectorFiles.length; i++) {
			conf.set("cluster.results.vector.files" + i, vectorFiles[i]
					.toString());
		}
		conf.setMapperClass(OutputClusterResultsMapper.class);
		conf.setInputFormat(SequenceFileInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		conf.setNumReduceTasks(1);

		JobClient.runJob(conf);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.err
					.println("Usage OutputClusterResults <clusters> <vectors> <out>");
			System.exit(1);
		} else {
			Configuration conf = new Configuration();
			ToolRunner.run(conf, new OutputClusterResults(), args);
		}
	}

}
