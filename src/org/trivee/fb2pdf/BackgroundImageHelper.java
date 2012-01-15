/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.IOException;

/**
 *
 * @author vzeltser
 */
public class BackgroundImageHelper extends PdfPageEventHelper {

    private Image image;

    protected final void init(Stylesheet stylesheet, Rectangle pageSize) throws IOException, BadElementException {
        String imagePath = stylesheet.getPageStyle().backgroundImage;
        float dpi = stylesheet.getGeneralSettings().imageDpi;
        Image img = Image.getInstance(Utilities.getValidatedFileName(imagePath));
        Utilities.rescaleImage(img, 1.0f, 0, 0, pageSize, dpi);
        this.image = img;
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
