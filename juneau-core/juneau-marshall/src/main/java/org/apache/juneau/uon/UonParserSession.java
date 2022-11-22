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
package org.apache.juneau.uon;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link UonParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UonParserSession extends ReaderParserSession implements HttpPartParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	// Characters that need to be preceded with an escape character.
	private static final AsciiSet escapedChars = AsciiSet.create("~'\u0001\u0002");

	private static final char AMP='\u0001', EQ='\u0002';  // Flags set in reader to denote & and = characters.

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(UonParser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends ReaderParserSession.Builder {

		UonParser ctx;
		boolean decoding;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(UonParser ctx) {
			super(ctx);
			this.ctx = ctx;
			decoding = ctx.decoding;
		}

		@Override
		public UonParserSession build() {
			return new UonParserSession(this);
		}

		/**
		 * Overrides the decoding flag on the context for this session.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder decoding(boolean value) {
			decoding = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final UonParser ctx;
	private final boolean decoding;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected UonParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		decoding = builder.decoding;
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (UonReader r = getUonReader(pipe, decoding)) {
			T o = parseAnything(type, r, getOuter(), true, null);
			validateEnd(r);
			return o;
		}
	}

	@Override /* ReaderParserSession */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		try (UonReader r = getUonReader(pipe, decoding)) {
			m = parseIntoMap(r, m, (ClassMeta<K>)getClassMeta(keyType), (ClassMeta<V>)getClassMeta(valueType), null);
			validateEnd(r);
			return m;
		}
	}

	@Override /* ReaderParserSession */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
		try (UonReader r = getUonReader(pipe, decoding)) {
			c = parseIntoCollection(r, c, (ClassMeta<E>)getClassMeta(elementType), false, null);
			validateEnd(r);
			return c;
		}
	}

	@Override /* HttpPartParser */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
		if (in == null)
			return null;
		if (toType.isString() && in.length() > 0) {
			// Shortcut - If we're returning a string and the value doesn't start with "'" or is "null", then
			// just return the string since it's a plain value.
			// This allows us to bypass the creation of a UonParserSession object.
			char x = firstNonWhitespaceChar(in);
			if (x != '\'' && x != 'n' && in.indexOf('~') == -1)
				return (T)in;
			if (x == 'n' && "null".equals(in))
				return null;
		}
		try (ParserPipe pipe = createPipe(in)) {
			try (UonReader r = getUonReader(pipe, false)) {
				return parseAnything(toType, r, null, true, null);
			}
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Workhorse method.
	 *
	 * @param <T> The class type being parsed, or <jk>null</jk> if unknown.
	 * @param eType The class type being parsed, or <jk>null</jk> if unknown.
	 * @param r The reader being parsed.
	 * @param outer The outer object (for constructing nested inner classes).
	 * @param isUrlParamValue
	 * 	If <jk>true</jk>, then we're parsing a top-level URL-encoded value which is treated a bit different than the
	 * 	default case.
	 * @param pMeta The current bean property being parsed.
	 * @return The parsed object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public <T> T parseAnything(ClassMeta<?> eType, UonReader r, Object outer, boolean isUrlParamValue, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		if (eType == null)
			eType = object();
		ObjectSwap<T,Object> swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)optional(parseAnything(eType.getElementType(), r, outer, isUrlParamValue, pMeta));

		setCurrentClass(sType);

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
		} else if (sType.isVoid()) {
			String s = parseString(r, isUrlParamValue);
			if (s != null)
				throw new ParseException(this, "Expected ''null'' for void value, but was ''{0}''.", s);
		} else if (sType.isObject()) {
			if (c == '(') {
				JsonMap m = new JsonMap(this);
				parseIntoMap(r, m, string(), object(), pMeta);
				o = cast(m, pMeta, eType);
			} else if (c == '@') {
				Collection l = new JsonList(this);
				o = parseIntoCollection(r, l, sType, isUrlParamValue, pMeta);
			} else {
				String s = parseString(r, isUrlParamValue);
				if (c != '\'') {
					if ("true".equals(s) || "false".equals(s))
						o = Boolean.valueOf(s);
					else if (! "null".equals(s)) {
						if (isNumeric(s))
							o = StringUtils.parseNumber(s, Number.class);
						else
							o = s;
					}
				} else {
					o = s;
				}
			}
		} else if (sType.isBoolean()) {
			o = parseBoolean(r);
		} else if (sType.isCharSequence()) {
			o = parseString(r, isUrlParamValue);
		} else if (sType.isChar()) {
			o = parseCharacter(parseString(r, isUrlParamValue));
		} else if (sType.isNumber()) {
			o = parseNumber(r, (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
			o = parseIntoMap(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
		} else if (sType.isCollection()) {
			if (c == '(') {
				JsonMap m = new JsonMap(this);
				parseIntoMap(r, m, string(), object(), pMeta);
				// Handle case where it's a collection, but serialized as a map with a _type or _value key.
				if (m.containsKey(getBeanTypePropertyName(sType)))
					o = cast(m, pMeta, eType);
				// Handle case where it's a collection, but only a single value was specified.
				else {
					Collection l = (
						sType.canCreateNewInstance(outer)
						? (Collection)sType.newInstance(outer)
						: new JsonList(this)
					);
					l.add(m.cast(sType.getElementType()));
					o = l;
				}
			} else {
				Collection l = (
					sType.canCreateNewInstance(outer)
					? (Collection)sType.newInstance(outer)
					: new JsonList(this)
				);
				o = parseIntoCollection(r, l, sType, isUrlParamValue, pMeta);
			}
		} else if (builder != null) {
			BeanMap m = toBeanMap(builder.create(this, eType));
			m = parseIntoBeanMap(r, m);
			o = m == null ? null : builder.build(this, m.getBean(), eType);
		} else if (sType.canCreateNewBean(outer)) {
			BeanMap m = newBeanMap(outer, sType.getInnerClass());
			m = parseIntoBeanMap(r, m);
			o = m == null ? null : m.getBean();
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			String s = parseString(r, isUrlParamValue);
			if (s != null)
				o = sType.newInstanceFromString(outer, s);
		} else if (sType.isArray() || sType.isArgs()) {
			if (c == '(') {
				JsonMap m = new JsonMap(this);
				parseIntoMap(r, m, string(), object(), pMeta);
				// Handle case where it's an array, but serialized as a map with a _type or _value key.
				if (m.containsKey(getBeanTypePropertyName(sType)))
					o = cast(m, pMeta, eType);
				// Handle case where it's an array, but only a single value was specified.
				else {
					ArrayList l = list(1);
					l.add(m.cast(sType.getElementType()));
					o = toArray(sType, l);
				}
			} else {
				ArrayList l = (ArrayList)parseIntoCollection(r, list(), sType, isUrlParamValue, pMeta);
				o = toArray(sType, l);
			}
		} else if (c == '(') {
			// It could be a non-bean with _type attribute.
			JsonMap m = new JsonMap(this);
			parseIntoMap(r, m, string(), object(), pMeta);
			if (m.containsKey(getBeanTypePropertyName(sType)))
				o = cast(m, pMeta, eType);
			else if (sType.getProxyInvocationHandler() != null)
				o = newBeanMap(outer, sType.getInnerClass()).load(m).getBean();
			else
				throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''",
					sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else if (c == 'n') {
			r.read();
			parseNull(r);
		} else {
			throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''",
				sType.getInnerClass().getName(), sType.getNotABeanReason());
		}

		if (o == null && sType.isPrimitive())
			o = sType.getPrimitiveDefault();
		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(UonReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType,
			BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int c = r.read();
		if (c == -1 || c == AMP)
			return null;
		if (c == 'n')
			return (Map<K,V>)parseNull(r);
		if (c != '(')
			throw new ParseException(this, "Expected '(' at beginning of object.");

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
						Object attr = parseAttr(r, decoding);
						currAttr = attr == null ? null : convertAttrToType(m, trim(attr.toString()), keyType);
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
						V value = parseAnything(valueType, r.unread(), m, false, pMeta);
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
			throw new ParseException(this, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(this, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(this, "Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException(this, "Could not find ')' marking end of object.");

		return null; // Unreachable.
	}

	private <E> Collection<E> parseIntoCollection(UonReader r, Collection<E> l, ClassMeta<E> type, boolean isUrlParamValue, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		int c = r.readSkipWs();
		if (c == -1 || c == AMP)
			return null;
		if (c == 'n')
			return (Collection<E>)parseNull(r);

		int argIndex = 0;

		// If we're parsing a top-level parameter, we're allowed to have comma-delimited lists outside parenthesis (e.g. "&foo=1,2,3&bar=a,b,c")
		// This is not allowed at lower levels since we use comma's as end delimiters.
		boolean isInParens = (c == '@');
		if (! isInParens) {
			if (isUrlParamValue)
				r.unread();
			else
				throw new ParseException(this, "Could not find '(' marking beginning of collection.");
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
							l.add((E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(),
									r.unread(), l, false, pMeta));
							r.read();
						}
						return l;
					} else if (Character.isWhitespace(c)) {
						skipSpace(r);
					} else {
						l.add((E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(),
								r.unread(), l, false, pMeta));
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
				throw new ParseException(this, "Could not find start of entry in array.");
			if (state == S3)
				throw new ParseException(this, "Could not find end of entry in array.");

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
						l.add((E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(),
								r.unread(), l, false, pMeta));
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

	private <T> BeanMap<T> parseIntoBeanMap(UonReader r, BeanMap<T> m) throws IOException, ParseException, ExecutableException {

		int c = r.readSkipWs();
		if (c == -1 || c == AMP)
			return null;
		if (c == 'n')
			return (BeanMap<T>)parseNull(r);
		if (c != '(')
			throw new ParseException(this, "Expected '(' at beginning of object.");

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for , or }
		boolean isInEscape = false;

		int state = S1;
		String currAttr = "";
		mark();
		try {
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
							mark();
							currAttr = parseAttrName(r, decoding);
							if (currAttr == null) { // Value was '%00'
								return null;
							}
							state = S2;
						}
					} else if (state == S2) {
						if (c == EQ || c == '=')
							state = S3;
						else if (c == -1 || c == ',' || c == ')' || c == AMP) {
							m.put(currAttr, null);
							if (c == ')' || c == -1 || c == AMP) {
								return m;
							}
							state = S1;
						}
					} else if (state == S3) {
						if (c == -1 || c == ',' || c == ')' || c == AMP) {
							if (! currAttr.equals(getBeanTypePropertyName(m.getClassMeta()))) {
								BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
								if (pMeta == null) {
									onUnknownProperty(currAttr, m, null);
									unmark();
								} else {
									unmark();
									Object value = convertToType("", pMeta.getClassMeta());
									try {
										pMeta.set(m, currAttr, value);
									} catch (BeanRuntimeException e) {
										onBeanSetterException(pMeta, e);
										throw e;
									}
								}
							}
							if (c == -1 || c == ')' || c == AMP)
								return m;
							state = S1;
						} else {
							if (! currAttr.equals(getBeanTypePropertyName(m.getClassMeta()))) {
								BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
								if (pMeta == null) {
									onUnknownProperty(currAttr, m, parseAnything(object(), r.unread(), m.getBean(false), false, null));
									unmark();
								} else {
									unmark();
									setCurrentProperty(pMeta);
									ClassMeta<?> cm = pMeta.getClassMeta();
									Object value = parseAnything(cm, r.unread(), m.getBean(false), false, pMeta);
									setName(cm, value, currAttr);
									try {
										pMeta.set(m, currAttr, value);
									} catch (BeanRuntimeException e) {
										onBeanSetterException(pMeta, e);
										throw e;
									}
									setCurrentProperty(null);
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
				throw new ParseException(this, "Could not find attribute name on object.");
			if (state == S2)
				throw new ParseException(this, "Could not find '=' following attribute name on object.");
			if (state == S3)
				throw new ParseException(this, "Could not find value following '=' on object.");
			if (state == S4)
				throw new ParseException(this, "Could not find ')' marking end of object.");
		} finally {
			unmark();
		}

		return null; // Unreachable.
	}

	private Object parseNull(UonReader r) throws IOException, ParseException {
		String s = parseString(r, false);
		if ("ull".equals(s))
			return null;
		throw new ParseException(this, "Unexpected character sequence: ''{0}''", s);
	}

	/**
	 * Convenience method for parsing an attribute from the specified parser.
	 *
	 * @param r The reader.
	 * @param encoded Whether the attribute is encoded.
	 * @return The parsed object
	 * @throws IOException Exception thrown by underlying stream.
	 * @throws ParseException Attribute was malformed.
	 */
	protected final Object parseAttr(UonReader r, boolean encoded) throws IOException, ParseException {
		Object attr;
		attr = parseAttrName(r, encoded);
		return attr;
	}

	/**
	 * Parses an attribute name from the specified reader.
	 *
	 * @param r The reader.
	 * @param encoded Whether the attribute is encoded.
	 * @return The parsed attribute name.
	 * @throws IOException Exception thrown by underlying stream.
	 * @throws ParseException Attribute name was malformed.
	 */
	protected final String parseAttrName(UonReader r, boolean encoded) throws IOException, ParseException {

		// If string is of form 'xxx', we're looking for ' at the end.
		// Otherwise, we're looking for '&' or '=' or WS or -1 denoting the end of this string.

		int c = r.peekSkipWs();
		if (c == '\'')
			return parsePString(r);

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
						return ("null".equals(s) ? null : trim(s));
					}
				}
				isInEscape = isInEscape(c, r, isInEscape);
			}
		}

		// We should never get here.
		throw new ParseException(this, "Unexpected condition.");
	}


	/*
	 * Returns true if the next character in the stream is preceded by an escape '~' character.
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

	/**
	 * Parses a string value from the specified reader.
	 *
	 * @param r The input reader.
	 * @param isUrlParamValue Whether this is a URL parameter.
	 * @return The parsed string.
	 * @throws IOException Exception thrown by underlying stream.
	 * @throws ParseException Malformed input found.
	 */
	protected final String parseString(UonReader r, boolean isUrlParamValue) throws IOException, ParseException {

		// If string is of form 'xxx', we're looking for ' at the end.
		// Otherwise, we're looking for ',' or ')' or -1 denoting the end of this string.

		int c = r.peekSkipWs();
		if (c == '\'')
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
			else if (Character.isWhitespace(c) && ! isUrlParamValue) {
				s = r.getMarked(0, -1);
				skipSpace(r);
				c = -1;
			}
			isInEscape = isInEscape(c, r, isInEscape);
		}

		if (isUrlParamValue)
			s = StringUtils.trim(s);

		return ("null".equals(s) ? null : trim(s));
	}

	private static final AsciiSet endCharsParam = AsciiSet.create(""+AMP), endCharsNormal = AsciiSet.create(",)"+AMP);


	/*
	 * Parses a string of the form "'foo'"
	 * All whitespace within parenthesis are preserved.
	 */
	private String parsePString(UonReader r) throws IOException, ParseException {

		r.read(); // Skip first quote.
		r.mark();
		int c = 0;

		boolean isInEscape = false;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (c == '\'')
					return trim(r.getMarked(0, -1));
			}
			if (c == EQ)
				r.replace('=');
			isInEscape = isInEscape(c, r, isInEscape);
		}
		throw new ParseException(this, "Unmatched parenthesis");
	}

	private Boolean parseBoolean(UonReader r) throws IOException, ParseException {
		String s = parseString(r, false);
		if (s == null || s.equals("null"))
			return null;
		if (s.equalsIgnoreCase("true"))
			return true;
		if (s.equalsIgnoreCase("false"))
			return false;
		throw new ParseException(this, "Unrecognized syntax for boolean.  ''{0}''.", s);
	}

	private Number parseNumber(UonReader r, Class<? extends Number> c) throws IOException, ParseException {
		String s = parseString(r, false);
		if (s == null)
			return null;
		return StringUtils.parseNumber(s, c);
	}

	/*
	 * Call this method after you've finished a parsing a string to make sure that if there's any
	 * remainder in the input, that it consists only of whitespace and comments.
	 */
	private void validateEnd(UonReader r) throws IOException, ParseException {
		if (! isValidateEnd())
			return;
		while (true) {
			int c = r.read();
			if (c == -1)
				return;
			if (! Character.isWhitespace(c))
				throw new ParseException(this, "Remainder after parse: ''{0}''.", (char)c);
		}
	}

	private static void skipSpace(ParserReader r) throws IOException {
		int c = 0;
		while ((c = r.read()) != -1) {
			if (c <= 2 || ! Character.isWhitespace(c)) {
				r.unread();
				return;
			}
		}
	}

	/**
	 * Creates a {@link UonReader} from the specified parser pipe.
	 *
	 * @param pipe The parser input.
	 * @param decodeChars Whether the reader should automatically decode URL-encoded characters.
	 * @return A new {@link UonReader} object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public final UonReader getUonReader(ParserPipe pipe, boolean decodeChars) throws IOException {
		Reader r = pipe.getReader();
		if (r instanceof UonReader)
			return (UonReader)r;
		return new UonReader(pipe, decodeChars);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Decode <js>"%xx"</js> sequences.
	 *
	 * @see UonParser.Builder#decoding()
	 * @return
	 * 	<jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk> if they've already been decoded
	 * 	before being passed to this parser.
	 */
	protected final boolean isDecoding() {
		return decoding;
	}

	/**
	 * Validate end.
	 *
	 * @see UonParser.Builder#validateEnd()
	 * @return
	 * 	<jk>true</jk> if after parsing a POJO from the input, verifies that the remaining input in
	 * 	the stream consists of only comments or whitespace.
	 */
	protected final boolean isValidateEnd() {
		return ctx.isValidateEnd();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap("decoding", decoding);
	}
}
