// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.testutils;

import static org.apache.juneau.testutils.StreamUtils.*;

import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class XmlUtils {

	/**
	 * A sorted node in a DOM tree.
	 */
	static class SortedNode implements Comparable<SortedNode> {
		public String name, text="", attrs="";
		public List<SortedNode> children = new LinkedList<>();

		SortedNode(Element e) {
			this.name = e.getNodeName();
			NamedNodeMap attrs = e.getAttributes();
			if (attrs != null) {
				StringBuilder sb = new StringBuilder();
				Set<String> attrNames = new TreeSet<>();
				for (int i = 0; i < attrs.getLength(); i++)
					attrNames.add(attrs.item(i).getNodeName());
				for (String n : attrNames) {
					Node node = attrs.getNamedItem(n);
					sb.append(" ").append(n).append("='").append(node.getNodeValue()).append("'");
				}
				this.attrs = sb.toString();
			}
			NodeList nl = e.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if (n instanceof Element)
					children.add(new SortedNode((Element)nl.item(i)));
				if (n instanceof Text)
					this.text += ((Text)n).getNodeValue();
			}
			Collections.sort(children);
		}

		@Override
		public int compareTo(SortedNode n) {
			int i = name.compareTo(n.name);
			if (i != 0)
				return i;
			i = attrs.compareTo(n.attrs);
			if (i != 0)
				return i;
			i = text.compareTo(n.text);
			if (i != 0)
				return i;
			return 0;
		}

		@Override
		public String toString() {
			return toString(0, new StringBuilder()).toString();
		}

		public StringBuilder toString(int depth ,StringBuilder sb) {
			XmlUtils.indent(depth, sb).append("<").append(name).append(attrs);
			if (children.isEmpty() && text.isEmpty()) {
				sb.append("/>\n");
				return sb;
			}
			sb.append(">\n");
			if (! text.isEmpty())
				XmlUtils.indent(depth+1, sb).append(text).append("\n");
			for (SortedNode c : children) {
				c.toString(depth+1, sb);
			}
			XmlUtils.indent(depth, sb).append("</").append(name).append(">\n");
			return sb;
		}
	}

	/**
	 * Validates that the whitespace is correct in the specified XML.
	 */
	public static final void checkXmlWhitespace(String out) throws SerializeException {
		if (out.indexOf('\u0000') != -1) {
			for (String s : out.split("\u0000"))
				checkXmlWhitespace(s);
			return;
		}

		int indent = -1;
		Pattern startTag = Pattern.compile("^(\\s*)<[^/>]+(\\s+\\S+=['\"]\\S*['\"])*\\s*>$");
		Pattern endTag = Pattern.compile("^(\\s*)</[^>]+>$");
		Pattern combinedTag = Pattern.compile("^(\\s*)<[^>/]+(\\s+\\S+=['\"]\\S*['\"])*\\s*/>$");
		Pattern contentOnly = Pattern.compile("^(\\s*)[^\\s\\<]+$");
		Pattern tagWithContent = Pattern.compile("^(\\s*)<[^>]+>.*</[^>]+>$");
		String[] lines = out.split("\n");
		try {
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				Matcher m = startTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on start tag line ''{0}''", i+1);
					continue;
				}
				m = endTag.matcher(line);
				if (m.matches()) {
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on end tag line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = combinedTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on combined tag line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = contentOnly.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on content-only line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = tagWithContent.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on tag-with-content line ''{0}''", i+1);
					indent--;
					continue;
				}
				throw new SerializeException("Unmatched whitespace line at line number ''{0}''", i+1);
			}
			if (indent != -1)
				throw new SerializeException("Possible unmatched tag.  indent=''{0}''", indent);
		} catch (SerializeException e) {
			XmlUtils.printLines(lines);
			throw e;
		}
	}

	public static final void validateXml(Object o) throws Exception {
		XmlUtils.validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		String xml = s.serialize(o);

		try {
			checkXmlWhitespace(xml);
		} catch (Exception e) {
			System.err.println("---XML---");       // NOT DEBUG
			System.err.println(xml);               // NOT DEBUG
			throw e;
		}
	}

	/**
	 * Sort an XML document by element and attribute names.
	 * This method is primarily meant for debugging purposes.
	 */
	private static final String sortXml(String xml) throws Exception {
		xml = xml.replaceAll("\\w+\\:", "").replaceAll(">\\s+<", "><");  // Strip out all namespaces and whitespace.

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(reader(xml)));

		XmlUtils.SortedNode n = new XmlUtils.SortedNode(doc.getDocumentElement());
		return n.toString();
	}

	/**
	 * Compares two XML documents for equality.
	 * Namespaces are stripped from each and elements/attributes are ordered in alphabetical order,
	 * 	then a simple string comparison is performed.
	 */
	public static final void assertXmlEquals(String expected, String actual) throws Exception {
		Assertions.assertString(sortXml(expected)).is(sortXml(actual));
	}

	private static StringBuilder indent(int depth, StringBuilder sb) {
		for (int i = 0; i < depth; i++)
			sb.append("\t");
		return sb;
	}

	private static final void printLines(String[] lines) {
		for (int i = 0; i < lines.length; i++)
			System.err.println(String.format("%4s:" + lines[i], i+1)); // NOT DEBUG
	}
}
