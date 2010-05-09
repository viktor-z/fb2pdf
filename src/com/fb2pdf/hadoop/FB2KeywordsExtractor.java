
package com.fb2pdf.hadoop;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.LongSumReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FB2KeywordsExtractor extends Configured implements Tool
{
    private static final Log          logger = LogFactory.getLog("com.fb2pdf.hadoop.FB2KeywordsExtractor");
    private final static LongWritable one    = new LongWritable(1);

    class ExtractKeywordsMapper extends MapReduceBase implements Mapper<LongWritable, Text, Text, LongWritable>
    {
        private Text word = new Text();

        @Override
        public void map(LongWritable key, Text value, OutputCollector<Text, LongWritable> output, Reporter reporter)
                throws IOException
        {
            String line = value.toString();
            StringTokenizer st = new StringTokenizer(line);
            while(st.hasMoreTokens())
            {
                word.set(st.nextToken());
                output.collect(word, one);
            }
        }
    };

    @Override
    public int run(String[] args) throws Exception
    {
        JobConf conf = new JobConf(getConf(), FB2KeywordsExtractor.class);
        conf.setJobName("FB2KeywordsExtractor");

        conf.setInputFormat(FB2TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapperClass(ExtractKeywordsMapper.class);
        conf.setCombinerClass(LongSumReducer.class);
        conf.setReducerClass(LongSumReducer.class);
        conf.setNumReduceTasks(1);

        Path inpath = new Path(args[0]);
        Path outpath = new Path(args[0]);
        FileInputFormat.addInputPath(conf, inpath);
        FileOutputFormat.setOutputPath(conf, outpath);

        logger.info("Extracting keywords from " + inpath + " to " + outpath);

        JobClient.runJob(conf);

        return 0;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        System.exit(ToolRunner.run(new Configuration(), new FB2KeywordsExtractor(), args));
    }

}
