/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.itextpdf.text;

import com.itextpdf.text.pdf.PdfTemplate;

/**
 *
 * @author vzeltser
 */
public class FootnoteLineImage extends ImgTemplate {

    public String refname = "";
    public boolean ready = false;

    private FootnoteLineImage(Image image) {
        super(image);
    }

    public FootnoteLineImage(PdfTemplate template, String refname) throws BadElementException{
        super(template);
        this.refname = refname;
    }

     /**
     * gets an instance of an Image
     *
     * @param template
     *            a PdfTemplate that has to be wrapped in an Image object
     * @return an Image object
     * @throws BadElementException
     */
    public static Image getInstance(PdfTemplate template, String refname)
                    throws BadElementException {
            return new FootnoteLineImage(template, refname);
    }
}
