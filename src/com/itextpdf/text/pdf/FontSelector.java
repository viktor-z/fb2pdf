/*
 * $Id: FontSelector.java 4784 2011-03-15 08:33:00Z blowagie $
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

import java.util.ArrayList;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Utilities;
import com.itextpdf.text.error_messages.MessageLocalization;

/** Selects the appropriate fonts that contain the glyphs needed to
 * render text correctly. The fonts are checked in order until the
 * character is found.
 * <p>
 * The built in fonts "Symbol" and "ZapfDingbats", if used, have a special encoding
 * to allow the characters to be referred by Unicode.
 * @author Paulo Soares
 */
public class FontSelector {

    protected ArrayList<Font> fonts = new ArrayList<Font>();

    /**
     * Adds a <CODE>Font</CODE> to be searched for valid characters.
     * @param font the <CODE>Font</CODE>
     */
    public void addFont(Font font) {
        if (font.getBaseFont() != null) {
            fonts.add(font);
            return;
        }
        BaseFont bf = font.getCalculatedBaseFont(true);
        Font f2 = new Font(bf, font.getSize(), font.getCalculatedStyle(), font.getColor());
        fonts.add(f2);
    }

    /**
     * Process the text so that it will render with a combination of fonts
     * if needed.
     * @param text the text
     * @return a <CODE>Phrase</CODE> with one or more chunks
     */
    public Phrase process(String text) {
        int fsize = fonts.size();
        if (fsize == 0)
            throw new IndexOutOfBoundsException(MessageLocalization.getComposedMessage("no.font.is.defined"));
        char cc[] = text.toCharArray();
        int len = cc.length;
        StringBuffer sb = new StringBuffer();
        Font font = null;
        int lastidx = -1;
        Phrase ret = new Phrase();
        for (int k = 0; k < len; ++k) {
            char c = cc[k];
            if (c == '\n' || c == '\r') {
                sb.append(c);
                continue;
            }
            if (Utilities.isSurrogatePair(cc, k)) {
                int u = Utilities.convertToUtf32(cc, k);
                for (int f = 0; f < fsize; ++f) {
                    font = fonts.get(f);
                    if (font.getBaseFont().charExists(u)) {
                        if (lastidx != f) {
                            if (sb.length() > 0 && lastidx != -1) {
                                Chunk ck = new Chunk(sb.toString(), fonts.get(lastidx));
                                ret.add(ck);
                                sb.setLength(0);
                            }
                            lastidx = f;
                        }
                        sb.append(c);
                        sb.append(cc[++k]);
                        break;
                    }
                }
            }
            else {
                for (int f = 0; f < fsize; ++f) {
                    font = fonts.get(f);
                    if (font.getBaseFont().charExists(c)) {
                        if (lastidx != f) {
                            if (sb.length() > 0 && lastidx != -1) {
                                Chunk ck = new Chunk(sb.toString(), fonts.get(lastidx));
                                ret.add(ck);
                                sb.setLength(0);
                            }
                            lastidx = f;
                        }
                        sb.append(c);
                        break;
                    }
                }
            }
        }
        if (sb.length() > 0) {
            Chunk ck = new Chunk(sb.toString(), fonts.get(lastidx == -1 ? 0 : lastidx));
            ret.add(ck);
        }
        return ret;
    }
}
