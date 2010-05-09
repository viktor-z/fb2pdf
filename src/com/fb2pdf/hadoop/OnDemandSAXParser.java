/**
 * 
 */

package com.fb2pdf.hadoop;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.*;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class OnDemandSAXParser extends DefaultHandler implements Runnable
{
    class StringPair
    {
        public String a;
        public String b;
    };

    private InputStream in;
    private Thread      thread;
    private SAXParser   saxParser;
    private Exception   exception;
    private Object      mutex;

    public OnDemandSAXParser(InputStream in) throws IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
            saxParser = factory.newSAXParser();
        } catch(ParserConfigurationException e)
        {
            throw new IOException(e);
        } catch(SAXException e)
        {
            throw new IOException(e);
        }

        this.in = in;
        mutex = new Object();
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run()
    {
        try
        {
            saxParser.parse(in, this);
        } catch(Exception e)
        {
            synchronized(mutex)
            {
                this.exception = e;
            }
        }
    }

    public void stop()
    {
        // TODO Auto-generated method stub

    }

    public StringPair getNext() throws IOException
    {
        if(exception != null)
        {
            if(exception instanceof IOException)
                throw (IOException) exception;
            else
                throw new IOException(exception);
        }

        return null;
        // TODO: implement
    }
}