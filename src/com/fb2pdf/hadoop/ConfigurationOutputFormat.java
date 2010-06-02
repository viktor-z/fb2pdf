package com.fb2pdf.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.util.Progressable;

public class ConfigurationOutputFormat <K extends Text, V extends Text> extends FileOutputFormat<K, V>
{
	@Override
	public RecordWriter<K, V> getRecordWriter(FileSystem ignored, JobConf job, String name, Progressable progress) throws IOException {
		  Path file = FileOutputFormat.getTaskOutputPath(job, name);
	      FileSystem fs = file.getFileSystem(job);
	      FSDataOutputStream fileOut = fs.create(file, progress);
	      return new ConfigurationRecordWriter<K, V>(fileOut);
	}

}
