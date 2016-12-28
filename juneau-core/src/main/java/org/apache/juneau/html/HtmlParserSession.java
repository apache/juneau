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
package org.apache.juneau.html;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlParser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public final class HtmlParserSession extends ParserSession {

	private XMLEventReader xmlEventReader;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
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
	 * @param locale The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 */
	public HtmlParserSession(HtmlParserContext ctx, ObjectMap op, Object input, Method javaMethod, Object outer, Locale locale, TimeZone timeZone) {
		super(ctx, op, input, javaMethod, outer, locale, timeZone);
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
	final XMLEventReader getXmlEventReader() throws Exception {
		Reader r = IOUtils.getBufferedReader(super.getReader());
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
		this.xmlEventReader = factory.createXMLEventReader(r);
		return xmlEventReader;
	}

	@Override /* ParserSession */
	public boolean close() {
		if (super.close()) {
			if (xmlEventReader != null) {
				try {
					xmlEventReader.close();
				} catch (XMLStreamException e) {
					throw new BeanRuntimeException(e);
				}
			}
			return true;
		}
		return false;
	}
}
