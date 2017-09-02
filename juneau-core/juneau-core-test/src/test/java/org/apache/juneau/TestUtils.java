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
package org.apache.juneau;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.*;
import org.w3c.dom.ls.*;
import org.xml.sax.*;

@SuppressWarnings({"javadoc"})
public class TestUtils {

	private static JsonSerializer js = new JsonSerializerBuilder()
		.simple()
		.trimNullProperties(false)
		.build();

	private static JsonSerializer jsSorted = new JsonSerializerBuilder()
		.simple()
		.sortCollections(true)
		.sortMaps(true)
		.trimNullProperties(false)
		.build();


	private static JsonSerializer js2 = new JsonSerializerBuilder()
		.simple()
		.pojoSwaps(IteratorSwap.class, EnumerationSwap.class)
		.build();

	private static JsonSerializer js3 = new JsonSerializerBuilder()
		.simple()
		.pojoSwaps(IteratorSwap.class, EnumerationSwap.class)
		.sortProperties(true)
		.build();

	private static final BeanSession beanSession = BeanContext.DEFAULT.createSession();

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
			System.err.println(String.format("%4s:" + lines[i], i+1)); // NOT DEBUG
	}

	/**
	 * Validates that the specified XML conforms to the specified schema.
	 */
	private static void validateXml(String xml, String xmlSchema) throws Exception {
		// parse an XML document into a DOM tree
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		DocumentBuilder documentBuilder = f.newDocumentBuilder();
		Document document = documentBuilder.parse(new InputSource(new StringReader(xml)));

		// create a SchemaFactory capable of understanding WXS schemas
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		if (xmlSchema.indexOf('\u0000') != -1) {

			// Break it up into a map of namespaceURI->schema document
			final Map<String,String> schemas = new HashMap<String,String>();
			String[] ss = xmlSchema.split("\u0000");
			xmlSchema = ss[0];
			for (String s : ss) {
				Matcher m = pTargetNs.matcher(s);
				if (m.find())
					schemas.put(m.group(1), s);
			}

			// Create a custom resolver
			factory.setResourceResolver(
				new LSResourceResolver() {

					@Override /* LSResourceResolver */
					public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {

						String schema = schemas.get(namespaceURI);
						if (schema == null)
							throw new FormattedRuntimeException("No schema found for namespaceURI ''{0}''", namespaceURI);

						try {
							DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
							DOMImplementationLS domImplementationLS = (DOMImplementationLS)registry.getDOMImplementation("LS 3.0");
							LSInput in = domImplementationLS.createLSInput();
							in.setCharacterStream(new StringReader(schema));
							in.setSystemId(systemId);
							return in;

						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			);
		}

		Schema schema = factory.newSchema(new StreamSource(new StringReader(xmlSchema)));

		// create a Validator instance, which can be used to validate an instance document
		Validator validator = schema.newValidator();

		// validate the DOM tree
		validator.validate(new DOMSource(document));
	}

	private static Pattern pTargetNs = Pattern.compile("targetNamespace=['\"]([^'\"]+)['\"]");

	public static void validateXml(Object o) throws Exception {
		validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.builder().ws().ns().addNamespaceUrisToRoot(true).build();
		String xml = s.serialize(o);

		String xmlSchema = null;
		try {
			xmlSchema = s.getSchemaSerializer().serialize(o);
			TestUtils.checkXmlWhitespace(xml);
			TestUtils.checkXmlWhitespace(xmlSchema);
			TestUtils.validateXml(xml, xmlSchema);
		} catch (Exception e) {
			System.err.println("---XML---");       // NOT DEBUG
			System.err.println(xml);               // NOT DEBUG
			System.err.println("---XMLSchema---"); // NOT DEBUG
			System.err.println(xmlSchema);         // NOT DEBUG
			throw e;
		}
	}

	/**
	 * Reads the specified file at the specified path.
	 * Removes '\r' characters.
	 * Remove license headers.
	 */
	public static String readFile(String path) throws Exception {
		InputStream is = TestUtils.class.getResourceAsStream(path);
		if (is == null) {
			is = new FileInputStream(path);
		}
		String e = read(is);
		e = e.replaceAll("\r", "");
		if (path.endsWith(".xml")) {
			e = e.replaceAll("(?s)\\<\\!\\-\\-(.*)\\-\\-\\>\\s*", "");
			e = e.replaceAll("\\<\\?.*\\?\\>\\s*", "");
		} else if (path.endsWith(".json")) {
			e = e.replaceAll("\\/\\/ \\*.*\\s*", "");
		}
		return e;
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
			System.err.println(decodeHex(JsonSerializer.DEFAULT_LAX.serialize(o))); // NOT DEBUG
		} catch (SerializeException e) {
			e.printStackTrace();
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
		public List<SortedNode> children = new LinkedList<SortedNode>();

		SortedNode(Element e) {
			this.name = e.getNodeName();
			NamedNodeMap attrs = e.getAttributes();
			if (attrs != null) {
				StringBuilder sb = new StringBuilder();
				Set<String> attrNames = new TreeSet<String>();
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

	/**
	 * Assert that the object equals the specified string after running it through JsonSerializer.DEFAULT_LAX.toString().
	 */
	public static void assertObjectEquals(String s, Object o) {
		assertObjectEquals(s, o, js2);
	}

	/**
	 * Assert that the object equals the specified string after running it through JsonSerializer.DEFAULT_LAX.toString()
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

	public static String toReadableBytes(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (byte b2 : b)
			sb.append((b2 < ' ' || b2 > 'z') ? String.format("[%02X]", b2) : (char)b2 + "   ");
		sb.append("\n");
		for (byte b2 : b)
			sb.append(String.format("[%02X]", b2));
		return sb.toString();
	}

	public static String toReadableBytes2(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (byte b2 : b)
			sb.append(String.format("%02X ", b2));
		return sb.toString().trim();
	}

	/**
	 * Tries to turn the serialized output to a String.
	 * If it's a byte[], convert it to a UTF-8 encoded String.
	 */
	public static String toString(Object o) {
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		if (o instanceof byte[])
			return new String((byte[])o, UTF8);
		return o.toString();
	}

	private static ThreadLocal<TimeZone> systemTimeZone = new ThreadLocal<TimeZone>();
	private static ThreadLocal<Locale> systemLocale = new ThreadLocal<Locale>();

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 * Use {@link #unsetTimeZone()} to unset it.
	 *
	 * @param name
	 */
	public static void setTimeZone(String name) {
		systemTimeZone.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(name));
	}

	public static void unsetTimeZone() {
		TimeZone.setDefault(systemTimeZone.get());
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 * Use {@link #unsetLocale()} to unset it.
	 *
	 * @param name
	 */
	public static void setLocale(Locale locale) {
		systemLocale.set(Locale.getDefault());
		Locale.setDefault(locale);
	}

	public static void unsetLocale() {
		Locale.setDefault(systemLocale.get());
	}

	public static void assertEqualsAfterSort(String expected, String actual, String msg, Object...args) {
		String[] e = expected.trim().split("\n"), a = actual.trim().split("\n");

		if (e.length != a.length)
			throw new ComparisonFailure(format(msg, args), expected, actual);

		Arrays.sort(e);
		Arrays.sort(a);

		for (int i = 0; i < e.length; i++)
			if (! e[i].equals(a[i]))
				throw new ComparisonFailure(format(msg, args), expected, actual);
	}

	/**
	 * Same as {@link Assert#assertEquals(String,String,String) except takes in a MessageFormat-style message.
	 */
	public static void assertEquals(Object expected, Object actual, String msg, Object...args) {
		if ("xxx".equals(expected))
			System.err.println("actual=["+actual+"]");
		if (! isEquals(expected, actual))
			throw new ComparisonFailure(format(msg, args), toString(expected), toString(actual));
	}

	/**
	 * Creates a ClassMeta for the given types.
	 */
	public static Type getType(Type type, Type...args) {
		return beanSession.getClassMeta(type, args);
	}

	/**
	 * Throws an AssertionError if the object isn't of the specified type.
	 */
	public static void assertType(Class<?> type, Object o) {
		if (type.isInstance(o))
			return;
		throw new AssertionError(new StringMessage("Expected type {0} but was {1}", type, (o == null ? null : o.getClass())));
	}

	private static boolean isEquals(Object o1, Object o2) {
		if (o1 == null)
			return o2 == null;
		if (o2 == null)
			return false;
		return o1.equals(o2);
	}
}
