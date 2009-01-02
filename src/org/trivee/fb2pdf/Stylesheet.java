package org.trivee.fb2pdf;

import java.io.IOException;
import java.io.FileReader;
import java.util.LinkedList;

import com.lowagie.text.DocumentException;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

public class Stylesheet
{
    private LinkedList<FontFamily> fontFamilies = new LinkedList<FontFamily>();

    public Stylesheet()
        throws Exception
    {
    }

    public void validate()
        throws DocumentException, IOException, FB2toPDFException
    {
        for (FontFamily family: fontFamilies)
        {
            family.validate();
        }
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

    public static void main(String[] args)
    {
        try
        {
            Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();

            Stylesheet s = gson.fromJson(new FileReader("./data/stylesheet.json"), Stylesheet.class);
            s.validate();

            System.out.println(gson.toJson(s));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}