package com.fb2pdf.hadoop;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueTextInputFormat;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.StringTuple;


public class CSV4DictionaryVectorizerTransformer extends Configured implements Tool{
	
//	private static final Log LOG = LogFactory.getLog(CSV4DictionaryVectorizerTransformer.class);
	
	static class UnfoldTFMapper extends MapReduceBase implements Mapper<Text, Text, Text, StringTuple>
    {
		
		private Text fileName;
		
        public UnfoldTFMapper()
        {
            super();
        }
        
        @Override
        public void configure(JobConf conf){
        	fileName = new Text(KeyValueTextInputFormat.getInputPaths(conf)[0].getName());
        }

        @Override
        public void map(Text key, Text value, OutputCollector<Text, StringTuple> output, Reporter reporter)
                throws IOException{
        	if(!"".equals(value.toString())){
	        	long wordFrequency = Long.parseLong(value.toString());
	        	StringTuple document = new StringTuple();
	        	for(int i = 0; i < wordFrequency; i++){
	        		document.add(key.toString());
	        	}
	        	output.collect(fileName, document);
        	}
        }
    };
    
	@Override
	public int run(String[] args) throws Exception {
        JobConf conf = new JobConf(getConf(), CSV4DictionaryVectorizerTransformer.class);
        conf.setJobName(CSV4DictionaryVectorizerTransformer.class.getName());

        conf.setInputFormat(KeyValueTextInputFormat.class);
        
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(StringTuple.class);

        conf.setMapperClass(UnfoldTFMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        conf.setNumReduceTasks(1);

        Path outpath = new Path(args[1]);
        FileInputFormat.addInputPaths(conf, args[0]);
        FileOutputFormat.setOutputPath(conf, outpath);

        JobClient.runJob(conf);

        return 0;
	}
	
	/**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        if(args.length != 2)
        {
            System.err.println("Usage CSV4DictionaryVectorizerTransformer <src> <dst>");
        } else{
        	Configuration conf = new Configuration();
        	ToolRunner.run(conf, new CSV4DictionaryVectorizerTransformer(), args);
        	FileSystem fs = FileSystem.get(conf);
        	String filename = UUID.randomUUID().toString();
        	fs.rename(new Path(args[1] + "/" + "part-00000"), new Path(conf.get("hadoop.tmp.dir", "/tmp") + filename));
        	fs.delete(new Path(args[1]), true);
        	fs.rename(new Path(conf.get("hadoop.tmp.dir", "/tmp") + filename), new Path(args[1]));
        }
    }

}
