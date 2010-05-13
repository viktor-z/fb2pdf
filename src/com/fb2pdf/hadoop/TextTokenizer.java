
package com.fb2pdf.hadoop;

/**
 * A placeholder class to extract meaningful keywords from string of text. This
 * is clearly unsufficient.
 * 
 * @author lord
 * 
 */
public class TextTokenizer
{
    String[] keywords;
    int      pos;

    public TextTokenizer(String str)
    {
        keywords = str.split("[^\\p{javaLetter}]+");
        if(keywords.length > 0 && keywords[0].isEmpty())
            pos = 1;
        else
            pos = 0;
    }

    public boolean hasMoreTokens()
    {
        return pos != keywords.length;
    }

    public String nextToken()
    {
        return keywords[pos++];
    }

}
