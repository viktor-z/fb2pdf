/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import java.util.ArrayList;

/**
 *
 * @author vzeltser
 */
public class GeneralSettings {
    public boolean transliterateMetaInfo;
    public boolean forceTransliterateAuthor;
    public float imageDpi;
    public boolean stretchCover = true;
    public boolean enableInlineImages;
    public String overrideImageTransparency;
    public boolean ignoreEmptyLineBeforeImage;
    public boolean ignoreEmptyLineAfterImage;
    public boolean generateTOC;
    public boolean generateNoteBackLinks = true;
    public boolean generateFrontMatter = true;
    public float trackingSpaceCharRatio;
    public boolean strictImageSequence;
    public String hangingPunctuation = ".,;:'-";
    public boolean enableLinkPageNum;
    public String linkPageNumFormat = "[%04d]";
    public int linkPageNumMax = 9999;
    public ArrayList<Integer> bodiesToSkip = new ArrayList<Integer>();
    public boolean fullCompression = false;

    public GeneralSettings()
    {
    }
}
