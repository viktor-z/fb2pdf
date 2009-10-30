/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

/**
 *
 * @author vzeltser
 */
public class TypographyPrettifier {

    public static String process(String text) {

        String result = makeEndUnbreakable(text);

        return result;
    }

    private static String makeEndUnbreakable(String text) {

        final int tailLength = 4;

        if (text.length() < tailLength)
            return text;

        String mainText = text.substring(0, text.length()-tailLength);
        String tail = text.substring(text.length()-tailLength, text.length());

        StringBuilder sb = new StringBuilder(mainText);
        for (int i=0; i < tailLength; i++) {
            sb.append('\uFEFF');
            sb.append(tail.charAt(i));
        }

        return sb.toString();
    }

}
