package com.fb2pdf.hadoop.cluster;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fb2pdf.hadoop.ConfigurationOutputFormat;
import com.fb2pdf.hadoop.XMLTextInputFormat;

public class FB2MetaExtractor extends Configured implements Tool
{
	
    private static final Log          logger = LogFactory.getLog("com.fb2pdf.hadoop.FB2MetaExtractor");

    static Map<String, String>        includeKeys;
    static
    {
        includeKeys = new HashMap<String, String>();
        includeKeys.put("FictionBook/description/title-info/lang", "lang");
    }

    static class ExtractMetaMapper extends MapReduceBase implements Mapper<Text, Text, Text, Text>
    {
        public ExtractMetaMapper()
        {
            super();
        }

        private Text metaKey = new Text();
        private Text metaVal = new Text();

        @Override
        public void map(Text key, Text value, OutputCollector<Text, Text> output, Reporter reporter)
                throws IOException
        {
            if(!includeKeys.containsKey(key.toString()))
                return;
            
            String line = value.toString().trim().toLowerCase();
            metaKey.set(includeKeys.get(key.toString()));
            metaVal.set(line);
            output.collect(metaKey, metaVal);
        }

    };
    
    @Override
    public int run(String[] args) throws Exception
    {
        JobConf conf = new JobConf(getConf(), FB2MetaExtractor.class);
        conf.setJobName("FB2MetaExtractor");

        conf.setInputFormat(XMLTextInputFormat.class);
        conf.setOutputFormat(ConfigurationOutputFormat.class);

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(ExtractMetaMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        conf.setNumReduceTasks(1);
        
        Path inpath = new Path(args[0]);
        Path outpath = new Path(args[1]);
        FileInputFormat.addInputPath(conf, inpath);
        FileOutputFormat.setOutputPath(conf, outpath);
        
        logger.info("Extracting meta from " + inpath + " to " + outpath);
        
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
            System.err.println("Usage FB2MetaExtractor <src> <dst>");
            System.exit(1);
        } else
            System.exit(ToolRunner.run(new Configuration(), new FB2MetaExtractor(), args));
    }

}
