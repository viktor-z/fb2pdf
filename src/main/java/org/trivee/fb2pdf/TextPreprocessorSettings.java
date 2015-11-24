/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.util.LinkedHashMap;

/**
 *
 * @author vzeltser
 */
public class TextPreprocessorSettings {

    public boolean enabled;
    public boolean makeReplacements;
    public boolean makeEndUnbreakable;

    public LinkedHashMap<String, String> replacementsMap = new LinkedHashMap<String, String>();

    public TextPreprocessorSettings() {
    }
}
