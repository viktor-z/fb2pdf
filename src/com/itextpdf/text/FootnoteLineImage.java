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

    private FootnoteLineImage(Image image) {
        super(image);
    }

    public FootnoteLineImage(PdfTemplate template) throws BadElementException{
        super(template);
    }

     /**
     * gets an instance of an Image
     *
     * @param template
     *            a PdfTemplate that has to be wrapped in an Image object
     * @return an Image object
     * @throws BadElementException
     */
    public static Image getInstance(PdfTemplate template)
                    throws BadElementException {
            return new FootnoteLineImage(template);
    }
}
