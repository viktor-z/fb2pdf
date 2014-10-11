package org.trivee.fb2pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Text;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author vzeltser
 */
public class FootnoteRenderer {
    
    static PageStyle pageStyle;
    static ParagraphStyle noteStyle;
    static PdfWriter writer;
    static Document doc;
    static float fontSize;
    static BaseFont basefont;
    static Rectangle pageSize;
    static ByteArrayOutputStream output;
    static float cutMarkerWidth = 0;
    private static boolean superscript;
    private static boolean subscript;
    static float topMargin = 0;

    private static void addCutMarker() throws FB2toPDFException, DocumentException {
        doc.setPageSize(new Rectangle(cutMarkerWidth, pageSize.getHeight()));
        doc.newPage();
        Paragraph paragraph = createParagraph();
        paragraph.setAlignment(Paragraph.ALIGN_RIGHT);
        paragraph.setIndentationLeft(0);
        paragraph.setIndentationRight(0);
        paragraph.setFirstLineIndent(0);
        Chunk chunk = noteStyle.createChunk();
        chunk.append("<\u2026> ");
        paragraph.add(chunk);
        doc.add(paragraph);
        
        doc.setPageSize(pageSize);

        doc.newPage();
    }

    private static void addNode(Node rootNode, HyphenationAuto hyphenation, Paragraph paragraph, String marker) throws FB2toPDFException {
        for (int j=0; j<rootNode.getChildCount(); j++) {
            Node node = rootNode.getChild(j);
            if (node instanceof Text) {
                Chunk chunk = noteStyle.createChunk();
                chunk.setHyphenation(hyphenation);
                if (superscript) {
                    chunk.setTextRise(noteStyle.getFontSize() / 3);
                }
                if (subscript) {
                    chunk.setTextRise(-noteStyle.getFontSize() / 12);
                }
                String value = (marker == null) ? node.getValue() : marker + " " + node.getValue();
                chunk.append(value);
                paragraph.add(chunk);
                marker = null;
                continue;
            } 
            if (!(node instanceof Element)) {
                continue;
            }
            if (marker != null) {
                Chunk chunk = noteStyle.createChunk();
                chunk.setHyphenation(hyphenation);
                chunk.append(marker + " ");
                paragraph.add(chunk);
                marker = null;
            }
            Element element = (Element)node;
            if (element.getLocalName().equals("emphasis")) {
                noteStyle.toggleItalic();
                addNode(node, hyphenation, paragraph, marker);
                noteStyle.toggleItalic();
            } else if (element.getLocalName().equals("strong")) {
                noteStyle.toggleBold();
                addNode(node, hyphenation, paragraph, null);
                noteStyle.toggleBold();
            } else if (element.getLocalName().equals("strikethrough")) {
                noteStyle.toggleStrikethrough();
                addNode(node, hyphenation, paragraph, null);
                noteStyle.toggleStrikethrough();
            } else if (element.getLocalName().equals("sup")) {
                noteStyle.toggleHalfSize();
                superscript = true;
                addNode(node, hyphenation, paragraph, null);
                noteStyle.toggleHalfSize();
                superscript = false;
            } else if (element.getLocalName().equals("sub")) {
                noteStyle.toggleHalfSize();
                subscript = true;
                addNode(node, hyphenation, paragraph, null);
                noteStyle.toggleHalfSize();
                subscript = false;
            } else {
                addNode(node, hyphenation, paragraph, null);
            }
   
        }
    }

    private static Paragraph createParagraph() throws FB2toPDFException {
        float ascent = basefont.getFontDescriptor(BaseFont.ASCENT, fontSize);
        Paragraph paragraph = noteStyle.createParagraph();
        paragraph.setLeading(ascent);
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBefore(0);
        return paragraph;
    }
   
    public static byte[] close() {
        doc.close();
        return output.toByteArray();
    }
    
    public static int getPageNumber() {
        return writer.getPageNumber();
    }
    
    public static void addFootnote(String marker, String refname, Element section, HyphenationAuto hyphenation) throws FB2toPDFException, DocumentException {

        boolean added = addFootnote(section, hyphenation, marker, true);

        if (added) {
            addCutMarker();
        }
    }

    private static boolean addFootnote(Element element, HyphenationAuto hyphenation, String marker, boolean skipTitle) throws FB2toPDFException, DocumentException {
        if (element == null) {
            return false;
        }
        
        boolean added = false;

        Elements children = element.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String localName = child.getLocalName();
            if(StringUtils.isBlank(localName)) {
                continue;
            }
            if(localName.equals("poem") || 
               localName.equals("stanza") || 
               localName.equals("epigraph") ||
               localName.equals("cite")){
                added = addFootnote(child, hyphenation, marker, false);
                if (added) {
                    marker = null;
                }
            } else if (localName.equals("p") || localName.equals("v") || localName.equals("text-author") ||
                    localName.equals("date") || 
                    (!skipTitle && localName.equals("title"))) {
                Paragraph paragraph = createParagraph();
                if (marker != null) {
                    paragraph.setFirstLineIndent(noteStyle.getFirstFirstLineIndent());
                }
                addNode(child, hyphenation, paragraph, marker);
                marker = null;
                doc.add(paragraph);
                added = true;
            }
        }
        return added;
    }
    
    public static void init(Stylesheet stylesheet) throws FB2toPDFException, DocumentException {
        initParams(stylesheet);
        doc = new Document(pageSize, 0, 0, topMargin, 0);
        PdfDocument.preventWidows = false;
        output = new ByteArrayOutputStream();
        writer = PdfWriter.getInstance(doc, output);
        writer.setPageEvent(new PageEvents());
        doc.open();
    }

    public static void reinit(Stylesheet stylesheet) throws FB2toPDFException, DocumentException {
        if (doc == null) {
            init(stylesheet);
            return;
        }
        
        initParams(stylesheet);
        doc.setPageSize(pageSize);
    }

    private static void initParams(Stylesheet stylesheet) throws FB2toPDFException {
        pageStyle = stylesheet.getPageStyle();
        noteStyle = stylesheet.getParagraphStyle("footnote");
        float pageWidth = pageStyle.getPageWidth() - pageStyle.getMarginLeft() - pageStyle.getMarginRight();
        fontSize = noteStyle.getFontSize();
        basefont = noteStyle.getBaseFont();
        cutMarkerWidth = basefont.getWidthPointKerned("  <\u2026> ", fontSize);
        float ascdesc = getAscDesc();
        float pageHeight = Math.max(ascdesc, noteStyle.getAbsoluteLeading());
        pageSize = new Rectangle(pageWidth, pageHeight);
        
        float delta = noteStyle.getAbsoluteLeading() - ascdesc;
        topMargin = (delta > 0) ? delta : 0;

        //pageSize.setBackgroundColor(BaseColor.LIGHT_GRAY);
        //pageSize.setBorder(Rectangle.BOX);
        //pageSize.setBorderColor(BaseColor.DARK_GRAY);
    }

    private static float getAscDesc() throws FB2toPDFException {
        
        FontFamily family = noteStyle.getFontFamily();
        BaseFont[] fonts = {
            family.getRegularFont(), 
            family.getBoldFont(),
            family.getItalicFont(),
            family.getBoldItalicFont()
        };
        
        float ascdesc = -999;
        
        for (BaseFont font: fonts) {
            float ascent = font.getFontDescriptor(BaseFont.ASCENT, fontSize);
            float descent = font.getFontDescriptor(BaseFont.DESCENT, fontSize);
            ascdesc = Math.max(ascdesc, ascent - descent);
        }
        
        return ascdesc;
    }

    private static class PageEvents extends PdfPageEventHelper {

        @Override
        public void onStartPage(PdfWriter writer,Document document) {
            writer.setPageEmpty(false);
        }
    }
}