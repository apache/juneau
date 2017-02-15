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

import static org.apache.juneau.urlencoding.UonParserContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parses UON (a notation for URL-encoded query parameter values) text into POJO models.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>text/uon</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	This parser uses a state machine, which makes it very fast and efficient.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonParserContext}
 * 	<li>{@link ParserContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes("text/uon")
public class UonParser extends ReaderParser {

	/** Reusable instance of {@link UonParser}, all default settings. */
	public static final UonParser DEFAULT = new UonParser().lock();

	/** Reusable instance of {@link UonParser.Decoding}. */
	public static final UonParser DEFAULT_DECODING = new Decoding().lock();

	// Characters that need to be preceeded with an escape character.
	private static final AsciiSet escapedChars = new AsciiSet("~'\u0001\u0002");

	private static final char AMP='\u0001', EQ='\u0002';  // Flags set in reader to denote & and = characters.

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingParser().setProperty(UonParserContext.<jsf>UON_decodeChars</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Decoding extends UonParser {
		/** Constructor */
		public Decoding() {
			setDecodeChars(true);
		}
	}

	/**
	 * Workhorse method.
	 *
	 * @param session The parser context for this parse.
	 * @param eType The class type being parsed, or <jk>null</jk> if unknown.
	 * @param r The reader being parsed.
	 * @param outer The outer object (for constructing nested inner classes).
	 * @param isUrlParamValue If <jk>true</jk>, then we're parsing a top-level URL-encoded value which is treated a bit different than the default case.
	 * @param pMeta The current bean property being parsed.
	 * @return The parsed object.
	 * @throws Exception
	 */
	protected <T> T parseAnything(UonParserSession session, ClassMeta<T> eType, ParserReader r, Object outer, boolean isUrlParamValue, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();

		Object o = null;

		int c = r.peekSkipWs();

		if (c == -1 || c == AMP) {
			// If parameter is blank and it's an array or collection, return an empty list.
			if (sType.isCollectionOrArray())
				o = sType.newInstance();
			else if (sType.isString() || sType.isObject())
				o = "";
			else if (sType.isPrimitive())
				o = sType.getPrimitiveDefault();
			// Otherwise, leave null.
		} else if (sType.isObject()) {
			if (c == '(') {
				ObjectMap m = new ObjectMap(session);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				o = session.cast(m, pMeta, eType);
			} else if (c == '@') {
				Collection l = new ObjectList(session);
				o = parseIntoCollection(session, r, l, sType.getElementType(), isUrlParamValue, pMeta);
			} else {
				String s = parseString(session, r, isUrlParamValue);
				if (c != '\'') {
					if ("true".equals(s) || "false".equals(s))
						o = Boolean.valueOf(s);
					else if (StringUtils.isNumeric(s))
						o = StringUtils.parseNumber(s, Number.class);
					else
						o = s;
				} else {
					o = s;
				}
			}
		} else if (sType.isBoolean()) {
			o = parseBoolean(session, r);
		} else if (sType.isCharSequence()) {
			o = parseString(session, r, isUrlParamValue);
		} else if (sType.isChar()) {
			String s = parseString(session, r, isUrlParamValue);
			o = s == null ? null : s.charAt(0);
		} else if (sType.isNumber()) {
			o = parseNumber(session, r, (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(session));
			o = parseIntoMap(session, r, m, sType.getKeyType(), sType.getValueType(), pMeta);
		} else if (sType.isCollection()) {
			if (c == '(') {
				ObjectMap m = new ObjectMap(session);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				// Handle case where it's a collection, but serialized as a map with a _type or _value key.
				if (m.containsKey(session.getBeanTypePropertyName()))
					o = session.cast(m, pMeta, eType);
				// Handle case where it's a collection, but only a single value was specified.
				else {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new ObjectList(session));
					l.add(m.cast(sType.getElementType()));
					o = l;
				}
			} else {
				Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new ObjectList(session));
				o = parseIntoCollection(session, r, l, sType.getElementType(), isUrlParamValue, pMeta);
			}
		} else if (sType.canCreateNewBean(outer)) {
			BeanMap m = session.newBeanMap(outer, sType.getInnerClass());
			m = parseIntoBeanMap(session, r, m);
			o = m == null ? null : m.getBean();
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			String s = parseString(session, r, isUrlParamValue);
			if (s != null)
				o = sType.newInstanceFromString(outer, s);
		} else if (sType.canCreateNewInstanceFromNumber(outer)) {
			o = sType.newInstanceFromNumber(session, outer, parseNumber(session, r, sType.getNewInstanceFromNumberClass()));
		} else if (sType.isArray()) {
			if (c == '(') {
				ObjectMap m = new ObjectMap(session);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				// Handle case where it's an array, but serialized as a map with a _type or _value key.
				if (m.containsKey(session.getBeanTypePropertyName()))
					o = session.cast(m, pMeta, eType);
				// Handle case where it's an array, but only a single value was specified.
				else {
					ArrayList l = new ArrayList(1);
					l.add(m.cast(sType.getElementType()));
					o = session.toArray(sType, l);
				}
			} else {
				ArrayList l = (ArrayList)parseIntoCollection(session, r, new ArrayList(), sType.getElementType(), isUrlParamValue, pMeta);
				o = session.toArray(sType, l);
			}
		} else if (c == '(') {
			// It could be a non-bean with _type attribute.
			ObjectMap m = new ObjectMap(session);
			parseIntoMap(session, r, m, string(), object(), pMeta);
			if (m.containsKey(session.getBeanTypePropertyName()))
				o = session.cast(m, pMeta, eType);
			else
				throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else {
			throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		}

		if (transform != null && o != null)
			o = transform.unswap(session, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(UonParserSession session, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int c = r.read();
		if (c == -1 || c == AMP)
			return null;
		if (c == 'n')
			return (Map<K,V>)parseNull(session, r);
		if (c != '(')
			throw new ParseException(session, "Expected '(' at beginning of object.");

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for , or )
		boolean isInEscape = false;

		int state = S1;
		K currAttr = null;
		while (c != -1 && c != AMP) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == ')')
						return m;
					if (Character.isWhitespace(c))
						skipSpace(r);
					else {
						r.unread();
						Object attr = parseAttr(session, r, session.isDecodeChars());
						currAttr = attr == null ? null : convertAttrToType(session, m, session.trim(attr.toString()), keyType);
						state = S2;
						c = 0; // Avoid isInEscape if c was '\'
					}
				} else if (state == S2) {
					if (c == EQ || c == '=')
						state = S3;
					else if (c == -1 || c == ',' || c == ')' || c == AMP) {
						if (currAttr == null) {
							// Value was '%00'
							r.unread();
							return null;
						}
						m.put(currAttr, null);
						if (c == ')' || c == -1 || c == AMP)
							return m;
						state = S1;
					}
				} else if (state == S3) {
					if (c == -1 || c == ',' || c == ')' || c == AMP) {
						V value = convertAttrToType(session, m, "", valueType);
						m.put(currAttr, value);
						if (c == -1 || c == ')' || c == AMP)
							return m;
						state = S1;
					} else  {
						V value = parseAnything(session, valueType, r.unread(), m, false, pMeta);
						setName(valueType, value, currAttr);
						m.put(currAttr, value);
						state = S4;
						c = 0; // Avoid isInEscape if c was '\'
					}
				} else if (state == S4) {
					if (c == ',')
						state = S1;
					else if (c == ')' || c == -1 || c == AMP) {
						return m;
					}
				}
			}
			isInEscape = isInEscape(c, r, isInEscape);
		}
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(session, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(session, "Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException(session, "Could not find ')' marking end of object.");

		return null; // Unreachable.
	}

	private <E> Collection<E> parseIntoCollection(UonParserSession session, ParserReader r, Collection<E> l, ClassMeta<E> elementType, boolean isUrlParamValue, BeanPropertyMeta pMeta) throws Exception {

		int c = r.readSkipWs();
		if (c == -1 || c == AMP)
			return null;
		if (c == 'n')
			return (Collection<E>)parseNull(session, r);

		// If we're parsing a top-level parameter, we're allowed to have comma-delimited lists outside parenthesis (e.g. "&foo=1,2,3&bar=a,b,c")
		// This is not allowed at lower levels since we use comma's as end delimiters.
		boolean isInParens = (c == '@');
		if (! isInParens) {
			if (isUrlParamValue)
				r.unread();
			else
				throw new ParseException(session, "Could not find '(' marking beginning of collection.");
		} else {
			r.read();
		}

		if (isInParens) {
			final int S1=1; // Looking for starting of first entry.
			final int S2=2; // Looking for starting of subsequent entries.
			final int S3=3; // Looking for , or ) after first entry.

			int state = S1;
			while (c != -1 && c != AMP) {
				c = r.read();
				if (state == S1 || state == S2) {
					if (c == ')') {
						if (state == S2) {
							l.add(parseAnything(session, elementType, r.unread(), l, false, pMeta));
							r.read();
						}
						return l;
					} else if (Character.isWhitespace(c)) {
						skipSpace(r);
					} else {
						l.add(parseAnything(session, elementType, r.unread(), l, false, pMeta));
						state = S3;
					}
				} else if (state == S3) {
					if (c == ',') {
						state = S2;
					} else if (c == ')') {
						return l;
					}
				}
			}
			if (state == S1 || state == S2)
				throw new ParseException(session, "Could not find start of entry in array.");
			if (state == S3)
				throw new ParseException(session, "Could not find end of entry in array.");

		} else {
			final int S1=1; // Looking for starting of entry.
			final int S2=2; // Looking for , or & or END after first entry.

			int state = S1;
			while (c != -1 && c != AMP) {
				c = r.read();
				if (state == S1) {
					if (Character.isWhitespace(c)) {
						skipSpace(r);
					} else {
						l.add(parseAnything(session, elementType, r.unread(), l, false, pMeta));
						state = S2;
					}
				} else if (state == S2) {
					if (c == ',') {
						state = S1;
					} else if (Character.isWhitespace(c)) {
						skipSpace(r);
					} else if (c == AMP || c == -1) {
						r.unread();
						return l;
					}
				}
			}
		}

		return null;  // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap(UonParserSession session, ParserReader r, BeanMap<T> m) throws Exception {

		int c = r.readSkipWs();
		if (c == -1 || c == AMP)
			return null;
		if (c == 'n')
			return (BeanMap<T>)parseNull(session, r);
		if (c != '(')
			throw new ParseException(session, "Expected '(' at beginning of object.");

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for , or }
		boolean isInEscape = false;

		int state = S1;
		String currAttr = "";
		int currAttrLine = -1, currAttrCol = -1;
		while (c != -1 && c != AMP) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == ')' || c == -1 || c == AMP) {
						return m;
					}
					if (Character.isWhitespace(c))
						skipSpace(r);
					else {
						r.unread();
						currAttrLine= r.getLine();
						currAttrCol = r.getColumn();
						currAttr = parseAttrName(session, r, session.isDecodeChars());
						if (currAttr == null)  // Value was '%00'
							return null;
						state = S2;
					}
				} else if (state == S2) {
					if (c == EQ || c == '=')
						state = S3;
					else if (c == -1 || c == ',' || c == ')' || c == AMP) {
						m.put(currAttr, null);
						if (c == ')' || c == -1 || c == AMP)
							return m;
						state = S1;
					}
				} else if (state == S3) {
					if (c == -1 || c == ',' || c == ')' || c == AMP) {
						if (! currAttr.equals(session.getBeanTypePropertyName())) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
							} else {
								Object value = session.convertToType("", pMeta.getClassMeta());
								pMeta.set(m, value);
							}
						}
						if (c == -1 || c == ')' || c == AMP)
							return m;
						state = S1;
					} else {
						if (! currAttr.equals(session.getBeanTypePropertyName())) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
								parseAnything(session, object(), r.unread(), m.getBean(false), false, null); // Read content anyway to ignore it
							} else {
								session.setCurrentProperty(pMeta);
								ClassMeta<?> cm = pMeta.getClassMeta();
								Object value = parseAnything(session, cm, r.unread(), m.getBean(false), false, pMeta);
								setName(cm, value, currAttr);
								pMeta.set(m, value);
								session.setCurrentProperty(null);
							}
						}
						state = S4;
					}
				} else if (state == S4) {
					if (c == ',')
						state = S1;
					else if (c == ')' || c == -1 || c == AMP) {
						return m;
					}
				}
			}
			isInEscape = isInEscape(c, r, isInEscape);
		}
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(session, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(session, "Could not find value following '=' on object.");
		if (state == S4)
			throw new ParseException(session, "Could not find ')' marking end of object.");

		return null; // Unreachable.
	}

	Object parseNull(UonParserSession session, ParserReader r) throws Exception {
		String s = parseString(session, r, false);
		if ("ull".equals(s))
			return null;
		throw new ParseException(session, "Unexpected character sequence: ''{0}''", s);
	}

	Object parseAttr(UonParserSession session, ParserReader r, boolean encoded) throws Exception {
		Object attr;
		attr = parseAttrName(session, r, encoded);
		return attr;
	}

	String parseAttrName(UonParserSession session, ParserReader r, boolean encoded) throws Exception {

		// If string is of form 'xxx', we're looking for ' at the end.
		// Otherwise, we're looking for '&' or '=' or WS or -1 denoting the end of this string.

		int c = r.peekSkipWs();
		if (c == '\'')
			return parsePString(session, r);

		r.mark();
		boolean isInEscape = false;
		if (encoded) {
			while (c != -1) {
				c = r.read();
				if (! isInEscape) {
					if (c == AMP || c == EQ || c == -1 || Character.isWhitespace(c)) {
						if (c != -1)
							r.unread();
						String s = r.getMarked();
						return ("null".equals(s) ? null : s);
					}
				}
				else if (c == AMP)
					r.replace('&');
				else if (c == EQ)
					r.replace('=');
				isInEscape = isInEscape(c, r, isInEscape);
			}
		} else {
			while (c != -1) {
				c = r.read();
				if (! isInEscape) {
					if (c == '=' || c == -1 || Character.isWhitespace(c)) {
						if (c != -1)
							r.unread();
						String s = r.getMarked();
						return ("null".equals(s) ? null : session.trim(s));
					}
				}
				isInEscape = isInEscape(c, r, isInEscape);
			}
		}

		// We should never get here.
		throw new ParseException(session, "Unexpected condition.");
	}


	/**
	 * Returns true if the next character in the stream is preceeded by an escape '~' character.
	 * @param c The current character.
	 * @param r The reader.
	 * @param prevIsInEscape What the flag was last time.
	 */
	private static final boolean isInEscape(int c, ParserReader r, boolean prevIsInEscape) throws Exception {
		if (c == '~' && ! prevIsInEscape) {
			c = r.peek();
			if (escapedChars.contains(c)) {
				r.delete();
				return true;
			}
		}
		return false;
	}

	String parseString(UonParserSession session, ParserReader r, boolean isUrlParamValue) throws Exception {

		// If string is of form 'xxx', we're looking for ' at the end.
		// Otherwise, we're looking for ',' or ')' or -1 denoting the end of this string.

		int c = r.peekSkipWs();
		if (c == '\'')
			return parsePString(session, r);

		r.mark();
		boolean isInEscape = false;
		String s = null;
		AsciiSet endChars = (isUrlParamValue ? endCharsParam : endCharsNormal);
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				// If this is a URL parameter value, we're looking for:  &
				// If not, we're looking for:  &,)
				if (endChars.contains(c)) {
					r.unread();
					c = -1;
				}
			}
			if (c == -1)
				s = r.getMarked();
			else if (c == EQ)
				r.replace('=');
			else if (Character.isWhitespace(c)) {
				s = r.getMarked(0, -1);
				skipSpace(r);
				c = -1;
			}
			isInEscape = isInEscape(c, r, isInEscape);
		}

		return ("null".equals(s) ? null : session.trim(s));
	}

	private static final AsciiSet endCharsParam = new AsciiSet(""+AMP), endCharsNormal = new AsciiSet(",)"+AMP);


	/**
	 * Parses a string of the form "'foo'"
	 * All whitespace within parenthesis are preserved.
	 */
	static String parsePString(UonParserSession session, ParserReader r) throws Exception {

		r.read(); // Skip first quote.
		r.mark();
		int c = 0;

		boolean isInEscape = false;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (c == '\'')
					return session.trim(r.getMarked(0, -1));
			}
			if (c == EQ)
				r.replace('=');
			isInEscape = isInEscape(c, r, isInEscape);
		}
		throw new ParseException(session, "Unmatched parenthesis");
	}

	private Boolean parseBoolean(UonParserSession session, ParserReader r) throws Exception {
		String s = parseString(session, r, false);
		if (s == null || s.equals("null"))
			return null;
		if (s.equals("true"))
			return true;
		if (s.equals("false"))
			return false;
		throw new ParseException(session, "Unrecognized syntax for boolean.  ''{0}''.", s);
	}

	private Number parseNumber(UonParserSession session, ParserReader r, Class<? extends Number> c) throws Exception {
		String s = parseString(session, r, false);
		if (s == null)
			return null;
		return StringUtils.parseNumber(s, c);
	}

	/*
	 * Call this method after you've finished a parsing a string to make sure that if there's any
	 * remainder in the input, that it consists only of whitespace and comments.
	 */
	private void validateEnd(UonParserSession session, ParserReader r) throws Exception {
		while (true) {
			int c = r.read();
			if (c == -1)
				return;
			if (! Character.isWhitespace(c))
				throw new ParseException(session, "Remainder after parse: ''{0}''.", (char)c);
		}
	}

	private Object[] parseArgs(UonParserSession session, ParserReader r, ClassMeta<?>[] argTypes) throws Exception {

		final int S1=1; // Looking for start of entry
		final int S2=2; // Looking for , or )

		Object[] o = new Object[argTypes.length];
		int i = 0;

		int c = r.readSkipWs();
		if (c == -1 || c == AMP)
			return null;
		if (c != '@')
			throw new ParseException(session, "Expected '@' at beginning of args array.");
		c = r.read();

		int state = S1;
		while (c != -1 && c != AMP) {
			c = r.read();
			if (state == S1) {
				if (c == ')')
					return o;
				o[i] = parseAnything(session, argTypes[i], r.unread(), session.getOuter(), false, null);
				i++;
				state = S2;
			} else if (state == S2) {
				if (c == ',') {
					state = S1;
				} else if (c == ')') {
					return o;
				}
			}
		}

		throw new ParseException(session, "Did not find ')' at the end of args array.");
	}

	private static void skipSpace(ParserReader r) throws Exception {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (c <= 2 || ! Character.isWhitespace(c)) {
				r.unread();
				return;
			}
		}
	}

	UonParserSession createParameterSession(Object input) {
		return new UonParserSession(getContext(UonParserContext.class), input);
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UonParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new UonParserSession(getContext(UonParserContext.class), op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		T o = parseAnything(s, type, r, s.getOuter(), true, null);
		validateEnd(s, r);
		return o;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		m = parseIntoMap(s, r, m, (ClassMeta<K>)session.getClassMeta(keyType), (ClassMeta<V>)session.getClassMeta(valueType), null);
		validateEnd(s, r);
		return m;
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(ParserSession session, Collection<E> c, Type elementType) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		c = parseIntoCollection(s, r, c, (ClassMeta<E>)session.getClassMeta(elementType), false, null);
		validateEnd(s, r);
		return c;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<?>[] argTypes) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		Object[] a = parseArgs(s, r, argTypes);
		return a;
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b> Decode <js>"%xx"</js> sequences.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonParser.decodeChars"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk> for {@link UonParser}, <jk>true</jk> for {@link UrlEncodingParser}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specify <jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk>
	 * 	if they've already been decoded before being passed to this parser.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>UON_decodeChars</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see UonParserContext#UON_decodeChars
	 */
	public UonParser setDecodeChars(boolean value) throws LockedException {
		return setProperty(UON_decodeChars, value);
	}

	@Override /* Parser */
	public UonParser setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Parser */
	public UonParser setStrict(boolean value) throws LockedException {
		super.setStrict(value);
		return this;
	}

	@Override /* Parser */
	public UonParser setInputStreamCharset(String value) throws LockedException {
		super.setInputStreamCharset(value);
		return this;
	}

	@Override /* Parser */
	public UonParser setFileCharset(String value) throws LockedException {
		super.setFileCharset(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public UonParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public UonParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public UonParser clone() {
		try {
			UonParser c = (UonParser)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
