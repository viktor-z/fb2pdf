package org.trivee.fb2pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.DottedLineSeparator;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.*;
import java.net.MalformedURLException;
import java.util.List;
import java.util.*;
import nu.xom.Element;
import nu.xom.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

public class FB2toPDF {

    private static final String NS_XLINK = "http://www.w3.org/1999/xlink";
    private static final String NS_FB2 = "http://www.gribuser.ru/xml/fictionbook/2.0";
    private static final XPathContext xCtx = new XPathContext("fb", NS_FB2);
    private String fromName;
    private String toName;
    private nu.xom.Document fb2;
    private com.itextpdf.text.Document doc;
    private PdfWriter writer;
    int bodyIndex;
    private Nodes bodies;
    private Paragraph currentParagraph;
    private String secondPassStylesheet;
    private boolean enableDoubleRenderingOutline;
    private int currentElementHash;
    private PdfOutline fontChangeOutline;
    private String passNamePrefix = "";
    private Map<Integer, Integer> pageElementMap;
    private Map<Integer, Integer> pageElementMap1 = new LinkedHashMap<Integer, Integer>();
    private Map<Integer, Integer> pageElementMap2 = new LinkedHashMap<Integer, Integer>();
    private Map<Integer, Integer> elementPageMap;
    private Map<Integer, Integer> elementPageMap1 = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> elementPageMap2 = new HashMap<Integer, Integer>();
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
    private List<PdfTemplate> footnoteTemplates = new ArrayList<PdfTemplate>();
    private HeaderHelper headerHelperOdd = null;
    private HeaderHelper headerHelperEven = null;
    private String chapterTitle = "";
    private BackgroundImageHelper backgroundImageHelper;
        
    private FB2toPDF(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }

    protected void addSectionsToTOC(Elements sections, int level) throws DocumentException, FB2toPDFException {
        currentStyle = stylesheet.getParagraphStyle("tocItem");
        
        float extraIndent = level * currentStyle.getFontSize();

        for (int i = 0; i < sections.size(); ++i) {
            Element section = sections.get(i);
            Nodes nodes = section.query("./fb:title//*[not(@type) or @type != 'note']/text()", xCtx);
            String title = getTextContent(nodes, null, null);
            if (title.length() == 0) {
                title = "#" + (i + 1);
            }

            Chunk chunk = currentStyle.createChunk();
            chunk.append(TextPreprocessor.process(title, stylesheet.getTextPreprocessorSettings(), currentStyle));

            String ref = section.getAttributeValue("id");
            if (isBlank(ref)) {
                ref = String.format("section%d", section.hashCode());
            }
            addGoToActionToChunk(ref, chunk);
            currentParagraph = currentStyle.createParagraph();
            
            currentParagraph.setIndentationLeft(currentParagraph.getIndentationLeft() + extraIndent);

            currentParagraph.add(chunk);
            currentReference = "#" + ref;
            
            
            if (settings().enableTOCPageNum) {
                currentParagraph.add(new Chunk(new DottedLineSeparator()));
                addPageNumTemplate(settings().tocPageNumFormat);
            }
            addElement(currentParagraph);
            
            currentReference = null;
            
            Elements internalSections = section.getChildElements("section", NS_FB2);
            if (internalSections.size() > 0 && settings().generateTOCLevels > level) {
                addSectionsToTOC(internalSections, level+1);
            }

        }
    }

    private void addElement(com.itextpdf.text.Element element) throws DocumentException {
        if (isColumnTextExperiment() && element instanceof Paragraph) {
            ColumnText columnText = new ColumnText(writer.getDirectContent());
            columnText.setSimpleColumn(doc.left(), doc.bottom(), doc.right(), doc.top());
            
            float beginYLine = writer.getVerticalPosition(true);
            columnText.setYLine(beginYLine);
            columnText.addElement(element);
            int status = columnText.go();
            while (ColumnText.hasMoreText(status)) {
                newPage();
                beginYLine = writer.getVerticalPosition(true);
                columnText.setYLine(beginYLine);
                status = columnText.go();
            }
            
            float endYLine = columnText.getYLine();
            
            PdfTemplate tmp = writer.getDirectContent().createTemplate(doc.right(), beginYLine - endYLine );
            
            doc.add(Image.getInstance(tmp));
            
            return;
        }
        doc.add(element);
    }

    private float getVerticalPosition() {
        return writer.getVerticalPosition(false);
    }

    private boolean isColumnTextExperiment() {
        return "ColumnTextRenderer".equalsIgnoreCase(System.getProperty("fb2pdf.experiment"));
    }

    private void newPage() {
        doc.newPage();
    }

    private GeneralSettings settings() {
        return stylesheet.getGeneralSettings();
    }

    protected void setupBackgroundImage() throws FB2toPDFException {
        try {
            backgroundImageHelper = new BackgroundImageHelper();
            backgroundImageHelper.init(stylesheet, doc.getPageSize());
            writer.setPageEvent(backgroundImageHelper);
        } catch (Exception ex) {
            throw new FB2toPDFException(ex.getMessage());
        }
    }

    private void addAnchor(String anchorName) throws FB2toPDFException {
        Anchor anchor = currentStyle.createAnchor();
        anchor.add(currentChunk);
        anchorName = passNamePrefix + anchorName;
        anchor.setName(anchorName);
        currentParagraph.add(anchor);
        Log.info("Adding A Destination [{0}]", anchorName);
        saveLinkPageNumber(anchorName);
    }

    private void addFootnote(Element child) throws DocumentException, FB2toPDFException {
        if (stylesheet.getPageStyle().footnotes && "note".equals(child.getAttributeValue("type"))) {
            addFootnote(child.getValue(), currentReference);
        }
    }

    private void addInternalLink() throws FB2toPDFException {
        String refname = currentReference.substring(1); //getting rid of "#" at the begin of the reference
        if (StringUtils.isBlank(refname)) return;
        currentChunk.setGenericTag("FOOTNOTE:" + refname);
        if (settings().generateInternalLinks) {
            Anchor anchor = currentStyle.createAnchor();
            //Unlike Anchor, Action won't fail even when local destination does not exist
            refname = passNamePrefix + refname;
            addGoToActionToChunk(refname, currentChunk);
            String aName = refname + "_backlink";
            anchor.setName(aName);
            Log.info("Adding A Destination [{0}]", aName);
            saveLinkPageNumber(aName);
            anchor.add(currentChunk);
            currentParagraph.add(anchor);
        } else {
            currentParagraph.add(currentChunk);
        }
    }

    private void addExternalLink() throws FB2toPDFException {
        Anchor anchor = currentStyle.createAnchor();
        anchor.setReference(currentReference);
        Log.info("Adding A Link [{0}]", currentReference);
        anchor.add(currentChunk);
        currentParagraph.add(anchor);
    }

    private void applyTransformations() throws RuntimeException {
        if (!stylesheet.getTransformationSettings().enabled) {
            return;
        }

        try {
            XQueryUtilities.transform(fb2, stylesheet.getTransformationSettings());
        } catch (Exception ex) {
            throw new RuntimeException("Error processing transformation. " + ex.getMessage());
        }
    }

    private void applyXPathStyles() throws RuntimeException, IOException {
        String prolog = XQueryUtilities.defaultProlog;
        String morpher = prolog + "(., attribute {'fb2pdf-style'} {'%s'})";
        for (ParagraphStyle style : stylesheet.getParagraphStyles()) {
            String xpath = style.getSelector();
            String name = style.getName();
            if (isBlank(xpath)) continue;
                
            try {
                XQueryUtilities.transform(fb2, prolog + xpath + "/(* | text())[last()]", String.format(morpher, name));
            } catch (Exception ex) {
                throw new RuntimeException("Error applying styles. " + ex.getMessage());
            }
        }
        XQueryUtilities.outputDebugInfo(fb2, stylesheet.getTransformationSettings(), "styling-result.xml");
    }

    private void addFontChangeOutline(Map<Integer, Integer> pageElementMap, Map<Integer, Integer> elementPageMap, int maxPageNumLength) {
        for (int srcpage : pageElementMap.keySet()) {
            int element = pageElementMap.get(srcpage);
            if (!elementPageMap.containsKey(element)) continue;
            int destpage = elementPageMap.get(element);
            String srcpageLabel = String.format("%0"+maxPageNumLength+"d", srcpage);
            addFontChangeOutlineItem(fontChangeOutline, maxPageNumLength, srcpageLabel, destpage);
        }
    }
    
    private void createHeaderSlot(PdfPTable table, HeaderSlotSettings slotSettings) throws FB2toPDFException {
        final ParagraphStyle slotStyle = stylesheet.getParagraphStyle(slotSettings.style);

        table.getDefaultCell().setBorder(slotSettings.border);
        BaseColor color = slotSettings.getBorderColor();
        if (color == null) color = slotStyle.getColor();
        table.getDefaultCell().setBorderColor(color);
        
        table.getDefaultCell().setPaddingTop(slotStyle.getSpacingBefore());
        table.getDefaultCell().setPaddingBottom(slotStyle.getSpacingAfter());
        table.getDefaultCell().setPaddingLeft(slotStyle.getLeftIndent());
        table.getDefaultCell().setPaddingRight(slotStyle.getRightIndent());
        table.getDefaultCell().setHorizontalAlignment(slotStyle.getAlignment());
        
        if (slotSettings.enabled) {
            Chunk chunk = slotStyle.createChunk();
           
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("declare variable $bookTitle := //title-info/book-title; "); 
            queryBuilder.append("declare variable $author := //title-info/author; "); 
            queryBuilder.append("declare variable $authorFullName := string-join($author/string-join((first-name, middle-name, last-name), ' '), ', '); "); 
            queryBuilder.append("declare variable $authorLastName := string-join($author/last-name, ', '); "); 
            queryBuilder.append("declare variable $authorFirstLastName := string-join($author/string-join((first-name, last-name), ' '), ', '); "); 
            queryBuilder.append("declare variable $authorFirstInitialLastName := string-join($author/string-join((substring(first-name, 1, 1), last-name), '. '), ', '); "); 
            queryBuilder.append("declare variable $authorAllInitialsLastName := string-join($author/string-join((substring(first-name, 1, 1), substring(middle-name, 1, 1), last-name), '. '), ', '); "); 
            queryBuilder.append("declare variable $pageNum as xs:string external; ");
            queryBuilder.append("declare variable $chapterTitle as xs:string external; ");
            queryBuilder.append(slotSettings.query); 
            String query = queryBuilder.toString();
            String txt = XQueryUtilities.getString(fb2.getRootElement(), stylesheet.getTransformationSettings(), query, " ", getDynamicVariables());
            chunk.append(txt);
            table.addCell(new Phrase(chunk));
        } else {
            table.addCell("");
        }
    }

    private Map<String, Object> getDynamicVariables() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (stylesheet.getPageStyle().getHeader().dynamic) {
            result.put("pageNum", new Integer(writer.getPageNumber()).toString());
            result.put("chapterTitle", chapterTitle);
        } else {
            result.put("pageNum", "$pageNum");
            result.put("chapterTitle", "$chapterTitle");
        }
        return result;
    }
    
    private PdfPTable createHeaderTable(boolean odd) throws DocumentException, FB2toPDFException {
        
        HeaderSettings headerSettings = stylesheet.getPageStyle().getHeader();
        
        PdfPTable table = new PdfPTable(3);
        table.setWidths(new float[]{0.3333f, 0.3333f, 0.3333f});
        table.setTotalWidth(doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin());
        table.getDefaultCell().setNoWrap(true);

        if (odd) {
            createHeaderSlot(table, headerSettings.leftOdd);
            createHeaderSlot(table, headerSettings.centerOdd);
            createHeaderSlot(table, headerSettings.rightOdd);
        } else {
            createHeaderSlot(table, headerSettings.leftEven);
            createHeaderSlot(table, headerSettings.centerEven);
            createHeaderSlot(table, headerSettings.rightEven);
        } 

        return table;
    }

    private Map<String, Integer> getCellHAlignmentMap() {
        Map<String, Integer> hAlignMap = new HashMap<String, Integer>();
        hAlignMap.put("center", PdfPCell.ALIGN_CENTER);
        hAlignMap.put("left", PdfPCell.ALIGN_LEFT);
        hAlignMap.put("right", PdfPCell.ALIGN_RIGHT);
        return hAlignMap;
    }

    private Map<String, Integer> getCellVAlignmentMap() {
        Map<String, Integer> vAlignMap = new HashMap<String, Integer>();
        vAlignMap.put("middle", PdfPCell.ALIGN_MIDDLE);
        vAlignMap.put("top", PdfPCell.ALIGN_TOP);
        vAlignMap.put("bottom", PdfPCell.ALIGN_BOTTOM);
        return vAlignMap;
    }

    private Element getNoteSection(String refname) {
        if (StringUtils.isBlank(refname)) {
            return null;
        }
        String query = "/*/fb:body[@name]/fb:section";
        Nodes sections = fb2.getRootElement().query(query, xCtx);
        Element section = getSectionById(sections, refname);
        return section;
    }

    private void fillPageNumTemplate(String referenceName) throws FB2toPDFException {
        for (LinkPageNumTemplate lpnt : linkPageNumTemplates.get(referenceName)) {
            PdfTemplate tp = lpnt.template;
            ParagraphStyle tpStyle = lpnt.style;
            BaseFont tpBaseFont = tpStyle.getBaseFont();
            float tpSize = tpStyle.getFontSize();
            String pageNum = String.format(lpnt.format, linkPageNumbers.get(referenceName));
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
        addGoToActionToChunk(passNamePrefix + id + "_backlink", chunk);
        addEmptyLine();
        addLine(chunk, currentStyle);
    }

    protected void addCenteredImage(Image image) throws DocumentException, FB2toPDFException {
        Rectangle pageSize = doc.getPageSize();
        float dpi = settings().imageDpi;
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
        addElement(image);
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

                Element cellElement = cols.get(j);
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

    private Rectangle getPageSize() throws NumberFormatException {
        final PageStyle pageStyle = stylesheet.getPageStyle();
        Rectangle pageSize = new Rectangle(pageStyle.getPageWidth(), pageStyle.getPageHeight(), pageStyle.getPageRotation());
        if(!isBlank(pageStyle.backgroundColor)) {
            pageSize.setBackgroundColor(new BaseColor(Color.decode(pageStyle.backgroundColor).getRGB()));
        }
        return pageSize;
    }

    private void renderBook(Element description) throws FB2toPDFException, IOException, DocumentException {
        
        if (description != null) {
            setupHyphenation(description);
            processDescription(description);
        }
        
        Element body = (Element)bodies.get(0);
        if (settings().generateTOCLevels > 0) {
            makeTOCPage(body);
        }

        if (stylesheet.getPageStyle().getHeader().enabled) {
            if (isBlank(passNamePrefix)) {
                setupHeader();
            } else {
                refreshHeader();
            }
        }
        
        for (int i = 0; i < bodies.size(); ++i) {
            body = (Element) bodies.get(i);
            bodyIndex = i;
            processBody(body);
            newPage();
        }
    }

    private void rescaleImage(Image image, float zoomFactor, float wSpace, float hSpace) {
        Rectangle pageSize = doc.getPageSize();
        float dpi = settings().imageDpi;
        Utilities.rescaleImage(image, zoomFactor, wSpace, hSpace, pageSize, dpi);
    }

    private void saveLinkPageNumber(String currentAnchorName) {
        if (settings().enableLinkPageNum || settings().enableTOCPageNum) {
            linkPageNumbers.put("#" + currentAnchorName, writer.getPageNumber());
        }

    }

    private void addPageNumTemplate(String format) throws BadElementException, FB2toPDFException {
        String text = String.format(settings().linkPageNumFormat, settings().linkPageNumMax);
        float tmpSize = currentStyle.getFontSize();
        BaseFont tmpBasefont = currentStyle.getBaseFont();
        float templateHight = tmpBasefont.getFontDescriptor(BaseFont.CAPHEIGHT, tmpSize);
        float templateWidth = tmpBasefont.getWidthPointKerned(text, tmpSize);
        PdfTemplate template = writer.getDirectContent().createTemplate(templateWidth, templateHight);
        Image templateImage = Image.getInstance(template);
        Chunk chunk = new Chunk(templateImage, 0, 0, false);
        chunk.setFont(currentStyle.getFont());
        currentParagraph.add(chunk);
        linkPageNumTemplates.put(currentReference, new LinkPageNumTemplate(template, currentStyle, format));
    }

    private void rescaleImage(Image image) throws FB2toPDFException {
        rescaleImage(image, 1.0f);
    }

    private void rescaleImage(Image image, float zoomFactor) throws FB2toPDFException {
        float hSpace = doc.topMargin() + doc.bottomMargin() + image.getSpacingAfter() + image.getSpacingBefore();
        float wSpace = doc.leftMargin() + doc.rightMargin() + stylesheet.getPageStyle().getImageExtraMargins();
        if (currentStyle != null) {
            hSpace += currentStyle.getAbsoluteLeading();// + currentStyle.getFontSize().getPoints();
        }
        rescaleImage(image, zoomFactor, wSpace, hSpace);
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
        addElement(para);
    }

    protected void addStretchedImage(Image image) throws DocumentException {
        Rectangle pageSize = doc.getPageSize();
        image.scaleToFit(pageSize.getWidth()/* - doc.leftMargin() - doc.rightMargin()*/, pageSize.getHeight()/* - doc.topMargin() - doc.bottomMargin()*/);
        image.setAlignment(Image.MIDDLE);
        addElement(image);
    }

    private void addGoToActionToChunk(String refname, Chunk chunk) {
        PdfAction action = PdfAction.gotoLocalPage(refname, false);
        Log.info("Adding Action LocalGoTo [{0}]", refname);
        chunk.setAction(action);
    }

    private void addFootnote(String marker, String refname) throws DocumentException, FB2toPDFException {
        if (isBlank(refname)) {
            Log.warning("Skipping footnote with empty reference");
            return;
        }
        refname = refname.substring(1);
        Log.info("Adding footnote [{0}]", refname);

        float w = FootnoteRenderer.pageSize.getWidth();
        float h = FootnoteRenderer.pageSize.getHeight();
        float cutW = FootnoteRenderer.cutMarkerWidth;
        int footnoteNumber = FootnoteRenderer.getPageNumber();

        Element section = getNoteSection(refname);
        FootnoteRenderer.addFootnote(marker, refname, section, hyphenation);
        
        footnoteNumber = FootnoteRenderer.getPageNumber() - footnoteNumber - 1;
        if (footnoteNumber < 1) {
            return;
        }
        List<Image> noteLineImages = prepareFootnoteLineImages(footnoteNumber, refname, w, h, cutW);
        for (Image image : noteLineImages) {
            addElement(image);
        }
    }

    private Element getSectionById(Element root, String id) {
        if (id.equals(root.getAttributeValue("id"))) {
            return root;
        }
        Elements sections = root.getChildElements("section", NS_FB2);
        for (int i=0; i<sections.size(); i++) {
            Element section = sections.get(i);
            if (id.equals(section.getAttributeValue("id"))) {
                return section;
            }
        }
        return getSectionById(sections, id);
    }

    private Element getSectionById(Elements sections, String id) {
        for (int i=0; i<sections.size(); i++) {
            Element section = sections.get(i);
            Element result = getSectionById(section, id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Element getSectionById(Nodes sections, String id) {
        for (int i=0; i<sections.size(); i++) {
            Element section = (Element)sections.get(i);
            Element result = getSectionById(section, id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private List<Image> prepareFootnoteLineImages(int numPages, String refname, float pageWidth, float pageHeight, float cutMarkerWidth) {

        List<Image> result = new ArrayList<Image>();
        try {
            int maxLines = stylesheet.getPageStyle().footnoteMaxLines;
            int numLines = Math.min(maxLines, numPages);
            Log.info("Footnote has {0} lines, maximum in settings is {1}, will render {2}", numPages, maxLines, numLines);

            PdfTemplate cutImg = PdfTemplate.createTemplate(writer, 100, pageHeight);
            for (int i = 1; i <= numPages; i++) {
                Image image;
                
                PdfTemplate tmp = PdfTemplate.createTemplate(writer, pageWidth, pageHeight);
                if (numLines < numPages && i == numLines) {
                    PdfTemplate tmp2 = PdfTemplate.createTemplate(writer, pageWidth, pageHeight);
                    tmp.setWidth(tmp.getWidth() - cutMarkerWidth);
                    tmp2.addTemplate(tmp, 0, 0);
                    tmp2.addTemplate(cutImg, tmp.getWidth(), 0);
                    image = FootnoteLineImage.getInstance(tmp2, refname);
                } else {
                    image = FootnoteLineImage.getInstance(tmp, refname);
                }
                
                if (i <= numLines) {
                    image.setSpacingBefore(0);
                    image.setSpacingAfter(0);
                    image.setAlignment(Image.MIDDLE);
                    image.setBorderColor(stylesheet.getParagraphStyle("footnote").getColor());
                    result.add(image);
                }
                footnoteTemplates.add(tmp);
            }
            footnoteTemplates.add(cutImg);
        } catch (Exception ex) {
            Log.warning("Failed to produce footnote lines: {0}", ex.getMessage());
        }

        return result;
    }

    private ParagraphStyle getStyleForElement(Element element) {

        ParagraphStyle result = currentStyle;

        String elementStyleAttr = element.getAttributeValue("fb2pdf-style");

        if (isBlank(elementStyleAttr)) {
            return result;
        }

        try {
            result = stylesheet.getParagraphStyle(elementStyleAttr);
            result.containerStyle = currentStyle;
        } catch (FB2toPDFException ex) {
            Log.warning("Element style [{0}] not found", elementStyleAttr);
        }

        Log.info("Element style [{0}] found", elementStyleAttr);
        return result;
    }

    private void processTable(Element table) throws DocumentException, FB2toPDFException {
        settings().enableInlineImages = true;
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
                Element cellElement = cols.get(j);
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
        addElement(pdftable);
    }

    private int setTableCellAttributes(Element cellElement, PdfPCell cell) throws NumberFormatException {

        Map<String, Integer> hAlignMap = getCellHAlignmentMap();
        Map<String, Integer> vAlignMap = getCellVAlignmentMap();

        
        int colspan = getCellElementSpan(cellElement, "colspan");
        int rowspan = getCellElementSpan(cellElement, "rowspan");

        String alignAttr = cellElement.getAttributeValue("align");
        String valignAttr = cellElement.getAttributeValue("valign");
        int hAlign = isBlank(alignAttr) ? PdfPCell.ALIGN_CENTER : hAlignMap.get(alignAttr);
        int vAlign = isBlank(valignAttr) ? PdfPCell.ALIGN_MIDDLE : vAlignMap.get(valignAttr);

        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(hAlign);
        cell.setVerticalAlignment(vAlign);
        
        return colspan;
    }
    
    private int getCellElementSpan(Element cellElement, String attrName) {
        String spanAttr = cellElement.getAttributeValue(attrName);
        return isBlank(spanAttr) ? 1 : Integer.parseInt(spanAttr);
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
                && settings().ignoreEmptyLineBeforeImage) {
            Log.info("Skipping empty line before image");
            ignore = true;
        }
        if (prevSib != null && prevSib.getLocalName().equalsIgnoreCase("image")
                && settings().ignoreEmptyLineAfterImage) {
            Log.info("Skipping empty line after image");
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
            Log.warning("Image [{0}] not found", href);
        }
    }

    private String getSequenceSubtitle(Element seq) {
        String seqname = seq.getAttributeValue("name");
        String seqnumber = seq.getAttributeValue("number");
        String subtitle = "";
        if (!isBlank(seqname)) {
            subtitle += seqname;
        }
        if (!isBlank(seqnumber)) {
            subtitle += " #" + seqnumber;
        }
        if (!isBlank(subtitle)) {
            subtitle = "(" + subtitle + ")";
        }
        return subtitle;
    }

    private boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    private void loadData(InputStream stylesheetInputStream)
            throws DocumentException, IOException, FB2toPDFException {
        if (stylesheetInputStream == null) {
            stylesheet = Stylesheet.readStylesheet(Utilities.getValidatedFileName("./data/stylesheet.json"));
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

    private void addFontChangeOutline() {
        Log.info("Building style change outline...");
        int lastPageIdx = pageElementMap2.keySet().size() - 1;
        Integer lastPage = (Integer)pageElementMap2.keySet().toArray()[lastPageIdx];
        int maxSize = lastPage.toString().length();
        addFontChangeOutline(pageElementMap1, elementPageMap2, maxSize);
        addFontChangeOutline(pageElementMap2, elementPageMap1, maxSize);
    }

    private void addFontChangeOutlineItem(PdfOutline root, int maxPageNumLength, String srcpageLabel, int destpage) {
        
        if (maxPageNumLength == 1) {
            PdfDestination destination = new PdfDestination(PdfDestination.FITH);
            PdfAction action = PdfAction.gotoLocalPage(destpage, destination, writer);
            PdfOutline bookmark = new PdfOutline(root, action, String.format("%s", srcpageLabel), false);
            return;
        }
        
        int length = srcpageLabel.length();
        String part1 = srcpageLabel.substring(0, length - maxPageNumLength+1);
        String part2 = srcpageLabel.substring(length - maxPageNumLength +1);
        @SuppressWarnings("ReplaceAllDot")
        String currentTitle = part1 + part2.replaceAll(".", "?");
        PdfOutline curOutline = null;
        for (PdfOutline outline : root.getKids()) {
            if (currentTitle.equals(outline.getTitle())) {
                curOutline = outline;
                break;
            }
        }
        if (curOutline == null) {
            PdfDestination destination = new PdfDestination(PdfDestination.FITH);
            PdfAction action = PdfAction.gotoLocalPage(destpage, destination, writer);
            curOutline = new PdfOutline(root, action, currentTitle, false);
        }
        
        addFontChangeOutlineItem(curOutline, maxPageNumLength-1, srcpageLabel, destpage);
    }

    private class PageElementMapHelper extends PdfPageEventHelper {
        
        @Override
        public void onParagraph(PdfWriter writer,com.itextpdf.text.Document document,float paragraphPosition) {
            if (pageElementMap == null || elementPageMap == null) {
                return;
            }

            int page = document.getPageNumber();
            if (!pageElementMap.containsKey(page)) {
                pageElementMap.put(page, currentElementHash);
            }
            elementPageMap.put(currentElementHash, page);
        }

    }
    
    private void createPDFDoc()
            throws DocumentException, FileNotFoundException, FB2toPDFException {

        final PageStyle pageStyle = stylesheet.getPageStyle();
        Rectangle pageSize = getPageSize();

        Log.info("Page size is [{0}]", pageSize);
        
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

        if (!isBlank(pageStyle.backgroundImage)){
            setupBackgroundImage();
        }
        
        if (!isBlank(settings().secondPassStylesheet) && settings().enableDoubleRenderingOutline){
            writer.setPageEvent(new PageElementMapHelper());
        }
        
        if (settings().fullCompression) {
            writer.setFullCompression();
        }

        writer.setSpaceCharRatio(settings().trackingSpaceCharRatio);
        writer.setStrictImageSequence(settings().strictImageSequence);
        PdfDocument.preventWidows = pageStyle.preventWidows;
        PdfDocument.maxFootnoteLines = pageStyle.footnotesMaxLines;
        PdfDocument.hangingPunctuation = settings().hangingPunctuation;
        doc.setMarginMirroring(pageStyle.getMarginMirroring());
    }

    private void closePDF() throws FB2toPDFException, IOException {

        for (String destination : linkPageNumbers.keySet()) {
            if (linkPageNumTemplates.containsKey(destination)) {
                fillPageNumTemplate(destination);
            }
        }
        
        fillFootnoteTemplates();
    
        if (!isBlank(secondPassStylesheet) && enableDoubleRenderingOutline){
            addFontChangeOutline();
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

        //bodies = root.getChildElements("body", NS_FB2);
        String query = settings().bodiesToRender;
        try {
            bodies = XQueryUtilities.getNodes(XQueryUtilities.defaultProlog + query, fb2);
        } catch (Exception ex) {
            throw new FB2toPDFException(ex.toString());
        }
        if (bodies.size() == 0) {
            throw new FB2toPDFException(String.format("Body elements not found for query '%s'", query));
        }


        Element description = getOptionalChildByTagName(root, "description");
        if (description != null) {
            addMetaInfo(description);
        } else {
            System.err.println("Description not found");
        }

        doc.open();
        currentOutline.put(0, writer.getDirectContent().getRootOutline());
        
        if (stylesheet.getPageStyle().footnotes) {
            FootnoteRenderer.init(stylesheet);
        }
        
        secondPassStylesheet = settings().secondPassStylesheet;
        enableDoubleRenderingOutline = settings().enableDoubleRenderingOutline;

        if (!isBlank(secondPassStylesheet)) {
            if (enableDoubleRenderingOutline) {
                fontChangeOutline = addBookmark("Switch style from page...", 0);
                pageElementMap = pageElementMap1;
                elementPageMap = elementPageMap1;
            }
            addBookmark("Pass 1", 0);
        }

        renderBook(description);

        if (!isBlank(secondPassStylesheet)){
            passNamePrefix = "secondPass_";
            addBookmark("Pass 2", 0);
            stylesheet = Stylesheet.readStylesheet(Utilities.getValidatedFileName(secondPassStylesheet));
            doc.setPageSize(getPageSize());
            if (stylesheet.getPageStyle().footnotes) {
                FootnoteRenderer.reinit(stylesheet);
            }
            if (!isBlank(stylesheet.getPageStyle().backgroundImage)){
                if (backgroundImageHelper == null) {
                    setupBackgroundImage();
                } else {
                    backgroundImageHelper.init(stylesheet, doc.getPageSize());
                }
            }
            pageElementMap = pageElementMap2;
            elementPageMap = elementPageMap2;
            
            newPage();
            writer.setPageEmpty(false);
            newPage();

            renderBook(description);
        }

        closePDF();
    }
    
    private class HeaderRefresher extends PdfPageEventHelper {
        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            try {
                refreshHeader();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void setupHeader() throws FB2toPDFException, DocumentException, BadElementException {
        PdfPTable tableOdd = createHeaderTable(true);
        PdfPTable tableEven = createHeaderTable(false);
        float adjustedMargin =  doc.topMargin() + Math.max(tableOdd.getTotalHeight(), tableEven.getTotalHeight());
        stylesheet.getPageStyle().setMarginTop(adjustedMargin);
        doc.setMargins(doc.leftMargin(), doc.rightMargin(), adjustedMargin , doc.bottomMargin());
        headerHelperOdd = new HeaderHelper(doc, writer, tableOdd, HeaderHelper.ODD);
        writer.setPageEvent(headerHelperOdd);
        headerHelperEven = new HeaderHelper(doc, writer, tableEven, HeaderHelper.EVEN);
        writer.setPageEvent(headerHelperEven);
        
        if (stylesheet.getPageStyle().getHeader().dynamic) {
            writer.setPageEvent(new HeaderRefresher());
        }
    }
    
    private void refreshHeader() throws DocumentException, FB2toPDFException {
        PdfPTable tableOdd = createHeaderTable(true);
        PdfPTable tableEven = createHeaderTable(false);
        headerHelperOdd.refresh(tableOdd);
        headerHelperEven.refresh(tableEven);
    }
    
    private void fillFootnoteTemplates() throws IOException {
        
        if (!stylesheet.getPageStyle().footnotes) return;
        
        byte[] noteDoc = FootnoteRenderer.close();

        PdfReader reader = new PdfReader(noteDoc);


        for (int i=0; i<footnoteTemplates.size();i++) {
            PdfImportedPage page = writer.getImportedPage(reader, i+1);
            footnoteTemplates.get(i).addTemplate(page, 0 ,0);
        }
            
    }

    private static class BinaryAttachment {

        private String href;
        private String contentType;
        private nu.xom.Element binary;
        private Image image;

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

        private byte[] getData() {
            Log.info("Loaded binary [{0}] (type [{1}])", this.href, this.contentType);
            return Base64.decodeBase64(this.binary.getValue().getBytes());
        }
        
        public Image getImage(boolean makeGrayImageTransparent, String overrideTransparency, boolean cacheImage) throws BadElementException, MalformedURLException, IOException {
            Image tmp = image;
            if (tmp == null) {

                if (makeGrayImageTransparent) {
                    java.awt.Image img = Utilities.makeGrayTransparent(getData());
                    tmp = Image.getInstance(img, null);
                } else if (overrideTransparency == null || overrideTransparency.isEmpty()) {
                    tmp = Image.getInstance(getData());
                } else {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    java.awt.Image img = toolkit.createImage(getData());
                    tmp = Image.getInstance(img, Color.decode(overrideTransparency));
                }
            }
            if (cacheImage) {
                image = tmp;
            } 
            
            return tmp;
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
                    if (settings().stretchCover) {
                        addStretchedImage(image);
                    } else {
                        addCenteredImage(image);
                    }
                    newPage();
                }
            }
        }

    }
    
    private String getMetaAuthorFullName(Element author) throws FB2toPDFException {
        String query = settings().metaAuthorQuery;
        return XQueryUtilities.getString(author, stylesheet.getTransformationSettings(), query, " ");
    }
    
    private String getBookInfoPageAuthorFullName(Element author) throws FB2toPDFException {
        String query = "(first-name,  middle-name,  last-name)";
        return XQueryUtilities.getString(author, stylesheet.getTransformationSettings(), query, " ");
    }

    private void addMetaInfo(Element description)
            throws FB2toPDFException, DocumentException {
        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            Elements authors = titleInfo.getChildElements("author", NS_FB2);
            StringBuilder allAuthors = new StringBuilder();

            boolean force = settings().forceTransliterateAuthor;

            for (int i = 0; i < authors.size(); ++i) {
                Element author = authors.get(i);
                String authorName = transliterate(getMetaAuthorFullName(author), force);
                Log.info("Adding author [{0}]", authorName);
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
                Log.info("Adding title [{0}]", transliterate(titleString));
            } else if (bookTitle != null && sequences.size() != 0) {
                for (int i = 0; i < sequences.size(); i++) {
                    String subtitle = getSequenceSubtitle(sequences.get(i));
                    doc.addTitle(transliterate(subtitle));
                    Log.info("Adding subtitle [{0}]", transliterate(subtitle));
                }

                String titleString = bookTitle.getValue();
                doc.addTitle(transliterate(titleString));
                Log.info("Adding title [{0}]", transliterate(titleString));
            }
        }
    }

    private void makeBookInfoPage(Element description)
            throws FB2toPDFException, DocumentException {
        ParagraphStyle titleStyle = stylesheet.getParagraphStyle("title");
        ParagraphStyle subtitleStyle = stylesheet.getParagraphStyle("subtitle");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("author");

        if (settings().generateFrontMatter) {
            makeFrontMatter(description);
        }

        addLine(" ", titleStyle);

        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            Elements authors = titleInfo.getChildElements("author", NS_FB2);
            for (int i = 0; i < authors.size(); ++i) {
                Element author = authors.get(i);
                String authorName = getBookInfoPageAuthorFullName(author);
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

        newPage();
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
        Log.info("Adding image [{0}]", href);
        for (int i = 0; i < attachments.size(); ++i) {
            BinaryAttachment attachment = attachments.get(i);
            if (!attachment.getHREF().equals(href)) {
                continue;
            }
            try {
                String overrideTransparency = settings().overrideImageTransparency;
                boolean makeGrayImageTransparent = settings().makeGrayImageTransparent;
                boolean cacheImage = settings().cacheImages;
                return attachment.getImage(makeGrayImageTransparent, overrideTransparency, cacheImage);
            } catch (Exception ex) {
                Log.error(ex.getMessage());
                return null;
            }
        }

        return null;
    }

    private void processBody(Element body)
            throws DocumentException, FB2toPDFException {
        HeaderSettings header = stylesheet.getPageStyle().getHeader();
        if (header.enabled && header.dynamic) {
            chapterTitle = "";
            refreshHeader();
        }
        
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
                Log.warning("Unhandled section tag [{0}]", element.getLocalName());
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

        currentStyle = stylesheet.getParagraphStyle("tocTitle");
        addLine(currentStyle.getText(), currentStyle);

        addSectionsToTOC(sections, 1);

        newPage();
    }

    private PdfOutline addBookmark(String title, int level) {
        if (!currentOutline.containsKey(level)) {
            return null;
        }
        Log.info("Adding bookmark [{0}]", transliterate(title));
        PdfDestination destination = new PdfDestination(PdfDestination.FITH);
        PdfOutline bookmark = new PdfOutline(currentOutline.get(level), destination, transliterate(title), false);
        currentOutline.put(level + 1, bookmark);
        return bookmark;
    }

    private PdfOutline addBookmark(String title, String refname, int level) {
        if (!isBlank(secondPassStylesheet)) {
            level += 1;
        }
        if (!currentOutline.containsKey(level)) {
            return null;
        }
        Log.info("Adding bookmark [{0}] to [{1}]", transliterate(title), refname);
        PdfAction action = PdfAction.gotoLocalPage(refname, false);
        PdfOutline bookmark = new PdfOutline(currentOutline.get(level), action, transliterate(title), false);
        currentOutline.put(level + 1, bookmark);
        return bookmark;
    }

    private void processSection(Element section, int level, int index)
            throws DocumentException, FB2toPDFException {

        Float newPagePosition = stylesheet.getPageStyle().sectionNewPage.get(level);

        if (newPagePosition != null && getVerticalPosition() < doc.getPageSize().getHeight() * newPagePosition) {
            newPage();
            writer.setPageEmpty(false);
        }

        String id = section.getAttributeValue("id");
        if (isBlank(id)) {
            id = String.format("section%d", section.hashCode());
        }
        
        if (!isBlank(id)) {
            addInvisibleAnchor(id);
        }

        if (bodyIndex == 0) {
            Nodes nodes = section.query("./fb:title//*[not(@type) or @type != 'note']/text()", xCtx);
            String bmk = getTextContent(nodes, " ", null);
            if (StringUtils.isNotBlank(bmk)) {
                addBookmark(bmk, passNamePrefix + id, level);
            }
        }
        
        HeaderSettings header = stylesheet.getPageStyle().getHeader();
        if (header.enabled && header.dynamic) {
            String query = header.chapterTitle;
            chapterTitle = XQueryUtilities.getString(section, stylesheet.getTransformationSettings(), query, " ");
            Log.info("Header chapter [{0}]", chapterTitle);
            refreshHeader();
        }

        processSectionContent(section, level);

        if (bodyIndex > 0 && StringUtils.isNotBlank(id) &&
                settings().generateNoteBackLinks) {
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
                boolean previousInlineMode = settings().enableInlineImages;
                processTable(element);
                settings().enableInlineImages = previousInlineMode;
                currentStyle = previousStyle;
            } else if (element.getLocalName().equals("title")) {
                processTitle(element, level);
            } else if (element.getLocalName().equals("epigraph")) {
                processEpigraph(element);
            } else {
                Log.warning("Unhandled section tag [{0}]", element.getLocalName());
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
                boolean previousInlineMode = settings().enableInlineImages;
                processTable(element);
                settings().enableInlineImages = previousInlineMode;
                currentStyle = previousStyle;
            } else {
                Log.warning("Unhandled section tag [{0}]", element.getLocalName());
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
                processParagraph(element, i == 0, i == children.size() - 1);
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                Log.warning("Unhandled title tag [{0}]", element.getLocalName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processEpigraph(Element epigraph)
            throws DocumentException, FB2toPDFException {
        addInvisibleAnchor(epigraph);

        ParagraphStyle previousStyle = currentStyle;

        currentStyle = stylesheet.getParagraphStyle("epigraph");

        Elements children = epigraph.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            boolean first = (i == 0);
            boolean last = (i == children.size() - 1);

            if (element.getLocalName().equals("p")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("poem")) {
                processPoem(element);
            } else if (element.getLocalName().equals("cite")) {
                processCite(element);
            } else if (element.getLocalName().equals("text-author")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                Log.warning("Unhandled tag '" + element.getLocalName() + "' inside 'epigraph'");
            }
        }

        currentStyle = previousStyle;
    }

    private void processCite(Element cite)
            throws DocumentException, FB2toPDFException {
        addInvisibleAnchor(cite);

        ParagraphStyle previousStyle = currentStyle;

        currentStyle = stylesheet.getParagraphStyle("cite");

        Elements children = cite.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);
            
            boolean first = (i == 0);
            boolean last = (i == children.size() - 1);

            if (element.getLocalName().equals("p")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("subtitle")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("poem")) {
                processPoem(element);
            } else if (element.getLocalName().equals("text-author")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                Log.warning("Unhandled tag [{0}] inside 'cite'", element.getLocalName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processPoem(Element poem)
            throws DocumentException, FB2toPDFException {
        addInvisibleAnchor(poem);
        ParagraphStyle previousStyle = currentStyle;

        currentStyle = stylesheet.getParagraphStyle("poem");

        Elements children = poem.getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            Element element = children.get(i);

            boolean first = (i == 0);
            boolean last = (i == children.size() - 1);

            if (element.getLocalName().equals("stanza")) {
                processStanza(element);
            } else if (element.getLocalName().equals("title")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("date")) {
                processParagraph(element, first, last);
            } else if (element.getLocalName().equals("epigraph")) {
                processEpigraph(element);
            } else if (element.getLocalName().equals("text-author")) {
                processParagraph(element, first, last);
            } else {
                Log.warning("Unhandled poem tag [{0}]", element.getLocalName());
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
                Log.warning("Unhandled stanza tag [{0}]", element.getLocalName());
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

        currentElementHash = paragraph.hashCode();
        addElement(currentParagraph);
        currentParagraph = null;
        currentReference = null;
    }

    private void addDropCap(String text) throws DocumentException, FB2toPDFException {

        ParagraphStyle dropcapStyle = stylesheet.getParagraphStyle(currentStyle.getDropcapStyle());

        float dropCapSize = dropcapStyle.getFontSize();
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

        if (getVerticalPosition() < spacingBefore + templateHight + doc.bottomMargin()) {
            newPage();
        }
        addElement(dropcap);
        //tp.setBoundingBox(new Rectangle(0,descent,templateWidth,ascent));
        tp.setBoundingBox(new Rectangle(-100, -100, 100, 100));
    }

    private void flushCurrentChunk()
            throws DocumentException, FB2toPDFException {
        if (currentChunk != null) {
            if (!isBlank(currentReference)) {
                if (currentReference.charAt(0) == '#') {
                    addInternalLink();
                } else {
                    addExternalLink();
                }
            } else {
                String currentAnchorName = anchorStack.isEmpty() ? null : anchorStack.pop();
                if (currentAnchorName != null) {
                    addAnchor(currentAnchorName);
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
        ParagraphStyle previousContainerStyle = currentStyle.containerStyle;
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
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                } else if (child.getLocalName().equals("a")) {
                    flushCurrentChunk();
                    currentReference = child.getAttributeValue("href", NS_XLINK);
                    if (currentReference.length() == 0) {
                        currentReference = child.getAttributeValue("href");
                    }
                    processParagraphContent(child);
                    flushCurrentChunk();
                    if (settings().enableLinkPageNum && currentReference.startsWith("#")) {
                        addPageNumTemplate(settings().linkPageNumFormat);
                    }

                    addFootnote(child);
                    currentReference = null;
                } else if (child.getLocalName().equals("style")) {
                    String styleName = child.getAttributeValue("name");
                    Log.warning("Style tag {0}" + styleName + " ignored.");
                    processParagraphContent(child);
                } else if (child.getLocalName().equals("image")) {
                    flushCurrentChunk();
                    addImage(child, settings().enableInlineImages);
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
                    Log.warning("Unhandled paragraph tag [{0}]", child.getLocalName());
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
                        addDropCap(dropcap);
                    }
                }

                if (currentChunk == null) {
                    currentChunk = currentStyle.createChunk();
                    if (superscript) {
                        currentChunk.setTextRise(currentStyle.getFontSize() / 3);
                    }
                    if (subscript) {
                        currentChunk.setTextRise(-currentStyle.getFontSize() / 6);
                    }
                    if (!currentStyle.getDisableHyphenation()) {
                        currentChunk.setHyphenation(hyphenation);
                    }
                }

                currentChunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings(), currentStyle));
            }
        }
        currentStyle = previousStyle;
        currentStyle.containerStyle = previousContainerStyle;
    }

    private String transliterate(String text) {
        return transliterate(text, false);
    }

    private String transliterate(String text, boolean force) {
        if (!settings().transliterateMetaInfo && !force) {
            return text;
        }
        return Translit.get(text);
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
                Log.info("Usage: java {0} <input.fb2> <output.pdf>" + FB2toPDF.class.getName());
                return;
            }
            translate(args[0], args[1]);
        } catch (Exception e) {
            Log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private String getLang(Element description) throws FB2toPDFException {
        Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            Element lang = getOptionalChildByTagName(titleInfo, "lang");
            if (lang != null) {
                String langString = lang.getValue();
                Log.info("Language of the FB2 is [{0}]", langString);
                return langString;
            }
        }
        Log.info("Language of the FB2 not found");
        return null;
    }

    private void setupHyphenation(Element description) throws FB2toPDFException {
        HyphenationSettings hyphSettings = stylesheet.getHyphenationSettings();
        if (hyphSettings.hyphenate) {
            Log.info("Hyphenation is on");
            String bookLang = getLang(description);
            if (isBlank(bookLang) || hyphSettings.overrideLanguage) {
                bookLang = hyphSettings.defaultLanguage;
            }
            hyphenation = new HyphenationAuto(bookLang, "none", 2, 2);
            Log.info("Hyphenation language is [{0}]", bookLang);
        } else {
            Log.info("Hyphenation is off");
        }
    }
}
