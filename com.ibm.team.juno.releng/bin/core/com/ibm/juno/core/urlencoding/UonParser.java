/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2013, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import static com.ibm.juno.core.urlencoding.UonParserProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

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
 * 	<li>{@link UonParserProperties}
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
	 * Equivalent to <code><jk>new</jk> UrlEncodingParser().setProperty(UonParserProperties.<jsf>UON_decodeChars</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Decoding extends UonParser {
		/** Constructor */
		public Decoding() {
			setProperty(UON_decodeChars, true);
		}
	}

	/** UON parser properties currently set on this serializer. */
	protected transient UonParserProperties upp = new UonParserProperties();

	/** URL-Encoding properties currently set on this serializer. */
	protected transient UrlEncodingProperties uep = new UrlEncodingProperties();

	/**
	 * Workhorse method.
	 *
	 * @param nt The class type being parsed, or <jk>null</jk> if unknown.
	 * @param ctx The parser context for this parse.
	 * @param r The reader being parsed.
	 * @param p If this is a bean property value, the bean property meta, or null if this is not a bean property value being parsed.
	 * @param outer The outer object (for constructing nested inner classes).
	 * @param isUrlParamValue If <jk>true</jk>, then we're parsing a top-level URL-encoded value which is treated a bit different than the default case.
	 * @param name The parent field or map key name.
	 * @return The parsed object.
	 * @throws ParseException
	 */
	protected <T> T parseAnything(ClassMeta<T> nt, UonParserContext ctx, ParserReader r, BeanPropertyMeta p, Object outer, boolean isUrlParamValue, Object name) throws ParseException {

		BeanContext bc = ctx.getBeanContext();
		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoFilter<T,Object> filter = (PojoFilter<T,Object>)nt.getPojoFilter();
		ClassMeta<?> ft = nt.getFilteredClassMeta();

		int line = r.getLine();
		int column = r.getColumn();
		Object o = null;
		try {
			// Parse type flag '$x'
			char flag = readFlag(r, (char)0);

			int c = r.peek();

			if (c == -1 || c == AMP) {
				// If parameter is blank and it's an array or collection, return an empty list.
				if (ft.isArray() || ft.isCollection())
					o = ft.newInstance();
				else if (ft.isString() || ft.isObject())
					o = "";
				else if (ft.isPrimitive())
					o = ft.getPrimitiveDefault();
				// Otherwise, leave null.
			} else if (ft.isObject()) {
				if (flag == 0 || flag == 's') {
					o = parseString(r, isUrlParamValue, ctx);
				} else if (flag == 'b') {
					o = parseBoolean(r, ctx);
				} else if (flag == 'n') {
					o = parseNumber(r, null, ctx);
				} else if (flag == 'o') {
					ObjectMap m = new ObjectMap(bc);
					parseIntoMap(ctx, r, m, string(), object());
					o = m.cast();
				} else if (flag == 'a') {
					Collection l = new ObjectList(bc);
					o = parseIntoCollection(ctx, r, l, ft.getElementType(), isUrlParamValue);
				} else {
					throw new ParseException(line, column, "Unexpected flag character ''{0}''.", flag);
				}
			} else if (ft.isBoolean()) {
				o = parseBoolean(r, ctx);
			} else if (ft.isCharSequence()) {
				o = parseString(r, isUrlParamValue, ctx);
			} else if (ft.isChar()) {
				String s = parseString(r, isUrlParamValue, ctx);
				o = s == null ? null : s.charAt(0);
			} else if (ft.isNumber()) {
				o = parseNumber(r, (Class<? extends Number>)ft.getInnerClass(), ctx);
			} else if (ft.isMap()) {
				Map m = (ft.canCreateNewInstance(outer) ? (Map)ft.newInstance(outer) : new ObjectMap(bc));
				o = parseIntoMap(ctx, r, m, ft.getKeyType(), ft.getValueType());
			} else if (ft.isCollection()) {
				if (flag == 'o') {
					ObjectMap m = new ObjectMap(bc);
					parseIntoMap(ctx, r, m, string(), object());
					// Handle case where it's a collection, but serialized as a map with a _class or _value key.
					if (m.containsKey("_class") || m.containsKey("_value"))
						o = m.cast();
					// Handle case where it's a collection, but only a single value was specified.
					else {
						Collection l = (ft.canCreateNewInstance(outer) ? (Collection)ft.newInstance(outer) : new ObjectList(bc));
						l.add(m.cast(ft.getElementType()));
						o = l;
					}
				} else {
					Collection l = (ft.canCreateNewInstance(outer) ? (Collection)ft.newInstance(outer) : new ObjectList(bc));
					o = parseIntoCollection(ctx, r, l, ft.getElementType(), isUrlParamValue);
				}
			} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(ctx, r, m, string(), object());
				o = ft.newInstanceFromObjectMap(outer, m);
			} else if (ft.canCreateNewBean(outer)) {
				BeanMap m = bc.newBeanMap(outer, ft.getInnerClass());
				m = parseIntoBeanMap(ctx, r, m);
				o = m == null ? null : m.getBean();
			} else if (ft.canCreateNewInstanceFromString(outer)) {
				String s = parseString(r, isUrlParamValue, ctx);
				if (s != null)
					o = ft.newInstanceFromString(outer, s);
			} else if (ft.isArray()) {
				if (flag == 'o') {
					ObjectMap m = new ObjectMap(bc);
					parseIntoMap(ctx, r, m, string(), object());
					// Handle case where it's an array, but serialized as a map with a _class or _value key.
					if (m.containsKey("_class") || m.containsKey("_value"))
						o = m.cast();
					// Handle case where it's an array, but only a single value was specified.
					else {
						ArrayList l = new ArrayList(1);
						l.add(m.cast(ft.getElementType()));
						o = bc.toArray(ft, l);
					}
				} else {
					ArrayList l = (ArrayList)parseIntoCollection(ctx, r, new ArrayList(), ft.getElementType(), isUrlParamValue);
					o = bc.toArray(ft, l);
				}
			} else if (flag == 'o') {
				// It could be a non-bean with _class attribute.
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(ctx, r, m, string(), object());
				if (m.containsKey("_class"))
					o = m.cast();
				else
					throw new ParseException(line, column, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", ft.getInnerClass().getName(), ft.getNotABeanReason());
			} else {
				throw new ParseException(line, column, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", ft.getInnerClass().getName(), ft.getNotABeanReason());
			}

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

	private <K,V> Map<K,V> parseIntoMap(UonParserContext ctx, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int c = r.read();
		if (c == -1 || c == NUL || c == AMP)
			return null;
		if (c != '(')
			throw new ParseException(line, column, "Expected '(' at beginning of object.");

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
					if ((c == '\n' || c == '\r') && ctx.whitespaceAware)
						skipSpace(r);
					else {
						r.unread();
						Object attr = parseAttr(r, ctx.decodeChars, ctx);
						currAttr = (attr == null ? null : ctx.getBeanContext().convertToType(attr, keyType));
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
						V value = convertAttrToType(m, "", valueType);
						m.put(currAttr, value);
						if (c == -1 || c == ')' || c == AMP)
							return m;
						state = S1;
					} else  {
						V value = parseAnything(valueType, ctx, r.unread(), null, m, false, currAttr);
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
			throw new ParseException(line, column, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(line, column, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(line, column, "Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException(line, column, "Could not find ')' marking end of object.");

		return null; // Unreachable.
	}

	private <E> Collection<E> parseIntoCollection(UonParserContext ctx, ParserReader r, Collection<E> l, ClassMeta<E> elementType, boolean isUrlParamValue) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

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
				throw new ParseException(line, column, "Could not find '(' marking beginning of collection.");

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
							l.add(parseAnything(elementType, ctx, r.unread(), null, l, false, null));
							r.read();
						}
						return l;
					} else if ((c == '\n' || c == '\r') && ctx.whitespaceAware) {
						skipSpace(r);
					} else {
						l.add(parseAnything(elementType, ctx, r.unread(), null, l, false, null));
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
				throw new ParseException(line, column, "Could not find start of entry in array.");
			if (state == S3)
				throw new ParseException(line, column, "Could not find end of entry in array.");

		} else {
			final int S1=1; // Looking for starting of entry.
			final int S2=2; // Looking for , or & or END after first entry.

			int state = S1;
			while (c != -1 && c != AMP) {
				c = r.read();
				if (state == S1) {
					if ((c == '\n' || c == '\r') && ctx.whitespaceAware) {
						skipSpace(r);
					} else {
						l.add(parseAnything(elementType, ctx, r.unread(), null, l, false, null));
						state = S2;
					}
				} else if (state == S2) {
					if (c == ',') {
						state = S1;
					} else if ((c == '\n' || c == '\r') && ctx.whitespaceAware) {
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

	private <T> BeanMap<T> parseIntoBeanMap(UonParserContext ctx, ParserReader r, BeanMap<T> m) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

		int c = r.read();
		if (c == -1 || c == NUL || c == AMP)
			return null;
		if (c != '(')
			throw new ParseException(line, column, "Expected '(' at beginning of object.");

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
					if ((c == '\n' || c == '\r') && ctx.whitespaceAware)
						skipSpace(r);
					else {
						r.unread();
						currAttrLine= r.getLine();
						currAttrCol = r.getColumn();
						currAttr = parseAttrName(r, ctx.decodeChars);
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
						if (! currAttr.equals("_class")) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								if (m.getMeta().isSubTyped()) {
									m.put(currAttr, "");
								} else {
									onUnknownProperty(ctx, currAttr, m, currAttrLine, currAttrCol);
								}
							} else {
								Object value = ctx.getBeanContext().convertToType("", pMeta.getClassMeta());
								pMeta.set(m, value);
							}
						}
						if (c == -1 || c == ')' || c == AMP)
							return m;
						state = S1;
					} else {
						if (! currAttr.equals("_class")) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								if (m.getMeta().isSubTyped()) {
									m.put(currAttr, parseAnything(object(), ctx, r.unread(), null, m.getBean(false), false, currAttr));
								} else {
									onUnknownProperty(ctx, currAttr, m, currAttrLine, currAttrCol);
									parseAnything(object(), ctx, r.unread(), null, m.getBean(false), false, null); // Read content anyway to ignore it
								}
							} else {
								Object value = parseAnything(pMeta.getClassMeta(), ctx, r.unread(), pMeta, m.getBean(false), false, currAttr);
								pMeta.set(m, value);
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
			throw new ParseException(line, column, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(line, column, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(line, column, "Could not find value following '=' on object.");
		if (state == S4)
			throw new ParseException(line, column, "Could not find ')' marking end of object.");

		return null; // Unreachable.
	}

	Object parseAttr(ParserReader r, boolean encoded, UonParserContext ctx) throws IOException, ParseException {
		Object attr;
		int c = r.peek();
		if (c == '$') {
			char f = readFlag(r, (char)0);
			if (f == 'b')
				attr = parseBoolean(r, ctx);
			else if (f == 'n')
				attr = parseNumber(r, null, ctx);
			else
				attr = parseAttrName(r, encoded);
		} else {
			attr = parseAttrName(r, encoded);
		}
		return attr;
	}

	String parseAttrName(ParserReader r, boolean encoded) throws IOException, ParseException {

		// If string is of form '(xxx)', we're looking for ')' at the end.
		// Otherwise, we're looking for '&' or '=' or -1 denoting the end of this string.

		int line = r.getLine();
		int column = r.getColumn();
		int c = r.peek();
		if (c == '$')
			readFlag(r, 's');
		if (c == '(')
			return parsePString(r);

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
						return (s.equals("\u0000") ? null : s);
					}
				}
				isInEscape = isInEscape(c, r, isInEscape);
			}
		}

		// We should never get here.
		throw new ParseException(line, column, "Unexpected condition.");
	}


	/**
	 * Returns true if the next character in the stream is preceeded by an escape '~' character.
	 * @param c The current character.
	 * @param r The reader.
	 * @param prevIsInEscape What the flag was last time.
	 */
	private static final boolean isInEscape(int c, ParserReader r, boolean prevIsInEscape) throws IOException {
		if (c == '~' && ! prevIsInEscape) {
			c = r.peek();
			if (escapedChars.contains(c)) {
				r.delete();
				return true;
			}
		}
		return false;
	}

	String parseString(ParserReader r, boolean isUrlParamValue, UonParserContext ctx) throws IOException, ParseException {

		// If string is of form '(xxx)', we're looking for ')' at the end.
		// Otherwise, we're looking for ',' or ')' or -1 denoting the end of this string.

		int c = r.peek();
		if (c == '(')
			return parsePString(r);

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
			else if ((c == '\n' || c == '\r') && ctx.whitespaceAware) {
				s = r.getMarked(0, -1);
				skipSpace(r);
				c = -1;
			}
			isInEscape = isInEscape(c, r, isInEscape);
		}

		return (s == null || s.equals("\u0000") ? null : s);
	}

	private static final AsciiSet endCharsParam = new AsciiSet(""+AMP), endCharsNormal = new AsciiSet(",)"+AMP);


	/**
	 * Parses a string of the form "(foo)"
	 * All whitespace within parenthesis are preserved.
	 */
	static String parsePString(ParserReader r) throws IOException, ParseException {

		int line = r.getLine();
		int column = r.getColumn();
		r.read(); // Skip first parenthesis.
		r.mark();
		int c = 0;

		boolean isInEscape = false;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (c == ')')
					return r.getMarked(0, -1);
			}
			if (c == EQ)
				r.replace('=');
			isInEscape = isInEscape(c, r, isInEscape);
		}
		throw new ParseException(line, column, "Unmatched parenthesis");
	}

	private Boolean parseBoolean(ParserReader r, UonParserContext ctx) throws IOException, ParseException {
		int line = r.getLine();
		int column = r.getColumn();
		readFlag(r, 'b');
		String s = parseString(r, false, ctx);
		if (s == null)
			return null;
		if (s.equals("true"))
			return true;
		if (s.equals("false"))
			return false;
		throw new ParseException(line, column, "Unrecognized syntax for boolean.  ''{0}''.", s);
	}

	private Number parseNumber(ParserReader r, Class<? extends Number> c, UonParserContext ctx) throws IOException, ParseException {
		readFlag(r, 'n');
		String s = parseString(r, false, ctx);
		if (s == null)
			return null;
		return StringUtils.parseNumber(s, c);
	}

	/*
	 * Call this method after you've finished a parsing a string to make sure that if there's any
	 * remainder in the input, that it consists only of whitespace and comments.
	 */
	private void validateEnd(ParserReader r) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();
		int c = r.read();
		if (c != -1)
			throw new ParseException(line, column, "Remainder after parse: ''{0}''.", (char)c);
	}

	/**
	 * Reads flag character from "$x(" construct if flag is present.
	 * Returns 0 if no flag is present.
	 */
	static char readFlag(ParserReader r, char expected) throws IOException, ParseException {
		int line = r.getLine();
		int column = r.getColumn();
		char c = (char)r.peek();
		if (c == '$') {
			r.read();
			char f = (char)r.read();
			if (expected != 0 && f != expected)
				throw new ParseException(line, column, "Unexpected flag character: ''{0}''.  Expected ''{1}''.", f, expected);
			c = (char)r.peek();
			// Type flag must be followed by '('
			if (c != '(')
				throw new ParseException(line, column, "Unexpected character following flag: ''{0}''.", c);
			return f;
		}
		return 0;
	}

	private Object[] parseArgs(UonParserContext ctx, ParserReader r, ClassMeta<?>[] argTypes) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

		final int S1=1; // Looking for start of entry
		final int S2=2; // Looking for , or )

		Object[] o = new Object[argTypes.length];
		int i = 0;

		int c = r.read();
		if (c == -1 || c == AMP)
			return null;
		if (c != '(')
			throw new ParseException("Expected '(' at beginning of args array.");

		int state = S1;
		while (c != -1 && c != AMP) {
			c = r.read();
			if (state == S1) {
				if (c == ')')
					return o;
				o[i] = parseAnything(argTypes[i], ctx, r.unread(), null, ctx.getOuter(), false, null);
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

		throw new ParseException(line, column, "Did not find ')' at the end of args array.");
	}

	private static void skipSpace(ParserReader r) throws IOException {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (c > ' ' || c <= 2) {
				r.unread();
				return;
			}
		}
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UonParserContext createContext(ObjectMap properties, Method javaMethod, Object outer) {
		return new UonParserContext(getBeanContext(), pp, upp, uep, properties, javaMethod, outer);
	}

	@Override /* Parser */
	protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		type = ctx.getBeanContext().normalizeClassMeta(type);
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		T o = parseAnything(type, uctx, r, null, ctx.getOuter(), true, null);
		validateEnd(r);
		return o;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(Reader in, int estimatedSize, Map<K,V> m, Type keyType, Type valueType, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		readFlag(r, 'o');
		m = parseIntoMap(uctx, r, m, ctx.getBeanContext().getClassMeta(keyType), ctx.getBeanContext().getClassMeta(valueType));
		validateEnd(r);
		return m;
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(Reader in, int estimatedSize, Collection<E> c, Type elementType, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		readFlag(r, 'a');
		c = parseIntoCollection(uctx, r, c, ctx.getBeanContext().getClassMeta(elementType), false);
		validateEnd(r);
		return c;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(Reader in, int estimatedSize, ClassMeta<?>[] argTypes, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		readFlag(r, 'a');
		Object[] a = parseArgs(uctx, r, argTypes);
		return a;
	}

	@Override /* Parser */
	public UonParser setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! upp.setProperty(property, value))
			if (! uep.setProperty(property, value))
				super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UonParser setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public UonParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UonParser addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
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
			c.upp = upp.clone();
			c.uep = uep.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
