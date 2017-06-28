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

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.juneau.html.HtmlTag.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlParser}.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public final class HtmlParserSession extends XmlParserSession {

	private static final Set<String> whitespaceElements = new HashSet<String>(
		Arrays.asList(
			new String[]{"br","bs","sp","ff"}
		)
	);

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param input
	 * 	The input.  Can be any of the following types:
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
	public HtmlParserSession(HtmlParserContext ctx, ObjectMap op, Object input, Method javaMethod, Object outer,
			Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	/**
	 * Parses CHARACTERS data.
	 *
	 * <p>
	 * Precondition:  Pointing to event immediately following opening tag.
	 * Postcondition:  Pointing to closing tag.
	 *
	 * @param r The stream being read from.
	 * @return The parsed string.
	 * @throws XMLStreamException
	 */
	@Override /* XmlParserSession */
	public String parseText(XMLStreamReader r) throws XMLStreamException {

		StringBuilder sb = getStringBuilder();

		int et = r.getEventType();
		if (et == END_ELEMENT)
			return "";

		int depth = 0;

		String characters = null;

		while (true) {
			if (et == START_ELEMENT) {
				if (characters != null) {
					if (sb.length() == 0)
						characters = trimStart(characters);
					sb.append(characters);
					characters = null;
				}
				HtmlTag tag = HtmlTag.forEvent(r);
				if (tag == BR) {
					sb.append('\n');
					r.nextTag();
				} else if (tag == BS) {
					sb.append('\b');
					r.nextTag();
				} else if (tag == SP) {
					et = r.next();
					if (et == CHARACTERS) {
						String s = r.getText();
						if (s.length() > 0) {
							char c = r.getText().charAt(0);
							if (c == '\u2003')
								c = '\t';
							sb.append(c);
						}
						r.nextTag();
					}
				} else if (tag == FF) {
					sb.append('\f');
					r.nextTag();
				} else if (tag.isOneOf(STRING, NUMBER, BOOLEAN)) {
					et = r.next();
					if (et == CHARACTERS) {
						sb.append(r.getText());
						r.nextTag();
					}
				} else {
					sb.append('<').append(r.getLocalName());
					for (int i = 0; i < r.getAttributeCount(); i++)
						sb.append(' ').append(r.getAttributeName(i)).append('=').append('\'').append(r.getAttributeValue(i)).append('\'');
					sb.append('>');
					depth++;
				}
			} else if (et == END_ELEMENT) {
				if (characters != null) {
					if (sb.length() == 0)
						characters = trimStart(characters);
					if (depth == 0)
						characters = trimEnd(characters);
					sb.append(characters);
					characters = null;
				}
				if (depth == 0)
					break;
				sb.append('<').append(r.getLocalName()).append('>');
				depth--;
			} else if (et == CHARACTERS) {
				characters = r.getText();
			}
			et = r.next();
		}

		String s = trim(sb.toString());
		returnStringBuilder(sb);
		return s;
	}

	/**
	 * Identical to {@link #parseText(XMLStreamReader)} except assumes the current event is the opening tag.
	 *
	 * <p>
	 * Precondition:  Pointing to opening tag.
	 * Postcondition:  Pointing to closing tag.
	 *
	 * @param r The stream being read from.
	 * @return The parsed string.
	 * @throws XMLStreamException
	 */
	@Override /* XmlParserSession */
	public String getElementText(XMLStreamReader r) throws XMLStreamException {
		r.next();
		return parseText(r);
	}

	@Override /* XmlParserSession */
	public boolean isWhitespaceElement(XMLStreamReader r) {
		String s = r.getLocalName();
		return whitespaceElements.contains(s);
	}

	@Override /* XmlParserSession */
	public String parseWhitespaceElement(XMLStreamReader r) throws XMLStreamException {

		HtmlTag tag = HtmlTag.forEvent(r);
		int et = r.next();
		if (tag == BR) {
			return "\n";
		} else if (tag == BS) {
			return "\b";
		} else if (tag == FF) {
			return "\f";
		} else if (tag == SP) {
			if (et == CHARACTERS) {
				String s = r.getText();
				if (s.charAt(0) == '\u2003')
					s = "\t";
				r.next();
				return decodeString(s);
			}
			return "";
		} else {
			throw new XMLStreamException("Invalid tag found in parseWhitespaceElement(): " + tag);
		}
	}
}
