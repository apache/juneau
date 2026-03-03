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

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;

/**
 * Session object for {@link MarkdownDocParser}.
 *
 * <p>
 * Parses Markdown documents (headings + tables) produced by {@link MarkdownDocSerializer}
 * into Java objects by mapping heading sections to nested bean properties.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for parser session hierarchy
	"java:S115", // Constants use UPPER_snakeCase convention
	"java:S3776", // Cognitive complexity acceptable for doParse / parseSections
	"java:S6541", // Brain method acceptable for parseSections
	"unchecked",
	"rawtypes",
})
public class MarkdownDocParserSession extends MarkdownParserSession {

	private static final String CONST_Property = "Property";
	private static final String CONST_Value = "Value";

	private final int headingLevel;

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for parser session builder hierarchy
	})
	public static class Builder extends MarkdownParserSession.Builder {

		int headingLevel;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(MarkdownDocParser ctx) {
			super(ctx);
			headingLevel = ctx.headingLevel;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public MarkdownDocParserSession build() {
			return new MarkdownDocParserSession(this);
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
	public static Builder create(MarkdownDocParser ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarkdownDocParserSession(Builder builder) {
		super(builder);
		headingLevel = builder.headingLevel;
	}

	@Override /* Overridden from MarkdownParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		try (var r = new java.io.BufferedReader(pipe.getReader())) {
			var lines = readAllLines(r);
			return parseDocAnything(lines, type, getOuter(), headingLevel);
		}
	}

	/**
	 * Main document-mode parse dispatch.
	 *
	 * @param <T> The target type.
	 * @param lines All input lines.
	 * @param eType The expected type.
	 * @param outer The outer object.
	 * @param level The expected heading level for the top-level section.
	 * @return The parsed object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException If parsing fails.
	 */
	protected <T> T parseDocAnything(List<String> lines, ClassMeta<T> eType, Object outer, int level) throws IOException, ParseException {
		if (eType == null)
			eType = (ClassMeta<T>) object();

		var swap = (org.apache.juneau.swap.ObjectSwap<T,Object>) eType.getSwap(this);
		var builder = (org.apache.juneau.swap.BuilderSwap<T,Object>) eType.getBuilderSwap(this);
		ClassMeta<?> sType;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		Object o = null;

		if (sType.isBean()) {
			o = parseBeanFromDoc(lines, sType, outer, level);
		} else if (sType.isMap()) {
			o = parseMapFromDoc(lines, sType, outer, level);
		} else {
			// For collections/arrays and simple types, fall back to fragment parsing
			o = parseAnything(lines, (ClassMeta<T>) sType, outer, null);
		}

		if (builder != null && o != null)
			o = builder.build(this, o, eType);

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		return (T) o;
	}

	/**
	 * Parses a bean from Markdown document structure (headings + tables).
	 *
	 * <p>
	 * The top-level heading (title) is ignored. Inline key/value tables at the current heading level
	 * populate simple properties. Sub-headings at {@code level+1} map to nested bean properties.
	 *
	 * @param lines All document lines.
	 * @param sType The bean type.
	 * @param outer The outer object.
	 * @param level The current heading level.
	 * @return The parsed bean.
	 * @throws ParseException If parsing fails.
	 */
	@SuppressWarnings({
		"java:S135" // Multiple continue statements in loop; refactoring would reduce clarity
	})
	protected Object parseBeanFromDoc(List<String> lines, ClassMeta<?> sType, Object outer, int level) throws IOException, ParseException {
		var m = newBeanMap(outer, sType.inner());

		// Segment lines into: top-level content and sub-sections
		// Sections are delineated by headings at level+1
		var sections = segmentBySections(lines, level + 1);

		// The "root" section (before any sub-heading) holds the key/value table
		var rootLines = sections.get("");
		if (rootLines != null && !rootLines.isEmpty()) {
			// Filter out the top-level heading (level)
			var tableLines = rootLines.stream()
				.filter(l -> !isHeadingLine(l, level))
				.toList();

			// Parse the key/value table into the bean
			var tableRows = tableLines.stream().filter(l -> l.trim().startsWith("|")).toList();
			if (!tableRows.isEmpty()) {
				var headers = splitTableRow(tableRows.get(0));
				var isKeyValue = headers.size() == 2
					&& (headers.get(0).equalsIgnoreCase(CONST_Property) || headers.get(0).equalsIgnoreCase("Key"))
					&& headers.get(1).equalsIgnoreCase(CONST_Value);

				if (isKeyValue) {
					var dataLines = tableRows.stream().skip(1).filter(l -> !isSeparatorRow(l)).toList();
					for (var line : dataLines) {
						var cells = splitTableRow(line);
						if (cells.size() < 2)
							continue;
						var key = cells.get(0);
						var rawVal = cells.get(1);
						var pm = m.getPropertyMeta(key);
						if (pm != null) {
							setCurrentProperty(pm);
							var val = parseCellValue(rawVal, pm.getClassMeta(), m.getBean(false));
							pm.set(m, key, val);
							setCurrentProperty(null);
						} else {
							onUnknownProperty(key, m, rawVal);
						}
					}
				}
			}
		}

		// Process sub-sections (each maps to a nested bean property)
		for (var entry : sections.entrySet()) {
			var sectionName = entry.getKey();
			if (sectionName.isEmpty())
				continue; // skip root section already processed

			var sectionLines = entry.getValue();
			var pm = m.getPropertyMeta(sectionName);
			if (pm == null)
				continue; // Skip root, unknown properties, or sections we ignore

			setCurrentProperty(pm);
			var propCm = pm.getClassMeta();

			// Find first content line to determine if it's a table or list
			var contentLines = sectionLines.stream()
				.filter(l -> !isHeadingLine(l, level + 1) && !l.isBlank())
				.toList();

			Object propVal;
			if (!contentLines.isEmpty() && contentLines.get(0).trim().startsWith("|")) {
				// Table content
				var tableRows = sectionLines.stream().filter(l -> l.trim().startsWith("|")).toList();
				if (!tableRows.isEmpty()) {
					var tableHeaders = splitTableRow(tableRows.get(0));
					var isKeyValue = tableHeaders.size() == 2
						&& (tableHeaders.get(0).equalsIgnoreCase(CONST_Property) || tableHeaders.get(0).equalsIgnoreCase("Key"))
						&& tableHeaders.get(1).equalsIgnoreCase(CONST_Value);

					if (isKeyValue && propCm.isBean()) {
						// Nested bean via key/value table
						propVal = parseDocAnything(sectionLines, (ClassMeta<Object>) propCm, m.getBean(), level + 1);
					} else {
						// Key/value table (non-bean) or multi-column table → list or array
						propVal = parseTable(sectionLines, propCm, m.getBean());
					}
				} else {
					propVal = null;
				}
			} else if (!contentLines.isEmpty() && isBulletLine(contentLines.get(0))) {
				// List content
				propVal = parseBulletList(sectionLines, propCm, m.getBean());
			} else {
				propVal = propCm.isBean()
					? parseDocAnything(sectionLines, (ClassMeta<Object>) propCm, m.getBean(), level + 1)
					: parseAnything(sectionLines, (ClassMeta<Object>) propCm, m.getBean(), pm);
			}

			pm.set(m, sectionName, propVal);
			setCurrentProperty(null);
		}

		return m.getBean();
	}

	/**
	 * Parses a map from Markdown document structure.
	 *
	 * @param lines All document lines.
	 * @param sType The map type.
	 * @param outer The outer object.
	 * @param level The current heading level.
	 * @return The parsed map.
	 * @throws ParseException If parsing fails.
	 */
	@SuppressWarnings({
		"java:S3740" // Raw Map needed for generic map construction from ClassMeta
	})
	protected Object parseMapFromDoc(List<String> lines, ClassMeta<?> sType, Object outer, int level) throws IOException, ParseException {
		// For maps, use the key/value table approach at the root level, then add sub-section maps
		Map result = sType.canCreateNewInstance(outer) ? (Map) sType.newInstance(outer) : new JsonMap(this);
		var keyType = sType.getKeyType() != null ? sType.getKeyType() : string();
		var valueType = sType.getValueType() != null ? sType.getValueType() : object();

		var sections = segmentBySections(lines, level + 1);
		var rootLines = sections.get("");
		if (rootLines != null) {
			var tableRows = rootLines.stream().filter(l -> l.trim().startsWith("|")).toList();
			if (!tableRows.isEmpty()) {
				var headers = splitTableRow(tableRows.get(0));
				var isKeyValue = headers.size() == 2
					&& (headers.get(0).equalsIgnoreCase(CONST_Property) || headers.get(0).equalsIgnoreCase("Key"))
					&& headers.get(1).equalsIgnoreCase(CONST_Value);

				if (isKeyValue) {
					var dataLines = tableRows.stream().skip(1).filter(l -> !isSeparatorRow(l)).toList();
					for (var line : dataLines) {
						var cells = splitTableRow(line);
						if (cells.size() < 2)
							continue;
						var key = convertAttrToType(result, cells.get(0), keyType);
						var val = parseCellValue(cells.get(1), valueType, null);
						result.put(key, val);
					}
				}
			}
		}

		for (var entry : sections.entrySet()) {
			var sectionName = entry.getKey();
			if (sectionName.isEmpty())
				continue;
			var key = convertAttrToType(result, sectionName, keyType);
			var val = parseAnything(entry.getValue(), (ClassMeta<Object>) valueType, result, null);
			result.put(key, val);
		}

		return result;
	}

	/**
	 * Segments document lines into sections by heading level.
	 *
	 * <p>
	 * Returns a map where the key is the heading text and the value is the lines in that section.
	 * The empty-string key maps to content before the first sub-heading (root content).
	 *
	 * @param lines All document lines.
	 * @param sectionLevel The heading level that defines section boundaries.
	 * @return Map of section name → section lines.
	 */
	protected Map<String,List<String>> segmentBySections(List<String> lines, int sectionLevel) {
		var result = new LinkedHashMap<String,List<String>>();
		var currentSection = "";
		var currentLines = new ArrayList<String>();
		result.put("", currentLines);

		for (var line : lines) {
			var headingText = getHeadingText(line, sectionLevel);
			if (headingText != null) {
				currentSection = headingText;
				currentLines = new ArrayList<>();
				result.put(currentSection, currentLines);
			} else {
				currentLines.add(line);
			}
		}

		return result;
	}

	/**
	 * Returns the heading text if the given line is a heading at the specified level, else null.
	 *
	 * @param line The input line.
	 * @param level The heading level to check.
	 * @return The heading text or null.
	 */
	protected String getHeadingText(String line, int level) {
		var prefix = "#".repeat(level) + " ";
		var trimmed = line.trim();
		if (trimmed.startsWith(prefix) && (trimmed.length() == prefix.length() || trimmed.charAt(prefix.length()) != '#'))
			return trimmed.substring(prefix.length()).trim();
		return null;
	}

	/**
	 * Returns whether the given line is a heading at the specified level.
	 *
	 * @param line The input line.
	 * @param level The heading level.
	 * @return Whether it is a heading at that level.
	 */
	protected boolean isHeadingLine(String line, int level) {
		return getHeadingText(line, level) != null;
	}

	/**
	 * Returns whether a line is a bullet list item.
	 *
	 * @param line The input line.
	 * @return Whether it is a bullet item.
	 */
	protected boolean isBulletLine(String line) {
		var trimmed = line.trim();
		return trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ");
	}
}
