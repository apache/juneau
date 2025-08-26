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

import static javax.xml.stream.XMLStreamConstants.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Used to validate that the syntax of XML documents are valid.
 *
 * <ul class='spaced-list'>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 */
public class XmlValidatorParser extends XmlParser {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	public XmlValidatorParser() {
		super(create());
	}

	@Override /* Context */
	public XmlParserSession.Builder createSession() {
		return new XmlParserSession.Builder(XmlParser.DEFAULT) {
			@Override
			public XmlParserSession build() {
				return new XmlParserSession(this) {

					@Override
					protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
						try {
							return (T)validate(pipe.getReader());
						} catch (Exception e) {
							throw new ParseException(e);
						}
					}

					@Override /* ReaderParser */
					protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
						return (Map<K,V>)validate(pipe.getReader());
					}

					@Override /* ReaderParser */
					protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
						return (Collection<E>)validate(pipe.getReader());
					}
				};
			}
		};
	}

	public <T> T validate(Reader r) throws Exception {
		XMLStreamReader sr = getStaxReader(r);
		while (sr.next() != END_DOCUMENT) {/*no-op*/}
		return null;
	}

	protected XMLStreamReader getStaxReader(Reader in) throws Exception {
		var factory = XMLInputFactory.newInstance();
		factory.setProperty("javax.xml.stream.isNamespaceAware", false);
		var parser = factory.createXMLStreamReader(in);
		parser.nextTag();
		return parser;
	}
}
