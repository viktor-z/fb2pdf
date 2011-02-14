/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
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

    public static byte[] renderNoteDoc(Stylesheet stylesheet, String body, HyphenationAuto hyphenation) throws FB2toPDFException, DocumentException {
        final PageStyle pageStyle = stylesheet.getPageStyle();
        final ParagraphStyle noteStyle = stylesheet.getParagraphStyle("footnote");
        float w = pageStyle.getPageWidth() - pageStyle.getMarginLeft() - pageStyle.getMarginRight();
        float fontSize = noteStyle.getFontSize().getPoints();
        BaseFont basefont = noteStyle.getBaseFont();
        float ascent = basefont.getFontDescriptor(BaseFont.ASCENT, fontSize);
        float descent = basefont.getFontDescriptor(BaseFont.DESCENT, fontSize);
        //float capheight = basefont.getFontDescriptor(BaseFont.CAPHEIGHT, fontSize);
        float h = ascent - descent + noteStyle.getAbsoluteLeading() / 2;
        Rectangle pageSize = new Rectangle(w, h);
        //pageSize.setBackgroundColor(BaseColor.LIGHT_GRAY);
        Document doc = new Document(pageSize, pageStyle.getMarginLeft(), pageStyle.getMarginRight(), 0, 0);
        PdfDocument.preventWidows = false;
        PdfWriter writer = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer = PdfWriter.getInstance(doc, output);
        writer.setPageEvent(new PageEvents());
        doc.open();
        Paragraph p = noteStyle.createParagraph();
        Chunk chunk = noteStyle.createChunk();
        chunk.setHyphenation(hyphenation);
        chunk.append(body);
        p.add(chunk);
        doc.add(p);
        doc.close();
        return output.toByteArray();
    }

    private static class PageEvents extends PdfPageEventHelper {

        @Override
        public void onStartPage(PdfWriter writer,Document document) {
            writer.setPageEmpty(false);
        }
    }
}
