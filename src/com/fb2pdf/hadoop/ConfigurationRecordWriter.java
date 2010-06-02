package com.fb2pdf.hadoop;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ConfigurationRecordWriter<K extends Text, V extends Text>
		implements RecordWriter<K, V> {
	private DataOutputStream out;
	private Properties properties = new Properties();

	public ConfigurationRecordWriter(FSDataOutputStream fileOut) {
		out = fileOut;
	}

	@Override
	public void close(Reporter reporter) throws IOException {
		write(out);
		out.close();
	}

	@Override
	public void write(K key, V value) throws IOException {
		properties.setProperty(key.toString(), value.toString());
	}

	public void write(OutputStream out) throws IOException {
		
		try {
			Document doc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().newDocument();
			Element conf = doc.createElement("configuration");
			doc.appendChild(conf);
			conf.appendChild(doc.createTextNode("\n"));
			for (Enumeration<Object> e = properties.keys(); e.hasMoreElements();) {
				String name = (String) e.nextElement();
				Object object = properties.get(name);
				String value = null;
				if (object instanceof String) {
					value = (String) object;
				} else {
					continue;
				}
				Element propNode = doc.createElement("property");
				conf.appendChild(propNode);

				Element nameNode = doc.createElement("name");
				nameNode.appendChild(doc.createTextNode(name));
				propNode.appendChild(nameNode);

				Element valueNode = doc.createElement("value");
				valueNode.appendChild(doc.createTextNode(value));
				propNode.appendChild(valueNode);

				conf.appendChild(doc.createTextNode("\n"));
			}

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(out);
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.transform(source, result);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
