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

import static org.apache.juneau.commons.utils.Utils.opt;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.parser.*;

/**
 * Session object for {@link MarkdownParser}.
 *
 * <p>
 * Parses Markdown tables and bulleted lists into Java objects.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for parser session hierarchy
	"java:S115", // Constants use UPPER_snakeCase convention
	"java:S3776", // Cognitive complexity acceptable for doParse / parseAnything
	"java:S6541", // Brain method acceptable for parseAnything
	"unchecked",
	"rawtypes",
})
public class MarkdownParserSession extends ReaderParserSession {

	private static final String CONST_type = "_type";

	final String nullValue;
	private JsonParser json5Parser;

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParserSession.Builder {

		String nullValue;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(MarkdownParser ctx) {
			super(ctx);
			nullValue = ctx.getNullValue();
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public MarkdownParserSession build() {
			return new MarkdownParserSession(this);
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
	public static Builder create(MarkdownParser ctx) {
		return new Builder(ctx);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarkdownParserSession(Builder builder) {
		super(builder);
		nullValue = builder.nullValue != null ? builder.nullValue : "*null*";
	}

	@Override /* Overridden from ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		try (var r = new java.io.BufferedReader(pipe.getReader())) {
			var lines = readAllLines(r);
			return parseAnything(lines, type, getOuter(), null);
		}
	}

	/**
	 * Main parse dispatch method.
	 *
	 * @param <T> The target type.
	 * @param lines All input lines.
	 * @param eType The expected type.
	 * @param outer The outer object (for inner class instantiation).
	 * @param pMeta The parent bean property meta (may be null).
	 * @return The parsed object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException If parsing fails.
	 */
	protected <T> T parseAnything(List<String> lines, ClassMeta<T> eType, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException {
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

		if (sType.isOptional())
			return (T) opt(parseAnything(lines, eType.getElementType(), outer, pMeta));

		// Skip blank lines and handle empty input
		var nonBlank = lines.stream().filter(l -> !l.isBlank()).toList();
		if (lines.isEmpty() || nonBlank.isEmpty())
			return sType.isString() ? (T) "" : null;

		var firstNonBlank = nonBlank.get(0).trim();

		Object o;

		if (firstNonBlank.startsWith("|")) {
			// Table input
			o = parseTable(lines, sType, outer);
		} else if (firstNonBlank.startsWith("- ") || firstNonBlank.startsWith("* ") || firstNonBlank.startsWith("+ ")) {
			// Bulleted list input
			o = parseBulletList(lines, sType, outer);
		} else {
			// Plain text — treat as simple value
			o = parseCellValue(firstNonBlank, sType, null);
		}

		if (builder != null && o != null)
			o = builder.build(this, o, eType);

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		return (T) o;
	}

	/**
	 * Parses a Markdown table (either 2-column key/value or multi-column).
	 *
	 * @param lines All input lines.
	 * @param eType The expected type.
	 * @param outer The outer object.
	 * @return The parsed object.
	 * @throws ParseException If parsing fails.
	 */
	protected Object parseTable(List<String> lines, ClassMeta<?> eType, Object outer) throws ParseException {
		// Extract table rows (lines starting with |)
		var tableLines = lines.stream().filter(l -> l.trim().startsWith("|")).toList();
		if (tableLines.isEmpty())
			return null;

		// Parse header row
		var headerLine = tableLines.get(0);
		var headers = splitTableRow(headerLine);
		if (headers.isEmpty())
			return null;

		// Skip separator row (|---|---|)
		var dataLines = tableLines.stream().skip(1).filter(l -> !isSeparatorRow(l)).toList();

		// Detect table type: if 2 columns and first header is "Property"/"Key", treat as key/value table
		var isKeyValue = headers.size() == 2
			&& (headers.get(0).equalsIgnoreCase("Property") || headers.get(0).equalsIgnoreCase("Key"))
			&& headers.get(1).equalsIgnoreCase("Value");

		if (isKeyValue)
			return parseKeyValueTable(headers, dataLines, eType, outer);
		return parseMultiColumnTable(headers, dataLines, eType, outer);
	}

	/**
	 * Parses a 2-column key/value Markdown table.
	 *
	 * @param headers The header names.
	 * @param dataLines The data row lines.
	 * @param eType The expected type.
	 * @param outer The outer object.
	 * @return The parsed bean, map, or other object.
	 * @throws ParseException If parsing fails.
	 */
	@SuppressWarnings({
		"java:S135" // Two continue statements needed: skip short rows and skip _type row
	})
	protected Object parseKeyValueTable(List<String> headers, List<String> dataLines, ClassMeta<?> eType, Object outer) throws ParseException {

		// For types with ObjectSwaps or that can't be populated via BeanMap property-setting,
		// convert the key-value table to a JSON5 object and delegate to the full JSON5 parser.
		if (needsJson5Path(eType)) {
			var json5 = keyValueTableToJson5(dataLines);
			try {
				return getJson5Parser().parseWithOuter(json5, eType, outer);
			} catch (Exception e) {
				throw new ParseException(this, e, "Could not parse key-value table as ''{0}'' via JSON5 ''{1}''.", eType, json5);
			}
		}

		if (eType.isBean()) {
			// First pass: look for _type to resolve concrete class for abstract/interface types
			ClassMeta<?> actualType = eType;
			for (var line : dataLines) {
				var cells = splitTableRow(line);
				if (cells.size() >= 2 && CONST_type.equals(cells.get(0))) {
					var typeName = cells.get(1);
					var registry = eType.getBeanRegistry();
					var resolved = registry != null ? registry.getClassMeta(typeName) : null;
					if (resolved != null)
						actualType = resolved;
					break;
				}
			}
			var m = newBeanMap(outer, actualType.inner());
			for (var line : dataLines) {
				var cells = splitTableRow(line);
				if (cells.size() < 2)
					continue;
				var key = cells.get(0);

				// Skip _type row - it was used for type resolution above
				if (CONST_type.equals(key))
					continue;

				var rawVal = cells.get(1);
				var pm = m.getPropertyMeta(key);
				if (pm != null) {
					setCurrentProperty(pm);
					var val = parseCellValue(rawVal, pm.getClassMeta(), m.getBean(false));
					try {
						setName(pm.getClassMeta(), val, key);
					} catch (Exception e) {
						throw new ParseException(this, e, "Could not set @NameProperty on ''{0}''.", key);
					}
					pm.set(m, key, val);
					setCurrentProperty(null);
				} else {
					onUnknownProperty(key, m, rawVal);
				}
			}
			return m.getBean();
		}

		if (eType.isMap()) {
			@SuppressWarnings({
				"java:S3740" // Raw Map needed for generic map construction from ClassMeta
			})
			Map map = eType.canCreateNewInstance(outer) ? (Map) eType.newInstance(outer) : new JsonMap(this);
			var keyType = eType.getKeyType() != null ? eType.getKeyType() : string();
			var valueType = eType.getValueType() != null ? eType.getValueType() : object();
			for (var line : dataLines) {
				var cells = splitTableRow(line);
				if (cells.size() < 2)
					continue;
				var rawKey = cells.get(0);
				var key = parseCellValue(rawKey, keyType, null);
				var val = parseCellValue(cells.get(1), valueType, null);
				try {
					setName(valueType, val, key);
				} catch (Exception e) {
					throw new ParseException(this, e, "Could not set @NameProperty for map key ''{0}''.", key);
				}
				map.put(key, val);
			}
			return map;
		}

		// Object or unknown: return JsonMap, check for _type and cast if possible
		var resultMap = new JsonMap(this);
		for (var line : dataLines) {
			var cells = splitTableRow(line);
			if (cells.size() < 2)
				continue;
			var key = cells.get(0);
			var val = parseCellValue(cells.get(1), object(), null);
			resultMap.put(key, val);
		}
		
		// Check for _type property and cast to typed bean if found
		if (resultMap.containsKey(getBeanTypePropertyName(eType))) {
			return cast(resultMap, null, eType);
		}
		
		return resultMap;
	}

	/**
	 * Parses a multi-column Markdown table (N columns).
	 *
	 * @param headers The column header names.
	 * @param dataLines The data row lines.
	 * @param eType The expected type.
	 * @param outer The outer object.
	 * @return The parsed list of beans, maps, or other objects.
	 * @throws ParseException If parsing fails.
	 */
	protected Object parseMultiColumnTable(List<String> headers, List<String> dataLines, ClassMeta<?> eType, Object outer) throws ParseException {
		ClassMeta<?> elementType;
		Collection<Object> result;

		if (eType.isArray()) {
			elementType = eType.getElementType();
			result = new ArrayList<>();
		} else if (eType.isCollection()) {
			elementType = eType.getElementType();
			result = eType.canCreateNewInstance(outer) ? (Collection<Object>) eType.newInstance() : new ArrayList<>();
		} else if (eType.isObject()) {
			elementType = object();
			result = new ArrayList<>();
		} else {
			// Single bean/map with multiple columns — not a standard use case, parse first row
			if (!dataLines.isEmpty()) {
				var cells = splitTableRow(dataLines.get(0));
				return parseRow(headers, cells, eType, outer);
			}
			return null;
		}

		for (var line : dataLines) {
			var cells = splitTableRow(line);
			var item = parseRow(headers, cells, elementType, result);
			result.add(item);
		}

		if (eType.isArray())
			return toArray(eType, result);

		return result;
	}

	/**
	 * Parses a single data row into the specified element type.
	 *
	 * @param headers The column header names.
	 * @param cells The cell values for this row.
	 * @param eType The element type.
	 * @param outer The outer object.
	 * @return The parsed element.
	 * @throws ParseException If parsing fails.
	 */
	protected Object parseRow(List<String> headers, List<String> cells, ClassMeta<?> eType, Object outer) throws ParseException {
		// Check if all cells are null - if so, return null for the entire row
		boolean allNull = true;
		for (var cell : cells) {
			if (cell != null && !cell.equals(nullValue) && !cell.trim().isEmpty()) {
				allNull = false;
				break;
			}
		}
		if (allNull) {
			return null;
		}

		// Check if there's a _type column and resolve the actual type using the bean registry
		ClassMeta<?> actualType = eType;
		int typeColIndex = headers.indexOf(CONST_type);
		if (typeColIndex >= 0 && typeColIndex < cells.size()) {
			var typeName = cells.get(typeColIndex);
			if (typeName != null && !typeName.isEmpty()) {
				var registry = eType != null ? eType.getBeanRegistry() : null;
				var resolved = registry != null ? registry.getClassMeta(typeName) : null;
				if (resolved != null)
					actualType = resolved;
			}
		}

		// For types with ObjectSwaps or that can't be populated via BeanMap property-setting,
		// convert the row to a JSON5 object and delegate to the full JSON5 parser.  This correctly
		// handles ObjectSwaps, @ParentProperty, JsonMap-constructor beans, etc.
		if (needsJson5Path(actualType)) {
			var json5 = rowToJson5(headers, cells);
				try {
				return getJson5Parser().parseWithOuter(json5, actualType, outer);
			} catch (Exception e) {
				throw new ParseException(this, e, "Could not parse table row as ''{0}'' via JSON5 ''{1}''.", actualType, json5);
			}
		}

		if (actualType == null || actualType.isObject()) {
			var m = new JsonMap(this);
			for (var i = 0; i < headers.size(); i++) {
				var header = headers.get(i);
				if (CONST_type.equals(header))
					continue;
				var val = i < cells.size() ? cells.get(i) : null;
				m.put(header, parseCellValue(val, object(), null));
			}
			if (typeColIndex >= 0 && m.size() > 0)
				return cast(m, null, eType);
			return m;
		}
		if (actualType.isBean()) {
			var m = newBeanMap(outer, actualType.inner());
			for (var i = 0; i < headers.size(); i++) {
				var header = headers.get(i);
				if (CONST_type.equals(header))
					continue;
				var rawVal = i < cells.size() ? cells.get(i) : null;
				var pm = m.getPropertyMeta(header);
				if (pm != null) {
					setCurrentProperty(pm);
					var val = parseCellValue(rawVal, pm.getClassMeta(), m.getBean(false));
					pm.set(m, header, val);
					setCurrentProperty(null);
				} else {
					onUnknownProperty(header, m, rawVal);
				}
			}
			return m.getBean();
		}
		if (actualType.isMap()) {
			@SuppressWarnings({
				"java:S3740" // Raw Map needed for generic map construction from ClassMeta
			})
			Map map = actualType.canCreateNewInstance(outer) ? (Map) actualType.newInstance(outer) : new JsonMap(this);
			var keyType = actualType.getKeyType() != null ? actualType.getKeyType() : string();
			var valueType = actualType.getValueType() != null ? actualType.getValueType() : object();
			for (var i = 0; i < headers.size(); i++) {
				var header = headers.get(i);
				if (CONST_type.equals(header))
					continue;
				var key = convertAttrToType(map, header, keyType);
				var rawVal = i < cells.size() ? cells.get(i) : null;
				var val = parseCellValue(rawVal, valueType, null);
				map.put(key, val);
			}
			return map;
		}
		// Simple type: use first cell
		return parseCellValue(cells.isEmpty() ? null : cells.get(0), actualType, null);
	}

	/**
	 * Parses a Markdown bulleted list into a collection or array.
	 *
	 * @param lines All input lines.
	 * @param eType The expected type.
	 * @param outer The outer object.
	 * @return The parsed collection, array, or single element.
	 * @throws ParseException If parsing fails.
	 */
	protected Object parseBulletList(List<String> lines, ClassMeta<?> eType, Object outer) throws ParseException {
		var items = new ArrayList<String>();
		for (var line : lines) {
			var trimmed = line.trim();
			if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ "))
				items.add(trimmed.substring(2));
			else if (trimmed.equals("-") || trimmed.equals("*") || trimmed.equals("+"))
				items.add("");
		}

		ClassMeta<?> elementType;
		Collection<Object> result;

		if (eType.isArray()) {
			elementType = eType.getElementType();
			result = new ArrayList<>();
		} else if (eType.isCollection()) {
			elementType = eType.getElementType();
			result = eType.canCreateNewInstance(outer) ? (Collection<Object>) eType.newInstance() : new ArrayList<>();
		} else {
			elementType = eType.isObject() ? object() : eType;
			result = new ArrayList<>();
		}

		for (var item : items)
			result.add(parseCellValue(item, elementType, null));

		if (eType.isArray())
			return toArray(eType, result);

		return result;
	}

	/**
	 * Returns a Json5Parser that shares this session's bean context (swaps, implClasses, dictionary).
	 */
	private JsonParser getJson5Parser() {
		if (json5Parser == null) {
			var b = Json5Parser.create().beanContext((BeanContext) getContext());
			if (isTrimStrings())
				b = b.trimStrings();
			json5Parser = b.build();
		}
		return json5Parser;
	}

	/**
	 * Converts a raw Markdown table cell string to the target type.
	 *
	 * <p>
	 * If the cell matches the configured null value, returns {@code null}.
	 * If the cell is backtick-wrapped JSON5, parses via {@link Json5Parser}.
	 * Otherwise, attempts numeric/boolean auto-detection when the target type is {@code Object}.
	 *
	 * @param <T> The target type.
	 * @param val The raw cell string (already unescaped).
	 * @param eType The target type.
	 * @param outer The outer object (parent bean) for @ParentProperty support.
	 * @return The parsed value.
	 * @throws ParseException If parsing fails.
	 */
	protected <T> T parseCellValue(String val, ClassMeta<T> eType, Object outer) throws ParseException {
		if (val == null)
			return null;
		val = val.trim();
		if (nullValue != null && val.equals(nullValue))
			return null;
		if (isTrimStrings())
			val = val.trim();
		// For String types, preserve empty cells as empty strings; for other types, treat as null
		if (val.isEmpty())
			return eType.isString() ? (T) "" : null;

		// Inline JSON5 wrapped in backticks — use context-aware parser with outer for full support
		// (handles ObjectSwaps, @ParentProperty, JsonMap constructors, etc.)
		if (val.startsWith("`") && val.endsWith("`") && val.length() > 1) {
			var inner = val.substring(1, val.length() - 1);
			try {
				return getJson5Parser().parseWithOuter(inner, eType, outer);
			} catch (Exception e) {
				throw new ParseException(this, e, "Could not parse inline JSON5 ''{0}'' to type ''{1}''.", inner, eType);
			}
		}

		// Unescape table cell escaping
		val = unescapeCell(val);

		if (eType.isObject()) {
			// Auto-detect type
			if ("true".equals(val)) return (T) Boolean.TRUE;
			if ("false".equals(val)) return (T) Boolean.FALSE;
			try { return (T) Integer.valueOf(val); } catch (@SuppressWarnings("unused") NumberFormatException ignored) { /* not int, try next */ }
			try { return (T) Long.valueOf(val); } catch (@SuppressWarnings("unused") NumberFormatException ignored) { /* not long, try next */ }
			try { return (T) Double.valueOf(val); } catch (@SuppressWarnings("unused") NumberFormatException ignored) { /* not double, treat as string */ }
			return (T) val;
		}

		try {
			return convertToType(val, eType);
		} catch (@SuppressWarnings("unused") Exception e) {
			throw new ParseException(this, "Could not convert Markdown cell value ''{0}'' to type ''{1}''.", val, eType);
		}
	}

	/**
	 * Returns true if the given ClassMeta requires the full JSON5 parsing machinery to be correctly
	 * instantiated — i.e., it has an ObjectSwap, cannot be created as a standard bean, or cannot be
	 * populated via direct property-setting through a BeanMap.
	 *
	 * <p>
	 * When this returns true, table rows and key-value tables should be converted to a JSON5 string
	 * and parsed via {@link Json5Parser} (using
	 * {@link org.apache.juneau.parser.Parser#parseWithOuter}) to correctly handle swaps,
	 * {@link org.apache.juneau.annotation.ParentProperty @ParentProperty}, and non-standard constructors.
	 */
	private boolean needsJson5Path(ClassMeta<?> type) {
		if (type == null || type.isObject() || type.isMap() || type.isPrimitive() || type.isNumber() || type.isString())
			return false;
		// If the type has an ObjectSwap, it cannot be parsed via BeanMap property-setting
		if (type.getSwap(this) != null)
			return true;
		// If not a bean (no public properties), can't use BeanMap approach
		return !type.isBean();
	}

	/**
	 * Converts a single Markdown table cell value to its JSON5 literal representation.
	 *
	 * <p>
	 * Inline JSON5 (backtick-wrapped) is unwrapped directly. Booleans and numbers are written bare.
	 * Everything else is single-quoted as a JSON5 string.
	 */
	private String cellToJson5(String cell) {
		if (cell == null || cell.equals(nullValue))
			return "null";
		cell = cell.trim();
		if (cell.isEmpty())
			return "''";
		// Inline JSON5 wrapped in backticks — unwrap and embed directly
		if (cell.startsWith("`") && cell.endsWith("`") && cell.length() > 1)
			return cell.substring(1, cell.length() - 1);
		// Unescape Markdown table escaping first
		cell = unescapeCell(cell);
		// Booleans and numbers don't need quoting
		if ("true".equals(cell) || "false".equals(cell))
			return cell;
		try { Integer.parseInt(cell); return cell; } catch (@SuppressWarnings("unused") NumberFormatException ignored) { /* not int, try next */ }
		try { Long.parseLong(cell); return cell; } catch (@SuppressWarnings("unused") NumberFormatException ignored) { /* not long, try next */ }
		try { Double.parseDouble(cell); return cell; } catch (@SuppressWarnings("unused") NumberFormatException ignored) { /* not double, quote as string */ }
		// String: single-quote with escaping for JSON5
		return "'" + cell.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	/**
	 * Converts a table row (headers + cells) into a JSON5 object string, suitable for parsing via
	 * {@link Json5Parser}.
	 *
	 * <p>
	 * Example: headers=["f1","f2"], cells=["a","2"] → <code>{f1:'a',f2:2}</code>
	 *
	 * <p>
	 * The {@code _type} column, if present, is included as-is (as a quoted string) so that the JSON5
	 * parser can perform type resolution via the bean dictionary.
	 */
	private String rowToJson5(List<String> headers, List<String> cells) {
		var sb = new StringBuilder("{");
		var first = true;
		for (var i = 0; i < headers.size(); i++) {
			var header = headers.get(i);
			var cell = i < cells.size() ? cells.get(i) : null;
			if (!first) sb.append(",");
			first = false;
			sb.append(header).append(":");
			if (CONST_type.equals(header)) {
				// Keep _type as a quoted string so the JSON5 parser can resolve it
				sb.append(cell == null ? "null" : "'" + cell + "'");
			} else {
				sb.append(cellToJson5(cell));
			}
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Converts a key-value table (list of [key, value] row lines) into a JSON5 object string.
	 */
	private String keyValueTableToJson5(List<String> dataLines) {
		var sb = new StringBuilder("{");
		var first = true;
		for (var line : dataLines) {
			var cells = splitTableRow(line);
			if (cells.size() < 2)
				continue;
			var key = cells.get(0);
			var val = cells.get(1);
			if (!first) sb.append(",");
			first = false;
			sb.append(key).append(":");
			if (CONST_type.equals(key)) {
				sb.append(val == null ? "null" : "'" + val + "'");
			} else {
				sb.append(cellToJson5(val));
			}
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Splits a Markdown table row into its cell values, stripping leading/trailing whitespace and pipes.
	 *
	 * @param line The raw table row line.
	 * @return The list of trimmed cell values.
	 */
	protected List<String> splitTableRow(String line) {
		var trimmed = line.trim();
		if (trimmed.startsWith("|"))
			trimmed = trimmed.substring(1);
		if (trimmed.endsWith("|") && (trimmed.length() < 2 || trimmed.charAt(trimmed.length() - 2) != '\\'))
			trimmed = trimmed.substring(0, trimmed.length() - 1);

		// Split on unescaped pipe characters
		var result = new ArrayList<String>();
		var sb = new StringBuilder();
		var i = 0;
		while (i < trimmed.length()) {
			var c = trimmed.charAt(i);
			if (c == '\\' && i + 1 < trimmed.length()) {
				var next = trimmed.charAt(i + 1);
				if (next == '|' || next == '\\') {
					sb.append('\\');
					sb.append(next);
					i += 2;
					continue;
				}
			}
			if (c == '|') {
				result.add(sb.toString().trim());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
			i++;
		}
		if (!sb.isEmpty() || !result.isEmpty())
			result.add(sb.toString().trim());
		return result;
	}

	/**
	 * Returns whether a table row is a separator row (e.g. {@code |---|---|}).
	 *
	 * @param line The table row line.
	 * @return Whether it is a separator row.
	 */
	protected boolean isSeparatorRow(String line) {
		var trimmed = line.trim();
		if (!trimmed.startsWith("|"))
			return false;
		return trimmed.replaceAll("[|\\-: ]", "").isEmpty();
	}

	/**
	 * Reads all lines from the given reader.
	 *
	 * @param r The reader.
	 * @return All lines.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected List<String> readAllLines(java.io.BufferedReader r) throws IOException {
		var lines = new ArrayList<String>();
		String line;
		while ((line = r.readLine()) != null)
			lines.add(line);
		return lines;
	}

	/**
	 * Unescapes Markdown table cell escape sequences.
	 *
	 * @param s The escaped cell value.
	 * @return The unescaped value.
	 */
	protected String unescapeCell(String s) {
		if (s == null || !s.contains("\\"))
			return s;
		var sb = new StringBuilder(s.length());
		var i = 0;
		while (i < s.length()) {
			var c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()) {
				var next = s.charAt(i + 1);
				if (next == '|' || next == '\\') {
					sb.append(next);
					i += 2;
					continue;
				}
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}
}
