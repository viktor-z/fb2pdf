package org.trivee;

import org.trivee.fb2.FB2toPDF;
import org.trivee.fb2.FB2toPDFException;

public class Test {

/*
    private static void makeHelloWorld()
        throws DocumentException, IOException, FileNotFoundException
    {
        // step 1: creation of a document-object
        com.lowagie.text.Document document = openITextDocument("HelloWorld.pdf");

        // step 4: we add a paragraph to the document

        BaseFont bf = BaseFont.createFont("georgia.ttf",
            "Cp1251", BaseFont.EMBEDDED);
        Font font = new Font(bf, 9);

        com.lowagie.text.pdf.hyphenation.Hyphenator.setHyphenDir(".");
        HyphenationAuto auto = new HyphenationAuto("ru", "none", 2, 2);

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("input.txt"), "Cp1251"));
        while (br.ready())
        {
            String line = br.readLine();
            Chunk chunk = new Chunk(line, font);
            chunk.setHyphenation(auto);

            Paragraph p = new Paragraph(chunk);
            p.setFirstLineIndent(40.0f);

            p.setAlignment(Paragraph.ALIGN_JUSTIFIED);
            document.add(p);
        }
        br.close();

        document.close();
    }
*/

/*
    private static void writeXMLDocument(org.w3c.dom.Document document, String fileName)
        throws IOException
    {
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(new FileOutputStream(fileName), format);
        writer.write( document );
        writer.close();
    }
*/

    public static void main(String[] args) {
        try
        {
            FB2toPDF.translate("input.fb2", "output.pdf");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
