package org.trivee.fb2pdf;

import java.util.HashMap;

public class PageStyle
{
    private Dimension pageWidth;
    private Dimension pageHeight;

    private Dimension marginLeft;
    private Dimension marginRight;
    private Dimension marginTop;
    private Dimension marginBottom;
    private boolean marginMirroring;

    public boolean enforcePageSize;
    public String pageSizeEnforcerColor;
    public boolean preventWidows = true;
    public boolean footnotes = false;
    public int footnotesMaxLines = 5;

    public String backgroundColor = "0xFFFFFF";

    public HashMap<Integer, Float> sectionNewPage = new HashMap<Integer, Float>();

    public PageStyle()
    {
    }

    public float getPageWidth()     { return pageWidth.getPoints(); }
    public float getPageHeight()    { return pageHeight.getPoints(); }

    public float getMarginLeft()    { return marginLeft.getPoints(); }
    public float getMarginRight()   { return marginRight.getPoints(); }
    public float getMarginTop()     { return marginTop.getPoints(); }
    public float getMarginBottom()  { return marginBottom.getPoints(); }
    public boolean getMarginMirroring() { return marginMirroring; }
}