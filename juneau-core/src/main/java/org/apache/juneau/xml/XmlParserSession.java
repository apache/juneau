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

import static org.apache.juneau.xml.XmlParserContext.*;

import java.io.*;
import java.lang.reflect.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlParser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class XmlParserSession extends ParserSession {

	private final String xsiNs;
	private final boolean
		trimWhitespace,
		validating,
		coalescing,
		replaceEntityReferences,
		preserveRootElement;
	private final XMLReporter reporter;
	private final XMLResolver resolver;
	private final XMLEventAllocator eventAllocator;
	private XMLStreamReader xmlStreamReader;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param input The input.  Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public XmlParserSession(XmlParserContext ctx, BeanContext beanContext, Object input, ObjectMap op, Method javaMethod, Object outer) {
		super(ctx, beanContext, input, op, javaMethod, outer);
		if (op == null || op.isEmpty()) {
			xsiNs = ctx.xsiNs;
			trimWhitespace = ctx.trimWhitespace;
			validating = ctx.validating;
			coalescing = ctx.coalescing;
			replaceEntityReferences = ctx.replaceEntityReferences;
			reporter = ctx.reporter;
			resolver = ctx.resolver;
			eventAllocator = ctx.eventAllocator;
			preserveRootElement = ctx.preserveRootElement;
		} else {
			xsiNs = op.getString(XML_xsiNs, ctx.xsiNs);
			trimWhitespace = op.getBoolean(XML_trimWhitespace, ctx.trimWhitespace);
			validating = op.getBoolean(XML_validating, ctx.validating);
			coalescing = op.getBoolean(XML_coalescing, ctx.coalescing);
			replaceEntityReferences = op.getBoolean(XML_replaceEntityReferences, ctx.replaceEntityReferences);
			reporter = (XMLReporter)op.get(XML_reporter, ctx.reporter);
			resolver = (XMLResolver)op.get(XML_resolver, ctx.resolver);
			eventAllocator = (XMLEventAllocator)op.get(XML_eventAllocator, ctx.eventAllocator);
			preserveRootElement = op.getBoolean(XML_preserveRootElement, ctx.preserveRootElement);
		}
	}

	/**
	 * Returns the {@link XmlParserContext#XML_xsiNs} setting value for this session.
	 *
	 * @return The {@link XmlParserContext#XML_xsiNs} setting value for this session.
	 */
	public final String getXsiNs() {
		return xsiNs;
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
			Reader r = IOUtils.getBufferedReader(getReader());
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_VALIDATING, validating);
			factory.setProperty(XMLInputFactory.IS_COALESCING, coalescing);
			factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, replaceEntityReferences);
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
	 * @param s The string to be decoded.
	 * @return The decoded string.
	 */
	public final String decodeString(String s) {
		if (s == null || s.isEmpty())
			return s;
		if (trimWhitespace)
			s = s.trim();
		s = XmlUtils.decode(s);
		if (isTrimStrings())
			s = s.trim();
		return s;
	}

	/**
	 * Decodes the specified literal (e.g. <js>"true"</js>, <js>"123"</js>).
	 * <p>
	 * Unlike <code>decodeString(String)</code>, the input string is ALWAYS trimmed before decoding, and
	 * 	NEVER trimmed after decoding.
	 *
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public final String decodeLiteral(String s) {
		if (s == null || s.isEmpty())
			return s;
		s = s.trim();
		s = XmlUtils.decode(s);
		return s;
	}

	/**
	 * Silently closes the XML stream.
	 */
	@Override /* ParserContext */
	public void close() throws ParseException {
		super.close();
		try {
			if (xmlStreamReader != null)
				xmlStreamReader.close();
		} catch (XMLStreamException e) {
			// Ignore.
		}
	}
}
