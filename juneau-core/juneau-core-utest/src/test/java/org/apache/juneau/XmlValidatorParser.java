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
 */
@SuppressWarnings({"unchecked"})
public class XmlValidatorParser extends XmlParser {

	public XmlValidatorParser() {
		super(PropertyStore.DEFAULT);
	}

	@Override /* Parser */
	public XmlParserSession createSession(ParserSessionArgs args) {
		return new XmlParserSession(null, args) {

			@Override
			protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
				return (T)validate(pipe.getReader());
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

	public <T> T validate(Reader r) throws Exception {
		XMLStreamReader sr = getStaxReader(r);
		while(sr.next() != END_DOCUMENT){}
		return null;
	}

	protected XMLStreamReader getStaxReader(Reader in) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty("javax.xml.stream.isNamespaceAware", false);
		XMLStreamReader parser = factory.createXMLStreamReader(in);
		parser.nextTag();
		return parser;
	}
}
