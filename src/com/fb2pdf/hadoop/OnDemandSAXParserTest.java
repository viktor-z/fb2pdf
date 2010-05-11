
package com.fb2pdf.hadoop;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;

import com.fb2pdf.hadoop.OnDemandSAXParser.StringPair;

public class OnDemandSAXParserTest
{

    @Test(expected=IOException.class) 
    public void testBroken() throws IOException
    {
        FileInputStream fs = new FileInputStream("test_data/broken_fictionbook_2_1.fb2");
        OnDemandSAXParser p = new OnDemandSAXParser(fs);
        StringPair n;
        while((n = p.getNext()) != null)
        {
            assertNotNull(n.a);
            assertNotNull(n.b);
        }
        p.stop();
    }

    /**
     * This test ensures that we are not parsing whole file at once. 
     * The XML is broken down the road, but we do not see as we only get first few elements.
     * @throws IOException
     */
    public void testOnDemand() throws IOException
    {
        FileInputStream fs = new FileInputStream("test_data/broken_fictionbook_2_1.fb2");
        OnDemandSAXParser p = new OnDemandSAXParser(fs);
        StringPair n= p.getNext();
        assertNotNull(n);
        p.stop();
    }

    @Test
    public void testGood() throws IOException
    {
        FileInputStream fs = new FileInputStream("test_data/fictionbook_2_1.fb2");
        OnDemandSAXParser p = new OnDemandSAXParser(fs);
        StringPair n;
        int i = 0;
        while((n = p.getNext()) != null)
        {
            i++;
            assertNotNull(n.a);
            assertNotNull(n.b);

            if(i == 19)
            {
                assertEquals("FictionBook/description/title-info/lang", n.a);
                assertEquals("ru", n.b.trim());
            }
            // System.err.println(i+": next: "+n);
        }
        assertEquals(156, i);
        p.stop();
    }

}
