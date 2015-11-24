/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.utils;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.SimpleBookmark;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author vzeltser
 */
public class Rotate {

    /**
     * @see com.lowagie.tools.AbstractTool#execute()
     */
    public static void execute(String srcvalue, String destvalue, String rotvalue) {
        try {
            if (StringUtils.isBlank(srcvalue)) {
                throw new InstantiationException("You need to choose a sourcefile");
            }
            File src = new File(srcvalue);
            if (StringUtils.isBlank(destvalue)) {
                throw new InstantiationException("You need to choose a destination file");
            }
            File dest = new File(destvalue);
            if (StringUtils.isBlank(rotvalue)) {
                throw new InstantiationException("You need to choose a rotation");
            }
            int rotation = Integer.parseInt(rotvalue);


            // we create a reader for a certain document
            PdfReader reader = new PdfReader(src.getAbsolutePath());
            // we retrieve the total number of pages and the page size
            int total = reader.getNumberOfPages();
            System.out.println("There are " + total + " pages in the original file.");

            PdfDictionary pageDict;
            int currentRotation;
            for (int p = 1; p <= total; p++) {
                currentRotation = reader.getPageRotation(p);
                pageDict = reader.getPageN(p);
                pageDict.put(PdfName.ROTATE, new PdfNumber(currentRotation + rotation));
            }
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
            stamper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates an NUp version of an existing PDF file.
     *
     * @param args String[]
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: Rotate <input file> <output file> <rotation>");
            System.err.println("  Valid values for rotation are 90, 180, 270");
            return;
        }
        execute(args[0], args[1], args[2]);
    }
}
