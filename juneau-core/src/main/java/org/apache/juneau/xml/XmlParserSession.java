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
import static org.apache.juneau.xml.XmlParserContext.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlParser}.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class XmlParserSession extends ParserSession {

	private final boolean
		validating,
		preserveRootElement;
	private final XMLReporter reporter;
	private final XMLResolver resolver;
	private final XMLEventAllocator eventAllocator;
	private XMLStreamReader xmlStreamReader;
	private final StringBuilder sb = new StringBuilder();  // Reusable string builder used in this class.

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param input
	 * 	The input.
	 * 	Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * @param op
	 * 	The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @param locale
	 * 	The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone
	 * 	The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	public XmlParserSession(XmlParserContext ctx, ObjectMap op, Object input, Method javaMethod, Object outer,
			Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
		if (op == null || op.isEmpty()) {
			validating = ctx.validating;
			reporter = ctx.reporter;
			resolver = ctx.resolver;
			eventAllocator = ctx.eventAllocator;
			preserveRootElement = ctx.preserveRootElement;
		} else {
			validating = op.getBoolean(XML_validating, ctx.validating);
			reporter = (XMLReporter)op.get(XML_reporter, ctx.reporter);
			resolver = (XMLResolver)op.get(XML_resolver, ctx.resolver);
			eventAllocator = (XMLEventAllocator)op.get(XML_eventAllocator, ctx.eventAllocator);
			preserveRootElement = op.getBoolean(XML_preserveRootElement, ctx.preserveRootElement);
		}
	}

	/**
	 * Returns the {@link XmlParserContext#XML_preserveRootElement} setting value for this session.
	 *
	 * @return The {@link XmlParserContext#XML_preserveRootElement} setting value for this session.
	 */
	public final boolean isPreserveRootElement() {
		return preserveRootElement;
	}

	/**
	 * Wrap the specified reader in a STAX reader based on settings in this context.
	 *
	 * @return The new STAX reader.
	 * @throws Exception If problem occurred trying to create reader.
	 */
	public final XMLStreamReader getXmlStreamReader() throws Exception {
		try {
			Reader r = getBufferedReader(getReader());
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_VALIDATING, validating);
			factory.setProperty(XMLInputFactory.IS_COALESCING, true);
			factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);  // This usually has no effect anyway.
			if (factory.isPropertySupported(XMLInputFactory.REPORTER) && reporter != null)
				factory.setProperty(XMLInputFactory.REPORTER, reporter);
			if (factory.isPropertySupported(XMLInputFactory.RESOLVER) && resolver != null)
				factory.setProperty(XMLInputFactory.RESOLVER, resolver);
			if (factory.isPropertySupported(XMLInputFactory.ALLOCATOR) && eventAllocator != null)
				factory.setProperty(XMLInputFactory.ALLOCATOR, eventAllocator);
			xmlStreamReader = factory.createXMLStreamReader(r);
			xmlStreamReader.nextTag();
		} catch (Error e) {
			close();
			throw new ParseException(e.getLocalizedMessage());
		} catch (XMLStreamException e) {
			close();
			throw new ParseException(e);
		}

		return xmlStreamReader;
	}

	/**
	 * Decodes and trims the specified string.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * @param s The string to be decoded.
	 * @return The decoded string.
	 */
	public final String decodeString(String s) {
		if (s == null)
			return null;
		sb.setLength(0);
		s = XmlUtils.decode(s, sb);
		if (isTrimStrings())
			s = s.trim();
		return s;
	}

	/**
	 * Returns the name of the current XML element.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * @param r The reader to read from.
	 * @return The decoded element name.
	 * @throws XMLStreamException
	 */
	public final String getElementName(XMLStreamReader r) throws XMLStreamException {
		return decodeString(r.getLocalName());
	}

	/**
	 * Returns the name of the specified attribute on the current XML element.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * @param r The reader to read from.
	 * @param i The attribute index.
	 * @return The decoded attribute name.
	 * @throws XMLStreamException
	 */
	public final String getAttributeName(XMLStreamReader r, int i) throws XMLStreamException {
		return decodeString(r.getAttributeLocalName(i));
	}

	/**
	 * Returns the value of the specified attribute on the current XML element.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * @param r The reader to read from.
	 * @param i The attribute index.
	 * @return The decoded attribute value.
	 * @throws XMLStreamException
	 */
	public final String getAttributeValue(XMLStreamReader r, int i) throws XMLStreamException {
		return decodeString(r.getAttributeValue(i));
	}

	/**
	 * Returns the text content of the current XML element.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * <p>
	 * Leading and trailing whitespace (unencoded) will be trimmed from the result.
	 *
	 * @param r The reader to read the element text from.
	 * @return The decoded text.  <jk>null</jk> if the text consists of the sequence <js>'_x0000_'</js>.
	 * @throws XMLStreamException
	 */
	public String getElementText(XMLStreamReader r) throws XMLStreamException {
		String s = r.getElementText().trim();
		return decodeString(s);
	}

	/**
	 * Returns the content of the current CHARACTERS node.
	 *
	 * <p>
	 * Any <js>'_x####_'</js> sequences in the string will be decoded.
	 *
	 * <p>
	 * Leading and trailing whitespace (unencoded) will be trimmed from the result.
	 *
	 * @param r The reader to read the element text from.
	 * @param trim
	 * 	If <jk>true</jk>, trim the contents of the text node BEFORE decoding escape sequences.
	 * 	Typically <jk>true</jk> for {@link XmlFormat#MIXED_PWS} and {@link XmlFormat#TEXT_PWS}.
	 * @return The decoded text.  <jk>null</jk> if the text consists of the sequence <js>'_x0000_'</js>.
	 * @throws XMLStreamException
	 */
	public String getText(XMLStreamReader r, boolean trim) throws XMLStreamException {
		String s = r.getText();
		if (trim)
			s = s.trim();
		if (s.isEmpty())
			return null;
		return decodeString(s);
	}

	/**
	 * Shortcut for calling <code>getText(r, <jk>true</jk>);</code>.
	 *
	 * @param r The reader to read the element text from.
	 * @return The decoded text.  <jk>null</jk> if the text consists of the sequence <js>'_x0000_'</js>.
	 * @throws XMLStreamException
	 */
	public String getText(XMLStreamReader r) throws XMLStreamException {
		return getText(r, true);
	}

	/**
	 * Takes the element being read from the XML stream reader and reconstructs it as XML.
	 *
	 * <p>
	 * Used when reconstructing bean properties of type {@link XmlFormat#XMLTEXT}.
	 *
	 * @param r The XML stream reader to read the current event from.
	 * @return The event as XML.
	 * @throws RuntimeException if the event is not a start or end tag.
	 */
	public final String getElementAsString(XMLStreamReader r) {
		int t = r.getEventType();
		if (t > 2)
			throw new FormattedRuntimeException("Invalid event type on stream reader for elementToString() method: ''{0}''", XmlUtils.toReadableEvent(r));
		sb.setLength(0);
		sb.append("<").append(t == 1 ? "" : "/").append(r.getLocalName());
		if (t == 1)
			for (int i = 0; i < r.getAttributeCount(); i++)
				sb.append(' ').append(r.getAttributeName(i)).append('=').append('\'').append(r.getAttributeValue(i)).append('\'');
		sb.append('>');
		return sb.toString();
	}

	/**
	 * Parses the current element as text.
	 *
	 * <p>
	 * Note that this is different than {@link #getText(XMLStreamReader)} since it assumes that we're pointing to a
	 * whitespace element.
	 *
	 * @param r
	 * @return The parsed text.
	 * @throws XMLStreamException
	 */
	public String parseText(XMLStreamReader r) throws XMLStreamException {
		StringBuilder sb2 = getStringBuilder();

		int depth = 0;
		while (true) {
			int et = r.getEventType();
			if (et == START_ELEMENT) {
				sb2.append(getElementAsString(r));
				depth++;
			} else if (et == CHARACTERS) {
				sb2.append(getText(r));
			} else if (et == END_ELEMENT) {
				sb2.append(getElementAsString(r));
				depth--;
				if (depth <= 0)
					break;
			}
			et = r.next();
		}
		String s = sb2.toString();
		returnStringBuilder(sb2);
		return s;
	}

	/**
	 * Returns <jk>true</jk> if the current element is a whitespace element.
	 *
	 * <p>
	 * For the XML parser, this always returns <jk>false</jk>.
	 * However, the HTML parser defines various whitespace elements such as <js>"br"</js> and <js>"sp"</js>.
	 *
	 * @param r The XML stream reader to read the current event from.
	 * @return <jk>true</jk> if the current element is a whitespace element.
	 */
	public boolean isWhitespaceElement(XMLStreamReader r) {
		return false;
	}

	/**
	 * Parses the current whitespace element.
	 *
	 * <p>
	 * For the XML parser, this always returns <jk>null</jk> since there is no concept of a whitespace element.
	 * However, the HTML parser defines various whitespace elements such as <js>"br"</js> and <js>"sp"</js>.
	 *
	 * @param r The XML stream reader to read the current event from.
	 * @return The whitespace character or characters.
	 * @throws XMLStreamException
	 */
	public String parseWhitespaceElement(XMLStreamReader r) throws XMLStreamException {
		return null;
	}

	/**
	 * Silently closes the XML stream.
	 */
	@Override /* ParserContext */
	public boolean close() {
		if (super.close()) {
			try {
				if (xmlStreamReader != null)
					xmlStreamReader.close();
			} catch (XMLStreamException e) {
				// Ignore.
			}
			return true;
		}
		return false;
	}
}
