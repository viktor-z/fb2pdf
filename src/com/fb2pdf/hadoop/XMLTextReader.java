
package com.fb2pdf.hadoop;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapred.*;

import com.fb2pdf.hadoop.OnDemandSAXParser.StringPair;


public class XMLTextReader implements RecordReader<Text, Text>
{

    private CompressionCodecFactory compressionCodecs;
    InputStream                     in;
    private long                    start;
    private long                    end;
    private OnDemandSAXParser       parser;

    public XMLTextReader(JobConf job, FileSplit split) throws IOException
    {
        final Path file = split.getPath();
        start = split.getStart();
        end = start + split.getLength();

        compressionCodecs = new CompressionCodecFactory(job);
        final CompressionCodec codec = compressionCodecs.getCodec(file);

        FileSystem fs = file.getFileSystem(job);
        FSDataInputStream fileIn = fs.open(split.getPath());

        fileIn.seek(start);
        if(codec != null)
            in = codec.createInputStream(fileIn);
        else
            in = fileIn;

        parser = new OnDemandSAXParser(in);
    }

    @Override
    public void close() throws IOException
    {
        parser.stop();
        in.close();
    }

    @Override
    public Text createKey()
    {
        return new Text();
    }

    @Override
    public Text createValue()
    {
        return new Text();
    }

    @Override
    public long getPos() throws IOException
    {
        return start; // TODO: implement
    }

    @Override
    public float getProgress() throws IOException
    {
        if(start == end)
            return 0.0f;
        else
            return Math.min(1.0f, (getPos() - start) / (float) (end - start));
    }

    @Override
    public boolean next(Text key, Text value) throws IOException
    {
        StringPair n = parser.getNext();
        if(n != null)
        {
            key.set(n.a);
            value.set(n.b);
            return true;
        } else
        {
            return false; // EOF
        }
    }

}
