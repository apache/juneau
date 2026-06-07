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
package org.apache.juneau.markdown;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to Markdown (fragment mode: tables and lists).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/markdown, text/x-markdown</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/markdown</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows:
 * <ul class='spaced-list'>
 * 	<li>Beans and Maps are serialized as 2-column key/value Markdown tables.
 * 	<li>Collections and arrays of beans/maps with uniform structure are serialized as multi-column Markdown tables.
 * 	<li>Collections and arrays of simple values are serialized as Markdown bulleted lists.
 * 	<li>Nested beans and complex values in table cells are serialized as inline JSON5 wrapped in backticks.
 * 	<li>Null values are rendered as <js>"*null*"</js> (italic) by default.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default serializer to serialize a bean</jc>
 * 	String <jv>md</jv> = MarkdownSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	MarkdownSerializer <jv>serializer</jv> = MarkdownSerializer.<jsm>create</jsm>().nullValue(<js>"—"</js>).build();
 *
 * 	<jc>// Serialize a List of beans to a multi-column Markdown table</jc>
 * 	String <jv>md</jv> = <jv>serializer</jv>.serialize(<jv>listOfBeans</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean):</h5>
 * <p class='bcode'>
 * 	| Property | Value |
 * 	|---|---|
 * 	| name | Alice |
 * 	| age | 30 |
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Multi-line string values cannot round-trip; newlines are replaced with {@code <br>}.
 * 	<li class='note'>The configured null value string (default: {@code *null*}) cannot be stored as a literal string value.
 * 	<li class='note'>This serializer is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarkdownBasics">Markdown Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class MarkdownSerializer extends WriterSerializer implements MarkdownMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";
	private static final String CONST_null = "*null*";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends WriterSerializer.Builder<SELF> {

		private static final Cache<HashKey,MarkdownSerializer> CACHE = Cache.of(HashKey.class, MarkdownSerializer.class).build();

		String nullValue;
		boolean showHeaders;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/markdown");
			accept("text/markdown,text/x-markdown");
			nullValue = CONST_null;
			showHeaders = true;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
			showHeaders = copyFrom.showHeaders;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
			showHeaders = copyFrom.showHeaders;
		}

		/**
		 * The string to render for null values.
		 *
		 * <p>
		 * Default is {@code *null*} (rendered as Markdown italic).
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public SELF nullValue(String value) {
			nullValue = value == null ? CONST_null : value;
			return self();
		}

		/**
		 * Whether to show table header rows.
		 *
		 * <p>
		 * Default is <jk>true</jk>.
		 *
		 * @param value Whether to show headers.
		 * @return This object.
		 */
		public SELF showHeaders(boolean value) {
			showHeaders = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullValue, showHeaders);
		}

		@Override /* Overridden from Context.Builder<?> */
		public MarkdownSerializer build() {
			return cache(CACHE).build(MarkdownSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link MarkdownSerializer#create()} / {@link MarkdownSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(MarkdownSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Default serializer, all default settings.
	 */
	public static final MarkdownSerializer DEFAULT = new MarkdownSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	private final Map<ClassMeta<?>,MarkdownClassMeta> markdownClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,MarkdownBeanPropertyMeta> markdownBeanPropertyMetas = new ConcurrentHashMap<>();
	final String nullValue;
	final boolean showHeaders;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MarkdownSerializer(Builder<?> builder) {
		super(builder);
		nullValue = builder.nullValue != null ? builder.nullValue : CONST_null;
		showHeaders = builder.showHeaders;
	}

	/**
	 * Returns the string rendered for null values.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	/**
	 * Returns whether table headers are shown.
	 *
	 * @return Whether headers are shown.
	 */
	public boolean isShowHeaders() {
		return showHeaders;
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public MarkdownSerializerSession.Builder<?> createSession() {
		return MarkdownSerializerSession.create(this);
	}

	@Override /* Overridden from MarkdownMetaProvider */
	public MarkdownBeanPropertyMeta getMarkdownBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return MarkdownBeanPropertyMeta.DEFAULT;
		return markdownBeanPropertyMetas.computeIfAbsent(bpm, k -> new MarkdownBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from MarkdownMetaProvider */
	public MarkdownClassMeta getMarkdownClassMeta(ClassMeta<?> cm) {
		return markdownClassMetas.computeIfAbsent(cm, k -> new MarkdownClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public MarkdownSerializerSession getSession() { return createSession().build(); }
}
