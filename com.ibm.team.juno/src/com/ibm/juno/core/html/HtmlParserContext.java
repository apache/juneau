/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.html;

import java.io.*;
import java.lang.reflect.*;

import javax.xml.stream.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

/**
 * Context object that lives for the duration of a single parsing of {@link HtmlParser}.
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class HtmlParserContext extends ParserContext {

	private XMLEventReader xmlEventReader;

	/**
	 * Create a new parser context with the specified options.
	 *
	 * @param beanContext The bean context being used.
	 * @param pp The default generic parser properties.
	 * @param hpp The default HTML parser properties.
	 * @param properties The override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	public HtmlParserContext(BeanContext beanContext, ParserProperties pp, HtmlParserProperties hpp, ObjectMap properties, Method javaMethod, Object outer) {
		super(beanContext, pp, properties, javaMethod, outer);
	}

	/**
	 * Wraps the specified reader in an {@link XMLEventReader}.
	 * This event reader gets closed by the {@link #close()} method.
	 *
	 * @param in The reader to read from.
	 * @param estimatedSize The estimated size of the input.  If <code>-1</code>, uses a default size of <code>8196</code>.
	 * @return A new XML event reader using a new {@link XMLInputFactory}.
	 * @throws ParseException
	 */
	final XMLEventReader getReader(Reader in, int estimatedSize) throws ParseException {
		try {
			in = IOUtils.getBufferedReader(in, estimatedSize);
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
			this.xmlEventReader = factory.createXMLEventReader(in);
		} catch (Error e) {
			throw new ParseException(e.getLocalizedMessage());
		} catch (XMLStreamException e) {
			throw new ParseException(e);
		}
		return xmlEventReader;
	}

	@Override /* ParserContext */
	public void close() throws ParseException {
		if (xmlEventReader != null) {
			try {
				xmlEventReader.close();
			} catch (XMLStreamException e) {
				throw new ParseException(e);
			}
		}
		super.close();
	}
}
