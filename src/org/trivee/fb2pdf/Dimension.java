package org.trivee.fb2pdf;

import java.lang.reflect.Type;
import com.google.gson.*;

public class Dimension
{
    private static final String EM = "em";
    private static final String PT = "pt";
    private static final String MM = "mm";

    private static final float MM_TO_POINTS = 72.0f / 25.4f;

    private String  dimension;
    private float   points;
    private boolean relative;

    public Dimension()
    {
        dimension = "0pt";
        points = 0.0f;
        relative = false;
    }

    public Dimension(String dimension)
        throws FB2toPDFException
    {
        setDimension(dimension);
    }

    public void setDimension(String dimension)
        throws FB2toPDFException
    {
        this.dimension = dimension;
        if (dimension.endsWith(PT))
        {
            points = Float.parseFloat(dimension.substring(0, dimension.length()-2));
        }
        else if (dimension.endsWith(MM))
        {
            points = MM_TO_POINTS * Float.parseFloat(dimension.substring(0, dimension.length()-2));
            relative = false;
        }
        else if (dimension.endsWith(EM))
        {
            points = 12.0f * Float.parseFloat(dimension.substring(0, dimension.length()-2));
            relative = true;
        }
        else
        {
            throw new FB2toPDFException("Dimension format '" + dimension + "' not recognized.");
        }
    }

    public String getDimension()
    {
        return dimension;
    }

    public float getPoints()
    {
        return points;
    }

    public float getPoints(float fontSize)
    {
        if (relative)
            return points * fontSize / 12.0f;
        else
            return points;
    }

    private static final class DimensionIO
        implements JsonDeserializer<Dimension>,JsonSerializer<Dimension>
    {
        public Dimension deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            try
            {
                return new Dimension(json.getAsString());
            }
            catch(FB2toPDFException e)
            {
                throw new JsonParseException("Invalid dimension", e);
            }
        }

        public JsonElement serialize(Dimension dimension, Type typeOfId, JsonSerializationContext context)
        {
            return new JsonPrimitive(dimension.getDimension());
        }
    }

    public static GsonBuilder prepare(GsonBuilder gsonBuilder)
    {
        return gsonBuilder.registerTypeAdapter(Dimension.class, new DimensionIO());
    }

    public static void main(String[] args)
    {
        try
        {
            System.out.println("20pt = " + new Dimension("20pt").getPoints() + " points");
            System.out.println("20mm = " + new Dimension("20mm").getPoints() + " points");
            System.out.println("20em @ 10pt = " + new Dimension("20pt").getPoints(10.0f) + " points");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
