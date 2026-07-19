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
package org.apache.juneau.marshall.csv;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.time.*;
import java.time.Duration;
import java.time.temporal.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.marshall.swap.*;

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
	"resource"   // Internal helpers return Closeables wired into pipe lifecycle; Eclipse JDT @Owning warning is by design.
})
public class CsvSerializerSession extends WriterSerializerSession implements RecordWritable {

	// Property name constants
	private static final String PROP_byteArrayFormat = "byteArrayFormat";
	private static final String PROP_allowNestedStructures = "allowNestedStructures";
	private static final String PROP_nullValue = "nullValue";
	private static final String PROP_CsvSerializerSession_byteArrayFormat = "CsvSerializerSession.byteArrayFormat";
	private static final String PROP_CsvSerializerSession_allowNestedStructures = "CsvSerializerSession.allowNestedStructures";
	private static final String PROP_CsvSerializerSession_nullValue = "CsvSerializerSession.nullValue";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	private final CsvByteArrayCellFormat byteArrayFormat;
	private final boolean allowNestedStructures;
	private final String nullValue;

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for builder hierarchy
	})
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

		private CsvByteArrayCellFormat byteArrayFormat;
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

		/**
		 * Allow nested structures.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowNestedStructures(boolean value) {
			allowNestedStructures = value;
			return this;
		}

		@Override
		public CsvSerializerSession build() {
			return new CsvSerializerSession(this);
		}

		/**
		 * Byte array format.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder byteArrayFormat(CsvByteArrayCellFormat value) {
			if (nn(value))
				byteArrayFormat = value;
			return this;
		}

		/**
		 * Null value string.
		 *
		 * @param value The new value for this property. Can be <jk>null</jk> (resets to default).
		 * @return This object.
		 */
		public Builder nullValue(String value) {
			nullValue = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			if (key == null) {
				super.property(key, value);
				return this;
			}
			switch (key) {
				case PROP_byteArrayFormat, PROP_CsvSerializerSession_byteArrayFormat:
					return byteArrayFormat(cvt(value, CsvByteArrayCellFormat.class));
				case PROP_allowNestedStructures, PROP_CsvSerializerSession_allowNestedStructures:
					return allowNestedStructures(cvt(value, Boolean.class));
				case PROP_nullValue, PROP_CsvSerializerSession_nullValue:
					return nullValue(cvt(value, String.class));
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
		nullValue = or(builder.nullValue, "<NULL>");
	}

	@Override /* RecordWritable */
	public RecordWriter serializeRecords(Object output) throws IOException {
		return RecordAdapter.arrayWriter(this, output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() {
		return false;
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
	@SuppressWarnings({
		"rawtypes" // Raw types necessary for ObjectSwap handling
	})
	private Object applySwap(Object value, ClassMeta<?> type) {
		try {
			if (value == null || type == null)
				return value;

			ObjectSwap swap = type.getSwap(this);
			if (nn(swap)) {
				return swap(swap, value);
			}
			if (type.isDate())
				return serializeDate((Date)value, type);
			if (type.isCalendar())
				return serializeCalendar(value, type);
			if (type.isTemporal())
				return serializeTemporal((TemporalAccessor)value, type);
			if (type.isDuration())
				return serializeDuration((Duration)value);
			if (type.isPeriod())
				return serializePeriod((Period)value);
			return value;
		} catch (SerializeException e) {
			throw rex(e);
		}
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic type handling
		"unchecked" // Type erasure requires unchecked casts
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

			if (ine(l)) {
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
					if (addTypeColumn && ine(typeColName)) {
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
							if (addTypeColumn && ine(typeColName)) {
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
								w.writeEntry(or(value, nullValue));
							});
							if (addTypeColumn && ine(typeColName)) {
								var typeName = getBeanTypeName(this, eType, aType, null);
								w.w(',');
								w.writeEntry(or(typeName, ""));
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
					if (addTypeColumn && ine(typeColName)) {
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
							w.writeEntry(or(value, nullValue));
						});
						if (addTypeColumn && ine(typeColName)) {
							var typeName = getBeanTypeName(this, eType, aType, null);
							w.w(',');
							w.writeEntry(or(typeName, ""));
						}
						w.w('\n');
					});
				} else {
					// Simple value path: single "value" column + optional _type
					w.writeEntry("value");
					if (addTypeColumn && ine(typeColName)) {
						w.w(',');
						w.writeEntry(typeColName);
					}
					w.append('\n');
					l.forEach(x -> {
						var value = applySwap(x, getClassMetaForObject(x));
						value = formatForCsvCell(value);
						// Use toString() to respect trimStrings setting
						w.writeEntry(value != null ? toString(value) : nullValue);
						if (addTypeColumn && ine(typeColName)) {
							if (x != null) {
								var aType = getClassMetaForObject(x);
								var typeName = getBeanTypeName(this, eType, aType, null);
								w.w(',');
								w.writeEntry(or(typeName, ""));
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
		var cm = getClassMetaForObject(value);
		if (value instanceof Date d)
			return serializeDate(d, cm);
		if (value instanceof Calendar || value instanceof javax.xml.datatype.XMLGregorianCalendar)
			return serializeCalendar(value, cm);
		if (value instanceof TemporalAccessor t)
			return serializeTemporal(t, cm);
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
			return byteArrayFormat == CsvByteArrayCellFormat.SEMICOLON_DELIMITED
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
		if (swapped instanceof Date d)
			return serializeDate(d, type);
		if (swapped instanceof Calendar || swapped instanceof XMLGregorianCalendar)
			return serializeCalendar(swapped, type);
		if (swapped instanceof TemporalAccessor t)
			return serializeTemporal(t, type);
		if (swapped instanceof Duration)
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