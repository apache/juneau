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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;

/**
 * Parses URL-encoded text into POJO models.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * Parses URL-Encoded text (e.g. <js>"foo=bar&amp;baz=bing"</js>) into POJOs.
 *
 * <p>
 * Expects parameter values to be in UON notation.
 *
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.
 */
@SuppressWarnings({ "unchecked" })
public class UrlEncodingParser extends UonParser implements PartParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "UrlEncodingParser.";

	/**
	 * Parser bean property collections/arrays as separate key/value pairs ({@link Boolean}, default=<jk>false</jk>).
	 *
	 * <p>
	 * This is the parser-side equivalent of the {@link #URLENC_expandedParams} setting.
	 *
	 * <p>
	 * This option only applies to beans.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If parsing multi-part parameters, it's highly recommended to use <code>Collections</code> or <code>Lists</code>
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 */
	public static final String URLENC_expandedParams = PREFIX + "expandedParams.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		expandedParams;

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public UrlEncodingParser(PropertyStore ps) {
		super(
			ps.builder()
				.set(UON_decodeChars, true)
				.build(), 
			"application/x-www-form-urlencoded"
		);
		expandedParams = getProperty(URLENC_expandedParams, boolean.class, false);
	}

	@Override /* Context */
	public UrlEncodingParserBuilder builder() {
		return new UrlEncodingParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UrlEncodingParserBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UrlEncodingParserBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link UrlEncodingParserBuilder} object.
	 */
	public static UrlEncodingParserBuilder create() {
		return new UrlEncodingParserBuilder();
	}

	/**
	 * Parse a URL query string into a simple map of key/value pairs.
	 *
	 * @param qs The query string to parse.
	 * @param map The map to parse into.  If <jk>null</jk>, then a new {@link TreeMap} will be used.
	 * @return A sorted {@link TreeMap} of query string entries.
	 * @throws Exception
	 */
	public Map<String,String[]> parseIntoSimpleMap(String qs, Map<String,String[]> map) throws Exception {

		Map<String,String[]> m = map == null ? new TreeMap<String,String[]>() : map;

		if (isEmpty(qs))
			return m;

		try (ParserPipe p = new ParserPipe(qs, false, false, null, null)) {
			
			final int S1=1; // Looking for attrName start.
			final int S2=2; // Found attrName start, looking for = or & or end.
			final int S3=3; // Found =, looking for valStart.
			final int S4=4; // Found valStart, looking for & or end.

			try (UonReader r = new UonReader(p, true)) {
				int c = r.peekSkipWs();
				if (c == '?')
					r.read();

				int state = S1;
				String currAttr = null;
				while (c != -1) {
					c = r.read();
					if (state == S1) {
						if (c != -1) {
							r.unread();
							r.mark();
							state = S2;
						}
					} else if (state == S2) {
						if (c == -1) {
							add(m, r.getMarked(), null);
						} else if (c == '\u0001') {
							m.put(r.getMarked(0,-1), null);
							state = S1;
						} else if (c == '\u0002') {
							currAttr = r.getMarked(0,-1);
							state = S3;
						}
					} else if (state == S3) {
						if (c == -1 || c == '\u0001') {
							add(m, currAttr, "");
						} else {
							if (c == '\u0002')
								r.replace('=');
							r.unread();
							r.mark();
							state = S4;
						}
					} else if (state == S4) {
						if (c == -1) {
							add(m, currAttr, r.getMarked());
						} else if (c == '\u0001') {
							add(m, currAttr, r.getMarked(0,-1));
							state = S1;
						} else if (c == '\u0002') {
							r.replace('=');
						}
					}
				}
			}

			return m;
		}
	}

	private static void add(Map<String,String[]> m, String key, String val) {
		boolean b = m.containsKey(key);
		if (val == null) {
			if (! b)
				m.put(key, null);
		} else if (b && m.get(key) != null) {
			m.put(key, append(m.get(key), val));
		} else {
			m.put(key, new String[]{val});
		}
	}

	@Override /* PartParser */
	public <T> T parse(PartType partType, String in, ClassMeta<T> type) throws ParseException {
		if (in == null)
			return null;
		if (type.isString() && in.length() > 0) {
			// Shortcut - If we're returning a string and the value doesn't start with "'" or is "null", then
			// just return the string since it's a plain value.
			// This allows us to bypass the creation of a UonParserSession object.
			char x = firstNonWhitespaceChar(in);
			if (x != '\'' && x != 'n' && in.indexOf('~') == -1)
				return (T)in;
			if (x == 'n' && "null".equals(in))
				return null;
		}
		UonParserSession session = createParameterSession();
		try (ParserPipe pipe = session.createPipe(in)) {
			try (UonReader r = session.getUonReader(pipe, false)) {
				return session.parseAnything(type, r, null, true, null);
			}
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UrlEncodingParserSession createSession(ParserSessionArgs args) {
		return new UrlEncodingParserSession(this, args);
	}
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UrlEncodingParser", new ObjectMap()
				.append("expandedParams", expandedParams)
			);
	}
}
