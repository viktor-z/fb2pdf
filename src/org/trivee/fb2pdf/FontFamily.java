package org.trivee.fb2pdf;

import java.io.IOException;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

import java.lang.reflect.Type;
import com.google.gson.*;

public class FontFamily
{
    private static final class FontInfo
    {
        private String   filename;
        private BaseFont font;

        private FontInfo(String filename)
            throws DocumentException, IOException
        {
            this.filename = filename;
            this.font = BaseFont.createFont(filename, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
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
                return new FontInfo(json.getAsString());
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

    public BaseFont getRegularFont()
    {
        return regular.font;
    }

    public BaseFont getBoldFont()
    {
        return bold.font;
    }

    public BaseFont getItalicFont()
    {
        return italic.font;
    }

    public BaseFont getBoldItalicFont()
    {
        return boldItalic.font;
    }
}
