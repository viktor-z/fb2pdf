/*
 * $Id: Paragraph.java 5120 2012-04-17 12:50:05Z eugenemark $
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
package com.itextpdf.text;

import com.itextpdf.text.api.Indentable;
import com.itextpdf.text.api.Spaceable;
import com.itextpdf.text.pdf.PdfPTable;

import java.util.ArrayList;

/**
 * A <CODE>Paragraph</CODE> is a series of <CODE>Chunk</CODE>s and/or <CODE>Phrases</CODE>.
 * <P>
 * A <CODE>Paragraph</CODE> has the same qualities of a <CODE>Phrase</CODE>, but also
 * some additional layout-parameters:
 * <UL>
 * <LI>the indentation
 * <LI>the alignment of the text
 * </UL>
 *
 * Example:
 * <BLOCKQUOTE><PRE>
 * <STRONG>Paragraph p = new Paragraph("This is a paragraph",
 *               FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLDITALIC, new Color(0, 0, 255)));</STRONG>
 * </PRE></BLOCKQUOTE>
 *
 * @see		Element
 * @see		Phrase
 * @see		ListItem
 */

public class Paragraph extends Phrase implements Indentable, Spaceable {

	// constants
	private static final long serialVersionUID = 7852314969733375514L;

    // membervariables

	/** The alignment of the text. */
    protected int alignment = Element.ALIGN_UNDEFINED;

    /** The text leading that is multiplied by the biggest font size in the line. */
    protected float multipliedLeading = 0;

    /** The indentation of this paragraph on the left side. */
    protected float indentationLeft;

    /** The indentation of this paragraph on the right side. */
    protected float indentationRight;

    /** Holds value of property firstLineIndent. */
    private float firstLineIndent = 0;

    /** The spacing before the paragraph. */
    protected float spacingBefore;

    /** The spacing after the paragraph. */
    protected float spacingAfter;

    /** Holds value of property extraParagraphSpace. */
    private float extraParagraphSpace = 0;

    /** Does the paragraph has to be kept together on 1 page. */
    protected boolean keeptogether = false;

    // constructors

    /**
     * Constructs a <CODE>Paragraph</CODE>.
     */
    public Paragraph() {
        super();
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain leading.
     *
     * @param	leading		the leading
     */
    public Paragraph(float leading) {
        super(leading);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain <CODE>Chunk</CODE>.
     *
     * @param	chunk		a <CODE>Chunk</CODE>
     */
    public Paragraph(Chunk chunk) {
        super(chunk);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain <CODE>Chunk</CODE>
     * and a certain leading.
     *
     * @param	leading		the leading
     * @param	chunk		a <CODE>Chunk</CODE>
     */
    public Paragraph(float leading, Chunk chunk) {
        super(leading, chunk);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain <CODE>String</CODE>.
     *
     * @param	string		a <CODE>String</CODE>
     */
    public Paragraph(String string) {
        super(string);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain <CODE>String</CODE>
     * and a certain <CODE>Font</CODE>.
     *
     * @param	string		a <CODE>String</CODE>
     * @param	font		a <CODE>Font</CODE>
     */
    public Paragraph(String string, Font font) {
        super(string, font);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain <CODE>String</CODE>
     * and a certain leading.
     *
     * @param	leading		the leading
     * @param	string		a <CODE>String</CODE>
     */
    public Paragraph(float leading, String string) {
        super(leading, string);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain leading, <CODE>String</CODE>
     * and <CODE>Font</CODE>.
     *
     * @param	leading		the leading
     * @param	string		a <CODE>String</CODE>
     * @param	font		a <CODE>Font</CODE>
     */
    public Paragraph(float leading, String string, Font font) {
        super(leading, string, font);
    }

    /**
     * Constructs a <CODE>Paragraph</CODE> with a certain <CODE>Phrase</CODE>.
     *
     * @param	phrase		a <CODE>Phrase</CODE>
     */
    public Paragraph(Phrase phrase) {
        super(phrase);
        if (phrase instanceof Paragraph) {
        	Paragraph p = (Paragraph)phrase;
        	setAlignment(p.alignment);
        	setLeading(phrase.getLeading(), p.multipliedLeading);
        	setIndentationLeft(p.getIndentationLeft());
        	setIndentationRight(p.getIndentationRight());
        	setFirstLineIndent(p.getFirstLineIndent());
        	setSpacingAfter(p.getSpacingAfter());
        	setSpacingBefore(p.getSpacingBefore());
        	setExtraParagraphSpace(p.getExtraParagraphSpace());
        }
    }

    /**
     * Creates a shallow clone of the Paragraph.
     * @return
     */
    public Paragraph cloneShallow(boolean spacingBefore) {
    	Paragraph copy = new Paragraph();
        copy.setFont(getFont());
    	copy.setAlignment(getAlignment());
    	copy.setLeading(getLeading(), multipliedLeading);
    	copy.setIndentationLeft(getIndentationLeft());
    	copy.setIndentationRight(getIndentationRight());
    	copy.setFirstLineIndent(getFirstLineIndent());
    	copy.setSpacingAfter(getSpacingAfter());
    	if (spacingBefore)
    		copy.setSpacingBefore(getSpacingBefore());
    	copy.setExtraParagraphSpace(getExtraParagraphSpace());
    	return copy;
    }
    
    /**
     * Breaks this Paragraph up in different parts, separating paragraphs, lists and tables from each other.
     * @return
     */
    public java.util.List<Element> breakUp() {
    	java.util.List<Element> list = new ArrayList<Element>();
		Paragraph tmp = null;
		for (Element e : this) {
			if (e.type() == Element.LIST || e.type() == Element.PTABLE || e.type() == Element.PARAGRAPH) {
				if (tmp != null && tmp.size() > 0) {
                    tmp.setSpacingAfter(0);
					list.add(tmp);
					tmp = cloneShallow(false);
				}
                if (list.size() == 0) {
                    switch (e.type()) {
                        case Element.PTABLE:
                            ((PdfPTable) e).setSpacingBefore(getSpacingBefore());
                            break;
                        case Element.PARAGRAPH:
                            ((Paragraph) e).setSpacingBefore(getSpacingBefore());
                            break;
                        case Element.LIST:
                            ListItem firstItem = ((List)e).getFirstItem();
                            if (firstItem != null) {
                                firstItem.setSpacingBefore(getSpacingBefore());
                            }
                            break;
                        default:
                            break;
                    }
                }
				list.add(e);
			}
			else {
                if (tmp == null) {
                    tmp = cloneShallow(list.size() == 0);
                }
				tmp.add(e);
			}
		}
		if (tmp != null && tmp.size() > 0) {
			list.add(tmp);
        }
        if (list.size() != 0) {
            Element lastElement = list.get(list.size() - 1);
            switch (lastElement.type()) {
                case Element.PTABLE :
                    ((PdfPTable)lastElement).setSpacingAfter(getSpacingAfter());
                    break;
                case Element.PARAGRAPH :
                    ((Paragraph)lastElement).setSpacingAfter(getSpacingAfter());
                    break;
                case Element.LIST :
                    ListItem lastItem = ((List)lastElement).getLastItem();
                    if (lastItem != null) {
                        lastItem.setSpacingAfter(getSpacingAfter());
                    }
                    break;
                default:
                    break;
            }
        }
    	return list;
    }
    
    // implementation of the Element-methods

    /**
     * Gets the type of the text element.
     *
     * @return	a type
     */
    @Override
    public int type() {
        return Element.PARAGRAPH;
    }

    // methods

    /**
     * Adds an <CODE>Element</CODE> to the <CODE>Paragraph</CODE>.
     *
     * @param	o the element to add.
     * @return true is adding the object succeeded
     */
    @Override
    public boolean add(Element o) {
        if (o instanceof List) {
            List list = (List) o;
            list.setIndentationLeft(list.getIndentationLeft() + indentationLeft);
            list.setIndentationRight(indentationRight);
            return super.add(list);
        }
        else if (o instanceof Image) {
            super.addSpecial(o);
            return true;
        }
        else if (o instanceof Paragraph) {
        	super.addSpecial(o);
        	return true;
        }
        return super.add(o);
    }

    // setting the membervariables

    /**
     * Sets the alignment of this paragraph.
     *
     * @param	alignment		the new alignment
     */
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    /**
     * @see com.itextpdf.text.Phrase#setLeading(float)
     */
    @Override
    public void setLeading(float fixedLeading) {
        this.leading = fixedLeading;
        this.multipliedLeading = 0;
    }

    /**
     * Sets the variable leading. The resultant leading will be
     * multipliedLeading*maxFontSize where maxFontSize is the
     * size of the biggest font in the line.
     * @param multipliedLeading the variable leading
     */
    public void setMultipliedLeading(float multipliedLeading) {
        this.leading = 0;
        this.multipliedLeading = multipliedLeading;
    }

    /**
     * Sets the leading fixed and variable. The resultant leading will be
     * fixedLeading+multipliedLeading*maxFontSize where maxFontSize is the
     * size of the biggest font in the line.
     * @param fixedLeading the fixed leading
     * @param multipliedLeading the variable leading
     */
    public void setLeading(float fixedLeading, float multipliedLeading) {
        this.leading = fixedLeading;
        this.multipliedLeading = multipliedLeading;
    }

    /* (non-Javadoc)
	 * @see com.itextpdf.text.Indentable#setIndentationLeft(float)
	 */
    public void setIndentationLeft(float indentation) {
        this.indentationLeft = indentation;
    }

    /* (non-Javadoc)
	 * @see com.itextpdf.text.Indentable#setIndentationRight(float)
	 */
    public void setIndentationRight(float indentation) {
        this.indentationRight = indentation;
    }

    /**
     * Setter for property firstLineIndent.
     * @param firstLineIndent New value of property firstLineIndent.
     */
    public void setFirstLineIndent(float firstLineIndent) {
        this.firstLineIndent = firstLineIndent;
    }

    /* (non-Javadoc)
	 * @see com.itextpdf.text.Spaceable#setSpacingBefore(float)
	 */
    public void setSpacingBefore(float spacing) {
        this.spacingBefore = spacing;
    }

    /* (non-Javadoc)
	 * @see com.itextpdf.text.Spaceable#setSpacingAfter(float)
	 */
    public void setSpacingAfter(float spacing) {
        this.spacingAfter = spacing;
    }

    /**
     * Indicates that the paragraph has to be kept together on one page.
     *
     * @param   keeptogether    true of the paragraph may not be split over 2 pages
     */
    public void setKeepTogether(boolean keeptogether) {
        this.keeptogether = keeptogether;
    }

    /**
     * Checks if this paragraph has to be kept together on one page.
     *
     * @return  true if the paragraph may not be split over 2 pages.
     */
    public boolean getKeepTogether() {
        return keeptogether;
    }

    // methods to retrieve information

	/**
     * Gets the alignment of this paragraph.
     *
     * @return	alignment
     */
    public int getAlignment() {
        return alignment;
    }

    /**
     * Gets the variable leading
     * @return the leading
     */
    public float getMultipliedLeading() {
        return multipliedLeading;
    }

    /**
     * Gets the total leading.
     * This method is based on the assumption that the
     * font of the Paragraph is the font of all the elements
     * that make part of the paragraph. This isn't necessarily
     * true.
     * @return the total leading (fixed and multiplied)
     */
    public float getTotalLeading() {
    	float m = font == null ?
    			Font.DEFAULTSIZE * multipliedLeading : font.getCalculatedLeading(multipliedLeading);
    	if (m > 0 && !hasLeading()) {
    		return m;
    	}
    	return getLeading() + m;
    }

	/* (non-Javadoc)
	 * @see com.itextpdf.text.Indentable#getIndentationLeft()
	 */
    public float getIndentationLeft() {
        return indentationLeft;
    }

	/* (non-Javadoc)
	 * @see com.itextpdf.text.Indentable#getIndentationRight()
	 */
    public float getIndentationRight() {
        return indentationRight;
    }

    /**
     * Getter for property firstLineIndent.
     * @return Value of property firstLineIndent.
     */
    public float getFirstLineIndent() {
        return this.firstLineIndent;
    }

    /* (non-Javadoc)
	 * @see com.itextpdf.text.Spaceable#getSpacingBefore()
	 */
    public float getSpacingBefore() {
    	return spacingBefore;
    }

    /* (non-Javadoc)
	 * @see com.itextpdf.text.Spaceable#getSpacingAfter()
	 */
    public float getSpacingAfter() {
    	return spacingAfter;
    }

    /**
     * Getter for property extraParagraphSpace.
     * @return Value of property extraParagraphSpace.
     */
    public float getExtraParagraphSpace() {
        return this.extraParagraphSpace;
    }

    /**
     * Setter for property extraParagraphSpace.
     * @param extraParagraphSpace New value of property extraParagraphSpace.
     */
    public void setExtraParagraphSpace(float extraParagraphSpace) {
        this.extraParagraphSpace = extraParagraphSpace;
    }

    // scheduled for removal

    /**
     * Gets the spacing before this paragraph.
     *
     * @return	the spacing
     * @deprecated As of iText 2.1.5, replaced by {@link #getSpacingBefore()},
     * scheduled for removal at 2.3.0
     */
    @Deprecated
    public float spacingBefore() {
        return getSpacingBefore();
    }

    /**
     * Gets the spacing after this paragraph.
     *
     * @return	the spacing
     * @deprecated As of iText 2.1.5, replaced by {@link #getSpacingAfter()},
     * scheduled for removal at 2.3.0
     */
    @Deprecated
    public float spacingAfter() {
        return spacingAfter;
    }

}
