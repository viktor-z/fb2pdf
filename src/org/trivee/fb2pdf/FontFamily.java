package org.trivee.fb2pdf;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.*;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;

public class FontFamily
{
    private static final class FontInfo
    {
        private String   filename;
        private BaseFont font;

        private FontInfo(String filename)
            throws DocumentException, IOException
        {
            this(filename, BaseFont.IDENTITY_H);
        }

        private FontInfo(String filename, String encoding)
            throws DocumentException, IOException
        {
            this.filename = filename;
            this.font = BaseFont.createFont(filename, encoding, BaseFont.EMBEDDED);
        }
    };

    private static final class FontInfoIO
        implements JsonDeserializer<FontInfo>,JsonSerializer<FontInfo>
    {

        public FontInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            try
            {
                String[] parts = json.getAsString().split("#");
                String[] parts2 = parts[0].split(",");
                String filename =  Utilities.getValidatedFileName(parts2[0]);
                String encoding = parts.length > 1 ? parts[1] : null;
                filename += parts2.length > 1 ? "," + parts2[1] : "";
                if (encoding == null)
                    return new FontInfo(filename);
                else
                    return new FontInfo(filename, encoding);
            }
            catch(IOException ioe)
            {
                throw new JsonParseException(ioe);
            }
            catch(DocumentException de)
            {
                throw new JsonParseException(de);
            }
        }

        public JsonElement serialize(FontInfo fontInfo, Type typeOfId, JsonSerializationContext context)
        {
            return new JsonPrimitive(fontInfo.filename);
        }

    }

    public static GsonBuilder prepare(GsonBuilder gsonBuilder)
    {
        return gsonBuilder.registerTypeAdapter(FontInfo.class, new FontInfoIO());
    }

    private String name;

    private FontInfo regular;
    private FontInfo bold;
    private FontInfo italic;
    private FontInfo boldItalic;

    private FontFamily()
    {
    }

    public String getName()
    {
        return name;
    }
    
    private void validateFontInfo(FontInfo fi, String fiName) {
        if (fi == null) {
            throw new RuntimeException(String.format("Invalid \"%s\" font settings in \"%s\" family", fiName, name));
        }
    }

    public BaseFont getRegularFont()
    {
        validateFontInfo(regular, "regular");
        return regular.font;
    }

    public BaseFont getBoldFont()
    {
        validateFontInfo(bold, "bold");
        return bold.font;
    }

    public BaseFont getItalicFont()
    {
        validateFontInfo(italic, "italic");
        return italic.font;
    }

    public BaseFont getBoldItalicFont()
    {
        validateFontInfo(boldItalic, "boldItalic");
        return boldItalic.font;
    }
}
