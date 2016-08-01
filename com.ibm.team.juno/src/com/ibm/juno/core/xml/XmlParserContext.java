/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import static com.ibm.juno.core.xml.XmlParserProperties.*;

import java.io.*;
import java.lang.reflect.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

/**
 * Context object that lives for the duration of a single parsing of {@link XmlParser}.
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class XmlParserContext extends ParserContext {

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
	 * Create a new parser context with the specified options.
	 *
	 * @param beanContext The bean context being used.
	 * @param pp The default parser properties.
	 * @param xpp The default XML parser properties.
	 * @param op The override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public XmlParserContext(BeanContext beanContext, ParserProperties pp, XmlParserProperties xpp, ObjectMap op, Method javaMethod, Object outer) {
		super(beanContext, pp, op, javaMethod, outer);
		if (op == null || op.isEmpty()) {
			xsiNs = xpp.getXsiNs();
			trimWhitespace = xpp.isTrimWhitespace();
			validating = xpp.isValidating();
			coalescing = xpp.isCoalescing();
			replaceEntityReferences = xpp.isReplaceEntityReferences();
			reporter = xpp.getReporter();
			resolver = xpp.getResolver();
			eventAllocator = xpp.getEventAllocator();
			preserveRootElement = xpp.isPreserveRootElement();
		} else {
			xsiNs = op.getString(XML_xsiNs, xpp.getXsiNs());
			trimWhitespace = op.getBoolean(XML_trimWhitespace, xpp.isTrimWhitespace());
			validating = op.getBoolean(XML_validating, xpp.isValidating());
			coalescing = op.getBoolean(XML_coalescing, xpp.isCoalescing());
			replaceEntityReferences = op.getBoolean(XML_replaceEntityReferences, xpp.isReplaceEntityReferences());
			reporter = (XMLReporter)op.get(XML_reporter, xpp.getReporter());
			resolver = (XMLResolver)op.get(XML_resolver, xpp.getResolver());
			eventAllocator = (XMLEventAllocator)op.get(XML_eventAllocator, xpp.getEventAllocator());
			preserveRootElement = op.getBoolean(XML_preserveRootElement, xpp.isPreserveRootElement());
		}
	}

	final String getXsiNs() {
		return xsiNs;
	}

	final boolean isPreserveRootElement() {
		return preserveRootElement;
	}

	/**
	 * Wrap the specified reader in a STAX reader based on settings in this context.
	 *
	 * @param r The input reader.
	 * @param estimatedSize The estimated size of the contents of the reader.
	 * @return The new STAX reader.
	 * @throws ParseException If problem occurred trying to create reader.
	 */
	final XMLStreamReader getReader(Reader r, int estimatedSize) throws ParseException {
		try {
			r = IOUtils.getBufferedReader(r, estimatedSize);
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
	 */
	final String decodeString(String s) {
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
	 * 	Unlike <code>decodeString(String)</code>, the input string is ALWAYS trimmed before decoding, and
	 * 	NEVER trimmed after decoding.
	 * </p>
	 *
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	final String decodeLiteral(String s) {
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
