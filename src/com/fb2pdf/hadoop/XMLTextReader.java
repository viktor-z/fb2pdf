package com.fb2pdf.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

public class XMLTextReader implements RecordReader<Text, Text>
{

    public XMLTextReader(JobConf job, FileSplit genericSplit)
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void close() throws IOException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Text createKey()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Text createValue()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getPos() throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getProgress() throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean next(Text key, Text value) throws IOException
    {
        // TODO Auto-generated method stub
        return false;
    }

}
