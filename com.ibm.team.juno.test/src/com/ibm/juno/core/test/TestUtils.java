/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static com.ibm.juno.core.xml.XmlSerializerProperties.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;

import org.junit.*;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.*;
import org.w3c.dom.ls.*;
import org.xml.sax.*;

import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

public class TestUtils {
	
	private static JsonSerializer js = new JsonSerializer.Simple()
		.setProperty(SERIALIZER_trimNullProperties, false);

	private static JsonSerializer jsSorted = new JsonSerializer.Simple()
		.setProperty(SERIALIZER_sortCollections, true)
		.setProperty(SERIALIZER_sortMaps, true)
		.setProperty(SERIALIZER_trimNullProperties, false);

	private static JsonSerializer js2 = new JsonSerializer.Simple()
		.addFilters(IteratorFilter.class, EnumerationFilter.class);

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
			System.err.println(String.format("%4s:" + lines[i], i+1));
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
							throw new RuntimeException(MessageFormat.format("No schema found for namespaceURI ''{0}''", namespaceURI));
					
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
		validateXml(o, XmlSerializer.DEFAULT_SQ);
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.clone().setProperty(SERIALIZER_useIndentation, true).setProperty(XML_enableNamespaces, true).setProperty(XML_addNamespaceUrisToRoot, true);
		String xml = s.serialize(o);
		
		String xmlSchema = null;
		try {
			xmlSchema = s.getSchemaSerializer().serialize(o);
			TestUtils.checkXmlWhitespace(xml);
			TestUtils.checkXmlWhitespace(xmlSchema);
			TestUtils.validateXml(xml, xmlSchema);
		} catch (Exception e) {
			System.err.println("---XML---");
			System.err.println(xml);
			System.err.println("---XMLSchema---");
			System.err.println(xmlSchema);
			throw e;
		}
	}
	
	public static String readFile(String p) throws Exception {
		InputStream is = TestUtils.class.getResourceAsStream(p);
		if (is == null)
			throw new FileNotFoundException(p);
		String e = IOUtils.read(is);
		e = e.replaceAll("\r", "");
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
			System.err.println(StringUtils.decodeHex(JsonSerializer.DEFAULT_LAX.serialize(o)));
		} catch (SerializeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sort an XML document by element and attribute names.
	 * This method is primarily meant for debugging purposes.
	 */
	public static final String sortXml(String xml) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(xml)));

		DOMSource s = new DOMSource(doc);
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		XML_SORT_TRANSFORMER.transform(s, sr);
		return sw.toString().replace('"', '\'').replace("\r", "");
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
			XML_SORT_TRANSFORMER.setOutputProperty(OutputKeys.INDENT, "yes");
			XML_SORT_TRANSFORMER.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Assert that the object equals the specified string after running it through JsonSerializer.DEFAULT_LAX.toString().
	 */
	public static void assertObjectEquals(String s, Object o) {
		assertObjectEquals(s, o, js2);
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
}
