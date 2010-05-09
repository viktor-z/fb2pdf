
package com.fb2pdf.hadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

/**
 * This reader reads XML file returning TEXT nodes for each element.
 * Key is element name (hierarchical. e.g. /a/b/c). They valye is concatenated
 * direct TEXT children of this element.
 * 
 * @author lord
 *
 */
public class XMLTextInputFormat extends FileInputFormat<Text, Text> implements JobConfigurable
{
    @Override
    public void configure(JobConf job)
    {
    }

    @Override
    protected boolean isSplitable(FileSystem fs, Path file)
    {
        return false;
    }

    @Override
    public RecordReader<Text, Text> getRecordReader(InputSplit genericSplit, JobConf job, Reporter reporter)
            throws IOException
    {
        reporter.setStatus(genericSplit.toString());
        return new XMLTextReader(job, (FileSplit) genericSplit);
    }

}
