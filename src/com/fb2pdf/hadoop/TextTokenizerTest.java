
package com.fb2pdf.hadoop;

import static org.junit.Assert.*;

import org.junit.Test;

public class TextTokenizerTest
{

    @Test
    public void marginalCasesTest()
    {
        TextTokenizer tt = new TextTokenizer("");
        assertFalse(tt.hasMoreTokens());

        tt = new TextTokenizer("   ! 123");
        assertFalse(tt.hasMoreTokens());

        tt = new TextTokenizer(",,,,");
        assertFalse(tt.hasMoreTokens());

        tt = new TextTokenizer("a");
        assertTrue(tt.hasMoreTokens());
        assertEquals("a", tt.nextToken());
        assertFalse(tt.hasMoreTokens());

    }

    @Test
    public void basicTest()
    {
        TextTokenizer tt = new TextTokenizer("hello world!");
        assertTrue(tt.hasMoreTokens());
        assertEquals("hello", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("world", tt.nextToken());
        assertFalse(tt.hasMoreTokens());
    }

    @Test
    public void basicTest1()
    {
        TextTokenizer tt = new TextTokenizer("'hello',  hello, a ,world!");
        assertTrue(tt.hasMoreTokens());
        assertEquals("hello", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("hello", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("a", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("world", tt.nextToken());
        assertFalse(tt.hasMoreTokens());
    }

    @Test
    public void hyphenationTest()
    {
        TextTokenizer tt = new TextTokenizer("loud- hooligan");
        assertTrue(tt.hasMoreTokens());
        assertEquals("loud", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("hooligan", tt.nextToken());
        assertFalse(tt.hasMoreTokens());

        tt = new TextTokenizer("loud -hooligan");
        assertTrue(tt.hasMoreTokens());
        assertEquals("loud", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("hooligan", tt.nextToken());
        assertFalse(tt.hasMoreTokens());

        tt = new TextTokenizer("loud-mouthed hooligan");
        assertTrue(tt.hasMoreTokens());
        assertEquals("loud-mouthed", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("hooligan", tt.nextToken());
        assertFalse(tt.hasMoreTokens());

    }

    @Test
    public void apostropheTest()
    {
        // English possessive apostrophe

        // singular
        TextTokenizer tt = new TextTokenizer("cat's whiskers");
        assertTrue(tt.hasMoreTokens());
        assertEquals("cats", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("whiskers", tt.nextToken());
        assertFalse(tt.hasMoreTokens());
        // TODO: Add test to verify that in case of russian words it splits on
        // apostrophe

        // double apostrophe is just punctuation
        tt = new TextTokenizer("''cat''s whiskers");
        assertTrue(tt.hasMoreTokens());
        assertEquals("cat", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("s", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("whiskers", tt.nextToken());
        assertFalse(tt.hasMoreTokens());

        // plural
        tt = new TextTokenizer("pens' caps");
        assertTrue(tt.hasMoreTokens());
        assertEquals("pens", tt.nextToken());
        assertTrue(tt.hasMoreTokens());
        assertEquals("caps", tt.nextToken());
        assertFalse(tt.hasMoreTokens());

    }

}
