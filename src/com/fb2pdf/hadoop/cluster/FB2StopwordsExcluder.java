package com.fb2pdf.hadoop.cluster;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;

public class FB2StopwordsExcluder extends Configured implements Tool
{

    private static final Log logger = LogFactory.getLog("com.fb2pdf.hadoop.FB2StopwordsExcluder");

    static class ExcluderMapper extends MapReduceBase implements Mapper<Text, LongWritable, Text, LongWritable>
    {
        private JobConf conf;
        private Set<String> stopwords = null;
        private Text word = new Text();

        public ExcluderMapper()
        {
            super();
        }

        @Override
        public void map(Text key, LongWritable value, OutputCollector<Text, LongWritable> output, Reporter reporter)
                throws IOException
        {
            if(stopwords != null && stopwords.contains(key.toString().trim()))
                return;

            word.set(key.toString());
            output.collect(word, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void configure(JobConf job) {
            super.configure(job);
            conf = job;
            boolean isLocalMode = "local".equals(conf.get("mapred.job.tracker", "local"));

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

                    if (!isLocalMode)
                    {
                        logger.debug("cacheFilePath: " + stopwordsFile);
                        Path[] cacheFiles = DistributedCache.getLocalCacheFiles(conf);
                        if (cacheFiles != null)
                            for (Path p:cacheFiles)
                                if (stopwordsFile.getName().equals(p.getName())) {
                                    stopwordsFilePath = p;
                                    break;
                                }
                    } else {
                        stopwordsFilePath = new Path(stopwordsCache, stopwordsFile);
                    }

                    logger.info("stopwordsFilePath: " + stopwordsFilePath);

                    if (stopwordsFilePath != null)
                    {
                        InputStream in = null;
                        try
                        {
                            FileSystem fsLocal = FileSystem.getLocal(conf);
                            in = fsLocal.open(stopwordsFilePath);
                            stopwords = new HashSet<String>(org.apache.commons.io.IOUtils.readLines(in, "UTF-8"));
                        }
                        finally
                        {
                            if (in != null) in.close();
                        }
                    }

                    logger.info((stopwords==null)?"no stopwords":"stopwords size: " + stopwords.size());
                }
            } catch (IOException e)	{
                logger.error("loading stopwords has failed", e);
            }
        }

    }

    @Override
    public int run(String[] args) throws Exception
    {
        JobConf conf = new JobConf(getConf(), FB2StopwordsExcluder.class);
        conf.setJobName("FB2StopwordsExcluder");
        conf.set("fb2.xmlreader.stopelement", "FictionBook/description");

        conf.setInputFormat(SequenceFileInputFormat.class);
        conf.setOutputFormat(SequenceFileOutputFormat.class);

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapperClass(ExcluderMapper.class);
        conf.setReducerClass(IdentityReducer.class);
        conf.setNumReduceTasks(1);

        Path inpath = new Path(args[0]);
        Path outpath = new Path(args[1]);
        FileInputFormat.addInputPath(conf, inpath);
        FileOutputFormat.setOutputPath(conf, outpath);
        conf.setBoolean("mapred.output.compress", true);
        conf.setClass("mapred.output.compression.codec", GzipCodec.class,  CompressionCodec.class);

        boolean isLocalMode = "local".equals(conf.get("mapred.job.tracker", "local"));

        Path srcStopwords = new Path(args[2]);
        FileSystem fs = isLocalMode?FileSystem.getLocal(conf):FileSystem.get(conf);

        if (fs.exists(srcStopwords))
        {
	        FileStatus srcStopwordsStatus = fs.getFileStatus(srcStopwords);
	        if (srcStopwordsStatus.isDir())
	        {
                if (!isLocalMode)
                {
                    FileStatus[] ls = fs.listStatus(srcStopwords);
                    for (FileStatus s:ls)
                        DistributedCache.addCacheFile(s.getPath().toUri(), conf);
                }

		        conf.set("fb2pdf.stopwords", args[2]);
	        }
        }

        conf.set("fb2pdf.metafile", args[3]);
        logger.info("Using stopwords from " + srcStopwords);
        logger.info("Using meta from " + args[3]);

        logger.info("Filtering keywords from " + inpath + " to " + outpath);

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
					metaConf.addResource(s.getPath().toString());
				}
			}
		}
		else
		{
			metaConf.addResource(metaFilePath.toString());
		}

		return metaConf;
    }

    public static void main(String[] args) throws Exception
    {
        if(args.length != 4)
        {
            System.err.println("Usage FB2StopwordsExcluder <src> <dst> <stopwords> <meta>");
            System.exit(1);
        } else
            System.exit(ToolRunner.run(new Configuration(), new FB2StopwordsExcluder(), args));
    }

}
