/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nu.xom.*;
import nux.xom.pool.XQueryPool;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;
import nux.xom.xquery.XQueryUtil;
import org.apache.commons.lang3.StringUtils;
import org.trivee.fb2pdf.TransformationSettings.Entry;

/**
 *
 * @author vzeltser
 */
public class XQueryUtilities {
    
    public static String defaultProlog = "declare default element namespace \"http://www.gribuser.ru/xml/fictionbook/2.0\"; "
        + "declare namespace l = \"http://www.w3.org/1999/xlink\"; ";
    private static String libImport = null;
    

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
    
    private static String getLibPath(String filename) {
        try {
            String path = Utilities.getValidatedFileName("data/" + filename);
            return new File(path).toURI().toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getLibImport() {
        if (libImport == null) {
            String libpath = getLibPath("library.xq");
            return String.format("import module namespace fb = 'https://sites.google.com/site/fb2pdfj' at '%s'; ", libpath);
        }
        return libImport;
    }

    public static void transform(Document xdoc, TransformationSettings settings) throws ParsingException, XQueryException, IOException {
        String queryProlog = settings.queryProlog + getLibImport();
        String morpherProlog = settings.morpherProlog + getLibImport();
        for (Entry entry : settings.transformationsMap)
        {
            if (entry == null) continue;
            transform(xdoc, queryProlog + entry.query, morpherProlog + entry.morpher);
        }
        outputDebugInfo(xdoc, settings, "transformation-result.xml");
    }

    public static void transform(Document xdoc, String query, String morpher) throws IOException, XQueryException, ParsingException {
        XQuery xmorpher = XQueryPool.GLOBAL_POOL.getXQuery(morpher, null);
        Nodes nodes = query(query, xdoc, null);
        Log.info("Transformation query [{0}] returned {1} nodes", query, nodes.size());
        XQueryUtil.update(nodes, xmorpher, null);
    }
    
    public static String getString(Element element, TransformationSettings settings, String query, String separator) {
        return getString(element, settings, query, separator, null);
    }
    
    public static String getString(Element element, TransformationSettings settings, String query, String separator, Map<String, Object> variables) {
        StringBuilder sb = new StringBuilder(settings.queryProlog);
        sb.append(getLibImport());
        sb.append(query);
        Nodes nodes = query(sb.toString(), (Node)element, variables);
        List<String> strings = new ArrayList<String>(nodes.size());
        for (int i=0; i<nodes.size(); i++) {
            strings.add(nodes.get(i).getValue());
        }
        return StringUtils.join(strings, separator == null ? " " : separator);
    }
    
    private static Nodes query(String query, Node contextNode, Map<String, Object> variables) {
        try {
            XQuery xselect = XQueryPool.GLOBAL_POOL.getXQuery(query, null);
            Nodes nodes = xselect.execute(contextNode, null, variables).toNodes();
            return nodes;
        } catch (XQueryException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Nodes getNodes(String query, Node contextNode) {
        Nodes nodes = query(query, contextNode, null);
        Log.info("Query [{0}] returned {1} nodes", query, nodes.size());
        return nodes;
    }
}
