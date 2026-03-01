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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import java.time.*;
import java.time.temporal.*;
import java.util.Calendar;
import java.util.Date;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Session object that lives for the duration of a single use of {@link CsvSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., ARG_ctx)
	"java:S3776", // Cognitive complexity acceptable for doSerialize, formatForCsvCell
	"java:S6541", // Brain method acceptable for doSerialize, formatForCsvCell
})
public class CsvSerializerSession extends WriterSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	private final ByteArrayFormat byteArrayFormat;
	private final boolean allowNestedStructures;
	private final String nullValue;

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for builder hierarchy
	})
	public static class Builder extends WriterSerializerSession.Builder {

		private ByteArrayFormat byteArrayFormat;
		private boolean allowNestedStructures;
		private String nullValue;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(CsvSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			byteArrayFormat = ctx.getByteArrayFormat();
			allowNestedStructures = ctx.isAllowNestedStructures();
			nullValue = ctx.getNullValue();
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
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(CsvSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CsvSerializerSession(Builder builder) {
		super(builder);
		byteArrayFormat = builder.byteArrayFormat;
		allowNestedStructures = builder.allowNestedStructures;
		nullValue = builder.nullValue != null ? builder.nullValue : "<NULL>";
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
			if (type.isDateOrCalendarOrTemporal())
				return Iso8601Utils.format(value, type, getTimeZone());
			if (type.isDuration())
				return value.toString();
			return value;
		} catch (SerializeException e) {
			throw rex(e);
		}
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic type handling
		"unchecked", // Type erasure requires unchecked casts
		"resource", // w is closed by try-with-resources; lambdas capture it
	})
	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		var cm = push2("root", o, getClassMetaForObject(o));
		if (cm == null)
			return;
		try {
			try (var w = getCsvWriter(pipe)) {
				Collection<?> l = null;
				if (cm.isArray()) {
				// Primitive arrays and byte[] serialize as single row (value = [1;2;3] or base64)
				if (o.getClass().getComponentType().isPrimitive())
					l = Collections.singletonList(o);
				else
					l = l((Object[])o);
			} else if (cm.isCollection()) {
				l = (Collection<?>)o;
			} else if (cm.isStreamable()) {
				// CSV must inspect first element for column headers, so materialization is unavoidable.
				l = toListFromStreamable(o, cm);
			} else {
				l = Collections.singleton(o);
			}

			if (ne(l)) {
				var firstOpt = first(l);
				if (!firstOpt.isPresent())
					return;

				// Apply any registered swap to the first element to determine column structure.
				var firstRaw = firstOpt.get();
				var firstEntry = applySwap(firstRaw, getClassMetaForObject(firstRaw));
				var entryType = getClassMetaForObject(firstEntry);
				// If swapped type is not a bean (e.g. interface proxy), use raw type for strategy
				if (! entryType.isBean() && firstRaw != null) {
					var rawType = getClassMetaForObject(firstRaw);
					if (rawType.isBean())
						entryType = rawType;
				}

				// Expected type for each element (for type discriminator)
				var eType = cm.isArray() || cm.isCollection() || cm.isStreamable()
					? cm.getElementType()
					: getExpectedRootType(firstRaw);

				// CSV is flat; when addBeanTypes or addRootType is set, add _type column to all rows
				var addTypeColumn = isAddBeanTypes() || isAddRootType();
				var typeColName = addTypeColumn ? getBeanTypePropertyName(entryType) : null;

				// Determine the best representation strategy.
				// Use the BeanMeta from the entry type for bean serialization.
				var bm = entryType.isBean() ? entryType.getBeanMeta() : null;

				if (bm != null) {
					// Bean or DynaBean path: header row = property names + optional _type
					var addComma = Flag.create();
					bm.getProperties().values().stream().filter(BeanPropertyMeta::canRead).forEach(x -> {
						addComma.ifSet(() -> w.w(',')).set();
						w.writeEntry(x.getName());
					});
					// Always append _type when addBeanTypes or addRootType to support polymorphic parsing
					if (addTypeColumn && ne(typeColName)) {
						w.w(',');
						w.writeEntry(typeColName);
					}
					w.append('\n');
					var readableProps = bm.getProperties().values().stream().filter(BeanPropertyMeta::canRead).toList();
					l.forEach(x -> {
						var addComma2 = Flag.create();
						if (x == null) {
							// Null entry: write null marker for each column
							readableProps.forEach(y -> {
								addComma2.ifSet(() -> w.w(',')).set();
								w.writeEntry(nullValue);
							});
							if (addTypeColumn && ne(typeColName)) {
								w.w(',');
								w.writeEntry("");
							}
						} else {
							// Apply swap before extracting bean properties (e.g. surrogate swaps)
							var swapped = applySwap(x, getClassMetaForObject(x));
							var aType = getClassMetaForObject(swapped);
							// When swap yields non-bean (e.g. String), use raw object for property extraction
							var objForBean = aType.isBean() ? swapped : x;
							BeanMap<?> bean = toBeanMap(objForBean);
							readableProps.forEach(y -> {
								addComma2.ifSet(() -> w.w(',')).set();
								var value = y.get(bean, y.getName());
								value = formatIfDateOrDuration(value);
								value = formatForCsvCell(value);
								// Use toString() to respect trimStrings setting on String values
								if (value instanceof String s) value = toString(s);
								w.writeEntry(value != null ? value : nullValue);
							});
							if (addTypeColumn && ne(typeColName)) {
								var typeName = getBeanTypeName(this, eType, aType, null);
								w.w(',');
								w.writeEntry(typeName != null ? typeName : "");
							}
						}
						w.w('\n');
					});
				} else if (entryType.isMap()) {
					// Map path: header row = map keys from the first entry + optional _type
					var addComma = Flag.create();
					var first = (Map) firstEntry;
					first.keySet().forEach(x -> {
						addComma.ifSet(() -> w.w(',')).set();
						// Apply trimStrings to map keys as well
						Object keyVal;
						if (x == null)
							keyVal = nullValue;
						else if (x instanceof String s)
							keyVal = toString(s);
						else
							keyVal = x;
						w.writeEntry(keyVal);
					});
					if (addTypeColumn && ne(typeColName)) {
						w.w(',');
						w.writeEntry(typeColName);
					}
					w.append('\n');
					l.forEach(x -> {
						var addComma2 = Flag.create();
						var swapped = applySwap(x, getClassMetaForObject(x));
						var aType = getClassMetaForObject(swapped);
						var map = (Map) swapped;
						map.values().forEach(y -> {
							addComma2.ifSet(() -> w.w(',')).set();
							var value = applySwap(y, getClassMetaForObject(y));
							value = formatForCsvCell(value);
							// Apply trimStrings to map values
							if (value instanceof String s) value = toString(s);
							w.writeEntry(value != null ? value : nullValue);
						});
						if (addTypeColumn && ne(typeColName)) {
							var typeName = getBeanTypeName(this, eType, aType, null);
							w.w(',');
							w.writeEntry(typeName != null ? typeName : "");
						}
						w.w('\n');
					});
				} else {
					// Simple value path: single "value" column + optional _type
					w.writeEntry("value");
					if (addTypeColumn && ne(typeColName)) {
						w.w(',');
						w.writeEntry(typeColName);
					}
					w.append('\n');
					l.forEach(x -> {
						var value = applySwap(x, getClassMetaForObject(x));
						value = formatForCsvCell(value);
						// Use toString() to respect trimStrings setting
						w.writeEntry(value != null ? toString(value) : nullValue);
						if (addTypeColumn && ne(typeColName)) {
							if (x != null) {
								var aType = getClassMetaForObject(x);
								var typeName = getBeanTypeName(this, eType, aType, null);
								w.w(',');
								w.writeEntry(typeName != null ? typeName : "");
							} else {
								w.w(',');
								w.writeEntry("");
							}
						}
						w.w('\n');
					});
				}
			}
			}
		} finally {
			pop();
		}
	}


	private Object formatIfDateOrDuration(Object value) {
		if (value == null)
			return null;
		if (value instanceof Calendar || value instanceof Date || value instanceof Temporal
				|| value instanceof javax.xml.datatype.XMLGregorianCalendar)
			return Iso8601Utils.format(value, getClassMetaForObject(value), getTimeZone());
		if (value instanceof Duration)
			return value.toString();
		return value;
	}

	/**
	 * Formats a value for a CSV cell, handling byte[], primitive arrays, and nested structures.
	 */
	private Object formatForCsvCell(Object value) {
		if (value == null)
			return null;
		if (allowNestedStructures) {
			var type = getClassMetaForObject(value);
			if (value instanceof Map || value instanceof Collection || value instanceof Object[]
					|| (type.isBean() && !(value instanceof Map)))
				return new CsvCellSerializer(byteArrayFormat, nullValue).serialize(value, this);
		}
		if (value instanceof byte[] b) {
			return byteArrayFormat == ByteArrayFormat.SEMICOLON_DELIMITED
				? formatByteArraySemicolon(b)
				: base64Encode(b);
		}
		if (value instanceof int[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append(a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		if (value instanceof long[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append(a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		if (value instanceof double[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append(a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		if (value instanceof float[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append(a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		if (value instanceof short[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append(a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		if (value instanceof boolean[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append(a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		if (value instanceof char[] a) {
			var sb = new StringBuilder();
			sb.append('[');
			for (var i = 0; i < a.length; i++) {
				if (i > 0) sb.append(';');
				sb.append((int) a[i]);
			}
			sb.append(']');
			return sb.toString();
		}
		return value;
	}

	private static String formatByteArraySemicolon(byte[] b) {
		var sb = new StringBuilder();
		for (var i = 0; i < b.length; i++) {
			if (i > 0) sb.append(';');
			sb.append(b[i] & 0xff);
		}
		return sb.toString();
	}

	/**
	 * Prepares a value for inline cell serialization (bean→Map, date format, etc.).
	 * Used by {@link CsvCellSerializer}.
	 */
	Object prepareForInlineValue(Object value) {
		if (value == null)
			return null;
		var swapped = applySwap(value, getClassMetaForObject(value));
		var type = getClassMetaForObject(swapped);
		if (type.isBean() && !(swapped instanceof Map))
			return toBeanMap(swapped);
		if (swapped instanceof Calendar || swapped instanceof Date || swapped instanceof Temporal
				|| swapped instanceof javax.xml.datatype.XMLGregorianCalendar)
			return Iso8601Utils.format(swapped, type, getTimeZone());
		if (swapped instanceof java.time.Duration)
			return swapped.toString();
		return swapped;
	}

	CsvWriter getCsvWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof CsvWriter output2)
			return output2;
		var w = new CsvWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), getQuoteChar(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}
}