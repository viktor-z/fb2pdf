package org.trivee.fb2pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
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
        return new BaseColor(Color.decode(c).getRGB());
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

    public static java.awt.Image makeGrayTransparent(byte[] imageData) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        java.awt.Image img = toolkit.createImage(imageData);
        ImageFilter filter = new RGBImageFilter() {

                public final int filterRGB(int x, int y, int rgb)
                {
                    int r = (rgb & 0xFF0000) >> 16;
                    int g = (rgb & 0xFF00) >> 8;
                    int b = rgb & 0xFF;
                    if (r == g && g == b) {
                        return ((0xFF - r) << 24) & 0xFF000000;
                    }
                    return rgb;
                }        
        };

        ImageProducer ip = new FilteredImageSource(img.getSource(), filter);
        return toolkit.createImage(ip);
    }

}
