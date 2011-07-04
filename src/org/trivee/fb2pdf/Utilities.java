package org.trivee.fb2pdf;

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

}
