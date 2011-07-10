package org.trivee.fb2pdf;

import java.util.HashMap;

public class PageStyle
{
    private Dimension pageWidth;
    private Dimension pageHeight;
    private int pageRotation;

    private Dimension marginLeft;
    private Dimension marginRight;
    private Dimension marginTop;
    private Dimension marginBottom;
    private Dimension imageExtraMargins;
    private boolean marginMirroring;

    public boolean enforcePageSize;
    public String pageSizeEnforcerColor;
    public boolean preventWidows = true;
    public boolean footnotes = false;
    public int footnotesMaxLines = 5;
    public int footnoteMaxLines = Integer.MAX_VALUE;
    public boolean tableCellsAutoWidth = false;
    private HeaderSettings header = new HeaderSettings();

    public String backgroundColor = null;
    public String backgroundImage = null;

    public HashMap<Integer, Float> sectionNewPage = new HashMap<Integer, Float>();

    public PageStyle()
    {
        try {
            imageExtraMargins = new Dimension("0pt");
        } catch (FB2toPDFException ex) {
            
        }
    }

    public float getPageWidth()     { return pageWidth.getPoints(); }
    public float getPageHeight()    { return pageHeight.getPoints(); }
    public int getPageRotation()    { return pageRotation; }

    public float getMarginLeft()    { return marginLeft.getPoints(); }
    public float getMarginRight()   { return marginRight.getPoints(); }
    public float getMarginTop()     { return marginTop.getPoints(); }
    public float getMarginBottom()  { return marginBottom.getPoints(); }
    public float getImageExtraMargins()  { return imageExtraMargins.getPoints(); }
    public boolean getMarginMirroring() { return marginMirroring; }

    public void setMarginTop(float adjustedMargin) throws FB2toPDFException {
        marginTop.setDimension(adjustedMargin);
    }

    /**
     * @return the header
     */
    public HeaderSettings getHeader() {
        return header;
    }
}