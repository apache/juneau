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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.Optional;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link CsvParser}.
 *
 * <p>
 * Parses CSV (Comma Separated Values) input into Java objects.  The first row of the CSV
 * is treated as a header row providing column names.  Subsequent rows are treated as data rows.
 *
 * <p>
 * The following target type mappings are supported:
 * <ul>
 *   <li>{@code Collection<Bean>} / {@code Bean[]} — Header row provides property names; each data row becomes a bean.
 *   <li>{@code Collection<Map>} / {@code Map[]} — Header row provides map keys; each data row becomes a map.
 *   <li>{@code Collection<SimpleType>} / {@code SimpleType[]} — Single {@code value} column; each row's value is coerced to the element type.
 *   <li>Single {@code Bean} — Header row + one data row → one bean.
 *   <li>Single {@code Map} — Header row + one data row → one map.
 *   <li>{@code Object} — Returns a {@link JsonList} of {@link JsonMap} entries.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 */
@SuppressWarnings({
	"unchecked",
	"rawtypes",
})
public class CsvParserSession extends ReaderParserSession {

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParserSession.Builder {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(CsvParser ctx) {
			super(ctx);
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public CsvParserSession build() {
			return new CsvParserSession(this);
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
		public Builder outer(Object value) {
			super.outer(value);
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
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(CsvParser ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CsvParserSession(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		try (var r = CsvReader.from(pipe, ',', '"', isTrimStrings())) {
			if (r == null)
				return null;
			return parseAnything(type, r, getOuter(), null);
		}
	}

	/**
	 * Core parse dispatch.
	 *
	 * <p>
	 * Reads the header row, then dispatches to the appropriate parsing strategy based on the
	 * target type.
	 */
	private <T> T parseAnything(ClassMeta<T> eType, CsvReader r, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException {
		if (eType == null)
			eType = (ClassMeta<T>) object();

		var swap = (ObjectSwap<T,Object>) eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>) eType.getBuilderSwap(this);
		ClassMeta<?> sType;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T) Optional.ofNullable(parseAnything(eType.getElementType(), r, outer, pMeta));

		// Read header row
		var headers = r.readRow();
		if (headers == null || headers.isEmpty())
			return null;

		Object o = null;

		if (sType.isArray()) {
			var elementType = sType.getElementType();
			var list = list();
			for (var row = r.readRow(); row != null; row = r.readRow())
				list.add(parseRow(headers, row, elementType, list));
			o = toArray(sType, list);

		} else if (sType.isCollection()) {
			var elementType = sType.getElementType();
			Collection<Object> l = sType.canCreateNewInstance(outer) ? (Collection<Object>) sType.newInstance() : new ArrayList<>();
			for (var row = r.readRow(); row != null; row = r.readRow())
				l.add(parseRow(headers, row, elementType, l));
			o = l;

		} else if (sType.isBean()) {
			var row = r.readRow();
			if (row != null)
				o = parseRowIntoBean(headers, row, sType, outer);

		} else if (sType.isMap()) {
			var row = r.readRow();
			if (row != null)
				o = parseRowIntoMap(headers, row, sType, outer);

		} else if (sType.isObject()) {
			// For Object target type: return a JsonList of JsonMaps (or a single JsonMap if one row)
			var results = new JsonList(this);
			for (var row = r.readRow(); row != null; row = r.readRow()) {
				var m = new JsonMap(this);
				for (var i = 0; i < headers.size(); i++) {
					var val = i < row.size() ? row.get(i) : null;
					m.put(headers.get(i), parseCellValue(val, object()));
				}
				results.add(m);
			}
			o = results.isEmpty() ? null : (results.size() == 1 ? results.get(0) : results);
		} else {
			// For simple target types (String, Number, Boolean, etc.) that are not beans/maps/collections,
			// treat CSV as a single "value" column.  Read the first data row's value column.
			var valueColIdx = headers.indexOf("value");
			if (valueColIdx < 0) valueColIdx = 0;
			var row = r.readRow();
			if (row != null && valueColIdx < row.size())
				o = parseCellValue(row.get(valueColIdx), sType);
		}

		if (builder != null && o != null)
			o = builder.build(this, o, eType);

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		return (T) o;
	}

	/**
	 * Parses a single data row into the appropriate element object.
	 */
	private Object parseRow(List<String> headers, List<String> row, ClassMeta<?> eType, Object outer) throws ParseException {
		if (eType == null || eType.isObject()) {
			var m = new JsonMap(this);
			for (var i = 0; i < headers.size(); i++) {
				var val = i < row.size() ? row.get(i) : null;
				m.put(headers.get(i), parseCellValue(val, object()));
			}
			return m;
		} else if (eType.isBean()) {
			return parseRowIntoBean(headers, row, eType, outer);
		} else if (eType.isMap()) {
			return parseRowIntoMap(headers, row, eType, outer);
		} else {
			// Simple type: use the "value" column (first column) or the only column present
			var val = row.isEmpty() ? null : row.get(0);
			return parseCellValue(val, eType);
		}
	}

	/**
	 * Parses a single data row into a bean of the specified type.
	 */
	private <T> T parseRowIntoBean(List<String> headers, List<String> row, ClassMeta<T> eType, Object outer) throws ParseException {
		var m = newBeanMap(outer, eType.inner());
		for (var i = 0; i < headers.size(); i++) {
			var header = headers.get(i);
			var val = i < row.size() ? row.get(i) : null;
			var pm = m.getPropertyMeta(header);
			if (pm != null) {
				setCurrentProperty(pm);
				var converted = parseCellValue(val, pm.getClassMeta());
				pm.set(m, header, converted);
				setCurrentProperty(null);
			} else {
				onUnknownProperty(header, m, val);
			}
		}
		return m.getBean();
	}

	/**
	 * Parses a single data row into a map of the specified type.
	 */
	@SuppressWarnings("java:S3740")
	private Object parseRowIntoMap(List<String> headers, List<String> row, ClassMeta<?> eType, Object outer) throws ParseException {
		Map m;
		if (eType.canCreateNewInstance(outer))
			m = (Map) eType.newInstance(outer);
		else
			m = new JsonMap(this);
		var keyType = eType.getKeyType() != null ? eType.getKeyType() : string();
		var valueType = eType.getValueType() != null ? eType.getValueType() : object();
		for (var i = 0; i < headers.size(); i++) {
			var header = headers.get(i);
			var val = i < row.size() ? row.get(i) : null;
			var key = convertAttrToType(m, header, keyType);
			var value = parseCellValue(val, valueType);
			m.put(key, value);
		}
		return m;
	}

	/**
	 * Converts a raw CSV cell string value to the target type.
	 *
	 * <p>
	 * The unquoted literal {@code null} maps to Java {@code null}.
	 * All other values are converted via {@link #convertToType(Object, ClassMeta)}.
	 */
	private <T> T parseCellValue(String val, ClassMeta<T> eType) throws ParseException {
		if (val == null || val.equals("null"))
			return null;
		// Apply trimStrings at the cell level (before type conversion) so that the String
		// value passed to convertToType() is already trimmed.
		if (isTrimStrings() && val != null)
			val = val.trim();
		if (val.isEmpty() && eType.isCharSequence())
			return null;
		try {
			return convertToType(val, eType);
		} catch (InvalidDataConversionException e) {
			throw new ParseException(e, "Could not convert CSV cell value ''{0}'' to type ''{1}''.", val, eType);
		}
	}
}
