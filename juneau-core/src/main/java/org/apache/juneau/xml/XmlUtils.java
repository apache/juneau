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
package org.apache.juneau.xml;

import static javax.xml.stream.XMLStreamConstants.*;

import java.io.*;
import java.util.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * XML utility methods.
 */
public final class XmlUtils {

	//--------------------------------------------------------------------------------
	// Encode URI part
	//--------------------------------------------------------------------------------

	/**
	 * Encodes invalid XML text characters.
	 * <p>
	 * Encodes <js>'&'</js>, <js>'&lt;'</js>, and <js>'&gt;'</js> as XML entities.<br>
	 * Encodes any other invalid XML text characters to <code>_x####_</code> sequences.
	 *
	 * @param o The object being encoded.
	 * @return The encoded string.
	 */
	public static final String encodeText(Object o) {

		if (o == null)
			return "_x0000_";

		String s = o.toString();

		try {
			if (needsTextEncoding(s))
				return encodeTextInner(new StringBuilderWriter(s.length()*2), s).toString();
		} catch (IOException e) {
			throw new RuntimeException(e); // Never happens
		}

		return s;
	}

	/**
	 * Same as {@link #encodeText(Object)}, but does not convert <js>'&'</js>, <js>'&lt;'</js>, and <js>'&gt;'</js>
	 * 	to entities.
	 *
	 * @param o The object being encoded.
	 * @return The encoded string.
	 */
	public static final String encodeTextInvalidChars(Object o) {

		if (o == null)
			return "_x0000_";

		String s = o.toString();

		try {
			if (needsTextEncoding(s))
				return encodeTextInvalidCharsInner(new StringBuilderWriter(s.length()*2), s).toString();
		} catch (IOException e) {
			throw new RuntimeException(e); // Never happens
		}

		return s;
	}

	/**
	 * Encodes any invalid XML text characters to <code>_x####_</code> sequences and sends the response
	 * 	to the specified writer.
	 *
	 * @param w The writer to send the output to.
	 * @param o The object being encoded.
	 * @return The same writer passed in.
	 * @throws IOException Thrown from the writer.
	 */
	public static final Writer encodeText(Writer w, Object o) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();

		if (needsTextEncoding(s))
			return encodeTextInner(w, s);

		w.append(s);

		return w;
	}

	/**
	 * Same as {@link #encodeText(Object)}, but does not convert <js>'&'</js>, <js>'&lt;'</js>, and <js>'&gt;'</js>
	 * 	to entities.
	 *
	 * @param w The writer to write to.
	 * @param o The object being encoded.
	 * @return The encoded string.
	 * @throws IOException
	 */
	public static final Writer encodeTextInvalidChars(Writer w, Object o) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();

		if (needsTextEncoding(s))
			return encodeTextInvalidCharsInner(w, s);

		w.append(s);

		return w;
	}

	/**
	 * Same as {@link #encodeText(Object)}, but only converts <js>'&'</js>, <js>'&lt;'</js>, and <js>'&gt;'</js>
	 * 	to entities.
	 *
	 * @param w The writer to write to.
	 * @param o The object being encoded.
	 * @return The encoded string.
	 * @throws IOException
	 */
	public static final Writer encodeTextXmlChars(Writer w, Object o) throws IOException {
		if (o == null)
			return w;

		String s = o.toString();

		if (needsTextEncoding(s))
			return encodeTextXmlCharsInner(w, s);

		w.append(s);

		return w;

	}

	private static final Writer encodeTextInner(Writer w, String s) throws IOException {
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c == '&')
				w.append("&amp;");
			else if (c == '<')
				w.append("&lt;");
			else if (c == '>')
				w.append("&gt;");
			else if (c == '_' && isEscapeSequence(s,i))
				appendPaddedHexChar(w, c);
			else if ((i == 0 || i == len-1) && Character.isWhitespace(c))
				appendPaddedHexChar(w, c);
			else if (isValidXmlCharacter(c))
				w.append(c);
			else if (c == 0x09 || c == 0x0A || c == 0x0D)
				w.append("&#x000").append(Integer.toHexString(c)).append(";");
			else
				appendPaddedHexChar(w, c);
		}
		return w;
	}

	private static final Writer encodeTextInvalidCharsInner(Writer w, String s) throws IOException {
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if ((i == 0 || i == len-1) && Character.isWhitespace(c))
				appendPaddedHexChar(w, c);
			else if (c == '_' && isEscapeSequence(s,i))
				appendPaddedHexChar(w, c);
			else if (isValidXmlCharacter(c))
				w.append(c);
			else
				appendPaddedHexChar(w, c);
		}
		return w;
	}

	private static final Writer encodeTextXmlCharsInner(Writer w, String s) throws IOException {
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c == '&')
				w.append("&amp;");
			else if (c == '<')
				w.append("&lt;");
			else if (c == '>')
				w.append("&gt;");
			else
				w.append(c);
		}
		return w;
	}

	private static final boolean needsTextEncoding(String s) {
		// See if we need to convert the string.
		// Conversion is somewhat expensive, so make sure we need to do so before hand.
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if ((i == 0 || i == len-1) && Character.isWhitespace(c))
				return true;
			if (c == '&' || c == '<' || c == '>' || c == '\n' || ! isValidXmlCharacter(c) || (c == '_' && isEscapeSequence(s,i)))
				return true;
		}
		return false;
	}


	//--------------------------------------------------------------------------------
	// Decode XML text
	//--------------------------------------------------------------------------------

	/**
	 * Translates any _x####_ sequences (introduced by the various encode methods) back into their original characters.
	 *
	 * @param s The string being decoded.
	 * @param sb The string builder to use as a scratch pad.
	 * @return The decoded string.
	 */
	public static final String decode(String s, StringBuilder sb) {
		if (s == null) return null;
		if (s.length() == 0)
			return s;
		if (s.indexOf('_') == -1)
			return s;

		if (sb == null)
			sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '_' && isEscapeSequence(s,i)) {

				int x = Integer.parseInt(s.substring(i+2, i+6), 16);

				// If we find _x0000_, then that means a null.
				if (x == 0)
					return null;

				sb.append((char)x);
				i+=6;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}


	//--------------------------------------------------------------------------------
	// Encode XML attributes
	//--------------------------------------------------------------------------------

	/**
	 * Serializes and encodes the specified object as valid XML attribute name.
	 *
	 * @param w The writer to send the output to.
	 * @param o The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public static final Writer encodeAttr(Writer w, Object o) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();

		if (needsAttributeEncoding(s))
			return encodeAttrInner(w, s);

		w.append(s);
		return w;
	}

	private static final Writer encodeAttrInner(Writer w, String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&')
				w.append("&amp;");
			else if (c == '<')
				w.append("&lt;");
			else if (c == '>')
				w.append("&gt;");
			else if (c == '\'')
				w.append("&apos;");
			else if (c == '"')
				w.append("&quot;");
			else if (c == '_' && isEscapeSequence(s,i))
				appendPaddedHexChar(w, c);
			else if (isValidXmlCharacter(c))
				w.append(c);
			else
				appendPaddedHexChar(w, c);
		}
		return w;
	}

	private static boolean needsAttributeEncoding(String s) {
		// See if we need to convert the string.
		// Conversion is somewhat expensive, so make sure we need to do so before hand.
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&' || c == '<' || c == '>' || c == '\n' || c == '\'' || c == '"' || ! isValidXmlCharacter(c))
				return true;
		}
		return false;
	}


	//--------------------------------------------------------------------------------
	// Encode XML element names
	//--------------------------------------------------------------------------------

	/**
	 * Encodes any invalid XML element name characters to <code>_x####_</code> sequences.
	 *
	 * @param w The writer to send the output to.
	 * @param o The object being encoded.
	 * @return The same writer passed in.
	 * @throws IOException Throw by the writer.
	 */
	public static final Writer encodeElementName(Writer w, Object o) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();

		if (needsElementNameEncoding(s))
			return encodeElementNameInner(w, s);

		w.append(s);
		return w;
	}

	/**
	 * Encodes any invalid XML element name characters to <code>_x####_</code> sequences.
	 *
	 * @param o The object being encoded.
	 * @return The encoded element name string.
	 */
	public static final String encodeElementName(Object o) {
		if (o == null)
			return "_x0000_";

		String s = o.toString();

		try {
			if (needsElementNameEncoding(s))
				return encodeElementNameInner(new StringBuilderWriter(s.length() * 2), s).toString();
		} catch (IOException e) {
			throw new RuntimeException(e); // Never happens
		}

		return s;
	}

	private static final Writer encodeElementNameInner(Writer w, String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ((c >= 'A' && c <= 'Z')
					|| (c == '_' && ! isEscapeSequence(s,i))
					|| (c >= 'a' && c <= 'z')
					|| (i != 0 && (
							c == '-'
							|| c == '.'
							|| (c >= '0' && c <= '9')
							|| c == '\u00b7'
							|| (c >= '\u0300' && c <= '\u036f')
							|| (c >= '\u203f' && c <= '\u2040')
						))
					|| (c >= '\u00c0' && c <= '\u00d6')
					|| (c >= '\u00d8' && c <= '\u00f6')
					|| (c >= '\u00f8' && c <= '\u02ff')
					|| (c >= '\u0370' && c <= '\u037d')
					|| (c >= '\u037f' && c <= '\u1fff')
					|| (c >= '\u200c' && c <= '\u200d')
					|| (c >= '\u2070' && c <= '\u218f')
					|| (c >= '\u2c00' && c <= '\u2fef')
					|| (c >= '\u3001' && c <= '\ud7ff')
					|| (c >= '\uf900' && c <= '\ufdcf')
					|| (c >= '\ufdf0' && c <= '\ufffd')) {
				w.append(c);
			}  else {
				appendPaddedHexChar(w, c);
			}
		}
		return w;
	}

	private static final boolean needsElementNameEncoding(String s) {
		// Note that this doesn't need to be perfect, just fast.
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (! (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'))
				return true;
			if (i == 0 && (c >= '0' && c <= '9'))
				return true;
		}
		return false;
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Utility method for reading XML mixed content from an XML element and returning it as text.
	 *
	 * @param r The reader to read from.
	 * @return The contents read as a string.
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	public static String readXmlContents(XMLStreamReader r) throws XMLStreamException, IOException {
		StringWriter sw = new StringWriter();
		XmlWriter w = new XmlWriter(sw, false, false, '"', null, null, false, null);
		try {
			int depth = 0;
			do {
				int event = r.next();
				if (event == START_ELEMENT) {
					depth++;
					QName n = r.getName();
					w.oTag(n.getPrefix(), n.getLocalPart());
					for (int i = 0; i < r.getNamespaceCount(); i++)
						w.attr(r.getNamespacePrefix(i), "xmlns", r.getNamespaceURI(i));
					for (int i = 0; i < r.getAttributeCount(); i++)
						w.attr(r.getAttributePrefix(i), r.getAttributeLocalName(i), r.getAttributeValue(i));
					w.append('>');
				} else if (r.hasText()) {
					w.encodeTextXmlChars(r.getText());
				} else if (event == ATTRIBUTE) {
					// attributes handled above.
				} else if (event == END_ELEMENT) {
					QName n = r.getName();
					if (depth > 0)
						w.eTag(n.getPrefix(), n.getLocalPart());
					depth--;
				}
				if (depth < 0)
					return sw.toString();
			} while (true);
		} finally {
			w.close();
		}
	}

	// Returns true if the specified character can safely be used in XML text or an attribute.
	private static final boolean isValidXmlCharacter(char c) {
		return (c >= 0x20 && c <= 0xD7FF) /*|| c == 0xA || c == 0xD*/ || (c >= 0xE000 && c <= 0xFFFD);
	}

	// Returns true if the string at the specified position is of the form "_x####_"
	// where '#' are hexadecimal characters.
	private static final boolean isEscapeSequence(String s, int i) {
		return s.length() > i+6
			&& s.charAt(i) == '_'
			&& s.charAt(i+1) == 'x'
			&& isHexCharacter(s.charAt(i+2))
			&& isHexCharacter(s.charAt(i+3))
			&& isHexCharacter(s.charAt(i+4))
			&& isHexCharacter(s.charAt(i+5))
			&& s.charAt(i+6) == '_';
	}

	// Returns true if the character is a hexadecimal character
	private static final boolean isHexCharacter(char c) {
		return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
	}

	// Converts an integer to a hexadecimal string padded to 4 places.
	private static final Writer appendPaddedHexChar(Writer out, int num) throws IOException {
		out.append("_x");
		char[] n = new char[4];
		int a = num%16;
		n[3] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		int base = 16;
		for (int i = 1; i < 4; i++) {
			a = (num/base)%16;
			base <<= 4;
			n[3-i] = (char)(a > 9 ? 'A'+a-10 : '0'+a);
		}
		for (int i = 0; i < 4; i++)
			out.append(n[i]);
		return out.append('_');
	}

	/**
	 * Find the namespace given a list of <ja>@Xml</ja> and <ja>@XmlSchema</ja> annotations.
	 * The annotations should be a child-to-parent ordering of annotations found on
	 * 	a class or method.
	 *
	 * @param xmls The list of <ja>@Xml</ja> annotations.
	 * @param schemas The list of <ja>@XmlSchema</ja> annotations.
	 * @return The namespace, or <jk>null</jk> if it couldn't be found.
	 */
	public static Namespace findNamespace(List<Xml> xmls, List<XmlSchema> schemas) {

		for (Xml xml : xmls) {
			Namespace ns = findNamespace(xml.prefix(), xml.namespace(), xmls, schemas);
			if (ns != null)
				return ns;
		}

		for (XmlSchema schema : schemas) {
			Namespace ns = findNamespace(schema.prefix(), schema.namespace(), null, schemas);
			if (ns != null)
				return ns;
		}

		return null;
	}

	private static Namespace findNamespace(String prefix, String ns, List<Xml> xmls, List<XmlSchema> schemas) {

		// If both prefix and namespace specified, use that Namespace mapping.
		if (! (prefix.isEmpty() || ns.isEmpty()))
			return NamespaceFactory.get(prefix, ns);

		// If only prefix specified, need to search for namespaceURI.
		if (! prefix.isEmpty()) {
			if (xmls != null)
				for (Xml xml2 : xmls)
					if (xml2.prefix().equals(prefix) && ! xml2.namespace().isEmpty())
						return NamespaceFactory.get(prefix, xml2.namespace());
			for (XmlSchema schema : schemas) {
				if (schema.prefix().equals(prefix) && ! schema.namespace().isEmpty())
					return NamespaceFactory.get(prefix, schema.namespace());
				for (XmlNs xmlNs : schema.xmlNs())
					if (xmlNs.prefix().equals(prefix))
						return NamespaceFactory.get(prefix, xmlNs.namespaceURI());
			}
			throw new BeanRuntimeException("Found @Xml.prefix annotation with no matching URI.  prefix='"+prefix+"'");
		}

		// If only namespaceURI specified, need to search for prefix.
		if (! ns.isEmpty()) {
			if (xmls != null)
				for (Xml xml2 : xmls)
					if (xml2.namespace().equals(ns) && ! xml2.prefix().isEmpty())
						return NamespaceFactory.get(xml2.prefix(), ns);
			for (XmlSchema schema : schemas) {
				if (schema.namespace().equals(ns) && ! schema.prefix().isEmpty())
					return NamespaceFactory.get(schema.prefix(), ns);
				for (XmlNs xmlNs : schema.xmlNs())
					if (xmlNs.namespaceURI().equals(ns))
						return NamespaceFactory.get(xmlNs.prefix(), ns);
			}
		}

		return null;
	}

	/**
	 * Utility method that converts the current event on the XML stream to something human-readable for debug purposes.
	 *
	 * @param r The XML stream reader whose current event is to be converted to a readable string.
	 * @return The event in human-readable form.
	 */
	public static final String toReadableEvent(XMLStreamReader r) {
		int t = r.getEventType();
		if (t == 1)
			return "<"+r.getLocalName()+">";
		if (t == 2)
			return "</"+r.getLocalName()+">";
		if (t == 3)
			return "PROCESSING_INSTRUCTION";
		if (t == 4)
			return "CHARACTERS=[" + r.getText() + "]";
		if (t == 5)
			return "COMMENTS=[" + r.getText() + "]";
		if (t == 6)
			return "SPACE=[" + r.getText() + "]";
		if (t == 7)
			return "START_DOCUMENT";
		if (t == 8)
			return "END_DOCUMENT";
		if (t == 9)
			return "ENTITY_REFERENCE";
		if (t == 10)
			return "ATTRIBUTE";
		if (t == 11)
			return "DTD";
		if (t == 12)
			return "CDATA=["+r.getText()+"]";
		if (t == 13)
			return "NAMESPACE";
		if (t == 14)
			return "NOTATION_DECLARATION";
		if (t == 15)
			return "ENTITY_DECLARATION";
		return "UNKNOWN";
	}
}
