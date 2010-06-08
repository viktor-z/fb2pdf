package com.fb2pdf.hadoop.cluster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.LongSumReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fb2pdf.hadoop.TextTokenizer;
import com.fb2pdf.hadoop.XMLTextInputFormat;

public class FB2KeywordsExtractor extends Configured implements Tool
{
    private static final Log          logger = LogFactory.getLog("com.fb2pdf.hadoop.FB2KeywordsExtractor");
    private final static LongWritable one    = new LongWritable(1);

    static Set<String>                excluded;
    static
    {
        excluded = new HashSet<String>();
        excluded.add("FictionBook/binary");
        
        try {
        	new URL("hdfs://localhost:9000/");
        } catch (MalformedURLException e) {
        	URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
		}        
    }

    static class ExtractKeywordsMapper extends MapReduceBase implements Mapper<Text, Text, Text, LongWritable>
    {
        public ExtractKeywordsMapper()
        {
            super();
        }

        private Text word = new Text();

        @Override
        public void map(Text key, Text value, OutputCollector<Text, LongWritable> output, Reporter reporter)
                throws IOException
        {
            if(excluded.contains(key.toString()))
                return;
            
            String line = value.toString().trim();
            TextTokenizer st = new TextTokenizer(line);
            while(st.hasMoreTokens())
            {
                String k = st.nextToken().trim().toLowerCase();
                if(k.length()>1)
                {
                    word.set(k);
                    output.collect(word, one);
                }
            }
        }
    }

    @Override
    public int run(String[] args) throws Exception
    {
        JobConf conf = new JobConf(getConf(), FB2KeywordsExtractor.class);
        conf.setJobName("FB2KeywordsExtractor");

        conf.setInputFormat(XMLTextInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapperClass(ExtractKeywordsMapper.class);
        conf.setCombinerClass(LongSumReducer.class);
        conf.setReducerClass(LongSumReducer.class);
        conf.setNumReduceTasks(1);
        conf.setBoolean("mapred.output.compress", true);
        conf.setClass("mapred.output.compression.codec", GzipCodec.class,  CompressionCodec.class);
        
        Path inpath = new Path(args[0]);
        Path outpath = new Path(args[1]);
        FileInputFormat.addInputPath(conf, inpath);
        FileOutputFormat.setOutputPath(conf, outpath);

        logger.info("Extracting keywords from " + inpath + " to " + outpath);

        JobClient.runJob(conf);

        return 0;
    }

    public static void main(String[] args) throws Exception
    {    	
        if(args.length != 2)
        {
            System.err.println("Usage FB2KeywordsExtractor <src> <dst>");
            System.exit(1);
        } else
            System.exit(ToolRunner.run(new Configuration(), new FB2KeywordsExtractor(), args));
    }

}
