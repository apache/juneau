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
package org.apache.juneau.marshall.markdown;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Parses Markdown fragment-mode output (tables and lists) back to POJOs.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>text/markdown, text/x-markdown</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Parses Markdown tables and bulleted lists produced by {@link MarkdownSerializer} back into Java objects.
 * The following mappings apply:
 * <ul class='spaced-list'>
 * 	<li>2-column key/value tables are parsed as beans or {@link Map}s.
 * 	<li>Multi-column tables are parsed as {@link List}s of beans or {@link Map}s.
 * 	<li>Bulleted lists are parsed as {@link List}s.
 * 	<li>Table cells containing backtick-wrapped JSON5 values are parsed via {@link Json5Parser}.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse a Markdown table to a bean</jc>
 * 	String <jv>md</jv> = <js>"| Property | Value |\n|---|---|\n| name | Alice |\n| age | 30 |"</js>;
 * 	Person <jv>p</jv> = MarkdownParser.<jsf>DEFAULT</jsf>.parse(<jv>md</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Round-trip example</jc>
 * 	String <jv>md</jv> = MarkdownSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 * 	MyBean <jv>parsed</jv> = MarkdownParser.<jsf>DEFAULT</jsf>.parse(<jv>md</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Multi-line string values cannot round-trip; newlines in cells are literal {@code <br>} text.
 * 	<li class='note'>The configured null value string (default: {@code *null*}) is interpreted as {@code null}, not as the literal string.
 * 	<li class='note'>Without a target type, table cell values are auto-detected as Integer, Long, Double, Boolean, or String.
 * 	<li class='note'>Type ambiguity: without a target type, tables return {@code Map<String,Object>} (key/value) or {@code List<Map>} (multi-column).
 * 	<li class='note'>This class is thread safe and reusable.
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
public class MarkdownParser extends ReaderParser implements MarkdownMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";
	private static final String CONST_null = "*null*";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends ReaderParser.Builder<SELF> {

		private static final Cache<HashKey,MarkdownParser> CACHE = Cache.of(HashKey.class, MarkdownParser.class).build();

		String nullValue;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("text/markdown,text/x-markdown");
			nullValue = CONST_null;
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
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
		}

		/**
		 * The string that is interpreted as {@code null} when parsing cell values.
		 *
		 * <p>
		 * Must match the value configured on the serializer. Default is {@code *null*}.
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public SELF nullValue(String value) {
			nullValue = value == null ? CONST_null : value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullValue);
		}

		@Override /* Overridden from Context.Builder<?> */
		public MarkdownParser build() {
			return cache(CACHE).build(MarkdownParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link MarkdownParser#create()} / {@link MarkdownParser#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(MarkdownParser copyFrom) {
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
	 * Default parser, all default settings.
	 */
	public static final MarkdownParser DEFAULT = new MarkdownParser(create());

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

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MarkdownParser(Builder<?> builder) {
		super(builder);
		nullValue = builder.nullValue != null ? builder.nullValue : CONST_null;
	}

	/**
	 * Returns the string that is interpreted as null when parsing.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public MarkdownParserSession.Builder<?> createSession() {
		return MarkdownParserSession.create(this);
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
	public MarkdownParserSession getSession() { return createSession().build(); }
}
