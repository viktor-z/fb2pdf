package org.trivee.fb2pdf;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
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
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Image;

import com.itextpdf.text.pdf.HyphenationAuto;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfAction;

import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfOutline;
import java.awt.Color;
import java.awt.Toolkit;
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

    private HyphenationAuto hyphenation;

    private Stylesheet stylesheet;

    private boolean isIgnoreEmptyLine(Element element) {

        Node nextSib = element.getNextSibling();
        Node prevSib = element.getPreviousSibling();
        boolean ignore = false;
        if (nextSib != null && nextSib.getNodeName().equalsIgnoreCase("image") &&
                stylesheet.getGeneralSettings().ignoreEmptyLineBeforeImage) {
            System.out.println("Skipping empty line before image");
            ignore = true;
        }
        if (prevSib != null && prevSib.getNodeName().equalsIgnoreCase("image") &&
                stylesheet.getGeneralSettings().ignoreEmptyLineAfterImage) {
            System.out.println("Skipping empty line after image");
            ignore = true;
        }

        return ignore;
    }

    private void addImage(Element element) throws DocumentException, DOMException {
        String href = element.getAttributeNS(NS_XLINK, "href");
        Image image = getImage(href);
        if (image != null) {
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
        } else {
            System.out.println("Image not found, href: " + href);
        }
    }

    private String getSequenceSubtitle(Element seq)
    {
        String seqname = seq.getAttribute("name");
        String seqnumber = seq.getAttribute("number");
        String subtitle = "";
        if (!isNullOrEmpty(seqname))
            subtitle += seqname;
        if (!isNullOrEmpty(seqnumber))
            subtitle += " #" + seqnumber;
        if (!isNullOrEmpty(subtitle))
            subtitle = "(" + subtitle + ")";
        return subtitle;
    }

    private boolean isNullOrEmpty(String str)
    {
        return str == null || str.trim().length() == 0;
    }

    private void loadData(InputStream stylesheetInputStream)
        throws DocumentException, IOException, FB2toPDFException
    {
        if (stylesheetInputStream == null)
            stylesheet = Stylesheet.readStylesheet(BASE_PATH + "/data/stylesheet.json");
        else
            stylesheet = Stylesheet.readStylesheet(stylesheetInputStream);
    }

    private org.w3c.dom.Document fb2;
    private com.itextpdf.text.Document doc;
    private PdfWriter writer;

    private void readFB2()
        throws IOException, FileNotFoundException, ParserConfigurationException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setCoalescing(true);
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setAttribute("http://apache.org/xml/features/continue-after-fatal-error", new Boolean(true));

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
        PageStyle ps = stylesheet.getPageStyle();

        Rectangle pageSize = new Rectangle(ps.getPageWidth(), ps.getPageHeight());

        doc = new com.itextpdf.text.Document(pageSize,
            ps.getMarginLeft(), ps.getMarginRight(),
            ps.getMarginTop(), ps.getMarginBottom());

        writer = PdfWriter.getInstance(doc, new FileOutputStream(toName));

    }

    private void closePDF()
    {
        doc.close();
    }

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

    private static org.w3c.dom.Element getOptionalChildByTagName(org.w3c.dom.Element element, String tagName)
        throws FB2toPDFException
    {
        ElementCollection children = ElementCollection.childrenByTagName(element, tagName);
        if (children.getLength() == 0)
            return null;
        else if (children.getLength() > 1)
            throw new FB2toPDFException("More than one element found: " + element.getTagName() + "/" + tagName);
        return children.item(0);
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
        ElementCollection children = ElementCollection.childrenByTagName(element, tagName);
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

    private void run(InputStream stylesheetInputStream)
        throws IOException, DocumentException, FB2toPDFException, ParserConfigurationException, SAXException
    {
        loadData(stylesheetInputStream);
 
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
            setupHyphenation(description);
            addMetaInfo(description);
            doc.open();
            processDescription(description);
        }
        else
        {
            doc.open();
            System.err.println("Description not found");
        }


        ElementCollection bodies = ElementCollection.childrenByTagName(root, "body");
        if (bodies.getLength() == 0)
            throw new FB2toPDFException("Element not found: FictionBook/body");

        org.w3c.dom.Element body = bodies.item(0);
        makeTOCPage(body);

        for (int i = 0; i < bodies.getLength(); ++i)
        {
            body = (org.w3c.dom.Element)bodies.item(i);
            bodyIndex = i;
            processBody(body);
            doc.newPage();
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

            System.out.println("Loaded binary " + this.href + " (" + this.contentType + ")");
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
        ElementCollection binaries = ElementCollection.childrenByTagName(root, "binary");
        for (int i = 0; i < binaries.getLength(); ++i)
        {
            org.w3c.dom.Element binary = binaries.item(i);
            attachments.add(new BinaryAttachment(binary));
        }
    }

    private ParagraphStyle currentStyle;
    private PdfOutline currentOutline;

    private void processDescription(org.w3c.dom.Element description)
        throws FB2toPDFException, DocumentException
    {
        makeCoverPage(description);
        makeBookInfoPage(description);
    }

    private void addLine(String text, ParagraphStyle style)
        throws FB2toPDFException, DocumentException
    {
        Chunk chunk = style.createChunk();
        chunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings()));
        Paragraph para = style.createParagraph();
        para.add(chunk);
        doc.add(para);
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
            ElementCollection images = ElementCollection.childrenByTagName(coverPage, "image");
            for (int i = 0; i < images.getLength(); ++i)
            {
                org.w3c.dom.Element coverImage = images.item(i);

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
        return String.format("%s %s %s", firstName, middleName, lastName).trim();
    }
    
    private void addMetaInfo(org.w3c.dom.Element description)
        throws FB2toPDFException, DocumentException
    {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null)
        {
            ElementCollection authors = ElementCollection.childrenByTagName(titleInfo, "author");
            StringBuilder allAuthors = new StringBuilder();
            for (int i = 0; i < authors.getLength(); ++i)
            {
                org.w3c.dom.Element author = authors.item(i);
                String authorName = getAuthorFullName(author);
                System.out.println("Adding author: " + transliterate(authorName));
                doc.addAuthor(transliterate(authorName));

                if(allAuthors.length() > 0)
                    allAuthors.append(", ");
                allAuthors.append(transliterate(authorName));
            }

            if (allAuthors.length() > 0)
                doc.addAuthor(allAuthors.toString());

            org.w3c.dom.Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            ElementCollection sequences = ElementCollection.childrenByTagName(titleInfo, "sequence");

            if (bookTitle != null && sequences.getLength() == 0)
            {
                String titleString = bookTitle.getTextContent();
                doc.addTitle(transliterate(titleString));
                System.out.println("Adding title: " + transliterate(titleString));
            }
            else if (bookTitle != null && sequences.getLength() != 0)
            {
                for(int i = 0; i < sequences.getLength(); i++)
                {
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
        throws FB2toPDFException, DocumentException
    {
        ParagraphStyle frontMatter = stylesheet.getParagraphStyle("frontMatter");
        ParagraphStyle titleStyle    = stylesheet.getParagraphStyle("title");
        ParagraphStyle subtitleStyle = stylesheet.getParagraphStyle("subtitle");
        ParagraphStyle authorStyle   = stylesheet.getParagraphStyle("author");

        org.w3c.dom.Element publishInfo = getOptionalChildByTagName(description, "publish-info");
        if (publishInfo != null)
        {
            addLine(
                getTextContentByTagName(publishInfo, "book-name", null, " // ") +
                getTextContentByTagName(publishInfo, "publisher") +
                getTextContentByTagName(publishInfo, "city", ", ")+
                getTextContentByTagName(publishInfo, "year", ", " ),
                    frontMatter);
            String isbn = getTextContentByTagName(publishInfo, "isbn");
            if (isbn.length() > 0)
                addLine("ISBN: " + isbn,
                    frontMatter);
        }

        org.w3c.dom.Element documentInfo = getOptionalChildByTagName(description, "document-info");
        ElementCollection documentAuthors = null;
        if (documentInfo != null)
            documentAuthors = ElementCollection.childrenByTagName(documentInfo, "author");
        for (int i = 0; documentAuthors != null && i < documentAuthors.getLength(); ++i)
        {
            org.w3c.dom.Element documentAuthor = documentAuthors.item(i);
            addLine(
                "FB2: " +
                getTextContentByTagName(documentAuthor, "first-name", " ") +
                getTextContentByTagName(documentAuthor, "last-name", " ") +
                getTextContentByTagName(documentAuthor, "nickname", " \u201C", "\u201D") +
                getTextContentByTagName(documentAuthor, "email", " <", ">") +
                getTextContentByTagName(documentInfo, "date", ", ") +
                getTextContentByTagName(documentInfo, "version", ", version "),
                    frontMatter);
        }
        if (documentInfo != null)
        {
            addLine(
                "UUID: " + getTextContentByTagName(documentInfo, "id"),
                    frontMatter);
        }

        addLine("PDF: org.trivee.fb2pdf.FB2toPDF 1.0, " + 
            java.text.DateFormat.getDateInstance().format(new java.util.Date()),
                    frontMatter);

        addLine(" ", titleStyle);

        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null)
        {
            ElementCollection authors = ElementCollection.childrenByTagName(titleInfo, "author");
            for (int i = 0; i < authors.getLength(); ++i)
            {
                org.w3c.dom.Element author = authors.item(i);
                String authorName = getAuthorFullName(author);
                // doc.addAuthor(transliterate(authorName));
                addLine(authorName, authorStyle);
            }

            org.w3c.dom.Element bookTitle = getOptionalChildByTagName(titleInfo, "book-title");
            ElementCollection sequences = ElementCollection.childrenByTagName(titleInfo, "sequence");

            if (bookTitle != null && sequences.getLength() == 0)
            {
                addLine(" ", titleStyle);
                addLine(bookTitle.getTextContent(), titleStyle);
                addLine(" ", titleStyle);
            }
            else if (bookTitle != null && sequences.getLength() != 0)
            {
                addLine(" ", titleStyle);
                addLine(bookTitle.getTextContent(), titleStyle);
                for(int i = 0; i < sequences.getLength(); i++)
                {
                    String subtitle = getSequenceSubtitle(sequences.item(i));
                    addLine(subtitle, subtitleStyle);
                    addLine(" ", titleStyle);
                }
            }

            org.w3c.dom.Element annotation = getOptionalChildByTagName(titleInfo, "annotation");
            if (annotation != null)
            {

                currentStyle = stylesheet.getParagraphStyle("annotation");
                processSectionContent(annotation, 0);
                currentStyle = null;
            }
        }

        doc.newPage();
    }

    private Image getImage(String href)
    {
        System.out.println("Loading image at " + href);
        for (int i = 0; i < attachments.size(); ++i)
        {
            BinaryAttachment attachment = attachments.elementAt(i);
            if (!attachment.getHREF().equals(href))
                continue;
            try
            {
                String overrideTransparency = stylesheet.getGeneralSettings().overrideImageTransparency;
                if (overrideTransparency == null || overrideTransparency.isEmpty()) {
                    return Image.getInstance(attachment.getData());
                } else {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    java.awt.Image img = toolkit.createImage(attachment.getData());
                    return Image.getInstance(img, Color.getColor(overrideTransparency));
                }
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
        throws DocumentException, FB2toPDFException
    {
        currentStyle = stylesheet.getParagraphStyle("body");
        //processSections(body);
        processSectionContent(body, -1);
        currentStyle = null;
    }

    private void makeTOCPage(org.w3c.dom.Element body)
        throws DocumentException, FB2toPDFException
    {
        ElementCollection sections = ElementCollection.childrenByTagName(body, "section");
        if (sections.getLength() <= 1)
            return;

        ParagraphStyle tocTitleStyle   = stylesheet.getParagraphStyle("tocTitle");
        ParagraphStyle tocItemStyle    = stylesheet.getParagraphStyle("tocItem");

        addLine(tocTitleStyle.getText(), tocTitleStyle);

        for (int i = 0; i < sections.getLength(); ++i)
        {
            org.w3c.dom.Element section = sections.item(i);

            String title = getTextContentByTagName(section, "title");
            if (title.length() == 0)
                title = "#" + (i+1);

            Chunk chunk = tocItemStyle.createChunk();
            chunk.append(TextPreprocessor.process(title, stylesheet.getTextPreprocessorSettings()));

            Anchor anchor = tocItemStyle.createAnchor();
            anchor.add(chunk);
            String ref = section.getAttribute("id");
            if(isNullOrEmpty(ref))
                ref = "section" + i;
            anchor.setReference("#" + ref);
            System.out.println("Adding A HREF=#" + ref);

            Paragraph para = tocItemStyle.createParagraph();
            para.add(anchor);

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

    private PdfOutline addBookmark(String title)
    {
        System.out.println("Adding bookmark: " + transliterate(title));
        PdfDestination destination = new PdfDestination(PdfDestination.FITH);
        return new PdfOutline(currentOutline, destination, transliterate(title));
    }

    private void processSection(org.w3c.dom.Element section, int level, int index)
        throws DocumentException, FB2toPDFException
    {
        PdfOutline previousOutline = currentOutline;

        if (level == 0)
        {
            doc.newPage();
            if (bodyIndex == 0)
            {
                currentOutline = writer.getDirectContent().getRootOutline();
                currentOutline = addBookmark(getTextContentByTagName(section, "title"));
            }
        }
        else if (level == 1)
        {
            if (writer.getVerticalPosition(false) < doc.getPageSize().getHeight() * 0.5f)
                doc.newPage();
            if (bodyIndex == 0)
                addBookmark(getTextContentByTagName(section, "title"));
        }

        String id = section.getAttribute("id");

        if (id.length() == 0 && bodyIndex == 0 && level == 0)
            id = "section" + index;

        if (id.length() > 0)
        {
            Anchor anchor = currentStyle.createInvisibleAnchor();
            anchor.setName(id);
            doc.add(anchor);
            System.out.println("Adding A NAME=" + id);
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

        processSectionContent(section, level);

        currentOutline = previousOutline;
    }

    private void addEmptyLine()
        throws DocumentException, FB2toPDFException
    {
        Chunk chunk = currentStyle.createChunk();
        chunk.append(" ");
        Paragraph p = currentStyle.createParagraph();
        p.add(chunk);
        doc.add(p);
    }

    private void processSectionContent(org.w3c.dom.Element parent, int level)
        throws DocumentException, FB2toPDFException
    {
        boolean bFirst = true;

        ElementCollection nodes = ElementCollection.children(parent);
        int subsectionIndex = 0;
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = nodes.item(i);

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
                processParagraph(element, bFirst, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("empty-line"))
            {
                if (!isIgnoreEmptyLine(element))
                    addEmptyLine();
            }
            else if (element.getTagName().equals("image"))
            {
                addImage(element);
            }
            else if (element.getTagName().equals("poem"))
            {
                processPoem(element);
            }
            else if (element.getTagName().equals("subtitle"))
            {
                ParagraphStyle previousStyle = currentStyle;
                currentStyle = stylesheet.getParagraphStyle("bodySubtitle");
                processParagraph(element, true, true);
                currentStyle = previousStyle;
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
        throws DocumentException, FB2toPDFException
    {
        ParagraphStyle previousStyle = currentStyle;

        if (level == 0)
        {
            currentStyle = stylesheet.getParagraphStyle("sectionTitle");
        }
        else if (level == 1)
        {
            currentStyle = stylesheet.getParagraphStyle("subSectionTitle");
        }
        else
        {
            currentStyle = stylesheet.getParagraphStyle("subSubSectionTitle");
        }

        ElementCollection nodes = ElementCollection.children(title);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("p"))
            {
                /* XXX TODO
                pid=x.getAttribute('id')
                if pid:
                    res+='\\hypertarget{%s}{}\n' % pid
                */
                processParagraph(element, i == 0, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("empty-line"))
            {
                if (!isIgnoreEmptyLine(element))
                    addEmptyLine();
            }
            else
            {
                System.out.println("Unhandled title tag " + element.getTagName() );
            }
        }

        currentStyle = previousStyle;
    }

    private void processEpigraph(org.w3c.dom.Element epigraph)
        throws DocumentException, FB2toPDFException
    {
        processTextWithAuthor(epigraph, "epigraph", "epigraphAuthor");
    }


    private void processCite(org.w3c.dom.Element cite)
        throws DocumentException, FB2toPDFException
    {
        processTextWithAuthor(cite, "cite", "citeAuthor");
    }

    private void processTextWithAuthor(org.w3c.dom.Element textWithAuthor, String mainStyleName, String authorStyleName)
        throws DocumentException, FB2toPDFException
    {
        ParagraphStyle previousStyle = currentStyle;

        ParagraphStyle mainStyle   = stylesheet.getParagraphStyle(mainStyleName);
        ParagraphStyle authorStyle = stylesheet.getParagraphStyle(authorStyleName);

        currentStyle = mainStyle;

        ElementCollection nodes = ElementCollection.children(textWithAuthor);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("p"))
            {
                processParagraph(element, i == 0, i == nodes.getLength()-1);
            }
            else if (element.getTagName().equals("poem"))
            {
                processPoem(element);
            }
            else if (element.getTagName().equals("text-author"))
            {
                currentStyle = authorStyle;
                processParagraph(element, true, true);
                currentStyle = mainStyle;
            }
            else if (element.getTagName().equals("empty-line"))
            {
                if (!isIgnoreEmptyLine(element))
                    addEmptyLine();
            }
            else
            {
                System.out.println("Unhandled tag " + element.getTagName() + " inside " + textWithAuthor.getTagName());
            }
        }

        currentStyle = previousStyle;
    }

    private void processPoem(org.w3c.dom.Element poem)
        throws DocumentException, FB2toPDFException
    {
        ElementCollection nodes = ElementCollection.children(poem);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("stanza"))
            {
                processStanza(element);
            }
            else
            {
                System.out.println("Unhandled poem tag " + element.getTagName() );
            }
        }
    }

    private void processStanza(org.w3c.dom.Element stanza)
        throws DocumentException, FB2toPDFException
    {
        ParagraphStyle previousStyle = currentStyle;
        currentStyle = stylesheet.getParagraphStyle("poem");

        ElementCollection nodes = ElementCollection.children(stanza);
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            org.w3c.dom.Element element = nodes.item(i);

            if (element.getTagName().equals("v"))
            {
                processParagraph(element, i == 0, i == nodes.getLength()-1);
            }
            else
            {
                System.out.println("Unhandled stanza tag " + element.getTagName() );
            }
        }

        currentStyle = previousStyle;
    }

    private Paragraph currentParagraph;
    private String    currentReference;
    private Chunk     currentChunk;

    private void processParagraph(org.w3c.dom.Element paragraph, boolean bFirst, boolean bLast)
        throws DocumentException, FB2toPDFException
    {
        currentParagraph = currentStyle.createParagraph(bFirst, bLast);

        processParagraphContent(paragraph);
        flushCurrentChunk();

        doc.add(currentParagraph);
        currentParagraph = null;
        currentReference = null;
    }

    private void flushCurrentChunk()
        throws DocumentException, FB2toPDFException
    {
        if (currentChunk != null)
        {
            if (!isNullOrEmpty(currentReference))
            {
                if (currentReference.charAt(0) == '#')
                {
                    //Unlike Anchor, Action won't fail even when local destination does not exist
                    String refname = currentReference.substring(1); //getting rid of "#" at the begin of the reference
                    PdfAction action = PdfAction.gotoLocalPage(refname, false);
                    System.out.println("Adding Action LocalGoTo " + refname);
                    currentChunk.setAction(action);
                    currentParagraph.add(currentChunk);
                }
                else
                {
                    Anchor anchor = currentStyle.createAnchor();
                    anchor.add(currentChunk);
                    anchor.setReference(currentReference);
                    System.out.println("Adding A HREF=" + currentReference);
                    currentParagraph.add(anchor);
                }
            }
            else
                currentParagraph.add(currentChunk);
            currentChunk = null;
        }
    }

    private void processParagraphContent(org.w3c.dom.Element parent)
        throws DocumentException, FB2toPDFException
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
                    currentStyle.toggleBold();
                    processParagraphContent(child);
                    flushCurrentChunk();
                    currentStyle.toggleBold();
                }
                else if (child.getTagName().equals("emphasis"))
                {
                    flushCurrentChunk();
                    currentStyle.toggleItalic();
                    processParagraphContent(child);
                    flushCurrentChunk();
                    currentStyle.toggleItalic();
                }
                else if (child.getTagName().equals("a"))
                {
                    flushCurrentChunk();
                    currentReference = child.getAttributeNS(NS_XLINK, "href");
                    if (currentReference.length() == 0)
                        currentReference = child.getAttribute("href");
                    processParagraphContent(child);
                    flushCurrentChunk();
                    currentReference = null;
                }
                else if (child.getTagName().equals("cite"))
                {
                    flushCurrentChunk();
                    String citeId = child.getAttribute("id");
                    if (citeId.length() > 0)
                    {
                        Anchor anchor = currentStyle.createInvisibleAnchor();
                        anchor.setName(citeId);
                        currentParagraph.add(anchor);
                        System.out.println("Adding A NAME=" + citeId);
                    }
                    processParagraphContent(child);
                    flushCurrentChunk();
                    currentReference = null;
                }
                else if (child.getTagName().equals("style"))
                {
                    String styleName = child.getAttribute("name");
                    System.out.println("Style tag " + styleName + " ignored.");
                    processParagraphContent(child);
                }
                else if (child.getTagName().equals("image"))
                {
                    addImage(child);
                }
                else
                {
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
                    processParagraphContent(child);
                }
            }
            else if (node.getNodeType() == Node.TEXT_NODE)
            {
                // XXX TODO res += _textQuote(s.data)
                if (currentChunk == null)
                {
                    currentChunk = currentStyle.createChunk();
                    if (!currentStyle.getDisableHyphenation()) {
                        currentChunk.setHyphenation(hyphenation);
                    }
                }

                String text = node.getTextContent();
                //currentChunk.append(fixCharacters(text));
                currentChunk.append(TextPreprocessor.process(text, stylesheet.getTextPreprocessorSettings()));
            }
        }
    }

    private static final String[][] TRANSTABLE = {
        // ������� �������
        // ������������� ������
        { "\u0429", "SCH" },
        // ������������� ������
        { "\u0401", "YO" },
        { "\u0416", "ZH" },
        { "\u0426", "TS" },
        { "\u0427", "CH" },
        { "\u0428", "SH" },
        { "\u042E", "YU" },
        { "\u042F", "YA" },
        // ������������� ������
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
        // ������ �������
        // ������������� ������
        { "\u0449", "sch" },
        // ������������� ������
        { "\u0451", "yo" },
        { "\u0436", "zh" },
        { "\u0446", "ts" },
        { "\u0447", "ch" },
        { "\u0448", "sh" },
        { "\u044E", "yu" },
        { "\u044F", "ya" },
        // ������������� ������
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

    private String transliterate(String text)
    {
        if (!stylesheet.getGeneralSettings().transliterateMetaInfo)
            return text;

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
        translate(fromName, toName, null);
    }

    public static void translate(String fromName, String toName, InputStream stylesheet)
         throws DocumentException, IOException, FB2toPDFException, ParserConfigurationException, SAXException
    {
        new FB2toPDF(fromName, toName).run(stylesheet);
    }

    public static void main(String [] args) 
    {
        try
        {
            if(args.length < 2)
            {
                System.out.println("Usage: java " + FB2toPDF.class.getName() + " <input.fb2> <output.pdf>");
                return;
            }
            translate(args[0], args[1]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private String getLang(org.w3c.dom.Element description) throws FB2toPDFException
    {
        org.w3c.dom.Element titleInfo = getOptionalChildByTagName(description, "title-info");
        if (titleInfo != null)
        {
            org.w3c.dom.Element lang = getOptionalChildByTagName(titleInfo, "lang");
            if (lang != null)
            {
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
            if (isNullOrEmpty(bookLang) || hyphSettings.overrideLanguage)
                bookLang = hyphSettings.defaultLanguage;
            hyphenation = new HyphenationAuto(bookLang, "none", 2, 2);
            System.out.println("Hyphenation language is: " + bookLang);
        }
        else
            System.out.println("Hyphenation is off");
    }
}
