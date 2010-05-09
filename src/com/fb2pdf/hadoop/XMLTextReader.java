
package com.fb2pdf.hadoop;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapred.*;

public class XMLTextReader implements RecordReader<Text, Text>
{
    private CompressionCodecFactory compressionCodecs;
    InputStream                     in;

    public XMLTextReader(JobConf job, FileSplit split) throws IOException
    {
        final Path file = split.getPath();
        compressionCodecs = new CompressionCodecFactory(job);
        final CompressionCodec codec = compressionCodecs.getCodec(file);

        FileSystem fs = file.getFileSystem(job);
        FSDataInputStream fileIn = fs.open(split.getPath());
        if(codec != null)
            in = codec.createInputStream(fileIn);
        else
            this.in = fileIn;

    }

    @Override
    public void close() throws IOException
    {
        in.close();
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
