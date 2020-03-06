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
package org.apache.juneau.examples.rest;

import static org.junit.Assert.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.regex.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class TestUtils {

	private static JsonSerializer js = JsonSerializer.create()
		.ssq()
		.trimNullProperties(false)
		.build();

	private static JsonSerializer jsSorted = JsonSerializer.create()
		.ssq()
		.sortCollections()
		.sortMaps()
		.trimNullProperties(false)
		.build();


	private static JsonSerializer js2 = JsonSerializer.create()
		.ssq()
		.pojoSwaps(IteratorSwap.class, EnumerationSwap.class)
		.build();

	private static JsonSerializer js3 = JsonSerializer.create()
		.ssq()
		.pojoSwaps(IteratorSwap.class, EnumerationSwap.class)
		.sortProperties()
		.build();

	/**
	 * Verifies that two objects are equivalent.
	 * Does this by doing a string comparison after converting both to JSON.
	 */
	public static void assertEqualObjects(Object o1, Object o2) throws SerializeException {
		assertEqualObjects(o1, o2, false);
	}

	/**
	 * Verifies that two objects are equivalent.
	 * Does this by doing a string comparison after converting both to JSON.
	 * @param sort If <jk>true</jk> sort maps and collections before comparison.
	 */
	public static void assertEqualObjects(Object o1, Object o2, boolean sort) throws SerializeException {
		JsonSerializer s = (sort ? jsSorted : js);
		String s1 = s.serialize(o1);
		String s2 = s.serialize(o2);
		if (s1.equals(s2))
			return;
		throw new ComparisonFailure(null, s1, s2);
	}

	/**
	 * Validates that the whitespace is correct in the specified XML.
	 */
	public static void checkXmlWhitespace(String out) throws SerializeException {
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

	private static void printLines(String[] lines) {
		for (int i = 0; i < lines.length; i++)
			System.err.println(String.format("%4s:" + lines[i], i+1));  // NOT DEBUG
	}

	public static void validateXml(Object o) throws Exception {
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
			System.err.println("---XML---");  // NOT DEBUG
			System.err.println(xml);  // NOT DEBUG
			throw e;
		}
	}

	public static String readFile(String p) throws Exception {
		InputStream is = TestUtils.class.getResourceAsStream(p);
		if (is == null) {
			is = new FileInputStream(p);
		}
		try (InputStream is2 = is) {
			String e = read(is2);
			e = e.replaceAll("\r", "");
			return e;
		}
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String toHex(byte b) {
		char[] c = new char[2];
		int v = b & 0xFF;
		c[0] = hexArray[v >>> 4];
		c[1] = hexArray[v & 0x0F];
		return new String(c);
	}

	public static void debugOut(Object o) {
		try {
			System.err.println(decodeHex(SimpleJsonSerializer.DEFAULT.serialize(o)));  // NOT DEBUG
		} catch (SerializeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sort an XML document by element and attribute names.
	 * This method is primarily meant for debugging purposes.
	 */
	private static final String sortXml(String xml) throws Exception {

		xml = xml.replaceAll("\\w+\\:", "");  // Strip out all namespaces.

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(xml)));

		DOMSource s = new DOMSource(doc);

		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		XML_SORT_TRANSFORMER.transform(s, sr);
		return sw.toString().replace('"', '\'').replace("\r", "");
	}

	/**
	 * Compares two XML documents for equality.
	 * Namespaces are stripped from each and elements/servletAttributes are ordered in alphabetical order,
	 * 	then a simple string comparison is performed.
	 */
	public static final void assertXmlEquals(String expected, String actual) throws Exception {
		assertEquals(sortXml(expected), sortXml(actual));
	}

	private static Transformer XML_SORT_TRANSFORMER;
	static {
		try {
			String xsl = ""
				+ "	<xsl:stylesheet version='1.0'"
				+ "	 xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "	 <xsl:output omit-xml-declaration='yes' indent='yes'/>"
				+ "	 <xsl:strip-space elements='*'/>"
				+ "	 <xsl:template match='node()|@*'>"
				+ "	  <xsl:copy>"
				+ "	   <xsl:apply-templates select='@*'>"
				+ "	    <xsl:sort select='name()'/>"
				+ "	   </xsl:apply-templates>"
				+ "	   <xsl:apply-templates select='node()'>"
				+ "	    <xsl:sort select='name()'/>"
				+ "	    <xsl:sort select='text()'/>"
				+ "	   </xsl:apply-templates>"
				+ "	  </xsl:copy>"
				+ "	 </xsl:template>"
				+ "	</xsl:stylesheet>";
			TransformerFactory tf = TransformerFactory.newInstance();
			StreamSource ss = new StreamSource(new StringReader(xsl));
			XML_SORT_TRANSFORMER = tf.newTransformer(ss);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Assert that the object equals the specified string after running it through SimpleJsonSerializer.DEFAULT.toString().
	 */
	public static void assertObjectEquals(String s, Object o) {
		assertObjectEquals(s, o, js2);
	}

	/**
	 * Assert that the object equals the specified string after running it through SimpleJsonSerializer.DEFAULT.toString()
	 * with BEAN_sortProperties set to true.
	 */
	public static void assertSortedObjectEquals(String s, Object o) {
		assertObjectEquals(s, o, js3);
	}

	/**
	 * Assert that the object equals the specified string after running it through ws.toString().
	 */
	public static void assertObjectEquals(String s, Object o, WriterSerializer ws) {
		Assert.assertEquals(s, ws.toString(o));
	}

	/**
	 * Replaces all newlines with pipes, then compares the strings.
	 */
	public static void assertTextEquals(String s, Object o) {
		String s2 = o.toString().replaceAll("\\r?\\n", "|");
		Assert.assertEquals(s, s2);
	}

	/**
	 * Tries to turn the serialized output to a String.
	 * If it's a byte[], convert it to a UTF-8 encoded String.
	 */
	public static final String toString(Object o) {
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		if (o instanceof byte[])
			return new String((byte[])o, UTF8);
		return o.toString();
	}

	public static final void assertContains(Object value, String...substrings) {
		for (String substring : substrings)
			if (! contains(toString(value), substring)) {
				System.err.println("Text did not contain expected substring: '" + toString(substring) + "'");
				System.err.println("=== TEXT ===");
				System.err.println(toString(value));
				System.err.println("============");
				throw new ComparisonFailure("Text did not contain expected substring.", toString(substring), toString(value));
			}
	}
}
