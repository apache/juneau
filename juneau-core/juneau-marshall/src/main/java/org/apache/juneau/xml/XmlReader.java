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

import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.parser.*;

/**
 * Wrapper class around a {@link XMLStreamReader}.
 *
 * <p>
 * The purpose is to encapsulate the reader with the {@link ParserPipe} object so that it can be retrieved for
 * debugging purposes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
public final class XmlReader implements XMLStreamReader, Positionable {

	private final ParserPipe pipe;
	private final XMLStreamReader sr;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @param validating The value for the {@link XMLInputFactory#IS_VALIDATING} setting.
	 * @param reporter The value for the {@link XMLInputFactory#REPORTER} setting.
	 * @param resolver The value for the {@link XMLInputFactory#RESOLVER} setting.
	 * @param eventAllocator The value for the {@link XMLInputFactory#ALLOCATOR} setting.
	 * @throws IOException Thrown by underling
	 * @throws XMLStreamException Thrown by underlying XML stream.
	 */
	protected XmlReader(ParserPipe pipe, boolean validating, XMLReporter reporter, XMLResolver resolver, XMLEventAllocator eventAllocator) throws IOException, XMLStreamException {
		this.pipe = pipe;
		try {
			@SuppressWarnings("resource")
			Reader r = pipe.getBufferedReader();
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_VALIDATING, validating);
			factory.setProperty(XMLInputFactory.IS_COALESCING, true);
			factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			if (factory.isPropertySupported(XMLInputFactory.REPORTER) && reporter != null)
				factory.setProperty(XMLInputFactory.REPORTER, reporter);
			if (factory.isPropertySupported(XMLInputFactory.RESOLVER) && resolver != null)
				factory.setProperty(XMLInputFactory.RESOLVER, resolver);
			if (factory.isPropertySupported(XMLInputFactory.ALLOCATOR) && eventAllocator != null)
				factory.setProperty(XMLInputFactory.ALLOCATOR, eventAllocator);
			sr = factory.createXMLStreamReader(r);
			sr.nextTag();
			pipe.setPositionable(this);
		} catch (Error e) {
			throw cast(IOException.class, e);
		}
	}

	/**
	 * Returns the pipe passed into the constructor.
	 *
	 * @return The pipe passed into the constructor.
	 */
	public ParserPipe getPipe() {
		return pipe;
	}

	@Override /* XMLStreamReader */
	public void close() throws XMLStreamException {
		sr.close();
	}

	@Override /* XMLStreamReader */
	public int getAttributeCount() {
		return sr.getAttributeCount();
	}

	@Override /* XMLStreamReader */
	public String getAttributeLocalName(int index) {
		return sr.getAttributeLocalName(index);
	}

	@Override /* XMLStreamReader */
	public QName getAttributeName(int index) {
		return sr.getAttributeName(index);
	}

	@Override /* XMLStreamReader */
	public String getAttributeNamespace(int index) {
		return sr.getAttributeNamespace(index);
	}

	@Override /* XMLStreamReader */
	public String getAttributePrefix(int index) {
		return sr.getAttributePrefix(index);
	}

	@Override /* XMLStreamReader */
	public String getAttributeType(int index) {
		return sr.getAttributeType(index);
	}

	@Override /* XMLStreamReader */
	public String getAttributeValue(int index) {
		return sr.getAttributeValue(index);
	}

	@Override /* XMLStreamReader */
	public String getAttributeValue(String namespaceURI, String localName) {
		return sr.getAttributeValue(namespaceURI, localName);
	}

	@Override /* XMLStreamReader */
	public String getCharacterEncodingScheme() {
		return sr.getCharacterEncodingScheme();
	}

	@Override /* XMLStreamReader */
	public String getElementText() throws XMLStreamException {
		return sr.getElementText();
	}

	@Override /* XMLStreamReader */
	public String getEncoding() {
		return sr.getEncoding();
	}

	@Override /* XMLStreamReader */
	public int getEventType() {
		return sr.getEventType();
	}

	@Override /* XMLStreamReader */
	public String getLocalName() {
		return sr.getLocalName();
	}

	@Override /* XMLStreamReader */
	public Location getLocation() {
		return sr.getLocation();
	}

	@Override /* XMLStreamReader */
	public QName getName() {
		return sr.getName();
	}

	@Override /* XMLStreamReader */
	public NamespaceContext getNamespaceContext() {
		return sr.getNamespaceContext();
	}

	@Override /* XMLStreamReader */
	public int getNamespaceCount() {
		return sr.getNamespaceCount();
	}

	@Override /* XMLStreamReader */
	public String getNamespacePrefix(int index) {
		return sr.getNamespacePrefix(index);
	}

	@Override /* XMLStreamReader */
	public String getNamespaceURI() {
		return sr.getNamespaceURI();
	}

	@Override /* XMLStreamReader */
	public String getNamespaceURI(String prefix) {
		return sr.getNamespaceURI(prefix);
	}

	@Override /* XMLStreamReader */
	public String getNamespaceURI(int index) {
		return sr.getNamespaceURI(index);
	}

	@Override /* XMLStreamReader */
	public String getPIData() {
		return sr.getPIData();
	}

	@Override /* XMLStreamReader */
	public String getPITarget() {
		return sr.getPITarget();
	}

	@Override /* XMLStreamReader */
	public String getPrefix() {
		return sr.getPrefix();
	}

	@Override /* XMLStreamReader */
	public Object getProperty(String name) throws IllegalArgumentException {
		return sr.getProperty(name);
	}

	@Override /* XMLStreamReader */
	public String getText() {
		return sr.getText();
	}

	@Override /* XMLStreamReader */
	public char[] getTextCharacters() {
		return sr.getTextCharacters();
	}

	@Override /* XMLStreamReader */
	public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
		return sr.getTextCharacters(sourceStart, target, targetStart, length);
	}

	@Override /* XMLStreamReader */
	public int getTextLength() {
		return sr.getTextLength();
	}

	@Override /* XMLStreamReader */
	public int getTextStart() {
		return sr.getTextStart();
	}

	@Override /* XMLStreamReader */
	public String getVersion() {
		return sr.getVersion();
	}

	@Override /* XMLStreamReader */
	public boolean hasName() {
		return sr.hasName();
	}

	@Override /* XMLStreamReader */
	public boolean hasNext() throws XMLStreamException {
		return sr.hasNext();
	}

	@Override /* XMLStreamReader */
	public boolean hasText() {
		return sr.hasText();
	}

	@Override /* XMLStreamReader */
	public boolean isAttributeSpecified(int index) {
		return sr.isAttributeSpecified(index);
	}

	@Override /* XMLStreamReader */
	public boolean isCharacters() {
		return sr.isCharacters();
	}

	@Override /* XMLStreamReader */
	public boolean isEndElement() {
		return sr.isEndElement();
	}

	@Override /* XMLStreamReader */
	public boolean isStandalone() {
		return sr.isStandalone();
	}

	@Override /* XMLStreamReader */
	public boolean isStartElement() {
		return sr.isStartElement();
	}

	@Override /* XMLStreamReader */
	public boolean isWhiteSpace() {
		return sr.isWhiteSpace();
	}

	@Override /* XMLStreamReader */
	public int next() throws XMLStreamException {
		return sr.next();
	}

	@Override /* XMLStreamReader */
	public int nextTag() throws XMLStreamException {
		return sr.nextTag();
	}

	@Override /* XMLStreamReader */
	public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
		sr.require(type, namespaceURI, localName);
	}

	@Override /* XMLStreamReader */
	public boolean standaloneSet() {
		return sr.standaloneSet();
	}

	@Override /* Positionable */
	public Position getPosition() {
		Location l = getLocation();
		return new Position(l.getLineNumber(), l.getColumnNumber());
	}
}
