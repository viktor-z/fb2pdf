/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nux.xom.pool.XQueryPool;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;
import nux.xom.xquery.XQueryUtil;
import org.apache.commons.lang.StringUtils;
import org.trivee.fb2pdf.TransformationSettings.Entry;

/**
 *
 * @author vzeltser
 */
public class XQueryUtilities {
    
    public static String defaultProlog = "declare default element namespace \"http://www.gribuser.ru/xml/fictionbook/2.0\"; "
        + "declare namespace l = \"http://www.w3.org/1999/xlink\"; ";
    

    public static void outputDebugInfo(Document xdoc, TransformationSettings settings, final String fileName) throws IOException {
        if (settings.outputDebugFile) {
            byte[] result = serialize(xdoc);
            (new FileOutputStream(fileName)).write(result);
        }
    }

    /*
    public static InputStream transformToInputStream(InputStream inputStream, TransformationSettings settings) throws ParsingException, ValidityException, IOException, XQueryException {

        if (!settings.enabled) {
            return inputStream;
        }
        Document xdoc = new Builder(false).build(inputStream);
        transform(xdoc, settings);
        byte[] result = serialize(xdoc);
        if (settings.outputDebugFile) {
            (new FileOutputStream("transformation-result.xml")).write(result);
        }
        return new ByteArrayInputStream(result);
    }
     */

    private static byte[] serialize(Document xdoc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer ser = new Serializer(out);
        ser.write(xdoc);
        out.close();
        return out.toByteArray();
    }

    public static void transform(Document xdoc, TransformationSettings settings) throws ParsingException, XQueryException, IOException {
        String queryProlog = settings.queryProlog;
        String morpherProlog = settings.morpherProlog;
        for (Entry entry : settings.transformationsMap)
        {
            if (entry == null) continue;
            transform(xdoc, queryProlog + entry.query, morpherProlog + entry.morpher);
        }
        outputDebugInfo(xdoc, settings, "transformation-result.xml");
    }

    public static void transform(Document xdoc, String query, String morpher) throws IOException, XQueryException, ParsingException {
        XQuery xmorpher = XQueryPool.GLOBAL_POOL.getXQuery(morpher, null);
        Nodes nodes = query(query, xdoc);
        XQueryUtil.update(nodes, xmorpher, null);
    }
    
    public static String getString(Element element, TransformationSettings settings, String query, String separator) {
        String queryProlog = settings.queryProlog;
        Nodes nodes = XQueryUtil.xquery(element, queryProlog + query);
        List<String> strings = new ArrayList<String>(nodes.size());
        for (int i=0; i<nodes.size(); i++) {
            strings.add(nodes.get(i).getValue());
        }
        return StringUtils.join(strings, separator == null ? " " : separator);
    }
    
    public static Nodes query(String query, Document xdoc) throws XQueryException {
        XQuery xselect = XQueryPool.GLOBAL_POOL.getXQuery(query, null);
        Nodes nodes = xselect.execute(xdoc).toNodes();
        System.out.println(String.format("Query '%s' returned %d nodes", query, nodes.size()));
        return nodes;
    }

}
