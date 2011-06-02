package org.trivee.fb2pdf;

import java.io.*;
import java.util.StringTokenizer;
import nu.xom.ParsingException;
import nu.xom.Element;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Image;
import com.itextpdf.text.FootnoteLineImage;
import com.itextpdf.text.Phrase;

import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfAction;

import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfTemplate;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import nu.xom.Builder;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.Text;
import nu.xom.XPathContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

public class FB2toPDF {

    public static String FB2PDF_HOME = ".";

    static {
        String fb2pdf_home = System.getenv("FB2PDF_HOME");
        if (fb2pdf_home != null) {
            FB2PDF_HOME = fb2pdf_home;
        }
    }
    private static final String NS_XLINK = "http://www.w3.org/1999/xlink";
    private static final String NS_FB2 = "http://www.gribuser.ru/xml/fictionbook/2.0";
    private static final XPathContext xCtx = new XPathContext("fb", NS_FB2);
    private static final String[][] TRANSTABLE = {
        // верхний регистр
        // трехбуквенные замены
        {"\u0429", "SCH"},
        // двухбуквенные замены
        {"\u0401", "YO"},
        {"\u0416", "ZH"},
        {"\u0426", "TS"},
        {"\u0427", "CH"},
        {"\u0428", "SH"},
        {"\u042E", "YU"},
        {"\u042F", "YA"},
        // однобуквенные замены
        {"\u0410", "A"},
        {"\u0411", "B"},
        {"\u0412", "V"},
        {"\u0413", "G"},
        {"\u0414", "D"},
        {"\u0415", "E"},
        {"\u0417", "Z"},
        {"\u0418", "I"},
        {"\u0419", "J"},
        {"\u041A", "K"},
        {"\u041B", "L"},
        {"\u041C", "M"},
        {"\u041D", "N"},
        {"\u041E", "O"},
        {"\u041F", "P"},
        {"\u0420", "R"},
        {"\u0421", "S"},
        {"\u0422", "T"},
        {"\u0423", "U"},
        {"\u0424", "F"},
        {"\u0425", "H"},
        {"\u042D", "E"},
        {"\u042B", "Y"},
        {"\u042C", "`"},
        {"\u042A", "'"},
        // нижний регистр
        // трехбуквенные замены
        {"\u0449", "sch"},
        // двухбуквенные замены
        {"\u0451", "yo"},
        {"\u0436", "zh"},
        {"\u0446", "ts"},
        {"\u0447", "ch"},
        {"\u0448", "sh"},
        {"\u044E", "yu"},
        {"\u044F", "ya"},
        // однобуквенные замены
        {"\u0430", "a"},
        {"\u0431", "b"},
        {"\u0432", "v"},
        {"\u0433", "g"},
        {"\u0434", "d"},
        {"\u0435", "e"},
        {"\u0437", "z"},
        {"\u0438", "i"},
        {"\u0439", "j"},
        {"\u043A", "k"},
        {"\u043B", "l"},
        {"\u043C", "m"},
        {"\u043D", "n"},
        {"\u043E", "o"},
        {"\u043F", "p"},
        {"\u0440", "r"},
        {"\u0441", "s"},
        {"\u0442", "t"},
        {"\u0443", "u"},
        {"\u0444", "f"},
        {"\u0445", "h"},
        {"\u044D", "e"},
        {"\u044C", "`"},
        {"\u044B", "y"},
        {"\u044A", "'"},
        // ukrainian
        {"\u0454", "ie"},
        {"\u0404", "IE"},
        {"\u0456", "i"},
        {"\u0406", "I"},
        {"\u0457", "yi"},
        {"\u0407", "YI"},
        {"\u0491", "g"},
        {"\u0490", "G"},};

    private String fromName;
    private String toName;
    private nu.xom.Document fb2;
    private com.itextpdf.text.Document doc;
    private PdfWriter writer;
    int bodyIndex;
    private Elements bodies;
    private Paragraph currentParagraph;
    private String currentReference;
    private Chunk currentChunk;
    private boolean superscript;
    private boolean subscript;
    private HyphenationAuto hyphenation;
    private Stylesheet stylesheet;
    private Stack<String> anchorStack = new Stack<String>();
    private Map<String, Integer> linkPageNumbers = new HashMap<String, Integer>();
    private LinkPageNumTemplateMap linkPageNumTemplates = new LinkPageNumTemplateMap();
    private ParagraphStyle currentStyle;
    private Map<Integer, PdfOutline> currentOutline = new HashMap<Integer, PdfOutline>();
    private ArrayList<BinaryAttachment> attachments = new ArrayList<BinaryAttachment>();

    private FB2toPDF(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }

    private void addFootnote(Element child) throws DocumentException, FB2toPDFException {
        if (stylesheet.getPageStyle().footnotes && "note".equals(child.getAttributeValue("type"))) {
            addFootnote(child.getValue(), currentReference);
        }
    }

    private void applyTransformations() throws RuntimeException {
        try {
            Transformation.transform(fb2, stylesheet.getTransformationSettings());
        } catch (Exception ex) {
            throw new RuntimeException("Error processing transformation. " + ex.getMessage());
        }
    }

    private void applyXPathStyles() throws RuntimeException {
        String prolog = "declare default element namespace \"http://www.gribuser.ru/xml/fictionbook/2.0\"; "
        + "declare namespace l = \"http://www.w3.org/1999/xlink\"; ";
        String morpher1 = prolog + "attribute {'fb2pdf-style'} {'%s'}";
        String morpher2 = prolog + "(., attribute {'fb2pdf-style'} {'%s'})";
        for (ParagraphStyle style : stylesheet.getParagraphStyles()) {
                String xpath = style.getSelector();
                String name = style.getName();
                if (isNullOrEmpty(xpath)) continue;
                
            try {
                Transformation.transform(fb2, prolog + xpath + "/@fb2pdf-style", String.format(morpher1, name));
                Transformation.transform(fb2, prolog + xpath + "/*[last()]", String.format(morpher2, name));
                Transformation.transform(fb2, prolog + xpath + "/text()[last()]", String.format(morpher2, name));
                Transformation.outputDebugInfo(fb2, stylesheet.getTransformationSettings(), "styling-result.xml");
            } catch (Exception ex) {
                throw new RuntimeException("Error applying styles. " + ex.getMessage());
            }
        }
    }

    private PdfPTable createHeaderTable() throws DocumentException, FB2toPDFException {
        //PdfPTable table = new PdfPTable(2);
        //table.setWidths(new float[]{0.5f, 0.5f});
        PdfPTable table = new PdfPTable(1);
        table.setWidths(new float[]{1.0f});
        table.setTotalWidth(doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin());
        table.getDefaultCell().setBorder(Rectangle.BOTTOM);
        table.getDefaultCell().setPaddingBottom(4);
        table.getDefaultCell().setNoWrap(true);
        
        final ParagraphStyle headerStyle = stylesheet.getParagraphStyle("header");
        table.getDefaultCell().setBorderColor(headerStyle.getColor());
        Chunk chunk = headerStyle.createChunk();
        String author = getTextContent(fb2.query("//fb:title-info//fb:author[1]//fb:first-name | //fb:title-info//fb:author[1]//fb:last-name", xCtx), "", "");
        String title = getTextContent(fb2.query("//fb:title-info//fb:book-title", xCtx), "", "");
        chunk.append(author + ", " + title);
        table.addCell(new Phrase(chunk));
        //table.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        //chunk = stylesheet.getParagraphStyle("header").createChunk();
        //chunk.append(title);
        //table.addCell(new Phrase(chunk));
        return table;
    }

    private void getNoteText(Element element, StringBuilder text, boolean skipTitle) {
        Elements children = element.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String localName = child.getLocalName();
            if(isNullOrEmpty(localName)) {
                continue;
            }
            if(localName.equals("poem") || localName.equals("stanza")){
                getNoteText(child, text, false);
            } else if (localName.equals("p") || localName.equals("v") || localName.equals("text-author") ||
                    localName.equals("date") || localName.equals("epigraph") ||
                    (!skipTitle && localName.equals("title"))) {
                Element paragraph = child;
                String paragraphText = paragraph.getValue();
                paragraphText = paragraphText.replaceAll("\n", " ").replaceAll("  ", " ").trim();
                if (paragraphText.isEmpty()) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append("    ");
                }
                text.append(paragraphText);
                text.append("\n");
            }
        }
    }

    private void fillPageNumTemplate(String referenceName) throws FB2toPDFException {
        String pageNumFormat = stylesheet.getGeneralSettings().linkPageNumFormat;
        for (LinkPageNumTemplate lpnt : linkPageNumTemplates.get(referenceName)) {
            PdfTemplate tp = lpnt.template;
            ParagraphStyle tpStyle = lpnt.style;
            BaseFont tpBaseFont = tpStyle.getBaseFont();
            float tpSize = tpStyle.getFontSize().getPoints();
            String pageNum = String.format(pageNumFormat, linkPageNumbers.get(referenceName));
            float tpHight = tpBaseFont.getFontDescriptor(BaseFont.CAPHEIGHT, tpSize) + tpBaseFont.getAscentPoint(pageNum, tpSize);
            float tpWidth = tpBaseFont.getWidthPointKerned(pageNum, tpSize);
            tp.setBoundingBox(new Rectangle(0, tpBaseFont.getDescentPoint(pageNum, tpSize), tpWidth, tpHight));
            //tp.saveState();
            //tp.setColorFill(BaseColor.RED);
            //tp.rectangle(0, 0, 200, 200);
            //tp.fillStroke();
            //tp.restoreState();
            tp.beginText();
            tp.setFontAndSize(tpStyle.getBaseFont(), tpSize);
            tp.showText(pageNum);
            tp.endText();
        }
    }

    private void addInvisibleAnchor(Element child) throws FB2toPDFException, DocumentException {
        String id = child.getAttributeValue("id");
        if (id != null && id.length() > 0) {
            addInvisibleAnchor(id);
        }
    }

    protected void addInvisibleAnchor(String name) {
        anchorStack.push(name);
    }

    protected void addBacklink(String id) throws DocumentException, FB2toPDFException {
        Chunk chunk = currentStyle.createChunk();
        chunk.append("[^^^]");
        addGoToActionToChunk(id + "_backlink", chunk);
        addEmptyLine();
        addLine(chunk, currentStyle);
    }

    protected void addCenteredImage(Image image) throws DocumentException, FB2toPDFException {
        Rectangle pageSize = doc.getPageSize();
        float dpi = stylesheet.getGeneralSettings().imageDpi;
        float scaleWidth = pageSize.getWidth() - doc.leftMargin() - doc.rightMargin();
        float scaleHeight = pageSize.getHeight() - doc.topMargin() - doc.bottomMargin();
        float imgWidth = image.getWidth() / dpi * 72;
        float imgHeight = image.getHeight() / dpi * 72;
        float Y = (scaleHeight - imgHeight) / 2;
        float X = (scaleWidth - imgWidth) / 2;
        X = X > 0 ? X : 0;
        Y = Y > 0 ? Y : 0;
        image.setAbsolutePosition(X, Y);
        addImage(image);
    }

    protected void addImage(Image image) throws DocumentException, FB2toPDFException {
        rescaleImage(image);
        image.setAlignment(Image.MIDDLE);
        doc.add(image);
    }

    private int[] getCellWidths(int colNumber, Elements rows) {
        int[] lengths = new int[colNumber];
        for (int i = 0; i < rows.size(); i++) {
            int curcol = 0;
            Element row = rows.get(i);
            Elements cols = row.getChildElements();
            for (int j = 0; j < cols.size(); j++) {

                String nodeName = cols.get(j).getLocalName();
                if (!nodeName.equals("th") && !nodeName.equals("td")) {
                    continue;
                }

                Element cellElement = (Element) cols.get(j);
                int length = cellElement.getValue().length();
                
                int colspan = getCellElementSpan(cellElement, "colspan");
                for (int k=0;k<colspan; k++) {
                    lengths[curcol+k] = Math.max(length, lengths[curcol+k]);
                }

                curcol += colspan;
            }
        }
        int totalMaxLength = 0;
        int maxLength = 0;
        for (int l : lengths) {
            totalMaxLength += l;
            maxLength = Math.max(maxLength, l);
        }
        
        int numOfUnits = 10;
        int unit = Math.round(100f / numOfUnits);
        for (int i=0; i<colNumber; i++) {
            int l = lengths[i];
            lengths[i] = Math.round((float)l / totalMaxLength * numOfUnits);
            lengths[i] = Math.max(lengths[i], 1);
        }
        return lengths;
    }

    private void saveLinkPageNumber(String currentAnchorName) {
        if (!stylesheet.getGeneralSettings().enableLinkPageNum) {
            return;
        }

        linkPageNumbers.put("#" + currentAnchorName, writer.getPageNumber());
    }

    private void addPageNumTemplate() throws BadElementException, FB2toPDFException {
        GeneralSettings settings = stylesheet.getGeneralSettings();
        if (!settings.enableLinkPageNum || !currentReference.startsWith("#")) {
            return;
        }

        String text = String.format(settings.linkPageNumFormat, settings.linkPageNumMax);
        float tmpSize = currentStyle.getFontSize().getPoints();
        BaseFont tmpBasefont = currentStyle.getBaseFont();
        float templateHight = tmpBasefont.getFontDescriptor(BaseFont.CAPHEIGHT, tmpSize);
        float templateWidth = tmpBasefont.getWidthPointKerned(text, tmpSize);
        PdfTemplate template = writer.getDirectContent().createTemplate(templateWidth, templateHight);
        Image templateImage = Image.getInstance(template);
        Chunk chunk = new Chunk(templateImage, 0, 0, false);
        chunk.setFont(currentStyle.getFont());
        currentParagraph.add(chunk);
        linkPageNumTemplates.put(currentReference, new LinkPageNumTemplate(template, currentStyle));
    }

    private float rescaleImage(Image image) throws FB2toPDFException {
        return rescaleImage(image, 1.0f);
    }

    private float rescaleImage(Image image, float zoomFactor) throws FB2toPDFException {
        Rectangle pageSize = doc.getPageSize();
        float dpi = stylesheet.getGeneralSettings().imageDpi;
        float hSpace = doc.topMargin() + doc.bottomMargin() + image.getSpacingAfter() + image.getSpacingBefore();
        float wSpace = doc.leftMargin() + doc.rightMargin() + stylesheet.getPageStyle().getImageExtraMargins();
        if (currentStyle != null) {
            hSpace += currentStyle.getAbsoluteLeading() + currentStyle.getFontSize().getPoints();
        }
        float scaleWidth = pageSize.getWidth() - wSpace;
        float scaleHeight = pageSize.getHeight() - hSpace;
        float imgWidth = image.getWidth() / dpi * 72 * zoomFactor;
        float imgHeight = image.getHeight() / dpi * 72 * zoomFactor;
        if ((imgWidth <= scaleWidth) && (imgHeight <= scaleHeight)) {
            scaleWidth = imgWidth;
            scaleHeight = imgHeight;
        }
        image.scaleToFit(scaleWidth, scaleHeight);
        return scaleHeight;
    }

    protected void addInlineImage(Image image) throws DocumentException, FB2toPDFException {
        float zoom = currentStyle.getInlineImageZoom();
        rescaleImage(image, zoom);
        float offsetY = currentStyle.getInlineImageOffsetY();
        Chunk chunk = new Chunk(image, 0, offsetY, true);
        chunk.setFont(currentStyle.getFont());
        currentParagraph.add(chunk);
    }

    protected void addLine(Chunk chunk, ParagraphStyle style) throws FB2toPDFException, DocumentException {
        Paragraph para = style.createParagraph();
        para.add(chunk);
        doc.add(para);
    }

    protected void addStretchedImage(Image image) throws DocumentException {
        Rectangle pageSize = doc.getPageSize();
        image.scaleToFit(pageSize.getWidth() - doc.leftMargin() - doc.rightMargin(), pageSize.getHeight() - doc.topMargin() - doc.bottomMargin());
        image.setAlignment(Image.MIDDLE);
        doc.add(image);
    }

    private void addGoToActionToChunk(String refname, Chunk chunk) {
        PdfAction action = PdfAction.gotoLocalPage(refname, false);
        System.out.println("Adding Action LocalGoTo " + refname);
        chunk.setAction(action);
    }

    private void addFootnote(String marker, String refname) throws DocumentException, FB2toPDFException {
        if (isNullOrEmpty(refname)) {
            System.out.println("Skipping footnote with empty reference");
            return;
        }
        refname = refname.substring(1);
        System.out.println("Adding footnote " + refname);
        String body = marker + " " + getNoteBody(refname);
        byte[] noteDoc = FootnoteRenderer.renderNoteDoc(stylesheet, body, hyphenation);
        List<Image> noteLineImages = getLinesImages(noteDoc, refname);
        for (Image image : noteLineImages) {
            doc.add(image);
        }
    }

    /*
    private void appendLF() throws FB2toPDFException, DocumentException {
    flushCurrentChunk();
    currentChunk = currentStyle.createChunk();
    currentChunk.append("\n");
    flushCurrentChunk();
    }
     */
    private String getNoteBody(String refname) {
        if (StringUtils.isBlank(refname)) {
            return "";
        }
        
        String query = String.format("//fb:body[@name]//fb:section[@id='%s']", refname);
        Nodes sections = fb2.getRootElement().query(query, xCtx);
        if (sections.size() > 1) {
            System.out.printf("WARNING: more than one note %s found\n", refname);
        }
        if (sections.size() > 0) {
            StringBuilder text = new StringBuilder();
            getNoteText((Element)sections.get(0), text, true);
            return text.toString();
        }

        System.out.printf("WARNING: note %s not found\n", refname);
        return "";
    }

    private List<Image> getLinesImages(byte[] noteDoc, String refname) {

        List<Image> result = new ArrayList<Image>();
        try {
            PdfReader reader = new PdfReader(noteDoc);

            int numPages = reader.getNumberOfPages() - 1;
            int maxLines = stylesheet.getPageStyle().footnoteMaxLines;
            int numLines = Math.min(maxLines, numPages);
            System.out.printf("Footnote has %d lines, maximum in settings is %d, will render %d\n", numPages, maxLines, numLines);

            for (int i = 1; i <= numLines; i++) {
                PdfImportedPage page = writer.getImportedPage(reader, i);
                Image image = null;
                if (numLines < numPages && i == numLines) {
                    PdfTemplate tmp = PdfTemplate.createTemplate(writer, page.getWidth(), page.getHeight());
                    PdfImportedPage cutImg = writer.getImportedPage(reader, numPages+1);
                    page.setWidth(tmp.getWidth() - cutImg.getWidth());
                    tmp.addTemplate(page, 0, 0);
                    tmp.addTemplate(cutImg, page.getWidth(), 0);
                    image = FootnoteLineImage.getInstance(tmp, refname);
                } else {
                    image = FootnoteLineImage.getInstance(page, refname);
                }
                image.setSpacingBefore(0);
                image.setSpacingAfter(0);
                image.setAlignment(Image.MIDDLE);
                image.setBorderColor(stylesheet.getParagraphStyle("footnote").getColor());
                result.add(image);
            }
        } catch (Exception ex) {
            System.out.println("WARNING: failed to produce footnote lines: " + ex);
        }

        return result;
    }

    private ParagraphStyle getStyleForElement(Element element) {

        ParagraphStyle result = currentStyle;

        String elementStyleAttr = element.getAttributeValue("fb2pdf-style");

        if (isNullOrEmpty(elementStyleAttr)) {
            return result;
        }

        try {
            result = stylesheet.getParagraphStyle(elementStyleAttr);
        } catch (FB2toPDFException ex) {
            System.out.println("Element style not found: " + elementStyleAttr);
        }

        System.out.println("Element style found: " + elementStyleAttr);
        return result;
    }

    private void processTable(Element table) throws DocumentException, FB2toPDFException {
        stylesheet.getGeneralSettings().enableInlineImages = true;
        List<PdfPCell> cells = new LinkedList<PdfPCell>();
        Elements rows = table.getChildElements("tr", NS_FB2);
        int maxcol = 0;
        for (int i = 0; i < rows.size(); i++) {
            int curcol = 0;
            Element row = rows.get(i);
            Elements cols = row.getChildElements();
            for (int j = 0; j < cols.size(); j++) {

                String nodeName = cols.get(j).getLocalName();
                if (!nodeName.equals("th") && !nodeName.equals("td")) {
                    continue;
                }

                if (nodeName.equalsIgnoreCase("td")) {
                    currentStyle = stylesheet.getParagraphStyle("tableTD");
                }
                if (nodeName.equalsIgnoreCase("th")) {
                    currentStyle = stylesheet.getParagraphStyle("tableTH");
                }
                Element cellElement = (Element) cols.get(j);
                currentParagraph = currentStyle.createParagraph();
                if (i == 0 && j == 0) {
                    addInvisibleAnchor(table);
                }
                processParagraphContent(cellElement);
                flushCurrentChunk();


                PdfPCell cell = new PdfPCell(currentParagraph);
                int colspan = setTableCellAttributes(cellElement, cell);

                currentParagraph = null;
                currentReference = null;
                
                cells.add(cell);
                curcol += colspan;
            }
            maxcol = Math.max(curcol, maxcol);
        }
        PdfPTable pdftable = new PdfPTable(maxcol);
        for (PdfPCell cell : cells) {
            pdftable.addCell(cell);
        }

        ParagraphStyle tableStyle = stylesheet.getParagraphStyle("table");
        pdftable.setSpacingBefore(tableStyle.getSpacingBefore());
        pdftable.setSpacingAfter(tableStyle.getSpacingAfter());
        float pageWidth = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
        pdftable.setWidthPercentage((pageWidth - tableStyle.getLeftIndent() - tableStyle.getRightIndent())/pageWidth*100f);
        if (stylesheet.getPageStyle().tableCellsAutoWidth) {
            int[] widths = getCellWidths(maxcol, rows);
            pdftable.setWidths(widths);
        }
        doc.add(pdftable);
    }

    private int setTableCellAttributes(Element cellElement, PdfPCell cell) throws NumberFormatException {

        Map<String, Integer> hAlignMap = new HashMap<String, Integer>() {

            {
                put("center", PdfPCell.ALIGN_CENTER);
                put("left", PdfPCell.ALIGN_LEFT);
                put("right", PdfPCell.ALIGN_RIGHT);
            }
        };
        Map<String, Integer> vAlignMap = new HashMap<String, Integer>() {

            {
                put("middle", PdfPCell.ALIGN_MIDDLE);
                put("top", PdfPCell.ALIGN_TOP);
                put("bottom", PdfPCell.ALIGN_BOTTOM);
            }
        };

        
        int colspan = getCellElementSpan(cellElement, "colspan");
        int rowspan = getCellElementSpan(cellElement, "rowspan");

        String alignAttr = cellElement.getAttributeValue("align");
        String valignAttr = cellElement.getAttributeValue("valign");
        int hAlign = isNullOrEmpty(alignAttr) ? PdfPCell.ALIGN_CENTER : hAlignMap.get(alignAttr);
        int vAlign = isNullOrEmpty(valignAttr) ? PdfPCell.ALIGN_MIDDLE : vAlignMap.get(valignAttr);

        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(hAlign);
        cell.setVerticalAlignment(vAlign);
        
        return colspan;
    }
    
    private int getCellElementSpan(Element cellElement, String attrName) {
        String spanAttr = cellElement.getAttributeValue(attrName);
        return isNullOrEmpty(spanAttr) ? 1 : Integer.parseInt(spanAttr);
    }

    protected int getDropCapCount(String text) {
        int idx = 1;
        if (Character.isDigit(text.charAt(0))) {
            for (int c = 0; c < text.length(); c++) {
                if (!Character.isDigit(text.charAt(c))) {
                    idx = c;
                    break;
                }
            }
            if (idx > 9) {
                idx = 1;
            }
        } else if (!Character.isLetter(text.charAt(0))) {
            for (int c = 0; c < text.length(); c++) {
                if (Character.isLetter(text.charAt(c))) {
                    idx = c + 1;
                    break;
                }
            }
            if (idx > 9) {
                idx = 1;
            }
        }
        return idx;
    }

    private static Element getNextSibling(Element current) {
      ParentNode parent = current.getParent();
      if (parent == null) return null;
      int index = parent.indexOf(current);
      for (int i=index+1;i < parent.getChildCount(); i++) {
          if (parent.getChild(i) instanceof Element) {
              return (Element)parent.getChild(i);
          }
      }
      return null;
    }

    private static Element getPrevSibling(Element current) {
      ParentNode parent = current.getParent();
      if (parent == null) return null;
      int index = parent.indexOf(current);
      for (int i=index-1;i >= 0; i--) {
          if (parent.getChild(i) instanceof Element) {
              return (Element)parent.getChild(i);
          }
      }
      return null;
    }

    private boolean isIgnoreEmptyLine(Element element) {

        Element nextSib = getNextSibling(element);
        Element prevSib = getPrevSibling(element);
        boolean ignore = false;
        if (nextSib != null && nextSib.getLocalName().equalsIgnoreCase("image")
                && stylesheet.getGeneralSettings().ignoreEmptyLineBeforeImage) {
            System.out.println("Skipping empty line before image");
            ignore = true;
        }
        if (prevSib != null && prevSib.getLocalName().equalsIgnoreCase("image")
                && stylesheet.getGeneralSettings().ignoreEmptyLineAfterImage) {
            System.out.println("Skipping empty line after image");
            ignore = true;
        }

        return ignore;
    }

    private void addImage(Element element) throws DocumentException, FB2toPDFException {
        addImage(element, false);
    }

    private void addImage(Element element, boolean inline) throws DocumentException, FB2toPDFException {
        String href = element.getAttributeValue("href", NS_XLINK);
        Image image = getImage(href);
        if (image != null) {
            if (inline) {
                addInlineImage(image);
            } else {
                addInvisibleAnchor(element);
                addImage(image);
            }
        } else {
            System.out.println("Image not found, href: " + href);
        }
    }

    private String getSequenceSubtitle(Element seq) {
        String seqname = seq.getAttributeValue("name");
        String seqnumber = seq.getAttributeValue("number");
        String subtitle = "";
        if (!isNullOrEmpty(seqname)) {
            subtitle += seqname;
        }
        if (!isNullOrEmpty(seqnumber)) {
            subtitle += " #" + seqnumber;
        }
        if (!isNullOrEmpty(subtitle)) {
            subtitle = "(" + subtitle + ")";
        }
        return subtitle;
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    private void loadData(InputStream stylesheetInputStream)
            throws DocumentException, IOException, FB2toPDFException {
        if (stylesheetInputStream == null) {
            stylesheet = Stylesheet.readStylesheet(FB2PDF_HOME + "/data/stylesheet.json");
        } else {
            stylesheet = Stylesheet.readStylesheet(stylesheetInputStream);
        }
    }

    private void readFB2()
            throws IOException, FileNotFoundException {
        
        InputStream is = null;
        if (fromName.toLowerCase().endsWith(".fb2")) {
            is = new FileInputStream(new File(fromName));
        } else if (fromName.toLowerCase().endsWith(".zip")) {
            ZipFile fromZip = new ZipFile(fromName);
            ZipEntry entry = (ZipEntry) fromZip.getEntries().nextElement();
            if (entry.getName().toLowerCase().endsWith(".fb2")) {
                is = fromZip.getInputStream(entry);
            } else {
                System.err.println("First archive entry " + entry.getName() + " is not an FB2 file.");
                System.exit(-1);
            }
        } else {
            System.err.println("Unrecognized file extension: " + fromName + ", only FB2 or ZIP supported.");
            System.exit(-1);
        }

        try {
            fb2 = new Builder(false).build(is);
        } catch (ParsingException e) {
            System.err.println("XML parsing error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
        }
    }

    private String getValidatedFileName(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            return filename;
        }

        String baseDir = CLIDriver.getBaseDir();
        file = new File(new File(baseDir).getParent() + "/" + filename);
        String fullFilename = file.getCanonicalPath();
        if (!file.exists()) {
            throw new IOException(String.format("File not found [%s or %s]", filename, fullFilename));
        }
        return fullFilename;
    }

    private void createPDFDoc()
            throws DocumentException, FileNotFoundException {
        final PageStyle pageStyle = stylesheet.getPageStyle();
        final GeneralSettings generalSettings = stylesheet.getGeneralSettings();

        Rectangle pageSize = new Rectangle(pageStyle.getPageWidth(), pageStyle.getPageHeight(), pageStyle.getPageRotation());
        if(!isNullOrEmpty(pageStyle.backgroundColor)) {
            pageSize.setBackgroundColor(new BaseColor(Color.decode(pageStyle.backgroundColor)));
        }

        System.out.println("Page size is " + pageSize);
        
        doc = new com.itextpdf.text.Document(pageSize,
                pageStyle.getMarginLeft(), pageStyle.getMarginRight(),
                pageStyle.getMarginTop(), pageStyle.getMarginBottom());

        writer = PdfWriter.getInstance(doc, new FileOutputStream(toName));
        if (pageStyle.enforcePageSize) {
            PageSizeEnforceHelper pageSizeEnforceHelper = new PageSizeEnforceHelper();
            pageSizeEnforceHelper.enforcePageSize = pageStyle.enforcePageSize;
            pageSizeEnforceHelper.pageSizeEnforcerColor = Color.decode(pageStyle.pageSizeEnforcerColor);
            writer.setPageEvent(pageSizeEnforceHelper);
        }
        
        if (!isNullOrEmpty(pageStyle.backgroundImage)){
            try {
                Image image = Image.getInstance(getValidatedFileName(pageStyle.backgroundImage));
                rescaleImage(image);
                BackgroundImageHelper backgroundImageHelper = new BackgroundImageHelper(image);
                writer.setPageEvent(backgroundImageHelper);
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

        writer.setSpaceCharRatio(generalSettings.trackingSpaceCharRatio);
        writer.setStrictImageSequence(generalSettings.strictImageSequence);
        PdfDocument.preventWidows = pageStyle.preventWidows;
        PdfDocument.maxFootnoteLines = pageStyle.footnotesMaxLines;
        PdfDocument.hangingPunctuation = generalSettings.hangingPunctuation;
        doc.setMarginMirroring(pageStyle.getMarginMirroring());
    }

    private void closePDF() throws FB2toPDFException {

        for (String destination : linkPageNumbers.keySet()) {
            if (linkPageNumTemplates.containsKey(destination)) {
                fillPageNumTemplate(destination);
            }
        }

        doc.close();
    }

    private static Element getOptionalChildByTagName(Element element, String tagName)
            throws FB2toPDFException {
        Elements children = element.getChildElements(tagName, NS_FB2);
        if (children.size() == 0) {
            return null;
        } else if (children.size() > 1) {
            throw new FB2toPDFException("More than one element found: " + element.getLocalName() + "/" + tagName);
        }
        return children.get(0);
    }

    private static String fixCharacters(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\u00A0') {
                c = ' ';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String removeExtraWhitespace(String text, boolean startWithSpace) {
        // normalize whitespace
        StringTokenizer st = new StringTokenizer(text);
        StringBuilder sb = new StringBuilder();
        boolean bFirst = !startWithSpace;
        while (st.hasMoreTokens()) {
            if (!bFirst) {
                sb.append(" ");
            }
            bFirst = false;
            sb.append(st.nextToken());
        }
        return sb.toString();
    }

    private static String getTextContent(Elements children, String prefix, String suffix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); ++i) {
            String content = children.get(i).getValue() + " ";
            if (sb.length() == 0 && content.length() > 0 && prefix != null) {
                sb.append(prefix);
            }
            sb.append(content);
        }
        if (sb.length() > 0 && suffix != null) {
            sb.append(suffix);
        }
        return removeExtraWhitespace(fixCharacters(sb.toString()), prefix != null && prefix.length() > 0 && prefix.charAt(0) == ' ');
    }

    private static String getTextContent(Nodes children, String prefix, String suffix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); ++i) {
            String content = children.get(i).getValue() + " ";
            if (sb.length() == 0 && content.length() > 0 && prefix != null) {
                sb.append(prefix);
            }
            sb.append(content);
        }
        if (sb.length() > 0 && suffix != null) {
            sb.append(suffix);
        }
        return removeExtraWhitespace(fixCharacters(sb.toString()), prefix != null && prefix.length() > 0 && prefix.charAt(0) == ' ');
    }

    private static String getTextContentByTagName(Element element, String tagName, String prefix, String suffix) {
        // collect text content
        Elements children = element.getChildElements(tagName, NS_FB2);
        return getTextContent(children, prefix, suffix);
    }

    private static String getTextContentByTagName(Element element, String tagName, String prefix) {
        return getTextContentByTagName(element, tagName, prefix, null);
    }

    private static String getTextContentByTagName(Element element, String tagName) {
        return getTextContentByTagName(element, tagName, null, null);
    }

    private void run(InputStream stylesheetInputStream)
            throws IOException, DocumentException, FB2toPDFException {

        loadData(stylesheetInputStream);
        readFB2();
        applyTransformations();
        applyXPathStyles();
        createPDFDoc();

        nu.xom.Element root = fb2.getRootElement();
        if (!root.getLocalName().equals("FictionBook")) {
            throw new FB2toPDFException("The file does not seems to contain 'fictionbook' root element");
        }


        extractBinaries(root);

        bodies = root.getChildElements("body");
        bodies = root.getChildElements("body", NS_FB2);
        if (bodies.size() == 0) {
            throw new FB2toPDFException("Element not found: FictionBook/body");
        }


        Element description = getOptionalChildByTagName(root, "description");
        if (description != null) {
            setupHyphenation(description);
            addMetaInfo(description);
            doc.open();
            processDescription(description);
        } else {
            doc.open();
            System.err.println("Description not found");
        }
        currentOutline.put(0, writer.getDirectContent().getRootOutline());

        if (stylesheet.getPageStyle().header) {
            setupHeader();
        }
                
        Element body = bodies.get(0);
        if (stylesheet.getGeneralSettings().generateTOC) {
            makeTOCPage(body);
        }

        for (int i = 0; i < bodies.size(); ++i) {
            body = (Element) bodies.get(i);
            bodyIndex = i;
            processBody(body);
            doc.newPage();
        }

        closePDF();
    }

    private void setupHeader() throws FB2toPDFException, DocumentException, BadElementException {
        PdfPTable table = createHeaderTable();
        float adjustedMargin =  doc.topMargin() + table.getTotalHeight();
        stylesheet.getPageStyle().setMarginTop(adjustedMargin);
        doc.setMargins(doc.leftMargin(), doc.rightMargin(), adjustedMargin , doc.bottomMargin());
        HeaderHelper footerHelper = new HeaderHelper(doc, writer, table);
        writer.setPageEvent(footerHelper);
    }

    private static class BinaryAttachment {

        private String href;
        private String contentType;
        private nu.xom.Element binary;

        public BinaryAttachment(nu.xom.Element binary) {
            this.href = "#" + binary.getAttributeValue("id");
            this.contentType = binary.getAttributeValue("content-type");
            this.binary = binary;
        }

        public String getHREF() {
            return href;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            System.out.println("Loaded binary " + this.href + " (" + this.contentType + ")");
            return Base64.decodeBase64(this.binary.getValue().getBytes());
        }
    };

    private void extractBinaries(nu.xom.Element root) {
        Elements binaries = root.getChildElements("binary", NS_FB2);
        for (int i = 0; i < binaries.size(); ++i) {
            nu.xom.Element binary = binaries.get(i);
            attachments.add(new BinaryAttachment(binary));
        }
    }

    private void processDescription(Element description)
            throws FB2toPDFException, DocumentException {
        makeCoverPage(description);
        makeBookInfoPage(description);
    }

    private void addLine(String text, ParagraphStyle style)
            throws FB2toPDFException, DocumentException {
        Chunk chunk = style.createChunk();
        chunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings(), currentStyle));
        addLine(chunk, style);
    }

    private void makeCoverPage(Element description)
            throws FB2toPDFException, DocumentException {
        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo == null) {
            System.err.println("Title info not found");
            return;
        }

        Element coverPage = getOptionalChildByTagName(titleInfo, "coverpage");
        if (coverPage != null) {
            Elements images = coverPage.getChildElements("image", NS_FB2);
            for (int i = 0; i < images.size(); ++i) {
                Element coverImage = images.get(i);

                String href = coverImage.getAttributeValue("href", NS_XLINK);
                Image image = getImage(href);
                if (image != null) {
                    if (stylesheet.getGeneralSettings().stretchCover) {
                        addStretchedImage(image);
                    } else {
                        addCenteredImage(image);
                    }
                    doc.newPage();
                }
            }
        }

    }

    private String getAuthorFullName(Element author) throws FB2toPDFException {
        String firstName = getTextContentByTagName(author, "first-name");
        String middleName = getTextContentByTagName(author, "middle-name");
        String lastName = getTextContentByTagName(author, "last-name");
        return String.format("%s %s %s", firstName, middleName, lastName).trim();
    }

    private void addMetaInfo(Element description)
            throws FB2toPDFException, DocumentException {
        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            Elements authors = titleInfo.getChildElements("author", NS_FB2);
            StringBuilder allAuthors = new StringBuilder();

            boolean force = stylesheet.getGeneralSettings().forceTransliterateAuthor;

            for (int i = 0; i < authors.size(); ++i) {
                Element author = authors.get(i);
                String authorName = transliterate(getAuthorFullName(author), force);
                System.out.println("Adding author: " + authorName);
                doc.addAuthor(authorName);

                if (allAuthors.length() > 0) {
                    allAuthors.append(", ");
                }
                allAuthors.append(authorName);
            }

            if (allAuthors.length() > 0) {
                doc.addAuthor(allAuthors.toString());
            }

            Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            Elements sequences = titleInfo.getChildElements("sequence", NS_FB2);

            if (bookTitle != null && sequences.size() == 0) {
                String titleString = bookTitle.getValue();
                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
            } else if (bookTitle != null && sequences.size() != 0) {
                for (int i = 0; i < sequences.size(); i++) {
                    String subtitle = getSequenceSubtitle(sequences.get(i));
                    doc.addTitle(transliterate(subtitle));
                    System.out.println("Adding subtitle: " + transliterate(subtitle));
                }

                String titleString = bookTitle.getValue();
                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
            }
        }
    }

    private void makeBookInfoPage(Element description)
            throws FB2toPDFException, DocumentException {
        ParagraphStyle titleStyle = stylesheet.getParagraphStyle("title");
        ParagraphStyle subtitleStyle = stylesheet.getParagraphStyle("subtitle");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("author");

        if (stylesheet.getGeneralSettings().generateFrontMatter) {
            makeFrontMatter(description);
        }

        addLine(" ", titleStyle);

        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            Elements authors = titleInfo.getChildElements("author", NS_FB2);
            for (int i = 0; i < authors.size(); ++i) {
                Element author = authors.get(i);
                String authorName = getAuthorFullName(author);
                // doc.addAuthor(transliterate(authorName));
                addLine(authorName, authorStyle);
            }

            Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            Elements sequences = titleInfo.getChildElements("sequence", NS_FB2);

            if (bookTitle != null && sequences.size() == 0) {
                addLine(" ", titleStyle);
                addLine(bookTitle.getValue(), titleStyle);
                addLine(" ", titleStyle);
            } else if (bookTitle != null && sequences.size() != 0) {
                addLine(" ", titleStyle);
                addLine(bookTitle.getValue(), titleStyle);
                for (int i = 0; i < sequences.size(); i++) {
                    String subtitle = getSequenceSubtitle(sequences.get(i));
                    addLine(subtitle, subtitleStyle);
                    addLine(" ", titleStyle);
                }
            }

            Element annotation = getOptionalChildByTagName(titleInfo, "annotation");
            if (annotation != null) {

                currentStyle = stylesheet.getParagraphStyle("annotation");
                processAnnotation(annotation, "annotationSubtitle");
                currentStyle = null;
            }
        }

        doc.newPage();
    }

    private void makeFrontMatter(Element description)
            throws FB2toPDFException, DocumentException {

        ParagraphStyle frontMatter = stylesheet.getParagraphStyle("frontMatter");
        Element publishInfo = getOptionalChildByTagName(description, "publish-info");
        if (publishInfo != null) {
            addLine(
                    getTextContentByTagName(publishInfo, "book-name", null, " // ")
                    + getTextContentByTagName(publishInfo, "publisher")
                    + getTextContentByTagName(publishInfo, "city", ", ")
                    + getTextContentByTagName(publishInfo, "year", ", "),
                    frontMatter);
            String isbn = getTextContentByTagName(publishInfo, "isbn");
            if (isbn.length() > 0) {
                addLine("ISBN: " + isbn, frontMatter);
            }
        }

        Element documentInfo = getOptionalChildByTagName(description, "document-info");
        Elements documentAuthors = null;
        if (documentInfo != null) {
            documentAuthors = documentInfo.getChildElements("author", NS_FB2);
        }

        for (int i = 0; documentAuthors != null && i < documentAuthors.size(); ++i) {
            Element documentAuthor = documentAuthors.get(i);
            addLine(
                    "FB2: "
                    + getTextContentByTagName(documentAuthor, "first-name", " ")
                    + getTextContentByTagName(documentAuthor, "last-name", " ")
                    + getTextContentByTagName(documentAuthor, "nickname", " \u201C", "\u201D")
                    + getTextContentByTagName(documentAuthor, "email", " <", ">")
                    + getTextContentByTagName(documentInfo, "date", ", ")
                    + getTextContentByTagName(documentInfo, "version", ", version "),
                    frontMatter);
        }

        if (documentInfo != null) {
            addLine(
                    "UUID: " + getTextContentByTagName(documentInfo, "id"),
                    frontMatter);
        }

        String buildDate = CLIDriver.class.getPackage().getImplementationVersion();
        String convertDate = java.text.DateFormat.getDateInstance().format(new java.util.Date());
        addLine(String.format("PDF: fb2pdf%s, %s", 
                (buildDate == null ? "" : "-j."+buildDate), convertDate), frontMatter);
    }

    private Image getImage(String href) {
        System.out.println("Loading image at " + href);
        for (int i = 0; i < attachments.size(); ++i) {
            BinaryAttachment attachment = attachments.get(i);
            if (!attachment.getHREF().equals(href)) {
                continue;
            }
            try {
                String overrideTransparency = stylesheet.getGeneralSettings().overrideImageTransparency;
                if (overrideTransparency == null || overrideTransparency.isEmpty()) {
                    return Image.getInstance(attachment.getData());
                } else {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    java.awt.Image img = toolkit.createImage(attachment.getData());
                    return Image.getInstance(img, Color.decode(overrideTransparency));
                }
            } catch (Exception ex) {
                System.out.println(ex);
                return null;
            }
        }

        return null;
    }

    private void processBody(Element body)
            throws DocumentException, FB2toPDFException {
        currentStyle = stylesheet.getParagraphStyle("body");

        Elements children = body.getChildElements();
        int subsectionIndex = 0;
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("section")) {
                processSection(element, 0, subsectionIndex);
                subsectionIndex++;
            } else if (element.getLocalName().equals("image")) {
                addImage(element);
            } else if (element.getLocalName().equals("title")) {
                processTitle(element, -1);
            } else if (element.getLocalName().equals("epigraph")) {
                processEpigraph(element);
            } else {
                System.out.println("Unhandled section tag " + element.getLocalName());
            }
        }

        currentStyle = null;
    }

    private void makeTOCPage(Element body)
            throws DocumentException, FB2toPDFException {
        Elements sections = body.getChildElements("section", NS_FB2);
        if (sections.size() <= 1) {
            return;
        }

        ParagraphStyle tocTitleStyle = stylesheet.getParagraphStyle("tocTitle");
        ParagraphStyle tocItemStyle = stylesheet.getParagraphStyle("tocItem");

        addLine(tocTitleStyle.getText(), tocTitleStyle);

        for (int i = 0; i < sections.size(); ++i) {
            Element section = sections.get(i);
            Nodes nodes = section.query("./fb:title//*[not(@type) or @type != 'note']/text()", xCtx);
            String title = getTextContent(nodes, null, null);
            if (title.length() == 0) {
                title = "#" + (i + 1);
            }

            Chunk chunk = tocItemStyle.createChunk();
            chunk.append(TextPreprocessor.process(title, stylesheet.getTextPreprocessorSettings(), currentStyle));

            String ref = section.getAttributeValue("id");
            if (isNullOrEmpty(ref)) {
                ref = "section" + i;
            }
            addGoToActionToChunk(ref, chunk);
            Paragraph para = tocItemStyle.createParagraph();
            para.add(chunk);

            doc.add(para);
        }

        doc.newPage();
    }

    private void addBookmark(String title, int level) {
        if (!currentOutline.containsKey(level)) {
            return;
        }
        System.out.println("Adding bookmark: " + transliterate(title));
        PdfDestination destination = new PdfDestination(PdfDestination.FITH);
        PdfOutline bookmark = new PdfOutline(currentOutline.get(level), destination, transliterate(title));
        currentOutline.put(level + 1, bookmark);
    }

    private void processSection(Element section, int level, int index)
            throws DocumentException, FB2toPDFException {

        Float newPagePosition = stylesheet.getPageStyle().sectionNewPage.get(level);

        if (newPagePosition != null && writer.getVerticalPosition(false) < doc.getPageSize().getHeight() * newPagePosition) {
            doc.newPage();
            writer.setPageEmpty(false);
        }

        if (bodyIndex == 0) {
            Nodes nodes = section.query("./fb:title//*[not(@type) or @type != 'note']/text()", xCtx);
            String bmk = getTextContent(nodes, " ", null);
            if (StringUtils.isNotBlank(bmk)) {
                addBookmark(bmk, level);
            }
        }

        String id = section.getAttributeValue("id");
        if (isNullOrEmpty(id) && bodyIndex == 0 && level == 0) {
            id = "section" + index;
        }

        if (!isNullOrEmpty(id)) {
            addInvisibleAnchor(id);
        }

        processSectionContent(section, level);

        if (bodyIndex > 0 && StringUtils.isNotBlank(id) &&
                stylesheet.getGeneralSettings().generateNoteBackLinks) {
            addBacklink(id);
        }

    }

    private void addEmptyLine()
            throws DocumentException, FB2toPDFException {
        addLine(" ", currentStyle);
    }

    private void processSectionContent(Element parent, int level)
            throws DocumentException, FB2toPDFException {
        boolean bFirst = true;

        Elements children = parent.getChildElements();
        int subsectionIndex = 0;
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("section")) {
                processSection(element, level + 1, subsectionIndex);
                subsectionIndex++;
            } else if (element.getLocalName().equals("p")) {
                processParagraph(element, bFirst, i == children.size() - 1);
                bFirst = false;
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else if (element.getLocalName().equals("image")) {
                addImage(element);
            } else if (element.getLocalName().equals("annotation")) {
                ParagraphStyle previousStyle = currentStyle;
                currentStyle = stylesheet.getParagraphStyle("annotation");
                processAnnotation(element, "annotationSubtitle");
                currentStyle = previousStyle;
            } else if (element.getLocalName().equals("poem")) {
                processPoem(element);
            } else if (element.getLocalName().equals("subtitle")) {
                ParagraphStyle previousStyle = currentStyle;
                currentStyle = stylesheet.getParagraphStyle("bodySubtitle");
                processParagraph(element, true, true);
                currentStyle = previousStyle;
            } else if (element.getLocalName().equals("cite")) {
                processCite(element);
            } else if (element.getLocalName().equals("table")) {
                ParagraphStyle previousStyle = currentStyle;
                boolean previousInlineMode = stylesheet.getGeneralSettings().enableInlineImages;
                processTable(element);
                stylesheet.getGeneralSettings().enableInlineImages = previousInlineMode;
                currentStyle = previousStyle;
            } else if (element.getLocalName().equals("title")) {
                processTitle(element, level);
            } else if (element.getLocalName().equals("epigraph")) {
                processEpigraph(element);
            } else {
                System.out.println("Unhandled section tag " + element.getLocalName());
            }
        }
    }

    private void processAnnotation(Element annotation, String subtitleStyle)
            throws DocumentException, FB2toPDFException {
        boolean bFirst = true;

        Elements children = annotation.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("p")) {
                processParagraph(element, bFirst, i == children.size() - 1);
                bFirst = false;
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else if (element.getLocalName().equals("poem")) {
                processPoem(element);
            } else if (element.getLocalName().equals("subtitle")) {
                ParagraphStyle previousStyle = currentStyle;
                currentStyle = stylesheet.getParagraphStyle(subtitleStyle);
                processParagraph(element, true, true);
                currentStyle = previousStyle;
            } else if (element.getLocalName().equals("cite")) {
                processCite(element);
            } else if (element.getLocalName().equals("table")) {
                ParagraphStyle previousStyle = currentStyle;
                boolean previousInlineMode = stylesheet.getGeneralSettings().enableInlineImages;
                processTable(element);
                stylesheet.getGeneralSettings().enableInlineImages = previousInlineMode;
                currentStyle = previousStyle;
            } else {
                System.out.println("Unhandled section tag " + element.getLocalName());
            }
        }
    }

    private void processTitle(Element title, int level)
            throws DocumentException, FB2toPDFException {
        ParagraphStyle previousStyle = currentStyle;

        switch (level) {
            case -1:
                currentStyle = stylesheet.getParagraphStyle("bodyTitle");
                break;
            case 0:
                currentStyle = stylesheet.getParagraphStyle("sectionTitle");
                break;
            case 1:
                currentStyle = stylesheet.getParagraphStyle("subSectionTitle");
                break;
            default:
                currentStyle = stylesheet.getParagraphStyle("subSubSectionTitle");
                break;
        }

        Elements children = title.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("p")) {
                /* XXX TODO
                pid=x.getAttributeValue('id')
                if pid:
                res+='\\hypertarget{%s}{}\n' % pid
                 */
                processParagraph(element, i == 0, i == children.size() - 1);
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                System.out.println("Unhandled title tag " + element.getLocalName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processEpigraph(Element epigraph)
            throws DocumentException, FB2toPDFException {
        addInvisibleAnchor(epigraph);

        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle = stylesheet.getParagraphStyle("epigraph");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("epigraphAuthor");

        currentStyle = mainStyle;

        Elements children = epigraph.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("p")) {
                processParagraph(element, i == 0, i == children.size() - 1);
            } else if (element.getLocalName().equals("poem")) {
                processPoem(element);
            } else if (element.getLocalName().equals("cite")) {
                processCite(element);
            } else if (element.getLocalName().equals("text-author")) {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                System.out.println("Unhandled tag '" + element.getLocalName() + "' inside 'epigraph'");
            }
        }

        currentStyle = previousStyle;
    }

    private void processCite(Element cite)
            throws DocumentException, FB2toPDFException {
        addInvisibleAnchor(cite);

        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle = stylesheet.getParagraphStyle("cite");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("citeAuthor");
        ParagraphStyle subtitleStyle = stylesheet.getParagraphStyle("citeSubtitle");

        currentStyle = mainStyle;

        Elements children = cite.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("p")) {
                processParagraph(element, i == 0, i == children.size() - 1);
            } else if (element.getLocalName().equals("subtitle")) {
                currentStyle = subtitleStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getLocalName().equals("poem")) {
                processPoem(element);
            } else if (element.getLocalName().equals("text-author")) {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                System.out.println("Unhandled tag '" + element.getLocalName() + "' inside 'cite'");
            }
        }

        currentStyle = previousStyle;
    }

    private void processPoem(Element poem)
            throws DocumentException, FB2toPDFException {
        addInvisibleAnchor(poem);
        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle = stylesheet.getParagraphStyle("poem");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("poemAuthor");
        ParagraphStyle titleStyle = stylesheet.getParagraphStyle("poemTitle");
        ParagraphStyle dateStyle = stylesheet.getParagraphStyle("poemDate");

        currentStyle = mainStyle;

        Elements children = poem.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("stanza")) {
                processStanza(element);
            } else if (element.getLocalName().equals("title")) {
                currentStyle = titleStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getLocalName().equals("date")) {
                currentStyle = dateStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getLocalName().equals("epigraph")) {
                processEpigraph(element);
            } else if (element.getLocalName().equals("text-author")) {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else {
                System.out.println("Unhandled poem tag " + element.getLocalName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processStanza(Element stanza)
            throws DocumentException, FB2toPDFException {
        ParagraphStyle previousStyle = currentStyle;
        currentStyle = stylesheet.getParagraphStyle("poem");

        Elements children = stanza.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            if (element.getLocalName().equals("v")) {
                processParagraph(element, i == 0, i == children.size() - 1);
            } else {
                System.out.println("Unhandled stanza tag " + element.getLocalName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processParagraph(Element paragraph, boolean bFirst, boolean bLast)
            throws DocumentException, FB2toPDFException {
        currentParagraph = getStyleForElement(paragraph).createParagraph(bFirst, bLast);

        addInvisibleAnchor(paragraph);
        processParagraphContent(paragraph, bFirst);
        flushCurrentChunk();

        doc.add(currentParagraph);
        currentParagraph = null;
        currentReference = null;
    }

    private void addDropCap(String text, com.itextpdf.text.Document doc) throws DocumentException, FB2toPDFException {

        ParagraphStyle dropcapStyle = stylesheet.getParagraphStyle(currentStyle.getDropcapStyle());

        float dropCapSize = dropcapStyle.getFontSize().getPoints();
        float spacingBefore = dropcapStyle.getSpacingBefore();
        float identationRight = dropcapStyle.getSpacingAfter();
        BaseFont basefont = dropcapStyle.getBaseFont();

        //float descent = basefont.getDescentPoint(text, dropCapSize);
        //float ascent = basefont.getAscentPoint(text, dropCapSize);
        int[] bbox = basefont.getCharBBox(text.charAt(0));
        float offsetLeft = bbox == null ? 0 : bbox[0] * 0.001f * dropCapSize;
        bbox = basefont.getCharBBox(text.charAt(text.length() - 1));
        float offsetRight = bbox == null ? 0 : (basefont.getWidth(text.charAt(text.length() - 1)) - bbox[2]) * 0.001f * dropCapSize;
        float templateHight = basefont.getFontDescriptor(BaseFont.CAPHEIGHT, dropCapSize);
        float templateWidth = basefont.getWidthPointKerned(text, dropCapSize) - offsetLeft - offsetRight;

        PdfTemplate tp = writer.getDirectContent().createTemplate(templateWidth, templateHight);
        //tp.saveState();
        //tp.setColorStroke(BaseColor.YELLOW);
        //tp.setColorFill(BaseColor.YELLOW);
        //tp.rectangle(-100, -100, 200, 200);
        //tp.fillStroke();
        //tp.setColorFill(BaseColor.RED);
        //tp.rectangle(0, 0, templateWidth, templateHight);
        //tp.fillStroke();
        //tp.restoreState();
        tp.beginText();
        tp.setFontAndSize(basefont, dropCapSize);
        tp.setColorFill(dropcapStyle.getColor());
        tp.setTextMatrix(-offsetLeft, 0);
        tp.showText(text);
        tp.endText();
        Image dropcap = Image.getInstance(tp);
        dropcap.setAlignment(Image.TEXTWRAP);
        dropcap.setIndentationRight(identationRight);
        dropcap.setSpacingBefore(spacingBefore);

        if (writer.getVerticalPosition(false) < spacingBefore + templateHight + doc.bottomMargin()) {
            doc.newPage();
        }
        doc.add(dropcap);
        //tp.setBoundingBox(new Rectangle(0,descent,templateWidth,ascent));
        tp.setBoundingBox(new Rectangle(-100, -100, 100, 100));
    }

    private void flushCurrentChunk()
            throws DocumentException, FB2toPDFException {
        if (currentChunk != null) {
            String currentAnchorName = anchorStack.isEmpty() ? null : anchorStack.pop();
            if (!isNullOrEmpty(currentReference)) {
                Anchor anchor = currentStyle.createAnchor();
                if (currentReference.charAt(0) == '#') {
                    //Unlike Anchor, Action won't fail even when local destination does not exist
                    String refname = currentReference.substring(1); //getting rid of "#" at the begin of the reference
                    currentChunk.setGenericTag("FOOTNOTE:" + refname);
                    addGoToActionToChunk(refname, currentChunk);
                    currentAnchorName = refname + "_backlink";
                } else {
                    anchor.setReference(currentReference);
                    System.out.println("Adding A Link " + currentReference);
                }
                if (currentAnchorName != null) {
                    anchor.setName(currentAnchorName);
                    System.out.println("Adding A Destination " + currentAnchorName);
                    saveLinkPageNumber(currentAnchorName);
                }
                anchor.add(currentChunk);
                currentParagraph.add(anchor);
            } else {
                if (currentAnchorName != null) {
                    Anchor anchor = currentStyle.createAnchor();
                    anchor.add(currentChunk);
                    anchor.setName(currentAnchorName);
                    currentParagraph.add(anchor);
                    System.out.println("Adding A Destination " + currentAnchorName);
                    saveLinkPageNumber(currentAnchorName);
                } else {
                    currentParagraph.add(currentChunk);
                }
            }
            currentChunk = null;
        }
    }

    private void processParagraphContent(Element parent)
            throws DocumentException, FB2toPDFException {
        processParagraphContent(parent, false);
    }

    private void processParagraphContent(Element parent, boolean bFirst)
            throws DocumentException, FB2toPDFException {
        ParagraphStyle previousStyle = currentStyle;
        currentStyle = getStyleForElement(parent);
        boolean bFirstTextNode = true;
        Node parentNode = (Node)parent;
        for (int i = 0; i < parentNode.getChildCount(); ++i) {
            Node node = parentNode.getChild(i);

            if (node instanceof Element) {
                Element child = (Element) node;
                if (child.getLocalName().equals("strong")) {
                    flushCurrentChunk();
                    currentStyle.toggleBold();
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleBold();
                } else if (child.getLocalName().equals("emphasis")) {
                    flushCurrentChunk();
                    currentStyle.toggleItalic();
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleItalic();
                } else if (child.getLocalName().equals("code")) {
                    flushCurrentChunk();
                    ParagraphStyle prevStyle = currentStyle;
                    currentStyle = stylesheet.getParagraphStyle("code");
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle = prevStyle;
                } else if (child.getLocalName().equals("a")) {
                    flushCurrentChunk();
                    currentReference = child.getAttributeValue("href", NS_XLINK);
                    if (currentReference.length() == 0) {
                        currentReference = child.getAttributeValue("href");
                    }
                    processParagraphContent(child);
                    flushCurrentChunk();
                    addPageNumTemplate();
                    addFootnote(child);
                    currentReference = null;
                } else if (child.getLocalName().equals("style")) {
                    String styleName = child.getAttributeValue("name");
                    System.out.println("Style tag " + styleName + " ignored.");
                    processParagraphContent(child);
                } else if (child.getLocalName().equals("image")) {
                    flushCurrentChunk();
                    addImage(child, stylesheet.getGeneralSettings().enableInlineImages);
                } else if (child.getLocalName().equals("strikethrough")) {
                    flushCurrentChunk();
                    currentStyle.toggleStrikethrough();
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleStrikethrough();
                } else if (child.getLocalName().equals("sup")) {
                    flushCurrentChunk();
                    currentStyle.toggleHalfSize();
                    superscript = true;
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleHalfSize();
                    superscript = false;
                } else if (child.getLocalName().equals("sub")) {
                    flushCurrentChunk();
                    currentStyle.toggleHalfSize();
                    subscript = true;
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleHalfSize();
                    subscript = false;
                } else {
                    /*
                    elif s.tagName == "code":
                     */
                    System.out.println("Unhandled paragraph tag " + child.getLocalName());
                    processParagraphContent(child);
                }
            } else if (node instanceof Text) {
                String text = node.getValue();
                if (bodyIndex == 0 && bFirst && bFirstTextNode
                        && !currentStyle.getDropcapStyle().equals("")) {
                    bFirstTextNode = false;
                    int idx = getDropCapCount(text);
                    String dropcap = text.substring(0, idx);
                    text = text.substring(idx);
                    if (dropcap != null && dropcap.trim().length() > 0) {
                        dropcap = dropcap.replaceAll("^\u2014", "\u2013");
                        addDropCap(dropcap, doc);
                    }
                }

                if (currentChunk == null) {
                    currentChunk = currentStyle.createChunk();
                    if (superscript) {
                        currentChunk.setTextRise(currentStyle.getFontSize().getPoints() / 3);
                    }
                    if (subscript) {
                        currentChunk.setTextRise(-currentStyle.getFontSize().getPoints() / 6);
                    }
                    if (!currentStyle.getDisableHyphenation()) {
                        currentChunk.setHyphenation(hyphenation);
                    }
                }

                currentChunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings(), currentStyle));
            }
        }
        currentStyle = previousStyle;
    }

    private String transliterate(String text) {
        return transliterate(text, false);
    }

    private String transliterate(String text, boolean force) {
        if (!stylesheet.getGeneralSettings().transliterateMetaInfo && !force) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            for (int j = 0; j < TRANSTABLE.length; ++j) {
                if (c != TRANSTABLE[j][0].charAt(0)) {
                    continue;
                }
                sb.append(TRANSTABLE[j][1]);
                c = 0;
                break;
            }
            if (c != 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void translate(String fromName, String toName)
            throws DocumentException, IOException, FB2toPDFException {
        translate(fromName, toName, null);
    }

    public static void translate(String fromName, String toName, InputStream stylesheet)
            throws DocumentException, IOException, FB2toPDFException {
        new FB2toPDF(fromName, toName).run(stylesheet);
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: java " + FB2toPDF.class.getName() + " <input.fb2> <output.pdf>");
                return;
            }
            translate(args[0], args[1]);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String getLang(Element description) throws FB2toPDFException {
        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            Element lang = getOptionalChildByTagName(titleInfo, "lang");
            if (lang != null) {
                String langString = lang.getValue();
                System.out.println("Language of the FB2: " + langString);
                return langString;
            }
        }
        System.out.println("Language of the FB2 not found");
        return null;
    }

    private void setupHyphenation(Element description) throws FB2toPDFException {
        HyphenationSettings hyphSettings = stylesheet.getHyphenationSettings();
        if (hyphSettings.hyphenate) {
            System.out.println("Hyphenation is on");
            String bookLang = getLang(description);
            if (isNullOrEmpty(bookLang) || hyphSettings.overrideLanguage) {
                bookLang = hyphSettings.defaultLanguage;
            }
            hyphenation = new HyphenationAuto(bookLang, "none", 2, 2);
            System.out.println("Hyphenation language is: " + bookLang);
        } else {
            System.out.println("Hyphenation is off");
        }
    }
}
