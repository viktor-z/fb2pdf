/*
 * $Id: PdfContentByte.java 5499 2012-10-26 13:00:19Z achingarev $
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 1998-2012 1T3XT BVBA
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY 1T3XT,
 * 1T3XT DISCLAIMS THE WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.text.pdf;
import com.itextpdf.awt.FontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.awt.PdfPrinterGraphics2D;
import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.text.*;
import com.itextpdf.text.error_messages.MessageLocalization;
import com.itextpdf.text.exceptions.IllegalPdfSyntaxException;
import com.itextpdf.text.pdf.interfaces.IPdfStructureElement;
import com.itextpdf.text.pdf.internal.PdfAnnotationsImp;
import com.itextpdf.text.pdf.internal.PdfIsoKeys;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <CODE>PdfContentByte</CODE> is an object containing the user positioned
 * text and graphic contents of a page. It knows how to apply the proper
 * font encoding.
 */

public class PdfContentByte {

    /**
     * This class keeps the graphic state of the current page
     */

    static class GraphicState {

        /** This is the font in use */
        FontDetails fontDetails;

        /** This is the color in use */
        ColorDetails colorDetails;

        /** This is the font size in use */
        float size;

        /** The x position of the text line matrix. */
        protected float xTLM = 0;
        /** The y position of the text line matrix. */
        protected float yTLM = 0;

        protected float aTLM = 1;
        protected float bTLM = 0;
        protected float cTLM = 0;
        protected float dTLM = 1;

        protected float tx = 0;

        /** The current text leading. */
        protected float leading = 0;

        /** The current horizontal scaling */
        protected float scale = 100;

        /** The current character spacing */
        protected float charSpace = 0;

        /** The current word spacing */
        protected float wordSpace = 0;

        GraphicState() {
        }

        GraphicState(final GraphicState cp) {
            fontDetails = cp.fontDetails;
            colorDetails = cp.colorDetails;
            size = cp.size;
            xTLM = cp.xTLM;
            yTLM = cp.yTLM;
            aTLM = cp.aTLM;
            bTLM = cp.bTLM;
            cTLM = cp.cTLM;
            dTLM = cp.dTLM;
            tx = cp.tx;
            leading = cp.leading;
            scale = cp.scale;
            charSpace = cp.charSpace;
            wordSpace = cp.wordSpace;
        }
    }

    /** The alignment is center */
    public static final int ALIGN_CENTER = Element.ALIGN_CENTER;

    /** The alignment is left */
    public static final int ALIGN_LEFT = Element.ALIGN_LEFT;

    /** The alignment is right */
    public static final int ALIGN_RIGHT = Element.ALIGN_RIGHT;

    /** A possible line cap value */
    public static final int LINE_CAP_BUTT = 0;
    /** A possible line cap value */
    public static final int LINE_CAP_ROUND = 1;
    /** A possible line cap value */
    public static final int LINE_CAP_PROJECTING_SQUARE = 2;

    /** A possible line join value */
    public static final int LINE_JOIN_MITER = 0;
    /** A possible line join value */
    public static final int LINE_JOIN_ROUND = 1;
    /** A possible line join value */
    public static final int LINE_JOIN_BEVEL = 2;

    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_FILL = 0;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_STROKE = 1;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_FILL_STROKE = 2;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_INVISIBLE = 3;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_FILL_CLIP = 4;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_STROKE_CLIP = 5;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_FILL_STROKE_CLIP = 6;
    /** A possible text rendering value */
    public static final int TEXT_RENDER_MODE_CLIP = 7;

    private static final float[] unitRect = {0, 0, 0, 1, 1, 0, 1, 1};
    // membervariables

    /** This is the actual content */
    protected ByteBuffer content = new ByteBuffer();

    /** This is the writer */
    protected PdfWriter writer;

    /** This is the PdfDocument */
    protected PdfDocument pdf;

    /** This is the GraphicState in use */
    protected GraphicState state = new GraphicState();

    /** The list were we save/restore the state */
    protected ArrayList<GraphicState> stateList = new ArrayList<GraphicState>();

    /** The list were we save/restore the layer depth */
    protected ArrayList<Integer> layerDepth;

    /** The separator between commands.
     */
    protected int separator = '\n';

    private int mcDepth = 0;
    private boolean inText = false;

    private static HashMap<PdfName, String> abrev = new HashMap<PdfName, String>();

    private ArrayList<Element> mcElements = new ArrayList<Element>();

    private PdfContentByte duplicatedFrom = null;

    /**
     * Indicates if to open/close text block automatically.
     */
    protected boolean autoControlTextBlocks = false;

    //for development needs only! to be removed once tagged pdf support is complete.
    private boolean allowTaggedImages = false;

    static {
        abrev.put(PdfName.BITSPERCOMPONENT, "/BPC ");
        abrev.put(PdfName.COLORSPACE, "/CS ");
        abrev.put(PdfName.DECODE, "/D ");
        abrev.put(PdfName.DECODEPARMS, "/DP ");
        abrev.put(PdfName.FILTER, "/F ");
        abrev.put(PdfName.HEIGHT, "/H ");
        abrev.put(PdfName.IMAGEMASK, "/IM ");
        abrev.put(PdfName.INTENT, "/Intent ");
        abrev.put(PdfName.INTERPOLATE, "/I ");
        abrev.put(PdfName.WIDTH, "/W ");
    }

    // constructors

    /**
     * Constructs a new <CODE>PdfContentByte</CODE>-object.
     *
     * @param wr the writer associated to this content
     */

    public PdfContentByte(final PdfWriter wr) {
        if (wr != null) {
            writer = wr;
            pdf = writer.getPdfDocument();
            autoControlTextBlocks = !pdf.useSeparateCanvasesForTextAndGraphics;
        }
    }

    // methods to get the content of this object

    /**
     * Returns the <CODE>String</CODE> representation of this <CODE>PdfContentByte</CODE>-object.
     *
     * @return      a <CODE>String</CODE>
     */

    @Override
    public String toString() {
        return content.toString();
    }

    /**
     * Gets the internal buffer.
     * @return the internal buffer
     */
    public ByteBuffer getInternalBuffer() {
        return content;
    }

    /** Returns the PDF representation of this <CODE>PdfContentByte</CODE>-object.
     *
     * @param writer the <CODE>PdfWriter</CODE>
     * @return a <CODE>byte</CODE> array with the representation
     */

    public byte[] toPdf(final PdfWriter writer) {
    	sanityCheck();
        return content.toByteArray();
    }

    // methods to add graphical content

    /**
     * Adds the content of another <CODE>PdfContentByte</CODE>-object to this object.
     *
     * @param       other       another <CODE>PdfByteContent</CODE>-object
     */

    public void add(final PdfContentByte other) {
        if (other.writer != null && writer != other.writer)
            throw new RuntimeException(MessageLocalization.getComposedMessage("inconsistent.writers.are.you.mixing.two.documents"));
        content.append(other.content);
    }

    /**
     * Gets the x position of the text line matrix.
     *
     * @return the x position of the text line matrix
     */
    public float getXTLM() {
        return state.xTLM;
    }

    /**
     * Gets the y position of the text line matrix.
     *
     * @return the y position of the text line matrix
     */
    public float getYTLM() {
        return state.yTLM;
    }

    /**
     * Gets the current text leading.
     *
     * @return the current text leading
     */
    public float getLeading() {
        return state.leading;
    }

    /**
     * Gets the current character spacing.
     *
     * @return the current character spacing
     */
    public float getCharacterSpacing() {
        return state.charSpace;
    }

    /**
     * Gets the current word spacing.
     *
     * @return the current word spacing
     */
    public float getWordSpacing() {
        return state.wordSpace;
    }

    /**
     * Gets the current character spacing.
     *
     * @return the current character spacing
     */
    public float getHorizontalScaling() {
        return state.scale;
    }

    /**
     * Changes the <VAR>Flatness</VAR>.
     * <P>
     * <VAR>Flatness</VAR> sets the maximum permitted distance in device pixels between the
     * mathematically correct path and an approximation constructed from straight line segments.<BR>
     *
     * @param       flatness        a value
     */

    public void setFlatness(final float flatness) {
        if (flatness >= 0 && flatness <= 100) {
            content.append(flatness).append(" i").append_i(separator);
        }
    }

    /**
     * Changes the <VAR>Line cap style</VAR>.
     * <P>
     * The <VAR>line cap style</VAR> specifies the shape to be used at the end of open subpaths
     * when they are stroked.<BR>
     * Allowed values are LINE_CAP_BUTT, LINE_CAP_ROUND and LINE_CAP_PROJECTING_SQUARE.<BR>
     *
     * @param       style       a value
     */

    public void setLineCap(final int style) {
        if (style >= 0 && style <= 2) {
            content.append(style).append(" J").append_i(separator);
        }
    }

    /**
     * Changes the value of the <VAR>line dash pattern</VAR>.
     * <P>
     * The line dash pattern controls the pattern of dashes and gaps used to stroke paths.
     * It is specified by an <I>array</I> and a <I>phase</I>. The array specifies the length
     * of the alternating dashes and gaps. The phase specifies the distance into the dash
     * pattern to start the dash.<BR>
     *
     * @param       phase       the value of the phase
     */

    public void setLineDash(final float phase) {
        content.append("[] ").append(phase).append(" d").append_i(separator);
    }

    /**
     * Changes the value of the <VAR>line dash pattern</VAR>.
     * <P>
     * The line dash pattern controls the pattern of dashes and gaps used to stroke paths.
     * It is specified by an <I>array</I> and a <I>phase</I>. The array specifies the length
     * of the alternating dashes and gaps. The phase specifies the distance into the dash
     * pattern to start the dash.<BR>
     *
     * @param       phase       the value of the phase
     * @param       unitsOn     the number of units that must be 'on' (equals the number of units that must be 'off').
     */

    public void setLineDash(final float unitsOn, final float phase) {
        content.append("[").append(unitsOn).append("] ").append(phase).append(" d").append_i(separator);
    }

    /**
     * Changes the value of the <VAR>line dash pattern</VAR>.
     * <P>
     * The line dash pattern controls the pattern of dashes and gaps used to stroke paths.
     * It is specified by an <I>array</I> and a <I>phase</I>. The array specifies the length
     * of the alternating dashes and gaps. The phase specifies the distance into the dash
     * pattern to start the dash.<BR>
     *
     * @param       phase       the value of the phase
     * @param       unitsOn     the number of units that must be 'on'
     * @param       unitsOff    the number of units that must be 'off'
     */

    public void setLineDash(final float unitsOn, final float unitsOff, final float phase) {
        content.append("[").append(unitsOn).append(' ').append(unitsOff).append("] ").append(phase).append(" d").append_i(separator);
    }

    /**
     * Changes the value of the <VAR>line dash pattern</VAR>.
     * <P>
     * The line dash pattern controls the pattern of dashes and gaps used to stroke paths.
     * It is specified by an <I>array</I> and a <I>phase</I>. The array specifies the length
     * of the alternating dashes and gaps. The phase specifies the distance into the dash
     * pattern to start the dash.<BR>
     *
     * @param       array       length of the alternating dashes and gaps
     * @param       phase       the value of the phase
     */

    public final void setLineDash(final float[] array, final float phase) {
        content.append("[");
        for (int i = 0; i < array.length; i++) {
            content.append(array[i]);
            if (i < array.length - 1) content.append(' ');
        }
        content.append("] ").append(phase).append(" d").append_i(separator);
    }

    /**
     * Changes the <VAR>Line join style</VAR>.
     * <P>
     * The <VAR>line join style</VAR> specifies the shape to be used at the corners of paths
     * that are stroked.<BR>
     * Allowed values are LINE_JOIN_MITER (Miter joins), LINE_JOIN_ROUND (Round joins) and LINE_JOIN_BEVEL (Bevel joins).<BR>
     *
     * @param       style       a value
     */

    public void setLineJoin(final int style) {
        if (style >= 0 && style <= 2) {
            content.append(style).append(" j").append_i(separator);
        }
    }

    /**
     * Changes the <VAR>line width</VAR>.
     * <P>
     * The line width specifies the thickness of the line used to stroke a path and is measured
     * in user space units.<BR>
     *
     * @param       w           a width
     */

    public void setLineWidth(final float w) {
        content.append(w).append(" w").append_i(separator);
    }

    /**
     * Changes the <VAR>Miter limit</VAR>.
     * <P>
     * When two line segments meet at a sharp angle and mitered joins have been specified as the
     * line join style, it is possible for the miter to extend far beyond the thickness of the line
     * stroking path. The miter limit imposes a maximum on the ratio of the miter length to the line
     * witdh. When the limit is exceeded, the join is converted from a miter to a bevel.<BR>
     *
     * @param       miterLimit      a miter limit
     */

    public void setMiterLimit(final float miterLimit) {
        if (miterLimit > 1) {
            content.append(miterLimit).append(" M").append_i(separator);
        }
    }

    /**
     * Modify the current clipping path by intersecting it with the current path, using the
     * nonzero winding number rule to determine which regions lie inside the clipping
     * path.
     */

    public void clip() {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        content.append("W").append_i(separator);
    }

    /**
     * Modify the current clipping path by intersecting it with the current path, using the
     * even-odd rule to determine which regions lie inside the clipping path.
     */

    public void eoClip() {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        content.append("W*").append_i(separator);
    }

    /**
     * Changes the currentgray tint for filling paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceGray</B> (or the <B>DefaultGray</B> color space),
     * and sets the gray tint to use for filling paths.</P>
     *
     * @param   gray    a value between 0 (black) and 1 (white)
     */

    public void setGrayFill(final float gray) {
        content.append(gray).append(" g").append_i(separator);
    }

    /**
     * Changes the current gray tint for filling paths to black.
     */

    public void resetGrayFill() {
        content.append("0 g").append_i(separator);
    }

    /**
     * Changes the currentgray tint for stroking paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceGray</B> (or the <B>DefaultGray</B> color space),
     * and sets the gray tint to use for stroking paths.</P>
     *
     * @param   gray    a value between 0 (black) and 1 (white)
     */

    public void setGrayStroke(final float gray) {
        content.append(gray).append(" G").append_i(separator);
    }

    /**
     * Changes the current gray tint for stroking paths to black.
     */

    public void resetGrayStroke() {
        content.append("0 G").append_i(separator);
    }

    /**
     * Helper to validate and write the RGB color components
     * @param   red     the intensity of red. A value between 0 and 1
     * @param   green   the intensity of green. A value between 0 and 1
     * @param   blue    the intensity of blue. A value between 0 and 1
     */
    private void HelperRGB(float red, float green, float blue) {
    	PdfWriter.checkPdfIsoConformance(writer, PdfIsoKeys.PDFISOKEY_RGB, null);
        if (red < 0)
            red = 0.0f;
        else if (red > 1.0f)
            red = 1.0f;
        if (green < 0)
            green = 0.0f;
        else if (green > 1.0f)
            green = 1.0f;
        if (blue < 0)
            blue = 0.0f;
        else if (blue > 1.0f)
            blue = 1.0f;
        content.append(red).append(' ').append(green).append(' ').append(blue);
    }

    /**
     * Changes the current color for filling paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceRGB</B> (or the <B>DefaultRGB</B> color space),
     * and sets the color to use for filling paths.</P>
     * <P>
     * Following the PDF manual, each operand must be a number between 0 (minimum intensity) and
     * 1 (maximum intensity).</P>
     *
     * @param   red     the intensity of red. A value between 0 and 1
     * @param   green   the intensity of green. A value between 0 and 1
     * @param   blue    the intensity of blue. A value between 0 and 1
     */

    public void setRGBColorFillF(final float red, final float green, final float blue) {
        HelperRGB(red, green, blue);
        content.append(" rg").append_i(separator);
    }

    /**
     * Changes the current color for filling paths to black.
     */

    public void resetRGBColorFill() {
        content.append("0 g").append_i(separator);
    }

    /**
     * Changes the current color for stroking paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceRGB</B> (or the <B>DefaultRGB</B> color space),
     * and sets the color to use for stroking paths.</P>
     * <P>
     * Following the PDF manual, each operand must be a number between 0 (miniumum intensity) and
     * 1 (maximum intensity).
     *
     * @param   red     the intensity of red. A value between 0 and 1
     * @param   green   the intensity of green. A value between 0 and 1
     * @param   blue    the intensity of blue. A value between 0 and 1
     */

    public void setRGBColorStrokeF(final float red, final float green, final float blue) {
        HelperRGB(red, green, blue);
        content.append(" RG").append_i(separator);
    }

    /**
     * Changes the current color for stroking paths to black.
     *
     */

    public void resetRGBColorStroke() {
        content.append("0 G").append_i(separator);
    }

    /**
     * Helper to validate and write the CMYK color components.
     *
     * @param   cyan    the intensity of cyan. A value between 0 and 1
     * @param   magenta the intensity of magenta. A value between 0 and 1
     * @param   yellow  the intensity of yellow. A value between 0 and 1
     * @param   black   the intensity of black. A value between 0 and 1
     */
    private void HelperCMYK(float cyan, float magenta, float yellow, float black) {
        if (cyan < 0)
            cyan = 0.0f;
        else if (cyan > 1.0f)
            cyan = 1.0f;
        if (magenta < 0)
            magenta = 0.0f;
        else if (magenta > 1.0f)
            magenta = 1.0f;
        if (yellow < 0)
            yellow = 0.0f;
        else if (yellow > 1.0f)
            yellow = 1.0f;
        if (black < 0)
            black = 0.0f;
        else if (black > 1.0f)
            black = 1.0f;
        content.append(cyan).append(' ').append(magenta).append(' ').append(yellow).append(' ').append(black);
    }

    /**
     * Changes the current color for filling paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceCMYK</B> (or the <B>DefaultCMYK</B> color space),
     * and sets the color to use for filling paths.</P>
     * <P>
     * Following the PDF manual, each operand must be a number between 0 (no ink) and
     * 1 (maximum ink).</P>
     *
     * @param   cyan    the intensity of cyan. A value between 0 and 1
     * @param   magenta the intensity of magenta. A value between 0 and 1
     * @param   yellow  the intensity of yellow. A value between 0 and 1
     * @param   black   the intensity of black. A value between 0 and 1
     */

    public void setCMYKColorFillF(final float cyan, final float magenta, final float yellow, final float black) {
        HelperCMYK(cyan, magenta, yellow, black);
        content.append(" k").append_i(separator);
    }

    /**
     * Changes the current color for filling paths to black.
     *
     */

    public void resetCMYKColorFill() {
        content.append("0 0 0 1 k").append_i(separator);
    }

    /**
     * Changes the current color for stroking paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceCMYK</B> (or the <B>DefaultCMYK</B> color space),
     * and sets the color to use for stroking paths.</P>
     * <P>
     * Following the PDF manual, each operand must be a number between 0 (miniumum intensity) and
     * 1 (maximum intensity).
     *
     * @param   cyan    the intensity of cyan. A value between 0 and 1
     * @param   magenta the intensity of magenta. A value between 0 and 1
     * @param   yellow  the intensity of yellow. A value between 0 and 1
     * @param   black   the intensity of black. A value between 0 and 1
     */

    public void setCMYKColorStrokeF(final float cyan, final float magenta, final float yellow, final float black) {
        HelperCMYK(cyan, magenta, yellow, black);
        content.append(" K").append_i(separator);
    }

    /**
     * Changes the current color for stroking paths to black.
     *
     */

    public void resetCMYKColorStroke() {
        content.append("0 0 0 1 K").append_i(separator);
    }

    /**
     * Move the current point <I>(x, y)</I>, omitting any connecting line segment.
     *
     * @param       x               new x-coordinate
     * @param       y               new y-coordinate
     */

    public void moveTo(final float x, final float y) {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append(x).append(' ').append(y).append(" m").append_i(separator);
    }

    /**
     * Appends a straight line segment from the current point <I>(x, y)</I>. The new current
     * point is <I>(x, y)</I>.
     *
     * @param       x               new x-coordinate
     * @param       y               new y-coordinate
     */

    public void lineTo(final float x, final float y) {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append(x).append(' ').append(y).append(" l").append_i(separator);
    }

    /**
     * Appends a B&#xea;zier curve to the path, starting from the current point.
     *
     * @param       x1      x-coordinate of the first control point
     * @param       y1      y-coordinate of the first control point
     * @param       x2      x-coordinate of the second control point
     * @param       y2      y-coordinate of the second control point
     * @param       x3      x-coordinate of the ending point (= new current point)
     * @param       y3      y-coordinate of the ending point (= new current point)
     */

    public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append(x1).append(' ').append(y1).append(' ').append(x2).append(' ').append(y2).append(' ').append(x3).append(' ').append(y3).append(" c").append_i(separator);
    }

    /**
     * Appends a B&#xea;zier curve to the path, starting from the current point.
     *
     * @param       x2      x-coordinate of the second control point
     * @param       y2      y-coordinate of the second control point
     * @param       x3      x-coordinate of the ending point (= new current point)
     * @param       y3      y-coordinate of the ending point (= new current point)
     */

    public void curveTo(final float x2, final float y2, final float x3, final float y3) {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append(x2).append(' ').append(y2).append(' ').append(x3).append(' ').append(y3).append(" v").append_i(separator);
    }

    /**
     * Appends a B&#xea;zier curve to the path, starting from the current point.
     *
     * @param       x1      x-coordinate of the first control point
     * @param       y1      y-coordinate of the first control point
     * @param       x3      x-coordinate of the ending point (= new current point)
     * @param       y3      y-coordinate of the ending point (= new current point)
     */

    public void curveFromTo(final float x1, final float y1, final float x3, final float y3) {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append(x1).append(' ').append(y1).append(' ').append(x3).append(' ').append(y3).append(" y").append_i(separator);
    }

    /** Draws a circle. The endpoint will (x+r, y).
     *
     * @param x x center of circle
     * @param y y center of circle
     * @param r radius of circle
     */
    public void circle(final float x, final float y, final float r) {
        float b = 0.5523f;
        moveTo(x + r, y);
        curveTo(x + r, y + r * b, x + r * b, y + r, x, y + r);
        curveTo(x - r * b, y + r, x - r, y + r * b, x - r, y);
        curveTo(x - r, y - r * b, x - r * b, y - r, x, y - r);
        curveTo(x + r * b, y - r, x + r, y - r * b, x + r, y);
    }



    /**
     * Adds a rectangle to the current path.
     *
     * @param       x       x-coordinate of the starting point
     * @param       y       y-coordinate of the starting point
     * @param       w       width
     * @param       h       height
     */

    public void rectangle(final float x, final float y, final float w, final float h) {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re").append_i(separator);
    }

    private boolean compareColors(final BaseColor c1, final BaseColor c2) {
        if (c1 == null && c2 == null)
            return true;
        if (c1 == null || c2 == null)
            return false;
        if (c1 instanceof ExtendedColor)
            return c1.equals(c2);
        return c2.equals(c1);
    }

    /**
     * Adds a variable width border to the current path.
     * Only use if {@link com.itextpdf.text.Rectangle#isUseVariableBorders() Rectangle.isUseVariableBorders}
     * = true.
     * @param rect a <CODE>Rectangle</CODE>
     */
    public void variableRectangle(final Rectangle rect) {
        float t = rect.getTop();
        float b = rect.getBottom();
        float r = rect.getRight();
        float l = rect.getLeft();
        float wt = rect.getBorderWidthTop();
        float wb = rect.getBorderWidthBottom();
        float wr = rect.getBorderWidthRight();
        float wl = rect.getBorderWidthLeft();
        BaseColor ct = rect.getBorderColorTop();
        BaseColor cb = rect.getBorderColorBottom();
        BaseColor cr = rect.getBorderColorRight();
        BaseColor cl = rect.getBorderColorLeft();
        saveState();
        setLineCap(PdfContentByte.LINE_CAP_BUTT);
        setLineJoin(PdfContentByte.LINE_JOIN_MITER);
        float clw = 0;
        boolean cdef = false;
        BaseColor ccol = null;
        boolean cdefi = false;
        BaseColor cfil = null;
        // draw top
        if (wt > 0) {
            setLineWidth(clw = wt);
            cdef = true;
            if (ct == null)
                resetRGBColorStroke();
            else
                setColorStroke(ct);
            ccol = ct;
            moveTo(l, t - wt / 2f);
            lineTo(r, t - wt / 2f);
            stroke();
        }

        // Draw bottom
        if (wb > 0) {
            if (wb != clw)
                setLineWidth(clw = wb);
            if (!cdef || !compareColors(ccol, cb)) {
                cdef = true;
                if (cb == null)
                    resetRGBColorStroke();
                else
                    setColorStroke(cb);
                ccol = cb;
            }
            moveTo(r, b + wb / 2f);
            lineTo(l, b + wb / 2f);
            stroke();
        }

        // Draw right
        if (wr > 0) {
            if (wr != clw)
                setLineWidth(clw = wr);
            if (!cdef || !compareColors(ccol, cr)) {
                cdef = true;
                if (cr == null)
                    resetRGBColorStroke();
                else
                    setColorStroke(cr);
                ccol = cr;
            }
            boolean bt = compareColors(ct, cr);
            boolean bb = compareColors(cb, cr);
            moveTo(r - wr / 2f, bt ? t : t - wt);
            lineTo(r - wr / 2f, bb ? b : b + wb);
            stroke();
            if (!bt || !bb) {
                cdefi = true;
                if (cr == null)
                    resetRGBColorFill();
                else
                    setColorFill(cr);
                cfil = cr;
                if (!bt) {
                    moveTo(r, t);
                    lineTo(r, t - wt);
                    lineTo(r - wr, t - wt);
                    fill();
                }
                if (!bb) {
                    moveTo(r, b);
                    lineTo(r, b + wb);
                    lineTo(r - wr, b + wb);
                    fill();
                }
            }
        }

        // Draw Left
        if (wl > 0) {
            if (wl != clw)
                setLineWidth(wl);
            if (!cdef || !compareColors(ccol, cl)) {
                if (cl == null)
                    resetRGBColorStroke();
                else
                    setColorStroke(cl);
            }
            boolean bt = compareColors(ct, cl);
            boolean bb = compareColors(cb, cl);
            moveTo(l + wl / 2f, bt ? t : t - wt);
            lineTo(l + wl / 2f, bb ? b : b + wb);
            stroke();
            if (!bt || !bb) {
                if (!cdefi || !compareColors(cfil, cl)) {
                    if (cl == null)
                        resetRGBColorFill();
                    else
                        setColorFill(cl);
                }
                if (!bt) {
                    moveTo(l, t);
                    lineTo(l, t - wt);
                    lineTo(l + wl, t - wt);
                    fill();
                }
                if (!bb) {
                    moveTo(l, b);
                    lineTo(l, b + wb);
                    lineTo(l + wl, b + wb);
                    fill();
                }
            }
        }
        restoreState();
    }

    /**
     * Adds a border (complete or partially) to the current path..
     *
     * @param       rectangle       a <CODE>Rectangle</CODE>
     */

    public void rectangle(final Rectangle rectangle) {
        // the coordinates of the border are retrieved
        float x1 = rectangle.getLeft();
        float y1 = rectangle.getBottom();
        float x2 = rectangle.getRight();
        float y2 = rectangle.getTop();

        // the backgroundcolor is set
        BaseColor background = rectangle.getBackgroundColor();
        if (background != null) {
        	saveState();
            setColorFill(background);
            rectangle(x1, y1, x2 - x1, y2 - y1);
            fill();
            restoreState();
        }

        // if the element hasn't got any borders, nothing is added
        if (! rectangle.hasBorders()) {
            return;
        }

        // if any of the individual border colors are set
        // we draw the borders all around using the
        // different colors
        if (rectangle.isUseVariableBorders()) {
            variableRectangle(rectangle);
        }
        else {
            // the width is set to the width of the element
            if (rectangle.getBorderWidth() != Rectangle.UNDEFINED) {
                setLineWidth(rectangle.getBorderWidth());
            }

            // the color is set to the color of the element
            BaseColor color = rectangle.getBorderColor();
            if (color != null) {
                setColorStroke(color);
            }

            // if the box is a rectangle, it is added as a rectangle
            if (rectangle.hasBorder(Rectangle.BOX)) {
               rectangle(x1, y1, x2 - x1, y2 - y1);
            }
            // if the border isn't a rectangle, the different sides are added apart
            else {
                if (rectangle.hasBorder(Rectangle.RIGHT)) {
                    moveTo(x2, y1);
                    lineTo(x2, y2);
                }
                if (rectangle.hasBorder(Rectangle.LEFT)) {
                    moveTo(x1, y1);
                    lineTo(x1, y2);
                }
                if (rectangle.hasBorder(Rectangle.BOTTOM)) {
                    moveTo(x1, y1);
                    lineTo(x2, y1);
                }
                if (rectangle.hasBorder(Rectangle.TOP)) {
                    moveTo(x1, y2);
                    lineTo(x2, y2);
                }
            }

            stroke();

            if (color != null) {
                resetRGBColorStroke();
            }
        }
    }

    /**
     * Closes the current subpath by appending a straight line segment from the current point
     * to the starting point of the subpath.
     */

    public void closePath() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("h").append_i(separator);
    }

    /**
     * Ends the path without filling or stroking it.
     */

    public void newPath() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("n").append_i(separator);
    }

    /**
     * Strokes the path.
     */

    public void stroke() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("S").append_i(separator);
    }

    /**
     * Closes the path and strokes it.
     */

    public void closePathStroke() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("s").append_i(separator);
    }

    /**
     * Fills the path, using the non-zero winding number rule to determine the region to fill.
     */

    public void fill() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("f").append_i(separator);
    }

    /**
     * Fills the path, using the even-odd rule to determine the region to fill.
     */

    public void eoFill() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("f*").append_i(separator);
    }

    /**
     * Fills the path using the non-zero winding number rule to determine the region to fill and strokes it.
     */

    public void fillStroke() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("B").append_i(separator);
    }

    /**
     * Closes the path, fills it using the non-zero winding number rule to determine the region to fill and strokes it.
     */

    public void closePathFillStroke() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("b").append_i(separator);
    }

    /**
     * Fills the path, using the even-odd rule to determine the region to fill and strokes it.
     */

    public void eoFillStroke() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("B*").append_i(separator);
    }

    /**
     * Closes the path, fills it using the even-odd rule to determine the region to fill and strokes it.
     */

    public void closePathEoFillStroke() {
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("path.construction.operator.inside.text.object"));
    	}
    	}
        content.append("b*").append_i(separator);
    }

    /**
     * Adds an <CODE>Image</CODE> to the page. The <CODE>Image</CODE> must have
     * absolute positioning.
     * @param image the <CODE>Image</CODE> object
     * @throws DocumentException if the <CODE>Image</CODE> does not have absolute positioning
     */
    public void addImage(final Image image) throws DocumentException {
        addImage(image, false);
    }

    /**
     * Adds an <CODE>Image</CODE> to the page. The <CODE>Image</CODE> must have
     * absolute positioning. The image can be placed inline.
     * @param image the <CODE>Image</CODE> object
     * @param inlineImage <CODE>true</CODE> to place this image inline, <CODE>false</CODE> otherwise
     * @throws DocumentException if the <CODE>Image</CODE> does not have absolute positioning
     */
    public void addImage(final Image image, final boolean inlineImage) throws DocumentException {
        if (!image.hasAbsoluteY())
            throw new DocumentException(MessageLocalization.getComposedMessage("the.image.must.have.absolute.positioning"));
        float matrix[] = image.matrix();
        matrix[Image.CX] = image.getAbsoluteX() - matrix[Image.CX];
        matrix[Image.CY] = image.getAbsoluteY() - matrix[Image.CY];
        addImage(image, matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5], inlineImage);
    }

    /**
     * Adds an <CODE>Image</CODE> to the page. The positioning of the <CODE>Image</CODE>
     * is done with the transformation matrix. To position an <CODE>image</CODE> at (x,y)
     * use addImage(image, image_width, 0, 0, image_height, x, y).
     * @param image the <CODE>Image</CODE> object
     * @param a an element of the transformation matrix
     * @param b an element of the transformation matrix
     * @param c an element of the transformation matrix
     * @param d an element of the transformation matrix
     * @param e an element of the transformation matrix
     * @param f an element of the transformation matrix
     * @throws DocumentException on error
     */
    public void addImage(final Image image, final float a, final float b, final float c, final float d, final float e, final float f) throws DocumentException {
        addImage(image, a, b, c, d, e, f, false);
    }

    /**
     * adds an image with the given matrix.
     * @param image image to add
     * @param transform transform to apply to the template prior to adding it.
     */
    public void addImage(final Image image, final AffineTransform transform) throws DocumentException {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	addImage( image, (float)matrix[0], (float)matrix[1], (float)matrix[2],
    			  (float)matrix[3], (float)matrix[4],(float) matrix[5], false );
    }

    /**
     * Adds an <CODE>Image</CODE> to the page. The positioning of the <CODE>Image</CODE>
     * is done with the transformation matrix. To position an <CODE>image</CODE> at (x,y)
     * use addImage(image, image_width, 0, 0, image_height, x, y). The image can be placed inline.
     * @param image the <CODE>Image</CODE> object
     * @param a an element of the transformation matrix
     * @param b an element of the transformation matrix
     * @param c an element of the transformation matrix
     * @param d an element of the transformation matrix
     * @param e an element of the transformation matrix
     * @param f an element of the transformation matrix
     * @param inlineImage <CODE>true</CODE> to place this image inline, <CODE>false</CODE> otherwise
     * @throws DocumentException on error
     */
    public void addImage(final Image image, final float a, final float b, final float c, final float d, final float e, final float f, final boolean inlineImage) throws DocumentException {
        try {
            if (image.getLayer() != null)
                beginLayer(image.getLayer());
            if (image.isImgTemplate()) {
                writer.addDirectImageSimple(image);
                PdfTemplate template = image.getTemplateData();
                float w = template.getWidth();
                float h = template.getHeight();
                addTemplate(template, a / w, b / w, c / h, d / h, e, f);
            }
            else {
                if (inText && autoControlTextBlocks) {
                    endText();
                }
                if (writer.isTagged() && allowTaggedImages)
                    beginMarkedContentSequence(new PdfStructureElement(getParentStructureElement(), PdfName.FIGURE));
                content.append("q ");
                content.append(a).append(' ');
                content.append(b).append(' ');
                content.append(c).append(' ');
                content.append(d).append(' ');
                content.append(e).append(' ');
                content.append(f).append(" cm");
                if (inlineImage) {
                    content.append("\nBI\n");
                    PdfImage pimage = new PdfImage(image, "", null);
                    if (image instanceof ImgJBIG2) {
                    	byte[] globals = ((ImgJBIG2)image).getGlobalBytes();
                    	if (globals != null) {
                    		PdfDictionary decodeparms = new PdfDictionary();
                    		decodeparms.put(PdfName.JBIG2GLOBALS, writer.getReferenceJBIG2Globals(globals));
                    		pimage.put(PdfName.DECODEPARMS, decodeparms);
                    	}
                    }
                    for (Object element : pimage.getKeys()) {
                        PdfName key = (PdfName)element;
                        PdfObject value = pimage.get(key);
                        String s = abrev.get(key);
                        if (s == null)
                            continue;
                        content.append(s);
                        boolean check = true;
                        if (key.equals(PdfName.COLORSPACE) && value.isArray()) {
                            PdfArray ar = (PdfArray)value;
                            if (ar.size() == 4
                                && PdfName.INDEXED.equals(ar.getAsName(0))
                                && ar.getPdfObject(1).isName()
                                && ar.getPdfObject(2).isNumber()
                                && ar.getPdfObject(3).isString()
                            ) {
                                check = false;
                            }

                        }
                        if (check && key.equals(PdfName.COLORSPACE) && !value.isName()) {
                            PdfName cs = writer.getColorspaceName();
                            PageResources prs = getPageResources();
                            prs.addColor(cs, writer.addToBody(value).getIndirectReference());
                            value = cs;
                        }
                        value.toPdf(null, content);
                        content.append('\n');
                    }
                    content.append("ID\n");
                    pimage.writeContent(content);
                    content.append("\nEI\nQ").append_i(separator);
                }
                else {
                    PdfName name;
                    PageResources prs = getPageResources();
                    Image maskImage = image.getImageMask();
                    if (maskImage != null) {
                        name = writer.addDirectImageSimple(maskImage);
                        prs.addXObject(name, writer.getImageReference(name));
                    }
                    name = writer.addDirectImageSimple(image);
                    name = prs.addXObject(name, writer.getImageReference(name));
                    content.append(' ').append(name.getBytes()).append(" Do Q").append_i(separator);
                }
                if (writer.isTagged() && allowTaggedImages)
                    endMarkedContentSequence();
            }
            if (image.hasBorders()) {
                saveState();
                float w = image.getWidth();
                float h = image.getHeight();
                concatCTM(a / w, b / w, c / h, d / h, e, f);
                rectangle(image);
                restoreState();
            }
            if (image.getLayer() != null)
                endLayer();
            Annotation annot = image.getAnnotation();
            if (annot == null)
                return;
            float[] r = new float[unitRect.length];
            for (int k = 0; k < unitRect.length; k += 2) {
                r[k] = a * unitRect[k] + c * unitRect[k + 1] + e;
                r[k + 1] = b * unitRect[k] + d * unitRect[k + 1] + f;
            }
            float llx = r[0];
            float lly = r[1];
            float urx = llx;
            float ury = lly;
            for (int k = 2; k < r.length; k += 2) {
                llx = Math.min(llx, r[k]);
                lly = Math.min(lly, r[k + 1]);
                urx = Math.max(urx, r[k]);
                ury = Math.max(ury, r[k + 1]);
            }
            annot = new Annotation(annot);
            annot.setDimensions(llx, lly, urx, ury);
            PdfAnnotation an = PdfAnnotationsImp.convertAnnotation(writer, annot, new Rectangle(llx, lly, urx, ury));
            if (an == null)
                return;
            addAnnotation(an);
        }
        catch (Exception ee) {
            throw new DocumentException(ee);
        }
    }

    /**
     * Makes this <CODE>PdfContentByte</CODE> empty.
     * Calls <code>reset( true )</code>
     */
    public void reset() {
        reset( true );
    }

    /**
     * Makes this <CODE>PdfContentByte</CODE> empty.
     * @param validateContent will call <code>sanityCheck()</code> if true.
     * @since 2.1.6
     */
    public void reset( final boolean validateContent ) {
        content.reset();
        if (validateContent) {
        	sanityCheck();
        }
        state = new GraphicState();
    }


    /**
     * Starts the writing of text.
     * @param restoreTM indicates if to restore text matrix of the previous text block.
     */
    public void beginText(boolean restoreTM) {
    	if (inText) {
            if (autoControlTextBlocks) {

            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.begin.end.text.operators"));
    	}
    	} else {
    	inText = true;
            content.append("BT").append_i(separator);
            if (restoreTM) {
                float tx = state.xTLM;
                setTextMatrix(state.aTLM, state.bTLM, state.cTLM, state.dTLM, state.tx, state.yTLM);
                state.xTLM = state.tx = tx;
            } else {
        state.xTLM = 0;
        state.yTLM = 0;
    }
        }
    }

    /**
     * Starts the writing of text.
     */
    public void beginText() {
    	beginText(false);
    }

    /**
     * Ends the writing of text and makes the current font invalid.
     */
    public void endText() {
    	if (!inText) {
            if (autoControlTextBlocks) {

            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.begin.end.text.operators"));
    	}
    	} else {
    	inText = false;
        content.append("ET").append_i(separator);
    }
    }

    /**
     * Saves the graphic state. <CODE>saveState</CODE> and
     * <CODE>restoreState</CODE> must be balanced.
     */
    public void saveState() {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        content.append("q").append_i(separator);
        stateList.add(new GraphicState(state));
    }

    /**
     * Restores the graphic state. <CODE>saveState</CODE> and
     * <CODE>restoreState</CODE> must be balanced.
     */
    public void restoreState() {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        content.append("Q").append_i(separator);
        int idx = stateList.size() - 1;
        if (idx < 0)
            throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.save.restore.state.operators"));
        state = stateList.get(idx);
        stateList.remove(idx);
    }

    /**
     * Sets the character spacing parameter.
     *
     * @param       charSpace           a parameter
     */
    public void setCharacterSpacing(final float charSpace) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.charSpace = charSpace;
        content.append(charSpace).append(" Tc").append_i(separator);
    }

    /**
     * Sets the word spacing parameter.
     *
     * @param       wordSpace           a parameter
     */
    public void setWordSpacing(final float wordSpace) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.wordSpace = wordSpace;
        content.append(wordSpace).append(" Tw").append_i(separator);
    }

    /**
     * Sets the horizontal scaling parameter.
     *
     * @param       scale               a parameter
     */
    public void setHorizontalScaling(final float scale) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.scale = scale;
        content.append(scale).append(" Tz").append_i(separator);
    }

    /**
     * Sets the text leading parameter.
     * <P>
     * The leading parameter is measured in text space units. It specifies the vertical distance
     * between the baselines of adjacent lines of text.</P>
     *
     * @param       leading         the new leading
     */
    public void setLeading(final float leading) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.leading = leading;
        content.append(leading).append(" TL").append_i(separator);
    }

    /**
     * Set the font and the size for the subsequent text writing.
     *
     * @param bf the font
     * @param size the font size in points
     */
    public void setFontAndSize(final BaseFont bf, final float size) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        checkWriter();
        if (size < 0.0001f && size > -0.0001f)
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("font.size.too.small.1", String.valueOf(size)));
        state.size = size;
        state.fontDetails = writer.addSimple(bf);
        PageResources prs = getPageResources();
        PdfName name = state.fontDetails.getFontName();
        name = prs.addFont(name, state.fontDetails.getIndirectReference());
        content.append(name.getBytes()).append(' ').append(size).append(" Tf").append_i(separator);
    }

    /**
     * Sets the text rendering parameter.
     *
     * @param       rendering               a parameter
     */
    public void setTextRenderingMode(final int rendering) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        content.append(rendering).append(" Tr").append_i(separator);
    }

    /**
     * Sets the text rise parameter.
     * <P>
     * This allows to write text in subscript or superscript mode.</P>
     *
     * @param       rise                a parameter
     */
    public void setTextRise(final float rise) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        content.append(rise).append(" Ts").append_i(separator);
    }

    /**
     * A helper to insert into the content stream the <CODE>text</CODE>
     * converted to bytes according to the font's encoding.
     *
     * @param text the text to write
     */
    private void showText2(final String text) {
        if (state.fontDetails == null)
            throw new NullPointerException(MessageLocalization.getComposedMessage("font.and.size.must.be.set.before.writing.any.text"));
        byte b[] = state.fontDetails.convertToBytes(text);
        escapeString(b, content);
    }

    /**
     * Shows the <CODE>text</CODE>.
     *
     * @param text the text to write
     */
    public void showText(final String text) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        if (writer.isTagged())
            beginMarkedContentSequence(new PdfStructureElement(getParentStructureElement(), PdfName.SPAN));
        showText2(text);
        updateTx(text, 0);
        content.append("Tj").append_i(separator);
        if (writer.isTagged())
           endMarkedContentSequence();
    }

    /**
     * Constructs a kern array for a text in a certain font
     * @param text the text
     * @param font the font
     * @return a PdfTextArray
     */
    public static PdfTextArray getKernArray(final String text, final BaseFont font) {
        PdfTextArray pa = new PdfTextArray();
        StringBuffer acc = new StringBuffer();
        int len = text.length() - 1;
        char c[] = text.toCharArray();
        if (len >= 0)
            acc.append(c, 0, 1);
        for (int k = 0; k < len; ++k) {
            char c2 = c[k + 1];
            int kern = font.getKerning(c[k], c2);
            if (kern == 0) {
                acc.append(c2);
            }
            else {
                pa.add(acc.toString());
                acc.setLength(0);
                acc.append(c, k + 1, 1);
                pa.add(-kern);
            }
        }
        pa.add(acc.toString());
        return pa;
    }

    /**
     * Shows the <CODE>text</CODE> kerned.
     *
     * @param text the text to write
     */
    public void showTextKerned(final String text) {
        if (state.fontDetails == null)
            throw new NullPointerException(MessageLocalization.getComposedMessage("font.and.size.must.be.set.before.writing.any.text"));
        BaseFont bf = state.fontDetails.getBaseFont();
        if (bf.hasKernPairs())
            showText(getKernArray(text, bf));
        else {
            showText(text);
        }
    }

    /**
     * Moves to the next line and shows <CODE>text</CODE>.
     *
     * @param text the text to write
     */
    public void newlineShowText(final String text) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        if (writer.isTagged())
            beginMarkedContentSequence(new PdfStructureElement(getParentStructureElement(), PdfName.SPAN));
        state.yTLM -= state.leading;
        showText2(text);
        content.append("'").append_i(separator);
        if (writer.isTagged())
            endMarkedContentSequence();
        state.tx = state.xTLM;
        updateTx(text, 0);
    }

    /**
     * Moves to the next line and shows text string, using the given values of the character and word spacing parameters.
     *
     * @param       wordSpacing     a parameter
     * @param       charSpacing     a parameter
     * @param text the text to write
     */
    public void newlineShowText(final float wordSpacing, final float charSpacing, final String text) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        if (writer.isTagged())
            beginMarkedContentSequence(new PdfStructureElement(getParentStructureElement(), PdfName.SPAN));
        state.yTLM -= state.leading;
        content.append(wordSpacing).append(' ').append(charSpacing);
        showText2(text);
        content.append("\"").append_i(separator);
        if (writer.isTagged())
            endMarkedContentSequence();
        // The " operator sets charSpace and wordSpace into graphics state
        // (cfr PDF reference v1.6, table 5.6)
        state.charSpace = charSpacing;
        state.wordSpace = wordSpacing;
        state.tx = state.xTLM;
        updateTx(text, 0);
    }

    /**
     * Changes the text matrix.
     * <P>
     * Remark: this operation also initializes the current point position.</P>
     *
     * @param       a           operand 1,1 in the matrix
     * @param       b           operand 1,2 in the matrix
     * @param       c           operand 2,1 in the matrix
     * @param       d           operand 2,2 in the matrix
     * @param       x           operand 3,1 in the matrix
     * @param       y           operand 3,2 in the matrix
     */
    public void setTextMatrix(final float a, final float b, final float c, final float d, final float x, final float y) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.xTLM = x;
        state.yTLM = y;
        state.aTLM = a;
        state.bTLM = b;
        state.cTLM = c;
        state.dTLM = d;
        state.tx = state.xTLM;
        content.append(a).append(' ').append(b).append_i(' ')
        .append(c).append_i(' ').append(d).append_i(' ')
        .append(x).append_i(' ').append(y).append(" Tm").append_i(separator);
    }

    /**
     * Changes the text matrix.
     * <P>
     * @param transform overwrite the current text matrix with this one
     */
    public void setTextMatrix(final AffineTransform transform) {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	setTextMatrix((float) matrix[0], (float) matrix[1], (float) matrix[2],
                (float) matrix[3], (float) matrix[4], (float) matrix[5]);
    }
    
    /**
     * Changes the text matrix. The first four parameters are {1,0,0,1}.
     * <P>
     * Remark: this operation also initializes the current point position.</P>
     *
     * @param       x           operand 3,1 in the matrix
     * @param       y           operand 3,2 in the matrix
     */
    public void setTextMatrix(final float x, final float y) {
        setTextMatrix(1, 0, 0, 1, x, y);
    }

    /**
     * Moves to the start of the next line, offset from the start of the current line.
     *
     * @param       x           x-coordinate of the new current point
     * @param       y           y-coordinate of the new current point
     */
    public void moveText(final float x, final float y) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.xTLM += x;
        state.yTLM += y;
        if (autoControlTextBlocks && state.xTLM != state.tx) {
            setTextMatrix(state.aTLM, state.bTLM, state.cTLM, state.dTLM, state.xTLM, state.yTLM);
        } else {
            content.append(x).append(' ').append(y).append(" Td").append_i(separator);
        }
    }

    /**
     * Moves to the start of the next line, offset from the start of the current line.
     * <P>
     * As a side effect, this sets the leading parameter in the text state.</P>
     *
     * @param       x           offset of the new current point
     * @param       y           y-coordinate of the new current point
     */
    public void moveTextWithLeading(final float x, final float y) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        state.xTLM += x;
        state.yTLM += y;
        state.leading = -y;
        if (autoControlTextBlocks && state.xTLM != state.tx) {
            setTextMatrix(state.aTLM, state.bTLM, state.cTLM, state.dTLM, state.xTLM, state.yTLM);
        } else {
            content.append(x).append(' ').append(y).append(" TD").append_i(separator);
        }
    }

    /**
     * Moves to the start of the next line.
     */
    public void newlineText() {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        if (autoControlTextBlocks && state.xTLM != state.tx) {
            setTextMatrix(state.aTLM, state.bTLM, state.cTLM, state.dTLM, state.xTLM, state.yTLM);
        }
        state.yTLM -= state.leading;
        content.append("T*").append_i(separator);
    }

    /**
     * Gets the size of this content.
     *
     * @return the size of the content
     */
    int size() {
        return content.size();
    }

    /**
     * Escapes a <CODE>byte</CODE> array according to the PDF conventions.
     *
     * @param b the <CODE>byte</CODE> array to escape
     * @return an escaped <CODE>byte</CODE> array
     */
    static byte[] escapeString(final byte b[]) {
        ByteBuffer content = new ByteBuffer();
        escapeString(b, content);
        return content.toByteArray();
    }

    /**
     * Escapes a <CODE>byte</CODE> array according to the PDF conventions.
     *
     * @param b the <CODE>byte</CODE> array to escape
     * @param content the content
     */
    static void escapeString(final byte b[], final ByteBuffer content) {
        content.append_i('(');
        for (int k = 0; k < b.length; ++k) {
            byte c = b[k];
            switch (c) {
                case '\r':
                    content.append("\\r");
                    break;
                case '\n':
                    content.append("\\n");
                    break;
                case '\t':
                    content.append("\\t");
                    break;
                case '\b':
                    content.append("\\b");
                    break;
                case '\f':
                    content.append("\\f");
                    break;
                case '(':
                case ')':
                case '\\':
                    content.append_i('\\').append_i(c);
                    break;
                default:
                    content.append_i(c);
            }
        }
        content.append(")");
    }

    /**
     * Adds a named outline to the document.
     *
     * @param outline the outline
     * @param name the name for the local destination
     */
    public void addOutline(final PdfOutline outline, final String name) {
        checkWriter();
        pdf.addOutline(outline, name);
    }
    /**
     * Gets the root outline.
     *
     * @return the root outline
     */
    public PdfOutline getRootOutline() {
        checkWriter();
        return pdf.getRootOutline();
    }

    /**
     * Computes the width of the given string taking in account
     * the current values of "Character spacing", "Word Spacing"
     * and "Horizontal Scaling".
     * The additional spacing is not computed for the last character
     * of the string.
     * @param text the string to get width of
     * @param kerned the kerning option
     * @return the width
     */

    public float getEffectiveStringWidth(final String text, final boolean kerned) {
        BaseFont bf = state.fontDetails.getBaseFont();

        float w;
        if (kerned)
            w = bf.getWidthPointKerned(text, state.size);
        else
            w = bf.getWidthPoint(text, state.size);

        if (state.charSpace != 0.0f && text.length() > 1) {
            w += state.charSpace * (text.length() -1);
        }

        int ft = bf.getFontType();
        if (state.wordSpace != 0.0f && (ft == BaseFont.FONT_TYPE_T1 || ft == BaseFont.FONT_TYPE_TT || ft == BaseFont.FONT_TYPE_T3)) {
            for (int i = 0; i < text.length() -1; i++) {
                if (text.charAt(i) == ' ')
                    w += state.wordSpace;
            }
        }
        if (state.scale != 100.0)
            w = w * state.scale / 100.0f;

        //System.out.println("String width = " + Float.toString(w));
        return w;
    }

    /**
     * Shows text right, left or center aligned with rotation.
     * @param alignment the alignment can be ALIGN_CENTER, ALIGN_RIGHT or ALIGN_LEFT
     * @param text the text to show
     * @param x the x pivot position
     * @param y the y pivot position
     * @param rotation the rotation to be applied in degrees counterclockwise
     */
    public void showTextAligned(final int alignment, final String text, final float x, final float y, final float rotation) {
        showTextAligned(alignment, text, x, y, rotation, false);
    }

    private void showTextAligned(final int alignment, final String text, float x, float y, final float rotation, final boolean kerned) {
        if (state.fontDetails == null)
            throw new NullPointerException(MessageLocalization.getComposedMessage("font.and.size.must.be.set.before.writing.any.text"));
        if (rotation == 0) {
            switch (alignment) {
                case ALIGN_CENTER:
                    x -= getEffectiveStringWidth(text, kerned) / 2;
                    break;
                case ALIGN_RIGHT:
                    x -= getEffectiveStringWidth(text, kerned);
                    break;
            }
            setTextMatrix(x, y);
            if (kerned)
                showTextKerned(text);
            else
                showText(text);
        }
        else {
            double alpha = rotation * Math.PI / 180.0;
            float cos = (float)Math.cos(alpha);
            float sin = (float)Math.sin(alpha);
            float len;
            switch (alignment) {
                case ALIGN_CENTER:
                    len = getEffectiveStringWidth(text, kerned) / 2;
                    x -=  len * cos;
                    y -=  len * sin;
                    break;
                case ALIGN_RIGHT:
                    len = getEffectiveStringWidth(text, kerned);
                    x -=  len * cos;
                    y -=  len * sin;
                    break;
            }
            setTextMatrix(cos, sin, -sin, cos, x, y);
            if (kerned)
                showTextKerned(text);
            else
                showText(text);
            setTextMatrix(0f, 0f);
        }
    }

    /**
     * Shows text kerned right, left or center aligned with rotation.
     * @param alignment the alignment can be ALIGN_CENTER, ALIGN_RIGHT or ALIGN_LEFT
     * @param text the text to show
     * @param x the x pivot position
     * @param y the y pivot position
     * @param rotation the rotation to be applied in degrees counterclockwise
     */
    public void showTextAlignedKerned(final int alignment, final String text, final float x, final float y, final float rotation) {
        showTextAligned(alignment, text, x, y, rotation, true);
    }

    /**
     * Concatenate a matrix to the current transformation matrix.
     * @param a an element of the transformation matrix
     * @param b an element of the transformation matrix
     * @param c an element of the transformation matrix
     * @param d an element of the transformation matrix
     * @param e an element of the transformation matrix
     * @param f an element of the transformation matrix
     **/
    public void concatCTM(final float a, final float b, final float c, final float d, final float e, final float f) {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        content.append(a).append(' ').append(b).append(' ').append(c).append(' ');
        content.append(d).append(' ').append(e).append(' ').append(f).append(" cm").append_i(separator);
    }

    /**
     * Concatenate a matrix to the current transformation matrix.
     * @param transform added to the Current Transformation Matrix
     */
    public void concatCTM(final AffineTransform transform) {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	concatCTM((float) matrix[0], (float) matrix[1], (float) matrix[2],
                (float) matrix[3], (float) matrix[4], (float) matrix[5]);
    }

    /**
     * Generates an array of bezier curves to draw an arc.
     * <P>
     * (x1, y1) and (x2, y2) are the corners of the enclosing rectangle.
     * Angles, measured in degrees, start with 0 to the right (the positive X
     * axis) and increase counter-clockwise.  The arc extends from startAng
     * to startAng+extent.  I.e. startAng=0 and extent=180 yields an openside-down
     * semi-circle.
     * <P>
     * The resulting coordinates are of the form float[]{x1,y1,x2,y2,x3,y3, x4,y4}
     * such that the curve goes from (x1, y1) to (x4, y4) with (x2, y2) and
     * (x3, y3) as their respective Bezier control points.
     * <P>
     * Note: this code was taken from ReportLab (www.reportlab.org), an excellent
     * PDF generator for Python (BSD license: http://www.reportlab.org/devfaq.html#1.3 ).
     *
     * @param x1 a corner of the enclosing rectangle
     * @param y1 a corner of the enclosing rectangle
     * @param x2 a corner of the enclosing rectangle
     * @param y2 a corner of the enclosing rectangle
     * @param startAng starting angle in degrees
     * @param extent angle extent in degrees
     * @return a list of float[] with the bezier curves
     */
    public static ArrayList<float[]> bezierArc(float x1, float y1, float x2, float y2, final float startAng, final float extent) {
        float tmp;
        if (x1 > x2) {
            tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        if (y2 > y1) {
            tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        float fragAngle;
        int Nfrag;
        if (Math.abs(extent) <= 90f) {
            fragAngle = extent;
            Nfrag = 1;
        }
        else {
            Nfrag = (int)Math.ceil(Math.abs(extent)/90f);
            fragAngle = extent / Nfrag;
        }
        float x_cen = (x1+x2)/2f;
        float y_cen = (y1+y2)/2f;
        float rx = (x2-x1)/2f;
        float ry = (y2-y1)/2f;
        float halfAng = (float)(fragAngle * Math.PI / 360.);
        float kappa = (float)Math.abs(4. / 3. * (1. - Math.cos(halfAng)) / Math.sin(halfAng));
        ArrayList<float[]> pointList = new ArrayList<float[]>();
        for (int i = 0; i < Nfrag; ++i) {
            float theta0 = (float)((startAng + i*fragAngle) * Math.PI / 180.);
            float theta1 = (float)((startAng + (i+1)*fragAngle) * Math.PI / 180.);
            float cos0 = (float)Math.cos(theta0);
            float cos1 = (float)Math.cos(theta1);
            float sin0 = (float)Math.sin(theta0);
            float sin1 = (float)Math.sin(theta1);
            if (fragAngle > 0f) {
                pointList.add(new float[]{x_cen + rx * cos0,
                y_cen - ry * sin0,
                x_cen + rx * (cos0 - kappa * sin0),
                y_cen - ry * (sin0 + kappa * cos0),
                x_cen + rx * (cos1 + kappa * sin1),
                y_cen - ry * (sin1 - kappa * cos1),
                x_cen + rx * cos1,
                y_cen - ry * sin1});
            }
            else {
                pointList.add(new float[]{x_cen + rx * cos0,
                y_cen - ry * sin0,
                x_cen + rx * (cos0 + kappa * sin0),
                y_cen - ry * (sin0 - kappa * cos0),
                x_cen + rx * (cos1 - kappa * sin1),
                y_cen - ry * (sin1 + kappa * cos1),
                x_cen + rx * cos1,
                y_cen - ry * sin1});
            }
        }
        return pointList;
    }

    /**
     * Draws a partial ellipse inscribed within the rectangle x1,y1,x2,y2,
     * starting at startAng degrees and covering extent degrees. Angles
     * start with 0 to the right (+x) and increase counter-clockwise.
     *
     * @param x1 a corner of the enclosing rectangle
     * @param y1 a corner of the enclosing rectangle
     * @param x2 a corner of the enclosing rectangle
     * @param y2 a corner of the enclosing rectangle
     * @param startAng starting angle in degrees
     * @param extent angle extent in degrees
     */
    public void arc(final float x1, final float y1, final float x2, final float y2, final float startAng, final float extent) {
        ArrayList<float[]> ar = bezierArc(x1, y1, x2, y2, startAng, extent);
        if (ar.isEmpty())
            return;
        float pt[] = ar.get(0);
        moveTo(pt[0], pt[1]);
        for (int k = 0; k < ar.size(); ++k) {
            pt = ar.get(k);
            curveTo(pt[2], pt[3], pt[4], pt[5], pt[6], pt[7]);
        }
    }

    /**
     * Draws an ellipse inscribed within the rectangle x1,y1,x2,y2.
     *
     * @param x1 a corner of the enclosing rectangle
     * @param y1 a corner of the enclosing rectangle
     * @param x2 a corner of the enclosing rectangle
     * @param y2 a corner of the enclosing rectangle
     */
    public void ellipse(final float x1, final float y1, final float x2, final float y2) {
        arc(x1, y1, x2, y2, 0f, 360f);
    }

    /**
     * Create a new colored tiling pattern.
     *
     * @param width the width of the pattern
     * @param height the height of the pattern
     * @param xstep the desired horizontal spacing between pattern cells.
     * May be either positive or negative, but not zero.
     * @param ystep the desired vertical spacing between pattern cells.
     * May be either positive or negative, but not zero.
     * @return the <CODE>PdfPatternPainter</CODE> where the pattern will be created
     */
    public PdfPatternPainter createPattern(final float width, final float height, final float xstep, final float ystep) {
        checkWriter();
        if ( xstep == 0.0f || ystep == 0.0f )
            throw new RuntimeException(MessageLocalization.getComposedMessage("xstep.or.ystep.can.not.be.zero"));
        PdfPatternPainter painter = new PdfPatternPainter(writer);
        painter.setWidth(width);
        painter.setHeight(height);
        painter.setXStep(xstep);
        painter.setYStep(ystep);
        writer.addSimplePattern(painter);
        return painter;
    }

    /**
     * Create a new colored tiling pattern. Variables xstep and ystep are set to the same values
     * of width and height.
     * @param width the width of the pattern
     * @param height the height of the pattern
     * @return the <CODE>PdfPatternPainter</CODE> where the pattern will be created
     */
    public PdfPatternPainter createPattern(final float width, final float height) {
        return createPattern(width, height, width, height);
    }

    /**
     * Create a new uncolored tiling pattern.
     *
     * @param width the width of the pattern
     * @param height the height of the pattern
     * @param xstep the desired horizontal spacing between pattern cells.
     * May be either positive or negative, but not zero.
     * @param ystep the desired vertical spacing between pattern cells.
     * May be either positive or negative, but not zero.
     * @param color the default color. Can be <CODE>null</CODE>
     * @return the <CODE>PdfPatternPainter</CODE> where the pattern will be created
     */
    public PdfPatternPainter createPattern(final float width, final float height, final float xstep, final float ystep, final BaseColor color) {
        checkWriter();
        if ( xstep == 0.0f || ystep == 0.0f )
            throw new RuntimeException(MessageLocalization.getComposedMessage("xstep.or.ystep.can.not.be.zero"));
        PdfPatternPainter painter = new PdfPatternPainter(writer, color);
        painter.setWidth(width);
        painter.setHeight(height);
        painter.setXStep(xstep);
        painter.setYStep(ystep);
        writer.addSimplePattern(painter);
        return painter;
    }

    /**
     * Create a new uncolored tiling pattern.
     * Variables xstep and ystep are set to the same values
     * of width and height.
     * @param width the width of the pattern
     * @param height the height of the pattern
     * @param color the default color. Can be <CODE>null</CODE>
     * @return the <CODE>PdfPatternPainter</CODE> where the pattern will be created
     */
    public PdfPatternPainter createPattern(final float width, final float height, final BaseColor color) {
        return createPattern(width, height, width, height, color);
    }

    /**
     * Creates a new template.
     * <P>
     * Creates a new template that is nothing more than a form XObject. This template can be included
     * in this <CODE>PdfContentByte</CODE> or in another template. Templates are only written
     * to the output when the document is closed permitting things like showing text in the first page
     * that is only defined in the last page.
     *
     * @param width the bounding box width
     * @param height the bounding box height
     * @return the created template
     */
    public PdfTemplate createTemplate(final float width, final float height) {
        return createTemplate(width, height, null);
    }

    PdfTemplate createTemplate(final float width, final float height, final PdfName forcedName) {
        checkWriter();
        PdfTemplate template = new PdfTemplate(writer);
        template.setWidth(width);
        template.setHeight(height);
        writer.addDirectTemplateSimple(template, forcedName);
        return template;
    }

    /**
     * Creates a new appearance to be used with form fields.
     *
     * @param width the bounding box width
     * @param height the bounding box height
     * @return the appearance created
     */
    public PdfAppearance createAppearance(final float width, final float height) {
        return createAppearance(width, height, null);
    }

    PdfAppearance createAppearance(final float width, final float height, final PdfName forcedName) {
        checkWriter();
        PdfAppearance template = new PdfAppearance(writer);
        template.setWidth(width);
        template.setHeight(height);
        writer.addDirectTemplateSimple(template, forcedName);
        return template;
    }

    /**
     * Adds a PostScript XObject to this content.
     *
     * @param psobject the object
     */
    public void addPSXObject(final PdfPSXObject psobject) {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        checkWriter();
        PdfName name = writer.addDirectTemplateSimple(psobject, null);
        PageResources prs = getPageResources();
        name = prs.addXObject(name, psobject.getIndirectReference());
        content.append(name.getBytes()).append(" Do").append_i(separator);
    }

    /**
     * Adds a template to this content.
     *
     * @param template the template
     * @param a an element of the transformation matrix
     * @param b an element of the transformation matrix
     * @param c an element of the transformation matrix
     * @param d an element of the transformation matrix
     * @param e an element of the transformation matrix
     * @param f an element of the transformation matrix
     */
    public void addTemplate(final PdfTemplate template, final float a, final float b, final float c, final float d, final float e, final float f) {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        if (writer.isTagged() && allowTaggedImages)
            beginMarkedContentSequence(new PdfStructureElement(getParentStructureElement(), PdfName.FIGURE));
        checkWriter();
        checkNoPattern(template);
        PdfName name = writer.addDirectTemplateSimple(template, null);
        PageResources prs = getPageResources();
        name = prs.addXObject(name, template.getIndirectReference());
        content.append("q ");
        content.append(a).append(' ');
        content.append(b).append(' ');
        content.append(c).append(' ');
        content.append(d).append(' ');
        content.append(e).append(' ');
        content.append(f).append(" cm ");
        content.append(name.getBytes()).append(" Do Q").append_i(separator);
        if (writer.isTagged() && allowTaggedImages)
            endMarkedContentSequence();
    }

    /**
     * adds a template with the given matrix.
     * @param template template to add
     * @param transform transform to apply to the template prior to adding it.
     */
    public void addTemplate(final PdfTemplate template, final AffineTransform transform) {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	addTemplate(template, (float) matrix[0], (float) matrix[1], (float) matrix[2],
                (float) matrix[3], (float) matrix[4], (float) matrix[5]);
    }

    void addTemplateReference(final PdfIndirectReference template, PdfName name, final float a, final float b, final float c, final float d, final float e, final float f) {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        checkWriter();
        PageResources prs = getPageResources();
        name = prs.addXObject(name, template);
        content.append("q ");
        content.append(a).append(' ');
        content.append(b).append(' ');
        content.append(c).append(' ');
        content.append(d).append(' ');
        content.append(e).append(' ');
        content.append(f).append(" cm ");
        content.append(name.getBytes()).append(" Do Q").append_i(separator);
    }

    /**
     * Adds a template to this content.
     *
     * @param template the template
     * @param x the x location of this template
     * @param y the y location of this template
     */
    public void addTemplate(final PdfTemplate template, final float x, final float y) {
        addTemplate(template, 1, 0, 0, 1, x, y);
    }

    /**
     * Changes the current color for filling paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceCMYK</B> (or the <B>DefaultCMYK</B> color space),
     * and sets the color to use for filling paths.</P>
     * <P>
     * This method is described in the 'Portable Document Format Reference Manual version 1.3'
     * section 8.5.2.1 (page 331).</P>
     * <P>
     * Following the PDF manual, each operand must be a number between 0 (no ink) and
     * 1 (maximum ink). This method however accepts only integers between 0x00 and 0xFF.</P>
     *
     * @param cyan the intensity of cyan
     * @param magenta the intensity of magenta
     * @param yellow the intensity of yellow
     * @param black the intensity of black
     */

    public void setCMYKColorFill(final int cyan, final int magenta, final int yellow, final int black) {
        content.append((float)(cyan & 0xFF) / 0xFF);
        content.append(' ');
        content.append((float)(magenta & 0xFF) / 0xFF);
        content.append(' ');
        content.append((float)(yellow & 0xFF) / 0xFF);
        content.append(' ');
        content.append((float)(black & 0xFF) / 0xFF);
        content.append(" k").append_i(separator);
    }
    /**
     * Changes the current color for stroking paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceCMYK</B> (or the <B>DefaultCMYK</B> color space),
     * and sets the color to use for stroking paths.</P>
     * <P>
     * This method is described in the 'Portable Document Format Reference Manual version 1.3'
     * section 8.5.2.1 (page 331).</P>
     * Following the PDF manual, each operand must be a number between 0 (minimum intensity) and
     * 1 (maximum intensity). This method however accepts only integers between 0x00 and 0xFF.
     *
     * @param cyan the intensity of red
     * @param magenta the intensity of green
     * @param yellow the intensity of blue
     * @param black the intensity of black
     */

    public void setCMYKColorStroke(final int cyan, final int magenta, final int yellow, final int black) {
        content.append((float)(cyan & 0xFF) / 0xFF);
        content.append(' ');
        content.append((float)(magenta & 0xFF) / 0xFF);
        content.append(' ');
        content.append((float)(yellow & 0xFF) / 0xFF);
        content.append(' ');
        content.append((float) (black & 0xFF) / 0xFF);
        content.append(" K").append_i(separator);
    }

    /**
     * Changes the current color for filling paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceRGB</B> (or the <B>DefaultRGB</B> color space),
     * and sets the color to use for filling paths.</P>
     * <P>
     * This method is described in the 'Portable Document Format Reference Manual version 1.3'
     * section 8.5.2.1 (page 331).</P>
     * <P>
     * Following the PDF manual, each operand must be a number between 0 (minimum intensity) and
     * 1 (maximum intensity). This method however accepts only integers between 0x00 and 0xFF.</P>
     *
     * @param red the intensity of red
     * @param green the intensity of green
     * @param blue the intensity of blue
     */

    public void setRGBColorFill(final int red, final int green, final int blue) {
        HelperRGB((float) (red & 0xFF) / 0xFF, (float) (green & 0xFF) / 0xFF, (float) (blue & 0xFF) / 0xFF);
        content.append(" rg").append_i(separator);
    }

    /**
     * Changes the current color for stroking paths (device dependent colors!).
     * <P>
     * Sets the color space to <B>DeviceRGB</B> (or the <B>DefaultRGB</B> color space),
     * and sets the color to use for stroking paths.</P>
     * <P>
     * This method is described in the 'Portable Document Format Reference Manual version 1.3'
     * section 8.5.2.1 (page 331).</P>
     * Following the PDF manual, each operand must be a number between 0 (minimum intensity) and
     * 1 (maximum intensity). This method however accepts only integers between 0x00 and 0xFF.
     *
     * @param red the intensity of red
     * @param green the intensity of green
     * @param blue the intensity of blue
     */

    public void setRGBColorStroke(final int red, final int green, final int blue) {
        HelperRGB((float) (red & 0xFF) / 0xFF, (float) (green & 0xFF) / 0xFF, (float) (blue & 0xFF) / 0xFF);
        content.append(" RG").append_i(separator);
    }

    /** Sets the stroke color. <CODE>color</CODE> can be an
     * <CODE>ExtendedColor</CODE>.
     * @param color the color
     */
    public void setColorStroke(final BaseColor color) {
    	PdfWriter.checkPdfIsoConformance(writer, PdfIsoKeys.PDFISOKEY_COLOR, color);
        int type = ExtendedColor.getType(color);
        switch (type) {
            case ExtendedColor.TYPE_GRAY: {
                setGrayStroke(((GrayColor) color).getGray());
                break;
            }
            case ExtendedColor.TYPE_CMYK: {
                CMYKColor cmyk = (CMYKColor)color;
                setCMYKColorStrokeF(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
                break;
            }
            case ExtendedColor.TYPE_SEPARATION: {
                SpotColor spot = (SpotColor)color;
                setColorStroke(spot.getPdfSpotColor(), spot.getTint());
                break;
            }
            case ExtendedColor.TYPE_PATTERN: {
                PatternColor pat = (PatternColor) color;
                setPatternStroke(pat.getPainter());
                break;
            }
            case ExtendedColor.TYPE_SHADING: {
                ShadingColor shading = (ShadingColor) color;
                setShadingStroke(shading.getPdfShadingPattern());
                break;
            }
            default:
                setRGBColorStroke(color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /** Sets the fill color. <CODE>color</CODE> can be an
     * <CODE>ExtendedColor</CODE>.
     * @param color the color
     */
    public void setColorFill(final BaseColor color) {
    	PdfWriter.checkPdfIsoConformance(writer, PdfIsoKeys.PDFISOKEY_COLOR, color);
        int type = ExtendedColor.getType(color);
        switch (type) {
            case ExtendedColor.TYPE_GRAY: {
                setGrayFill(((GrayColor) color).getGray());
                break;
            }
            case ExtendedColor.TYPE_CMYK: {
                CMYKColor cmyk = (CMYKColor)color;
                setCMYKColorFillF(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
                break;
            }
            case ExtendedColor.TYPE_SEPARATION: {
                SpotColor spot = (SpotColor)color;
                setColorFill(spot.getPdfSpotColor(), spot.getTint());
                break;
            }
            case ExtendedColor.TYPE_PATTERN: {
                PatternColor pat = (PatternColor) color;
                setPatternFill(pat.getPainter());
                break;
            }
            case ExtendedColor.TYPE_SHADING: {
                ShadingColor shading = (ShadingColor) color;
                setShadingFill(shading.getPdfShadingPattern());
                break;
            }
            default:
                setRGBColorFill(color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /** Sets the fill color to a spot color.
     * @param sp the spot color
     * @param tint the tint for the spot color. 0 is no color and 1
     * is 100% color
     */
    public void setColorFill(final PdfSpotColor sp, final float tint) {
        checkWriter();
        state.colorDetails = writer.addSimple(sp);
        PageResources prs = getPageResources();
        PdfName name = state.colorDetails.getColorName();
        name = prs.addColor(name, state.colorDetails.getIndirectReference());
        content.append(name.getBytes()).append(" cs ").append(tint).append(" scn").append_i(separator);
    }

    /** Sets the stroke color to a spot color.
     * @param sp the spot color
     * @param tint the tint for the spot color. 0 is no color and 1
     * is 100% color
     */
    public void setColorStroke(final PdfSpotColor sp, final float tint) {
        checkWriter();
        state.colorDetails = writer.addSimple(sp);
        PageResources prs = getPageResources();
        PdfName name = state.colorDetails.getColorName();
        name = prs.addColor(name, state.colorDetails.getIndirectReference());
        content.append(name.getBytes()).append(" CS ").append(tint).append(" SCN").append_i(separator);
    }

    /** Sets the fill color to a pattern. The pattern can be
     * colored or uncolored.
     * @param p the pattern
     */
    public void setPatternFill(final PdfPatternPainter p) {
        if (p.isStencil()) {
            setPatternFill(p, p.getDefaultColor());
            return;
        }
        checkWriter();
        PageResources prs = getPageResources();
        PdfName name = writer.addSimplePattern(p);
        name = prs.addPattern(name, p.getIndirectReference());
        content.append(PdfName.PATTERN.getBytes()).append(" cs ").append(name.getBytes()).append(" scn").append_i(separator);
    }

    /** Outputs the color values to the content.
     * @param color The color
     * @param tint the tint if it is a spot color, ignored otherwise
     */
    void outputColorNumbers(final BaseColor color, final float tint) {
    	PdfWriter.checkPdfIsoConformance(writer, PdfIsoKeys.PDFISOKEY_COLOR, color);
        int type = ExtendedColor.getType(color);
        switch (type) {
            case ExtendedColor.TYPE_RGB:
                content.append((float)color.getRed() / 0xFF);
                content.append(' ');
                content.append((float)color.getGreen() / 0xFF);
                content.append(' ');
                content.append((float)color.getBlue() / 0xFF);
                break;
            case ExtendedColor.TYPE_GRAY:
                content.append(((GrayColor)color).getGray());
                break;
            case ExtendedColor.TYPE_CMYK: {
                CMYKColor cmyk = (CMYKColor)color;
                content.append(cmyk.getCyan()).append(' ').append(cmyk.getMagenta());
                content.append(' ').append(cmyk.getYellow()).append(' ').append(cmyk.getBlack());
                break;
            }
            case ExtendedColor.TYPE_SEPARATION:
                content.append(tint);
                break;
            default:
                throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.color.type"));
        }
    }

    /** Sets the fill color to an uncolored pattern.
     * @param p the pattern
     * @param color the color of the pattern
     */
    public void setPatternFill(final PdfPatternPainter p, final BaseColor color) {
        if (ExtendedColor.getType(color) == ExtendedColor.TYPE_SEPARATION)
            setPatternFill(p, color, ((SpotColor)color).getTint());
        else
            setPatternFill(p, color, 0);
    }

    /** Sets the fill color to an uncolored pattern.
     * @param p the pattern
     * @param color the color of the pattern
     * @param tint the tint if the color is a spot color, ignored otherwise
     */
    public void setPatternFill(final PdfPatternPainter p, final BaseColor color, final float tint) {
        checkWriter();
        if (!p.isStencil())
            throw new RuntimeException(MessageLocalization.getComposedMessage("an.uncolored.pattern.was.expected"));
        PageResources prs = getPageResources();
        PdfName name = writer.addSimplePattern(p);
        name = prs.addPattern(name, p.getIndirectReference());
        ColorDetails csDetail = writer.addSimplePatternColorspace(color);
        PdfName cName = prs.addColor(csDetail.getColorName(), csDetail.getIndirectReference());
        content.append(cName.getBytes()).append(" cs").append_i(separator);
        outputColorNumbers(color, tint);
        content.append(' ').append(name.getBytes()).append(" scn").append_i(separator);
    }

    /** Sets the stroke color to an uncolored pattern.
     * @param p the pattern
     * @param color the color of the pattern
     */
    public void setPatternStroke(final PdfPatternPainter p, final BaseColor color) {
        if (ExtendedColor.getType(color) == ExtendedColor.TYPE_SEPARATION)
            setPatternStroke(p, color, ((SpotColor)color).getTint());
        else
            setPatternStroke(p, color, 0);
    }

    /** Sets the stroke color to an uncolored pattern.
     * @param p the pattern
     * @param color the color of the pattern
     * @param tint the tint if the color is a spot color, ignored otherwise
     */
    public void setPatternStroke(final PdfPatternPainter p, final BaseColor color, final float tint) {
        checkWriter();
        if (!p.isStencil())
            throw new RuntimeException(MessageLocalization.getComposedMessage("an.uncolored.pattern.was.expected"));
        PageResources prs = getPageResources();
        PdfName name = writer.addSimplePattern(p);
        name = prs.addPattern(name, p.getIndirectReference());
        ColorDetails csDetail = writer.addSimplePatternColorspace(color);
        PdfName cName = prs.addColor(csDetail.getColorName(), csDetail.getIndirectReference());
        content.append(cName.getBytes()).append(" CS").append_i(separator);
        outputColorNumbers(color, tint);
        content.append(' ').append(name.getBytes()).append(" SCN").append_i(separator);
    }

    /** Sets the stroke color to a pattern. The pattern can be
     * colored or uncolored.
     * @param p the pattern
     */
    public void setPatternStroke(final PdfPatternPainter p) {
        if (p.isStencil()) {
            setPatternStroke(p, p.getDefaultColor());
            return;
        }
        checkWriter();
        PageResources prs = getPageResources();
        PdfName name = writer.addSimplePattern(p);
        name = prs.addPattern(name, p.getIndirectReference());
        content.append(PdfName.PATTERN.getBytes()).append(" CS ").append(name.getBytes()).append(" SCN").append_i(separator);
    }

    /**
     * Paints using a shading object.
     * @param shading the shading object
     */
    public void paintShading(final PdfShading shading) {
        writer.addSimpleShading(shading);
        PageResources prs = getPageResources();
        PdfName name = prs.addShading(shading.getShadingName(), shading.getShadingReference());
        content.append(name.getBytes()).append(" sh").append_i(separator);
        ColorDetails details = shading.getColorDetails();
        if (details != null)
            prs.addColor(details.getColorName(), details.getIndirectReference());
    }

    /**
     * Paints using a shading pattern.
     * @param shading the shading pattern
     */
    public void paintShading(final PdfShadingPattern shading) {
        paintShading(shading.getShading());
    }

    /**
     * Sets the shading fill pattern.
     * @param shading the shading pattern
     */
    public void setShadingFill(final PdfShadingPattern shading) {
        writer.addSimpleShadingPattern(shading);
        PageResources prs = getPageResources();
        PdfName name = prs.addPattern(shading.getPatternName(), shading.getPatternReference());
        content.append(PdfName.PATTERN.getBytes()).append(" cs ").append(name.getBytes()).append(" scn").append_i(separator);
        ColorDetails details = shading.getColorDetails();
        if (details != null)
            prs.addColor(details.getColorName(), details.getIndirectReference());
    }

    /**
     * Sets the shading stroke pattern
     * @param shading the shading pattern
     */
    public void setShadingStroke(final PdfShadingPattern shading) {
        writer.addSimpleShadingPattern(shading);
        PageResources prs = getPageResources();
        PdfName name = prs.addPattern(shading.getPatternName(), shading.getPatternReference());
        content.append(PdfName.PATTERN.getBytes()).append(" CS ").append(name.getBytes()).append(" SCN").append_i(separator);
        ColorDetails details = shading.getColorDetails();
        if (details != null)
            prs.addColor(details.getColorName(), details.getIndirectReference());
    }

    /** Check if we have a valid PdfWriter.
     *
     */
    protected void checkWriter() {
        if (writer == null)
            throw new NullPointerException(MessageLocalization.getComposedMessage("the.writer.in.pdfcontentbyte.is.null"));
    }

    /**
     * Show an array of text.
     * @param text array of text
     */
    public void showText(final PdfTextArray text) {
        if (!inText && autoControlTextBlocks) {
            beginText(true);
        }
        if (state.fontDetails == null)
            throw new NullPointerException(MessageLocalization.getComposedMessage("font.and.size.must.be.set.before.writing.any.text"));
        if (writer.isTagged())
            beginMarkedContentSequence(new PdfStructureElement(getParentStructureElement(), PdfName.SPAN));
        content.append("[");
        ArrayList<Object> arrayList = text.getArrayList();
        boolean lastWasNumber = false;
        for (Object obj : arrayList) {
            if (obj instanceof String) {
                showText2((String)obj);
                updateTx((String)obj, 0);
                lastWasNumber = false;
            }
            else {
                if (lastWasNumber)
                    content.append(' ');
                else
                    lastWasNumber = true;
                content.append(((Float)obj).floatValue());
                updateTx("", ((Float)obj).floatValue());
            }
        }
        content.append("]TJ").append_i(separator);
        if (writer.isTagged())
            endMarkedContentSequence();
    }

    /**
     * Gets the <CODE>PdfWriter</CODE> in use by this object.
     * @return the <CODE>PdfWriter</CODE> in use by this object
     */
    public PdfWriter getPdfWriter() {
        return writer;
    }

    /**
     * Gets the <CODE>PdfDocument</CODE> in use by this object.
     * @return the <CODE>PdfDocument</CODE> in use by this object
     */
    public PdfDocument getPdfDocument() {
        return pdf;
    }

    /**
     * Implements a link to other part of the document. The jump will
     * be made to a local destination with the same name, that must exist.
     * @param name the name for this link
     * @param llx the lower left x corner of the activation area
     * @param lly the lower left y corner of the activation area
     * @param urx the upper right x corner of the activation area
     * @param ury the upper right y corner of the activation area
     */
    public void localGoto(final String name, final float llx, final float lly, final float urx, final float ury) {
        pdf.localGoto(name, llx, lly, urx, ury);
    }

    /**
     * The local destination to where a local goto with the same
     * name will jump.
     * @param name the name of this local destination
     * @param destination the <CODE>PdfDestination</CODE> with the jump coordinates
     * @return <CODE>true</CODE> if the local destination was added,
     * <CODE>false</CODE> if a local destination with the same name
     * already exists
     */
    public boolean localDestination(final String name, final PdfDestination destination) {
        return pdf.localDestination(name, destination);
    }

    /**
     * Gets a duplicate of this <CODE>PdfContentByte</CODE>. All
     * the members are copied by reference but the buffer stays different.
     *
     * @return a copy of this <CODE>PdfContentByte</CODE>
     */
    public PdfContentByte getDuplicate() {
        PdfContentByte cb = new PdfContentByte(writer);
        cb.duplicatedFrom = this;
        return cb;
    }

    /**
     * Implements a link to another document.
     * @param filename the filename for the remote document
     * @param name the name to jump to
     * @param llx the lower left x corner of the activation area
     * @param lly the lower left y corner of the activation area
     * @param urx the upper right x corner of the activation area
     * @param ury the upper right y corner of the activation area
     */
    public void remoteGoto(final String filename, final String name, final float llx, final float lly, final float urx, final float ury) {
        pdf.remoteGoto(filename, name, llx, lly, urx, ury);
    }

    /**
     * Implements a link to another document.
     * @param filename the filename for the remote document
     * @param page the page to jump to
     * @param llx the lower left x corner of the activation area
     * @param lly the lower left y corner of the activation area
     * @param urx the upper right x corner of the activation area
     * @param ury the upper right y corner of the activation area
     */
    public void remoteGoto(final String filename, final int page, final float llx, final float lly, final float urx, final float ury) {
        pdf.remoteGoto(filename, page, llx, lly, urx, ury);
    }
    /**
     * Adds a round rectangle to the current path.
     *
     * @param x x-coordinate of the starting point
     * @param y y-coordinate of the starting point
     * @param w width
     * @param h height
     * @param r radius of the arc corner
     */
    public void roundRectangle(float x, float y, float w, float h, float r) {
        if (w < 0) {
            x += w;
            w = -w;
        }
        if (h < 0) {
            y += h;
            h = -h;
        }
        if (r < 0)
            r = -r;
        float b = 0.4477f;
        moveTo(x + r, y);
        lineTo(x + w - r, y);
        curveTo(x + w - r * b, y, x + w, y + r * b, x + w, y + r);
        lineTo(x + w, y + h - r);
        curveTo(x + w, y + h - r * b, x + w - r * b, y + h, x + w - r, y + h);
        lineTo(x + r, y + h);
        curveTo(x + r * b, y + h, x, y + h - r * b, x, y + h - r);
        lineTo(x, y + r);
        curveTo(x, y + r * b, x + r * b, y, x + r, y);
    }

    /** Implements an action in an area.
     * @param action the <CODE>PdfAction</CODE>
     * @param llx the lower left x corner of the activation area
     * @param lly the lower left y corner of the activation area
     * @param urx the upper right x corner of the activation area
     * @param ury the upper right y corner of the activation area
     */
    public void setAction(final PdfAction action, final float llx, final float lly, final float urx, final float ury) {
        pdf.setAction(action, llx, lly, urx, ury);
    }

    /** Outputs a <CODE>String</CODE> directly to the content.
     * @param s the <CODE>String</CODE>
     */
    public void setLiteral(final String s) {
        content.append(s);
    }

    /** Outputs a <CODE>char</CODE> directly to the content.
     * @param c the <CODE>char</CODE>
     */
    public void setLiteral(final char c) {
        content.append(c);
    }

    /** Outputs a <CODE>float</CODE> directly to the content.
     * @param n the <CODE>float</CODE>
     */
    public void setLiteral(final float n) {
        content.append(n);
    }

    /** Throws an error if it is a pattern.
     * @param t the object to check
     */
    void checkNoPattern(final PdfTemplate t) {
        if (t.getType() == PdfTemplate.TYPE_PATTERN)
            throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.use.of.a.pattern.a.template.was.expected"));
    }

    /**
     * Draws a TextField.
     * @param llx
     * @param lly
     * @param urx
     * @param ury
     * @param on
     */
    public void drawRadioField(float llx, float lly, float urx, float ury, final boolean on) {
        if (llx > urx) { float x = llx; llx = urx; urx = x; }
        if (lly > ury) { float y = lly; lly = ury; ury = y; }
        saveState();
        // silver circle
        setLineWidth(1);
        setLineCap(1);
        setColorStroke(new BaseColor(0xC0, 0xC0, 0xC0));
        arc(llx + 1f, lly + 1f, urx - 1f, ury - 1f, 0f, 360f);
        stroke();
        // gray circle-segment
        setLineWidth(1);
        setLineCap(1);
        setColorStroke(new BaseColor(0xA0, 0xA0, 0xA0));
        arc(llx + 0.5f, lly + 0.5f, urx - 0.5f, ury - 0.5f, 45, 180);
        stroke();
        // black circle-segment
        setLineWidth(1);
        setLineCap(1);
        setColorStroke(new BaseColor(0x00, 0x00, 0x00));
        arc(llx + 1.5f, lly + 1.5f, urx - 1.5f, ury - 1.5f, 45, 180);
        stroke();
        if (on) {
            // gray circle
            setLineWidth(1);
            setLineCap(1);
            setColorFill(new BaseColor(0x00, 0x00, 0x00));
            arc(llx + 4f, lly + 4f, urx - 4f, ury - 4f, 0, 360);
            fill();
        }
        restoreState();
    }

    /**
     * Draws a TextField.
     * @param llx
     * @param lly
     * @param urx
     * @param ury
     */
    public void drawTextField(float llx, float lly, float urx, float ury) {
        if (llx > urx) { float x = llx; llx = urx; urx = x; }
        if (lly > ury) { float y = lly; lly = ury; ury = y; }
        // silver rectangle not filled
        saveState();
        setColorStroke(new BaseColor(0xC0, 0xC0, 0xC0));
        setLineWidth(1);
        setLineCap(0);
        rectangle(llx, lly, urx - llx, ury - lly);
        stroke();
        // white rectangle filled
        setLineWidth(1);
        setLineCap(0);
        setColorFill(new BaseColor(0xFF, 0xFF, 0xFF));
        rectangle(llx + 0.5f, lly + 0.5f, urx - llx - 1f, ury -lly - 1f);
        fill();
        // silver lines
        setColorStroke(new BaseColor(0xC0, 0xC0, 0xC0));
        setLineWidth(1);
        setLineCap(0);
        moveTo(llx + 1f, lly + 1.5f);
        lineTo(urx - 1.5f, lly + 1.5f);
        lineTo(urx - 1.5f, ury - 1f);
        stroke();
        // gray lines
        setColorStroke(new BaseColor(0xA0, 0xA0, 0xA0));
        setLineWidth(1);
        setLineCap(0);
        moveTo(llx + 1f, lly + 1);
        lineTo(llx + 1f, ury - 1f);
        lineTo(urx - 1f, ury - 1f);
        stroke();
        // black lines
        setColorStroke(new BaseColor(0x00, 0x00, 0x00));
        setLineWidth(1);
        setLineCap(0);
        moveTo(llx + 2f, lly + 2f);
        lineTo(llx + 2f, ury - 2f);
        lineTo(urx - 2f, ury - 2f);
        stroke();
        restoreState();
    }

    /**
     * Draws a button.
     * @param llx
     * @param lly
     * @param urx
     * @param ury
     * @param text
     * @param bf
     * @param size
     */
    public void drawButton(float llx, float lly, float urx, float ury, final String text, final BaseFont bf, final float size) {
        if (llx > urx) { float x = llx; llx = urx; urx = x; }
        if (lly > ury) { float y = lly; lly = ury; ury = y; }
        // black rectangle not filled
        saveState();
        setColorStroke(new BaseColor(0x00, 0x00, 0x00));
        setLineWidth(1);
        setLineCap(0);
        rectangle(llx, lly, urx - llx, ury - lly);
        stroke();
        // silver rectangle filled
        setLineWidth(1);
        setLineCap(0);
        setColorFill(new BaseColor(0xC0, 0xC0, 0xC0));
        rectangle(llx + 0.5f, lly + 0.5f, urx - llx - 1f, ury -lly - 1f);
        fill();
        // white lines
        setColorStroke(new BaseColor(0xFF, 0xFF, 0xFF));
        setLineWidth(1);
        setLineCap(0);
        moveTo(llx + 1f, lly + 1f);
        lineTo(llx + 1f, ury - 1f);
        lineTo(urx - 1f, ury - 1f);
        stroke();
        // dark grey lines
        setColorStroke(new BaseColor(0xA0, 0xA0, 0xA0));
        setLineWidth(1);
        setLineCap(0);
        moveTo(llx + 1f, lly + 1f);
        lineTo(urx - 1f, lly + 1f);
        lineTo(urx - 1f, ury - 1f);
        stroke();
        // text
        resetRGBColorFill();
        beginText();
        setFontAndSize(bf, size);
        showTextAligned(PdfContentByte.ALIGN_CENTER, text, llx + (urx - llx) / 2, lly + (ury - lly - size) / 2, 0);
        endText();
        restoreState();
    }

    PageResources getPageResources() {
        return pdf.getPageResources();
    }

    /** Sets the graphic state
     * @param gstate the graphic state
     */
    public void setGState(final PdfGState gstate) {
        PdfObject obj[] = writer.addSimpleExtGState(gstate);
        PageResources prs = getPageResources();
        PdfName name = prs.addExtGState((PdfName)obj[0], (PdfIndirectReference)obj[1]);
        content.append(name.getBytes()).append(" gs").append_i(separator);
    }

    /**
     * Begins a graphic block whose visibility is controlled by the <CODE>layer</CODE>.
     * Blocks can be nested. Each block must be terminated by an {@link #endLayer()}.<p>
     * Note that nested layers with {@link PdfLayer#addChild(PdfLayer)} only require a single
     * call to this method and a single call to {@link #endLayer()}; all the nesting control
     * is built in.
     * @param layer the layer
     */
    public void beginLayer(final PdfOCG layer) {
        if (layer instanceof PdfLayer && ((PdfLayer)layer).getTitle() != null)
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("a.title.is.not.a.layer"));
        if (layerDepth == null)
            layerDepth = new ArrayList<Integer>();
        if (layer instanceof PdfLayerMembership) {
            layerDepth.add(Integer.valueOf(1));
            beginLayer2(layer);
            return;
        }
        int n = 0;
        PdfLayer la = (PdfLayer)layer;
        while (la != null) {
            if (la.getTitle() == null) {
                beginLayer2(la);
                ++n;
            }
            la = la.getParent();
        }
        layerDepth.add(Integer.valueOf(n));
    }

    private void beginLayer2(final PdfOCG layer) {
        PdfName name = (PdfName)writer.addSimpleProperty(layer, layer.getRef())[0];
        PageResources prs = getPageResources();
        name = prs.addProperty(name, layer.getRef());
        content.append("/OC ").append(name.getBytes()).append(" BDC").append_i(separator);
    }

    /**
     * Ends a layer controlled graphic block. It will end the most recent open block.
     */
    public void endLayer() {
        int n = 1;
        if (layerDepth != null && !layerDepth.isEmpty()) {
            n = layerDepth.get(layerDepth.size() - 1).intValue();
            layerDepth.remove(layerDepth.size() - 1);
        } else {
        	throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.layer.operators"));
        }
        while (n-- > 0)
            content.append("EMC").append_i(separator);
    }

    /** Concatenates a transformation to the current transformation
     * matrix.
     * @param af the transformation
     */
    public void transform(final AffineTransform af) {
        if (inText && autoControlTextBlocks) {
            endText();
        }
        double matrix[] = new double[6];
        af.getMatrix(matrix);
        content.append(matrix[0]).append(' ').append(matrix[1]).append(' ').append(matrix[2]).append(' ');
        content.append(matrix[3]).append(' ').append(matrix[4]).append(' ').append(matrix[5]).append(" cm").append_i(separator);
    }

    void addAnnotation(final PdfAnnotation annot) {
        writer.addAnnotation(annot);
    }

    /**
     * Sets the default colorspace.
     * @param name the name of the colorspace. It can be <CODE>PdfName.DEFAULTGRAY</CODE>, <CODE>PdfName.DEFAULTRGB</CODE>
     * or <CODE>PdfName.DEFAULTCMYK</CODE>
     * @param obj the colorspace. A <CODE>null</CODE> or <CODE>PdfNull</CODE> removes any colorspace with the same name
     */
    public void setDefaultColorspace(final PdfName name, final PdfObject obj) {
        PageResources prs = getPageResources();
        prs.addDefaultColor(name, obj);
    }

    /**
     * Begins a marked content sequence. This sequence will be tagged with the structure <CODE>struc</CODE>.
     * The same structure can be used several times to connect text that belongs to the same logical segment
     * but is in a different location, like the same paragraph crossing to another page, for example.
     * @param struc the tagging structure
     */
    public void beginMarkedContentSequence(final PdfStructureElement struc) {
        PdfObject obj = struc.get(PdfName.K);
        int mark = pdf.getMarkPoint();
        if (obj != null) {
            PdfArray ar = null;
            if (obj.isNumber()) {
                ar = new PdfArray();
                ar.add(obj);
                struc.put(PdfName.K, ar);
            }
            else if (obj.isArray()) {
                ar = (PdfArray)obj;
                if (!ar.getPdfObject(0).isNumber())
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.structure.has.kids"));
            }
            else
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("unknown.object.at.k.1", obj.getClass().toString()));
            PdfDictionary dic = new PdfDictionary(PdfName.MCR);
            dic.put(PdfName.PG, writer.getCurrentPage());
            dic.put(PdfName.MCID, new PdfNumber(mark));
            ar.add(dic);
            struc.setPageMark(writer.getPageNumber() - 1, -1);
        }
        else {
            struc.setPageMark(writer.getPageNumber() - 1, mark);
            struc.put(PdfName.PG, writer.getCurrentPage());
        }
        pdf.incMarkPoint();
        setMcDepth(getMcDepth() + 1);
        content.append(struc.get(PdfName.S).getBytes()).append(" <</MCID ").append(mark).append(">> BDC").append_i(separator);
    }

    /**
     * Ends a marked content sequence
     */
    public void endMarkedContentSequence() {
    	if (getMcDepth() == 0) {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.begin.end.marked.content.operators"));
    	}
    	setMcDepth(getMcDepth() - 1);
        content.append("EMC").append_i(separator);
    }

    /**
     * Begins a marked content sequence. If property is <CODE>null</CODE> the mark will be of the type
     * <CODE>BMC</CODE> otherwise it will be <CODE>BDC</CODE>.
     * @param tag the tag
     * @param property the property
     * @param inline <CODE>true</CODE> to include the property in the content or <CODE>false</CODE>
     * to include the property in the resource dictionary with the possibility of reusing
     */
    public void beginMarkedContentSequence(final PdfName tag, final PdfDictionary property, final boolean inline) {
        if (property == null) {
            content.append(tag.getBytes()).append(" BMC").append_i(separator);
            setMcDepth(getMcDepth() + 1);
            return;
        }
        content.append(tag.getBytes()).append(' ');
        if (inline)
            try {
                property.toPdf(writer, content);
            }
            catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        else {
            PdfObject[] objs;
            if (writer.propertyExists(property))
                objs = writer.addSimpleProperty(property, null);
            else
                objs = writer.addSimpleProperty(property, writer.getPdfIndirectReference());
            PdfName name = (PdfName)objs[0];
            PageResources prs = getPageResources();
            name = prs.addProperty(name, (PdfIndirectReference)objs[1]);
            content.append(name.getBytes());
        }
        content.append(" BDC").append_i(separator);
        setMcDepth(getMcDepth() + 1);
    }

    /**
     * This is just a shorthand to <CODE>beginMarkedContentSequence(tag, null, false)</CODE>.
     * @param tag the tag
     */
    public void beginMarkedContentSequence(final PdfName tag) {
        beginMarkedContentSequence(tag, null, false);
    }

    /**
     * Checks for any dangling state: Mismatched save/restore state, begin/end text,
     * begin/end layer, or begin/end marked content sequence.
     * If found, this function will throw.  This function is called automatically
     * during a reset() (from Document.newPage() for example), and before writing
     * itself out in toPdf().
     * One possible cause: not calling myPdfGraphics2D.dispose() will leave dangling
     *                     saveState() calls.
     * @since 2.1.6
     * @throws IllegalPdfSyntaxException (a runtime exception)
     */
    public void sanityCheck() {
    	if (getMcDepth() != 0) {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.marked.content.operators"));
    	}
    	if (inText) {
            if (autoControlTextBlocks) {
                endText();
            } else {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.begin.end.text.operators"));
    	}
    	}
    	if (layerDepth != null && !layerDepth.isEmpty()) {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.layer.operators"));
    	}
    	if (!stateList.isEmpty()) {
    		throw new IllegalPdfSyntaxException(MessageLocalization.getComposedMessage("unbalanced.save.restore.state.operators"));
    	}
    }

    // AWT related methods (remove this if you port to Android / GAE)
    
    /** Gets a <CODE>Graphics2D</CODE> to write on. The graphics
     * are translated to PDF commands as shapes. No PDF fonts will appear.
     * @param width the width of the panel
     * @param height the height of the panel
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfGraphics2D
     */
    public java.awt.Graphics2D createGraphicsShapes(final float width, final float height) {
        return new PdfGraphics2D(this, width, height, true);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands as shapes. No PDF fonts will appear.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param printerJob a printer job
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createPrinterGraphicsShapes(final float width, final float height, final java.awt.print.PrinterJob printerJob) {
        return new PdfPrinterGraphics2D(this, width, height, true, printerJob);
    }

    /** Gets a <CODE>Graphics2D</CODE> to write on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfGraphics2D
     */
    public java.awt.Graphics2D createGraphics(final float width, final float height) {
        return new PdfGraphics2D(this, width, height);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param printerJob
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createPrinterGraphics(final float width, final float height, final java.awt.print.PrinterJob printerJob) {
        return new PdfPrinterGraphics2D(this, width, height, printerJob);
    }

    /** Gets a <CODE>Graphics2D</CODE> to write on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param convertImagesToJPEG
     * @param quality
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfGraphics2D
     */
    public java.awt.Graphics2D createGraphics(final float width, final float height, final boolean convertImagesToJPEG, final float quality) {
        return new PdfGraphics2D(this, width, height, null, false, convertImagesToJPEG, quality);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param convertImagesToJPEG
     * @param quality
     * @param printerJob
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfGraphics2D
     */
    public java.awt.Graphics2D createPrinterGraphics(final float width, final float height, final boolean convertImagesToJPEG, final float quality, final java.awt.print.PrinterJob printerJob) {
        return new PdfPrinterGraphics2D(this, width, height, null, false, convertImagesToJPEG, quality, printerJob);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands.
     * @param width
     * @param height
     * @param convertImagesToJPEG
     * @param quality
     * @return A Graphics2D object
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createGraphicsShapes(final float width, final float height, final boolean convertImagesToJPEG, final float quality) {
        return new PdfGraphics2D(this, width, height, null, true, convertImagesToJPEG, quality);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands.
     * @param width
     * @param height
     * @param convertImagesToJPEG
     * @param quality
     * @param printerJob
     * @return a Graphics2D object
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createPrinterGraphicsShapes(final float width, final float height, final boolean convertImagesToJPEG, final float quality, final java.awt.print.PrinterJob printerJob) {
        return new PdfPrinterGraphics2D(this, width, height, null, true, convertImagesToJPEG, quality, printerJob);
    }

    /** Gets a <CODE>Graphics2D</CODE> to write on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param fontMapper the mapping from awt fonts to <CODE>BaseFont</CODE>
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createGraphics(final float width, final float height, final FontMapper fontMapper) {
        return new PdfGraphics2D(this, width, height, fontMapper);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param fontMapper the mapping from awt fonts to <CODE>BaseFont</CODE>
     * @param printerJob a printer job
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createPrinterGraphics(final float width, final float height, final FontMapper fontMapper, final java.awt.print.PrinterJob printerJob) {
        return new PdfPrinterGraphics2D(this, width, height, fontMapper, printerJob);
    }

    /** Gets a <CODE>Graphics2D</CODE> to write on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param fontMapper the mapping from awt fonts to <CODE>BaseFont</CODE>
     * @param convertImagesToJPEG converts awt images to jpeg before inserting in pdf
     * @param quality the quality of the jpeg
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createGraphics(final float width, final float height, final FontMapper fontMapper, final boolean convertImagesToJPEG, final float quality) {
        return new PdfGraphics2D(this, width, height, fontMapper, false, convertImagesToJPEG, quality);
    }

    /** Gets a <CODE>Graphics2D</CODE> to print on. The graphics
     * are translated to PDF commands.
     * @param width the width of the panel
     * @param height the height of the panel
     * @param fontMapper the mapping from awt fonts to <CODE>BaseFont</CODE>
     * @param convertImagesToJPEG converts awt images to jpeg before inserting in pdf
     * @param quality the quality of the jpeg
     * @param printerJob a printer job
     * @return a <CODE>Graphics2D</CODE>
     * @deprecated use the constructor in PdfPrinterGraphics2D
     */
    public java.awt.Graphics2D createPrinterGraphics(final float width, final float height, final FontMapper fontMapper, final boolean convertImagesToJPEG, final float quality, final java.awt.print.PrinterJob printerJob) {
        return new PdfPrinterGraphics2D(this, width, height, fontMapper, false, convertImagesToJPEG, quality, printerJob);
    }
    
    /**
     * adds an image with the given matrix.
     * @param image image to add
     * @param transform transform to apply to the template prior to adding it.
     * @since 5.0.1
     * @deprecated use com.itextpdf.text.geom.AffineTransform as parameter
     */
    public void addImage(final Image image, final java.awt.geom.AffineTransform transform) throws DocumentException {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	addImage(image, new AffineTransform(matrix));
    }
    
    /**
     * adds a template with the given matrix.
     * @param template template to add
     * @param transform transform to apply to the template prior to adding it.
     * @deprecated use com.itextpdf.text.geom.AffineTransform as parameter
     */
    public void addTemplate(final PdfTemplate template, final java.awt.geom.AffineTransform transform) {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	addTemplate(template, new AffineTransform(matrix));
    }
    
    /**
     * Concatenate a matrix to the current transformation matrix.
     * @param transform added to the Current Transformation Matrix
     * @deprecated use com.itextpdf.text.geom.AffineTransform as parameter
     */
    public void concatCTM(final java.awt.geom.AffineTransform transform) {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	concatCTM(new AffineTransform(matrix));
    }
    
    /**
     * Changes the text matrix.
     * <P>
     * @param transform overwrite the current text matrix with this one
     * @deprecated use com.itextpdf.text.geom.AffineTransform as parameter
     */
    public void setTextMatrix(final java.awt.geom.AffineTransform transform) {
    	double matrix[] = new double[6];
    	transform.getMatrix(matrix);
    	setTextMatrix(new AffineTransform(matrix));
    }

    /** Concatenates a transformation to the current transformation
     * matrix.
     * @param af the transformation
     * @deprecated use com.itextpdf.text.geom.AffineTransform as parameter
     */
    public void transform(final java.awt.geom.AffineTransform af) {
        double matrix[] = new double[6];
        af.getMatrix(matrix);
        transform(new AffineTransform(matrix));
    }

    protected void openMCBlock(Element element) {
        if (writer.isTagged()) {
            if (!getMcElements().contains(element)) {
                PdfStructureElement structureElement = openMCBlockInt(element);
                getMcElements().add(element);
                pdf.structElements.put(element, structureElement);
            }
        }
    }

    private PdfDictionary getParentStructureElement() {
        PdfDictionary parent = null;
        if (getMcElements().size() > 0)
            parent = pdf.structElements.get(getMcElements().get(getMcElements().size() - 1));
        if (parent == null) {
            parent = writer.getStructureTreeRoot();
        }
        return parent;
    }

    private IPdfStructureElement getParentStructureInterface() {
        if (getMcElements().size() > 0)
            return pdf.structElements.get(getMcElements().get(getMcElements().size() - 1));
        else
            return writer.getStructureTreeRoot();
    }

    private PdfStructureElement openMCBlockInt(Element element) {
        PdfStructureElement structureElement = null;
        if (writer.isTagged()) {
            if (element instanceof Paragraph) {
                structureElement = pdf.structElements.get(element);
                if (structureElement == null) {
                    structureElement = new PdfStructureElement(getParentStructureElement(), PdfName.P);
                  Paragraph p = (Paragraph) element;

                    // Setting non-inheritable attributes
                    if ((p.getFont() != null) && (p.getFont().getColor() != null)){
                        BaseColor c = p.getFont().getColor();
                        float [] colors = new float[] {c.getRed()/255, c.getGreen()/255, c.getBlue()/255};
                        structureElement.setAttribute(PdfName.COLOR, new PdfArray(colors));
                    }
                    if (Float.compare(p.getSpacingBefore(), 0f) != 0)
                        structureElement.setAttribute(PdfName.SPACEBEFORE, new PdfNumber(p.getSpacingBefore()));
                    if (Float.compare(p.getSpacingAfter(), 0f) != 0)
                        structureElement.setAttribute(PdfName.SPACEAFTER, new PdfNumber(p.getSpacingAfter()));
                    if (Float.compare(p.getFirstLineIndent(), 0f) != 0)
                        structureElement.setAttribute(PdfName.TEXTINDENT, new PdfNumber(p.getFirstLineIndent()));

                    // Setting inheritable attributes
                    IPdfStructureElement parent = getParentStructureInterface();
                    PdfObject obj = parent.getAttribute(PdfName.STARTINDENT);
                    if (obj instanceof PdfNumber) {
                        float startIndent = ((PdfNumber) obj).floatValue();
                        if (Float.compare(startIndent, p.getIndentationLeft()) != 0)
                            structureElement.setAttribute(PdfName.STARTINDENT, new PdfNumber(p.getIndentationLeft()));
                    }
                    else {
                        if (Math.abs(p.getIndentationLeft()) > Float.MIN_VALUE)
                            structureElement.setAttribute(PdfName.STARTINDENT, new PdfNumber(p.getIndentationLeft()));
                    }

                    obj = parent.getAttribute(PdfName.ENDINDENT);
                    if (obj instanceof PdfNumber) {
                        float endIndent = ((PdfNumber) obj).floatValue();
                        if (Float.compare(endIndent, p.getIndentationRight()) != 0)
                            structureElement.setAttribute(PdfName.ENDINDENT, new PdfNumber(p.getIndentationRight()));
                    }
                    else {
                        if (Float.compare(p.getIndentationRight(), 0) != 0)
                            structureElement.setAttribute(PdfName.ENDINDENT, new PdfNumber(p.getIndentationRight()));
                    }

                    PdfName align = null;
                    switch (p.getAlignment()){
                        case Element.ALIGN_LEFT:
                            align = PdfName.START;
                            break;
                        case Element.ALIGN_CENTER:
                            align = PdfName.CENTER;
                            break;
                        case Element.ALIGN_RIGHT:
                            align = PdfName.END;
                            break;
                        case Element.ALIGN_JUSTIFIED:
                            align = PdfName.JUSTIFY;
                            break;
                    }
                    obj = parent.getAttribute(PdfName.TEXTALIGN);
                    if (obj instanceof PdfName) {
                        PdfName textAlign = ((PdfName) obj);
                        if (align != null && !textAlign.equals(align))
                            structureElement.setAttribute(PdfName.TEXTALIGN, align);
                    }
                    else {
                        if (align != null && !PdfName.START.equals(align))
                            structureElement.setAttribute(PdfName.TEXTALIGN, align);
                    }

                }
                if (inText && autoControlTextBlocks) {
                    endText();
                }
                beginMarkedContentSequence(structureElement);
            }
        }
        return structureElement;
    }

    protected void closeMCBlock(Element element) {
        if (writer.isTagged()) {
            if (getMcElements().contains(element)) {
                closeMCBlockInt(element);
                getMcElements().remove(element);
                pdf.structElements.remove(element);
            }
        }
    }

    private void closeMCBlockInt(Element element) {
        if (writer.isTagged()) {
            if (element instanceof Paragraph) {
                if (inText && autoControlTextBlocks)
                    endText();
                endMarkedContentSequence();
            }
        }
    }

    protected ArrayList<Element> saveMCBlocks() {
        ArrayList<Element> mc = new ArrayList<Element>();
        if (writer.isTagged()) {
            mc = getMcElements();
            for (int i = 0; i < mc.size(); i++) {
                closeMCBlockInt(mc.get(i));
            }
            setMcElements(new ArrayList<Element>());
        }
        return mc;
    }

    protected void restoreMCBlocks(ArrayList<Element> mcElements) {
        if (writer.isTagged() && mcElements != null) {
            setMcElements(mcElements);
            for (int i = 0; i < this.getMcElements().size(); i++) {
                openMCBlockInt(this.getMcElements().get(i));
            }
        }
    }

    protected int getMcDepth() {
        if (duplicatedFrom != null)
            return duplicatedFrom.getMcDepth();
        else
            return mcDepth;
    }

    protected void setMcDepth(int value) {
        if (duplicatedFrom != null)
            duplicatedFrom.setMcDepth(value);
        else
            mcDepth = value;
    }

    protected ArrayList<Element> getMcElements() {
        if (duplicatedFrom != null)
            return duplicatedFrom.getMcElements();
        else
            return mcElements;
    }

    protected void setMcElements(ArrayList<Element> value) {
        if (duplicatedFrom != null)
            duplicatedFrom.setMcElements(value);
        else
            mcElements = value;
    }

    protected void updateTx(String text, float Tj) {
        state.tx = state.tx + getEffectiveStringWidth(text, false) + (-Tj / 1000.f * state.size + state.charSpace + state.wordSpace) * state.scale / 100.f;
    }

}