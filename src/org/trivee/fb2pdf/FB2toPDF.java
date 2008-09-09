package org.trivee.fb2pdf;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import com.lowagie.text.DocumentException;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Anchor;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Chunk;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.Image;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.HyphenationAuto;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.commons.codec.binary.Base64;

public class FB2toPDF
{
    private String BASE_PATH = ".";

    private String fromName;
    private String toName;
    
    private FB2toPDF(String fromName, String toName)
    {
        this.fromName = fromName;
        this.toName   = toName;
    }

    private static class FontFamily
    {
        private BaseFont regular;
        private BaseFont bold;
        private BaseFont italic;
        private BaseFont bold_italic;

        public FontFamily(String regular, String bold, String italic, String boldItalic)
            throws DocumentException, IOException
        {
            this.regular     = BaseFont.createFont(regular,    "Cp1251", BaseFont.EMBEDDED);
            this.bold        = BaseFont.createFont(bold,       "Cp1251", BaseFont.EMBEDDED);
            this.italic      = BaseFont.createFont(italic,     "Cp1251", BaseFont.EMBEDDED);
            this.bold_italic = BaseFont.createFont(boldItalic, "Cp1251", BaseFont.EMBEDDED);
        }

        public BaseFont getRegular()    { return regular; }
        public BaseFont getBold()       { return bold; }
        public BaseFont getItalic()     { return italic; }
        public BaseFont getBoldItalic() { return bold_italic; }
    }

    private FontFamily arial;
    private FontFamily georgia;

    private HyphenationAuto hyphen_ru;

    private static final float MM_TO_POINTS = 72.0f / 25.4f;

    private static class Style
    {
        private float pageWidth;
        private float pageHeight;

        private float marginLeft;
        private float marginRight;
        private float marginTop;
        private float marginBottom;

        private float frontMatterFontSize;
        private float titleFontSize;
        private float subTitleFontSize;
        private float authorFontSize;
        private float annotationFontSize;
        private float contentsTitleFontSize;
        private float contentsItemFontSize;
        private float sectionTitleFontSize;
        private float subSectionTitleFontSize;
        private float subSubSectionTitleFontSize;
        private float bodyFontSize;
        private float poemFontSize;

        public Style(String propfile)
            throws IOException
        {
            Properties properties = new Properties();

            File file = new File(propfile); 
            InputStream in = new FileInputStream(file);
            properties.load(in); 
            in.close();

            pageWidth  = MM_TO_POINTS * Float.parseFloat(properties.getProperty("page.width",  "215.9"));
            pageHeight = MM_TO_POINTS * Float.parseFloat(properties.getProperty("page.height", "279.4"));

            marginLeft   = MM_TO_POINTS * Float.parseFloat(properties.getProperty("margin.left",   "25.4"));
            marginRight  = MM_TO_POINTS * Float.parseFloat(properties.getProperty("margin.right",  "25.4"));
            marginTop    = MM_TO_POINTS * Float.parseFloat(properties.getProperty("margin.top",    "25.4"));
            marginBottom = MM_TO_POINTS * Float.parseFloat(properties.getProperty("margin.bottom", "25.4"));

            frontMatterFontSize         = Float.parseFloat(properties.getProperty("frontMatter.fontSize",           "12.0"));
            titleFontSize               = Float.parseFloat(properties.getProperty("title.fontSize",                 "20.0"));
            subTitleFontSize            = Float.parseFloat(properties.getProperty("subTitle.fontSize",              "16.0"));
            authorFontSize              = Float.parseFloat(properties.getProperty("author.fontSize",                "18.0"));
            annotationFontSize          = Float.parseFloat(properties.getProperty("annotation.fontSize",            "12.0"));
            contentsTitleFontSize       = Float.parseFloat(properties.getProperty("contentsTitle.fontSize",         "20.0"));
            contentsItemFontSize        = Float.parseFloat(properties.getProperty("contentsItem.fontSize",          "16.0"));
            sectionTitleFontSize        = Float.parseFloat(properties.getProperty("sectionTitle.fontSize",          "20.0"));
            subSectionTitleFontSize     = Float.parseFloat(properties.getProperty("subSectionTitle.fontSize",       "16.0"));
            subSubSectionTitleFontSize  = Float.parseFloat(properties.getProperty("subSubSectionTitle.fontSize",    "14.0"));
            bodyFontSize                = Float.parseFloat(properties.getProperty("body.fontSize",                  "12.0"));
            poemFontSize                = Float.parseFloat(properties.getProperty("poem.fontSize",                  "12.0"));
        }

        public float getPageWidth()     { return pageWidth; }
        public float getPageHeight()    { return pageHeight; }

        public float getMarginLeft()    { return marginLeft; }
        public float getMarginRight()   { return marginRight; }
        public float getMarginTop()     { return marginTop; }
        public float getMarginBottom()  { return marginBottom; }

        public float getFrontMatterFontSize()           { return frontMatterFontSize; }
        public float getTitleFontSize()                 { return titleFontSize; }
        public float getSubTitleFontSize()              { return subTitleFontSize; }
        public float getAuthorFontSize()                { return authorFontSize; }
        public float getAnnotationFontSize()            { return annotationFontSize; }
        public float getContentsTitleFontSize()         { return contentsTitleFontSize; }
        public float getContentsItemFontSize()          { return contentsItemFontSize; }
        public float getSectionTitleFontSize()          { return sectionTitleFontSize; }
        public float getSubSectionTitleFontSize()       { return subSectionTitleFontSize; }
        public float getSubSubSectionTitleFontSize()    { return subSubSectionTitleFontSize; }
        public float getBodyFontSize()                  { return bodyFontSize; }
        public float getPoemFontSize()                  { return poemFontSize; }
    }

    private Style style;

    private void loadData()
        throws DocumentException, IOException
    {
        arial = new FontFamily(
            BASE_PATH + "/data/arial.ttf",
            BASE_PATH + "/data/arialbd.ttf",
            BASE_PATH + "/data/ariali.ttf",
            BASE_PATH + "/data/arialbi.ttf");

        georgia = new FontFamily(
            BASE_PATH + "/data/georgia.ttf",
            BASE_PATH + "/data/georgiab.ttf",
            BASE_PATH + "/data/georgiai.ttf",
            BASE_PATH + "/data/georgiaz.ttf");

        com.lowagie.text.pdf.hyphenation.Hyphenator.setHyphenDir(BASE_PATH + "/data");

        hyphen_ru = new HyphenationAuto("ru", "none", 2, 2);

        style = new Style(BASE_PATH + "/data/style.properties");
    }

    private org.w3c.dom.Document fb2;
    private com.lowagie.text.Document doc;
    private PdfWriter writer;

    private void readFB2()
        throws IOException, FileNotFoundException, ParserConfigurationException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setCoalescing(true);
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
            public void fatalError(org.xml.sax.SAXParseException e)
            {
                System.err.println("SAX fatal error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
            }
            public void error(org.xml.sax.SAXParseException e)
            {
                System.err.println("SAX fatal error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
            }
            public void warning(org.xml.sax.SAXParseException e)
            {
                System.err.println("SAX fatal error at line " + e.getLineNumber() + "#" + e.getColumnNumber() + ": " + e.getMessage());
            }
        });

        if (fromName.toLowerCase().endsWith(".fb2"))
            fb2 = builder.parse(new File(fromName));
        else if (fromName.toLowerCase().endsWith(".zip"))
        {
            ZipInputStream fromZip = new ZipInputStream(new FileInputStream(fromName));
            ZipEntry entry = fromZip.getNextEntry();
            if (entry.getName().toLowerCase().endsWith(".fb2"))
            {
                fb2 = builder.parse(fromZip);
            }
            else
            {
                System.err.println("First archive entry " + entry.getName() + " is not an FB2 file.");
                System.exit(-1);
            }
        }
        else
        {
            System.err.println("Unrecognized file extension: " + fromName + ", only FB2 or ZIP supported.");
            System.exit(-1);
        }
    }

    private void createPDFDoc()
        throws DocumentException, FileNotFoundException
    {
        Rectangle pageSize = new Rectangle(style.getPageWidth(), style.getPageHeight());
        doc = new com.lowagie.text.Document(pageSize,
            style.getMarginLeft(), style.getMarginRight(),
            style.getMarginTop(), style.getMarginBottom());

        writer = PdfWriter.getInstance(doc, new FileOutputStream(toName));

    }

    private void closePDF()
    {
        doc.close();
    }

    private static class NodeCollection implements NodeList
    {
        private Vector<Node> nodes = new Vector<Node>();
        public NodeCollection()         { }
        public void add(Node node)      { nodes.add(node); }
        public int getLength()          { return nodes.size(); }
        public Node item(int index)     { return nodes.elementAt(index); }
    }

    private static NodeList getChildElementsByTagName(org.w3c.dom.Element element, String tagName)
    {
        NodeCollection result = new NodeCollection();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            org.w3c.dom.Element child = (org.w3c.dom.Element)node;
            if (child.getTagName().equals(tagName))
                result.add(child);
        }

        return result;
    }

    private static NodeList getChildElements(org.w3c.dom.Element element)
    {
        NodeCollection result = new NodeCollection();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            org.w3c.dom.Element child = (org.w3c.dom.Element)node;
            result.add(child);
        }

        return result;
    }

    private static org.w3c.dom.Element getOnlyChildByTagName(org.w3c.dom.Element element, String tagName)
        throws FB2toPDFException
    {
        NodeList children = getChildElementsByTagName(element, tagName);
        if (children.getLength() == 0)
            throw new FB2toPDFException("Element not found: " + element.getTagName() + "/" + tagName);
        else if (children.getLength() > 1)
            throw new FB2toPDFException("More than one element found: " + element.getTagName() + "/" + tagName);
        return (org.w3c.dom.Element)children.item(0);
    }

    private static org.w3c.dom.Element getOptionalChildByTagName(org.w3c.dom.Element element, String tagName)
        throws FB2toPDFException
    {
        NodeList children = getChildElementsByTagName(element, tagName);
        if (children.getLength() == 0)
            return null;
        else if (children.getLength() > 1)
            throw new FB2toPDFException("More than one element found: " + element.getTagName() + "/" + tagName);
        return (org.w3c.dom.Element)children.item(0);
    }

    private static String fixCharacters(String text)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            if (c == '\u00A0')
                c = ' ';
            sb.append(c);
        }
        return sb.toString();
    }

    private static String removeExtraWhitespace(String text, boolean startWithSpace)
    {
        // normalize whitespace
        StringTokenizer st = new StringTokenizer(text);
        StringBuffer sb = new StringBuffer();
        boolean bFirst = !startWithSpace;
        while (st.hasMoreTokens())
        {
            if (!bFirst)
                sb.append(" ");
            bFirst = false;
            sb.append(st.nextToken());
        }
        return sb.toString();
    }

    private static String getTextContentByTagName(org.w3c.dom.Element element, String tagName, String prefix, String suffix)
    {
        // collect text content
        NodeList children = getChildElementsByTagName(element, tagName);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < children.getLength(); ++i)
        {
            String content = children.item(i).getTextContent();
            if (sb.length() == 0 && content.length() > 0 && prefix != null)
                sb.append(prefix);
            sb.append(content);
        }
        if (sb.length() > 0 && suffix != null)
            sb.append(suffix);
        return removeExtraWhitespace(fixCharacters(sb.toString()), prefix != null && prefix.length() > 0 && prefix.charAt(0) == ' ');
    }

    private static String getTextContentByTagName(org.w3c.dom.Element element, String tagName, String prefix)
    {
        return getTextContentByTagName(element, tagName, prefix, null);
    }

    private static String getTextContentByTagName(org.w3c.dom.Element element, String tagName)
    {
        return getTextContentByTagName(element, tagName, null, null);
    }

    private void run()
        throws IOException, DocumentException, FB2toPDFException, ParserConfigurationException, SAXException
    {
        loadData();

        readFB2();
        createPDFDoc();

        org.w3c.dom.Element root = fb2.getDocumentElement();
        if (!root.getTagName().equals("FictionBook"))
            throw new FB2toPDFException("The file does not seems to contain 'fictionbook' root element");

    
        extractBinaries(root);
                // findEnclosures(fb, outdir, outname)

        org.w3c.dom.Element description = getOptionalChildByTagName(root, "description");
        if (description != null)
        {
            addMetaInfo(description);
            doc.open();
            processDescription(description);
        }
        else
        {
            doc.open();
            System.err.println("Description not found");
        }


        NodeList bodies = getChildElementsByTagName(root, "body");
        if (bodies.getLength() == 0)
            throw new FB2toPDFException("Element not found: FictionBook/body");

        org.w3c.dom.Element body = (org.w3c.dom.Element)bodies.item(0);
        makeTOCPage(body);

        for (int i = 0; i < bodies.getLength(); ++i)
        {
            body = (org.w3c.dom.Element)bodies.item(i);
            bodyIndex = i;
            processBody(body);
        }

        closePDF();        
    }

    int bodyIndex;

    private static class BinaryAttachment
    {
        private String href;
        private String contentType;
        private byte[] data;

        public BinaryAttachment(org.w3c.dom.Element binary)
        {
            this.href        = "#" + binary.getAttribute("id");
            this.contentType = binary.getAttribute("content-type");
            this.data        = Base64.decodeBase64(binary.getTextContent().getBytes());

            System.err.println("Loaded binary " + this.href + " (" + this.contentType + ")");
        }

        public String getHREF()
        {
            return href;
        }

        public String getContentType()
        {
            return contentType;
        }

        public byte[] getData()
        {
            return data;
        }
    };

    private Vector<BinaryAttachment> attachments = new Vector<BinaryAttachment>();

    private void extractBinaries(org.w3c.dom.Element root)
    {
        NodeList binaries = getChildElementsByTagName(root, "binary");
        for (int i = 0; i < binaries.getLength(); ++i)
        {
            org.w3c.dom.Element binary = (org.w3c.dom.Element)binaries.item(i);
            attachments.add(new BinaryAttachment(binary));
        }
    }

    private static class FontManager
    {
        private FontFamily family;
        private float      size;
        private boolean    bold;
        private boolean    italic;

        public FontManager(FontFamily family, float size)
        {
            this.family = family;
            this.size   = size;
            this.bold   = false;
            this.italic = false;
        }

        public Font createFont()
        {
            if (bold)
            {
                if (italic)
                    return new Font(family.getBoldItalic(), size);
                else
                    return new Font(family.getBold(), size);
            }
            else
            {
                if (italic)
                    return new Font(family.getItalic(), size);
                else
                    return new Font(family.getRegular(), size);
            }
        }

        public float getSize()
        {
            return size;
        }

        public void toggleBold()
        {
            bold = !bold;
        }

        public void toggleItalic()
        {
            italic = !italic;
        }
    }

    private FontManager currentFonts;

    private void processDescription(org.w3c.dom.Element description)
        throws FB2toPDFException, DocumentException
    {
        makeCoverPage(description);
        makeBookInfoPage(description);
    }

    private void addLine(String text, BaseFont bf, float size, float before, float after, int alignment)
        throws DocumentException
    {
        Font font = new Font(bf, size);

        Chunk chunk = new Chunk();
        chunk.setFont(font);
        chunk.append(text);

        Paragraph para = new Paragraph(size);
        para.setAlignment(alignment);
        para.setSpacingBefore(before);
        para.setSpacingAfter(after);

        para.add(chunk);
        doc.add(para);
    }

    private void addTitleLine(String text, BaseFont bf, float size, float before, float after)
        throws DocumentException
    {
        addLine(text, bf, size, before, after, Paragraph.ALIGN_CENTER);
    }

    private static final String NS_XLINK = "http://www.w3.org/1999/xlink";

    private void makeCoverPage(org.w3c.dom.Element description)
        throws FB2toPDFException, DocumentException
    {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo == null)
        {
            System.err.println("Title info not found");
            return;
        }

        org.w3c.dom.Element coverPage = getOptionalChildByTagName(titleInfo, "coverpage");
        if (coverPage != null)
        {
            NodeList images = getChildElementsByTagName(coverPage, "image");
            for (int i = 0; i < images.getLength(); ++i)
            {
                org.w3c.dom.Element coverImage = (org.w3c.dom.Element)images.item(i);

                String href = coverImage.getAttributeNS(NS_XLINK, "href");
                Image image = getImage(href);
                if (image != null)
                {
                    Rectangle pageSize = doc.getPageSize();
                    image.scaleToFit(
                        pageSize.getWidth() - doc.leftMargin() - doc.rightMargin(),
                        pageSize.getHeight() - doc.topMargin() - doc.bottomMargin());
                    image.setAlignment(Image.MIDDLE);
                    doc.add(image);
                    doc.newPage();
                }
            }
        }

    }
    
    private String getAuthorFullName(org.w3c.dom.Element author) throws FB2toPDFException
    {
        String firstName = getTextContentByTagName(author, "first-name");
        String middleName = getTextContentByTagName(author, "middle-name");
        String lastName = getTextContentByTagName(author, "last-name");
        return String.format("%s %s %s", firstName, middleName, lastName);
    }
    
    private void addMetaInfo(org.w3c.dom.Element description)
        throws FB2toPDFException, DocumentException
    {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null)
        {
            NodeList authors = getChildElementsByTagName(titleInfo, "author");
            for (int i = 0; i < authors.getLength(); ++i)
            {
                org.w3c.dom.Element author = (org.w3c.dom.Element)authors.item(i);
                String authorName = getAuthorFullName(author);
                System.out.println("Adding author: " + transliterate(authorName));
                doc.addAuthor(transliterate(authorName));
            }

            org.w3c.dom.Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            org.w3c.dom.Element sequence = getOptionalChildByTagName(titleInfo, "sequence");

            if (bookTitle != null && sequence == null)
            {
                String titleString = bookTitle.getTextContent();
                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
            }
            else if (bookTitle != null && sequence != null)
            {
                String titleString = bookTitle.getTextContent();
                String subtitle = "(" + sequence.getAttribute("name") + " #" + sequence.getAttribute("number") + ")";

                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
                doc.addTitle(transliterate(subtitle));
            }
        }
    }

    private void makeBookInfoPage(org.w3c.dom.Element description)
        throws FB2toPDFException, DocumentException
    {
        org.w3c.dom.Element publishInfo = getOptionalChildByTagName(description, "publish-info");
        if (publishInfo != null)
        {
            addLine(
                getTextContentByTagName(publishInfo, "book-name", null, " // ") +
                getTextContentByTagName(publishInfo, "publisher") +
                getTextContentByTagName(publishInfo, "city", ", ")+
                getTextContentByTagName(publishInfo, "year", ", " ),
                    arial.getRegular(), style.getFrontMatterFontSize(), 0, 0, Paragraph.ALIGN_LEFT);
            addLine(
                "ISBN: " + getTextContentByTagName(publishInfo, "isbn"),
                    arial.getRegular(), style.getFrontMatterFontSize(), 0, 0, Paragraph.ALIGN_LEFT);
        }

        org.w3c.dom.Element documentInfo = getOptionalChildByTagName(description, "document-info");
        NodeList documentAuthors = null;
        if (documentInfo != null)
            documentAuthors = getChildElementsByTagName(documentInfo, "author");
        for (int i = 0; documentAuthors != null && i < documentAuthors.getLength(); ++i)
        {
            org.w3c.dom.Element documentAuthor = (org.w3c.dom.Element)documentAuthors.item(i);
            addLine(
                "FB2: " +
                getTextContentByTagName(documentAuthor, "first-name", " ") +
                getTextContentByTagName(documentAuthor, "last-name", " ") +
                getTextContentByTagName(documentAuthor, "nickname", " \u201C", "\u201D") +
                getTextContentByTagName(documentAuthor, "email", " <", ">") +
                getTextContentByTagName(documentInfo, "date", ", ") +
                getTextContentByTagName(documentInfo, "version", ", version "),
                    arial.getRegular(), style.getFrontMatterFontSize(), 0, 0, Paragraph.ALIGN_LEFT);
        }
        if (documentInfo != null)
        {
            addLine(
                "UUID: " + getTextContentByTagName(documentInfo, "id"),
                    arial.getRegular(), style.getFrontMatterFontSize(), 0, 0, Paragraph.ALIGN_LEFT);
        }

        addLine("PDF: org.trivee.fb2pdf.FB2toPDF 1.0, " + 
            java.text.DateFormat.getDateInstance().format(new java.util.Date()),
                    arial.getRegular(), style.getFrontMatterFontSize(), 0, 0, Paragraph.ALIGN_LEFT);


        addTitleLine(" ", arial.getRegular(), style.getTitleFontSize(), 0, 0);

        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null)
        {
            NodeList authors = getChildElementsByTagName(titleInfo, "author");
            for (int i = 0; i < authors.getLength(); ++i)
            {
                org.w3c.dom.Element author = (org.w3c.dom.Element)authors.item(i);
                String authorName = getAuthorFullName(author);
                // doc.addAuthor(transliterate(authorName));
                addTitleLine(authorName, arial.getRegular(), style.getAuthorFontSize(), 0, 0);
            }

            org.w3c.dom.Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            org.w3c.dom.Element sequence = getOptionalChildByTagName(titleInfo, "sequence");

            if (bookTitle != null && sequence == null)
            {
                String titleString = bookTitle.getTextContent();
                // doc.addTitle(transliterate(titleString));
                addTitleLine(titleString, arial.getRegular(),
                    style.getTitleFontSize(), style.getTitleFontSize(), style.getTitleFontSize());
            }
            else if (bookTitle != null && sequence != null)
            {
                String subtitle = "(" + sequence.getAttribute("name") + " #" + sequence.getAttribute("number") + ")";

                addTitleLine(bookTitle.getTextContent(), arial.getRegular(),
                    style.getTitleFontSize(), style.getTitleFontSize(), 0);
                addTitleLine(subtitle, arial.getRegular(),
                    style.getSubTitleFontSize(), 0, style.getTitleFontSize());
            }

            org.w3c.dom.Element annotation = getOptionalChildByTagName(titleInfo, "annotation");
            if (annotation != null)
            {

                currentFonts = new FontManager(arial, style.getAnnotationFontSize());
                currentFonts.toggleItalic();
                processSectionContent(annotation, PARAGRAPH_ANNOTATION, 0);
                currentFonts = null;
            }
        }

        doc.newPage();
    }

    private Image getImage(String href)
    {
        System.err.println("Loading image at " + href);
        for (int i = 0; i < attachments.size(); ++i)
        {
            BinaryAttachment attachment = attachments.elementAt(i);
            if (!attachment.getHREF().equals(href))
                continue;
            try
            {
                return Image.getInstance(attachment.getData());
            }
            catch (BadElementException e)
            {
                e.printStackTrace();
                return null;
            }
            catch (java.net.MalformedURLException e1)
            {
                e1.printStackTrace();
                return null;
            }
            catch (IOException e2)
            {
                e2.printStackTrace();
                return null;
            }
        }

        return null;
    }
    
    private void processBody(org.w3c.dom.Element body)
        throws DocumentException
    {
        // XXX TODO processEpigraphs(body);

        currentFonts = new FontManager(georgia, style.getBodyFontSize());
        processSections(body);
        currentFonts = null;
    }

    private void makeTOCPage(org.w3c.dom.Element body)
        throws DocumentException
    {
        NodeList sections = getChildElementsByTagName(body, "section");
        if (sections.getLength() <= 1)
            return;

        addTitleLine("Содержание", arial.getRegular(),
            style.getContentsTitleFontSize(), 0, style.getContentsTitleFontSize());

        for (int i = 0; i < sections.getLength(); ++i)
        {
            org.w3c.dom.Element section = (org.w3c.dom.Element)sections.item(i);

            String title = getTextContentByTagName(section, "title");
            if (title.length() == 0)
                title = "#" + (i+1);

            float em = style.getContentsItemFontSize();

            Font font = new Font(arial.getBold(), em);

            Chunk chunk = new Chunk();
            chunk.setFont(font);
            chunk.append(title);


            Anchor anchor = new Anchor(chunk);
            String ref = section.getAttribute("id");
            if(ref.isEmpty())
                ref = "section" + i;
            anchor.setReference("#" + ref);
            System.err.println("Adding A HREF=#" + ref);

            Paragraph para = new Paragraph(em);
            para.setAlignment(Paragraph.ALIGN_LEFT);
            para.setIndentationLeft(5.0f * em);
            para.setFirstLineIndent(-2.5f * em);
            para.add(anchor);

            doc.add(para);
        }

        doc.newPage();
    }

    private void processSections(org.w3c.dom.Element body)
        throws DocumentException
    {
        NodeList sections = getChildElementsByTagName(body, "section");
        for (int i = 0; i < sections.getLength(); ++i)
        {
            org.w3c.dom.Element section = (org.w3c.dom.Element)sections.item(i);
            // XXX TODO if s.getAttribute('id') not in notes],'')
            processSection(section, 0, i);
        }

    }

    private void processSection(org.w3c.dom.Element section, int level, int index)
        throws DocumentException
    {
        if (level == 0)
            doc.newPage();
        else if (level == 1)
        {
            if (writer.getVerticalPosition(false) < doc.getPageSize().getHeight() * 0.5f)
                doc.newPage();
        }

        String id = section.getAttribute("id");

        if (id.length() == 0 && bodyIndex == 0 && level == 0)
            id = "section" + index;

        if (id.length() > 0)
        {
            Anchor anchor = new Anchor(".", new Font(arial.getRegular(), 0.01f));
            anchor.setName(id);
            doc.add(anchor);
            System.err.println("Adding A NAME=" + id);
        }

/* XXX TODO
    res = u''
    if level!=-1:
        pid=s.getAttribute('id')
        if pid:
            res+='\\hypertarget{%s}{}\n' % pid

        t = find(s,"title")
        if t:
            title = getSectionTitle(t)
        else:
            title = ""

        if level>=len(section_commands):
            cmd = "section"
        else:
            cmd = section_commands[level]

        res+="\n\\%s{%s}\n" % (cmd,_tocElement(title, t))
        res+=processEpigraphs(s)
*/

        processSectionContent(section, PARAGRAPH_TEXT, level);
    }

    private void processSectionContent(org.w3c.dom.Element parent, int paragraphType, int level)
        throws DocumentException
    {
        boolean bFirst = true;

        NodeList nodes = getChildElements(parent);
        int subsectionIndex = 0;
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = (org.w3c.dom.Element)nodes.item(i);

            if (element.getTagName().equals("section"))
            {
                processSection(element, level+1, subsectionIndex);
                subsectionIndex++;
            }
            else if (element.getTagName().equals("p"))
            {
                /* XXX TODO
                pid=x.getAttribute('id')
                if pid:
                    res+='\\hypertarget{%s}{}\n' % pid
                */
                processParagraph(element, paragraphType, bFirst, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("empty-line"))
            {
                // XXX TODO res+="\\vspace{12pt}\n\n"
                Chunk chunk = new Chunk(" ", currentFonts.createFont());
                Paragraph p = new Paragraph(chunk);
                doc.add(p);
                // System.out.println("Unhandled section tag " + element.getTagName() );
            }
            else if (element.getTagName().equals("image"))
            {
                String href = element.getAttributeNS(NS_XLINK, "href");
                Image image = getImage(href);
                if (image != null)
                {
                    Rectangle pageSize = doc.getPageSize();
                    image.scaleToFit(
                        (pageSize.getWidth() - doc.leftMargin() - doc.rightMargin()) / 2.0f,
                        (pageSize.getHeight() - doc.topMargin() - doc.bottomMargin()) / 2.0f);
                    image.setAlignment(Image.MIDDLE);
                    doc.add(image);
                }
                else
                {
                    System.out.println("Image not found, href: " + href);
                }
            }
            else if (element.getTagName().equals("poem"))
            {
                processPoem(element);
            }
            else if (element.getTagName().equals("subtitle"))
            {
                // XXX TODO res+="\\subsection{%s}\n" % _tocElement(par(x, True), x)
                if (level == 0)
                {
                    if (writer.getVerticalPosition(false) < doc.getPageSize().getHeight() * 0.5f)
                        doc.newPage();
                }
                processParagraph(element, PARAGRAPH_SUBTITLE, true, true);
            }
            else if (element.getTagName().equals("cite"))
            {
                processCite(element);
            }
            else if (element.getTagName().equals("table"))
            {
                // XXX TODO logging.getLogger('fb2pdf').warning("Unsupported element: %s" % x.tagName)
                // XXX TODO pass # TODO
                System.out.println("Unhandled section tag " + element.getTagName() );
            }
            else if (element.getTagName().equals("title"))
            {
                processTitle(element, level);
            }
            else if (element.getTagName().equals("epigraph"))
            {
                processEpigraph(element);
            }
            else
            {
                // XXX TODO logging.getLogger('fb2pdf').error("Unknown section element: %s" % x.tagName)
                System.out.println("Unhandled section tag " + element.getTagName() );
            }

            bFirst = false;
        }
    }

    private void processTitle(org.w3c.dom.Element title, int level)
        throws DocumentException
    {
        float em = currentFonts.getSize();
        FontManager previousFonts = currentFonts;

        if (level == 0)
        {
            currentFonts = new FontManager(arial, style.getSectionTitleFontSize());
            currentFonts.toggleBold();
        }
        else if (level == 1)
        {
            currentFonts = new FontManager(arial, style.getSubSectionTitleFontSize());
        }
        else
        {
            currentFonts = new FontManager(arial, style.getSubSubSectionTitleFontSize());
            currentFonts.toggleBold();
        }

        NodeList nodes = getChildElements(title);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = (org.w3c.dom.Element)nodes.item(i);

            if (element.getTagName().equals("p"))
            {
                /* XXX TODO
                pid=x.getAttribute('id')
                if pid:
                    res+='\\hypertarget{%s}{}\n' % pid
                */
                processParagraph(element, PARAGRAPH_TITLE, i == 0, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("empty-line"))
            {
                // XXX TODO res+="\\vspace{12pt}\n\n"
                Chunk chunk = new Chunk(" ", currentFonts.createFont());
                Paragraph p = new Paragraph(chunk);
                doc.add(p);
                // System.out.println("Unhandled section tag " + element.getTagName() );
            }
            else
            {
                System.out.println("Unhandled title tag " + element.getTagName() );
            }
        }

        currentFonts = previousFonts;
    }

    private void processEpigraph(org.w3c.dom.Element epigraph)
        throws DocumentException
    {
        FontManager previousFonts = currentFonts;
        currentFonts = new FontManager(georgia, style.getPoemFontSize());
        currentFonts.toggleItalic();

        NodeList nodes = getChildElements(epigraph);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = (org.w3c.dom.Element)nodes.item(i);

            if (element.getTagName().equals("p"))
            {
                processParagraph(element, PARAGRAPH_VERSE, i == 0, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("poem"))
            {
                processPoem(element);
            }
            else if (element.getTagName().equals("text-author"))
            {
                currentFonts.toggleItalic();
                processParagraph(element, PARAGRAPH_VERSE, true, true);
                currentFonts.toggleItalic();
            }
            else if (element.getTagName().equals("empty-line"))
            {
                Chunk chunk = new Chunk(" ", currentFonts.createFont());
                Paragraph p = new Paragraph(chunk);
                doc.add(p);
            }
            else
            {
                System.out.println("Unhandled epigraph tag " + element.getTagName() );
            }
        }

        currentFonts = previousFonts;
    }

    private void processCite(org.w3c.dom.Element cite)
        throws DocumentException
    {
        FontManager previousFonts = currentFonts;
        currentFonts = new FontManager(georgia, style.getPoemFontSize());
        currentFonts.toggleItalic();

        NodeList nodes = getChildElements(cite);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = (org.w3c.dom.Element)nodes.item(i);

            if (element.getTagName().equals("p"))
            {
                processParagraph(element, PARAGRAPH_VERSE, i == 0, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("text-author"))
            {
                currentFonts.toggleItalic();
                processParagraph(element, PARAGRAPH_VERSE, true, true);
                currentFonts.toggleItalic();
            }
            else if (element.getTagName().equals("empty-line"))
            {
                Chunk chunk = new Chunk(" ", currentFonts.createFont());
                Paragraph p = new Paragraph(chunk);
                doc.add(p);
            }
            else
            {
                System.out.println("Unhandled cite tag " + element.getTagName() );
            }
        }

        currentFonts = previousFonts;
    }

    private void processPoem(org.w3c.dom.Element poem)
        throws DocumentException
    {
        FontManager previousFonts = currentFonts;
        currentFonts = new FontManager(georgia, style.getPoemFontSize());
        currentFonts.toggleItalic();

        NodeList nodes = getChildElements(poem);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = (org.w3c.dom.Element)nodes.item(i);

            if (element.getTagName().equals("stanza"))
            {
                processStanza(element);
            }
            else
            {
                System.out.println("Unhandled poem tag " + element.getTagName() );
            }
        }

        currentFonts = previousFonts;
    }

    private void processStanza(org.w3c.dom.Element stanza)
        throws DocumentException
    {
        NodeList nodes = getChildElements(stanza);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = (org.w3c.dom.Element)nodes.item(i);

            if (element.getTagName().equals("v"))
            {
                processParagraph(element, PARAGRAPH_VERSE, i == 0, i == nodes.getLength()-1);
            }
            else
            {
                System.out.println("Unhandled stanza tag " + element.getTagName() );
            }
        }
    }

    private static final int PARAGRAPH_TEXT  = 0;
    private static final int PARAGRAPH_TITLE = 1;
    private static final int PARAGRAPH_VERSE = 2;
    private static final int PARAGRAPH_ANNOTATION = 3;
    private static final int PARAGRAPH_SUBTITLE = 4;

    private Paragraph currentParagraph;
    private String    currentReference;
    private Chunk     currentChunk;

    private void processParagraph(org.w3c.dom.Element paragraph, int type,
                                  boolean bFirst, boolean bLast)
        throws DocumentException
    {
        float em = currentFonts.getSize();

        currentParagraph = new Paragraph(em);
        if (type == PARAGRAPH_TITLE)
        {
            currentParagraph.setAlignment(Paragraph.ALIGN_LEFT);
            if (bFirst)
                currentParagraph.setSpacingBefore(0.6f * em);
            else
                currentParagraph.setSpacingBefore(0.3f * em);
            if (bLast)
                currentParagraph.setSpacingAfter(0.6f * em);
        }
        else if (type == PARAGRAPH_VERSE)
        {
            currentParagraph.setAlignment(Paragraph.ALIGN_LEFT);
            currentParagraph.setIndentationLeft(5.0f * em);
            currentParagraph.setFirstLineIndent(-2.5f * em);
            if (bFirst)
                currentParagraph.setSpacingBefore(0.5f * em);
            if (bLast)
                currentParagraph.setSpacingAfter(0.5f * em);
        }
        else if (type == PARAGRAPH_ANNOTATION)
        {
            currentParagraph.setAlignment(Paragraph.ALIGN_JUSTIFIED);
            if (bFirst)
                currentParagraph.setSpacingBefore(0.3f * em);
            currentParagraph.setSpacingAfter(0.3f * em);
        }
        else if (type == PARAGRAPH_SUBTITLE)
        {
            currentParagraph.setAlignment(Paragraph.ALIGN_CENTER);
            currentParagraph.setSpacingBefore(0.3f * em);
            currentParagraph.setSpacingAfter(0.3f * em);
            currentFonts.toggleBold();
        }
        else
        {
            currentParagraph.setAlignment(Paragraph.ALIGN_JUSTIFIED);
            currentParagraph.setFirstLineIndent(2.5f * em);
        }

        processParagraphContent(paragraph, type);
        flushCurrentChunk();

        doc.add(currentParagraph);
        currentParagraph = null;
        currentReference = null;

        if (type == PARAGRAPH_SUBTITLE)
        {
            currentFonts.toggleBold();
        }
    }

    private void flushCurrentChunk()
        throws DocumentException
    {
        if (currentChunk != null)
        {
            if (currentReference != null)
            {
                Anchor anchor = new Anchor(currentChunk);
                anchor.setReference(currentReference);
                System.err.println("Adding A HREF=" + currentReference);
                currentParagraph.add(anchor);
            }
            else
                currentParagraph.add(currentChunk);
            currentChunk = null;
        }
    }

    private void processParagraphContent(org.w3c.dom.Element parent, int type)
        throws DocumentException
    {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                org.w3c.dom.Element child = (org.w3c.dom.Element)node;
                if (child.getTagName().equals("strong"))
                {
                    flushCurrentChunk();
                    currentFonts.toggleBold();
                    processParagraphContent(child, type);
                    flushCurrentChunk();
                    currentFonts.toggleBold();
                }
                else if (child.getTagName().equals("emphasis"))
                {
                    flushCurrentChunk();
                    currentFonts.toggleItalic();
                    processParagraphContent(child, type);
                    flushCurrentChunk();
                    currentFonts.toggleItalic();
                }
                else if (child.getTagName().equals("a"))
                {
                    flushCurrentChunk();
                    currentReference = child.getAttributeNS(NS_XLINK, "href");
                    if (currentReference.length() == 0)
                        currentReference = child.getAttribute("href");
                    processParagraphContent(child, type);
                    flushCurrentChunk();
                    currentReference = null;
                }
                else if (child.getTagName().equals("cite"))
                {
                    flushCurrentChunk();
                    String citeId = child.getAttribute("id");
                    if (citeId.length() > 0)
                    {
                        Anchor anchor = new Anchor(".", new Font(arial.getRegular(), 0.01f));
                        anchor.setName(citeId);
                        currentParagraph.add(anchor);
                        System.err.println("Adding A NAME=" + citeId);
                    }
                    processParagraphContent(child, type);
                    flushCurrentChunk();
                    currentReference = null;
                }
                else if (child.getTagName().equals("style"))
                {
                    String styleName = child.getAttribute("name");
                    System.out.println("Style tag " + styleName + " ignored.");
                    processParagraphContent(child, type);
                }
                else {
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
                    System.out.println("Unhandled paragraph tag " + child.getTagName() );
                    processParagraphContent(child, type);
                }
            }
            else if (node.getNodeType() == Node.TEXT_NODE)
            {
                // XXX TODO res += _textQuote(s.data)
                if (currentChunk == null)
                {
                    currentChunk = new Chunk();
                    currentChunk.setFont(currentFonts.createFont());
                    if (type == PARAGRAPH_TEXT)
                        currentChunk.setHyphenation(hyphen_ru);
                }

                String text = node.getTextContent();
                currentChunk.append(fixCharacters(text));
            }
        }
    }

    private static final String[][] TRANSTABLE = {
        // верхний регистр
        // трехбуквенные замены
        { "\u0429", "SCH" },
        // двухбуквенные замены
        { "\u0401", "YO" },
        { "\u0416", "ZH" },
        { "\u0426", "TS" },
        { "\u0427", "CH" },
        { "\u0428", "SH" },
        { "\u042E", "YU" },
        { "\u042F", "YA" },
        // однобуквенные замены
        { "\u0410", "A" },
        { "\u0411", "B" },
        { "\u0412", "V" },
        { "\u0413", "G" },
        { "\u0414", "D" },
        { "\u0415", "E" },
        { "\u0417", "Z" },
        { "\u0418", "I" },
        { "\u0419", "J" },
        { "\u041A", "K" },
        { "\u041B", "L" },
        { "\u041C", "M" },
        { "\u041D", "N" },
        { "\u041E", "O" },
        { "\u041F", "P" },
        { "\u0420", "R" },
        { "\u0421", "S" },
        { "\u0422", "T" },
        { "\u0423", "U" },
        { "\u0424", "F" },
        { "\u0425", "H" },
        { "\u042D", "E" },
        { "\u042B", "Y" },
        { "\u042C", "`" },
        { "\u042A", "'" },
        // нижний регистр
        // трехбуквенные замены
        { "\u0449", "sch" },
        // двухбуквенные замены
        { "\u0451", "yo" },
        { "\u0436", "zh" },
        { "\u0446", "ts" },
        { "\u0447", "ch" },
        { "\u0448", "sh" },
        { "\u044E", "yu" },
        { "\u044F", "ya" },
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
        {"\u0490", "G"},
    };

    private static String transliterate(String text)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            for (int j = 0; j < TRANSTABLE.length; ++j)
            {
                if (c != TRANSTABLE[j][0].charAt(0))
                    continue;
                sb.append(TRANSTABLE[j][1]);
                c = 0;
                break;
            }
            if (c != 0)
                sb.append(c);
        }
        return sb.toString();
    }

    public static void translate(String fromName, String toName)
         throws DocumentException, IOException, FB2toPDFException, ParserConfigurationException, SAXException
    {
        new FB2toPDF(fromName, toName).run();
    }

    public static void main(String [] args) 
    {
        try
        {
            if(args.length < 2)
            {
                System.err.println("Usage: java " + FB2toPDF.class.getName() + " <input.fb2> <output.pdf>");
                return;
            }
            translate(args[0], args[1]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
