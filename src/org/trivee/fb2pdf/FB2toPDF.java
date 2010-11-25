package org.trivee.fb2pdf;

import java.io.*;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Image;

import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfAction;

import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfTemplate;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

public class FB2toPDF
{
    public static String FB2PDF_HOME = ".";

    static
    {
        String fb2pdf_home = System.getenv("FB2PDF_HOME");
        if (fb2pdf_home != null)
            FB2PDF_HOME = fb2pdf_home;
    }

    private static final String NS_XLINK = "http://www.w3.org/1999/xlink";
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
    private org.w3c.dom.Document fb2;
    private com.itextpdf.text.Document doc;
    private PdfWriter writer;
    int bodyIndex;
    private ElementCollection bodies;
    private Paragraph currentParagraph;
    private String currentReference;
    private Chunk currentChunk;
    private boolean superscript;
    private boolean subscript;
    private HyphenationAuto hyphenation;
    private Stylesheet stylesheet;

    private FB2toPDF(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }

    private void addAnchor(Element element) throws FB2toPDFException, DocumentException {
        String id = element.getAttribute("id");
        if (id.length() > 0) {
            addAnchor(id);
        }
    }

    protected void addAnchor(String id) throws DocumentException, FB2toPDFException {
        Anchor anchor = currentStyle.createInvisibleAnchor();
        anchor.setName(id);
        doc.add(anchor);
        System.out.println("Adding A NAME=" + id);
    }

    protected void addBacklink(String id) throws DocumentException, FB2toPDFException {
        Chunk chunk = currentStyle.createChunk();
        chunk.append("[^^^]");
        addGoToActionToChunk(id + "_backlink", chunk);
        addEmptyLine();
        addLine(chunk, currentStyle);
    }

    protected void addCenteredImage(Image image) throws DocumentException {
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

    protected void addImage(Image image) throws DocumentException {
        Rectangle pageSize = doc.getPageSize();
        float dpi = stylesheet.getGeneralSettings().imageDpi;
        float scaleWidth = (pageSize.getWidth() - doc.leftMargin() - doc.rightMargin()) * 0.95f;
        float scaleHeight = (pageSize.getHeight() - doc.topMargin() - doc.bottomMargin()) * 0.95f;
        float imgWidth = image.getWidth() / dpi * 72;
        float imgHeight = image.getHeight() / dpi * 72;
        if ((imgWidth <= scaleWidth) && (imgHeight <= scaleHeight)) {
            scaleWidth = imgWidth;
            scaleHeight = imgHeight;
        }
        image.scaleToFit(scaleWidth, scaleHeight);
        image.setAlignment(Image.MIDDLE);
        doc.add(image);
    }

    protected void addInvisibleAnchor(String name) throws FB2toPDFException, DocumentException {
        Anchor anchor = currentStyle.createInvisibleAnchor();
        anchor.setName(name);
        currentParagraph.add(anchor);
        System.out.println("Adding A NAME=" + name);
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

    private void addFootnote(String refname) throws DocumentException, FB2toPDFException {
        refname = refname.substring(1);
        System.out.println("Adding footnote " + refname);
        String body = getNoteBody(refname);
        byte[] noteDoc = FootnoteRenderer.renderNoteDoc(stylesheet, body);
        List<Image> noteLineImages = getLinesImages(noteDoc);
        System.out.printf("Footnote has %d lines\n", noteLineImages.size());
        for (Image image: noteLineImages){
            currentParagraph.add(image);
        }
    }

    private String getNoteBody(String refname) {
        if (StringUtils.isBlank(refname)) return "";
        Element noteBody = getNotesBody();
        if (noteBody == null) return "";

        NodeList sections = noteBody.getElementsByTagName("section");
        for (int i=0; i<sections.getLength(); i++){
            Element section = (Element)sections.item(i);
            String id = section.getAttribute("id");
            if (refname.equals(id)){
                String text = section.getTextContent();
                text = text.replaceAll("\n", " ").replaceAll("  ", " ").trim();
                return text;
            }
        }
        System.out.printf("WARNING: note %s not found\n", refname);
        return "";
    }

    private Element getNotesBody() {
        for (int i = 0; i < bodies.getLength(); i++) {
            Element body = bodies.item(i);
            if ("notes".equals(body.getAttribute("name"))) {
                return body;
            }
        }
        System.out.println("WARNING: notes not found in the document");
        return null;
    }

    private List<Image> getLinesImages(byte[] noteDoc) {
        List<Image> result = new ArrayList<Image>();
        try {
            PdfReader reader = new PdfReader(noteDoc);

            for (int i=2; i<reader.getNumberOfPages(); i++){
                PdfImportedPage page = writer.getImportedPage(reader, i);
                Image image = Image.getInstance(page);
                image.setSpacingBefore(0);
                image.setSpacingAfter(0);
                image.setAlignment(Image.TEXTWRAP);
                result.add(image);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    private class PageEventHandler extends PdfPageEventHelper {

        public boolean enforcePageSize = false;
        public Color pageSizeEnforcerColor;
        private Image marginEnforcerImage = null;

        protected void addPageSizeEnforcer(PdfWriter writer) {
            if (marginEnforcerImage == null) {
                marginEnforcerImage = createEnforcerImage();
            }
            PdfContentByte cb = writer.getDirectContent();
            Rectangle pageSize = writer.getPageSize();
            try {
                float sz = 2.5f;
                float dx = pageSize.getWidth() - sz;
                float dy = pageSize.getHeight() - sz;
                cb.addImage(marginEnforcerImage, sz, 0, 0, sz, 0, dy);
                cb.addImage(marginEnforcerImage, 0, sz, -sz, 0, sz, 0);
                cb.addImage(marginEnforcerImage, -sz, 0, 0, -sz, dx + sz, sz);
                cb.addImage(marginEnforcerImage, 0, -sz, sz, 0, dx, dy + sz);
            } catch (DocumentException ex) {
                Logger.getLogger(FB2toPDF.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private Image createEnforcerImage() {
            int[] transparency = new int[]{0, 0, 0, 0, 0, 0};

            byte R = pageSizeEnforcerColor == null ? 127 : (byte) pageSizeEnforcerColor.getRed();
            byte G = pageSizeEnforcerColor == null ? 127 : (byte) pageSizeEnforcerColor.getGreen();
            byte B = pageSizeEnforcerColor == null ? 127 : (byte) pageSizeEnforcerColor.getBlue();
            byte[] data = new byte[]{
                R, G, B, 0, 0, 0, R, G, B, 0, 0, 0, R, G, B,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                R, G, B, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                R, G, B, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            };

            Image img = null;
            try {
                img = Image.getInstance(5, 5, 3, 8, data, transparency);
            } catch (BadElementException ex) {
                Logger.getLogger(FB2toPDF.class.getName()).log(Level.SEVERE, null, ex);
            }
            return img;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (enforcePageSize) {
                addPageSizeEnforcer(writer);
            }
        }
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

    private boolean isIgnoreEmptyLine(Element element) {

        Node nextSib = element.getNextSibling();
        Node prevSib = element.getPreviousSibling();
        boolean ignore = false;
        if (nextSib != null && nextSib.getNodeName().equalsIgnoreCase("image")
                && stylesheet.getGeneralSettings().ignoreEmptyLineBeforeImage) {
            System.out.println("Skipping empty line before image");
            ignore = true;
        }
        if (prevSib != null && prevSib.getNodeName().equalsIgnoreCase("image")
                && stylesheet.getGeneralSettings().ignoreEmptyLineAfterImage) {
            System.out.println("Skipping empty line after image");
            ignore = true;
        }

        return ignore;
    }

    private void addImage(Element element) throws DocumentException, DOMException, FB2toPDFException {
        String href = element.getAttributeNS(NS_XLINK, "href");
        Image image = getImage(href);
        if (image != null) {
            addAnchor(element);
            addImage(image);
        } else {
            System.out.println("Image not found, href: " + href);
        }
    }

    private String getSequenceSubtitle(Element seq) {
        String seqname = seq.getAttribute("name");
        String seqnumber = seq.getAttribute("number");
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
            throws IOException, FileNotFoundException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setCoalescing(true);
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setAttribute("http://apache.org/xml/features/continue-after-fatal-error", true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.ErrorHandler() {

            public void fatalError(org.xml.sax.SAXParseException e) {
                System.err.println("SAX fatal error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
            }

            public void error(org.xml.sax.SAXParseException e) {
                System.err.println("SAX fatal error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
            }

            public void warning(org.xml.sax.SAXParseException e) {
                System.err.println("SAX fatal error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
            }
        });

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

        /*
        try {
            nu.xom.Document xdoc = new nu.xom.Builder().build(is);
            String query = "declare default element namespace \"http://www.gribuser.ru/xml/fictionbook/2.0\";";
            String morpher = "declare default element namespace \"http://www.gribuser.ru/xml/fictionbook/2.0\";";
            morpher += "declare namespace l = \"http://www.w3.org/1999/xlink\";";
            //query += "//cite//emphasis";
            //morpher += "./text()";
            query += "//a[@type='note']";
            morpher += "declare variable $hid := ./@l:href; <emphasis> [{//body[@name='notes']//section[@id=substring($hid,2)]/*[local-name()!='title']/text()}]</emphasis>";
            nux.xom.xquery.XQuery xselect = nux.xom.pool.XQueryPool.GLOBAL_POOL.getXQuery(query, null);
            nux.xom.xquery.XQuery xmorpher = nux.xom.pool.XQueryPool.GLOBAL_POOL.getXQuery(morpher, null);
            nux.xom.xquery.XQueryUtil.update(xselect.execute(xdoc).toNodes(), xmorpher, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            nu.xom.Serializer ser = new nu.xom.Serializer(out);
            ser.write(xdoc);
            out.close();
            is = new ByteArrayInputStream(out.toByteArray());
        } catch(Exception ex) {
            System.err.println(ex);
            System.exit(-1);
        }
        */

        fb2 = builder.parse(is);
    }

    private void createPDFDoc()
            throws DocumentException, FileNotFoundException {
        final PageStyle pageStyle = stylesheet.getPageStyle();
        final GeneralSettings generalSettings = stylesheet.getGeneralSettings();

        Rectangle pageSize = new Rectangle(pageStyle.getPageWidth(), pageStyle.getPageHeight());

        doc = new com.itextpdf.text.Document(pageSize,
                pageStyle.getMarginLeft(), pageStyle.getMarginRight(),
                pageStyle.getMarginTop(), pageStyle.getMarginBottom());

        writer = PdfWriter.getInstance(doc, new FileOutputStream(toName));
        if (pageStyle.enforcePageSize) {
            PageEventHandler pageEventHandler = new PageEventHandler();
            pageEventHandler.enforcePageSize = pageStyle.enforcePageSize;
            pageEventHandler.pageSizeEnforcerColor = Color.decode(pageStyle.pageSizeEnforcerColor);
            writer.setPageEvent(pageEventHandler);
        }
        writer.setSpaceCharRatio(generalSettings.trackingSpaceCharRatio);
        writer.setStrictImageSequence(generalSettings.strictImageSequence);
        PdfDocument.preventWidows = pageStyle.preventWidows;
        PdfDocument.hangingPunctuation = generalSettings.hangingPunctuation;
    }

    private void closePDF() {
        doc.close();
    }

    /*
    private static org.w3c.dom.Element getOnlyChildByTagName(org.w3c.dom.Element element, String tagName)
    throws FB2toPDFException
    {
    ElementCollection children = ElementCollection.childrenByTagName(element, tagName);
    if (children.getLength() == 0)
    throw new FB2toPDFException("Element not found: " + element.getTagName() + "/" + tagName);
    else if (children.getLength() > 1)
    throw new FB2toPDFException("More than one element found: " + element.getTagName() + "/" + tagName);
    return children.item(0);
    }
     */
    private static org.w3c.dom.Element getOptionalChildByTagName(org.w3c.dom.Element element, String tagName)
            throws FB2toPDFException {
        ElementCollection children = ElementCollection.childrenByTagName(element, tagName);
        if (children.getLength() == 0) {
            return null;
        } else if (children.getLength() > 1) {
            throw new FB2toPDFException("More than one element found: " + element.getTagName() + "/" + tagName);
        }
        return children.item(0);
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

    private static String getTextContentByTagName(org.w3c.dom.Element element, String tagName, String prefix, String suffix) {
        // collect text content
        ElementCollection children = ElementCollection.childrenByTagName(element, tagName);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.getLength(); ++i) {
            String content = children.item(i).getTextContent();
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

    private static String getTextContentByTagName(org.w3c.dom.Element element, String tagName, String prefix) {
        return getTextContentByTagName(element, tagName, prefix, null);
    }

    private static String getTextContentByTagName(org.w3c.dom.Element element, String tagName) {
        return getTextContentByTagName(element, tagName, null, null);
    }

    private void run(InputStream stylesheetInputStream)
            throws IOException, DocumentException, FB2toPDFException, ParserConfigurationException, SAXException {
        loadData(stylesheetInputStream);

        readFB2();
        createPDFDoc();

        org.w3c.dom.Element root = fb2.getDocumentElement();
        if (!root.getTagName().equals("FictionBook")) {
            throw new FB2toPDFException("The file does not seems to contain 'fictionbook' root element");
        }


        extractBinaries(root);
        // findEnclosures(fb, outdir, outname)

        org.w3c.dom.Element description = getOptionalChildByTagName(root, "description");
        if (description != null) {
            setupHyphenation(description);
            addMetaInfo(description);
            doc.open();
            processDescription(description);
        } else {
            doc.open();
            System.err.println("Description not found");
        }


        bodies = ElementCollection.childrenByTagName(root, "body");
        if (bodies.getLength() == 0) {
            throw new FB2toPDFException("Element not found: FictionBook/body");
        }

        org.w3c.dom.Element body = bodies.item(0);
        if (stylesheet.getGeneralSettings().generateTOC) {
            makeTOCPage(body);
        }

        for (int i = 0; i < bodies.getLength(); ++i) {
            body = (org.w3c.dom.Element) bodies.item(i);
            bodyIndex = i;
            processBody(body);
            doc.newPage();
        }

        closePDF();
    }

    private static class BinaryAttachment {

        private String href;
        private String contentType;
        private byte[] data;

        public BinaryAttachment(org.w3c.dom.Element binary) {
            this.href = "#" + binary.getAttribute("id");
            this.contentType = binary.getAttribute("content-type");
            this.data = Base64.decodeBase64(binary.getTextContent().getBytes());

            System.out.println("Loaded binary " + this.href + " (" + this.contentType + ")");
        }

        public String getHREF() {
            return href;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            return data;
        }
    };
    private ArrayList<BinaryAttachment> attachments = new ArrayList<BinaryAttachment>();

    private void extractBinaries(org.w3c.dom.Element root) {
        ElementCollection binaries = ElementCollection.childrenByTagName(root, "binary");
        for (int i = 0; i < binaries.getLength(); ++i) {
            org.w3c.dom.Element binary = binaries.item(i);
            attachments.add(new BinaryAttachment(binary));
        }
    }
    private ParagraphStyle currentStyle;
    private PdfOutline currentOutline;

    private void processDescription(org.w3c.dom.Element description)
            throws FB2toPDFException, DocumentException {
        makeCoverPage(description);
        makeBookInfoPage(description);
    }

    private void addLine(String text, ParagraphStyle style)
            throws FB2toPDFException, DocumentException {
        Chunk chunk = style.createChunk();
        chunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings()));
        addLine(chunk, style);
    }

    private void makeCoverPage(org.w3c.dom.Element description)
            throws FB2toPDFException, DocumentException {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo == null) {
            System.err.println("Title info not found");
            return;
        }

        org.w3c.dom.Element coverPage = getOptionalChildByTagName(titleInfo, "coverpage");
        if (coverPage != null) {
            ElementCollection images = ElementCollection.childrenByTagName(coverPage, "image");
            for (int i = 0; i < images.getLength(); ++i) {
                org.w3c.dom.Element coverImage = images.item(i);

                String href = coverImage.getAttributeNS(NS_XLINK, "href");
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

    private String getAuthorFullName(org.w3c.dom.Element author) throws FB2toPDFException {
        String firstName = getTextContentByTagName(author, "first-name");
        String middleName = getTextContentByTagName(author, "middle-name");
        String lastName = getTextContentByTagName(author, "last-name");
        return String.format("%s %s %s", firstName, middleName, lastName).trim();
    }

    private void addMetaInfo(org.w3c.dom.Element description)
            throws FB2toPDFException, DocumentException {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            ElementCollection authors = ElementCollection.childrenByTagName(titleInfo, "author");
            StringBuilder allAuthors = new StringBuilder();

            boolean force = stylesheet.getGeneralSettings().forceTransliterateAuthor;

            for (int i = 0; i < authors.getLength(); ++i) {
                org.w3c.dom.Element author = authors.item(i);
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

            org.w3c.dom.Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            ElementCollection sequences = ElementCollection.childrenByTagName(titleInfo, "sequence");

            if (bookTitle != null && sequences.getLength() == 0) {
                String titleString = bookTitle.getTextContent();
                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
            } else if (bookTitle != null && sequences.getLength() != 0) {
                for (int i = 0; i < sequences.getLength(); i++) {
                    String subtitle = getSequenceSubtitle(sequences.item(i));
                    doc.addTitle(transliterate(subtitle));
                    System.out.println("Adding subtitle: " + transliterate(subtitle));
                }

                String titleString = bookTitle.getTextContent();
                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
            }
        }
    }

    private void makeBookInfoPage(org.w3c.dom.Element description)
            throws FB2toPDFException, DocumentException {
        ParagraphStyle titleStyle = stylesheet.getParagraphStyle("title");
        ParagraphStyle subtitleStyle = stylesheet.getParagraphStyle("subtitle");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("author");

        if (stylesheet.getGeneralSettings().generateFrontMatter) {
            makeFrontMatter(description);
        }

        addLine(" ", titleStyle);

        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            ElementCollection authors = ElementCollection.childrenByTagName(titleInfo, "author");
            for (int i = 0; i < authors.getLength(); ++i) {
                org.w3c.dom.Element author = authors.item(i);
                String authorName = getAuthorFullName(author);
                // doc.addAuthor(transliterate(authorName));
                addLine(authorName, authorStyle);
            }

            org.w3c.dom.Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            ElementCollection sequences = ElementCollection.childrenByTagName(titleInfo, "sequence");

            if (bookTitle != null && sequences.getLength() == 0) {
                addLine(" ", titleStyle);
                addLine(bookTitle.getTextContent(), titleStyle);
                addLine(" ", titleStyle);
            } else if (bookTitle != null && sequences.getLength() != 0) {
                addLine(" ", titleStyle);
                addLine(bookTitle.getTextContent(), titleStyle);
                for (int i = 0; i < sequences.getLength(); i++) {
                    String subtitle = getSequenceSubtitle(sequences.item(i));
                    addLine(subtitle, subtitleStyle);
                    addLine(" ", titleStyle);
                }
            }

            org.w3c.dom.Element annotation = getOptionalChildByTagName(titleInfo, "annotation");
            if (annotation != null) {

                currentStyle = stylesheet.getParagraphStyle("annotation");
                processSectionContent(annotation, 0);
                currentStyle = null;
            }
        }

        doc.newPage();
    }

    private void makeFrontMatter(org.w3c.dom.Element description)
            throws FB2toPDFException, DocumentException {

        ParagraphStyle frontMatter = stylesheet.getParagraphStyle("frontMatter");
        org.w3c.dom.Element publishInfo = getOptionalChildByTagName(description, "publish-info");
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

        org.w3c.dom.Element documentInfo = getOptionalChildByTagName(description, "document-info");
        ElementCollection documentAuthors = null;
        if (documentInfo != null) {
            documentAuthors = ElementCollection.childrenByTagName(documentInfo, "author");
        }

        for (int i = 0; documentAuthors != null && i < documentAuthors.getLength(); ++i) {
            org.w3c.dom.Element documentAuthor = documentAuthors.item(i);
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

        addLine("PDF: org.trivee.fb2pdf.FB2toPDF 1.0, "
                + java.text.DateFormat.getDateInstance().format(new java.util.Date()),
                frontMatter);
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
            } catch (BadElementException e) {
                e.printStackTrace();
                return null;
            } catch (java.net.MalformedURLException e1) {
                e1.printStackTrace();
                return null;
            } catch (IOException e2) {
                e2.printStackTrace();
                return null;
            }
        }

        return null;
    }

    private void processBody(org.w3c.dom.Element body)
            throws DocumentException, FB2toPDFException {
        currentStyle = stylesheet.getParagraphStyle("body");
        //processSections(body);
        processSectionContent(body, -1);
        currentStyle = null;
    }

    private void makeTOCPage(org.w3c.dom.Element body)
            throws DocumentException, FB2toPDFException {
        ElementCollection sections = ElementCollection.childrenByTagName(body, "section");
        if (sections.getLength() <= 1) {
            return;
        }

        ParagraphStyle tocTitleStyle = stylesheet.getParagraphStyle("tocTitle");
        ParagraphStyle tocItemStyle = stylesheet.getParagraphStyle("tocItem");

        addLine(tocTitleStyle.getText(), tocTitleStyle);

        for (int i = 0; i < sections.getLength(); ++i) {
            org.w3c.dom.Element section = sections.item(i);

            String title = getTextContentByTagName(section, "title");
            if (title.length() == 0) {
                title = "#" + (i + 1);
            }

            Chunk chunk = tocItemStyle.createChunk();
            chunk.append(TextPreprocessor.process(title, stylesheet.getTextPreprocessorSettings()));

            String ref = section.getAttribute("id");
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

    /*
    private void processSections(org.w3c.dom.Element body)
    throws DocumentException, FB2toPDFException
    {
    ElementCollection sections = ElementCollection.childrenByTagName(body, "section");
    for (int i = 0; i < sections.getLength(); ++i)
    {
    org.w3c.dom.Element section = sections.item(i);
    // XXX TODO if s.getAttribute('id') not in notes],'')
    processSection(section, 0, i);
    }

    }
     */
    private PdfOutline addBookmark(String title) {
        System.out.println("Adding bookmark: " + transliterate(title));
        PdfDestination destination = new PdfDestination(PdfDestination.FITH);
        return new PdfOutline(currentOutline, destination, transliterate(title));
    }

    private void processSection(org.w3c.dom.Element section, int level, int index)
            throws DocumentException, FB2toPDFException {
        PdfOutline previousOutline = currentOutline;

        Float newPagePosition = stylesheet.getPageStyle().sectionNewPage.get(level);

        if (newPagePosition != null && writer.getVerticalPosition(false) < doc.getPageSize().getHeight() * newPagePosition) {
            doc.newPage();
            writer.setPageEmpty(false);
        }

        if (level == 0) {
            //doc.newPage();
            if (bodyIndex == 0) {
                String bmk = getTextContentByTagName(section, "title");
                if (StringUtils.isNotBlank(bmk)) {
                    currentOutline = writer.getDirectContent().getRootOutline();
                    currentOutline = addBookmark(bmk);
                }
            }
        } else if (level == 1) {
            //if (writer.getVerticalPosition(false) < doc.getPageSize().getHeight() * 0.5f) {
            //    doc.newPage();
            //}
            //writer.setPageEmpty(false);
            if (bodyIndex == 0) {
                String bmk = getTextContentByTagName(section, "title");
                if (StringUtils.isNotBlank(bmk)) {
                    addBookmark(bmk);
                }
            }
        }

        String id = section.getAttribute("id");
        if (id.length() == 0 && bodyIndex == 0 && level == 0) {
            id = "section" + index;
        }

        if (id.length() > 0) {
            addAnchor(id);
        }

        processSectionContent(section, level);

        if (bodyIndex > 0 && StringUtils.isNotBlank(id)) {
            addBacklink(id);
        }

        if (previousOutline != null) {
            currentOutline = previousOutline;
        }
    }

    private void addEmptyLine()
            throws DocumentException, FB2toPDFException {
        addLine(" ", currentStyle);
    }

    private void processSectionContent(org.w3c.dom.Element parent, int level)
            throws DocumentException, FB2toPDFException {
        boolean bFirst = true;

        ElementCollection nodes = ElementCollection.children(parent);
        int subsectionIndex = 0;
        for (int i = 0; i < nodes.getLength(); ++i) {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("section")) {
                processSection(element, level + 1, subsectionIndex);
                subsectionIndex++;
            } else if (element.getTagName().equals("p")) {
                /* XXX TODO
                pid=x.getAttribute('id')
                if pid:
                res+='\\hypertarget{%s}{}\n' % pid
                 */
                processParagraph(element, bFirst, i == nodes.getLength() - 1);
                bFirst = false;
            } else if (element.getTagName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else if (element.getTagName().equals("image")) {
                addImage(element);
            } else if (element.getTagName().equals("poem")) {
                processPoem(element);
            } else if (element.getTagName().equals("subtitle")) {
                ParagraphStyle previousStyle = currentStyle;
                currentStyle = stylesheet.getParagraphStyle("bodySubtitle");
                processParagraph(element, true, true);
                currentStyle = previousStyle;
            } else if (element.getTagName().equals("cite")) {
                processCite(element);
            } else if (element.getTagName().equals("table")) {
                // XXX TODO logging.getLogger('fb2pdf').warning("Unsupported element: %s" % x.tagName)
                // XXX TODO pass # TODO
                System.out.println("Unhandled section tag " + element.getTagName());
            } else if (element.getTagName().equals("title")) {
                processTitle(element, level);
            } else if (element.getTagName().equals("epigraph")) {
                processEpigraph(element);
            } else {
                // XXX TODO logging.getLogger('fb2pdf').error("Unknown section element: %s" % x.tagName)
                System.out.println("Unhandled section tag " + element.getTagName());
            }
        }
    }

    private void processTitle(org.w3c.dom.Element title, int level)
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

        ElementCollection nodes = ElementCollection.children(title);
        for (int i = 0; i < nodes.getLength(); ++i) {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("p")) {
                /* XXX TODO
                pid=x.getAttribute('id')
                if pid:
                res+='\\hypertarget{%s}{}\n' % pid
                 */
                processParagraph(element, i == 0, i == nodes.getLength() - 1);
            } else if (element.getTagName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                System.out.println("Unhandled title tag " + element.getTagName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processEpigraph(org.w3c.dom.Element epigraph)
            throws DocumentException, FB2toPDFException {
        addAnchor(epigraph);

        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle = stylesheet.getParagraphStyle("epigraph");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("epigraphAuthor");

        currentStyle = mainStyle;

        ElementCollection nodes = ElementCollection.children(epigraph);
        for (int i = 0; i < nodes.getLength(); ++i) {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("p")) {
                processParagraph(element, i == 0, i == nodes.getLength() - 1);
            } else if (element.getTagName().equals("poem")) {
                processPoem(element);
            } else if (element.getTagName().equals("cite")) {
                processCite(element);
            } else if (element.getTagName().equals("text-author")) {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getTagName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                System.out.println("Unhandled tag '" + element.getTagName() + "' inside 'epigraph'");
            }
        }

        currentStyle = previousStyle;
    }

    private void processCite(org.w3c.dom.Element cite)
            throws DocumentException, FB2toPDFException {
        addAnchor(cite);

        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle = stylesheet.getParagraphStyle("cite");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("citeAuthor");
        ParagraphStyle subtitleStyle = stylesheet.getParagraphStyle("citeSubtitle");

        currentStyle = mainStyle;

        ElementCollection nodes = ElementCollection.children(cite);
        for (int i = 0; i < nodes.getLength(); ++i) {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("p")) {
                processParagraph(element, i == 0, i == nodes.getLength() - 1);
            } else if (element.getTagName().equals("subtitle")) {
                currentStyle = subtitleStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getTagName().equals("poem")) {
                processPoem(element);
            } else if (element.getTagName().equals("text-author")) {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getTagName().equals("empty-line")) {
                if (!isIgnoreEmptyLine(element)) {
                    addEmptyLine();
                }
            } else {
                System.out.println("Unhandled tag '" + element.getTagName() + "' inside 'cite'");
            }
        }

        currentStyle = previousStyle;
    }

    private void processPoem(org.w3c.dom.Element poem)
            throws DocumentException, FB2toPDFException {
        addAnchor(poem);
        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle = stylesheet.getParagraphStyle("poem");
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle("poemAuthor");
        ParagraphStyle titleStyle = stylesheet.getParagraphStyle("poemTitle");
        ParagraphStyle dateStyle = stylesheet.getParagraphStyle("poemDate");

        currentStyle = mainStyle;

        ElementCollection nodes = ElementCollection.children(poem);
        for (int i = 0; i < nodes.getLength(); ++i) {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("stanza")) {
                processStanza(element);
            } else if (element.getTagName().equals("title")) {
                currentStyle = titleStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getTagName().equals("date")) {
                currentStyle = dateStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else if (element.getTagName().equals("epigraph")) {
                processEpigraph(element);
            } else if (element.getTagName().equals("text-author")) {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            } else {
                System.out.println("Unhandled poem tag " + element.getTagName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processStanza(org.w3c.dom.Element stanza)
            throws DocumentException, FB2toPDFException {
        ParagraphStyle previousStyle = currentStyle;
        currentStyle = stylesheet.getParagraphStyle("poem");

        ElementCollection nodes = ElementCollection.children(stanza);
        for (int i = 0; i < nodes.getLength(); ++i) {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("v")) {
                processParagraph(element, i == 0, i == nodes.getLength() - 1);
            } else {
                System.out.println("Unhandled stanza tag " + element.getTagName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processParagraph(org.w3c.dom.Element paragraph, boolean bFirst, boolean bLast)
            throws DocumentException, FB2toPDFException {
        currentParagraph = currentStyle.createParagraph(bFirst, bLast);

        addAnchor(paragraph);
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
            if (!isNullOrEmpty(currentReference)) {
                if (currentReference.charAt(0) == '#') {
                    //Unlike Anchor, Action won't fail even when local destination does not exist
                    String refname = currentReference.substring(1); //getting rid of "#" at the begin of the reference
                    addGoToActionToChunk(refname, currentChunk);
                    //currentParagraph.add(currentChunk);
                    Anchor anchor = currentStyle.createAnchor();
                    anchor.add(currentChunk);
                    anchor.setName(refname + "_backlink");
                    currentParagraph.add(anchor);
                } else {
                    Anchor anchor = currentStyle.createAnchor();
                    anchor.add(currentChunk);
                    anchor.setReference(currentReference);
                    System.out.println("Adding A HREF=" + currentReference);
                    currentParagraph.add(anchor);
                }
            } else {
                currentParagraph.add(currentChunk);
            }
            currentChunk = null;
        }
    }

    private void processParagraphContent(org.w3c.dom.Element parent)
            throws DocumentException, FB2toPDFException {
        processParagraphContent(parent, false);
    }

    private void processParagraphContent(org.w3c.dom.Element parent, boolean bFirst)
            throws DocumentException, FB2toPDFException {
        boolean bFirstTextNode = true;
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                org.w3c.dom.Element child = (org.w3c.dom.Element) node;
                if (child.getTagName().equals("strong")) {
                    flushCurrentChunk();
                    currentStyle.toggleBold();
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleBold();
                } else if (child.getTagName().equals("emphasis")) {
                    flushCurrentChunk();
                    currentStyle.toggleItalic();
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleItalic();
                } else if (child.getTagName().equals("a")) {
                    flushCurrentChunk();
                    currentReference = child.getAttributeNS(NS_XLINK, "href");
                    if (currentReference.length() == 0) {
                        currentReference = child.getAttribute("href");
                    }
                    processParagraphContent(child);
                    flushCurrentChunk();
                    //if ("note".equals(child.getAttribute("type"))) {
                    //    addFootnote(currentReference);
                    //}
                    currentReference = null;
                } else if (child.getTagName().equals("cite")) {
                    flushCurrentChunk();
                    String citeId = child.getAttribute("id");
                    if (citeId.length() > 0) {
                        addInvisibleAnchor(citeId);
                    }
                    processParagraphContent(child);
                    flushCurrentChunk();
                    currentReference = null;
                } else if (child.getTagName().equals("style")) {
                    String styleName = child.getAttribute("name");
                    System.out.println("Style tag " + styleName + " ignored.");
                    processParagraphContent(child);
                } else if (child.getTagName().equals("image")) {
                    addImage(child);
                } else if (child.getTagName().equals("strikethrough")) {
                    flushCurrentChunk();
                    currentStyle.toggleStrikethrough();
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleStrikethrough();
                } else if (child.getTagName().equals("sup")) {
                    flushCurrentChunk();
                    currentStyle.toggleHalfSize();
                    superscript = true;
                    processParagraphContent(child, bFirst && bFirstTextNode);
                    bFirstTextNode = false;
                    flushCurrentChunk();
                    currentStyle.toggleHalfSize();
                    superscript = false;
                } else if (child.getTagName().equals("sub")) {
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
                    elif s.tagName == "strikethrough":
                    res += u'\\sout{' + par(s,intitle) + u'}'
                    elif s.tagName == "sub":
                    res += u'$_{\\textrm{' + par(s,intitle) + '}}$'
                    elif s.tagName == "sup":
                    res += u'$^{\\textrm{' + par(s,intitle) + '}}$'
                    elif s.tagName == "code":
                    res += u'{\\sc' + par(s,intitle) + u'}'
                    elif s.tagName == "image":
                    if not intitle:
                    res += processInlineImage(s)
                    else:
                    # TODO: nicer workaround for issue #44
                    res += "[...]"
                    elif s.tagName == "l":
                    logging.getLogger('fb2pdf').warning("Unsupported element: %s" % s.tagName)
                    res += "" #TODO
                    else:
                    logging.getLogger('fb2pdf').error("Unknown paragraph element: %s" % s.tagName)
                     */
                    System.out.println("Unhandled paragraph tag " + child.getTagName());
                    processParagraphContent(child);
                }
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getTextContent();
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

                currentChunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings()));
            }
        }
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
            throws DocumentException, IOException, FB2toPDFException, ParserConfigurationException, SAXException {
        translate(fromName, toName, null);
    }

    public static void translate(String fromName, String toName, InputStream stylesheet)
            throws DocumentException, IOException, FB2toPDFException, ParserConfigurationException, SAXException {
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
            e.printStackTrace();
        }
    }

    private String getLang(org.w3c.dom.Element description) throws FB2toPDFException {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null) {
            org.w3c.dom.Element lang = getOptionalChildByTagName(titleInfo, "lang");
            if (lang != null) {
                String langString = lang.getTextContent();
                System.out.println("Language of the FB2: " + langString);
                return langString;
            }
        }
        System.out.println("Language of the FB2 not found");
        return null;
    }

    private void setupHyphenation(org.w3c.dom.Element description) throws FB2toPDFException {
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
