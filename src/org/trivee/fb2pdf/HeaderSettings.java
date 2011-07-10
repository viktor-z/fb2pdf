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
    public HeaderSlotSettings left = new HeaderSlotSettings();
    public HeaderSlotSettings center = new HeaderSlotSettings();
    public HeaderSlotSettings right = new HeaderSlotSettings();

    public HeaderSettings() {
    }
}
