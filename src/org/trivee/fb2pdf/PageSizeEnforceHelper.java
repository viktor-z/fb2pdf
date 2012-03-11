/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Color;

/**
 *
 * @author vzeltser
 */
public class PageSizeEnforceHelper  extends PdfPageEventHelper {
    public boolean enforcePageSize = false;
    public Color pageSizeEnforcerColor;
    private Image marginEnforcerImage = null;

    private void addPageSizeEnforcer(PdfWriter writer) {
        if (marginEnforcerImage == null) {
            marginEnforcerImage = createEnforcerImage();
        }
        PdfContentByte cb = writer.getDirectContent();
        Rectangle pageSize = writer.getPageSize();
        try {
            float sz = 2.5f;
            float dx = pageSize.getWidth() - sz;
            float dy = pageSize.getHeight() - sz;
            cb.addImage(marginEnforcerImage, sz, 0, 0, sz, 0, dy);
            cb.addImage(marginEnforcerImage, 0, sz, -sz, 0, sz, 0);
            cb.addImage(marginEnforcerImage, -sz, 0, 0, -sz, dx + sz, sz);
            cb.addImage(marginEnforcerImage, 0, -sz, sz, 0, dx, dy + sz);
        } catch (DocumentException ex) {
            Log.error(ex.getMessage());
        }
    }

    private Image createEnforcerImage() {
        int[] transparency = new int[]{0, 0, 0, 0, 0, 0};

        byte R = pageSizeEnforcerColor == null ? 127 : (byte) pageSizeEnforcerColor.getRed();
        byte G = pageSizeEnforcerColor == null ? 127 : (byte) pageSizeEnforcerColor.getGreen();
        byte B = pageSizeEnforcerColor == null ? 127 : (byte) pageSizeEnforcerColor.getBlue();
        byte[] data = new byte[]{
            R, G, B, 0, 0, 0, R, G, B, 0, 0, 0, R, G, B,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            R, G, B, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            R, G, B, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        Image img = null;
        try {
            img = Image.getInstance(5, 5, 3, 8, data, transparency);
        } catch (BadElementException ex) {
            Log.error(ex.getMessage());
        }
        return img;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (enforcePageSize) {
            addPageSizeEnforcer(writer);
        }
    }
}
