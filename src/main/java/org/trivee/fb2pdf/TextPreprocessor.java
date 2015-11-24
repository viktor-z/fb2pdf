/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vzeltser
 */
public class TextPreprocessor {

    public static String process(String text, TextPreprocessorSettings settings, ParagraphStyle currentStyle) throws FB2toPDFException {

        String result = text;

        if(currentStyle == null || !currentStyle.getPreserveWhitespaces())
            result = cleanWhiteSpaces(result);

        if (!settings.enabled)
            return result;

        if(settings.makeReplacements)
            result = makeReplacements(result, settings.replacementsMap);

        if(settings.makeEndUnbreakable)
            result = makeEndUnbreakable(result);

        return result;
    }

    private static String makeReplacements(String text, Map<String, String> replacementsMap) {

        String result = text;

        /*
        Map<String, String> replacementsMap = new HashMap<String, String>()
        {
            {
                put(" \u2012 ", "\u00A0\u2012 "); //figure dash
                put(" \u2013 ", "\u00A0\u2013 "); //en dash
                put(" \u2014 ", "\u00A0\u2014 "); //em dash
                put(" \u2015 ", "\u00A0\u2015 "); //horizontal bar
                put(" \u2212 ", "\u00A0\u2212 "); //minus
                put(" \u2010 ", "\u00A0\u2010 "); //hyphen
                put(" \u002D ", "\u00A0\u002D "); //ASCII hyphen-minus
            }
        };
         */

        for (String key: replacementsMap.keySet()){
            result = result.replaceAll(key, replacementsMap.get(key));
        }

        return result;
    }

    private static String cleanWhiteSpaces(String text) {

        Map<String, String> replacementsMap = new HashMap<String, String>()
        {
            {
                put("\r\n", "\u0020"); //end-of-line
                put("\n", "\u0020"); //end-of-line to space
                put("\r", "\u0020"); //end-of-line to space
                put("\u0020\u0020+", "\u0020"); //two-or-more-spaces to space
            }
        };
        
        return makeReplacements(text, replacementsMap);
    }

    private static String makeEndUnbreakable(String text) {

        final int tailLength = 4;

        if (text.length() < tailLength)
            return text;

        String head = text.substring(0, text.length()-tailLength);
        String tail = text.substring(text.length()-tailLength, text.length());

        StringBuilder sb = new StringBuilder(head);
        for (int i=0; i < tailLength; i++) {
            //sb.append('\uFEFF'); // BOM / zero width non-breaking space
            sb.append('\u2060'); //zero width no-break space

            sb.append(tail.charAt(i));
        }

        return sb.toString();
    }

}
