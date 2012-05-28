/*
 * $Id: XmpWriter.java 5075 2012-02-27 16:36:18Z blowagie $
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
package com.itextpdf.text.xml.xmp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.xml.XMLUtil;

/**
 * With this class you can create an Xmp Stream that can be used for adding
 * Metadata to a PDF Dictionary. Remark that this class doesn't cover the
 * complete XMP specification.
 */
public class XmpWriter {

	/** A possible charset for the XMP. */
	public static final String UTF8 = "UTF-8";
	/** A possible charset for the XMP. */
	public static final String UTF16 = "UTF-16";
	/** A possible charset for the XMP. */
	public static final String UTF16BE = "UTF-16BE";
	/** A possible charset for the XMP. */
	public static final String UTF16LE = "UTF-16LE";

	/** String used to fill the extra space. */
	public static final String EXTRASPACE = "                                                                                                   \n";

	/** You can add some extra space in the XMP packet; 1 unit in this variable represents 100 spaces and a newline. */
	protected int extraSpace;

	/** The writer to which you can write bytes for the XMP stream. */
	protected OutputStreamWriter writer;

	/** The about string that goes into the rdf:Description tags. */
	protected String about;

	/**
	 * Processing Instruction required at the start of an XMP stream
	 * @since iText 2.1.6
	 */
	public static final String XPACKET_PI_BEGIN = "<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n";

	/**
	 * Processing Instruction required at the end of an XMP stream for XMP streams that can be updated
	 * @since iText 2.1.6
	 */
	public static final String XPACKET_PI_END_W = "<?xpacket end=\"w\"?>";

	/**
	 * Processing Instruction required at the end of an XMP stream for XMP streams that are read only
	 * @since iText 2.1.6
	 */
	public static final String XPACKET_PI_END_R = "<?xpacket end=\"r\"?>";

	/** The end attribute. */
	protected char end = 'w';

	/**
	 * Creates an XmpWriter.
	 * @param os
	 * @param utfEncoding
	 * @param extraSpace
	 * @throws IOException
	 */
	public XmpWriter(OutputStream os, String utfEncoding, int extraSpace) throws IOException {
		this.extraSpace = extraSpace;
		writer = new OutputStreamWriter(os, utfEncoding);
		writer.write(XPACKET_PI_BEGIN);
		writer.write("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n");
		writer.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
		about = "";
	}

	/**
	 * Creates an XmpWriter.
	 * @param os
	 * @throws IOException
	 */
	public XmpWriter(OutputStream os) throws IOException {
		this(os, UTF8, 20);
	}

	/** Sets the XMP to read-only */
	public void setReadOnly() {
		end = 'r';
	}

	/**
	 * @param about The about to set.
	 */
	public void setAbout(String about) {
		this.about = about;
	}

	/**
	 * Adds an rdf:Description.
	 * @param xmlns
	 * @param content
	 * @throws IOException
	 */
	public void addRdfDescription(String xmlns, String content) throws IOException {
		writer.write("<rdf:Description rdf:about=\"");
		writer.write(about);
		writer.write("\" ");
		writer.write(xmlns);
		writer.write(">");
		writer.write(content);
		writer.write("</rdf:Description>\n");
	}

	/**
	 * Adds an rdf:Description.
	 * @param s
	 * @throws IOException
	 */
	public void addRdfDescription(XmpSchema s) throws IOException {
		writer.write("<rdf:Description rdf:about=\"");
		writer.write(about);
		writer.write("\" ");
		writer.write(s.getXmlns());
		writer.write(">");
		writer.write(s.toString());
		writer.write("</rdf:Description>\n");
	}

	/**
	 * Flushes and closes the XmpWriter.
	 * @throws IOException
	 */
	public void close() throws IOException {
		writer.write("</rdf:RDF>");
		writer.write("</x:xmpmeta>\n");
		for (int i = 0; i < extraSpace; i++) {
			writer.write(EXTRASPACE);
		}
		writer.write(end == 'r' ? XPACKET_PI_END_R : XPACKET_PI_END_W);
		writer.flush();
		writer.close();
	}

    /**
     * @param os
     * @param info
     * @throws IOException
     */
    public XmpWriter(OutputStream os, PdfDictionary info, int PdfXConformance) throws IOException {
        this(os);
        if (info != null) {
        	DublinCoreSchema dc = new DublinCoreSchema();
        	PdfSchema p = new PdfSchema();
        	XmpBasicSchema basic = new XmpBasicSchema();
        	PdfName key;
        	PdfObject obj;
        	String value;
        	for (PdfName pdfName : info.getKeys()) {
        		key = pdfName;
        		obj = info.get(key);
        		if (obj == null)
        			continue;
        		value = ((PdfString)obj).toUnicodeString();
        		if (PdfName.TITLE.equals(key)) {
        			dc.addTitle(value);
        		}
        		if (PdfName.AUTHOR.equals(key)) {
        			dc.addAuthor(value);
        		}
        		if (PdfName.SUBJECT.equals(key)) {
        			dc.addSubject(value);
        			dc.addDescription(value);
        		}
        		if (PdfName.KEYWORDS.equals(key)) {
        			p.addKeywords(value);
        		}
        		if (PdfName.CREATOR.equals(key)) {
        			basic.addCreatorTool(value);
        		}
        		if (PdfName.PRODUCER.equals(key)) {
        			p.addProducer(value);
        		}
        		if (PdfName.CREATIONDATE.equals(key)) {
        			basic.addCreateDate(PdfDate.getW3CDate(obj.toString()));
        		}
        		if (PdfName.MODDATE.equals(key)) {
        			basic.addModDate(PdfDate.getW3CDate(obj.toString()));
        		}
        	}
        	if (dc.size() > 0) addRdfDescription(dc);
        	if (p.size() > 0) addRdfDescription(p);
        	if (basic.size() > 0) addRdfDescription(basic);
            if (PdfXConformance == PdfWriter.PDFA1A || PdfXConformance == PdfWriter.PDFA1B) {
                PdfA1Schema a1 = new PdfA1Schema();
                if (PdfXConformance == PdfWriter.PDFA1A)
                    a1.addConformance("A");
                else
                    a1.addConformance("B");
                addRdfDescription(a1);
            }
        }
    }

    /**
     * @param os
     * @param info
     * @throws IOException
     * @since 5.0.1 (generic type in signature)
     */
    public XmpWriter(OutputStream os, Map<String, String> info) throws IOException {
        this(os);
        if (info != null) {
        	DublinCoreSchema dc = new DublinCoreSchema();
        	PdfSchema p = new PdfSchema();
        	XmpBasicSchema basic = new XmpBasicSchema();
        	String key;
        	String value;
        	for (Map.Entry<String, String> entry: info.entrySet()) {
        		key = entry.getKey();
        		value = entry.getValue();
        		if (value == null)
        			continue;
        		if ("Title".equals(key)) {
        			dc.addTitle(value);
        		}
        		if ("Author".equals(key)) {
        			dc.addAuthor(value);
        		}
        		if ("Subject".equals(key)) {
        			dc.addSubject(value);
        			dc.addDescription(value);
        		}
        		if ("Keywords".equals(key)) {
        			p.addKeywords(value);
        		}
        		if ("Creator".equals(key)) {
        			basic.addCreatorTool(value);
        		}
        		if ("Producer".equals(key)) {
        			p.addProducer(value);
        		}
        		if ("CreationDate".equals(key)) {
        			basic.addCreateDate(PdfDate.getW3CDate(value));
        		}
        		if ("ModDate".equals(key)) {
        			basic.addModDate(PdfDate.getW3CDate(value));
        		}
        	}
        	if (dc.size() > 0) addRdfDescription(dc);
        	if (p.size() > 0) addRdfDescription(p);
        	if (basic.size() > 0) addRdfDescription(basic);
        }
    }
}