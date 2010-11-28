/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;
import nux.xom.pool.XQueryPool;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;
import nux.xom.xquery.XQueryUtil;
import org.trivee.fb2pdf.TransformationSettings.Entry;

/**
 *
 * @author vzeltser
 */
public class Transformation {

    public static InputStream transform(InputStream inputStream, TransformationSettings settings) throws ParsingException, ValidityException, IOException, XQueryException {

        if (!settings.enabled) {
            return inputStream;
        }

        String queryProlog = settings.queryProlog;
        String morpherProlog = settings.morpherProlog;
        Document xdoc = new Builder().build(inputStream);

        for (Entry entry : settings.transformationsMap)
        {
            transform(queryProlog + entry.query, morpherProlog + entry.morpher, xdoc);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer ser = new Serializer(out);
        ser.write(xdoc);
        out.close();
        if (settings.outputDebugFile) {
            (new FileOutputStream("transformation-result.xml")).write(out.toByteArray());
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static void transform(String query, String morpher, Document xdoc) throws IOException, XQueryException, ParsingException {
        XQuery xselect = XQueryPool.GLOBAL_POOL.getXQuery(query, null);
        XQuery xmorpher = XQueryPool.GLOBAL_POOL.getXQuery(morpher, null);
        Nodes nodes = xselect.execute(xdoc).toNodes();
        System.out.println(String.format("Transformation query '%s' returned %d nodes", query, nodes.size()));
        XQueryUtil.update(nodes, xmorpher, null);
    }
}
