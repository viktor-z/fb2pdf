/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author vzeltser
 */
public class HeaderHelper extends PdfPageEventHelper {

    public static int ODD = 1;
    public static int EVEN = 0;

    private Image image;
    private boolean firstPass = true;
    private Document doc;
    private int oddOrEven;

    public HeaderHelper(Document doc, PdfWriter writer, PdfPTable table, int oddOrEven) throws BadElementException {
        this.doc = doc;
        this.oddOrEven = oddOrEven;
        float templateWidth = doc.getPageSize().getWidth();
        float templateHight = doc.getPageSize().getHeight();
        PdfTemplate tp = PdfTemplate.createTemplate(writer, templateWidth, templateHight);
        table.writeSelectedRows(0, -1, doc.leftMargin(), templateHight, tp);
        Image footer = Image.getInstance(tp);
        footer.setAbsolutePosition(0, 0);
        this.image = footer;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (firstPass) {
            firstPass = false;
            return;
        }
    
        try {
            if (oddOrEven == document.getPageNumber() % 2) {
                writer.getDirectContent().addImage(image);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
