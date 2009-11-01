/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.util.HashMap;

/**
 *
 * @author vzeltser
 */
public class TextPreprocessorSettings {

    public boolean enabled;
    public boolean makeReplacements;
    public boolean makeEndUnbreakable;

    public HashMap<String, String> replacementsMap = new HashMap<String, String>();

    public TextPreprocessorSettings() {
    }
}
