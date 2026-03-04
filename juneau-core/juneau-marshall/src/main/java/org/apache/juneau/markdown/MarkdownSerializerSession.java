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

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.utils.*;

/**
 * Session object for {@link MarkdownSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"java:S115", // Constants use UPPER_snakeCase convention
	"java:S3776", // Cognitive complexity acceptable for doSerialize
	"java:S6541", // Brain method acceptable for doSerialize / serializeAnything
	"resource", // MarkdownWriter/Writer lifecycle managed by SerializerPipe
	"rawtypes",
})
public class MarkdownSerializerSession extends WriterSerializerSession {

	private static final String ARG_ctx = "ctx";

	final String nullValue;
	final boolean showHeaders;

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for serializer session builder hierarchy
	})
	public static class Builder extends WriterSerializerSession.Builder {

		String nullValue;
		boolean showHeaders;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			nullValue = ctx.getNullValue();
			showHeaders = ctx.isShowHeaders();
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public MarkdownSerializerSession build() {
			return new MarkdownSerializerSession(this);
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
	public static Builder create(MarkdownSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarkdownSerializerSession(Builder builder) {
		super(builder);
		nullValue = builder.nullValue != null ? builder.nullValue : "*null*";
		showHeaders = builder.showHeaders;
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		try (var w = getMarkdownWriter(pipe)) {
			serializeAnything(w, o, getExpectedRootType(o), null);
		}
	}

	/**
	 * Main serialization dispatch method.
	 *
	 * @param w The writer to write to.
	 * @param o The object to serialize.
	 * @param eType The expected type of the object.
	 * @param pMeta The bean property meta (may be null).
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException If serialization fails.
	 */
	protected void serializeAnything(MarkdownWriter w, Object o, ClassMeta<?> eType, BeanPropertyMeta pMeta) throws IOException, SerializeException {
		if (o == null) {
			w.text(nullValue);
			return;
		}

		ClassMeta cm = push2("root", o, eType);
		if (cm == null) {
			// Recursion detected
			pop();
			w.text(nullValue);
			return;
		}

		ObjectSwap swap = cm.getSwap(this);
		if (swap != null) {
			o = swap(swap, o);
			cm = swap.getSwapClassMeta(this);
		}

		if (cm.isOptional()) {
			var opt = (Optional<?>) o;
			if (opt.isEmpty()) {
				pop();
				w.text(nullValue);
			} else {
				serializeAnything(w, opt.get(), eType, pMeta);
				pop();
			}
			return;
		}

		if (cm.isBean()) {
			serializeBeanMap(w, toBeanMap(o), eType, cm);
		} else if (cm.isMap()) {
			serializeMap(w, (Map<?,?>) o, cm);
		} else if (cm.isCollectionOrArray()) {
			var l = cm.isArray() ? toObjectList(o) : (Collection<?>) o;
			serializeCollection(w, l, eType, cm);
		} else if (cm.isStreamable()) {
			serializeCollection(w, toListFromStreamable(o, cm), eType, cm);
		} else if (cm.isDateOrCalendarOrTemporal() || cm.isDuration()) {
			var formatted = cm.isDateOrCalendarOrTemporal()
				? Iso8601Utils.format(o, cm, getTimeZone())
				: o.toString();
			w.text(formatted);
		} else {
			var s = toString(o);
			// Only use JSON5 wrapping for actual string/enum types where auto-detection could misinterpret
			// the value; non-bean objects (e.g. those with a valueOf(String) factory) use plain text so
			// that the parser can use convertToType() on the raw string.
			if ((cm.isCharSequence() || cm.isEnum()) && isAmbiguousString(s, nullValue))
				w.text("`'" + escapeJson5String(s) + "'`");
			else
				w.text(MarkdownWriter.escapeText(s));
		}
		
		pop();
	}

	/**
	 * Serializes a bean as a 2-column key/value Markdown table.
	 *
	 * @param w The writer.
	 * @param bm The bean map.
	 * @param eType The expected type (for type name calculation).
	 * @param aType The actual type (for type name calculation).
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException If serialization fails.
	 */
	protected void serializeBeanMap(MarkdownWriter w, BeanMap<?> bm, ClassMeta<?> eType, ClassMeta<?> aType) throws IOException, SerializeException {
		if (showHeaders) {
			w.tableHeader("Property", "Value");
			w.tableSeparator(2);
		}
		
		// Add _type row if needed for polymorphic type identification
		// This handles both addBeanTypes (eType != aType) and addRootType (root object with dict name)
		String typeName = getBeanTypeName(this, eType, aType, null);
		if (typeName != null) {
			w.tableRow("_type", typeName);
		}
		
		for (var entry : bm.entrySet()) {
			if (entry instanceof BeanMapEntry bme && !bme.getMeta().canRead())
				continue;
			w.tableRow(MarkdownWriter.escapeCell(entry.getKey()), serializeInlineValue(entry.getValue()));
		}
	}

	/**
	 * Serializes a Map as a 2-column key/value Markdown table.
	 *
	 * @param w The writer.
	 * @param m The map.
	 * @param cm The class meta.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException If serialization fails.
	 */
	protected void serializeMap(MarkdownWriter w, Map<?,?> m, ClassMeta<?> cm) throws IOException, SerializeException {
		if (showHeaders) {
			w.tableHeader("Key", "Value");
			w.tableSeparator(2);
		}
		for (var entry : m.entrySet()) {
			String rawKey = entry.getKey() == null ? null : toString(entry.getKey());
			String keyStr;
			if (rawKey == null)
				keyStr = nullValue;
			else if (isAmbiguousString(rawKey, nullValue))
				keyStr = "`'" + escapeJson5String(rawKey) + "'`";
			else
				keyStr = MarkdownWriter.escapeCell(rawKey);
			w.tableRow(keyStr, serializeInlineValue(entry.getValue()));
		}
	}

	/**
	 * Serializes a collection as either a multi-column table (uniform beans/maps) or a bulleted list.
	 *
	 * @param w The writer.
	 * @param c The collection.
	 * @param eType The expected type.
	 * @param cm The class meta.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException If serialization fails.
	 */
	protected void serializeCollection(MarkdownWriter w, Collection<?> c, ClassMeta<?> eType, ClassMeta<?> cm) throws IOException, SerializeException {
		if (c.isEmpty())
			return;

		var headers = getTableHeaders(c);
		if (headers != null) {
			// Uniform collection of beans or maps → multi-column table
			
			// Determine if we need a _type column
			boolean needsTypeColumn = isAddBeanTypes();
			var elementType = eType != null && eType.isCollectionOrArray() ? eType.getElementType() : cm.getElementType();

			// Add _type column to headers if needed
			String[] finalHeaders = headers;
			if (needsTypeColumn) {
				finalHeaders = new String[headers.length + 1];
				finalHeaders[0] = "_type";
				System.arraycopy(headers, 0, finalHeaders, 1, headers.length);
			}

			if (showHeaders && finalHeaders.length > 0) {
				w.tableHeader(finalHeaders);
				w.tableSeparator(finalHeaders.length);
			}

			for (var item : c) {
				if (item == null) {
					var row = new String[finalHeaders.length];
					Arrays.fill(row, nullValue);
					w.tableRow(row);
					continue;
				}
				@SuppressWarnings({
					"java:S3740" // Raw ClassMeta from getClassMetaForObject
				})
				ClassMeta itemCm = getClassMetaForObject(item);
				@SuppressWarnings({
					"java:S3740" // Raw ObjectSwap from ClassMeta.getSwap
				})
				ObjectSwap itemSwap = itemCm.getSwap(this);
				var swapped = itemSwap != null ? swap(itemSwap, item) : item;
				@SuppressWarnings({
					"java:S3740" // Raw ClassMeta from ObjectSwap.getSwapClassMeta
				})
				ClassMeta swappedCm = itemSwap != null ? itemSwap.getSwapClassMeta(this) : itemCm;

				var row = new String[finalHeaders.length];
				int colOffset = needsTypeColumn ? 1 : 0;

				// Add type name if needed
				// For root-level collections with addRootType, treat each element as root
				if (needsTypeColumn) {
					String typeName;
					if (isRoot() && isAddRootType()) {
						// Root-level collection: add type name if the bean has a dictionary name
						typeName = swappedCm.getBeanDictionaryName();
					} else {
						// Nested collection: use standard logic
						typeName = getBeanTypeName(this, elementType, swappedCm, null);
					}
					row[0] = typeName != null ? typeName : "";
				}

				if (swappedCm.isBean()) {
					var bm = toBeanMap(swapped);
					for (var i = 0; i < headers.length; i++) {
						var val = bm.get(headers[i]);
						row[i + colOffset] = serializeInlineValue(val);
					}
				} else if (swappedCm.isMap()) {
					var map = (Map<?,?>) swapped;
					for (var i = 0; i < headers.length; i++) {
						var val = map.get(headers[i]);
						row[i + colOffset] = serializeInlineValue(val);
					}
				}
				w.tableRow(row);
			}
		} else {
			// Mixed or simple values → bulleted list
			for (var item : c)
				w.bulletItem(0, serializeInlineValue(item));
		}
	}

	/**
	 * Returns true if the string could be misinterpreted during parsing when the target type is Object,
	 * or if the string contains characters that would be lost or corrupted in Markdown table cells.
	 *
	 * <p>Strings that are empty, equal to nullValue, look like booleans or numbers, or contain control
	 * characters (< 32 or = 127) are considered ambiguous and should be wrapped in JSON5 backtick syntax.
	 */
	private static boolean isAmbiguousString(String s, String nullValue) {
		if (s.isEmpty()) return true;
		if (s.equals(nullValue)) return true;
		if ("true".equals(s) || "false".equals(s)) return true;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 32 || c == 127) return true;
		}
		try { Integer.parseInt(s); return true; } catch (NumberFormatException ignored) { /* Not a number; fall through */ }
		try { Long.parseLong(s); return true; } catch (NumberFormatException ignored) { /* Not a number; fall through */ }
		try { Double.parseDouble(s); return true; } catch (NumberFormatException ignored) { /* Not a number; fall through */ }
		return false;
	}

	/**
	 * Escapes a string for use within a JSON5 single-quoted string literal.
	 */
	private static String escapeJson5String(String s) {
		if (s == null) return "";
		var sb = new StringBuilder(s.length() + 4);
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			switch (c) {
				case '\'' -> sb.append("\\'");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Converts an array (including primitive arrays) to a List of boxed objects.
	 */
	private static List<Object> toObjectList(Object array) {
		int len = Array.getLength(array);
		var list = new ArrayList<>(len);
		for (int i = 0; i < len; i++)
			list.add(Array.get(array, i));
		return list;
	}

	/**
	 * Returns a Json5Serializer that shares this session's bean context (swaps, implClasses, dictionary).
	 * Creates a new serializer each time to ensure settings are current.
	 */
	private JsonSerializer getJson5Serializer() {
		var b = Json5Serializer.create().beanContext((BeanContext) getContext());
		if (isAddBeanTypes())
			b = b.addBeanTypes();
		if (isAddRootType())
			b = b.addRootType();
		if (isTrimStrings())
			b = b.trimStrings();
		return b.build();
	}

	/**
	 * Serializes a value for placement in a Markdown table cell.
	 *
	 * <p>
	 * Simple values (strings, numbers, booleans, enums, dates) are escaped and returned inline.
	 * Complex values (beans, maps, collections) are serialized as JSON5 in backticks.
	 *
	 * @param o The value to serialize.
	 * @return The cell string value.
	 * @throws SerializeException If serialization fails.
	 */
	@SuppressWarnings({
		"java:S3740" // Raw ClassMeta/ObjectSwap from runtime object inspection
	})
	protected String serializeInlineValue(Object o) throws SerializeException {
		if (o == null)
			return nullValue;

		ClassMeta cm = getClassMetaForObject(o);
		ObjectSwap swap = cm.getSwap(this);
		if (swap != null) {
			o = swap(swap, o);
			cm = swap.getSwapClassMeta(this);
		}

		if (cm.isNumber() || cm.isBoolean())
			return o.toString();

		if (cm.isDateOrCalendarOrTemporal())
			return MarkdownWriter.escapeCell(Iso8601Utils.format(o, cm, getTimeZone()));

		if (cm.isDuration())
			return MarkdownWriter.escapeCell(o.toString());

		if (cm.isCharSequence() || cm.isEnum()) {
			var s = toString(o);
			if (isAmbiguousString(s, nullValue))
				return "`'" + escapeJson5String(s) + "'`";
			return MarkdownWriter.escapeCell(s);
		}

		// Complex value: serialize as JSON5 in backticks — use context-aware serializer for swaps
		try {
			var json5 = getJson5Serializer().serialize(o);
			return "`" + json5 + "`";
		} catch (@SuppressWarnings("unused") SerializeException e) {
			return MarkdownWriter.escapeCell(o.toString());
		}
	}

	/**
	 * Determines if a collection can be rendered as a multi-column table.
	 *
	 * <p>
	 * Returns the column header names if the collection is a uniform set of beans or maps,
	 * or {@code null} if it should be rendered as a bulleted list.
	 *
	 * @param c The collection to inspect.
	 * @return The column header names, or {@code null} for list rendering.
	 * @throws SerializeException If inspection fails.
	 */
	@SuppressWarnings({
		"java:S1168", // null signals list rendering; empty array would incorrectly trigger table mode
		"java:S3740" // Raw ClassMeta from getClassMetaForObject on collection elements
	})
	protected String[] getTableHeaders(Collection<?> c) throws SerializeException {
		if (c.isEmpty())
			return null;

		// Find first non-null element
		Object first = null;
		for (var item : c) {
			if (item != null) {
				first = item;
				break;
			}
		}
		if (first == null)
			return null;

		ClassMeta cm = getClassMetaForObject(first);
		ObjectSwap swap = cm.getSwap(this);
		if (swap != null) {
			try {
				first = swap(swap, first);
				cm = swap.getSwapClassMeta(this);
			} catch (@SuppressWarnings("unused") SerializeException e) {
				return null;
			}
		}

		if (!cm.isMapOrBean())
			return null;

		if (cm.isBean()) {
			var bm = toBeanMap(first);
			return bm.keySet().toArray(new String[0]);
		}

		// Non-bean map: collect keys from first entry
		if (first instanceof Map<?,?> map2) {
			return map2.keySet().stream().map(Object::toString).toArray(String[]::new);
		}

		return null;
	}

	/**
	 * Gets or creates a {@link MarkdownWriter} from the given pipe.
	 *
	 * @param out The serializer pipe.
	 * @return The writer.
	 */
	protected MarkdownWriter getMarkdownWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof MarkdownWriter output2)
			return output2;
		var w = new MarkdownWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}
}
