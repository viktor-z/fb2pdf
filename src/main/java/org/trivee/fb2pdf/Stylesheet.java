package org.trivee.fb2pdf;

import java.io.IOException;
import java.util.LinkedList;

import com.itextpdf.text.DocumentException;

import com.google.gson.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.commons.io.input.BOMInputStream;

public class Stylesheet
{
    private LinkedList<FontFamily> fontFamilies = new LinkedList<FontFamily>();
    private PageStyle pageStyle = new PageStyle();
    private HyphenationSettings hyphenationSettings = new HyphenationSettings();
    private TextPreprocessorSettings textPreprocessorSettings = new TextPreprocessorSettings();
    private TransformationSettings transformationSettings = new TransformationSettings();
    private GeneralSettings generalSettings = new GeneralSettings();
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

    public HyphenationSettings getHyphenationSettings()
    {
        return hyphenationSettings;
    }

    public TextPreprocessorSettings getTextPreprocessorSettings() {
        return textPreprocessorSettings;
    }

    public TransformationSettings getTransformationSettings() {
        return transformationSettings;
    }

    public GeneralSettings getGeneralSettings()
    {
        return generalSettings;
    }

    public LinkedList<ParagraphStyle> getParagraphStyles() {
        return paragraphStyles;
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
        FileInputStream fis = new FileInputStream(filename);
        return readStylesheet(fis);
    }

    public static Stylesheet readStylesheet(InputStream stream)
        throws DocumentException, IOException, FB2toPDFException
    {
        BOMInputStream bomStream = new BOMInputStream(stream);
        return readStylesheet(new InputStreamReader(bomStream, "UTF-8"));
    }

    public static Stylesheet readStylesheet(Reader reader)
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

        Stylesheet stylesheet = gson.fromJson(reader, Stylesheet.class);
        return stylesheet;
    }

    @Override
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
