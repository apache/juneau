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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.common.utils.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UrlEncodingSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UrlEncodingBasics">URL-Encoding Basics</a>
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "resource" })
public class UrlEncodingSerializerSession extends UonSerializerSession {
	/**
	 * Builder class.
	 */
	public static class Builder extends UonSerializerSession.Builder {

		UrlEncodingSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(UrlEncodingSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public UrlEncodingSerializerSession build() {
			return new UrlEncodingSerializerSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder encoding(boolean value) {
			super.encoding(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(UrlEncodingSerializer ctx) {
		return new Builder(ctx);
	}

	/*
	 * Converts a Collection into an integer-indexed map.
	 */
	private static Map<Integer,Object> getCollectionMap(Collection<?> c) {
		Map<Integer,Object> m = new TreeMap<>();
		IntValue i = IntValue.create();
		c.forEach(o -> m.put(i.getAndIncrement(), o));
		return m;
	}

	/*
	 * Converts an array into an integer-indexed map.
	 */
	private static Map<Integer,Object> getCollectionMap(Object array) {
		Map<Integer,Object> m = new TreeMap<>();
		for (int i = 0; i < Array.getLength(array); i++)
			m.put(i, Array.get(array, i));
		return m;
	}

	private final UrlEncodingSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected UrlEncodingSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	/*
	 * Workhorse method. Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	private SerializerWriter serializeAnything(UonWriter out, Object o) throws IOException, SerializeException {

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		ClassMeta<?> eType = getExpectedRootType(o);
		aType = push2("root", o, eType);
		indent--;
		if (aType == null)
			aType = object();

		sType = aType;
		String typeName = getBeanTypeName(this, eType, aType, null);

		// Swap if necessary
		ObjectSwap swap = aType.getSwap(this);
		if (swap != null) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, sType);
		} else if (sType.isBean()) {
			serializeBeanMap(out, toBeanMap(o), typeName);
		} else if (sType.isCollection() || sType.isArray()) {
			Map m = sType.isCollection() ? getCollectionMap((Collection)o) : getCollectionMap(o);
			serializeCollectionMap(out, m, getClassMeta(Map.class, Integer.class, Object.class));
		} else if (sType.isReader()) {
			pipe((Reader)o, out);
		} else if (sType.isInputStream()) {
			pipe((InputStream)o, out);
		} else {
			// All other types can't be serialized as key/value pairs, so we create a
			// mock key/value pair with a "_value" key.
			out.append("_value=");
			pop();
			super.serializeAnything(out, o, null, null, null);
			return out;
		}

		pop();
		return out;
	}

	private SerializerWriter serializeBeanMap(UonWriter out, BeanMap<?> m, String typeName) throws SerializeException {
		Flag addAmp = Flag.create();

		if (typeName != null) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			out.appendObject(pm.getName(), true).append('=').appendObject(typeName, false);
			addAmp.set();
		}

		Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;
		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			ClassMeta<?> cMeta = pMeta.getClassMeta();
			ClassMeta<?> sMeta = cMeta.getSerializedClassMeta(this);

			if (thrown != null)
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(sMeta, key, value))
				return;

			if (value != null && shouldUseExpandedParams(pMeta)) {
				// Transformed object array bean properties may be transformed resulting in ArrayLists,
				// so we need to check type if we think it's an array.
				if (sMeta.isCollection() || value instanceof Collection) {
					((Collection<?>)value).forEach(x -> {
						addAmp.ifSet(() -> out.cr(indent).append('&')).set();
						out.appendObject(key, true).append('=');
						super.serializeAnything(out, x, cMeta.getElementType(), key, pMeta);
					});
				} else /* array */ {
					for (int i = 0; i < Array.getLength(value); i++) {
						addAmp.ifSet(() -> out.cr(indent).append('&')).set();
						out.appendObject(key, true).append('=');
						super.serializeAnything(out, Array.get(value, i), cMeta.getElementType(), key, pMeta);
					}
				}
			} else {
				addAmp.ifSet(() -> out.cr(indent).append('&')).set();
				out.appendObject(key, true).append('=');
				super.serializeAnything(out, value, cMeta, key, pMeta);
			}
		});

		return out;
	}

	private SerializerWriter serializeCollectionMap(UonWriter out, Map<?,?> m, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> valueType = type.getValueType();

		Flag addAmp = Flag.create();

		m.forEach((k, v) -> {
			addAmp.ifSet(() -> out.cr(indent).append('&')).set();
			out.append(k).append('=');
			super.serializeAnything(out, v, valueType, null, null);
		});

		return out;
	}

	private SerializerWriter serializeMap(UonWriter out, Map m, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		Flag addAmp = Flag.create();

		forEachEntry(m, e -> {
			Object key = generalize(e.getKey(), keyType);
			Object value = e.getValue();

			if (shouldUseExpandedParams(value)) {
				if (value instanceof Collection) {
					((Collection<?>)value).forEach(x -> {
						addAmp.ifSet(() -> out.cr(indent).append('&')).set();
						out.appendObject(key, true).append('=');
						super.serializeAnything(out, x, null, Utils.s(key), null);
					});
				} else /* array */ {
					for (int i = 0; i < Array.getLength(value); i++) {
						addAmp.ifSet(() -> out.cr(indent).append('&')).set();
						out.appendObject(key, true).append('=');
						super.serializeAnything(out, Array.get(value, i), null, Utils.s(key), null);
					}
				}
			} else {
				addAmp.ifSet(() -> out.cr(indent).append('&')).set();
				out.appendObject(key, true).append('=');
				super.serializeAnything(out, value, valueType, (key == null ? null : key.toString()), null);
			}
		});

		return out;
	}

	/*
	 * Returns <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 */
	private boolean shouldUseExpandedParams(BeanPropertyMeta pMeta) {
		ClassMeta<?> cm = pMeta.getClassMeta().getSerializedClassMeta(this);
		if (cm.isCollectionOrArray()) {
			if (isExpandedParams() || getUrlEncodingClassMeta(pMeta.getBeanMeta().getClassMeta()).isExpandedParams())
				return true;
		}
		return false;
	}

	/*
	 * Returns <jk>true</jk> if the specified value should be represented as an expanded parameter list.
	 */
	private boolean shouldUseExpandedParams(Object value) {
		if (value == null || ! isExpandedParams())
			return false;
		ClassMeta<?> cm = getClassMetaForObject(value).getSerializedClassMeta(this);
		if (cm.isCollectionOrArray()) {
			if (isExpandedParams())
				return true;
		}
		return false;
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getUonWriter(out).i(getInitialDepth()), o);
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

	/**
	 * Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * @see UrlEncodingSerializer.Builder#expandedParams()
	 * @return
	 * 	<jk>false</jk> if serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * 	<br><jk>true</jk> if serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 */
	protected final boolean isExpandedParams() { return ctx.isExpandedParams(); }
}