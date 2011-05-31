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

    private Image image;
    private boolean firstPass = true;
    private Document doc;

    public HeaderHelper(Document doc, PdfWriter writer, PdfPTable table) throws BadElementException {
        this.doc = doc;
        float templateWidth = doc.getPageSize().getWidth();
        float templateHight = doc.getPageSize().getHeight();
        PdfTemplate tp = PdfTemplate.createTemplate(writer, templateWidth, templateHight);
        table.writeSelectedRows(0, -1, doc.leftMargin(), templateHight, tp);
        //tp.saveState();
        //tp.setColorStroke(BaseColor.YELLOW);
        //tp.setColorFill(BaseColor.YELLOW);
        //tp.rectangle(-100, -100, 200, 200);
        //tp.fillStroke();
        //tp.setColorFill(BaseColor.RED);
        //tp.rectangle(0, 0, templateWidth, templateHight);
        //tp.fillStroke();
        //stp.restoreState();
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
            writer.getDirectContent().addImage(image);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
