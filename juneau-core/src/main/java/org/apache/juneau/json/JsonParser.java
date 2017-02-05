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
package org.apache.juneau.json;

import static org.apache.juneau.json.JsonParserContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.MediaType;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parses any valid JSON text into a POJO model.
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>application/json, text/json</code>
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This parser uses a state machine, which makes it very fast and efficient.  It parses JSON in about 70% of the
 * 	time that it takes the built-in Java DOM parsers to parse equivalent XML.
 * <p>
 * 	This parser handles all valid JSON syntax.
 * 	In addition, when strict mode is disable, the parser also handles the following:
 * 	<ul class='spaced-list'>
 * 		<li> Javascript comments (both {@code /*} and {@code //}) are ignored.
 * 		<li> Both single and double quoted strings.
 * 		<li> Automatically joins concatenated strings (e.g. <code><js>"aaa"</js> + <js>'bbb'</js></code>).
 * 		<li> Unquoted attributes.
 * 	</ul>
 * 	Also handles negative, decimal, hexadecimal, octal, and double numbers, including exponential notation.
 * <p>
 * 	This parser handles the following input, and automatically returns the corresponding Java class.
 * 	<ul class='spaced-list'>
 * 		<li> JSON objects (<js>"{...}"</js>) are converted to {@link ObjectMap ObjectMaps}.  <br>
 * 				Note:  If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object, then an attempt is made to convert the object
 * 				to an instance of the specified Java bean class.  See the classProperty setting on the {@link ContextFactory} for more information
 * 				about parsing beans from JSON.
 * 		<li> JSON arrays (<js>"[...]"</js>) are converted to {@link ObjectList ObjectLists}.
 * 		<li> JSON string literals (<js>"'xyz'"</js>) are converted to {@link String Strings}.
 * 		<li> JSON numbers (<js>"123"</js>, including octal/hexadecimal/exponential notation) are converted to {@link Integer Integers},
 * 				{@link Long Longs}, {@link Float Floats}, or {@link Double Doubles} depending on whether the number is decimal, and the size of the number.
 * 		<li> JSON booleans (<js>"false"</js>) are converted to {@link Boolean Booleans}.
 * 		<li> JSON nulls (<js>"null"</js>) are converted to <jk>null</jk>.
 * 		<li> Input consisting of only whitespace or JSON comments are converted to <jk>null</jk>.
 * 	</ul>
 * <p>
 * 	Input can be any of the following:<br>
 * 	<ul class='spaced-list'>
 * 		<li> <js>"{...}"</js> - Converted to a {@link ObjectMap} or an instance of a Java bean if a <xa>_type</xa> attribute is present.
 *  		<li> <js>"[...]"</js> - Converted to a {@link ObjectList}.
 *  		<li> <js>"123..."</js> - Converted to a {@link Number} (either {@link Integer}, {@link Long}, {@link Float}, or {@link Double}).
 *  		<li> <js>"true"</js>/<js>"false"</js> - Converted to a {@link Boolean}.
 *  		<li> <js>"null"</js> - Returns <jk>null</jk>.
 *  		<li> <js>"'xxx'"</js> - Converted to a {@link String}.
 *  		<li> <js>"\"xxx\""</js> - Converted to a {@link String}.
 *  		<li> <js>"'xxx' + \"yyy\""</js> - Converted to a concatenated {@link String}.
 * 	</ul>
  * <p>
 * 	TIP:  If you know you're parsing a JSON object or array, it can be easier to parse it using the {@link ObjectMap#ObjectMap(CharSequence) ObjectMap(CharSequence)}
 * 		or {@link ObjectList#ObjectList(CharSequence) ObjectList(CharSequence)} constructors instead of using this class.  The end result should be the same.
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link JsonParserContext}
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes("application/json,text/json")
public final class JsonParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT = new JsonParser().lock();

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT_STRICT = new JsonParser().setProperty(PARSER_strict, true).lock();

	private static final AsciiSet decChars = new AsciiSet("0123456789");

	private <T> T parseAnything(JsonParserSession session, ClassMeta<T> eType, ParserReader r, Object outer, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();
		session.setCurrentClass(sType);
		String wrapperAttr = sType.getExtendedMeta(JsonClassMeta.class).getWrapperAttr();
		BeanRegistry breg = pMeta == null ? session.getBeanRegistry() : pMeta.getBeanRegistry();

		Object o = null;

		skipCommentsAndSpace(session, r);
		if (wrapperAttr != null)
			skipWrapperAttrStart(session, r, wrapperAttr);
		int c = r.peek();
		if (c == -1) {
			if (session.isStrict())
				throw new ParseException(session, "Empty input.");
			// Let o be null.
		} else if ((c == ',' || c == '}' || c == ']')) {
			if (session.isStrict())
				throw new ParseException(session, "Missing value detected.");
			// Handle bug in Cognos 10.2.1 that can product non-existent values.
			// Let o be null;
		} else if (c == 'n') {
			parseKeyword(session, "null", r);
		} else if (sType.isObject()) {
			if (c == '{') {
				ObjectMap m2 = new ObjectMap(session);
				parseIntoMap2(session, r, m2, string(), object(), pMeta);
				o = breg.cast(m2);
			} else if (c == '[') {
				o = parseIntoCollection2(session, r, new ObjectList(session), object(), pMeta);
			} else if (c == '\'' || c == '"') {
				o = parseString(session, r);
				if (sType.isChar())
					o = o.toString().charAt(0);
			} else if (c >= '0' && c <= '9' || c == '-' || c == '.') {
				o = parseNumber(session, r, null);
			} else if (c == 't') {
				parseKeyword(session, "true", r);
				o = Boolean.TRUE;
			} else {
				parseKeyword(session, "false", r);
				o = Boolean.FALSE;
			}
		} else if (sType.isBoolean()) {
			o = parseBoolean(session, r);
		} else if (sType.isCharSequence()) {
			o = parseString(session, r);
		} else if (sType.isChar()) {
			o = parseString(session, r).charAt(0);
		} else if (sType.isNumber()) {
			o = parseNumber(session, r, (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(session));
			o = parseIntoMap2(session, r, m, sType.getKeyType(), sType.getValueType(), pMeta);
		} else if (sType.isCollection()) {
			if (c == '{') {
				ObjectMap m = new ObjectMap(session);
				parseIntoMap2(session, r, m, string(), object(), pMeta);
				o = breg.cast(m);
			} else {
				Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : new ObjectList(session));
				o = parseIntoCollection2(session, r, l, sType.getElementType(), pMeta);
			}
		} else if (sType.canCreateNewInstanceFromObjectMap(outer)) {
			ObjectMap m = new ObjectMap(session);
			parseIntoMap2(session, r, m, string(), object(), pMeta);
			o = sType.newInstanceFromObjectMap(session, outer, m);
		} else if (sType.canCreateNewBean(outer)) {
			BeanMap m = session.newBeanMap(outer, sType.getInnerClass());
			o = parseIntoBeanMap2(session, r, m).getBean();
		} else if (sType.canCreateNewInstanceFromString(outer) && (c == '\'' || c == '"')) {
			o = sType.newInstanceFromString(outer, parseString(session, r));
		} else if (sType.canCreateNewInstanceFromNumber(outer) && StringUtils.isFirstNumberChar((char)c)) {
			o = sType.newInstanceFromNumber(session, outer, parseNumber(session, r, sType.getNewInstanceFromNumberClass()));
		} else if (sType.isArray()) {
			if (c == '{') {
				ObjectMap m = new ObjectMap(session);
				parseIntoMap2(session, r, m, string(), object(), pMeta);
				o = breg.cast(m);
			} else {
				ArrayList l = (ArrayList)parseIntoCollection2(session, r, new ArrayList(), sType.getElementType(), pMeta);
				o = session.toArray(sType, l);
			}
		} else if (c == '{') {
			Map m = new ObjectMap(session);
			parseIntoMap2(session, r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (m.containsKey(session.getBeanTypePropertyName()))
				o = breg.cast((ObjectMap)m);
			else
				throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else if (sType.canCreateNewInstanceFromString(outer) && ! session.isStrict()) {
			o = sType.newInstanceFromString(outer, parseString(session, r));
		} else {
			throw new ParseException(session, "Unrecognized syntax for class type ''{0}'', starting character ''{1}''", sType, (char)c);
		}

		if (wrapperAttr != null)
			skipWrapperAttrEnd(session, r);

		if (transform != null && o != null)
			o = transform.unswap(session, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private Number parseNumber(JsonParserSession session, ParserReader r, Class<? extends Number> type) throws Exception {
		int c = r.peek();
		if (c == '\'' || c == '"')
			return parseNumber(session, parseString(session, r), type);
		return parseNumber(session, StringUtils.parseNumberString(r), type);
	}

	private Number parseNumber(JsonParserSession session, String s, Class<? extends Number> type) throws Exception {

		// JSON has slightly different number rules from Java.
		// Strict mode enforces these different rules, lax does not.
		if (session.isStrict()) {

			// Lax allows blank strings to represent 0.
			// Strict does not allow blank strings.
			if (s.length() == 0)
				throw new ParseException(session, "Invalid JSON number: '"+s+"'");

			// Need to weed out octal and hexadecimal formats:  0123,-0123,0x123,-0x123.
			// Don't weed out 0 or -0.
			boolean isNegative = false;
			char c = (s.length() == 0 ? 'x' : s.charAt(0));
			if (c == '-') {
				isNegative = true;
				c = (s.length() == 1 ? 'x' : s.charAt(1));
			}

			// JSON doesn't allow '.123' and '-.123'.
			if (c == '.')
				throw new ParseException(session, "Invalid JSON number: '"+s+"'");

			// '01' is not a valid number, but '0.1', '0e1', '0e+1' are valid.
			if (c == '0' && s.length() > (isNegative ? 2 : 1)) {
				 char c2 = s.charAt((isNegative ? 2 : 1));
				 if (c2 != '.' && c2 != 'e' && c2 != 'E')
						throw new ParseException(session, "Invalid JSON number: '"+s+"'");
			}

			// JSON doesn't allow '1.' or '0.e1'.
			int i = s.indexOf('.');
			if (i != -1 && (s.length() == (i+1) || ! decChars.contains(s.charAt(i+1))))
				throw new ParseException(session, "Invalid JSON number: '"+s+"'");

		}
		return StringUtils.parseNumber(s, type);
	}

	private Boolean parseBoolean(JsonParserSession session, ParserReader r) throws Exception {
		int c = r.peek();
		if (c == '\'' || c == '"')
			return Boolean.valueOf(parseString(session, r));
		if (c == 't') {
			parseKeyword(session, "true", r);
			return Boolean.TRUE;
		}
		parseKeyword(session, "false", r);
		return Boolean.FALSE;
	}


	private <K,V> Map<K,V> parseIntoMap2(JsonParserSession session, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int S0=0; // Looking for outer {
		int S1=1; // Looking for attrName start.
		int S3=3; // Found attrName end, looking for :.
		int S4=4; // Found :, looking for valStart: { [ " ' LITERAL.
		int S5=5; // Looking for , or }
		int S6=6; // Found , looking for attr start.

		int state = S0;
		String currAttr = null;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (state == S0) {
				if (c == '{')
					state = S1;
			} else if (state == S1) {
				if (c == '}') {
					return m;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					currAttr = parseFieldName(session, r.unread());
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
			} else if (state == S4) {
				if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					K key = convertAttrToType(session, m, currAttr, keyType);
					V value = parseAnything(session, valueType, r.unread(), m, pMeta);
					setName(valueType, value, key);
					m.put(key, value);
					state = S5;
				}
			} else if (state == S5) {
				if (c == ',')
					state = S6;
				else if (session.isCommentOrWhitespace(c))
					skipCommentsAndSpace(session, r.unread());
				else if (c == '}') {
					return m;
				} else {
					break;
				}
			} else if (state == S6) {
				if (c == '}') {
					break;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					currAttr = parseFieldName(session, r.unread());
					state = S3;
				}
			}
		}
		if (state == S0)
			throw new ParseException(session, "Expected '{' at beginning of JSON object.");
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on JSON object.");
		if (state == S3)
			throw new ParseException(session, "Could not find ':' following attribute name on JSON object.");
		if (state == S4)
			throw new ParseException(session, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S5)
			throw new ParseException(session, "Could not find '}' marking end of JSON object.");
		if (state == S6)
			throw new ParseException(session, "Unexpected '}' found in JSON object.");

		return null; // Unreachable.
	}

	/*
	 * Parse a JSON attribute from the character array at the specified position, then
	 * set the position marker to the last character in the field name.
	 */
	private String parseFieldName(JsonParserSession session, ParserReader r) throws Exception {
		int c = r.peek();
		if (c == '\'' || c == '"')
			return parseString(session, r);
		if (session.isStrict())
			throw new ParseException(session, "Unquoted attribute detected.");
		r.mark();
		// Look for whitespace.
		while (c != -1) {
			c = r.read();
			if (c == ':' || session.isWhitespace(c) || c == '/') {
				r.unread();
				String s = r.getMarked().intern();
				return s.equals("null") ? null : s;
			}
		}
		throw new ParseException(session, "Could not find the end of the field name.");
	}

	private <E> Collection<E> parseIntoCollection2(JsonParserSession session, ParserReader r, Collection<E> l, ClassMeta<E> elementType, BeanPropertyMeta pMeta) throws Exception {

		int S0=0; // Looking for outermost [
		int S1=1; // Looking for starting [ or { or " or ' or LITERAL or ]
		int S2=2; // Looking for , or ]
		int S3=3; // Looking for starting [ or { or " or ' or LITERAL

		int state = S0;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (state == S0) {
				if (c == '[')
					state = S1;
			} else if (state == S1) {
				if (c == ']') {
					return l;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else if (c != -1) {
					l.add(parseAnything(session, elementType, r.unread(), l, pMeta));
					state = S2;
				}
			} else if (state == S2) {
				if (c == ',') {
					state = S3;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else if (c == ']') {
					return l;
				} else {
					break;  // Invalid character found.
				}
			} else if (state == S3) {
				if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else if (c == ']') {
					break;
				} else if (c != -1) {
					l.add(parseAnything(session, elementType, r.unread(), l, pMeta));
					state = S2;
				}
			}
		}
		if (state == S0)
			throw new ParseException(session, "Expected '[' at beginning of JSON array.");
		if (state == S1)
			throw new ParseException(session, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S2)
			throw new ParseException(session, "Expected ',' or ']'.");
		if (state == S3)
			throw new ParseException(session, "Unexpected trailing comma in array.");

		return null;  // Unreachable.
	}

	private Object[] parseArgs(JsonParserSession session, ParserReader r, ClassMeta<?>[] argTypes) throws Exception {

		int S0=0; // Looking for outermost [
		int S1=1; // Looking for starting [ or { or " or ' or LITERAL
		int S2=2; // Looking for , or ]

		Object[] o = new Object[argTypes.length];
		int i = 0;

		int state = S0;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (state == S0) {
				if (c == '[')
					state = S1;
			} else if (state == S1) {
				if (c == ']') {
					return o;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					o[i] = parseAnything(session, argTypes[i], r.unread(), session.getOuter(), null);
					i++;
					state = S2;
				}
			} else if (state == S2) {
				if (c == ',') {
					state = S1;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else if (c == ']') {
					return o;
				}
			}
		}
		if (state == S0)
			throw new ParseException(session, "Expected '[' at beginning of JSON array.");
		if (state == S1)
			throw new ParseException(session, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S2)
			throw new ParseException(session, "Expected ',' or ']'.");

		return null;  // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap2(JsonParserSession session, ParserReader r, BeanMap<T> m) throws Exception {

		int S0=0; // Looking for outer {
		int S1=1; // Looking for attrName start.
		int S3=3; // Found attrName end, looking for :.
		int S4=4; // Found :, looking for valStart: { [ " ' LITERAL.
		int S5=5; // Looking for , or }

		int state = S0;
		String currAttr = "";
		int c = 0;
		int currAttrLine = -1, currAttrCol = -1;
		while (c != -1) {
			c = r.read();
			if (state == S0) {
				if (c == '{')
					state = S1;
			} else if (state == S1) {
				if (c == '}') {
					return m;
				} else if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					r.unread();
					currAttrLine= r.getLine();
					currAttrCol = r.getColumn();
					currAttr = parseFieldName(session, r);
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
			} else if (state == S4) {
				if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					if (! currAttr.equals(session.getBeanTypePropertyName())) {
						BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
						session.setCurrentProperty(pMeta);
						if (pMeta == null) {
							if (m.getMeta().isSubTyped()) {
								Object value = parseAnything(session, object(), r.unread(), m.getBean(false), null);
								m.put(currAttr, value);
							} else {
								onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
								parseAnything(session, object(), r.unread(), m.getBean(false), null); // Read content anyway to ignore it
							}
						} else {
							ClassMeta<?> cm = pMeta.getClassMeta();
							Object value = parseAnything(session, cm, r.unread(), m.getBean(false), pMeta);
							setName(cm, value, currAttr);
							pMeta.set(m, value);
						}
						session.setCurrentProperty(null);
					}
					state = S5;
				}
			} else if (state == S5) {
				if (c == ',')
					state = S1;
				else if (session.isCommentOrWhitespace(c))
					skipCommentsAndSpace(session, r.unread());
				else if (c == '}') {
					return m;
				}
			}
		}
		if (state == S0)
			throw new ParseException(session, "Expected '{' at beginning of JSON object.");
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on JSON object.");
		if (state == S3)
			throw new ParseException(session, "Could not find ':' following attribute name on JSON object.");
		if (state == S4)
			throw new ParseException(session, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S5)
			throw new ParseException(session, "Could not find '}' marking end of JSON object.");

		return null; // Unreachable.
	}

	/*
	 * Starting from the specified position in the character array, returns the
	 * position of the character " or '.
	 * If the string consists of a concatenation of strings (e.g. 'AAA' + "BBB"), this method
	 * will automatically concatenate the strings and return the result.
	 */
	private String parseString(JsonParserSession session, ParserReader r) throws Exception  {
		r.mark();
		int qc = r.read();		// The quote character being used (" or ')
		if (qc != '"' && session.isStrict()) {
			String msg = (qc == '\'' ? "Invalid quote character \"{0}\" being used." : "Did not find quote character marking beginning of string.  Character=\"{0}\"");
			throw new ParseException(session, msg, (char)qc);
		}
		final boolean isQuoted = (qc == '\'' || qc == '"');
		String s = null;
		boolean isInEscape = false;
		int c = 0;
		while (c != -1) {
			c = r.read();
			// Strict syntax requires that all control characters be escaped.
			if (session.isStrict() && c <= 0x1F)
				throw new ParseException("Unescaped control character encountered: ''0x{0}''", String.format("%04X", c));
			if (isInEscape) {
				switch (c) {
					case 'n': r.replace('\n'); break;
					case 'r': r.replace('\r'); break;
					case 't': r.replace('\t'); break;
					case 'f': r.replace('\f'); break;
					case 'b': r.replace('\b'); break;
					case '\\': r.replace('\\'); break;
					case '/': r.replace('/'); break;
					case '\'': r.replace('\''); break;
					case '"': r.replace('"'); break;
					case 'u': {
						String n = r.read(4);
						try {
							r.replace(Integer.parseInt(n, 16), 6);
						} catch (NumberFormatException e) {
							throw new ParseException(session, "Invalid Unicode escape sequence in string.");
						}
						break;
					}
					default:
						throw new ParseException(session, "Invalid escape sequence in string.");
				}
				isInEscape = false;
			} else {
				if (c == '\\') {
					isInEscape = true;
					r.delete();
				} else if (isQuoted) {
					if (c == qc) {
						s = r.getMarked(1, -1);
						break;
					}
				} else {
					if (c == ',' || c == '}' || session.isWhitespace(c)) {
						s = r.getMarked(0, -1);
						r.unread();
						break;
					} else if (c == -1) {
						s = r.getMarked(0, 0);
						break;
					}
				}
			}
		}
		if (s == null)
			throw new ParseException(session, "Could not find expected end character ''{0}''.", (char)qc);

		// Look for concatenated string (i.e. whitespace followed by +).
		skipCommentsAndSpace(session, r);
		if (r.peek() == '+') {
			if (session.isStrict())
				throw new ParseException(session, "String concatenation detected.");
			r.read();	// Skip past '+'
			skipCommentsAndSpace(session, r);
			s += parseString(session, r);
		}
		return session.trim(s); // End of input reached.
	}

	/*
	 * Looks for the keywords true, false, or null.
	 * Throws an exception if any of these keywords are not found at the specified position.
	 */
	private void parseKeyword(JsonParserSession session, String keyword, ParserReader r) throws Exception {
		try {
			String s = r.read(keyword.length());
			if (s.equals(keyword))
				return;
			throw new ParseException(session, "Unrecognized syntax.");
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(session, "Unrecognized syntax.");
		}
	}

	/*
	 * Doesn't actually parse anything, but moves the position beyond any whitespace or comments.
	 * If positionOnNext is 'true', then the cursor will be set to the point immediately after
	 * the comments and whitespace.  Otherwise, the cursor will be set to the last position of
	 * the comments and whitespace.
	 */
	private void skipCommentsAndSpace(JsonParserSession session, ParserReader r) throws Exception {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (! session.isWhitespace(c)) {
				if (c == '/') {
					if (session.isStrict())
						throw new ParseException(session, "Javascript comment detected.");
					skipComments(session, r);
				} else {
					r.unread();
					return;
				}
			}
		}
	}

	/*
	 * Doesn't actually parse anything, but moves the position beyond the construct "{wrapperAttr:" when
	 * the @Json.wrapperAttr() annotation is used on a class.
	 */
	private void skipWrapperAttrStart(JsonParserSession session, ParserReader r, String wrapperAttr) throws Exception {

		int S0=0; // Looking for outer {
		int S1=1; // Looking for attrName start.
		int S3=3; // Found attrName end, looking for :.
		int S4=4; // Found :, looking for valStart: { [ " ' LITERAL.

		int state = S0;
		String currAttr = null;
		int c = 0;
		while (c != -1) {
			c = r.read();
			if (state == S0) {
				if (c == '{')
					state = S1;
			} else if (state == S1) {
				if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					currAttr = parseFieldName(session, r.unread());
					if (! currAttr.equals(wrapperAttr))
						throw new ParseException(session, "Expected to find wrapper attribute ''{0}'' but found attribute ''{1}''", wrapperAttr, currAttr);
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
			} else if (state == S4) {
				if (session.isCommentOrWhitespace(c)) {
					skipCommentsAndSpace(session, r.unread());
				} else {
					r.unread();
					return;
				}
			}
		}
		if (state == S0)
			throw new ParseException(session, "Expected '{' at beginning of JSON object.");
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on JSON object.");
		if (state == S3)
			throw new ParseException(session, "Could not find ':' following attribute name on JSON object.");
		if (state == S4)
			throw new ParseException(session, "Expected one of the following characters: {,[,',\",LITERAL.");
	}

	/*
	 * Doesn't actually parse anything, but moves the position beyond the construct "}" when
	 * the @Json.wrapperAttr() annotation is used on a class.
	 */
	private void skipWrapperAttrEnd(JsonParserSession session, ParserReader r) throws ParseException, IOException {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (! session.isWhitespace(c)) {
				if (c == '/') {
					if (session.isStrict())
						throw new ParseException(session, "Javascript comment detected.");
					skipComments(session, r);
				} else if (c == '}') {
					return;
				} else {
					throw new ParseException(session, "Could not find '}' at the end of JSON wrapper object.");
				}
			}
		}
	}

	/*
	 * Doesn't actually parse anything, but when positioned at the beginning of comment,
	 * it will move the pointer to the last character in the comment.
	 */
	private void skipComments(JsonParserSession session, ParserReader r) throws ParseException, IOException {
		int c = r.read();
		//  "/* */" style comments
		if (c == '*') {
			while (c != -1)
				if ((c = r.read()) == '*')
					if ((c = r.read()) == '/')
						return;
		//  "//" style comments
		} else if (c == '/') {
			while (c != -1) {
				c = r.read();
				if (c == -1 || c == '\n')
					return;
			}
		}
		throw new ParseException(session, "Open ended comment.");
	}

	/*
	 * Call this method after you've finished a parsing a string to make sure that if there's any
	 * remainder in the input, that it consists only of whitespace and comments.
	 */
	private void validateEnd(JsonParserSession session, ParserReader r) throws Exception {
		skipCommentsAndSpace(session, r);
		int c = r.read();
		if (c != -1 && c != ';')  // var x = {...}; expressions can end with a semicolon.
			throw new ParseException(session, "Remainder after parse: ''{0}''.", (char)c);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public JsonParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new JsonParserSession(getContext(JsonParserContext.class), op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		JsonParserSession s = (JsonParserSession)session;
		type = s.normalizeClassMeta(type);
		ParserReader r = s.getReader();
		if (r == null)
			return null;
		T o = parseAnything(s, type, r, s.getOuter(), null);
		validateEnd(s, r);
		return o;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		JsonParserSession s = (JsonParserSession)session;
		ParserReader r = s.getReader();
		m = parseIntoMap2(s, r, m, s.getClassMeta(keyType), s.getClassMeta(valueType), null);
		validateEnd(s, r);
		return m;
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(ParserSession session, Collection<E> c, Type elementType) throws Exception {
		JsonParserSession s = (JsonParserSession)session;
		ParserReader r = s.getReader();
		c = parseIntoCollection2(s, r, c, s.getClassMeta(elementType), null);
		validateEnd(s, r);
		return c;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<?>[] argTypes) throws Exception {
		JsonParserSession s = (JsonParserSession)session;
		ParserReader r = s.getReader();
		Object[] a = parseArgs(s, r, argTypes);
		validateEnd(s, r);
		return a;
	}

	@Override /* Parser */
	public JsonParser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser addToDictionary(Class<?>...classes) throws LockedException {
		super.addToDictionary(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> JsonParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public JsonParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public JsonParser clone() {
		try {
			return (JsonParser)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
