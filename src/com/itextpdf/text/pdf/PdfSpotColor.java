/*
 * $Id: PdfSpotColor.java 4784 2011-03-15 08:33:00Z blowagie $
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 1998-2011 1T3XT BVBA
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

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.error_messages.MessageLocalization;

/**
 * A <CODE>PdfSpotColor</CODE> defines a ColorSpace
 *
 * @see		PdfDictionary
 */

public class PdfSpotColor{
    
/**	The color name */
    public PdfName name;
    
/** The alternative color space */
    public BaseColor altcs;
    // constructors
    
    /**
     * Constructs a new <CODE>PdfSpotColor</CODE>.
     *
     * @param		name		a String value
     * @param		altcs		an alternative colorspace value
     */
    
    public PdfSpotColor(String name, BaseColor altcs) {
        this.name = new PdfName(name);
        this.altcs = altcs;
    }
    
    /**
     * Gets the alternative ColorSpace.
     * @return a BaseColor
     */
    public BaseColor getAlternativeCS() {
        return altcs;
    }
    
    protected PdfObject getSpotObject(PdfWriter writer) {
        PdfArray array = new PdfArray(PdfName.SEPARATION);
        array.add(name);
        PdfFunction func = null;
        if (altcs instanceof ExtendedColor) {
            int type = ((ExtendedColor)altcs).type;
            switch (type) {
                case ExtendedColor.TYPE_GRAY:
                    array.add(PdfName.DEVICEGRAY);
                    func = PdfFunction.type2(writer, new float[]{0, 1}, null, new float[]{0}, new float[]{((GrayColor)altcs).getGray()}, 1);
                    break;
                case ExtendedColor.TYPE_CMYK:
                    array.add(PdfName.DEVICECMYK);
                    CMYKColor cmyk = (CMYKColor)altcs;
                    func = PdfFunction.type2(writer, new float[]{0, 1}, null, new float[]{0, 0, 0, 0},
                        new float[]{cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack()}, 1);
                    break;
                default:
                    throw new RuntimeException(MessageLocalization.getComposedMessage("only.rgb.gray.and.cmyk.are.supported.as.alternative.color.spaces"));
            }
        }
        else {
            array.add(PdfName.DEVICERGB);
            func = PdfFunction.type2(writer, new float[]{0, 1}, null, new float[]{1, 1, 1},
                new float[]{(float)altcs.getRed() / 255, (float)altcs.getGreen() / 255, (float)altcs.getBlue() / 255}, 1);
        }
        array.add(func.getReference());
        return array;
    }
}
