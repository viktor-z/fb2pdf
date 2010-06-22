package com.fb2pdf.hadoop.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopy;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputClusterResults extends Configured implements Tool {
	
	private static final Logger LOG = LoggerFactory.getLogger(OutputClusterResults.class);

	public static class OutputClusterResultsMapper extends MapReduceBase
			implements
			Mapper<Text, MeanShiftCanopy, Text, Text> {
		
		CosineDistanceMeasure measure;
		List<Path> vectorPaths = new ArrayList<Path>();
		Configuration conf;
		float t;

		@Override
		public void map(Text key, MeanShiftCanopy canopy,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			Vector v1 = canopy.getCenter();
			for(Path vectorPath : vectorPaths){
				if(vectorPath.getName().startsWith("part-")){
					conf.setClass("mapred.output.compression.codec", GzipCodec.class,
							CompressionCodec.class);
					FileSystem fs = vectorPath.getFileSystem(conf);
					SequenceFile.Reader reader = new SequenceFile.Reader( fs, vectorPath, conf);
					VectorWritable v2 = new VectorWritable();
					Text vkey = new Text();
					while(reader.next(vkey, v2)){
						if(v2 != null){
							if(measure.distance(v1, v2.get()) < t){
								output.collect(key, new Text(v2.get().getName()));
							}
						}
					}
				}
			}
			
		}

		@Override
		public void configure(JobConf job) {
			conf = job;
			int i = 0;
			String vectorPath;
			while((vectorPath = job.get("cluster.results.vector.files" + i)) != null){
				vectorPaths.add(new Path(vectorPath));
				i++;
			}
			measure = new CosineDistanceMeasure();
			t = job.getFloat("cluster.t2.distance", 0.02F);
		}
	}
	
	public static class OutputClusterResultsReducer extends MapReduceBase
	implements
	Reducer<Text, Text, Text, Text> {

		@Override
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter arg3)
				throws IOException {
			List<String> strings = new ArrayList<String>();
			while(values.hasNext()){
				strings.add(values.next().toString());
			}
			if(strings.size() > 1){
				output.collect(key, new Text(StringUtils.join(strings, ", ")));
			}
		}
		
	}

	@Override
	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), OutputClusterResults.class);
		conf.setJobName(OutputClusterResults.class.getName());

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(args[1]));
		Path tmpDir = new Path(new Path(conf.get("hadoop.tmp.dir")), UUID.randomUUID().toString());
		Path outPath = new Path(tmpDir, UUID.randomUUID().toString());
		FileOutputFormat.setOutputPath(conf, outPath);

		Path vectorParent = new Path(args[2]);
		FileSystem fs = vectorParent.getFileSystem(conf);
		Path[] vectorFiles = FileUtil.stat2Paths(fs.listStatus(vectorParent));
		for (int i = 0; i < vectorFiles.length; i++) {
			conf.set("cluster.results.vector.files" + i, vectorFiles[i]
					.toString());
		}
		conf.setMapperClass(OutputClusterResultsMapper.class);
		conf.setReducerClass(OutputClusterResultsReducer.class);
		conf.setInputFormat(SequenceFileInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);
		conf.setNumReduceTasks(1);
		conf.set("cluster.t2.distance", args[0]);

		JobClient.runJob(conf);
		FileSystem tmpFs = outPath.getFileSystem(conf);
		Path resultedFile = new Path(args[3]);
		FileSystem OutputFs = resultedFile.getFileSystem(conf);
		FileUtil.copy(tmpFs, new Path(outPath, "part-00000"), OutputFs, resultedFile, true, conf);
		tmpFs.delete(outPath, true);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err
					.println("Usage OutputClusterResults <t2> <clusters> <vectors> <out_file>");
			System.exit(1);
		} else {
			Configuration conf = new Configuration();
			ToolRunner.run(conf, new OutputClusterResults(), args);
		}
	}

}
