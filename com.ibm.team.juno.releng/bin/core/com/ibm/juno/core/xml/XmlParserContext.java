/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
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
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class XmlParserContext extends ParserContext {

	/**
	 * XMLSchema namespace URI ({@link String}, default=<js>"http://www.w3.org/2001/XMLSchema-instance"</js>).
	 * <p>
	 * The XMLSchema namespace.
	 */
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

	/**
	 * Returns the {@link XmlParserProperties#XML_xsiNs} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_xsiNs} setting in this context.
	 */
	public String getXsiNs() {
		return xsiNs;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_trimWhitespace} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_trimWhitespace} setting in this context.
	 */
	public boolean isTrimWhitespace() {
		return trimWhitespace;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_preserveRootElement} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_preserveRootElement} setting in this context.
	 */
	public boolean isPreserveRootElement() {
		return preserveRootElement;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_validating} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_validating} setting in this context.
	 */
	public boolean isValidating() {
		return validating;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_coalescing} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_coalescing} setting in this context.
	 */
	public boolean isCoalescing() {
		return coalescing;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_replaceEntityReferences} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_replaceEntityReferences} setting in this context.
	 */
	public boolean isReplaceEntityReferences() {
		return replaceEntityReferences;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_reporter} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_reporter} setting in this context.
	 */
	public XMLReporter getReporter() {
		return reporter;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_resolver} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_resolver} setting in this context.
	 */
	public XMLResolver getResolver() {
		return resolver;
	}

	/**
	 * Returns the {@link XmlParserProperties#XML_eventAllocator} setting in this context.
	 *
	 * @return The {@link XmlParserProperties#XML_eventAllocator} setting in this context.
	 */
	public XMLEventAllocator getEventAllocator() {
		return eventAllocator;
	}


	/**
	 * Wrap the specified reader in a STAX reader based on settings in this context.
	 *
	 * @param r The input reader.
	 * @param estimatedSize The estimated size of the contents of the reader.
	 * @return The new STAX reader.
	 * @throws ParseException If problem occurred trying to create reader.
	 */
	public XMLStreamReader getReader(Reader r, int estimatedSize) throws ParseException {
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
	 * Silently closes the XML stream returned by the call to {@link #getReader(Reader, int)}.
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
