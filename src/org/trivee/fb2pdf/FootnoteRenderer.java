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
        
    private static byte[] renderNoteDoc(Stylesheet stylesheet, String body, HyphenationAuto hyphenation) throws FB2toPDFException, DocumentException {
        
        init(stylesheet);

        addFootnote(body, hyphenation);

        return close();
    }

    public static byte[] close() {
        doc.close();
        return output.toByteArray();
    }
    
    public static int getPageNumber() {
        return writer.getPageNumber();
    }

    public static void addFootnote(String body, HyphenationAuto hyphenation) throws FB2toPDFException, DocumentException {
        Paragraph paragraph = noteStyle.createParagraph();
        Chunk chunk = noteStyle.createChunk();
        chunk.setHyphenation(hyphenation);
        chunk.append(body);
        paragraph.add(chunk);
        doc.add(paragraph);

        doc.setPageSize(new Rectangle(cutMarkerWidth, pageSize.getHeight()));
        doc.setMargins(0,0,0,0);
        doc.newPage();
        paragraph = noteStyle.createParagraph();
        paragraph.setAlignment(Paragraph.ALIGN_RIGHT);
        chunk = noteStyle.createChunk();
        chunk.append("<…> ");
        paragraph.add(chunk);
        doc.add(paragraph);
        
        doc.setPageSize(pageSize);
        doc.setMargins(0,0,0,0);

        doc.newPage();
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
        //float capheight = basefont.getFontDescriptor(BaseFont.CAPHEIGHT, fontSize);
        float pageHeight = ascent - descent + noteStyle.getAbsoluteLeading() / 2;
        pageSize = new Rectangle(pageWidth, pageHeight);
        //pageSize.setBackgroundColor(BaseColor.LIGHT_GRAY);
        doc = new Document(pageSize, pageStyle.getMarginLeft(), pageStyle.getMarginRight(), 0, 0);
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
