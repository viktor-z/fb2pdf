/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.utils;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author vzeltser
 */
public class TwoUp {

    /**
     * @see com.lowagie.tools.AbstractTool#execute()
     */
    public static void execute(String srcvalue, String destvalue) {
        try {
            if (StringUtils.isBlank(srcvalue)) {
                throw new InstantiationException("You need to choose a sourcefile");
            }
            File src = new File(srcvalue);
            if (StringUtils.isBlank(destvalue)) {
                throw new InstantiationException("You need to choose a destination file");
            }
            File dest = new File(destvalue);
            // we create a reader for a certain document
            PdfReader reader = new PdfReader(src.getAbsolutePath());
            // we retrieve the total number of pages and the page size
            int total = reader.getNumberOfPages();
            System.out.println("There are " + total + " pages in the original file.");
            Rectangle pageSize = reader.getPageSize(1);
            Rectangle newSize = new Rectangle(pageSize.getWidth()*2, pageSize.getHeight());
            // step 1: creation of a document-object
            Document document = new Document(newSize, 0, 0, 0, 0);
            // step 2: we create a writer that listens to the document
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));
            // step 3: we open the document
            document.open();
            // step 4: adding the content
            PdfContentByte cb = writer.getDirectContent();
            PdfImportedPage page;
            float offsetX;
            int p;
            for (int i = 0; i < total; i++) {
                offsetX = 0;
                if (i % 2 == 0) {
                    document.newPage();
                } else {
                    offsetX = pageSize.getWidth();
                }
                p = i + 1;
                page = writer.getImportedPage(reader, p);
                cb.addTemplate(page, offsetX, 0);
            }
            // step 5: we close the document
            document.close();
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
        if (args.length < 2) {
            System.err.println("Usage: java org.trivee.utils.TwoUp src dest");
        }
        execute(args[0], args[1]);
    }
}
