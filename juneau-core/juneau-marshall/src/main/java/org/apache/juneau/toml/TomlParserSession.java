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
package org.apache.juneau.toml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.time.temporal.*;
import java.util.*;
import java.util.Map.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.commons.bean.BeanMap;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Session for parsing TOML into POJOs.
 */
/*
 * Suppression rationale:
 * - rawtypes, unchecked: TOML maps use Map<String,Object> with dynamic values requiring casts.
 * - java:S3776: Cognitive complexity; parser logic is inherently branched for TOML syntax.
 * - java:S6541: Brain method; parser orchestration spans many concerns.
 * - java:S135: Multiple break/continue per loop needed for TOML state-machine parsing.
 * - java:S115: ARG_ctx follows project assertion-param naming convention (ARG_<param>).
 */
@SuppressWarnings({
	"rawtypes", // Raw types necessary for generic Map/List handling
	"unchecked", // Type erasure requires unchecked casts in convertValue
	"java:S3776", // Cognitive complexity acceptable for parseMessage
	"java:S6541", // Brain method acceptable for parseMessage
	"java:S135", // Multiple breaks acceptable in parse loop
	"java:S115", // ARG_ prefix follows framework convention
	"java:S1612" // Class.isAssignableFrom necessary when checking target type (Class<?>), not object instance
})
public class TomlParserSession extends ReaderParserSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for TOML parser session.
	 */
	public static class Builder extends ReaderParserSession.Builder {

		private TomlParser ctx;

		protected Builder(TomlParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public TomlParserSession build() {
			return new TomlParserSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The parser context.
	 * @return The builder.
	 */
	public static Builder create(TomlParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final TomlParser ctx;

	protected TomlParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (Reader r = pipe.getParserReader()) {
			if (r == null)
				return null;
			Map<String, Object> root = parseTomlDocument(new TomlTokenizer(r));
			if (root == null || root.isEmpty())
				return type.canCreateNewBean(getOuter()) ? type.newInstance(getOuter()) : null;
			// Root _value wrapper: document has exactly one key _value
			if (root.size() == 1 && root.containsKey("_value")) {
				Object inner = root.get("_value");
				return (T) convertValue(inner, type);
			}
			return convertMapToType(root, type);
		}
	}

	private Map<String, Object> parseTomlDocument(TomlTokenizer t) throws IOException, ParseException {
		Map<String, Object> root = new LinkedHashMap<>();
		Map<String, Object> currentTable = root;

		while (!t.isEof()) {
			t.skipWhitespaceAndComments();
			if (t.isEof())
				break;

			int c = t.peek();
			if (c == '[') {
				t.read();
				if (t.peek() == '[') {
					t.read();
					String path = parseTablePath(t);
					Object existing = getOrCreateAt(root, path);
					if (!(existing instanceof List)) {
						if (existing != null)
							throw t.parseException("Key " + path + " redefined");
						setAt(root, path, new ArrayList<>());
						existing = getOrCreateAt(root, path);
					}
					Map<String, Object> newTable = new LinkedHashMap<>();
					((List)existing).add(newTable);
					currentTable = newTable;
				} else {
					String path = parseTablePath(t);
					currentTable = getOrCreateTableAt(root, path);
				}
			} else if (c == '=' || Character.isLetterOrDigit(c) || c == '"' || c == '\'' || c == '_' || c == '-') {
				List<String> keyPath = parseKey(t);
				if (keyPath.isEmpty())
					throw t.parseException("Expected key");
				t.skipWhitespace();
				if (t.read() != '=')
					throw t.parseException("Expected '='");
				t.skipWhitespace();
				Object value = parseValue(t);
				setValueAt(currentTable, keyPath, value);
			} else {
				break;
			}
		}
		return root;
	}

	private static String parseTablePath(TomlTokenizer t) throws IOException, ParseException {
		t.skipWhitespace();
		var parts = new ArrayList<String>();
		while (true) {
			String key = t.readBareKey();
			if (key == null)
				break;
			parts.add(key);
			t.skipWhitespace();
			if (t.peek() != '.')
				break;
			t.read();
			t.skipWhitespace();
		}
		if (t.peek() == ']')
			t.read();
		if (t.peek() == ']')
			t.read();
		return String.join(".", parts);
	}

	private static List<String> parseKey(TomlTokenizer t) throws IOException, ParseException {
		var parts = new ArrayList<String>();
		while (true) {
			String key = t.readBareKey();
			if (key == null)
				break;
			parts.add(key);
			t.skipWhitespace();
			if (t.peek() != '.')
				break;
			t.read();
			t.skipWhitespace();
		}
		return parts;
	}

	private Object parseValue(TomlTokenizer t) throws IOException, ParseException {
		int c = t.peek();
		if (c == '"') {
			if (isTripleQuote(t, '"'))
				return t.readMultiLineBasicString();
			return t.readBasicString();
		}
		if (c == '\'') {
			if (isTripleQuote(t, '\''))
				return t.readMultiLineLiteralString();
			return t.readLiteralString();
		}
		if (c == 't' || c == 'f')
			return t.readBoolean();
		if (c == '[') {
			t.read();
			t.skipWhitespaceAndComments();
			var list = new ArrayList<>();
			while (t.peek() != ']') {
				list.add(parseValue(t));
				t.skipWhitespaceAndComments();
				if (t.peek() == ',') {
					t.read();
					t.skipWhitespaceAndComments();
				}
			}
			t.read();
			return list;
		}
		if (c == '{') {
			t.read();
			t.skipWhitespace();
			var map = new LinkedHashMap<String, Object>();
			while (t.peek() != '}') {
				t.skipWhitespace();
				List<String> k = parseKey(t);
				if (k.isEmpty())
					throw t.parseException("Expected key in inline table");
				t.skipWhitespace();
				if (t.read() != '=')
					throw t.parseException("Expected '='");
				t.skipWhitespace();
				Object v = parseValue(t);
				map.put(k.get(0), v);
				t.skipWhitespace();
				if (t.peek() == ',') {
					t.read();
					t.skipWhitespace();
				}
			}
			t.read();
			return map;
		}
		if (Character.isDigit(c) || c == '+' || c == '-') {
			String s = readUntilValueEnd(t);
			if (s == null || s.isEmpty())
				throw t.parseException("Expected value");
			String noUnderscore = s.replace("_", "");
			if (s.equals("inf") || s.equals("-inf") || s.equals("+inf") || s.toLowerCase().startsWith("nan"))
				return Double.parseDouble(noUnderscore);
			if (s.length() >= 10 && s.charAt(4) == '-' && (s.contains("T") || s.contains(":") || s.contains(" ")))
				return TomlTokenizer.parseDateTimeString(s);
			// Date-like patterns kept as string for convertValue to parse as LocalDate, YearMonth, Year
			if (isDateLikeString(noUnderscore))
				return s;
			if (s.contains(".") || s.toLowerCase().contains("e"))
				return Double.parseDouble(noUnderscore);
			try {
				return Long.parseLong(noUnderscore);
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				// May be date/time string that slipped through (e.g. LocalDate, LocalTime)
				if (s.contains("-") || s.contains(":"))
					return s;
				throw t.parseException("Invalid number: " + s);
			}
		}
		if (c == 'i' || c == 'n') {
			String s = readUntilValueEnd(t);
			if (s != null && (s.equalsIgnoreCase("inf") || s.equals("-inf") || s.equals("+inf") || s.regionMatches(true, 0, "nan", 0, 3)))
				return Double.parseDouble(s.replace("_", ""));
			throw t.parseException("Expected inf or nan");
		}
		throw t.parseException("Unexpected character: " + (char)c);
	}

	private static boolean isTripleQuote(TomlTokenizer t, char q) throws IOException {
		if (t.peek() != q) return false;
		int c1 = t.read();
		int c2 = t.read();
		int c3 = t.peek();
		if (c2 == q && c3 == q)
			return true;
		t.unread(c2);
		t.unread(c1);
		return false;
	}

	private static boolean isDateLikeString(String s) {
		if (s == null || s.length() < 7)
			return false;
		// yyyy-MM-dd (LocalDate) | yyyy-MM (YearMonth) - exclude plain 4-digit (e.g. port numbers)
		return (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-')
			|| (s.length() == 7 && s.charAt(4) == '-');
	}

	private static String readUntilValueEnd(TomlTokenizer t) throws IOException {
		var sb = new StringBuilder();
		int c;
		while ((c = t.peek()) >= 0 && c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '#' && c != ',' && c != ']' && c != '}') {
			sb.append((char) t.read());
		}
		return sb.isEmpty() ? null : sb.toString().trim();
	}

	private static Object getOrCreateAt(Map<String, Object> root, String path) {
		String[] parts = path.split("\\.");
		Object current = root;
		for (int i = 0; i < parts.length - 1; i++) {
			current = ((Map)current).computeIfAbsent(parts[i], k -> new LinkedHashMap<String, Object>());
		}
		return ((Map)current).get(parts[parts.length - 1]);
	}

	private static Map<String, Object> getOrCreateTableAt(Map<String, Object> root, String path) {
		String[] parts = path.split("\\.");
		Object current = root;
		for (String part : parts) {
			current = ((Map)current).computeIfAbsent(part, k -> new LinkedHashMap<String, Object>());
		}
		return (Map<String, Object>) current;
	}

	private static void setAt(Map<String, Object> root, String path, Object value) {
		String[] parts = path.split("\\.");
		Object current = root;
		for (int i = 0; i < parts.length - 1; i++) {
			current = ((Map)current).computeIfAbsent(parts[i], k -> new LinkedHashMap<String, Object>());
		}
		((Map)current).put(parts[parts.length - 1], value);
	}

	private static void setValueAt(Map<String, Object> root, List<String> keyPath, Object value) {
		if (keyPath.size() == 1) {
			root.put(keyPath.get(0), value);
			return;
		}
		Object current = root;
		for (int i = 0; i < keyPath.size() - 1; i++) {
			current = ((Map)current).computeIfAbsent(keyPath.get(i), k -> new LinkedHashMap<String, Object>());
		}
		((Map)current).put(keyPath.get(keyPath.size() - 1), value);
	}

	private <T> T convertMapToType(Map<String, Object> map, ClassMeta<T> type) throws ParseException, ExecutableException {
		if (type.isMap()) {
			var keyType = type.getKeyType();
			var valueType = type.getValueType();
			Map m = type.canCreateNewInstance(getOuter()) ? (Map) type.newInstance(getOuter()) : newGenericMap(type);
			for (var e : map.entrySet()) {
				String keyStr = "null".equals(e.getKey()) ? null : e.getKey();
				Object key = convertAttrToType(m, keyStr, keyType);
				Object val = convertValue(e.getValue(), valueType);
				m.put(key, val);
			}
			return (T) m;
		}
		BeanMap<?> bm = toBeanMap(type.newInstance(getOuter()));
		populateBeanMap(bm, map);
		return (T) bm.getBean();
	}

	private JsonMap toJsonMap(Map<?, ?> map) throws ParseException, ExecutableException {
		var jm = new JsonMap();
		for (Entry<?, ?> e : map.entrySet()) {
			Object k = e.getKey();
			Object v = e.getValue();
			Object converted = v instanceof Map m ? toJsonMap(m) : convertValue(v, object());
			jm.put(k == null ? "null" : k.toString(), converted);
		}
		return jm;
	}

	private void populateBeanMap(BeanMap<?> bm, Map<String, Object> map) throws ParseException, ExecutableException {
		for (Entry<String, Object> e : map.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			BeanPropertyMeta pMeta = bm.getMeta().getProperties().get(key);
			if (pMeta == null && isIgnoreUnknownBeanProperties())
				continue;
			if (pMeta == null)
				throw new ParseException(this, "Unknown property ''{0}''", key);
			ClassMeta<?> targetType = (ClassMeta<?>) pMeta.getBeanInfo();
			Object converted = convertValue(val, targetType);
			bm.put(key, converted);
		}
	}

	private Object convertValue(Object val, ClassMeta<?> targetType) throws ParseException, ExecutableException {
		if (val == null)
			return null;
		String nv = ctx != null ? ctx.getNullValue() : null;
		if (val instanceof String s && nv != null && s.equals(nv))
			return null;
		if (val instanceof Map map && JsonMap.class.isAssignableFrom(targetType.inner())) {
			return toJsonMap(map);
		}
		if (val instanceof Map map && targetType.isBean()) {
			BeanMap<?> child = toBeanMap(targetType.newInstance(getOuter()));
			populateBeanMap(child, map);
			return child.getBean();
		}
		if (val instanceof List list && targetType.isCollectionOrArray()) {
			ClassMeta<?> elType = targetType.getElementType();
			List result = new ArrayList();
			for (Object item : list) {
				result.add(convertValue(item, elType));
			}
			return targetType.isArray() ? toArray(targetType, result) : result;
		}
		if (val instanceof Number && targetType.isNumber())
			return convertToMemberType(null, val, targetType);
		if (val instanceof String string) {
			if (targetType.isDate())
				return parseDate(string, targetType);
			if (targetType.isCalendar())
				return parseCalendar(string, targetType);
			if (targetType.isTemporal())
				return parseTemporal(string, targetType);
			if (targetType.isDuration())
				return parseDuration(string);
			if (targetType.isPeriod())
				return parsePeriod(string);
		}
		// Bare numeric wire literals (e.g. "2024", "8100000000000") arrive here as Number values
		// from the TOML tokenizer.  Route them through the format-aware parsers so the configured
		// MarshallingContext.get<Format>() hint (NANOS, ISO_YEAR, MILLIS, …) reaches the coercion.
		// Without this routing the generic Number → T coercion below would silently drop the hint
		// (e.g. treating Long(2024) as epoch-millis → 1970-01-01T00:00:02.024Z instead of year 2024).
		if (val instanceof Number num) {
			if (targetType.isDuration())
				return parseDuration(num.toString());
			if (targetType.isPeriod())
				return parsePeriod(num.toString());
			if (targetType.isDate())
				return parseDate(num.toString(), targetType);
			if (targetType.isCalendar())
				return parseCalendar(num.toString(), targetType);
			if (targetType.isTemporal())
				return parseTemporal(num.toString(), targetType);
		}
		// Native TOML datetime literals (Z-zoned / offset / local) are returned by TomlTokenizer as
		// java.time.OffsetDateTime / LocalDateTime / LocalDate / LocalTime objects.  Re-stringify
		// through the temporal's own ISO toString() and route through parseTemporal so the
		// configured TemporalFormat hint (e.g. ISO_INSTANT) is honored — otherwise the generic
		// Temporal → T coercion below applies the local zone and we lose the wire's UTC anchor.
		if (val instanceof TemporalAccessor ta && targetType.isTemporal())
			return parseTemporal(ta.toString(), targetType);
		// Bug #12: route String-shaped byte[] values (collection-element + top-level dispatch sites)
		// through the configured BinaryFormat's variant binarySwap before falling through to the
		// default String → byte[] coercion.  The bean-property path is unaffected because the
		// per-property MPP install hides the type-level default swap behind the per-property swap.
		var binBytes = tryUnswapByteArray(val, targetType);
		if (binBytes != null)
			return binBytes;
		return convertToMemberType(null, val, targetType);
	}

	/**
	 * Routes a {@link String}-shaped {@code byte[]} value through the session-aware
	 * {@link org.apache.juneau.swaps.BinarySwap} when one is matched.
	 *
	 * <p>Collection-element and top-level dispatch sites don't go through
	 * {@code MarshalledPropertyPostProcessor}'s per-property swap install, so they fall back to the
	 * {@link org.apache.juneau.swap.DefaultSwaps} registry.  Returns {@code null} when the target isn't
	 * {@code byte[]}, the value isn't a {@link String}, or no swap is matched on the current session
	 * (e.g. {@link BinaryFormat#NOT_SET} is configured).
	 */
	private byte[] tryUnswapByteArray(Object val, ClassMeta<?> targetType) throws ParseException {
		if (!(val instanceof String s) || targetType == null || targetType.inner() != byte[].class)
			return null;
		var swap = targetType.getSwap(this);
		if (swap == null)
			return null;
		return (byte[]) unswap(swap, s, targetType);
	}
}
