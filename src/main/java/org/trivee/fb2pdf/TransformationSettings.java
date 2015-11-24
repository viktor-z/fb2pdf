/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import java.util.LinkedList;

/**
 *
 * @author vzeltser
 */
public class TransformationSettings {

    public boolean enabled;
    public String queryProlog;
    public String morpherProlog;
    public boolean outputDebugFile;

    public LinkedList<Entry> transformationsMap = new LinkedList<Entry>();

    public TransformationSettings() {
    }

    public static class Entry {
        String query;
        String morpher;
    }

}
