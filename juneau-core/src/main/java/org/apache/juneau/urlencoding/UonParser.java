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
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>text/uon</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This parser uses a state machine, which makes it very fast and efficient.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonParserContext}
 * 	<li>{@link ParserContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes("text/uon")
public class UonParser extends ReaderParser {

	/** Reusable instance of {@link UonParser}, all default settings. */
	public static final UonParser DEFAULT = new UonParser().lock();

	/** Reusable instance of {@link UonParser.Decoding}. */
	public static final UonParser DEFAULT_DECODING = new Decoding().lock();

	/** Reusable instance of {@link UonParser}, all default settings, whitespace-aware. */
	public static final UonParser DEFAULT_WS_AWARE = new UonParser().setProperty(UON_whitespaceAware, true).lock();

	// Characters that need to be preceeded with an escape character.
	private static final AsciiSet escapedChars = new AsciiSet(",()~=$\u0001\u0002");

	private static final char NUL='\u0000', AMP='\u0001', EQ='\u0002';  // Flags set in reader to denote & and = characters.

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingParser().setProperty(UonParserContext.<jsf>UON_decodeChars</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Decoding extends UonParser {
		/** Constructor */
		public Decoding() {
			setProperty(UON_decodeChars, true);
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

		BeanContext bc = session.getBeanContext();
		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();
		BeanDictionary bd = (pMeta == null ? bc.getBeanDictionary() : pMeta.getBeanDictionary());

		Object o = null;

		// Parse type flag '$x'
		char flag = readFlag(session, r, (char)0);

		int c = r.peek();

		if (c == -1 || c == AMP) {
			// If parameter is blank and it's an array or collection, return an empty list.
			if (sType.isArray() || sType.isCollection())
				o = sType.newInstance();
			else if (sType.isString() || sType.isObject())
				o = "";
			else if (sType.isPrimitive())
				o = sType.getPrimitiveDefault();
			// Otherwise, leave null.
		} else if (sType.isObject()) {
			if (flag == 0 || flag == 's') {
				o = parseString(session, r, isUrlParamValue);
			} else if (flag == 'b') {
				o = parseBoolean(session, r);
			} else if (flag == 'n') {
				o = parseNumber(session, r, null);
			} else if (flag == 'o') {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				o = bd.cast(m);
			} else if (flag == 'a') {
				Collection l = new ObjectList(bc);
				o = parseIntoCollection(session, r, l, sType.getElementType(), isUrlParamValue, pMeta);
			} else {
				throw new ParseException(session, "Unexpected flag character ''{0}''.", flag);
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
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(bc));
			o = parseIntoMap(session, r, m, sType.getKeyType(), sType.getValueType(), pMeta);
		} else if (sType.isCollection()) {
			if (flag == 'o') {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				// Handle case where it's a collection, but serialized as a map with a _type or _value key.
				if (m.containsKey(bc.getBeanTypePropertyName()))
					o = bd.cast(m);
				// Handle case where it's a collection, but only a single value was specified.
				else {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new ObjectList(bc));
					l.add(m.cast(sType.getElementType()));
					o = l;
				}
			} else {
				Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new ObjectList(bc));
				o = parseIntoCollection(session, r, l, sType.getElementType(), isUrlParamValue, pMeta);
			}
		} else if (sType.canCreateNewInstanceFromObjectMap(outer)) {
			ObjectMap m = new ObjectMap(bc);
			parseIntoMap(session, r, m, string(), object(), pMeta);
			o = sType.newInstanceFromObjectMap(outer, m);
		} else if (sType.canCreateNewBean(outer)) {
			BeanMap m = bc.newBeanMap(outer, sType.getInnerClass());
			m = parseIntoBeanMap(session, r, m);
			o = m == null ? null : m.getBean();
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			String s = parseString(session, r, isUrlParamValue);
			if (s != null)
				o = sType.newInstanceFromString(outer, s);
		} else if (sType.canCreateNewInstanceFromNumber(outer)) {
			o = sType.newInstanceFromNumber(outer, parseNumber(session, r, sType.getNewInstanceFromNumberClass()));
		} else if (sType.isArray()) {
			if (flag == 'o') {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				// Handle case where it's an array, but serialized as a map with a _type or _value key.
				if (m.containsKey(bc.getBeanTypePropertyName()))
					o = bd.cast(m);
				// Handle case where it's an array, but only a single value was specified.
				else {
					ArrayList l = new ArrayList(1);
					l.add(m.cast(sType.getElementType()));
					o = bc.toArray(sType, l);
				}
			} else {
				ArrayList l = (ArrayList)parseIntoCollection(session, r, new ArrayList(), sType.getElementType(), isUrlParamValue, pMeta);
				o = bc.toArray(sType, l);
			}
		} else if (flag == 'o') {
			// It could be a non-bean with _type attribute.
			ObjectMap m = new ObjectMap(bc);
			parseIntoMap(session, r, m, string(), object(), pMeta);
			if (m.containsKey(bc.getBeanTypePropertyName()))
				o = bd.cast(m);
			else
				throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else {
			throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		}

		if (transform != null && o != null)
			o = transform.unswap(o, eType, bc);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(UonParserSession session, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int c = r.read();
		if (c == -1 || c == NUL || c == AMP)
			return null;
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
					if ((c == '\n' || c == '\r') && session.isWhitespaceAware())
						skipSpace(r);
					else {
						r.unread();
						Object attr = parseAttr(session, r, session.isDecodeChars());
						currAttr = session.trim((attr == null ? null : session.getBeanContext().convertToType(attr, keyType)));
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

		int c = r.read();
		if (c == -1 || c == NUL || c == AMP)
			return null;

		// If we're parsing a top-level parameter, we're allowed to have comma-delimited lists outside parenthesis (e.g. "&foo=1,2,3&bar=a,b,c")
		// This is not allowed at lower levels since we use comma's as end delimiters.
		boolean isInParens = (c == '(');
		if (! isInParens)
			if (isUrlParamValue)
				r.unread();
			else
				throw new ParseException(session, "Could not find '(' marking beginning of collection.");

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
					} else if ((c == '\n' || c == '\r') && session.isWhitespaceAware()) {
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
					if ((c == '\n' || c == '\r') && session.isWhitespaceAware()) {
						skipSpace(r);
					} else {
						l.add(parseAnything(session, elementType, r.unread(), l, false, pMeta));
						state = S2;
					}
				} else if (state == S2) {
					if (c == ',') {
						state = S1;
					} else if ((c == '\n' || c == '\r') && session.isWhitespaceAware()) {
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

		BeanContext bc = session.getBeanContext();

		int c = r.read();
		if (c == -1 || c == NUL || c == AMP)
			return null;
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
					if ((c == '\n' || c == '\r') && session.isWhitespaceAware())
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
						if (! currAttr.equals(bc.getBeanTypePropertyName())) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								if (m.getMeta().isSubTyped()) {
									m.put(currAttr, "");
								} else {
									onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
								}
							} else {
								Object value = session.getBeanContext().convertToType("", pMeta.getClassMeta());
								pMeta.set(m, value);
							}
						}
						if (c == -1 || c == ')' || c == AMP)
							return m;
						state = S1;
					} else {
						if (! currAttr.equals(bc.getBeanTypePropertyName())) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								if (m.getMeta().isSubTyped()) {
									Object value = parseAnything(session, object(), r.unread(), m.getBean(false), false, null);
									m.put(currAttr, value);
								} else {
									onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
									parseAnything(session, object(), r.unread(), m.getBean(false), false, null); // Read content anyway to ignore it
								}
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

	Object parseAttr(UonParserSession session, ParserReader r, boolean encoded) throws Exception {
		Object attr;
		int c = r.peek();
		if (c == '$') {
			char f = readFlag(session, r, (char)0);
			if (f == 'b')
				attr = parseBoolean(session, r);
			else if (f == 'n')
				attr = parseNumber(session, r, null);
			else
				attr = parseAttrName(session, r, encoded);
		} else {
			attr = parseAttrName(session, r, encoded);
		}
		return attr;
	}

	String parseAttrName(UonParserSession session, ParserReader r, boolean encoded) throws Exception {

		// If string is of form '(xxx)', we're looking for ')' at the end.
		// Otherwise, we're looking for '&' or '=' or -1 denoting the end of this string.

		int c = r.peek();
		if (c == '$')
			readFlag(session, r, 's');
		if (c == '(')
			return parsePString(session, r);

		r.mark();
		boolean isInEscape = false;
		if (encoded) {
			while (c != -1) {
				c = r.read();
				if (! isInEscape) {
					if (c == AMP || c == EQ || c == -1) {
						if (c != -1)
							r.unread();
						String s = r.getMarked();
						return (s.equals("\u0000") ? null : s);
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
					if (c == '=' || c == -1) {
						if (c != -1)
							r.unread();
						String s = r.getMarked();
						return (s.equals("\u0000") ? null : session.trim(s));
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

		// If string is of form '(xxx)', we're looking for ')' at the end.
		// Otherwise, we're looking for ',' or ')' or -1 denoting the end of this string.

		int c = r.peek();
		if (c == '(')
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
			else if ((c == '\n' || c == '\r') && session.isWhitespaceAware()) {
				s = r.getMarked(0, -1);
				skipSpace(r);
				c = -1;
			}
			isInEscape = isInEscape(c, r, isInEscape);
		}

		return (s == null || s.equals("\u0000") ? null : session.trim(s));
	}

	private static final AsciiSet endCharsParam = new AsciiSet(""+AMP), endCharsNormal = new AsciiSet(",)"+AMP);


	/**
	 * Parses a string of the form "(foo)"
	 * All whitespace within parenthesis are preserved.
	 */
	static String parsePString(UonParserSession session, ParserReader r) throws Exception {

		r.read(); // Skip first parenthesis.
		r.mark();
		int c = 0;

		boolean isInEscape = false;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (c == ')')
					return session.trim(r.getMarked(0, -1));
			}
			if (c == EQ)
				r.replace('=');
			isInEscape = isInEscape(c, r, isInEscape);
		}
		throw new ParseException(session, "Unmatched parenthesis");
	}

	private Boolean parseBoolean(UonParserSession session, ParserReader r) throws Exception {
		readFlag(session, r, 'b');
		String s = parseString(session, r, false);
		if (s == null)
			return null;
		if (s.equals("true"))
			return true;
		if (s.equals("false"))
			return false;
		throw new ParseException(session, "Unrecognized syntax for boolean.  ''{0}''.", s);
	}

	private Number parseNumber(UonParserSession session, ParserReader r, Class<? extends Number> c) throws Exception {
		readFlag(session, r, 'n');
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
		int c = r.read();
		if (c != -1)
			throw new ParseException(session, "Remainder after parse: ''{0}''.", (char)c);
	}

	/**
	 * Reads flag character from "$x(" construct if flag is present.
	 * Returns 0 if no flag is present.
	 */
	static char readFlag(UonParserSession session, ParserReader r, char expected) throws Exception {
		char c = (char)r.peek();
		if (c == '$') {
			r.read();
			char f = (char)r.read();
			if (expected != 0 && f != expected)
				throw new ParseException(session, "Unexpected flag character: ''{0}''.  Expected ''{1}''.", f, expected);
			c = (char)r.peek();
			// Type flag must be followed by '('
			if (c != '(')
				throw new ParseException(session, "Unexpected character following flag: ''{0}''.", c);
			return f;
		}
		return 0;
	}

	private Object[] parseArgs(UonParserSession session, ParserReader r, ClassMeta<?>[] argTypes) throws Exception {

		final int S1=1; // Looking for start of entry
		final int S2=2; // Looking for , or )

		Object[] o = new Object[argTypes.length];
		int i = 0;

		int c = r.read();
		if (c == -1 || c == AMP)
			return null;
		if (c != '(')
			throw new ParseException(session, "Expected '(' at beginning of args array.");

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
			if (c > ' ' || c <= 2) {
				r.unread();
				return;
			}
		}
	}

	UonParserSession createParameterContext(Object input) {
		return new UonParserSession(getContext(UonParserContext.class), getBeanContext(), input);
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UonParserSession createSession(Object input, ObjectMap properties, Method javaMethod, Object outer) {
		return new UonParserSession(getContext(UonParserContext.class), getBeanContext(), input, properties, javaMethod, outer);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		UonParserSession s = (UonParserSession)session;
		type = s.getBeanContext().normalizeClassMeta(type);
		UonReader r = s.getReader();
		T o = parseAnything(s, type, r, s.getOuter(), true, null);
		validateEnd(s, r);
		return o;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		readFlag(s, r, 'o');
		m = parseIntoMap(s, r, m, s.getBeanContext().getClassMeta(keyType), s.getBeanContext().getClassMeta(valueType), null);
		validateEnd(s, r);
		return m;
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(ParserSession session, Collection<E> c, Type elementType) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		readFlag(s, r, 'a');
		c = parseIntoCollection(s, r, c, s.getBeanContext().getClassMeta(elementType), false, null);
		validateEnd(s, r);
		return c;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<?>[] argTypes) throws Exception {
		UonParserSession s = (UonParserSession)session;
		UonReader r = s.getReader();
		readFlag(s, r, 'a');
		Object[] a = parseArgs(s, r, argTypes);
		return a;
	}

	@Override /* Parser */
	public UonParser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addToDictionary(Class<?>...classes) throws LockedException {
		super.addToDictionary(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> UonParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

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
