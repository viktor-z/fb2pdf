/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.hyphenation.Hyphenation;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author vzeltser
 */
public class HyphenationAuto extends com.itextpdf.text.pdf.HyphenationAuto {

    public HyphenationAuto(String lang, String country, int leftMin, int rightMin) {
        super(lang, country, leftMin, rightMin);
    }

    @Override
    public String getHyphenatedWordPre(String word, BaseFont font, float fontSize, float remainingWidth) {
        post = word;
        String hyphen = getHyphenSymbol();
        float hyphenWidth = font.getWidthPoint(hyphen, fontSize);
        if (hyphenWidth > remainingWidth)
            return "";

        List<String> parts = split(word);

        List<String> hyphparts = new ArrayList<String>();
        for (String part: parts) {
            Hyphenation hyphenation = hyphenator.hyphenate(part);
            if (hyphenation == null) {
                hyphparts.add(part);
            } else {
                hyphparts.addAll(getHyphParts(hyphenation, part));
            }
        }

        hyphparts = joinSome(hyphparts);

        String bufferPre = "";
        String bufferPost = "";
        int idx;
        for (idx = 0; idx<hyphparts.size(); idx++) {
            String part = hyphparts.get(idx);
            if (font.getWidthPoint(bufferPre+part, fontSize) + hyphenWidth > remainingWidth)
                break;
            bufferPre += part;
        }

        if (idx <= 0) {
            return "";
        }

        for (; idx<hyphparts.size(); idx++) {
            bufferPost += hyphparts.get(idx);
        }

        post = bufferPost;
        return bufferPre + hyphen;

    }

    private List<String> getHyphParts(Hyphenation hyphenation, String word) {
        List<String> res  = new ArrayList<String>();
        int len = hyphenation.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            int hpoint = hyphenation.getHyphenationPoints()[i];
            res.add(word.substring(start, hpoint));
            start = hpoint;
        }
        res.add(word.substring(start));
        return res;
    }

    private List<String> split(String words) {
        String[] wordsarr = StringUtils.splitByCharacterType(words);
        List<String> res = new ArrayList<String>();
        String buffer = "";
        boolean letterState = true;
        for (int i=0; i<wordsarr.length;i++){
            boolean letterInput = Character.isLetter(wordsarr[i].charAt(0));
            if (!letterState && letterInput) {
                if (buffer.length()>0) {
                    res.add(buffer);
                }
                buffer = wordsarr[i];
            } else {
                buffer += wordsarr[i];
            }
            letterState = letterInput;
        }
        if (!buffer.isEmpty()) {
            res.add(buffer);
        }
        return res;
    }

    private List<String> joinSome (List<String> parts){
        List<String> res = new ArrayList<String>();
        String buffer = "";
        for (String part: parts) {
            boolean bufferEndsWithLetter = buffer.length() > 0 && Character.isLetter(buffer.charAt(buffer.length()-1));
            boolean partStartsWithLetter = part.length() > 0 && Character.isLetter(part.charAt(0));

            if (bufferEndsWithLetter && partStartsWithLetter) {
                if (buffer.length() > 0){
                    res.add(buffer);
                }
                buffer = part;
            } else {
                buffer += part;
            }
        }
        if (buffer.length() > 0){
            res.add(buffer);
        }
        return res;
    }
}
