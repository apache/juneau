/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import static com.ibm.juno.core.json.JsonParserProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

/**
 * Parses any valid JSON text into a POJO model.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>application/json, text/json</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This parser uses a state machine, which makes it very fast and efficient.  It parses JSON in about 70% of the
 * 	time that it takes the built-in Java DOM parsers to parse equivalent XML.
 * <p>
 * 	This parser handles all valid JSON syntax.
 * 	In addition, when strict mode is disable, the parser also handles the following:
 * 	<ul>
 * 		<li> Javascript comments (both {@code /*} and {@code //}) are ignored.
 * 		<li> Both single and double quoted strings.
 * 		<li> Automatically joins concatenated strings (e.g. <code><js>"aaa"</js> + <js>'bbb'</js></code>).
 * 		<li> Unquoted attributes.
 * 	</ul>
 * 	Also handles negative, decimal, hexadecimal, octal, and double numbers, including exponential notation.
 * <p>
 * 	This parser handles the following input, and automatically returns the corresponding Java class.
 * 	<ul>
 * 		<li> JSON objects (<js>"{...}"</js>) are converted to {@link ObjectMap ObjectMaps}.  <br>
 * 				Note:  If a <code><xa>_class</xa>=<xs>'xxx'</xs></code> attribute is specified on the object, then an attempt is made to convert the object
 * 				to an instance of the specified Java bean class.  See the classProperty setting on the {@link BeanContextFactory} for more information
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
 * 	<ul>
 * 		<li> <js>"{...}"</js> - Converted to a {@link ObjectMap} or an instance of a Java bean if a <xa>_class</xa> attribute is present.
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
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes({"application/json","text/json"})
public final class JsonParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT = new JsonParser().lock();

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT_STRICT = new JsonParser().setProperty(JSON_strictMode, true).lock();

	/** JSON specific properties currently defined on this class */
	protected transient JsonParserProperties jpp = new JsonParserProperties();

	private <T> T parseAnything(JsonParserContext ctx, ClassMeta<T> nt, ParserReader r, BeanPropertyMeta p, Object outer, Object name) throws ParseException {

		BeanContext bc = ctx.getBeanContext();
		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoFilter<T,Object> filter = (PojoFilter<T,Object>)nt.getPojoFilter();
		ClassMeta<?> ft = nt.getFilteredClassMeta();
		String wrapperAttr = ft.getJsonMeta().getWrapperAttr();

		int line = r.getLine();
		int column = r.getColumn();
		Object o = null;
		try {
			skipCommentsAndSpace(ctx, r);
			if (wrapperAttr != null)
				skipWrapperAttrStart(ctx, r, wrapperAttr);
			int c = r.peek();
			if (c == -1) {
				// Let o be null.
			} else if ((c == ',' || c == '}' || c == ']')) {
				if (ctx.isStrictMode())
					throw new ParseException(line, column, "Missing value detected.");
				// Handle bug in Cognos 10.2.1 that can product non-existent values.
				// Let o be null;
			} else if (c == 'n') {
				parseKeyword("null", r);
			} else if (ft.isObject()) {
				if (c == '{') {
					ObjectMap m2 = new ObjectMap(bc);
					parseIntoMap2(ctx, r, m2, string(), object());
					o = m2.cast();
				} else if (c == '[')
					o = parseIntoCollection2(ctx, r, new ObjectList(bc), object());
				else if (c == '\'' || c == '"') {
					o = parseString(ctx, r);
					if (ft.isChar())
						o = o.toString().charAt(0);
				}
				else if (c >= '0' && c <= '9' || c == '-')
					o = parseNumber(ctx, r, null);
				else if (c == 't') {
					parseKeyword("true", r);
					o = Boolean.TRUE;
				} else {
					parseKeyword("false", r);
					o = Boolean.FALSE;
				}
			} else if (ft.isBoolean()) {
				o = parseBoolean(ctx, r);
			} else if (ft.isCharSequence()) {
				o = parseString(ctx, r);
			} else if (ft.isChar()) {
				o = parseString(ctx, r).charAt(0);
			} else if (ft.isNumber()) {
				o = parseNumber(ctx, r, (Class<? extends Number>)ft.getInnerClass());
			} else if (ft.isMap()) {
				Map m = (ft.canCreateNewInstance(outer) ? (Map)ft.newInstance(outer) : new ObjectMap(bc));
				o = parseIntoMap2(ctx, r, m, ft.getKeyType(), ft.getValueType());
			} else if (ft.isCollection()) {
				if (c == '{') {
					ObjectMap m = new ObjectMap(bc);
					parseIntoMap2(ctx, r, m, string(), object());
					o = m.cast();
				} else {
					Collection l = (ft.canCreateNewInstance(outer) ? (Collection)ft.newInstance() : new ObjectList(bc));
					o = parseIntoCollection2(ctx, r, l, ft.getElementType());
				}
			} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap2(ctx, r, m, string(), object());
				o = ft.newInstanceFromObjectMap(outer, m);
			} else if (ft.canCreateNewBean(outer)) {
				BeanMap m = bc.newBeanMap(outer, ft.getInnerClass());
				o = parseIntoBeanMap2(ctx, r, m).getBean();
			} else if (ft.canCreateNewInstanceFromString(outer) && (c == '\'' || c == '"')) {
				o = ft.newInstanceFromString(outer, parseString(ctx, r));
			} else if (ft.isArray()) {
				if (c == '{') {
					ObjectMap m = new ObjectMap(bc);
					parseIntoMap2(ctx, r, m, string(), object());
					o = m.cast();
				} else {
					ArrayList l = (ArrayList)parseIntoCollection2(ctx, r, new ArrayList(), ft.getElementType());
					o = bc.toArray(ft, l);
				}
			} else if (c == '{' ) {
				Map m = new ObjectMap(bc);
				parseIntoMap2(ctx, r, m, ft.getKeyType(), ft.getValueType());
				if (m.containsKey("_class"))
					o = ((ObjectMap)m).cast();
				else
					throw new ParseException(line, column, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", ft.getInnerClass().getName(), ft.getNotABeanReason());
			} else if (ft.canCreateNewInstanceFromString(outer) && ! ctx.isStrictMode()) {
				o = ft.newInstanceFromString(outer, parseString(ctx, r));
			} else {
				throw new ParseException(line, column, "Unrecognized syntax for class type ''{0}'', starting character ''{1}''", ft, (char)c);
			}

			if (wrapperAttr != null)
				skipWrapperAttrEnd(ctx, r);

			if (filter != null && o != null)
				o = filter.unfilter(o, nt);

			if (outer != null)
				setParent(nt, o, outer);

			if (name != null)
				setName(nt, o, name);

			return (T)o;

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			if (p == null)
				throw new ParseException("Error occurred trying to parse into class ''{0}''", ft).initCause(e);
			throw new ParseException("Error occurred trying to parse value for bean property ''{0}'' on class ''{1}''",
				p.getName(), p.getBeanMeta().getClassMeta()
			).initCause(e);
		}
	}

	private Number parseNumber(JsonParserContext ctx, ParserReader r, Class<? extends Number> type) throws IOException, ParseException {
		int c = r.peek();
		if (c == '\'' || c == '"')
			return parseNumber(ctx, parseString(ctx, r), type);
		return parseNumber(ctx, StringUtils.parseNumberString(r), type);
	}

	private Number parseNumber(JsonParserContext ctx, String s, Class<? extends Number> type) throws ParseException {
		if (ctx.isStrictMode()) {
			// Need to weed out octal and hexadecimal formats:  0123,-0123,0x123,-0x123.
			// Don't weed out 0 or -0.
			// All other number formats are supported in JSON.
			boolean isNegative = false;
			char c = (s.length() == 0 ? 'x' : s.charAt(0));
			if (c == '-') {
				isNegative = true;
				c = (s.length() == 1 ? 'x' : s.charAt(1));
			}
			if (c == 'x' || (c == '0' && s.length() > (isNegative ? 2 : 1)))
				throw new NumberFormatException("Invalid JSON number '"+s+"'");
		}
		return StringUtils.parseNumber(s, type);
	}

	private Boolean parseBoolean(JsonParserContext ctx, ParserReader r) throws IOException, ParseException {
		int c = r.peek();
		if (c == '\'' || c == '"')
			return Boolean.valueOf(parseString(ctx, r));
		if (c == 't') {
			parseKeyword("true", r);
			return Boolean.TRUE;
		}
		parseKeyword("false", r);
		return Boolean.FALSE;
	}


	private <K,V> Map<K,V> parseIntoMap2(JsonParserContext ctx, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int S0=0; // Looking for outer {
		int S1=1; // Looking for attrName start.
		int S3=3; // Found attrName end, looking for :.
		int S4=4; // Found :, looking for valStart: { [ " ' LITERAL.
		int S5=5; // Looking for , or }

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
				} else if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					currAttr = parseFieldName(ctx, r.unread());
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
			} else if (state == S4) {
				if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					K key = convertAttrToType(m, currAttr, keyType);
					V value = parseAnything(ctx, valueType, r.unread(), null, m, key);
					m.put(key, value);
					state = S5;
				}
			} else if (state == S5) {
				if (c == ',')
					state = S1;
				else if (c == '/')
					skipCommentsAndSpace(ctx, r.unread());
				else if (c == '}') {
					return m;
				}
			}
		}
		if (state == S0)
			throw new ParseException(line, column, "Expected '{' at beginning of JSON object.");
		if (state == S1)
			throw new ParseException(line, column, "Could not find attribute name on JSON object.");
		if (state == S3)
			throw new ParseException(line, column, "Could not find ':' following attribute name on JSON object.");
		if (state == S4)
			throw new ParseException(line, column, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S5)
			throw new ParseException(line, column, "Could not find '}' marking end of JSON object.");

		return null; // Unreachable.
	}

	/*
	 * Parse a JSON attribute from the character array at the specified position, then
	 * set the position marker to the last character in the field name.
	 */
	private String parseFieldName(JsonParserContext ctx, ParserReader r) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();
		int c = r.peek();
		if (c == '\'' || c == '"')
			return parseString(ctx, r);
		if (ctx.isStrictMode())
			throw new ParseException(line, column, "Unquoted attribute detected.");
		r.mark();
		// Look for whitespace.
		while (c != -1) {
			c = r.read();
			if (c == ':' || Character.isWhitespace(c) || c == '/') {
				r.unread();
				String s = r.getMarked().intern();
				return s.equals("null") ? null : s;
			}
		}
		throw new ParseException(line, column, "Could not find the end of the field name.");
	}

	private <E> Collection<E> parseIntoCollection2(JsonParserContext ctx, ParserReader r, Collection<E> l, ClassMeta<E> elementType) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

		int S0=0; // Looking for outermost [
		int S1=1; // Looking for starting [ or { or " or ' or LITERAL
		int S2=2; // Looking for , or ]

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
				} else if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					l.add(parseAnything(ctx, elementType, r.unread(), null, l, null));
					state = S2;
				}
			} else if (state == S2) {
				if (c == ',') {
					state = S1;
				} else if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (c == ']') {
					return l;
				}
			}
		}
		if (state == S0)
			throw new ParseException(line, column, "Expected '[' at beginning of JSON array.");
		if (state == S1)
			throw new ParseException(line, column, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S2)
			throw new ParseException(line, column, "Expected ',' or ']'.");

		return null;  // Unreachable.
	}

	private Object[] parseArgs(JsonParserContext ctx, ParserReader r, ClassMeta<?>[] argTypes) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

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
				} else if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					o[i] = parseAnything(ctx, argTypes[i], r.unread(), null, ctx.getOuter(), null);
					i++;
					state = S2;
				}
			} else if (state == S2) {
				if (c == ',') {
					state = S1;
				} else if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (c == ']') {
					return o;
				}
			}
		}
		if (state == S0)
			throw new ParseException(line, column, "Expected '[' at beginning of JSON array.");
		if (state == S1)
			throw new ParseException(line, column, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S2)
			throw new ParseException(line, column, "Expected ',' or ']'.");

		return null;  // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap2(JsonParserContext ctx, ParserReader r, BeanMap<T> m) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

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
				} else if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					r.unread();
					currAttrLine= r.getLine();
					currAttrCol = r.getColumn();
					currAttr = parseFieldName(ctx, r);
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
			} else if (state == S4) {
				if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					if (! currAttr.equals("_class")) {
						BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
						if (pMeta == null) {
							if (m.getMeta().isSubTyped()) {
								m.put(currAttr, parseAnything(ctx, object(), r.unread(), null, m.getBean(false), currAttr));
							} else {
								onUnknownProperty(ctx, currAttr, m, currAttrLine, currAttrCol);
								parseAnything(ctx, object(), r.unread(), null, m.getBean(false), null); // Read content anyway to ignore it
							}
						} else {
							Object value = parseAnything(ctx, pMeta.getClassMeta(), r.unread(), pMeta, m.getBean(false), currAttr);
							pMeta.set(m, value);
						}
					}
					state = S5;
				}
			} else if (state == S5) {
				if (c == ',')
					state = S1;
				else if (c == '/')
					skipCommentsAndSpace(ctx, r.unread());
				else if (c == '}') {
					return m;
				}
			}
		}
		if (state == S0)
			throw new ParseException(line, column, "Expected '{' at beginning of JSON object.");
		if (state == S1)
			throw new ParseException(line, column, "Could not find attribute name on JSON object.");
		if (state == S3)
			throw new ParseException(line, column, "Could not find ':' following attribute name on JSON object.");
		if (state == S4)
			throw new ParseException(line, column, "Expected one of the following characters: {,[,',\",LITERAL.");
		if (state == S5)
			throw new ParseException(line, column, "Could not find '}' marking end of JSON object.");

		return null; // Unreachable.
	}

	/*
	 * Starting from the specified position in the character array, returns the
	 * position of the character " or '.
	 * If the string consists of a concatenation of strings (e.g. 'AAA' + "BBB"), this method
	 * will automatically concatenate the strings and return the result.
	 */
	private String parseString(JsonParserContext ctx, ParserReader r) throws ParseException, IOException  {
		int line = r.getLine();
		int column = r.getColumn();
		r.mark();
		int qc = r.read();		// The quote character being used (" or ')
		if (qc != '"' && ctx.isStrictMode()) {
			String msg = (qc == '\'' ? "Invalid quote character \"{0}\" being used." : "Did not find quote character marking beginning of string.  Character=\"{0}\"");
			throw new ParseException(line, column, msg, (char)qc);
		}
		final boolean isQuoted = (qc == '\'' || qc == '"');
		String s = null;
		boolean isInEscape = false;
		int c = 0;
		while (c != -1) {
			c = r.read();
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
						r.replace(Integer.parseInt(n, 16), 6);
						break;
					}
					default:
						throw new ParseException(line, column, "Invalid escape sequence in string.");
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
					if (c == ',' || c == '}' || Character.isWhitespace(c)) {
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
			throw new ParseException(line, column, "Could not find expected end character ''{0}''.", (char)qc);

		// Look for concatenated string (i.e. whitespace followed by +).
		skipCommentsAndSpace(ctx, r);
		if (r.peek() == '+') {
			if (ctx.isStrictMode())
				throw new ParseException(r.getLine(), r.getColumn(), "String concatenation detected.");
			r.read();	// Skip past '+'
			skipCommentsAndSpace(ctx, r);
			s += parseString(ctx, r);
		}
		return ctx.trim(s); // End of input reached.
	}

	/*
	 * Looks for the keywords true, false, or null.
	 * Throws an exception if any of these keywords are not found at the specified position.
	 */
	private void parseKeyword(String keyword, ParserReader r) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();
		try {
			String s = r.read(keyword.length());
			if (s.equals(keyword))
				return;
			throw new ParseException(line, column, "Unrecognized syntax.");
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(line, column, "Unrecognized syntax.");
		}
	}

	/*
	 * Doesn't actually parse anything, but moves the position beyond any whitespace or comments.
	 * If positionOnNext is 'true', then the cursor will be set to the point immediately after
	 * the comments and whitespace.  Otherwise, the cursor will be set to the last position of
	 * the comments and whitespace.
	 */
	private void skipCommentsAndSpace(JsonParserContext ctx, ParserReader r) throws ParseException, IOException {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (! Character.isWhitespace(c)) {
				if (c == '/') {
					if (ctx.isStrictMode())
						throw new ParseException(r.getLine(), r.getColumn(), "Javascript comment detected.");
					skipComments(r);
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
	private void skipWrapperAttrStart(JsonParserContext ctx, ParserReader r, String wrapperAttr) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

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
				if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					currAttr = parseFieldName(ctx, r.unread());
					if (! currAttr.equals(wrapperAttr))
						throw new ParseException(line, column, "Expected to find wrapper attribute ''{0}'' but found attribute ''{1}''", wrapperAttr, currAttr);
					state = S3;
				}
			} else if (state == S3) {
				if (c == ':')
					state = S4;
			} else if (state == S4) {
				if (c == '/') {
					skipCommentsAndSpace(ctx, r.unread());
				} else if (! Character.isWhitespace(c)) {
					r.unread();
					return;
				}
			}
		}
		if (state == S0)
			throw new ParseException(line, column, "Expected '{' at beginning of JSON object.");
		if (state == S1)
			throw new ParseException(line, column, "Could not find attribute name on JSON object.");
		if (state == S3)
			throw new ParseException(line, column, "Could not find ':' following attribute name on JSON object.");
		if (state == S4)
			throw new ParseException(line, column, "Expected one of the following characters: {,[,',\",LITERAL.");
	}

	/*
	 * Doesn't actually parse anything, but moves the position beyond the construct "}" when
	 * the @Json.wrapperAttr() annotation is used on a class.
	 */
	private void skipWrapperAttrEnd(JsonParserContext ctx, ParserReader r) throws ParseException, IOException {
		int c = 0;
		int line = r.getLine();
		int column = r.getColumn();
		while ((c = r.read()) != -1) {
			if (! Character.isWhitespace(c)) {
				if (c == '/') {
					if (ctx.isStrictMode())
						throw new ParseException(line, column, "Javascript comment detected.");
					skipComments(r);
				} else if (c == '}') {
					return;
				} else {
					throw new ParseException(line, column, "Could not find '}' at the end of JSON wrapper object.");
				}
			}
		}
	}

	/*
	 * Doesn't actually parse anything, but when positioned at the beginning of comment,
	 * it will move the pointer to the last character in the comment.
	 */
	private void skipComments(ParserReader r) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();
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
		throw new ParseException(line, column, "Open ended comment.");
	}

	/*
	 * Call this method after you've finished a parsing a string to make sure that if there's any
	 * remainder in the input, that it consists only of whitespace and comments.
	 */
	private void validateEnd(JsonParserContext ctx, ParserReader r) throws ParseException, IOException {
		skipCommentsAndSpace(ctx, r);
		int line = r.getLine();
		int column = r.getColumn();
		int c = r.read();
		if (c != -1 && c != ';')  // var x = {...}; expressions can end with a semicolon.
			throw new ParseException(line, column, "Remainder after parse: ''{0}''.", (char)c);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public JsonParserContext createContext(ObjectMap op, Method javaMethod, Object outer) {
		return new JsonParserContext(getBeanContext(), jpp, pp, op, javaMethod, outer);
	}

	@Override /* Parser */
	protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
		JsonParserContext jctx = (JsonParserContext)ctx;
		type = ctx.getBeanContext().normalizeClassMeta(type);
		ParserReader r = jctx.getReader(in, estimatedSize);
		T o = parseAnything(jctx, type, r, null, ctx.getOuter(), null);
		validateEnd(jctx, r);
		return o;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(Reader in, int estimatedSize, Map<K,V> m, Type keyType, Type valueType, ParserContext ctx) throws ParseException, IOException {
		JsonParserContext jctx = (JsonParserContext)ctx;
		ParserReader r = jctx.getReader(in, estimatedSize);
		m = parseIntoMap2(jctx, r, m, ctx.getBeanContext().getClassMeta(keyType), ctx.getBeanContext().getClassMeta(valueType));
		validateEnd(jctx, r);
		return m;
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(Reader in, int estimatedSize, Collection<E> c, Type elementType, ParserContext ctx) throws ParseException, IOException {
		JsonParserContext jctx = (JsonParserContext)ctx;
		ParserReader r = jctx.getReader(in, estimatedSize);
		c = parseIntoCollection2(jctx, r, c, ctx.getBeanContext().getClassMeta(elementType));
		validateEnd(jctx, r);
		return c;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(Reader in, int estimatedSize, ClassMeta<?>[] argTypes, ParserContext ctx) throws ParseException, IOException {
		JsonParserContext jctx = (JsonParserContext)ctx;
		ParserReader r = jctx.getReader(in, estimatedSize);
		Object[] a = parseArgs(jctx, r, argTypes);
		validateEnd(jctx, r);
		return a;
	}

	@Override /* Parser */
	public JsonParser setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! jpp.setProperty(property, value))
			super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public JsonParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public JsonParser addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
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
