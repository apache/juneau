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

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;

/**
 * Session object for {@link MarkdownDocSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"java:S115", // Constants use UPPER_snakeCase convention
	"java:S3776", // Cognitive complexity acceptable for doSerialize / serializeBeanWithHeadings
	"java:S6541", // Brain method acceptable for doSerialize
	"resource", // MarkdownWriter/Writer lifecycle managed by SerializerPipe
	"rawtypes",
})
public class MarkdownDocSerializerSession extends MarkdownSerializerSession {

	private static final String ARG_ctx = "ctx";

	private final String title;
	private final int headingLevel;
	private final boolean addHorizontalRules;
	private final String headerContent;
	private final String footerContent;

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for doc serializer session builder hierarchy
	})
	public static class Builder extends MarkdownSerializerSession.Builder {

		String title;
		int headingLevel;
		boolean addHorizontalRules;
		String headerContent;
		String footerContent;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownDocSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			title = ctx.title;
			headingLevel = ctx.headingLevel;
			addHorizontalRules = ctx.addHorizontalRules;
			headerContent = ctx.headerContent;
			footerContent = ctx.footerContent;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public MarkdownDocSerializerSession build() {
			return new MarkdownDocSerializerSession(this);
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
	public static Builder create(MarkdownDocSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarkdownDocSerializerSession(Builder builder) {
		super(builder);
		title = builder.title;
		headingLevel = builder.headingLevel;
		addHorizontalRules = builder.addHorizontalRules;
		headerContent = builder.headerContent;
		footerContent = builder.footerContent;
	}

	@Override /* Overridden from MarkdownSerializerSession */
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		try (var w = getMarkdownWriter(pipe)) {
			if (ne(headerContent)) {
				w.append(headerContent);
				w.blankLine();
			}

			if (ne(title)) {
				w.heading(headingLevel, title);
				w.blankLine();
			}

			if (o == null) {
				w.text(nullValue);
		} else {
			@SuppressWarnings({
				"java:S3740" // Raw ClassMeta from getClassMetaForObject
			})
			ClassMeta cm = getClassMetaForObject(o);
			@SuppressWarnings({
				"java:S3740" // Raw ObjectSwap from ClassMeta.getSwap
			})
			ObjectSwap swap = cm.getSwap(this);
			if (swap != null) {
				o = swap(swap, o);
				cm = swap.getSwapClassMeta(this);
			}

			if (cm.isBean()) {
				serializeBeanWithHeadings(w, toBeanMap(o), headingLevel);
			} else if (cm.isCollectionOrArray()) {
				var l = cm.isArray() ? java.util.Arrays.asList((Object[]) o) : (java.util.Collection<?>) o;
				serializeCollection(w, l, object(), cm);
			} else {
				serializeAnything(w, o, cm, null);
			}
		}

			if (ne(footerContent)) {
				w.blankLine();
				w.append(footerContent);
			}
		}
	}

	/**
	 * Serializes a bean in document mode, using headings to structure nested objects.
	 *
	 * <p>
	 * Simple properties go into a key/value table. Nested bean, map, and collection properties
	 * get their own sub-headings.
	 *
	 * @param w The writer.
	 * @param bm The bean map.
	 * @param level The current heading level.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException If serialization fails.
	 */
	protected void serializeBeanWithHeadings(MarkdownWriter w, BeanMap<?> bm, int level) throws IOException, SerializeException {
		// Phase 1: collect simple properties for the key/value table
		var simpleProps = new java.util.LinkedHashMap<String, Object>();
		var complexProps = new java.util.LinkedHashMap<String, Object>();

		for (var e : bm.entrySet()) {
			var key = e.getKey();
			var val = e.getValue();
			if (val == null) {
				simpleProps.put(key, null);
			} else {
				ClassMeta cm = getClassMetaForObject(val);
				ObjectSwap swap = cm.getSwap(this);
				Object swapped = val;
				ClassMeta swappedCm = cm;
				if (swap != null) {
					swapped = swap(swap, val);
					swappedCm = swap.getSwapClassMeta(this);
				}
				if (swappedCm.isBean() || swappedCm.isMap() || swappedCm.isCollectionOrArray() || swappedCm.isStreamable()) {
					complexProps.put(key, swapped);
				} else {
					simpleProps.put(key, val);
				}
			}
		}

		// Render simple properties as a key/value table
		if (!simpleProps.isEmpty()) {
			if (showHeaders) {
				w.tableHeader("Property", "Value");
				w.tableSeparator(2);
			}
			for (var e : simpleProps.entrySet())
				w.tableRow(MarkdownWriter.escapeCell(e.getKey()), serializeInlineValue(e.getValue()));
			w.blankLine();
		}

		// Render complex properties as sub-sections
		for (var e : complexProps.entrySet()) {
			var key = e.getKey();
			var val = e.getValue();

			if (addHorizontalRules)
				w.horizontalRule();

			w.heading(level + 1, key);
			w.blankLine();

			ClassMeta cm = getClassMetaForObject(val);
			ObjectSwap swap = cm.getSwap(this);
			Object swapped = val;
			ClassMeta swappedCm = cm;
			if (swap != null) {
				swapped = swap(swap, val);
				swappedCm = swap.getSwapClassMeta(this);
			}

			if (swappedCm.isBean()) {
				if (level + 1 < 6) {
					serializeBeanWithHeadings(w, toBeanMap(swapped), level + 1);
				} else {
					// Heading cap: fall back to inline rendering
					serializeBeanMap(w, toBeanMap(swapped), object(), swappedCm);
					w.blankLine();
				}
			} else if (swappedCm.isMap()) {
				serializeMap(w, (Map<?,?>) swapped, swappedCm);
				w.blankLine();
			} else if (swappedCm.isCollectionOrArray()) {
				var l = swappedCm.isArray() ? java.util.Arrays.asList((Object[]) swapped) : (java.util.Collection<?>) swapped;
				serializeCollection(w, l, object(), swappedCm);
				w.blankLine();
			} else {
				serializeAnything(w, val, swappedCm, null);
				w.blankLine();
			}
		}
	}
}
