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

import static org.apache.juneau.uon.UonParserContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.uon.*;

/**
 * Parses URL-encoded text into POJO models.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Parses URL-Encoded text (e.g. <js>"foo=bar&amp;baz=bing"</js>) into POJOs.
 * <p>
 * Expects parameter values to be in UON notation.
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonParserContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "hiding" })
@Consumes("application/x-www-form-urlencoded")
public class UrlEncodingParser extends UonParser {

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser(PropertyStore.create());


	private final UrlEncodingParserContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public UrlEncodingParser(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(UrlEncodingParserContext.class);
	}

	@Override /* CoreObject */
	public ObjectMap getOverrideProperties() {
		return super.getOverrideProperties().append(UON_decodeChars, true);
	}

	@Override /* CoreObject */
	public UrlEncodingParserBuilder builder() {
		return new UrlEncodingParserBuilder(propertyStore);
	}

	private <T> T parseAnything(UrlEncodingParserSession session, ClassMeta<T> eType, ParserReader r, Object outer) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();

		int c = r.peekSkipWs();
		if (c == '?')
			r.read();

		Object o;

		if (sType.isObject()) {
			ObjectMap m = new ObjectMap(session);
			parseIntoMap(session, r, m, session.getClassMeta(Map.class, String.class, Object.class), outer);
			if (m.containsKey("_value"))
				o = m.get("_value");
			else
				o = session.cast(m, null, eType);
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance() ? (Map)sType.newInstance() : new ObjectMap(session));
			o = parseIntoMap(session, r, m, sType, m);
		} else if (sType.canCreateNewBean(outer)) {
			BeanMap m = session.newBeanMap(outer, sType.getInnerClass());
			m = parseIntoBeanMap(session, r, m);
			o = m == null ? null : m.getBean();
		} else if (sType.isCollection() || sType.isArray() || sType.isArgs()) {
			// ?1=foo&2=bar...
			Collection c2 = ((sType.isArray() || sType.isArgs()) || ! sType.canCreateNewInstance(outer)) ? new ObjectList(session) : (Collection)sType.newInstance();
			Map<Integer,Object> m = new TreeMap<Integer,Object>();
			parseIntoMap(session, r, m, sType, c2);
			c2.addAll(m.values());
			if (sType.isArray())
				o = ArrayUtils.toArray(c2, sType.getElementType().getInnerClass());
			else if (sType.isArgs())
				o = c2.toArray(new Object[c2.size()]);
			else
				o = c2;
		} else {
			// It could be a non-bean with _type attribute.
			ObjectMap m = new ObjectMap(session);
			parseIntoMap(session, r, m, session.getClassMeta(Map.class, String.class, Object.class), outer);
			if (m.containsKey(session.getBeanTypePropertyName(eType)))
				o = session.cast(m, null, eType);
			else if (m.containsKey("_value")) {
				o = session.convertToType(m.get("_value"), sType);
			} else {
				if (sType.getNotABeanReason() != null)
					throw new ParseException(session, "Class ''{0}'' could not be instantiated as application/x-www-form-urlencoded.  Reason: ''{1}''", sType, sType.getNotABeanReason());
				throw new ParseException(session, "Malformed application/x-www-form-urlencoded input for class ''{0}''.", sType);
			}
		}

		if (transform != null && o != null)
			o = transform.unswap(session, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(UonParserSession session, ParserReader r, Map<K,V> m, ClassMeta<?> type, Object outer) throws Exception {

		ClassMeta<K> keyType = (ClassMeta<K>)(type.isArgs() || type.isCollectionOrArray() ? session.getClassMeta(Integer.class) : type.getKeyType());

		int c = r.peekSkipWs();
		if (c == -1)
			return m;

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for & or end.
		boolean isInEscape = false;

		int state = S1;
		int argIndex = 0;
		K currAttr = null;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == -1)
						return m;
					r.unread();
					Object attr = parseAttr(session, r, true);
					currAttr = attr == null ? null : convertAttrToType(session, m, session.trim(attr.toString()), keyType);
					state = S2;
					c = 0; // Avoid isInEscape if c was '\'
				} else if (state == S2) {
					if (c == '\u0002')
						state = S3;
					else if (c == -1 || c == '\u0001') {
						m.put(currAttr, null);
						if (c == -1)
							return m;
						state = S1;
					}
				} else if (state == S3) {
					if (c == -1 || c == '\u0001') {
						ClassMeta<V> valueType = (ClassMeta<V>)(type.isArgs() ? type.getArg(argIndex++) : type.isCollectionOrArray() ? type.getElementType() : type.getValueType());
						V value = convertAttrToType(session, m, "", valueType);
						m.put(currAttr, value);
						if (c == -1)
							return m;
						state = S1;
					} else  {
						// For performance, we bypass parseAnything for string values.
						ClassMeta<V> valueType = (ClassMeta<V>)(type.isArgs() ? type.getArg(argIndex++) : type.isCollectionOrArray() ? type.getElementType() : type.getValueType());
						V value = (V)(valueType.isString() ? super.parseString(session, r.unread(), true) : super.parseAnything(session, valueType, r.unread(), outer, true, null));

						// If we already encountered this parameter, turn it into a list.
						if (m.containsKey(currAttr) && valueType.isObject()) {
							Object v2 = m.get(currAttr);
							if (! (v2 instanceof ObjectList)) {
								v2 = new ObjectList(v2).setBeanSession(session);
								m.put(currAttr, (V)v2);
							}
							((ObjectList)v2).add(value);
						} else {
							m.put(currAttr, value);
						}
						state = S4;
						c = 0; // Avoid isInEscape if c was '\'
					}
				} else if (state == S4) {
					if (c == '\u0001')
						state = S1;
					else if (c == -1) {
						return m;
					}
				}
			}
			isInEscape = (c == '\\' && ! isInEscape);
		}
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(session, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(session, "Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException(session, "Could not find end of object.");

		return null; // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap(UrlEncodingParserSession session, ParserReader r, BeanMap<T> m) throws Exception {

		int c = r.peekSkipWs();
		if (c == -1)
			return m;

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for , or }
		boolean isInEscape = false;

		int state = S1;
		String currAttr = "";
		int currAttrLine = -1, currAttrCol = -1;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == -1) {
						return m;
					}
					r.unread();
					currAttrLine= r.getLine();
					currAttrCol = r.getColumn();
					currAttr = parseAttrName(session, r, true);
					if (currAttr == null)  // Value was '%00'
						return null;
					state = S2;
				} else if (state == S2) {
					if (c == '\u0002')
						state = S3;
					else if (c == -1 || c == '\u0001') {
						m.put(currAttr, null);
						if (c == -1)
							return m;
						state = S1;
					}
				} else if (state == S3) {
					if (c == -1 || c == '\u0001') {
						if (! currAttr.equals(session.getBeanTypePropertyName(m.getClassMeta()))) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
							} else {
								session.setCurrentProperty(pMeta);
								// In cases of "&foo=", create an empty instance of the value if createable.
								// Otherwise, leave it null.
								ClassMeta<?> cm = pMeta.getClassMeta();
								if (cm.canCreateNewInstance())
									pMeta.set(m, currAttr, cm.newInstance());
								session.setCurrentProperty(null);
							}
						}
						if (c == -1)
							return m;
						state = S1;
					} else {
						if (! currAttr.equals(session.getBeanTypePropertyName(m.getClassMeta()))) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
								parseAnything(session, object(), r.unread(), m.getBean(false), true, null); // Read content anyway to ignore it
							} else {
								session.setCurrentProperty(pMeta);
								if (session.shouldUseExpandedParams(pMeta)) {
									ClassMeta et = pMeta.getClassMeta().getElementType();
									Object value = parseAnything(session, et, r.unread(), m.getBean(false), true, pMeta);
									setName(et, value, currAttr);
									pMeta.add(m, currAttr, value);
								} else {
									ClassMeta<?> cm = pMeta.getClassMeta();
									Object value = parseAnything(session, cm, r.unread(), m.getBean(false), true, pMeta);
									setName(cm, value, currAttr);
									pMeta.set(m, currAttr, value);
								}
								session.setCurrentProperty(null);
							}
						}
						state = S4;
					}
				} else if (state == S4) {
					if (c == '\u0001')
						state = S1;
					else if (c == -1) {
						return m;
					}
				}
			}
			isInEscape = (c == '\\' && ! isInEscape);
		}
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(session, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(session, "Could not find value following '=' on object.");
		if (state == S4)
			throw new ParseException(session, "Could not find end of object.");

		return null; // Unreachable.
	}

	/**
	 * Parse a URL query string into a simple map of key/value pairs.
	 *
	 * @param qs The query string to parse.
	 * @return A sorted {@link TreeMap} of query string entries.
	 * @throws Exception
	 */
	public Map<String,String[]> parseIntoSimpleMap(String qs) throws Exception {

		Map<String,String[]> m = new TreeMap<String,String[]>();

		if (StringUtils.isEmpty(qs))
			return m;

		UonReader r = new UonReader(qs, true);

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName start, looking for = or & or end.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Found valStart, looking for & or end.

		try {
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
		} finally {
			r.close();
		}

		return m;
	}

	private static void add(Map<String,String[]> m, String key, String val) {
		boolean b = m.containsKey(key);
		if (val == null) {
			if (! b)
				m.put(key, null);
		} else if (b && m.get(key) != null) {
			m.put(key, ArrayUtils.append(m.get(key), val));
		} else {
			m.put(key, new String[]{val});
		}
	}

	/**
	 * Parses a single query parameter or header value into the specified class type.
	 *
	 * @param in The input query string value.
	 * @param type The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return A new instance of the specified type.
	 * @throws ParseException
	 */
	public <T> T parsePart(String in, Type type, Type...args) throws ParseException {
		if (in == null)
			return null;
		return (T)parsePart(in, getBeanContext().getClassMeta(type, args));
	}

	/**
	 * Parses a single query parameter or header value into the specified class type.
	 *
	 * @param in The input query string value.
	 * @param type The class type of the object to create.
	 * @return A new instance of the specified type.
	 * @throws ParseException
	 */
	public <T> T parsePart(String in, Class<T> type) throws ParseException {
		if (in == null)
			return null;
		return parsePart(in, getBeanContext().getClassMeta(type));
	}

	/**
	 * Same as {@link #parsePart(String, Type, Type...)} except the type has already
	 * been converted to a {@link ClassMeta} object.
	 *
	 * @param in The input query string value.
	 * @param type The class type of the object to create.
	 * @return A new instance of the specified type.
	 * @throws ParseException
	 */
	public <T> T parsePart(String in, ClassMeta<T> type) throws ParseException {
		if (in == null)
			return null;
		if (type.isString() && in.length() > 0) {
			// Shortcut - If we're returning a string and the value doesn't start with "'" or is "null", then
			// just return the string since it's a plain value.
			// This allows us to bypass the creation of a UonParserSession object.
			char x = StringUtils.firstNonWhitespaceChar(in);
			if (x != '\'' && x != 'n' && in.indexOf('~') == -1)
				return (T)in;
			if (x == 'n' && "null".equals(in))
				return null;
		}
		UonParserSession session = createParameterSession(in);
		try {
			UonReader r = session.getReader();
			return super.parseAnything(session, type, r, null, true, null);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(session, e);
		} finally {
			session.close();
		}
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UrlEncodingParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new UrlEncodingParserSession(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		UrlEncodingParserSession s = (UrlEncodingParserSession)session;
		UonReader r = s.getReader();
		T o = parseAnything(s, type, r, s.getOuter());
		return o;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		UrlEncodingParserSession s = (UrlEncodingParserSession)session;
		UonReader r = s.getReader();
		if (r.peekSkipWs() == '?')
			r.read();
		m = parseIntoMap(s, r, m, session.getClassMeta(Map.class, keyType, valueType), null);
		return m;
	}
}
