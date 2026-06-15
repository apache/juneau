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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Parses CSV (Comma Separated Values) input into Java objects.
 *
 * <p>
 * Parses RFC 4180-compliant CSV into collections of beans, maps, or simple values. Each row becomes an
 * element; the header row defines column names.
 *
 * <h5 class='section'>JSON Parity Features (opt-in):</h5>
 * <ul>
 *   <li><b>Type discriminator</b> — Parse {@code _type} column when present; use {@link CsvParser.Builder#beanDictionary(Class[])
 *       beanDictionary()} for polymorphic types.
 *   <li><b>Byte arrays</b> — {@link CsvParser.Builder#byteArrayFormat(CsvByteArrayCellFormat) byteArrayFormat(BASE64)} or
 *       {@code SEMICOLON_DELIMITED}; primitive arrays from {@code [1;2;3]}.
 *   <li><b>Nested structures</b> — {@link CsvParser.Builder#allowNestedStructures(boolean) allowNestedStructures(true)}
 *       parses inline {@code {key:val}} and {@code [val;val]} in cells.
 *   <li><b>Null marker</b> — {@link CsvParser.Builder#nullValue(String) nullValue("&lt;NULL&gt;")} (default); cells
 *       matching this are parsed as null.
 * </ul>
 *
 * <h5 class='section'>Data Structures Not Supported:</h5>
 * <ul>
 *   <li><b>Parent/inherited properties</b> — Bean hierarchy cannot be reconstructed from flat columns.
 *   <li><b>Optional wrappers</b> — Round-trip may differ from tree formats.
 * </ul>
 *
 * <h5 class='section'>Best Supported:</h5>
 * <p>
 * Parsing into {@link Collection} of flat beans or maps, or single beans/maps with simple
 * property types (primitives, strings, numbers, enums, dates, byte arrays, or nested structures when enabled).
 *
 * <h5 class='figure'>Example input (list of maps with a,b):</h5>
 * <p class='bcode'>
 * 	a,b
 * 	foo,bar
 * </p>
 *
 * <h5 class='figure'>Complex (list of beans with nested address, flattened):</h5>
 * <p class='bcode'>
 * 	name,age,address_street,address_city,address_state,tags
 * 	Alice,30,123 Main St,Boston,MA,"a,b,c"
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention
	"resource" // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class CsvParser extends ReaderParser implements CsvMetaProvider, RecordReadable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParser.Builder<Builder> {

		private static final Cache<HashKey,CsvParser> CACHE = Cache.of(HashKey.class, CsvParser.class).build();

		private CsvByteArrayCellFormat byteArrayFormat;
		private boolean allowNestedStructures;
		private String nullValue;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("text/csv");
			byteArrayFormat = CsvByteArrayCellFormat.BASE64;
			nullValue = "<NULL>";
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			byteArrayFormat = copyFrom.byteArrayFormat;
			allowNestedStructures = copyFrom.allowNestedStructures;
			nullValue = copyFrom.nullValue;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(CsvParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			byteArrayFormat = copyFrom.byteArrayFormat;
			allowNestedStructures = copyFrom.allowNestedStructures;
			nullValue = copyFrom.nullValue;
		}

		/**
		 * String that denotes null when parsing. Must match the serializer's nullValue.
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public Builder nullValue(String value) {
			nullValue = value;
			return this;
		}

		/**
		 * Enables parsing of inline {@code {key:val}} and {@code [val;val]} notation in cells.
		 *
		 * @param value Whether to allow nested structures.
		 * @return This object.
		 */
		public Builder allowNestedStructures(boolean value) {
			allowNestedStructures = value;
			return this;
		}

		/**
		 * Format for parsing {@code byte[]} arrays from CSV cells.
		 *
		 * <p>
		 * Must match the format used by the serializer. Default is {@link CsvByteArrayCellFormat#BASE64}.
		 *
		 * @param value The format to use.
		 * @return This object.
		 */
		public Builder byteArrayFormat(CsvByteArrayCellFormat value) {
			byteArrayFormat = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), byteArrayFormat, allowNestedStructures, nullValue);
		}

		@Override /* Overridden from Context.Builder<?> */
		public CsvParser build() {
			return cache(CACHE).build(CsvParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}


	}

	/** Default parser, all default settings.*/
	public static final CsvParser DEFAULT = new CsvParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<ClassMeta<?>,CsvClassMeta> csvClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,CsvBeanPropertyMeta> csvBeanPropertyMetas = new ConcurrentHashMap<>();
	private final CsvByteArrayCellFormat byteArrayFormat;
	private final boolean allowNestedStructures;
	private final String nullValue;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public CsvParser(Builder builder) {
		super(builder);
		byteArrayFormat = builder.byteArrayFormat != null ? builder.byteArrayFormat : CsvByteArrayCellFormat.BASE64;
		allowNestedStructures = builder.allowNestedStructures;
		nullValue = builder.nullValue != null ? builder.nullValue : "<NULL>";
	}

	/**
	 * Returns the format for parsing {@code byte[]} arrays from CSV cells.
	 *
	 * @return The byte array format.
	 */
	public CsvByteArrayCellFormat getByteArrayFormat() {
		return byteArrayFormat;
	}

	/**
	 * Returns whether inline {@code {key:val}} and {@code [val;val]} notation parsing is enabled.
	 *
	 * @return Whether nested structures are allowed.
	 */
	public boolean isAllowNestedStructures() {
		return allowNestedStructures;
	}

	/**
	 * Returns the string that denotes null when parsing.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public CsvParserSession.Builder createSession() {
		return CsvParserSession.create(this);
	}

	@Override /* Overridden from CsvMetaProvider */
	public CsvBeanPropertyMeta getCsvBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return CsvBeanPropertyMeta.DEFAULT;
		return csvBeanPropertyMetas.computeIfAbsent(bpm, k -> new CsvBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from CsvMetaProvider */
	public CsvClassMeta getCsvClassMeta(ClassMeta<?> cm) {
		return csvClassMetas.computeIfAbsent(cm, k -> new CsvClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public CsvParserSession getSession() { return createSession().build(); }

	/**
	 * Convenience delegator that opens a {@link RecordReader} over the input using
	 * <b>default session arguments</b> (mirrors {@link #parse(Object, Class)}).
	 *
	 * <p>
	 * CSV is naturally row-oriented &mdash; each row becomes one record &mdash; but the current cursor
	 * is buffered (the full row list is materialized at parse); a streaming implementation is a Phase 3b
	 * deferred item, so {@link #isRecordStreaming()} returns <jk>false</jk>.
	 *
	 * <p>
	 * The real implementation lives on {@link CsvParserSession#parseRecords(Object)}.  Callers that need
	 * request-derived configuration (locale, timezone, schema, swaps) should call {@link #createSession()}
	 * and invoke {@link CsvParserSession#parseRecords(Object)} on the built session instead.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return getSession().parseRecords(input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}
}