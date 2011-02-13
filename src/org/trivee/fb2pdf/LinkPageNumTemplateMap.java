/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author vzeltser
 */
public class LinkPageNumTemplateMap extends HashMap<String, LinkedList<LinkPageNumTemplate>> {

    public void put(String key, LinkPageNumTemplate value) {
        LinkedList<LinkPageNumTemplate> bucket;
        if (super.containsKey(key)) {
            bucket = get(key);
        } else {
            bucket = new LinkedList<LinkPageNumTemplate>();
            put(key, bucket);
        }
        bucket.add(value);
    }
}
