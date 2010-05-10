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
        public String a;
        public String b;
    };

    private InputStream  in;
    private Thread       thread;
    private SAXParser    saxParser;
    private Exception    exception;
    private Object       mutex;
    private List<String> path;
    StringBuffer         text;
    private boolean      done;

    @Override
    public void endDocument() throws SAXException
    {
        done = true;
        exception = null;
    }

    @Override
    public void error(SAXParseException e) throws SAXException
    {
        done = true;
        exception = e;
        synchronized(mutex)
        {
            mutex.notifyAll();
        }
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException
    {
        done = true;
        exception = e;
        synchronized(mutex)
        {
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
        text.delete(0, text.length() - 1);
        path.add(localName);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        if(path.size() == 0)
            throw new SAXException("Non matching start/end element");
        path.remove(path.size() - 1);
        synchronized(mutex)
        {
            mutex.notifyAll();
        }
    }

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
        synchronized(mutex)
        {
            while(true)
            {
                try
                {
                    mutex.wait();
                    break;
                } catch(InterruptedException e)
                {
                }
            }

            if(exception != null)
            {
                if(exception instanceof IOException)
                    throw (IOException) exception;
                else
                    throw new IOException(exception);
            }

            if(done)
                return null;
            else
            {
                StringPair res = new StringPair();
                res.a = StringUtils.join(path, XML_PATH_SEPARATOR);
                res.b = text.toString();
                return res;
            }
        }
    }
}