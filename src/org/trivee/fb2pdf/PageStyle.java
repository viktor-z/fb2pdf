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

    public boolean enforcePageSize;
    public String pageSizeEnforcerColor;

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

}