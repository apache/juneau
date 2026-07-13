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
package org.apache.juneau.marshall.ini;

import static org.apache.juneau.commons.function.Suppliers.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session for parsing INI format into POJOs.
 */
@SuppressWarnings({
	"unchecked",
	"java:S115", // ARG_ctx follows project assertion-param naming convention (ARG_<param>)
	"java:S3776", "java:S6541", "java:S135",
	"resource" // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class IniParserSession extends ReaderParserSession implements RecordReadable {

	private static final String ARG_ctx = "ctx";

	/** Delimiter for nested section names (e.g. {@code address/street}). */
	private static final String SECTION_PATH_DELIMITER = "/";

	private final Memoizer<JsonParser> json5Parser = memoizer(
		() -> Json5Parser.create().marshallingContext((MarshallingContext) getContext()).build()
	);

	/**
	 * Builder for INI parser session.
	 */
	public static class Builder extends ReaderParserSession.Builder<Builder> {

		protected Builder(IniParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public IniParserSession build() {
			return new IniParserSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The parser context.
	 * @return The builder.
	 */
	public static Builder create(IniParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	protected IniParserSession(Builder builder) {
		super(builder);
	}

	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return RecordAdapter.reader(this, input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (Reader r = pipe.getParserReader()) {
			if (r == null)
				return null;
			var sections = parseIniContent(r);
			if (sections.isEmpty())
				return type.canCreateNewBean(getOuter()) ? type.newInstance(getOuter()) : null;
			if (!type.isBean() && !type.isMap())
				throw new ParseException(this, "INI format requires bean or Map<String,?> target. Got: {0}", type.inner().getName());
			if (type.isMap()) {
				var result = buildMapFromSections(sections, "");
				return (T) convertMapToTarget(result, type);
			}
			var bm = toBeanMap(type.newInstance(getOuter()));
			populateBean(bm, sections, "");
			return type.cast(bm.getBean());
		}
	}

	/**
	 * Parses INI content into section -> (key -> raw value) structure.
	 *
	 * @param r The reader.
	 * @return Map of section name to key-value map. Default section uses "".
	 */
	protected Map<String, Map<String, String>> parseIniContent(Reader r) throws IOException {
		var sections = new LinkedHashMap<String, Map<String, String>>();
		var current = sections.computeIfAbsent("", k -> new LinkedHashMap<>());
		var br = r instanceof BufferedReader r2 ? r2 : new BufferedReader(r);
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#"))
				continue;
			if (line.startsWith("[")) {
				var end = line.indexOf(']');
				if (end < 0)
					continue;
				var sectionName = line.substring(1, end).trim();
				current = sections.computeIfAbsent(sectionName, k -> new LinkedHashMap<>());
				continue;
			}
			var kv = splitKeyValue(line);
			if (kv != null) {
				var key = kv[0].trim();
				var value = kv[1].trim();
				// Strip inline comment (# ...) from value (when not inside quotes)
				value = stripInlineComment(value);
				current.put(key, value);
			}
		}
		return sections;
	}

	/**
	 * Splits an INI {@code key=value} / {@code key:value} line into its key and raw-value parts.
	 *
	 * <p>
	 * Behavior mirrors the former {@code ^([^=#\s][^=]*)\s*[=:]\s*(.*)$} regex but without its super-linear
	 * backtracking: an {@code '='} delimiter (if present) binds to the first {@code '='}; otherwise the last
	 * {@code ':'} is used. Surrounding whitespace is left in place (callers trim).
	 *
	 * @param line The (already-trimmed) candidate line.
	 * @return A two-element array of {raw-key, raw-value}, or <jk>null</jk> if the line is not a key/value pair.
	 */
	private static String[] splitKeyValue(String line) {
		if (line.isEmpty())
			return null;
		var first = line.charAt(0);
		if (first == '=' || first == '#' || Character.isWhitespace(first))
			return null;
		var idx = line.indexOf('=');
		if (idx < 0)
			idx = line.lastIndexOf(':');
		if (idx < 1)
			return null;
		return new String[] { line.substring(0, idx), line.substring(idx + 1) };
	}

	private void populateBean(BeanMap<?> bm, Map<String, Map<String, String>> sections, String sectionPath) throws ParseException, ExecutableException {
		var defaultSection = sections.get(sectionPath);
		if (defaultSection != null) {
			for (Entry<String, String> e : defaultSection.entrySet()) {
				var key = e.getKey();
				var rawValue = e.getValue();
				var pMeta = bm.getMeta().getProperties().get(key);
				if (pMeta == null && isIgnoreUnknownBeanProperties())
					continue;
				if (pMeta == null)
					throw new ParseException(this, "Unknown property ''{0}''", key);
				var value = parseValue(rawValue, (ClassMeta<?>) pMeta.getBeanInfo());
				bm.put(key, value);
			}
		}
		// Process nested sections
		for (var entry : sections.entrySet()) {
			var sectionName = entry.getKey();
			var sub = entry.getValue();
			if (sectionName.equals(sectionPath))
				continue;
			var isChild = sectionPath.isEmpty() ? !sectionName.contains(SECTION_PATH_DELIMITER)
				: sectionName.startsWith(sectionPath + SECTION_PATH_DELIMITER);
			if (!isChild)
				continue;
			var childName = sectionPath.isEmpty() ? sectionName
				: sectionName.substring(sectionPath.length() + SECTION_PATH_DELIMITER.length()).split(Pattern.quote(SECTION_PATH_DELIMITER))[0];
			var pMeta = bm.getMeta().getProperties().get(childName);
			if (pMeta == null && isIgnoreUnknownBeanProperties())
				continue;
			if (pMeta == null)
				continue;
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			var childPath = sectionPath.isEmpty() ? childName : sectionPath + SECTION_PATH_DELIMITER + childName;
			if (cMeta.isBean()) {
				var child = toBeanMap(cMeta.newInstance(getOuter()));
				populateBean(child, sections, childPath);
				bm.put(childName, child.getBean());
			} else if (cMeta.isMap() && sub != null) {
				var valueType = cMeta.getValueType();
				if (valueType == null)
					valueType = object();
				var map = new LinkedHashMap<String, Object>();
				for (Entry<String, String> e : sub.entrySet())
					map.put(e.getKey(), parseValue(e.getValue(), valueType));
				bm.put(childName, convertMapToTarget(map, cMeta));
			}
		}
	}

	private Map<String, Object> buildMapFromSections(Map<String, Map<String, String>> sections, String sectionPath) throws ParseException, ExecutableException {
		var result = new LinkedHashMap<String, Object>();
		var defaultSection = sections.get(sectionPath);
		if (defaultSection != null) {
			for (Entry<String, String> e : defaultSection.entrySet())
				result.put(e.getKey(), parseValue(e.getValue(), object()));
		}
		for (var entry : sections.entrySet()) {
			var sectionName = entry.getKey();
			var childSection = entry.getValue();
			if (sectionName.equals(sectionPath))
				continue;
			var isChild = sectionPath.isEmpty() ? !sectionName.contains(SECTION_PATH_DELIMITER)
				: sectionName.startsWith(sectionPath + SECTION_PATH_DELIMITER);
			if (!isChild)
				continue;
			var childName = sectionPath.isEmpty() ? sectionName
				: sectionName.substring(sectionPath.length() + SECTION_PATH_DELIMITER.length()).split(Pattern.quote(SECTION_PATH_DELIMITER))[0];
			if (result.containsKey(childName))
				continue;
			var childPath = sectionPath.isEmpty() ? childName : sectionPath + SECTION_PATH_DELIMITER + childName;
			var hasNested = sections.keySet().stream().anyMatch(s -> s.startsWith(childPath + SECTION_PATH_DELIMITER));
			if (hasNested) {
				result.put(childName, buildMapFromSections(sections, childPath));
			} else if (childSection != null) {
				var map = new LinkedHashMap<String, Object>();
				for (Entry<String, String> e : childSection.entrySet())
					map.put(e.getKey(), parseValue(e.getValue(), object()));
				result.put(childName, map);
			}
		}
		return result;
	}

	private Object parseValue(String raw, ClassMeta<?> targetType) throws ParseException, ExecutableException {
		if (raw == null)
			return null;
		var trimmed = raw.trim();
		if (trimmed.equals("null"))
			return null;
		if (trimmed.equalsIgnoreCase("true"))
			return true;
		if (trimmed.equalsIgnoreCase("false"))
			return false;
		if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
			var inner = trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
			return convertToMemberType(null, inner, targetType);
		}
		if (trimmed.startsWith("[") || trimmed.startsWith("{"))
			return getJson5Parser().parse(trimmed, targetType);
		if (targetType.isNumber()) {
			try {
				if (trimmed.contains(".") || trimmed.toLowerCase().contains("e"))
					return Double.parseDouble(trimmed);
				return Long.parseLong(trimmed);
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				return convertToMemberType(null, trimmed, targetType);
			}
		}
		if (targetType.isDate())
			return parseDate(trimmed, targetType);
		if (targetType.isCalendar())
			return parseCalendar(trimmed, targetType);
		if (targetType.isTemporal())
			return parseTemporal(trimmed, targetType);
		if (targetType.isDuration())
			return parseDuration(trimmed);
		if (targetType.isPeriod())
			return parsePeriod(trimmed);
		return convertToMemberType(null, trimmed, targetType);
	}

	/**
	 * Returns a Json5 parser configured with this session's {@link MarshallingContext}.
	 *
	 * <p>
	 * Required so that format hints carried on the parent {@code IniParser} context (e.g.
	 * {@link LocaleFormat}, {@link TimeZoneFormat}) are honored
	 * when nested JSON5-encoded collection/map values are deserialized.  Using {@code Json5Parser.DEFAULT}
	 * here would silently drop those hints and fall back to the format-default behavior.
	 *
	 * <p>
	 * Memoized via {@link Memoizer} — first call constructs the parser; subsequent calls return
	 * the cached instance.  {@link #parseValue} calls this once per JSON5-encoded value, so a single
	 * builder-construction + parser-instantiation is amortized across the whole INI document rather
	 * than repeated for every {@code […]} / {@code {…}} value.
	 *
	 * <p>
	 * Bug #8: list-of-{@link Locale} values written under {@link LocaleFormat#UNDERSCORE}
	 * (wire form {@code "en_US"}) were being parsed via {@code Locale.forLanguageTag("en_US")} (BCP_47 default)
	 * which returns {@link Locale#ROOT}, dropping the locale content.
	 */
	private JsonParser getJson5Parser() {
		return json5Parser.get();
	}

	private static Object convertMapToTarget(Map<String, Object> map, ClassMeta<?> type) throws ParseException, ExecutableException {
		if (JsonMap.class.isAssignableFrom(type.inner()))
			return new JsonMap(map);
		return map;
	}

	private static String stripInlineComment(String value) {
		if (value == null || !value.contains("#"))
			return value;
		var inQuote = false;
		for (var i = 0; i < value.length(); i++) {
			var c = value.charAt(i);
			if (c == '\'')
				inQuote = !inQuote;
			else if (c == '#' && !inQuote)
				return value.substring(0, i).trim();
		}
		return value;
	}
}
