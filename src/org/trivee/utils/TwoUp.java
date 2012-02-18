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
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, tmp);
            writer.setFullCompression();
            // step 3: we open the document
            document.open();
            // step 4: adding the content
            PdfContentByte cb = writer.getDirectContent();
            PdfImportedPage page;
            float offsetX;
            for (int p = 1; p <= total; p++) {
                offsetX = 0;
                if (p % 2 != 0) {
                    document.newPage();
                } else {
                    offsetX = pageSize.getWidth();
                }
                page = writer.getImportedPage(reader, p);
                cb.addTemplate(page, offsetX, 0);
            }
            // step 5: we close the document
            document.close();

            PdfReader reader2 = new PdfReader(new ByteArrayInputStream(tmp.toByteArray()));
            PdfStamper stamper = new PdfStamper(reader2, new FileOutputStream(dest));
            stamper.setMoreInfo(reader.getInfo());
            List<HashMap<String, Object>> outlines = SimpleBookmark.getBookmark(reader);
            for (int pageNum=1; pageNum<=total; pageNum++){
                int[] range = {pageNum,pageNum};
                int newPageNum = (pageNum / 2) + (pageNum % 2 != 0 ? 1 : 0);
                SimpleBookmark.shiftPageNumbers(outlines, newPageNum - pageNum, range);
            }
            stamper.setOutlines(outlines);
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
        if (args.length != 2) {
            System.err.println("Usage: TwoUp <input file> <output file>");
            return;
        }
        execute(args[0], args[1]);
    }
}
