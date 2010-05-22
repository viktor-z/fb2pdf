package com.fb2pdf.hadoop;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.math.VectorWritable;

public class VectorDumper extends MeanShiftOutDumper {

	@Override
	protected void dump(SequenceFile.Reader reader, PrintWriter writer) throws IOException{
		VectorWritable value = new VectorWritable();
		Text key = new Text();
		while(reader.next(key, value)){
			if(key != null && value != null){
				writer.print(key.toString() + " : " + value.get().asFormatString());
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err
					.println("Usage VectorDumper <src> <out>");
			System.exit(1);
		} else {
			Configuration conf = new Configuration();
			ToolRunner.run(conf, new VectorDumper(), args);
		}
	}
	
}
