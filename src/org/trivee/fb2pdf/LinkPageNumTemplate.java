/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import com.itextpdf.text.pdf.PdfTemplate;

/**
 *
 * @author vzeltser
 */
public class LinkPageNumTemplate {
    public PdfTemplate template;
    public ParagraphStyle style;

    public LinkPageNumTemplate(PdfTemplate template, ParagraphStyle style) {
        this.template = template;
        this.style = style;
    }
}
