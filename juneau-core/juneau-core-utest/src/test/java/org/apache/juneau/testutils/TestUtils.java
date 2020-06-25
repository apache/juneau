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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.w3c.dom.*;
import org.xml.sax.*;

@SuppressWarnings({})
public class TestUtils {

	private static JsonSerializer js2 = JsonSerializer.create()
		.ssq()
		.addBeanTypes().addRootType()
		.build();

	private static final BeanSession beanSession = BeanContext.DEFAULT.createSession();

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
			printLines(lines);
			throw e;
		}
	}

	private static final void printLines(String[] lines) {
		for (int i = 0; i < lines.length; i++)
			System.err.println(String.format("%4s:" + lines[i], i+1)); // NOT DEBUG
	}

	public static final void validateXml(Object o) throws Exception {
		validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.builder().ws().ns().addNamespaceUrisToRoot().build();
		String xml = s.serialize(o);

		try {
			TestUtils.checkXmlWhitespace(xml);
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
		Document doc = db.parse(new InputSource(new StringReader(xml)));

		SortedNode n = new SortedNode(doc.getDocumentElement());
		return n.toString();
	}

	/**
	 * A sorted node in a DOM tree.
	 */
	private static class SortedNode implements Comparable<SortedNode> {
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
			indent(depth, sb).append("<").append(name).append(attrs);
			if (children.isEmpty() && text.isEmpty()) {
				sb.append("/>\n");
				return sb;
			}
			sb.append(">\n");
			if (! text.isEmpty())
				indent(depth+1, sb).append(text).append("\n");
			for (SortedNode c : children) {
				c.toString(depth+1, sb);
			}
			indent(depth, sb).append("</").append(name).append(">\n");
			return sb;
		}
	}

	private static StringBuilder indent(int depth, StringBuilder sb) {
		for (int i = 0; i < depth; i++)
			sb.append("\t");
		return sb;
	}

	/**
	 * Compares two XML documents for equality.
	 * Namespaces are stripped from each and elements/attributes are ordered in alphabetical order,
	 * 	then a simple string comparison is performed.
	 */
	public static final void assertXmlEquals(String expected, String actual) throws Exception {
		Assert.assertEquals(sortXml(expected), sortXml(actual));
	}

	public static final void assertObjectMatches(String s, Object o) {
		assertObjectMatches(s, o, js2);
	}

	public static final void assertObjectMatches(String s, Object o, WriterSerializer ws) {
		if ("xxx".equals(s))
			System.err.println(ws.toString(o).replaceAll("\\\\", "\\\\\\\\")); // NOT DEBUG
		String o2 =  ws.toString(o);
		if (! StringUtils.getMatchPattern(s).matcher(o2).matches())
			throw new ComparisonFailure(null, s, o2);
	}

	private static ThreadLocal<TimeZone> systemTimeZone = new ThreadLocal<>();
	private static ThreadLocal<Locale> systemLocale = new ThreadLocal<>();

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 * Use {@link #unsetTimeZone()} to unset it.
	 *
	 * @param name
	 */
	public synchronized static final void setTimeZone(String name) {
		systemTimeZone.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(name));
	}

	public synchronized static final void unsetTimeZone() {
		TimeZone.setDefault(systemTimeZone.get());
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 * Use {@link #unsetLocale()} to unset it.
	 *
	 * @param name
	 */
	public static final void setLocale(Locale locale) {
		systemLocale.set(Locale.getDefault());
		Locale.setDefault(locale);
	}

	public static final void unsetLocale() {
		Locale.setDefault(systemLocale.get());
	}

	public static final void assertEqualsAfterSort(String expected, String actual, String msg, Object...args) {
		// Must work for windows too.
		String[] e = expected.trim().split("[\r\n]+"), a = actual.trim().split("[\r\n]+");

		if (e.length != a.length)
			throw new ComparisonFailure(format(msg, args), expected, actual);

		Arrays.sort(e);
		Arrays.sort(a);

		for (int i = 0; i < e.length; i++)
			if (! e[i].equals(a[i]))
				throw new ComparisonFailure(format(msg, args), expected, actual);
	}

	/**
	 * Creates a ClassMeta for the given types.
	 */
	public static final Type getType(Type type, Type...args) {
		return beanSession.getClassMeta(type, args);
	}

	/**
	 * Throws an AssertionError if the object isn't of the specified type.
	 */
	public static final <T> T assertInstanceOf(Class<?> type, T o) {
		if (type.isInstance(o))
			return o;
		throw new AssertionError(new StringMessage("Expected type {0} but was {1}", type, (o == null ? null : o.getClass())));
	}

	public static final String verifyInstanceOf(Class<?> type, Object o) {
		if (type.isInstance(o))
			return null;
		return new StringMessage("Expected type {0} but was {1}", type, (o == null ? null : o.getClass())).toString();
	}

	public static final String verifyEquals(Object expected, Object actual) {
		if (expected == actual)
			return null;
		if (expected == null || actual == null)
			return StringUtils.format("Expected {0} but was {1}", expected, actual);
		if (! expected.equals(actual))
			return StringUtils.format("Expected {0} but was {1}", expected, actual);
		return null;
	}

	public static final String verifyTrue(boolean b) {
		return verifyEquals(true, b);
	}

	public static final String verifyFalse(boolean b) {
		return verifyEquals(false, b);
	}

	public static final String verifyNull(Object o) {
		if (o == null)
			return null;
		return StringUtils.format("Expected {0} but was {1}", null, o);
	}

	public static final String verifyNotNull(Object o) {
		if (o != null)
			return null;
		return StringUtils.format("Expected non null value but was null");
	}
}
