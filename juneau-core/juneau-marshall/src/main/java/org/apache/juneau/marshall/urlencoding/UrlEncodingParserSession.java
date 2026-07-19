/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.urlencoding;

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.swap.*;
import org.apache.juneau.marshall.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UrlEncodingParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UrlEncodingSupport">URL-Encoding Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"unchecked",   // Type erasure requires unchecked casts
	"rawtypes",    // Raw types necessary for generic type handling
	"resource",    // UonReader is managed by caller
	"java:S110",   // Inheritance depth acceptable for session hierarchy
	"java:S115"    // Constants use UPPER_snakeCase convention (e.g., CONST_value)
})
public class UrlEncodingParserSession extends UonParserSession {

	private static final String CONST_value = "_value";

	// Property name constants
	private static final String PROP_expandedParams = "expandedParams";
	private static final String PROP_UrlEncodingParserSession_expandedParams = "UrlEncodingParserSession.expandedParams";

	/**
	 * Builder class.
	 */
	public static class Builder extends UonParserSession.Builder<Builder> {

		private boolean expandedParams;
		private UrlEncodingParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(UrlEncodingParser ctx) {
			super(ctx);
			this.ctx = ctx;
			expandedParams = ctx.isExpandedParams();
		}

		@Override
		public UrlEncodingParserSession build() {
			return new UrlEncodingParserSession(this);
		}

		/**
		 * Serialize bean property collections/arrays as separate key/value pairs.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder expandedParams(boolean value) {
			expandedParams = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			if (key == null) { super.property(key, value); return this; }
			switch (key) {
				case PROP_expandedParams, PROP_UrlEncodingParserSession_expandedParams:
					return expandedParams(cvt(value, Boolean.class));
				default:
					super.property(key, value);
					return this;
			}
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(UrlEncodingParser ctx) {
		return new Builder(ctx);
	}

	private final UrlEncodingParser ctx;
	private final boolean expandedParams;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UrlEncodingParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		expandedParams = builder.expandedParams;
	}

	/**
	 * Returns <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 *
	 * @param pMeta The metadata on the bean property.
	 * @return <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 */
	public final boolean shouldUseExpandedParams(BeanPropertyMeta pMeta) {
		var cm = ((ClassMeta<?>) pMeta.getBeanInfo()).getSerializedClassMeta(this);
		return cm.isCollectionOrArray() && (isExpandedParams() || getUrlEncodingClassMeta((ClassMeta<?>) pMeta.getBeanMeta().getBeanInfo()).isExpandedParams());
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for parser state machine
	})
	private <T> T readAnything(ClassMeta<T> eType, UonReader r, Object outer) throws IOException, ParseException, ExecutableException {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		var swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (nn(builder))
			sType = builder.getBuilderClassMeta(this);
		else if (nn(swap))
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)o(readAnything(eType.getElementType(), r, outer));

		int c = r.peekSkipWs();
		if (c == '?') {
			@SuppressWarnings({
				"unused" // Intentionally unused; variable/parameter is required by the interface contract
			})
			int ignored = r.read();
		}

		Object o;

		if (sType.isObject()) {
			var m = newGenericMap();
			readIntoMap2(r, m, getClassMeta(Map.class, String.class, Object.class), outer);
			if (m.containsKey(CONST_value))
				o = m.get(CONST_value);
			else
				o = cast(m, null, eType);
		} else if (nn(builder)) {
			var m = toBeanMap(builder.create(this, eType));
			m = readIntoBeanMap(r, m);
			o = m == null ? null : builder.build(this, m.getBean(), eType);
		} else if (sType.canCreateNewBean(outer)) {
			var m = newBeanMap(outer, sType.inner());
			m = readIntoBeanMap(r, m);
			o = m == null ? null : m.getBean();
		} else if (sType.isMap()) {
			var m = (sType.canCreateNewInstance() ? (Map)sType.newInstance() : newGenericMap(sType));
			o = readIntoMap2(r, m, sType, m);
		} else if (sType.isCollection() || sType.isArray() || sType.isArgs()) {
			// ?1=foo&2=bar...
			var c2 = ((sType.isArray() || sType.isArgs()) || ! sType.canCreateNewInstance(outer)) ? newGenericList() : (Collection)sType.newInstance();
			var m = new TreeMap<Integer,Object>();
			readIntoMap2(r, m, sType, c2);
			c2.addAll(m.values());
			if (sType.isArgs())
				o = c2.toArray(new Object[c2.size()]);
			else if (sType.isArray())
				o = CollectionUtils.toArray(c2, sType.getElementType().inner());
			else
				o = c2;
		} else {
			// It could be a non-bean with _type attribute.
			var m = newGenericMap();
			readIntoMap2(r, m, getClassMeta(Map.class, String.class, Object.class), outer);
			if (m.containsKey(getBeanTypePropertyName(eType)))
				o = cast(m, null, eType);
			else if (m.containsKey(CONST_value))
				o = unwrapValueAs(m.get(CONST_value), sType);
			else if (nn(sType.getProxyInvocationHandler())) {
				o = newBeanMap(outer, sType.inner()).load(m).getBean();
			} else {
				if (nn(sType.getNotABeanReason()))
					throw new ParseException(this, "Class '%s' could not be instantiated as application/x-www-form-urlencoded.  Reason: '%s'", sType, sType.getNotABeanReason());
				throw new ParseException(this, "Malformed application/x-www-form-urlencoded input for class '%s'.", sType);
			}
		}

		if (nn(swap) && nn(o))
			o = unswap(swap, o, eType);

		if (nn(outer))
			setParent(eType, o, outer);

		return (T)o;
	}

	@SuppressWarnings({
		"java:S1168",    // Null when currAttr is '%00'. Parser state machine.
		"java:S2177",    // Intentional: UrlEncodingParserSession provides its own private readIntoBeanMap() with expanded-params logic
		"java:S125",     // State-machine comments (S1: ..., S2: ...)
		"java:S2583",    // State variables persist across loop iterations
		"java:S2589",    // Final if (state==S4) is always true given prior checks; exhaustive state error-reporting pattern
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541", // Single-threaded session contexts do not require synchronization
	})
	private <T> BeanMap<T> readIntoBeanMap(UonReader r, BeanMap<T> m) throws IOException, ParseException, ExecutableException {

		int c = r.peekSkipWs();
		if (c == -1)
			return m;

		// S1: Looking for attrName start.
		// S2: Found attrName end, looking for =.
		// S3: Found =, looking for valStart.
		// S4: Looking for , or }

		boolean isInEscape = false;

		var state = S1;
		var currAttr = "";
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
						currAttr = readAttrName(r, true);
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
					} else if (state == S3) {  // NOSONAR - State check necessary for state machine
						if (c == -1 || c == '\u0001') {
							if (! currAttr.equals(getBeanTypePropertyName((ClassMeta<?>) m.getBeanInfo()))) {
								var pMeta = m.getPropertyMeta(currAttr);
								if (pMeta == null) {
									onUnknownProperty(currAttr, m, null);
									unmark();
								} else {
									unmark();
									setCurrentProperty(pMeta);
									// In cases of "&foo=", create an empty instance of the value if createable.
									// Otherwise, leave it null.  Arrays (especially primitive arrays
									// such as byte[]) report canCreateNewInstance()=false because their
									// element type has no no-arg constructor, but ClassMeta.newInstance()
									// already handles arrays via Array.newInstance(...,0); fall through
									// to that path so empty expanded-params arrays round-trip from the
									// "key=" sentinel emitted by the serializer.
									var cm = (ClassMeta<?>) pMeta.getBeanInfo();
									if (cm.canCreateNewInstance() || cm.isArray()) {
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
							if (! currAttr.equals(getBeanTypePropertyName((ClassMeta<?>) m.getBeanInfo()))) {
								var pMeta = m.getPropertyMeta(currAttr);
								if (pMeta == null) {
									onUnknownProperty(currAttr, m, readAnything(object(), r.unread(), m.getBean(false), true, null));
									unmark();
								} else {
									unmark();
									setCurrentProperty(pMeta);
									if (shouldUseExpandedParams(pMeta)) {
										var et = ((ClassMeta<?>) pMeta.getBeanInfo()).getElementType();
										var value = readAnything(et, r.unread(), m.getBean(false), true, pMeta);
										setName(et, value, currAttr);
										try {
											pMeta.add(m, currAttr, value);
										} catch (BeanRuntimeException e) {
											onBeanSetterException(pMeta, e);
											throw e;
										}
									} else {
										var cm = (ClassMeta<?>) pMeta.getBeanInfo();
										var value = readAnything(cm, r.unread(), m.getBean(false), true, pMeta);
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

	@SuppressWarnings({
		"java:S1168",    // Compiler-satisfying return: all paths return m or throw. S1168 flags null returns; here null is unreachable.
		"java:S3776"     // Cognitive complexity acceptable for parser state machine
	})
	private <K,V> Map<K,V> readIntoMap2(UonReader r, Map<K,V> m, ClassMeta<?> type, Object outer) throws IOException, ParseException, ExecutableException {

		var keyType = (ClassMeta<K>)(type.isArgs() || type.isCollectionOrArray() ? getClassMeta(Integer.class) : type.getKeyType());

		int c = r.peekSkipWs();
		if (c == -1)
			return m;

		// S1: Looking for attrName start.
		// S2: Found attrName end, looking for =.
		// S3: Found =, looking for valStart.
		// S4: Looking for & or end.

		boolean isInEscape = false;

		var state = S1;
		int argIndex = 0;
		K currAttr = null;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == -1)
						return m;
					r.unread();
					var attr = readAttr(r, true);
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
						ClassMeta<V> valueType;
						if (type.isArgs()) {
							valueType = (ClassMeta<V>)type.getArg(argIndex++);
						} else if (type.isCollectionOrArray()) {
							valueType = (ClassMeta<V>)type.getElementType();
						} else {
							valueType = (ClassMeta<V>)type.getValueType();
						}
						V value = convertAttrToType(m, "", valueType);
						m.put(currAttr, value);
						if (c == -1)
							return m;
						state = S1;
					} else {
						// For performance, we bypass readAnything for string values.
						ClassMeta<V> valueType;
						if (type.isArgs()) {
							valueType = (ClassMeta<V>)type.getArg(argIndex++);
						} else if (type.isCollectionOrArray()) {
							valueType = (ClassMeta<V>)type.getElementType();
						} else {
							valueType = (ClassMeta<V>)type.getValueType();
						}
						V value = (V)(valueType.isString() ? super.readString(r.unread(), true) : super.readAnything(valueType, r.unread(), outer, true, null));

						// If we already encountered this parameter, turn it into a list.
						if (m.containsKey(currAttr) && valueType.isObject()) {
							Object v2 = m.get(currAttr);
							if (! (v2 instanceof MarshalledList)) {
								v2 = new JsonList(v2).setBeanSession(this);
								m.put(currAttr, (V)v2);
							}
							((MarshalledList)v2).add(value);
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

	@Override /* Overridden from ParserSession */
	protected <T> T doRead(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (var r = getUonReader(pipe, true)) {
			return readAnything(type, r, getOuter());
		}
	}

	@Override /* Overridden from ReaderParserSession */
	protected <K,V> Map<K,V> doReadIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		try (var r = getUonReader(pipe, true)) {
			if (r.peekSkipWs() == '?') {
				@SuppressWarnings({
					"unused" // Intentionally unused; variable/parameter is required by the interface contract
				})
				int ignored = r.read();
			}
			m = readIntoMap2(r, m, getClassMeta(Map.class, keyType, valueType), null);
			return m;
		}
	}

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected UrlEncodingClassMeta getUrlEncodingClassMeta(ClassMeta<?> cm) {
		return ctx.getUrlEncodingClassMeta(cm);
	}

	/*
	 * Routes the {@code _value=...} top-level unwrap through the format-aware
	 * Duration/Period/Date/Calendar/Temporal parsers when the target type is one of those
	 * date/time/duration cluster types.  Otherwise falls through to the generic
	 * {@link #convertToType(Object, ClassMeta)} path.
	 *
	 * <p>
	 * Without this routing the inner value (typically a {@link Number} parsed from a bare wire
	 * literal) is converted via the generic {@code Number → T} coercion which does not consult
	 * {@link MarshallingContext#getDurationFormat()} / {@code getTemporalFormat()} etc., so the
	 * configured wire format hint (NANOS, ISO_YEAR, MILLIS, …) is silently dropped.
	 */
	private <T> Object unwrapValueAs(Object rawValue, ClassMeta<T> sType) {
		if (rawValue != null) {
			if (sType.isDuration())
				return readDuration(rawValue.toString());
			if (sType.isPeriod())
				return readPeriod(rawValue.toString());
			if (sType.isDate())
				return readDate(rawValue.toString(), sType);
			if (sType.isCalendar())
				return readCalendar(rawValue.toString(), sType);
			if (sType.isTemporal())
				return readTemporal(rawValue.toString(), sType);
		}
		return convertToType(rawValue, sType);
	}

	/**
	 * Parser bean property collections/arrays as separate key/value pairs.
	 *
	 * @see UrlEncodingParser.Builder#expandedParams()
	 * @return
	 * <jk>false</jk> if serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br><jk>true</jk> if serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 */
	protected final boolean isExpandedParams() { return expandedParams; }
}
