/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

/**
 *
 * @author vzeltser
 */
public class HeaderSettings {

    public boolean enabled;
    public boolean dynamic = true;
    public String chapterTitle = "";
    public boolean skipBeforeSection = false;
    public boolean addHeightToMargin = true;
    public HeaderSlotSettings leftOdd = new HeaderSlotSettings();
    public HeaderSlotSettings centerOdd = new HeaderSlotSettings();
    public HeaderSlotSettings rightOdd = new HeaderSlotSettings();
    public HeaderSlotSettings leftEven = new HeaderSlotSettings();
    public HeaderSlotSettings centerEven = new HeaderSlotSettings();
    public HeaderSlotSettings rightEven = new HeaderSlotSettings();

    public HeaderSettings() {
    }
}
