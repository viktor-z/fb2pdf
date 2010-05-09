package com.fb2pdf.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

public class FB2TextInputFormat extends TextInputFormat
{
    @Override
    protected boolean isSplitable(FileSystem fs,
            Path file)
    {
        return false;
    }

    @Override
    public RecordReader<LongWritable, Text> getRecordReader(InputSplit genericSplit, JobConf job, Reporter reporter)
            throws IOException
    {
        // TODO Auto-generated method stub
        return super.getRecordReader(genericSplit, job, reporter);
    }

    

}
