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
    public String secondPassStylesheet;
    public boolean transliterateMetaInfo;
    public boolean forceTransliterateAuthor;
    public String metaAuthorQuery = "(first-name,  middle-name,  last-name)";
    public float imageDpi;
    public boolean stretchCover = true;
    public boolean enableInlineImages;
    public String overrideImageTransparency;
    public boolean cacheImages = true;
    public boolean ignoreEmptyLineBeforeImage;
    public boolean ignoreEmptyLineAfterImage;
    public boolean generateTOC;
    public boolean generateNoteBackLinks = true;
    public boolean generateInternalLinks = true;
    public boolean generateFrontMatter = true;
    public float trackingSpaceCharRatio;
    public boolean strictImageSequence;
    public String hangingPunctuation = ".,;:'-";
    public boolean enableLinkPageNum;
    public String linkPageNumFormat = "[%04d]";
    public int linkPageNumMax = 9999;
    public String bodiesToRender = "//body";
    public boolean fullCompression = false;
    boolean enableDoubleRenderingOutline = true;

    public GeneralSettings()
    {
    }
}
