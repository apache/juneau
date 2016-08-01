/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static javax.xml.stream.XMLStreamConstants.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.xml.*;

/**
 * Used to validate that the syntax of XML documents are valid.
 */
@SuppressWarnings("unchecked")
public class XmlValidatorParser extends XmlParser {

	public XmlValidatorParser() {
		super();
	}

	@Override /* Parser */
	protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
		return (T)validate(in);
	}

	public <T> T validate(Reader r) throws ParseException {
		try {
			XMLStreamReader sr = getStaxReader(r);
			while(sr.next() != END_DOCUMENT){}
			return null;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(Reader in, int estimatedSize, Map<K,V> m, Type keyType, Type valueType, ParserContext ctx) throws ParseException, IOException {
		return (Map<K,V>)validate(in);
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(Reader in, int estimatedSize, Collection<E> c, Type elementType, ParserContext ctx) throws ParseException, IOException {
		return (Collection<E>)validate(in);
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(Reader in, int estimatedSize, ClassMeta<?>[] argTypes, ParserContext ctx) throws ParseException, IOException {
		return (Object[])validate(in);
	}

	protected XMLStreamReader getStaxReader(Reader in) throws ParseException {
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty("javax.xml.stream.isNamespaceAware", false);
			XMLStreamReader parser = factory.createXMLStreamReader(in);
			parser.nextTag();
			return parser;
		} catch (Error e) {
			throw new ParseException(e.getLocalizedMessage());
		} catch (XMLStreamException e) {
			throw new ParseException(e);
		}
	}

	@Override /* Lockable */
	public XmlValidatorParser clone() {
		return (XmlValidatorParser)super.clone();
	}
}
