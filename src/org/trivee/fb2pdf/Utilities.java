package org.trivee.fb2pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

/**
 *
 * @author vzeltser
 */
public class Utilities {

    public static String getValidatedFileName(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            return filename;
        }

        file = new File(getBaseDir() + "/" + filename);
        String fullFilename = file.getCanonicalPath();
        if (!file.exists()) {
            throw new IOException(String.format("File not found [%s or %s]", filename, fullFilename));
        }
        return fullFilename;
    }

    public static String getBaseDir()  throws IOException {
        String libPath = URLDecoder.decode(new File(Utilities.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent(), "UTF-8");
        return (new File(libPath)).getParent();
    }

    public static BaseColor getColor(String c) {
        return new BaseColor(Color.decode(c));
    }

    public static void rescaleImage(Image image, float zoomFactor, float wSpace, float hSpace, Rectangle pageSize, float dpi) {
        float scaleWidth = pageSize.getWidth() - wSpace;
        float scaleHeight = pageSize.getHeight() - hSpace;
        float imgWidth = image.getWidth() / dpi * 72 * zoomFactor;
        float imgHeight = image.getHeight() / dpi * 72 * zoomFactor;
        if ((imgWidth <= scaleWidth) && (imgHeight <= scaleHeight)) {
            scaleWidth = imgWidth;
            scaleHeight = imgHeight;
        }
        image.scaleToFit(scaleWidth, scaleHeight);
    }
}
