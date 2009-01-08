package org.trivee.fb2pdf;

import java.io.IOException;
import java.io.FileReader;
import java.util.LinkedList;

import com.lowagie.text.DocumentException;

import com.google.gson.*;

public class Stylesheet
{
    private LinkedList<FontFamily> fontFamilies = new LinkedList<FontFamily>();
    private PageStyle pageStyle = new PageStyle();
    private LinkedList<ParagraphStyle> paragraphStyles = new LinkedList<ParagraphStyle>();

    public Stylesheet()
    {
    }

    public FontFamily getFontFamily(String name)
        throws FB2toPDFException
    {
        for (FontFamily family: fontFamilies)
        {
            if (family.getName() == null)
                throw new FB2toPDFException("Font family without a name found in the stylesheet.");

            if (family.getName().equalsIgnoreCase(name))
                return family;
        }

        throw new FB2toPDFException("Font family " + name + " not defined in the stylesheet.");
    }

    public PageStyle getPageStyle()
    {
        return pageStyle;
    }

    public ParagraphStyle getParagraphStyle(String name)
        throws FB2toPDFException
    {
        for (ParagraphStyle paragraphStyle: paragraphStyles)
        {
            if (paragraphStyle.getName() == null)
                throw new FB2toPDFException("Paragraph style without a name found in the stylesheet.");

            if (paragraphStyle.getName().equalsIgnoreCase(name))
            {
                paragraphStyle.setStylesheet(this);
                return paragraphStyle;
            }
        }

        throw new FB2toPDFException("Paragraph style " + name + " not defined in the stylesheet.");
    }

    public static Stylesheet readStylesheet(String filename)
        throws DocumentException, IOException, FB2toPDFException
    {
        Gson gson =
            Dimension.prepare(
            FontFamily.prepare(
            ParagraphStyle.prepare(
                new GsonBuilder()
            )))
            .setPrettyPrinting()
            .create();

        Stylesheet stylesheet = gson.fromJson(new FileReader(filename), Stylesheet.class);
        return stylesheet;
    }

    public String toString()
    {
        Gson gson =
            Dimension.prepare(
            FontFamily.prepare(
            ParagraphStyle.prepare(
                new GsonBuilder()
            )))
            .setPrettyPrinting()
            .create();

        return gson.toJson(this);
    }
    
    public static void main(String[] args)
    {
        try
        {
            Stylesheet s = readStylesheet("./data/stylesheet.json");
            System.out.println(s);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
