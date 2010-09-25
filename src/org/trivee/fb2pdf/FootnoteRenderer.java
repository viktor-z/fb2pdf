/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vzeltser
 */
public class FootnoteRenderer {

    static List<Image> renderFootnoteImages(String body, Stylesheet stylesheet) throws DocumentException, FB2toPDFException {

        boolean savedPreventWidows = PdfDocument.preventWidows;
        PdfDocument.preventWidows = false;
        try {
            byte[] pdf = renderNoteDoc(stylesheet, body);
            List<Image> images = getImages(pdf);
            return images;
        } finally {
            PdfDocument.preventWidows = savedPreventWidows;
        }
    }

    public static byte[] renderNoteDoc(Stylesheet stylesheet, String body) throws FB2toPDFException, DocumentException {
        final PageStyle pageStyle = stylesheet.getPageStyle();
        final ParagraphStyle noteStyle = stylesheet.getParagraphStyle("footnote");
        float w = pageStyle.getPageWidth() - pageStyle.getMarginLeft() - pageStyle.getMarginRight();
        float fontSize = noteStyle.getFontSize().getPoints();
        BaseFont basefont = noteStyle.getBaseFont();
        float h = basefont.getFontDescriptor(BaseFont.CAPHEIGHT, fontSize) + noteStyle.getAbsoluteLeading();
        Rectangle pageSize = new Rectangle(w, h);
        Document doc = new Document(pageSize, pageStyle.getMarginLeft(), pageStyle.getMarginRight(), pageStyle.getMarginTop(), pageStyle.getMarginBottom());
        PdfWriter writer = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer = PdfWriter.getInstance(doc, output);
        writer.setPageEvent(new PageEvents());
        doc.open();
        Paragraph p = noteStyle.createParagraph();
        p.add(body);
        doc.add(p);
        doc.close();
        return output.toByteArray();
    }

    private static List<Image> getImages(byte[] pdf) {

        List<Image> result = new ArrayList<Image>();
        try {
            PdfReader reader = new PdfReader(pdf);
            PdfStamper stamper = new PdfStamper(reader, new ByteArrayOutputStream());

            for (int i=1; i<reader.getNumberOfPages(); i++){
                PdfImportedPage page = stamper.getImportedPage(reader, i);
                Image image = Image.getInstance(page);
                result.add(image);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    private static class PageEvents extends PdfPageEventHelper {

        @Override
        public void onStartPage(PdfWriter writer,Document document) {
            writer.setPageEmpty(false);
        }
    }
}
