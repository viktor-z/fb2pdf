/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Rectangle;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author vzeltser
 */
public class HeaderSlotSettings {

    public boolean enabled;
    public String style = "header";
    public String query;
    public int border = Rectangle.BOTTOM;
    private String borderColor;

    public HeaderSlotSettings() {
    }

    BaseColor getBorderColor() {
        return (StringUtils.isNotBlank(borderColor)) ? Utilities.getColor(borderColor) : null;
    }
}
