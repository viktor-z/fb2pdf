package com.fb2pdf.hadoop.cluster;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
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
    	private JobConf conf;
    	private Set<String> stopwords = null;
    	
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
                if(k.length()>1 && (stopwords == null || !stopwords.contains(k)))
                {
                    word.set(k);
                    output.collect(word, one);
                }
            }
        }

		@SuppressWarnings("unchecked")
		@Override
		public void configure(JobConf job) {
			super.configure(job);
			conf = job;
			
			try
			{
				String metaFile = conf.get("fb2pdf.metafile", null);
								
				Configuration metaConf = getMetadataConfiguration(conf, metaFile);
				String lang = metaConf.get("lang");
				logger.debug("lang: " + metaConf.get("lang"));
				
				Path stopwordsFilePath = null;
				String stopwordsCache = conf.get("fb2pdf.stopwords", null);
				if (stopwordsCache != null && lang != null)
				{
					Path stopwordsFile = new Path(lang.toLowerCase()+".txt");
					
					logger.debug("cacheFilePath: " + stopwordsFile);
					Path[] cacheFiles = DistributedCache.getLocalCacheFiles(conf);
					for (Path p:cacheFiles)
						if (stopwordsFile.getName().equals(p.getName())) {
							stopwordsFilePath = p;
							break;
						}
					
					logger.debug("stopwordsFilePath: " + stopwordsFilePath);
					
					if (stopwordsFilePath != null)
					{						
						InputStream in = null;
						try
						{
							FileSystem fsLocal = FileSystem.getLocal(conf);
							in = fsLocal.open(stopwordsFilePath);
							stopwords = new HashSet<String>(org.apache.commons.io.IOUtils.readLines(in));
						}
						finally
						{
							if (in != null) in.close();
						}
					}
					
					logger.debug("stopwords size: " + stopwords.size());
				}
			} catch (IOException e)	{
				
			}
		}
    };

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
        
        Path srcStopwords = new Path(args[2]);
        FileSystem fs = FileSystem.get(conf);
        
        if (fs.exists(srcStopwords))
        {
	        FileStatus srcStopwordsStatus = fs.getFileStatus(srcStopwords);
	        if (srcStopwordsStatus.isDir())
	        {
		        FileStatus[] ls = fs.listStatus(srcStopwords);
		        
		        for (FileStatus s:ls)
		        	DistributedCache.addCacheFile(s.getPath().toUri(), conf);
		        
		        conf.set("fb2pdf.stopwords", args[2]);
	        }
        }
        
        conf.set("fb2pdf.metafile", args[3]);
        
        logger.info("Extracting keywords from " + inpath + " to " + outpath);
        logger.info("Using stopwords from " + srcStopwords);
        logger.info("Using meta from " + args[3]);
        
        JobClient.runJob(conf);

        return 0;
    }
    
    public static Configuration getMetadataConfiguration(Configuration conf, String metaFile) 
    	throws IOException
    {
		if (metaFile == null) return null;
		
		Configuration metaConf = new Configuration();
		Path metaFilePath = new Path(metaFile);
		
		FileSystem fs = FileSystem.get(metaFilePath.toUri(), conf);
		FileStatus metaFileStatus = fs.getFileStatus(metaFilePath);
		if (metaFileStatus.isDir())
		{
			FileStatus[] ls = fs.listStatus(metaFilePath);
			for (FileStatus s:ls)
			{
				if (!s.isDir())
				{
					metaConf.addResource(s.getPath().toUri().toURL());
				}
			}
		}
		else
		{
			metaConf.addResource(metaFilePath.toUri().toURL());
		}
		
		return metaConf;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {    	
        if(args.length != 4)
        {
            System.err.println("Usage FB2KeywordsExtractor <src> <dst> <stopwords> <meta>");
            System.exit(1);
        } else
            System.exit(ToolRunner.run(new Configuration(), new FB2KeywordsExtractor(), args));
    }

}
