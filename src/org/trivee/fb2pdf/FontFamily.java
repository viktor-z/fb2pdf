package org.trivee.fb2pdf;

import java.io.IOException;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

public class FontFamily
{
    private String name;
    private String regular;
    private String bold;
    private String italic;
    private String boldItalic;

    private transient BaseFont regularFont;
    private transient BaseFont boldFont;
    private transient BaseFont italicFont;
    private transient BaseFont boldItalicFont;

    private FontFamily()
    {
    }

    public FontFamily(String name, String regular, String bold, String italic, String boldItalic)
        throws DocumentException, IOException, FB2toPDFException
    {
        this.name = name;

        this.regular    = regular;
        this.bold       = bold;
        this.italic     = italic;
        this.boldItalic = boldItalic;

        validate();
    }

    public void validate()
        throws DocumentException, IOException, FB2toPDFException
    {
        if (regular     == null) throw new FB2toPDFException("Regular font for " + name + " not defined.");
        if (bold        == null) throw new FB2toPDFException("Bold font for " + name + " not defined.");
        if (italic      == null) throw new FB2toPDFException("Italic font for " + name + " not defined.");
        if (boldItalic  == null) throw new FB2toPDFException("Bold italic font for " + name + " not defined.");

        this.regularFont    = BaseFont.createFont(regular, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        this.boldFont       = BaseFont.createFont(bold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        this.italicFont     = BaseFont.createFont(italic, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        this.boldItalicFont = BaseFont.createFont(boldItalic, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    }

    public String getName()
    {
        return this.name;
    }

    public String getRegular()
    {
        return this.regular;
    }

    public String getBold()
    {
        return this.bold;
    }

    public String getItalic()
    {
        return this.italic;
    }

    public String getBoldItalic()
    {
        return this.boldItalic;
    }

    public BaseFont getRegularFont()
    {
        return regularFont;
    }

    public BaseFont getBoldFont()
    {
        return boldFont;
    }

    public BaseFont getItalicFont()
    {
        return italicFont;
    }

    public BaseFont getBoldItalicFont()
    {
        return boldItalicFont;
    }
}
