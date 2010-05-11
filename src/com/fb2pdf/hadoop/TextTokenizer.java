package com.fb2pdf.hadoop;


/**
 * A placeholder class to extract meaningful keywords from string of text.
 * This is clearly unsifficient - we need to handle many cases, such as
 * "hello- " (hello) and "hocus-pocus" (hocus-pocus), "'jazz'" (jazz), etc.
 * 
 * @author lord
 *
 */
public class TextTokenizer
{
    private String[] keywords;
    private int pos;
    
    public TextTokenizer(String str)
    {
        keywords = str.split("[^\\p{javaLetter}]");
        pos = 0;
    }

    public boolean hasMoreTokens()
    {
        return pos!=keywords.length;
    }

    public String nextToken()
    {
        return keywords[pos++];
    }

}
