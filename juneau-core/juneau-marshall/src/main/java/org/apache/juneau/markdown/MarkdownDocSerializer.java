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

import org.apache.juneau.commons.collections.*;

/**
 * Serializes POJO models to Markdown document mode (headings + tables).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/markdown, text/x-markdown</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/markdown</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Extends {@link MarkdownSerializer} to produce full Markdown documents with heading-based structure.
 * Simple bean properties are rendered in a key/value table under the top-level heading.
 * Nested bean and map properties are rendered as sub-sections with their own headings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to a Markdown document</jc>
 * 	String <jv>md</jv> = MarkdownDocSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someBean</jv>);
 *
 * 	<jc>// Create a custom doc serializer with a document title</jc>
 * 	MarkdownDocSerializer <jv>s</jv> = MarkdownDocSerializer.<jsm>create</jsm>().title(<js>"My Report"</js>).build();
 * 	String <jv>md</jv> = <jv>s</jv>.serialize(<jv>someBean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (nested bean):</h5>
 * <p class='bcode'>
 * 	# Person
 *
 * 	| Property | Value |
 * 	|---|---|
 * 	| name | Alice |
 * 	| age | 30 |
 *
 * 	## address
 *
 * 	| Property | Value |
 * 	|---|---|
 * 	| city | Boston |
 * 	| state | MA |
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Multi-line string values cannot round-trip; newlines are replaced with {@code <br>}.
 * 	<li class='note'>The configured null value string (default: {@code *null*}) cannot be stored as a literal string value.
 * 	<li class='note'>Document mode output requires {@link MarkdownDocParser} for parsing; heading text must match property names exactly.
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
public class MarkdownDocSerializer extends MarkdownSerializer {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends MarkdownSerializer.Builder {

		private static final Cache<HashKey,MarkdownDocSerializer> CACHE = Cache.of(HashKey.class, MarkdownDocSerializer.class).build();

		String title;
		int headingLevel;
		boolean addHorizontalRules;
		String headerContent;
		String footerContent;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			headingLevel = 1;
			type(MarkdownDocSerializer.class);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			title = copyFrom.title;
			headingLevel = copyFrom.headingLevel;
			addHorizontalRules = copyFrom.addHorizontalRules;
			headerContent = copyFrom.headerContent;
			footerContent = copyFrom.footerContent;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownDocSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			title = copyFrom.title;
			headingLevel = copyFrom.headingLevel;
			addHorizontalRules = copyFrom.addHorizontalRules;
			headerContent = copyFrom.headerContent;
			footerContent = copyFrom.footerContent;
		}

		/**
		 * The document title (rendered as the top-level heading).
		 *
		 * @param value The title.
		 * @return This object.
		 */
		public Builder title(String value) {
			title = value;
			return this;
		}

		/**
		 * The starting heading level (1-6).
		 *
		 * <p>
		 * Default is 1 ({@code # Heading}).
		 *
		 * @param value The heading level.
		 * @return This object.
		 */
		public Builder headingLevel(int value) {
			headingLevel = Math.min(6, Math.max(1, value));
			return this;
		}

		/**
		 * Whether to add horizontal rules ({@code ---}) between sections.
		 *
		 * @param value Whether to add horizontal rules.
		 * @return This object.
		 */
		public Builder addHorizontalRules(boolean value) {
			addHorizontalRules = value;
			return this;
		}

		/**
		 * Static content prepended to the document output (e.g. YAML front matter).
		 *
		 * @param value The header content.
		 * @return This object.
		 */
		public Builder headerContent(String value) {
			headerContent = value;
			return this;
		}

		/**
		 * Static content appended to the document output.
		 *
		 * @param value The footer content.
		 * @return This object.
		 */
		public Builder footerContent(String value) {
			footerContent = value;
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), title, headingLevel, addHorizontalRules, headerContent, footerContent);
		}

		@Override /* Overridden from Context.Builder */
		public MarkdownDocSerializer build() {
			return cache(CACHE).build(MarkdownDocSerializer.class);
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from MarkdownSerializer.Builder */
		public Builder nullValue(String value) {
			super.nullValue(value);
			return this;
		}

		@Override /* Overridden from MarkdownSerializer.Builder */
		public Builder showHeaders(boolean value) {
			super.showHeaders(value);
			return this;
		}
	}

	/**
	 * Default serializer, all default settings.
	 */
	public static final MarkdownDocSerializer DEFAULT = new MarkdownDocSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final String title;
	final int headingLevel;
	final boolean addHorizontalRules;
	final String headerContent;
	final String footerContent;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MarkdownDocSerializer(Builder builder) {
		super(builder);
		title = builder.title;
		headingLevel = builder.headingLevel;
		addHorizontalRules = builder.addHorizontalRules;
		headerContent = builder.headerContent;
		footerContent = builder.footerContent;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public MarkdownDocSerializerSession.Builder createSession() {
		return MarkdownDocSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public MarkdownDocSerializerSession getSession() { return createSession().build(); }
}
