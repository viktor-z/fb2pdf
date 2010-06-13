package com.fb2pdf.hadoop.cluster;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopy;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.list.IntArrayList;

public class MeanShiftOutDumper extends Configured implements Tool {
	
	
	public class PrefixAdditionFilter implements PathFilter {
		
		private FileSystem fs;
		private Configuration conf;
		private PrintWriter os;
		
		public PrefixAdditionFilter(FileSystem fs, Configuration conf, PrintWriter out){
			this.fs = fs;
			this.conf = conf;
			this.os = out;
		}

		@Override
		public boolean accept(Path path) {
			try {
				if(fs.getFileStatus(path).isDir()){
					try {
						fs.listStatus(path, new PrefixAdditionFilter(fs, conf, os));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else{
					SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf);
					dump(reader, os);
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		
	}
	
	protected void dump(SequenceFile.Reader reader, PrintWriter writer) throws IOException{
		MeanShiftCanopy value = new MeanShiftCanopy();
		Text key = new Text();
		while(reader.next(key, value)){
			if(key != null && value != null){
				writer.println("key: " + key.toString());
				writer.println("value: " + value.getCenter().getName());
			}
		}
	}
	
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Path path = new Path(args[1]);
		FileSystem fs = path.getFileSystem(conf);
		fs.delete(new Path(args[1]), true);
		PrintWriter out = new PrintWriter(fs.create(new Path(args[1])));
		fs.listStatus(new Path(args[0]), new PrefixAdditionFilter(fs, conf, out));
		out.close();
		return 0;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err
					.println("Usage MeanShiftOutDumper <src> <out>");
			System.exit(1);
		} else {
			Configuration conf = new Configuration();
			ToolRunner.run(conf, new MeanShiftOutDumper(), args);
		}
	}
	
}
