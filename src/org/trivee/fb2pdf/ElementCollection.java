package org.trivee.fb2pdf;

import java.util.Vector;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class ElementCollection
{
    private Vector<Element> elements = new Vector<Element>();

    public ElementCollection()
    {
    }

    public void add(Element element)
    {
        elements.add(element);
    }

    public int getLength()
    {
        return elements.size();
    }

    public Element item(int index)
    {
        return elements.elementAt(index);
    }

    public static ElementCollection childrenByTagName(Element element, String tagName)
    {
        ElementCollection result = new ElementCollection();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element child = (Element)node;
            if (child.getTagName().equals(tagName))
                result.add(child);
        }

        return result;
    }

    public static ElementCollection children(Element element)
    {
        ElementCollection result = new ElementCollection();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element child = (Element)node;
            result.add(child);
        }

        return result;
    }
}
