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

    public Stylesheet()
    {
    }

    public FontFamily getFontFamily(String name)
        throws FB2toPDFException
    {
        for (FontFamily family: fontFamilies)
        {
            if (family.getName().equalsIgnoreCase(name))
                return family;
        }

        throw new FB2toPDFException("Font family " + name + " not defined in the stylesheet.");
    }

    public PageStyle getPageStyle()
    {
        return pageStyle;
    }

    public static Stylesheet readStylesheet(String filename)
        throws DocumentException, IOException, FB2toPDFException
    {
        Gson gson =
            Dimension.prepare(
            FontFamily.prepare(
                new GsonBuilder()
            ))
            .serializeNulls()
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
                new GsonBuilder()
            ))
            .serializeNulls()
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
