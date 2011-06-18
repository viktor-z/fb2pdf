/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import nu.xom.Element;
import nu.xom.Elements;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author vzeltser
 */
public class FootnoteRenderer {
    
    static PageStyle pageStyle;
    static ParagraphStyle noteStyle;
    static PdfWriter writer;
    static Document doc;
    static float fontSize;
    static BaseFont basefont;
    static Rectangle pageSize;
    static ByteArrayOutputStream output;
    static float cutMarkerWidth = 0;

    private static void addCutMarker(Chunk chunk) throws FB2toPDFException, DocumentException {
        doc.setPageSize(new Rectangle(cutMarkerWidth, pageSize.getHeight()));
        doc.newPage();
        Paragraph paragraph = createParagraph();
        paragraph.setAlignment(Paragraph.ALIGN_RIGHT);
        paragraph.setIndentationLeft(0);
        paragraph.setIndentationRight(0);
        paragraph.setFirstLineIndent(0);
        chunk = noteStyle.createChunk();
        chunk.append("<…> ");
        paragraph.add(chunk);
        doc.add(paragraph);
        
        doc.setPageSize(pageSize);

        doc.newPage();
    }

    private static Paragraph createParagraph() throws FB2toPDFException {
        float ascent = basefont.getFontDescriptor(BaseFont.ASCENT, fontSize);
        Paragraph paragraph = noteStyle.createParagraph();
        paragraph.setLeading(ascent);
        return paragraph;
    }
   
    public static byte[] close() {
        doc.close();
        return output.toByteArray();
    }
    
    public static int getPageNumber() {
        return writer.getPageNumber();
    }
    
    private static String getNoteText(Element section) {
        
        if (section == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        getNoteText(section, text, true);
        return text.toString();
    }
    
    private static void getNoteText(Element element, StringBuilder text, boolean skipTitle) {
        Elements children = element.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String localName = child.getLocalName();
            if(StringUtils.isBlank(localName)) {
                continue;
            }
            if(localName.equals("poem") || localName.equals("stanza") || localName.equals("cite")){
                getNoteText(child, text, false);
            } else if (localName.equals("p") || localName.equals("v") || localName.equals("text-author") ||
                    localName.equals("date") || localName.equals("epigraph") ||
                    (!skipTitle && localName.equals("title"))) {
                Element paragraph = child;
                String paragraphText = paragraph.getValue();
                paragraphText = paragraphText.replaceAll("\n", " ").replaceAll("  ", " ").trim();
                if (paragraphText.isEmpty()) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append("    ");
                }
                text.append(paragraphText);
                text.append("\n");
            }
        }
    }

    public static void addFootnote(String marker, String refname, Element section, HyphenationAuto hyphenation) throws FB2toPDFException, DocumentException {
        Chunk chunk = noteStyle.createChunk();
        chunk.append(marker + " ");

        boolean added = addFootnote(section, hyphenation, chunk, true);

        if (added) {
            addCutMarker(chunk);
        }
    }

    private static boolean addFootnote(Element element, HyphenationAuto hyphenation, Chunk firstChunk, boolean skipTitle) throws FB2toPDFException, DocumentException {
        if (element == null) {
            return false;
        }
        
        boolean added = false;

        Elements children = element.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String localName = child.getLocalName();
            if(StringUtils.isBlank(localName)) {
                continue;
            }
            if(localName.equals("poem") || localName.equals("stanza") || localName.equals("cite")){
                addFootnote(child, hyphenation, firstChunk, false);
            } else if (localName.equals("p") || localName.equals("v") || localName.equals("text-author") ||
                    localName.equals("date") || localName.equals("epigraph") ||
                    (!skipTitle && localName.equals("title"))) {
                Element childElement = child;
                String paragraphText = childElement.getValue();
                paragraphText = paragraphText.replaceAll("\n", " ").replaceAll("  ", " ").trim();
                if (paragraphText.isEmpty()) {
                    continue;
                }
                Paragraph paragraph = createParagraph();
                if (firstChunk != null) {
                    paragraph.add(firstChunk);
                    firstChunk = null;
                }
                Chunk chunk = noteStyle.createChunk();
                chunk.setHyphenation(hyphenation);
                chunk.append(paragraphText);
                paragraph.add(chunk);
                doc.add(paragraph);
                added = true;
            }
        }
        return added;
    }
    
    public static void init(Stylesheet stylesheet) throws FB2toPDFException, DocumentException {
        pageStyle = stylesheet.getPageStyle();
        noteStyle = stylesheet.getParagraphStyle("footnote");
        float pageWidth = pageStyle.getPageWidth() - pageStyle.getMarginLeft() - pageStyle.getMarginRight();
        fontSize = noteStyle.getFontSize().getPoints();
        basefont = noteStyle.getBaseFont();
        cutMarkerWidth = basefont.getWidthPointKerned("  <…> ", fontSize);
        float ascent = basefont.getFontDescriptor(BaseFont.ASCENT, fontSize);
        float descent = basefont.getFontDescriptor(BaseFont.DESCENT, fontSize);
        float pageHeight = Math.max(ascent - descent, noteStyle.getAbsoluteLeading());
        pageSize = new Rectangle(pageWidth, pageHeight);
        //pageSize.setBackgroundColor(BaseColor.LIGHT_GRAY);
        doc = new Document(pageSize, 0, 0, 0, 0);
        PdfDocument.preventWidows = false;
        output = new ByteArrayOutputStream();
        writer = PdfWriter.getInstance(doc, output);
        writer.setPageEvent(new PageEvents());
        doc.open();
    }

    private static class PageEvents extends PdfPageEventHelper {

        @Override
        public void onStartPage(PdfWriter writer,Document document) {
            writer.setPageEmpty(false);
        }
    }
}
