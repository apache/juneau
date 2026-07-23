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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;

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
	"java:S3776", // Cognitive complexity acceptable for doWrite / writeBeanWithHeadings
	"java:S6541", // Brain method acceptable for doWrite
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
	public static class Builder extends MarkdownSerializerSession.Builder<Builder> {

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

		@Override
		public MarkdownDocSerializerSession build() {
			return new MarkdownDocSerializerSession(this);
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
	protected void doWrite(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		try (var w = getMarkdownWriter(pipe)) {
			if (ine(headerContent)) {
				w.append(headerContent);
				w.blankLine();
			}

			if (ine(title)) {
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
				writeBeanWithHeadings(w, toBeanMap(o), headingLevel);
			} else if (cm.isCollectionOrArray()) {
				var l = cm.isArray() ? Arrays.asList((Object[]) o) : (Collection<?>) o;
				writeCollection(w, l, object(), cm);
			} else {
				writeAnything(w, o, cm, null);
			}
		}

			if (ine(footerContent)) {
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
	protected void writeBeanWithHeadings(MarkdownWriter w, BeanMap<?> bm, int level) throws IOException, SerializeException {
		// Collect simple properties for the key/value table
		var simpleProps = new LinkedHashMap<String,Object>();
		var complexProps = new LinkedHashMap<String,Object>();

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
				w.tableRow(MarkdownWriter.escapeCell(e.getKey()), writeInlineValue(e.getValue()));
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
					writeBeanWithHeadings(w, toBeanMap(swapped), level + 1);
				} else {
					// Heading cap: fall back to inline rendering
					writeBeanMap(w, toBeanMap(swapped), object(), swappedCm);
					w.blankLine();
				}
			} else if (swappedCm.isMap()) {
				writeMap(w, (Map<?,?>) swapped, swappedCm);
				w.blankLine();
			} else if (swappedCm.isCollectionOrArray()) {
				var l = swappedCm.isArray() ? Arrays.asList((Object[]) swapped) : (Collection<?>) swapped;
				writeCollection(w, l, object(), swappedCm);
				w.blankLine();
			} else {
				writeAnything(w, val, swappedCm, null);
				w.blankLine();
			}
		}
	}
}
