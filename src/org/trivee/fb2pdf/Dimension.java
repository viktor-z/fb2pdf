package org.trivee.fb2pdf;

import java.lang.reflect.Type;
import com.google.gson.*;

public class Dimension
{
    private static final String PCT = "%";
    private static final String EM = "em";
    private static final String PT = "pt";
    private static final String MM = "mm";

    private static final float MM_TO_POINTS = 72.0f / 25.4f;

    private String  dimension;
    private boolean relative;
    private float   points;

    public Dimension()
    {
        dimension = "0pt";
        relative = false;
        points = 0.0f;
    }

    public Dimension(String dimension)
        throws FB2toPDFException
    {
        setDimension(dimension);
    }
    
    private float parseDimension(String suffix) {
        return Float.parseFloat(dimension.substring(0, dimension.length()-suffix.length()));
    }

    public void setDimension(String dimension)
        throws FB2toPDFException
    {
        this.dimension = dimension;
        if (dimension.endsWith(PT))
        {
            relative = false;
            points = parseDimension(PT);
        }
        else if (dimension.endsWith(MM))
        {
            relative = false;
            points = MM_TO_POINTS * parseDimension(MM);
        }
        else if (dimension.endsWith(EM))
        {
            relative = true;
            points = 12.0f * parseDimension(EM);
        }
        else if (dimension.endsWith(PCT))
        {
           relative = true;
           points = 12.0f * parseDimension(PCT) / 100;
        }
        else
        {
            throw new FB2toPDFException("Dimension format '" + dimension + "' not recognized.");
        }
    }

    public void setDimension(float points) {
        this.points = points;
    }

    public String getDimension()
    {
        return dimension;
    }

    public boolean isRelative()
    {
        return relative;
    }

    public float getPoints()
    {
        return points;
    }

    public float getPoints(float fontSize)
    {
        if (isRelative())
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
