/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author vzeltser
 */
public class BackgroundImageHelper extends PdfPageEventHelper {

    private Image image;

    public BackgroundImageHelper(Image image) {
        this.image = image;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            image.setAbsolutePosition(0, 0);
            writer.getDirectContentUnder().addImage(image);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
