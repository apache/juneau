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
 * Parses Markdown document-mode output (headings + tables) back to POJOs.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>text/markdown, text/x-markdown</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Extends {@link MarkdownParser} to parse Markdown documents produced by {@link MarkdownDocSerializer}.
 * Heading sections are mapped back to nested bean properties based on heading text matching property names.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse a Markdown document back to a bean</jc>
 * 	String <jv>md</jv> = <js>"# Person\n\n| Property | Value |\n|---|---|\n| name | Alice |\n\n## address\n\n| Property | Value |\n|---|---|\n| city | Boston |"</js>;
 * 	Person <jv>p</jv> = MarkdownDocParser.<jsf>DEFAULT</jsf>.parse(<jv>md</jv>, Person.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Round-trip example</jc>
 * 	String <jv>md</jv> = MarkdownDocSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 * 	MyBean <jv>parsed</jv> = MarkdownDocParser.<jsf>DEFAULT</jsf>.parse(<jv>md</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Heading text must match bean property names exactly (case-sensitive).
 * 	<li class='note'>Heading levels must be consistent with serializer output; manually edited heading levels may not parse correctly.
 * 	<li class='note'>A heading followed by a bulleted list is treated as a {@code List&lt;String&gt;} property.
 * 	<li class='note'>A heading followed by a multi-column table is treated as a {@code List&lt;Bean&gt;} property.
 * 	<li class='note'>The top-level heading (title) is ignored; only sub-headings map to properties.
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
public class MarkdownDocParser extends MarkdownParser {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends MarkdownParser.Builder {

		private static final Cache<HashKey,MarkdownDocParser> CACHE = Cache.of(HashKey.class, MarkdownDocParser.class).build();

		int headingLevel;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			headingLevel = 1;
			type(MarkdownDocParser.class);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			headingLevel = copyFrom.headingLevel;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownDocParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			headingLevel = copyFrom.headingLevel;
		}

		/**
		 * The expected starting heading level for the document.
		 *
		 * <p>
		 * Default is 1 ({@code # Heading}). Must match the level used by the serializer.
		 *
		 * @param value The heading level.
		 * @return This object.
		 */
		public Builder headingLevel(int value) {
			headingLevel = Math.min(6, Math.max(1, value));
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), headingLevel);
		}

		@Override /* Overridden from Context.Builder */
		public MarkdownDocParser build() {
			return cache(CACHE).build(MarkdownDocParser.class);
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from MarkdownParser.Builder */
		public Builder nullValue(String value) {
			super.nullValue(value);
			return this;
		}
	}

	/**
	 * Default parser, all default settings.
	 */
	public static final MarkdownDocParser DEFAULT = new MarkdownDocParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final int headingLevel;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MarkdownDocParser(Builder builder) {
		super(builder);
		headingLevel = builder.headingLevel;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public MarkdownDocParserSession.Builder createSession() {
		return MarkdownDocParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public MarkdownDocParserSession getSession() { return createSession().build(); }
}
