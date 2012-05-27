package org.trivee.fb2pdf;

import com.google.gson.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;

public class ParagraphStyle {

    public static final class FontStyleInfo {

        private String style;
        private boolean fontBold;
        private boolean fontItalic;

        public FontStyleInfo(String style)
                throws FB2toPDFException {
            this.style = style;
            if (style.equalsIgnoreCase("regular")) {
                fontBold = false;
                fontItalic = false;
            } else if (style.equalsIgnoreCase("bold")) {
                fontBold = true;
                fontItalic = false;
            } else if (style.equalsIgnoreCase("italic")) {
                fontBold = false;
                fontItalic = true;
            } else if (style.equalsIgnoreCase("bolditalic")) {
                fontBold = true;
                fontItalic = true;
            } else {
                throw new FB2toPDFException("Invalid style '" + style + "'");
            }
        }

        public boolean isFontBold() {
            return fontBold;
        }

        public boolean isFontItalic() {
            return fontItalic;
        }
    };

    private static final class FontStyleInfoIO
            implements JsonDeserializer<FontStyleInfo>, JsonSerializer<FontStyleInfo> {

        @Override
        public FontStyleInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return new FontStyleInfo(json.getAsString());
            } catch (FB2toPDFException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(FontStyleInfo info, Type typeOfId, JsonSerializationContext context) {
            return new JsonPrimitive(info.style);
        }
    }

    private static final class AlignmentInfo {

        private String alignment;
        private int alignmentValue;

        public AlignmentInfo(String alignment)
                throws FB2toPDFException {
            this.alignment = alignment;
            if (alignment.equalsIgnoreCase("left")) {
                this.alignmentValue = Paragraph.ALIGN_LEFT;
            } else if (alignment.equalsIgnoreCase("center")) {
                this.alignmentValue = Paragraph.ALIGN_CENTER;
            } else if (alignment.equalsIgnoreCase("right")) {
                this.alignmentValue = Paragraph.ALIGN_RIGHT;
            } else if (alignment.equalsIgnoreCase("justified")) {
                this.alignmentValue = Paragraph.ALIGN_JUSTIFIED;
            } else {
                throw new FB2toPDFException("Invalid alignment '" + alignment + "'");
            }
        }

        public int getAlignmentValue() {
            return alignmentValue;
        }
    };

    private static final class AlignmentInfoIO
            implements JsonDeserializer<AlignmentInfo>, JsonSerializer<AlignmentInfo> {

        @Override
        public AlignmentInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return new AlignmentInfo(json.getAsString());
            } catch (FB2toPDFException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(AlignmentInfo info, Type typeOfId, JsonSerializationContext context) {
            return new JsonPrimitive(info.alignment);
        }
    }

    public static GsonBuilder prepare(GsonBuilder gsonBuilder) {
        return gsonBuilder.registerTypeAdapter(FontStyleInfo.class, new FontStyleInfoIO()).registerTypeAdapter(AlignmentInfo.class, new AlignmentInfoIO());
    }
    private transient Stylesheet stylesheet;
    private String name;
    private String baseStyle;
    public transient ParagraphStyle containerStyle;
    private String selector;
    private String fontFamily;
    private FontStyleInfo fontStyle;
    private transient boolean boldToggle;
    private transient boolean italicToggle;
    private transient boolean strikethroughToggle;
    private transient boolean halfSizeToggle;
    private Dimension fontSize;
    private Dimension leading;
    private AlignmentInfo alignment;
    private Dimension spacingBefore;
    private Dimension firstSpacingBefore;
    private Dimension spacingAfter;
    private Dimension lastSpacingAfter;
    private Dimension leftIndent;
    private Dimension rightIndent;
    private Dimension firstLineIndent;
    private Dimension firstFirstLineIndent;
    private Boolean disableHyphenation;
    private Boolean preserveWhitespaces;
    private String dropcapStyle;
    private String color;
    private String text;
    private Float inlineImageOffsetY;
    private Float inlineImageZoom;
    private Float strokeWidth;
    private Float characterSpacing;
    private Float horizontalScaling;

    public ParagraphStyle() {
    }

    public void setStylesheet(Stylesheet stylesheet) {
        this.stylesheet = stylesheet;
    }

    public Stylesheet getStylesheet() {
        return stylesheet;
    }

    public String getName() {
        return name;
    }
    
    public String getSelector(){
        return selector;
    }
    
    public BaseFont getBaseFont() throws FB2toPDFException {
        FontFamily ff = getFontFamily();
        FontStyleInfo fs = getFontStyle();
        boolean bold = fs.isFontBold();
        if (boldToggle) {
            bold = !bold;
        }
        boolean italic = fs.isFontItalic();
        if (italicToggle) {
            italic = !italic;
        }
        BaseFont bf = italic ? (bold ? ff.getBoldItalicFont() : ff.getItalicFont()) : (bold ? ff.getBoldFont() : ff.getRegularFont());
        return bf;
    }

    private ParagraphStyle getBaseStyle()
            throws FB2toPDFException {
        if (baseStyle == null) {
            return null;
        }

        if (stylesheet == null) {
            throw new FB2toPDFException("Stylesheet not set.");
        }

        return stylesheet.getParagraphStyle(baseStyle);
    }

    public FontFamily getFontFamily()
            throws FB2toPDFException {
        if (stylesheet == null) {
            throw new FB2toPDFException("Stylesheet not set.");
        }
        
        FontFamily result = StringUtils.isNotBlank(fontFamily) ? stylesheet.getFontFamily(fontFamily) : null;
        
        result = getProperty(result, "getFontFamily", null);

        if(result == null) {         
            throw new FB2toPDFException("Font family for style " + name + " not defined.");
        }
        
        return result;
    }

    public FontStyleInfo getFontStyle()
            throws FB2toPDFException {
        return getProperty(fontStyle, "getFontStyle", new FontStyleInfo("regular"));
    }

    public Dimension getFontSizeDimension() throws FB2toPDFException {
        
        boolean isRelative = fontSize != null && fontSize.isRelative();
        
        Dimension result = getProperty(isRelative ? null : fontSize, "getFontSizeDimension", null);
        
        if (result == null) {
            throw new FB2toPDFException("Font size for style " + name + " not defined.");
        }
        
        return isRelative ? new Dimension(fontSize.getPoints(result.getPoints()) + "pt") : result;
    }
    
    public float getFontSize() throws FB2toPDFException {
        return getFontSizeDimension().getPoints();
    }

    private <T> T getPropertyFromStyle(String getterName, ParagraphStyle style) throws FB2toPDFException {
        try {
            Class<? extends ParagraphStyle> styleClass = this.getClass();
            Method getter = styleClass.getMethod(getterName);
            return (T)getter.invoke(style);
        } catch (Exception ex) {
            throw new FB2toPDFException(ex.toString());
        }
    }

    private <T> T getProperty(T fieldValue, String getterName, T defaultValue) throws FB2toPDFException {
        if (fieldValue != null) {
            return fieldValue;
        }

        ParagraphStyle baseStyle = getBaseStyle();
        if (baseStyle != null) {
            return (T)getPropertyFromStyle(getterName, baseStyle);
        }

        if (containerStyle != null && containerStyle != this) {
            return (T)getPropertyFromStyle(getterName, containerStyle);
        }

        return defaultValue;
    }

    public boolean getDisableHyphenation() throws FB2toPDFException {
        return getProperty(disableHyphenation, "getDisableHyphenation", false);
    }

    public boolean getPreserveWhitespaces() throws FB2toPDFException {
        return getProperty(preserveWhitespaces, "getPreserveWhitespaces", false);
    }

    public String getDropcapStyle() throws FB2toPDFException {
        return getProperty(dropcapStyle, "getDropcapStyle", "");
    }

    public BaseColor getColor() throws FB2toPDFException {
        BaseColor result = (StringUtils.isNotBlank(color)) ? Utilities.getColor(color) : null;
        return getProperty(result, "getColor", Utilities.getColor("0x000000"));
    }

    public float getInlineImageOffsetY() throws FB2toPDFException {
        return getProperty(inlineImageOffsetY, "getInlineImageOffsetY", 0.0f);
    }

    public float getInlineImageZoom() throws FB2toPDFException {
        return getProperty(inlineImageZoom, "getInlineImageZoom", 1.0f);
    }
    
    public float getStrokeWidth() throws FB2toPDFException {
        return getProperty(strokeWidth, "getStrokeWidth", 0.0f);
    }

    public float getCharacterSpacing() throws FB2toPDFException {
        return getProperty(characterSpacing, "getCharacterSpacing", 0.0f);
    }

    public float getHorizontalScaling() throws FB2toPDFException {
        return getProperty(horizontalScaling, "getHorizontalScaling", 1.0f);
    }

    public void toggleBold() {
        boldToggle = !boldToggle;
    }

    public void toggleItalic() {
        italicToggle = !italicToggle;
    }

    void toggleStrikethrough() {
        strikethroughToggle = !strikethroughToggle;
    }

    void toggleHalfSize() {
        halfSizeToggle = !halfSizeToggle;
    }

    public Font getFont()
            throws FB2toPDFException {

        BaseFont bf = getBaseFont();
        float points = getFontSize();
        if (halfSizeToggle) {
            points = points / 2;
        }
        final Font font = new Font(bf, points);
        if (strikethroughToggle) {
            font.setStyle(Font.STRIKETHRU);
        }
        font.setColor(getColor());
        return font;
    }

    public Font getTinyFont()
            throws FB2toPDFException {
        FontFamily ff = getFontFamily();
        BaseFont bf = ff.getRegularFont();

        return new Font(bf, 0.01f);
    }

    public Dimension getLeadingDimension()
            throws FB2toPDFException {
        return getProperty(leading, "getLeadingDimension", new Dimension("1em"));
    }

    public float getAbsoluteLeading()
            throws FB2toPDFException {
        return getLeadingDimension().getPoints(getFontSize());
    }

    public float getRelativeLeading()
            throws FB2toPDFException {
        return 0.0f;
    }

    public int getAlignment()
            throws FB2toPDFException {
        Integer result = (alignment != null) ? alignment.getAlignmentValue() : null;
        return getProperty(result, "getAlignment", Paragraph.ALIGN_LEFT);
    }

    public Dimension getSpacingBeforeDimension()
            throws FB2toPDFException {
        return getProperty(spacingBefore, "getSpacingBeforeDimension", new Dimension("0pt"));
    }

    public float getSpacingBefore()
            throws FB2toPDFException {
        return getSpacingBeforeDimension().getPoints(getFontSize());
    }

    public Dimension getSpacingAfterDimension()
            throws FB2toPDFException {
        return getProperty(spacingAfter, "getSpacingAfterDimension", new Dimension("0pt"));
    }

    public Dimension getFirstSpacingBeforeDimension()
            throws FB2toPDFException {
        return getProperty(firstSpacingBefore, "getFirstSpacingBeforeDimension", getSpacingBeforeDimension());
    }

    public float getFirstSpacingBefore()
            throws FB2toPDFException {
        return getFirstSpacingBeforeDimension().getPoints(getFontSize());
    }

    public float getSpacingAfter()
            throws FB2toPDFException {
        return getSpacingAfterDimension().getPoints(getFontSize());
    }

    public Dimension getLastSpacingAfterDimension()
            throws FB2toPDFException {
        return getProperty(lastSpacingAfter, "getLastSpacingAfterDimension", getSpacingAfterDimension());
    }

    public float getLastSpacingAfter()
            throws FB2toPDFException {
        return getLastSpacingAfterDimension().getPoints(getFontSize());
    }

    public Dimension getLeftIndentDimension()
            throws FB2toPDFException {
        return getProperty(leftIndent, "getLeftIndentDimension", new Dimension("0pt"));
    }

    public Dimension getRightIndentDimension()
            throws FB2toPDFException {
        return getProperty(rightIndent, "getRightIndentDimension", new Dimension("0pt"));
    }

    public float getLeftIndent()
            throws FB2toPDFException {
        return getLeftIndentDimension().getPoints(getFontSize());
    }

    public float getRightIndent()
            throws FB2toPDFException {
        return getRightIndentDimension().getPoints(getFontSize());
    }

    public Dimension getFirstLineIndentDimension()
            throws FB2toPDFException {
        return getProperty(firstLineIndent, "getFirstLineIndentDimension", new Dimension("0pt"));
    }

    public Dimension getFirstFirstLineIndentDimension()
            throws FB2toPDFException {
        return getProperty(firstFirstLineIndent, "getFirstFirstLineIndentDimension", new Dimension("0pt"));
    }

    public float getFirstLineIndent()
            throws FB2toPDFException {
        return getFirstLineIndentDimension().getPoints(getFontSize());
    }

    public float getFirstFirstLineIndent()
            throws FB2toPDFException {
        return getFirstFirstLineIndentDimension().getPoints(getFontSize());
    }

    public String getText() {
        return text;
    }

    public Chunk createChunk()
            throws FB2toPDFException {
        Chunk chunk = new Chunk();
        chunk.setFont(getFont());
        float strokeW = getStrokeWidth();
        if (strokeW > 0) {
            chunk.setTextRenderMode(PdfContentByte.TEXT_RENDER_MODE_FILL_STROKE, strokeW, getColor());
        }
        
        float cs = getCharacterSpacing();
        if (cs != 0.0f) chunk.setCharacterSpacing(cs);
        float hs = getHorizontalScaling();
        if (hs != 1.0f) chunk.setHorizontalScaling(hs);
        return chunk;
    }

    public Anchor createAnchor()
            throws FB2toPDFException {
        return new Anchor(getAbsoluteLeading());
    }

    public Anchor createInvisibleAnchor()
            throws FB2toPDFException {
        Chunk chunk = new Chunk();
        chunk.setFont(getTinyFont());
        chunk.append(".");
        Anchor anchor = new Anchor(0.01f);
        anchor.add(chunk);
        return anchor;
    }

    public Paragraph createParagraph()
            throws FB2toPDFException {
        Paragraph para = new Paragraph();
        para.setLeading(getAbsoluteLeading(), getRelativeLeading());
        para.setAlignment(getAlignment());
        para.setSpacingBefore(getSpacingBefore());
        para.setSpacingAfter(getSpacingAfter());
        para.setIndentationLeft(getLeftIndent());
        para.setIndentationRight(getRightIndent());
        para.setFirstLineIndent(getFirstLineIndent());
        para.setFont(getFont());
        return para;
    }

    public Paragraph createParagraph(boolean bFirst, boolean bLast)
            throws FB2toPDFException {
        Paragraph para = createParagraph();

        if (bFirst) {
            para.setSpacingBefore(getFirstSpacingBefore());
            para.setFirstLineIndent(getFirstFirstLineIndent());
        }
        if (bLast) {
            para.setSpacingAfter(getLastSpacingAfter());
        }

        return para;
    }
}
