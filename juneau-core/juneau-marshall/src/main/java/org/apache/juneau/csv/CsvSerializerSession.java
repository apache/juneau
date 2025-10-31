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
package org.apache.juneau.csv;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Session object that lives for the duration of a single use of {@link CsvSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>

 * </ul>
 */
@SuppressWarnings("resource")
public class CsvSerializerSession extends WriterSerializerSession {
	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializerSession.Builder {

		CsvSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(CsvSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public CsvSerializerSession build() {
			return new CsvSerializerSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
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
	public static Builder create(CsvSerializer ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CsvSerializerSession(Builder builder) {
		super(builder);
	}

	/**
	 * Applies any registered object swap to the specified value.
	 *
	 * <p>
	 * If a swap is registered for the value's type, the value is transformed using the swap's
	 * {@code swap()} method before being serialized.
	 *
	 * @param value The value to potentially swap.
	 * @param type The class metadata of the value's type.
	 * @return The swapped value, or the original value if no swap is registered.
	 */
	@SuppressWarnings({ "rawtypes" })
	private Object applySwap(Object value, ClassMeta<?> type) {
		try {
			if (value == null || type == null)
				return value;

			org.apache.juneau.swap.ObjectSwap swap = type.getSwap(this);
			if (nn(swap)) {
				return swap(swap, value);
			}
			return value;
		} catch (SerializeException e) {
			throw runtimeException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {

		try (CsvWriter w = getCsvWriter(pipe)) {
			ClassMeta<?> cm = getClassMetaForObject(o);
			Collection<?> l = null;
			if (cm.isArray()) {
				l = l((Object[])o);
			} else if (cm.isCollection()) {
				l = (Collection<?>)o;
			} else {
				l = Collections.singleton(o);
			}

			// TODO - Doesn't support DynaBeans.
			if (isNotEmpty(l)) {
				ClassMeta<?> entryType = getClassMetaForObject(l.iterator().next());
				if (entryType.isBean()) {
					BeanMeta<?> bm = entryType.getBeanMeta();
					var addComma = Flag.create();
					bm.forEachProperty(BeanPropertyMeta::canRead, x -> {
						addComma.ifSet(() -> w.w(',')).set();
						w.writeEntry(x.getName());
					});
					w.append('\n');
					l.forEach(x -> {
						var addComma2 = Flag.create();
						BeanMap<?> bean = toBeanMap(x);
						bm.forEachProperty(BeanPropertyMeta::canRead, y -> {
							addComma2.ifSet(() -> w.w(',')).set();
							// Bean property values are already swapped by BeanPropertyMeta.get() via toSerializedForm()
							Object value = y.get(bean, y.getName());
							w.writeEntry(value);
						});
						w.w('\n');
					});
				} else if (entryType.isMap()) {
					var addComma = Flag.create();
					var first = (Map)l.iterator().next();
					first.keySet().forEach(x -> {
						addComma.ifSet(() -> w.w(',')).set();
						w.writeEntry(x);
					});
					w.append('\n');
					l.stream().forEach(x -> {
						var addComma2 = Flag.create();
						var map = (Map)x;
						map.values().forEach(y -> {
							addComma2.ifSet(() -> w.w(',')).set();
							Object value = applySwap(y, getClassMetaForObject(y));
							w.writeEntry(value);
						});
						w.w('\n');
					});
				} else {
					w.writeEntry("value");
					w.append('\n');
					l.stream().forEach(x -> {
						Object value = applySwap(x, getClassMetaForObject(x));
						w.writeEntry(value);
						w.w('\n');
					});
				}
			}
		}
	}

	CsvWriter getCsvWriter(SerializerPipe out) {
		Object output = out.getRawOutput();
		if (output instanceof CsvWriter)
			return (CsvWriter)output;
		var w = new CsvWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), getQuoteChar(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}
}