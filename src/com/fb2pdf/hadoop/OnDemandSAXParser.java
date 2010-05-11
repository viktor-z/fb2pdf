/**
 * Parser which returns (when asked) next pair of XML path and concatenation of
 * text elements.
 */

package com.fb2pdf.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.*;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

class OnDemandSAXParser extends DefaultHandler implements Runnable
{
    public static final String XML_PATH_SEPARATOR = "/";

    class StringPair
    {
        @Override
        public String toString()
        {
            return ""+a+","+b;
        }
        public String a;
        public String b;
    };

    private InputStream  in;
    private Thread       thread;
    private SAXParser    saxParser;
    private List<String> path;
    StringBuffer         text;

    private Object       mutex;     // protects following fields
    private boolean      done;
    private boolean      data_ready;
    private Exception    exception;
    private StringPair   pair;

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
        this.path = new LinkedList<String>();
        this.text = new StringBuffer();
        this.done = false;
        this.data_ready = false;
        this.mutex = new Object();
        this.thread = new Thread(this);
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
                waitForSpace();
                done = true;
                exception = e;
                pair = null;
                data_ready = true;
                mutex.notifyAll();
            }
        }
    }

    @Override
    public void endDocument() throws SAXException
    {
        synchronized(mutex)
        {
            waitForSpace();
            done = true;
            exception = null;
            pair = null;
            data_ready = true;
            mutex.notifyAll();
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXException
    {
        synchronized(mutex)
        {
            waitForSpace();
            done = true;
            pair = null;
            exception = e;
            data_ready = true;
            mutex.notifyAll();
        }
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException
    {
        synchronized(mutex)
        {
            waitForSpace();
            done = true;
            pair = null;
            exception = e;
            data_ready = true;
            mutex.notifyAll();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        text.append(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
    {
        text.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
    {
        path.add(name);
        //System.err.println("Start element : " + StringUtils.join(path, XML_PATH_SEPARATOR));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        //System.err.println("End element : " + StringUtils.join(path, XML_PATH_SEPARATOR));
        if(path.size() == 0)
            throw new SAXException("Non matching start/end element");

        synchronized(mutex)
        {
            waitForSpace();
            pair = new StringPair();
            pair.a = StringUtils.join(path, XML_PATH_SEPARATOR);
            pair.b = text.toString();
            text = new StringBuffer();
            data_ready = true;
            mutex.notifyAll();
        }
        path.remove(path.size() - 1);

    }

    private void waitForSpace()
    {
        // we are synchronized(mutex) here
        while(data_ready)
        {
            try
            {
                mutex.wait();
            } catch(InterruptedException e)
            {
            }
        }
    }

    public void stop()
    {
        // TODO Auto-generated method stub
    }

    public StringPair getNext() throws IOException
    {
        synchronized(mutex)
        {
            while(!data_ready)
            {
                try
                {
                    mutex.wait();
                } catch(InterruptedException e)
                {
                }
            }

            data_ready = false;

            if(exception != null)
            {            
                mutex.notifyAll(); // need this?
                if(exception instanceof IOException)
                    throw (IOException) exception;
                else
                    throw new IOException(exception);
            }

            if(done)
            {
                mutex.notifyAll(); // need this?
                return null;
            } else
            {
                StringPair r = pair;
                mutex.notifyAll();
                return r;
            }
        }
    }
}