/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

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
    public boolean generateFrontMatter = true;
    public float trackingSpaceCharRatio;
    public boolean strictImageSequence;
    public String hangingPunctuation = ".,;:'-";

    public GeneralSettings()
    {
    }
}
