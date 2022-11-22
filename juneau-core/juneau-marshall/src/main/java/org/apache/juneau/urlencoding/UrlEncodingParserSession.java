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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UrlEncodingParser}.
 *
 * <ul class='notes'>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.UrlEncodingDetails">URL-Encoding Details</a>
 * </ul>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UrlEncodingParserSession extends UonParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(UrlEncodingParser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends UonParserSession.Builder {

		UrlEncodingParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(UrlEncodingParser ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public UrlEncodingParserSession build() {
			return new UrlEncodingParserSession(this);
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

		@Override /* GENERATED - org.apache.juneau.uon.UonParserSession.Builder */
		public Builder decoding(boolean value) {
			super.decoding(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final UrlEncodingParser ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UrlEncodingParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	/**
	 * Returns <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 *
	 * @param pMeta The metadata on the bean property.
	 * @return <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 */
	public final boolean shouldUseExpandedParams(BeanPropertyMeta pMeta) {
		ClassMeta<?> cm = pMeta.getClassMeta().getSerializedClassMeta(this);
		if (cm.isCollectionOrArray()) {
			if (isExpandedParams() || getUrlEncodingClassMeta(pMeta.getBeanMeta().getClassMeta()).isExpandedParams())
				return true;
		}
		return false;
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (UonReader r = getUonReader(pipe, true)) {
			return parseAnything(type, r, getOuter());
		}
	}

	@Override /* ReaderParserSession */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		try (UonReader r = getUonReader(pipe, true)) {
			if (r.peekSkipWs() == '?')
				r.read();
			m = parseIntoMap2(r, m, getClassMeta(Map.class, keyType, valueType), null);
			return m;
		}
	}

	private <T> T parseAnything(ClassMeta<T> eType, UonReader r, Object outer) throws IOException, ParseException, ExecutableException {

		if (eType == null)
			eType = (ClassMeta<T>)object();
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
			return (T)optional(parseAnything(eType.getElementType(), r, outer));

		int c = r.peekSkipWs();
		if (c == '?')
			r.read();

		Object o;

		if (sType.isObject()) {
			JsonMap m = new JsonMap(this);
			parseIntoMap2(r, m, getClassMeta(Map.class, String.class, Object.class), outer);
			if (m.containsKey("_value"))
				o = m.get("_value");
			else
				o = cast(m, null, eType);
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance() ? (Map)sType.newInstance() : newGenericMap(sType));
			o = parseIntoMap2(r, m, sType, m);
		} else if (builder != null) {
			BeanMap m = toBeanMap(builder.create(this, eType));
			m = parseIntoBeanMap(r, m);
			o = m == null ? null : builder.build(this, m.getBean(), eType);
		} else if (sType.canCreateNewBean(outer)) {
			BeanMap m = newBeanMap(outer, sType.getInnerClass());
			m = parseIntoBeanMap(r, m);
			o = m == null ? null : m.getBean();
		} else if (sType.isCollection() || sType.isArray() || sType.isArgs()) {
			// ?1=foo&2=bar...
			Collection c2 = ((sType.isArray() || sType.isArgs()) || ! sType.canCreateNewInstance(outer)) ? new JsonList(this) : (Collection)sType.newInstance();
			Map<Integer,Object> m = new TreeMap<>();
			parseIntoMap2(r, m, sType, c2);
			c2.addAll(m.values());
			if (sType.isArray())
				o = ArrayUtils.toArray(c2, sType.getElementType().getInnerClass());
			else if (sType.isArgs())
				o = c2.toArray(new Object[c2.size()]);
			else
				o = c2;
		} else {
			// It could be a non-bean with _type attribute.
			JsonMap m = new JsonMap(this);
			parseIntoMap2(r, m, getClassMeta(Map.class, String.class, Object.class), outer);
			if (m.containsKey(getBeanTypePropertyName(eType)))
				o = cast(m, null, eType);
			else if (m.containsKey("_value"))
				o = convertToType(m.get("_value"), sType);
			else if (sType.getProxyInvocationHandler() != null) {
				o = newBeanMap(outer, sType.getInnerClass()).load(m).getBean();
			} else {
				if (sType.getNotABeanReason() != null)
					throw new ParseException(this, "Class ''{0}'' could not be instantiated as application/x-www-form-urlencoded.  Reason: ''{1}''", sType, sType.getNotABeanReason());
				throw new ParseException(this, "Malformed application/x-www-form-urlencoded input for class ''{0}''.", sType);
			}
		}

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap2(UonReader r, Map<K,V> m, ClassMeta<?> type, Object outer) throws IOException, ParseException, ExecutableException {

		ClassMeta<K> keyType = (ClassMeta<K>)(type.isArgs() || type.isCollectionOrArray() ? getClassMeta(Integer.class) : type.getKeyType());

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
					Object attr = parseAttr(r, true);
					currAttr = attr == null ? null : convertAttrToType(m, trim(attr.toString()), keyType);
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
						V value = convertAttrToType(m, "", valueType);
						m.put(currAttr, value);
						if (c == -1)
							return m;
						state = S1;
					} else  {
						// For performance, we bypass parseAnything for string values.
						ClassMeta<V> valueType = (ClassMeta<V>)(type.isArgs() ? type.getArg(argIndex++) : type.isCollectionOrArray() ? type.getElementType() : type.getValueType());
						V value = (V)(valueType.isString() ? super.parseString(r.unread(), true) : super.parseAnything(valueType, r.unread(), outer, true, null));

						// If we already encountered this parameter, turn it into a list.
						if (m.containsKey(currAttr) && valueType.isObject()) {
							Object v2 = m.get(currAttr);
							if (! (v2 instanceof JsonList)) {
								v2 = new JsonList(v2).setBeanSession(this);
								m.put(currAttr, (V)v2);
							}
							((JsonList)v2).add(value);
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
			throw new ParseException(this, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(this, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(this, "Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException(this, "Could not find end of object.");

		return null; // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap(UonReader r, BeanMap<T> m) throws IOException, ParseException, ExecutableException {

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
		mark();
		try {
			while (c != -1) {
				c = r.read();
				if (! isInEscape) {
					if (state == S1) {
						if (c == -1) {
							return m;
						}
						r.unread();
						mark();
						currAttr = parseAttrName(r, true);
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
							if (! currAttr.equals(getBeanTypePropertyName(m.getClassMeta()))) {
								BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
								if (pMeta == null) {
									onUnknownProperty(currAttr, m, null);
									unmark();
								} else {
									unmark();
									setCurrentProperty(pMeta);
									// In cases of "&foo=", create an empty instance of the value if createable.
									// Otherwise, leave it null.
									ClassMeta<?> cm = pMeta.getClassMeta();
									if (cm.canCreateNewInstance()) {
										try {
											pMeta.set(m, currAttr, cm.newInstance());
										} catch (BeanRuntimeException e) {
											onBeanSetterException(pMeta, e);
											throw e;
										}
									}
									setCurrentProperty(null);
								}
							}
							if (c == -1)
								return m;
							state = S1;
						} else {
							if (! currAttr.equals(getBeanTypePropertyName(m.getClassMeta()))) {
								BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
								if (pMeta == null) {
									onUnknownProperty(currAttr, m, parseAnything(object(), r.unread(), m.getBean(false), true, null));
									unmark();
								} else {
									unmark();
									setCurrentProperty(pMeta);
									if (shouldUseExpandedParams(pMeta)) {
										ClassMeta et = pMeta.getClassMeta().getElementType();
										Object value = parseAnything(et, r.unread(), m.getBean(false), true, pMeta);
										setName(et, value, currAttr);
										try {
											pMeta.add(m, currAttr, value);
										} catch (BeanRuntimeException e) {
											onBeanSetterException(pMeta, e);
											throw e;
										}
									} else {
										ClassMeta<?> cm = pMeta.getClassMeta();
										Object value = parseAnything(cm, r.unread(), m.getBean(false), true, pMeta);
										setName(cm, value, currAttr);
										try {
											pMeta.set(m, currAttr, value);
										} catch (BeanRuntimeException e) {
											onBeanSetterException(pMeta, e);
											throw e;
										}
									}
									setCurrentProperty(null);
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
				throw new ParseException(this, "Could not find attribute name on object.");
			if (state == S2)
				throw new ParseException(this, "Could not find '=' following attribute name on object.");
			if (state == S3)
				throw new ParseException(this, "Could not find value following '=' on object.");
			if (state == S4)
				throw new ParseException(this, "Could not find end of object.");
		} finally {
			unmark();
		}

		return null; // Unreachable.
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parser bean property collections/arrays as separate key/value pairs.
	 *
	 * @see UrlEncodingParser.Builder#expandedParams()
	 * @return
	 * <jk>false</jk> if serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br><jk>true</jk> if serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 */
	protected final boolean isExpandedParams() {
		return ctx.isExpandedParams();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected UrlEncodingClassMeta getUrlEncodingClassMeta(ClassMeta<?> cm) {
		return ctx.getUrlEncodingClassMeta(cm);
	}
}
