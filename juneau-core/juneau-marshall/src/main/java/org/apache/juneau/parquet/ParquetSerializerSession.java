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
package org.apache.juneau.parquet;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.parquet.ParquetSchemaElement.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.juneau.BeanMap;
import org.apache.juneau.ClassMeta;
import org.apache.juneau.serializer.OutputStreamSerializerSession;
import org.apache.juneau.serializer.SerializeException;
import org.apache.juneau.serializer.SerializerPipe;
import org.apache.juneau.utils.Iso8601Utils;

/**
 * Session for {@link ParquetSerializer}.
 */
@SuppressWarnings({
	"resource",   // Output streams managed by calling code
	"java:S110",
	"java:S115",
	"java:S3776",
	"java:S6541", // Brain Method: Parquet bean collection and list flattening are inherently branchy
	"java:S1192"  // Duplicated "value" is ValueHolder/Optional schema key; constant would obscure
})
public class ParquetSerializerSession extends OutputStreamSerializerSession {

	private static final byte[] MAGIC = "PAR1".getBytes(StandardCharsets.UTF_8);
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for serializer sessions.
	 */
	public static class Builder extends OutputStreamSerializerSession.Builder {

		private ParquetSerializer ctx;

		protected Builder(ParquetSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public ParquetSerializerSession build() {
			return new ParquetSerializerSession(this);
		}
	}

	/**
	 * Creates a new session builder.
	 *
	 * @param ctx The serializer context.
	 * @return A new builder.
	 */
	public static Builder create(ParquetSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final ParquetSerializer ctx;

	protected ParquetSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		var out = pipe.getOutputStream();
		var rows = collectBeans(o);
		if (rows.isEmpty()) {
			writeEmptyFile(out);
			return;
		}
		var first = rows.get(0);
		var rootType = getExpectedRootType(o);
		List<ParquetSchemaElement> schema;
		if (first instanceof BeanMap<?> bm) {
			var elementType = getClassMetaForObject(bm.getBean());
			// For collection root: use element type for flat row schema (root.name, root.age).
			// Each collection element becomes one Parquet row. Avoids list column format issues.
			schema = new ParquetSchemaBuilder(ctx.getBeanContext(), ctx.writeDatesAsTimestamp, ctx.cycleHandling).buildSchema(elementType, first);
		} else if (first instanceof Map<?,?> mapFirst) {
			if (rootType.isMap() && o instanceof Map<?, ?> origMap && !origMap.isEmpty()
				&& !(origMap.keySet().iterator().next() instanceof String)) {
				schema = new ParquetSchemaBuilder(ctx.getBeanContext(), ctx.writeDatesAsTimestamp, ctx.cycleHandling)
					.buildSchemaForKeyValuePairs(mapFirst.get("key"), mapFirst.get("value"));
			} else {
				schema = new ParquetSchemaBuilder(ctx.getBeanContext(), ctx.writeDatesAsTimestamp, ctx.cycleHandling).buildSchemaFromMap((Map<?, ?>) first);
			}
		} else {
			var elementType = getClassMetaForObject(first);
			var cm = (rootType.isCollection() || rootType.isArray())
				? ctx.getBeanContext().getClassMeta(List.class, elementType.inner())
				: elementType;
			schema = new ParquetSchemaBuilder(ctx.getBeanContext(), ctx.writeDatesAsTimestamp, ctx.cycleHandling).buildSchema(cm, first);
		}
		var leafColumns = ParquetSchemaBuilder.getLeafColumns(schema);
		writeMagic(out);
		long dataStart = 4;
		var rowGroups = new ArrayList<RowGroupMeta>();
		var columnChunks = new ArrayList<ColumnChunkMeta>();
		long fileOffset = dataStart;
		for (var col : leafColumns) {
			var chunk = writeColumnChunk(out, rows, col, fileOffset);
			columnChunks.add(chunk);
			fileOffset += chunk.totalCompressedSize;
		}
		long totalByteSize = 0;
		for (var c : columnChunks)
			totalByteSize += c.totalUncompressedSize;
		long totalCompressedSize = 0;
		for (var c : columnChunks)
			totalCompressedSize += c.totalCompressedSize;
		rowGroups.add(new RowGroupMeta(columnChunks, totalByteSize, rows.size(), dataStart, totalCompressedSize));
		var footerBytes = writeFileMetaData(schema, rows.size(), rowGroups);
		out.write(footerBytes);
		writeLe4(out, footerBytes.length);
		out.write(MAGIC);
	}

	private void writeEmptyFile(OutputStream out) throws IOException {
		writeMagic(out);
		var schema = List.of(
			new ParquetSchemaElement("root", null, null, null, 0, null, null, null, null, null));
		var footerBytes = writeFileMetaData(schema, 0L, List.of());
		out.write(footerBytes);
		writeLe4(out, footerBytes.length);
		out.write(MAGIC);
	}

	private static void writeMagic(OutputStream out) throws IOException {
		out.write(MAGIC);
	}

	private static void writeLe4(OutputStream out, int v) throws IOException {
		out.write(v & 0xFF);
		out.write((v >> 8) & 0xFF);
		out.write((v >> 16) & 0xFF);
		out.write((v >> 24) & 0xFF);
	}

	private List<?> collectBeans(Object o) throws SerializeException {
		if (o == null)
			return List.of();
		// Unwrap Optional at root so we serialize the inner value, not Optional as a bean
		if (o instanceof Optional<?> opt) {
			var inner = opt.orElse(null);
			if (inner == null)
				return List.of();
			return collectBeans(inner);
		}
		var sType = getExpectedRootType(o);
		if (sType.isMap() && o instanceof Map<?, ?> m) {
			if (m.isEmpty())
				return List.of();
			var firstKey = m.keySet().iterator().next();
			if (firstKey instanceof String || firstKey == null) {
				// Apply POJO swaps to map values so types like A (with swap to JsonMap) use their swapped schema/encoding.
				// Replace null keys with the null-key sentinel so they survive as a named column in the schema.
				var swappedMap = new LinkedHashMap<>();
				for (var e : m.entrySet()) {
					var key = e.getKey() == null ? ctx.nullKeyString : e.getKey();
					var val = e.getValue();
					if (val != null)
						val = applySwap(val, getClassMetaForObject(val));
					swappedMap.put(key, val);
				}
				return List.of(swappedMap);
			}
			var result = new ArrayList<Map<String, Object>>(m.size());
			for (var e : m.entrySet()) {
				var row = new LinkedHashMap<String, Object>();
				row.put("key", e.getKey());
				row.put("value", e.getValue());
				result.add(row);
			}
			return result;
		}
		if (sType.isCollection() || sType.isArray()) {
			Collection<?> list = sType.isArray() ? toList(sType.inner(), o) : (Collection<?>)o;
			if (list.getClass().getName().startsWith("java.util.ImmutableCollections"))
				list = new ArrayList<>(list);
			var result = new ArrayList<BeanMap<?>>(list.size());
			for (var e : list) {
				if (e == null) {
					result.add(toBeanMap(createWrapperBean(null)));
					continue;
				}
				var swapped = applySwap(e, getClassMetaForObject(e));
				var swappedType = getClassMetaForObject(swapped);
				var origType = getClassMetaForObject(e);
				Object toUse;
				if (swappedType.isBean())
					toUse = swapped;
				else if (origType.isBean())
					toUse = e;
				else
					toUse = swapped;
				var useType = getClassMetaForObject(toUse);
				if (useType.isBean())
					result.add(toBeanMap(toUse));
				else
					result.add(toBeanMap(createWrapperBean(toUse)));
			}
			return result;
		}
		if (sType.isStreamable()) {
			return collectBeans(toListFromStreamable(o, sType));
		}
		var swapped = applySwap(o, sType);
		var aType = getClassMetaForObject(swapped);
		if (aType.isBean())
			return List.of(toBeanMap(swapped));
		return List.of(toBeanMap(createWrapperBean(swapped)));
	}

	/** Simple bean for wrapping non-bean values (Map is not convertible to BeanMap). */
	@SuppressWarnings("java:S1104") // Public field required for Parquet schema
	public static class ValueHolder {
		/** The wrapped value. */
		public Object value;
	}

	private static Object createWrapperBean(Object value) throws SerializeException {
		var h = new ValueHolder();
		h.value = value;
		return h;
	}

	private Object applySwap(Object value, ClassMeta<?> type) throws SerializeException {
		if (value == null || type == null)
			return value;
		var swap = type.getSwap(this);
		if (nn(swap))
			return swap(swap, value);
		return value;
	}

	private ColumnChunkMeta writeColumnChunk(OutputStream out, List<?> rows, ParquetSchemaElement col, long chunkStart) throws IOException, SerializeException {
		var path = rowRelativePath(col.path);
		if (path != null && path.contains(".list.element"))
			return writeListColumnChunk(out, rows, col, path, chunkStart);
		if (path != null && isMapKeyValueColumnPath(path)) {
			return writeMapColumnChunk(out, rows, col, path, chunkStart);
		}
		var values = extractColumnValues(rows, path);
		// Root key_value format uses root.key/root.value - convert keys to stored strings (ISO8601, etc.)
		boolean isRootKeyColumn = "root.key".equals(col.path);
		if (isRootKeyColumn) {
			var converted = new ArrayList<>(values.size());
			for (var val : values) {
				converted.add(val == null ? null : mapKeyToStoredString(unwrapOptional(val)));
			}
			values = converted;
		}
		boolean hasNulls = col.repetitionType != null && col.repetitionType == OPTIONAL;
		RleBitPackingEncoder defLevels = hasNulls ? new RleBitPackingEncoder(1) : null;
		var writer = new ParquetColumnWriter(col);
		for (var i = 0; i < values.size(); i++) {
			var v = unwrapOptional(values.get(i));
			if (defLevels != null)
				defLevels.writeInt(v == null ? 0 : 1);
			if (v != null)
				writeValue(writer, col, v);
		}
		byte[] valueBytes = writer.finalizePage();
		byte[] defLevelBytes = defLevels != null ? defLevels.toByteArrayWithLength() : new byte[0];
		byte[] pageData = new byte[defLevelBytes.length + valueBytes.length];
		System.arraycopy(defLevelBytes, 0, pageData, 0, defLevelBytes.length);
		System.arraycopy(valueBytes, 0, pageData, defLevelBytes.length, valueBytes.length);
		int uncompressedSize = pageData.length;
		byte[] compressedData = ctx.compressionCodec.compress(pageData);
		int compressedSize = compressedData.length;
		var pageHeader = ParquetColumnWriter.createPageHeader(values.size(), uncompressedSize, compressedSize);
		out.write(pageHeader);
		int headerSize = pageHeader.length;
		out.write(compressedData);
		var pathParts = col.path != null ? List.of(col.path.split("\\.")) : List.<String>of();
		return new ColumnChunkMeta(col.path, pathParts, col.type != null ? col.type : TYPE_BYTE_ARRAY, values.size(), uncompressedSize, compressedSize + (long) headerSize, chunkStart, headerSize);
	}

	/** Returns true if path is a map key_value leaf (e.g. f9.key_value.key or f9.key_value.value). */
	private static boolean isMapKeyValueColumnPath(String path) {
		if (path == null)
			return false;
		return path.endsWith(".key_value.key") || path.endsWith(".key_value.value");
	}

	/** For map key_value path like "f9.key_value.key", returns the map property name ("f9"). */
	private static String mapPropertyPath(String mapKeyValuePath) {
		if (mapKeyValuePath == null)
			return mapKeyValuePath;
		int idx = mapKeyValuePath.indexOf(".key_value.");
		return idx < 0 ? mapKeyValuePath : mapKeyValuePath.substring(0, idx);
	}

	/** Path relative to the row (bean) for extraction. Schema paths may include root prefix for list roots. */
	private static String rowRelativePath(String fullPath) {
		if (fullPath == null)
			return fullPath;
		if (fullPath.startsWith("root.list.element."))
			return fullPath.substring("root.list.element.".length());
		if (fullPath.startsWith("root."))
			return fullPath.substring("root.".length());
		return fullPath;
	}

	private static int listDepth(String path) {
		if (path == null)
			return 0;
		int depth = 0;
		for (int idx = 0; (idx = path.indexOf(".list.element", idx)) >= 0; idx += 13)
			depth++;
		return depth;
	}

	private static int maxDefLevelForListPath(String path) {
		return 3 * listDepth(path);
	}

	private static int defRepBitWidth(int maxLevel) {
		return maxLevel <= 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxLevel);
	}

	private ColumnChunkMeta writeListColumnChunk(OutputStream out, List<?> rows, ParquetSchemaElement col, String path, long chunkStart) throws IOException, SerializeException {
		var flattened = extractFlattenedListValues(rows, path);
		int maxDef = maxDefLevelForListPath(path);
		int maxRep = listDepth(path);
		int defBitWidth = defRepBitWidth(maxDef);
		int repBitWidth = maxRep > 0 ? defRepBitWidth(maxRep) : 1;
		var defLevels = new RleBitPackingEncoder(defBitWidth);
		var repLevels = maxRep > 0 ? new RleBitPackingEncoder(repBitWidth) : null;
		var writer = new ParquetColumnWriter(col);
		for (var e : flattened) {
			if (repLevels != null)
				repLevels.writeInt(e.repLevel);
			defLevels.writeInt(e.defLevel);
			var v = unwrapOptional(e.value);
			if (e.defLevel >= maxDef && v != null)
				writeValue(writer, col, v);
		}
		byte[] valueBytes = writer.finalizePage();
		byte[] defLevelBytes = defLevels.toByteArrayWithLength();
		byte[] repLevelBytes = repLevels != null ? repLevels.toByteArrayWithLength() : new byte[0];
		byte[] pageData = new byte[repLevelBytes.length + defLevelBytes.length + valueBytes.length];
		int off = 0;
		System.arraycopy(repLevelBytes, 0, pageData, off, repLevelBytes.length);
		off += repLevelBytes.length;
		System.arraycopy(defLevelBytes, 0, pageData, off, defLevelBytes.length);
		off += defLevelBytes.length;
		System.arraycopy(valueBytes, 0, pageData, off, valueBytes.length);
		int uncompressedSize = pageData.length;
		byte[] compressedData = ctx.compressionCodec.compress(pageData);
		int compressedSize = compressedData.length;
		var pageHeader = ParquetColumnWriter.createPageHeader(flattened.size(), uncompressedSize, compressedSize);
		out.write(pageHeader);
		int headerSize = pageHeader.length;
		out.write(compressedData);
		var pathParts = col.path != null ? List.of(col.path.split("\\.")) : List.<String>of();
		return new ColumnChunkMeta(col.path, pathParts, col.type != null ? col.type : TYPE_BYTE_ARRAY, flattened.size(), uncompressedSize, compressedSize + (long) headerSize, chunkStart, headerSize);
	}

	private ColumnChunkMeta writeMapColumnChunk(OutputStream out, List<?> rows, ParquetSchemaElement col, String path, long chunkStart) throws IOException, SerializeException {
		boolean isKeyColumn = path.endsWith(".key_value.key");
		var flattened = extractFlattenedMapValues(rows, path, isKeyColumn);
		int defBitWidth = defRepBitWidth(MAP_MAX_DEF);
		int repBitWidth = defRepBitWidth(MAP_MAX_REP);
		var defLevels = new RleBitPackingEncoder(defBitWidth);
		var repLevels = new RleBitPackingEncoder(repBitWidth);
		var writer = new ParquetColumnWriter(col);
		for (var e : flattened) {
			repLevels.writeInt(e.repLevel);
			defLevels.writeInt(e.defLevel);
			var v = unwrapOptional(e.value);
			if (e.defLevel >= MAP_MAX_DEF && v != null)
				writeValue(writer, col, v);
		}
		byte[] valueBytes = writer.finalizePage();
		byte[] defLevelBytes = defLevels.toByteArrayWithLength();
		byte[] repLevelBytes = repLevels.toByteArrayWithLength();
		byte[] pageData = new byte[repLevelBytes.length + defLevelBytes.length + valueBytes.length];
		int off = 0;
		System.arraycopy(repLevelBytes, 0, pageData, off, repLevelBytes.length);
		off += repLevelBytes.length;
		System.arraycopy(defLevelBytes, 0, pageData, off, defLevelBytes.length);
		off += defLevelBytes.length;
		System.arraycopy(valueBytes, 0, pageData, off, valueBytes.length);
		int uncompressedSize = pageData.length;
		byte[] compressedData = ctx.compressionCodec.compress(pageData);
		int compressedSize = compressedData.length;
		var pageHeader = ParquetColumnWriter.createPageHeader(flattened.size(), uncompressedSize, compressedSize);
		out.write(pageHeader);
		int headerSize = pageHeader.length;
		out.write(compressedData);
		var pathParts = col.path != null ? List.of(col.path.split("\\.")) : List.<String>of();
		return new ColumnChunkMeta(col.path, pathParts, col.type != null ? col.type : TYPE_BYTE_ARRAY, flattened.size(), uncompressedSize, compressedSize + (long) headerSize, chunkStart, headerSize);
	}

	private record FlattenedEntry(Object value, int defLevel, int repLevel) {}

	/** Converts a map key to the string stored in Parquet (UTF8). Handles null, Date, Enum like other formats. */
	private Object mapKeyToStoredString(Object key) throws SerializeException {
		if (key == null)
			return ctx.nullKeyString;
		if (key instanceof Date || key instanceof Calendar
			|| key instanceof TemporalAccessor) {
			var cm = getClassMetaForObject(key);
			return Iso8601Utils.format(key, cm, getTimeZone());
		}
		if (key instanceof Enum<?> e)
			return ctx.getBeanContext().isUseEnumNames() ? e.name() : e.toString();
		return String.valueOf(generalize(key, getClassMetaForObject(key)));
	}

	private List<FlattenedEntry> extractFlattenedListValues(List<?> rows, String path) throws SerializeException {
		var result = new ArrayList<FlattenedEntry>();
		int maxDef = maxDefLevelForListPath(path);
		int depth = listDepth(path);
		for (var row : rows) {
			var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
			flattenListValues(row, path, 0, true, 0, depth, maxDef, result, seen);
		}
		return result;
	}

	private static final int MAP_MAX_DEF = 2;
	private static final int MAP_MAX_REP = 1;

	private List<FlattenedEntry> extractFlattenedMapValues(List<?> rows, String path, boolean isKeyColumn) throws SerializeException {
		var result = new ArrayList<FlattenedEntry>();
		var mapProp = mapPropertyPath(path);
		boolean isRootKeyValue = "root".equals(mapProp) && path != null && path.contains(".key_value.");
		for (var row : rows) {
			Map<?, ?> map;
			if (isRootKeyValue && row instanceof Map<?, ?> rowMap && rowMap.containsKey("key") && rowMap.containsKey("value")) {
				// Rows are key-value pairs [{key:k1,value:v1}, {key:k2,value:v2}, ...]; treat as single-entry map
				map = Collections.singletonMap(rowMap.get("key"), rowMap.get("value"));
			} else {
				map = getMapAtPath(row, mapProp);
			}
			if (map.isEmpty()) {
				// Do not add any entry; parser will produce empty list per row and mergeMapColumns returns empty LinkedHashMap
			} else {
				// Write entries; skip empty maps (parser pads with empty list per row, avoids {null=null} misread)
			int idx = 0;
			for (var e : map.entrySet()) {
				int rep = idx == 0 ? 0 : MAP_MAX_REP;
				Object val = isKeyColumn ? e.getKey() : e.getValue();
				val = unwrapOptional(val);
				if (isKeyColumn)
					val = mapKeyToStoredString(val);
				int def;
				if (val != null)
					def = MAP_MAX_DEF;
				else if (isKeyColumn)
					def = MAP_MAX_DEF;
				else
					def = 1;
				result.add(new FlattenedEntry(val, def, rep));
				idx++;
			}
			}
		}
		return result;
	}

	private Map<?, ?> getMapAtPath(Object row, String mapProp) throws SerializeException {
		var obj = getValueByPath(row, mapProp);
		if (obj == null)
			return Collections.emptyMap();
		if (obj instanceof Map<?,?> m)
			return m;
		return toBeanMap(obj);
	}

	@SuppressWarnings({
		"java:S107" // Recursive list flattening requires path/index/def/rep context; refactor would obscure
	})
	private void flattenListValues(Object obj, String path, int partIndex, boolean firstInRow, int currentRep, int listDepth, int maxDef, List<FlattenedEntry> out, Set<Object> seen) throws SerializeException {
		if (obj != null && seen.contains(obj)) {
			if (ctx.cycleHandling == ParquetCycleHandling.THROW)
				throw new SerializeException("Cyclic reference at path ''{0}''. Set cycleHandling(NULL) to serialize as null.", path);
			out.add(new FlattenedEntry(null, Math.max(0, maxDef - 1), currentRep));
			return;
		}
		if (obj != null)
			seen.add(obj);
		try {
			flattenListValuesImpl(obj, path, partIndex, firstInRow, currentRep, listDepth, maxDef, out, seen);
		} finally {
			if (obj != null)
				seen.remove(obj);
		}
	}

	@SuppressWarnings({
		"java:S107" // Recursive list flattening requires path/index/def/rep context; refactor would obscure
	})
	private void flattenListValuesImpl(Object obj, String path, int partIndex, boolean firstInRow, int currentRep, int listDepth, int maxDef, List<FlattenedEntry> out, Set<Object> seen) throws SerializeException {
		var parts = path.split("\\.");
		if (partIndex >= parts.length) {
			var val = unwrapOptional(obj);
			out.add(new FlattenedEntry(val, val != null ? maxDef : Math.max(0, maxDef - 1), currentRep));
			return;
		}
		if (partIndex + 1 < parts.length && "list".equals(parts[partIndex]) && "element".equals(parts[partIndex + 1])) {
			Collection<?> list = null;
			if (obj != null) {
			if (obj instanceof Collection<?> c)
				list = c;
			else if (obj instanceof Object[] arr) {
				list = Arrays.asList(arr);
			} else if (obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive()) {
				// Primitive arrays (boolean[], int[], char[], etc.) — box each element
				int len = Array.getLength(obj);
				var al = new ArrayList<>(len);
				for (int ai = 0; ai < len; ai++)
					al.add(Array.get(obj, ai));
				list = al;
			} else
				list = Collections.singletonList(obj);
			}
		// Count how many "list" segments have appeared up to and including partIndex.
		// This gives the true nesting depth regardless of non-list path segments (e.g. "value" for Optional).
		int listDepthHere = 0;
		for (int di = 0; di <= partIndex; di++)
			if ("list".equals(parts[di]))
				listDepthHere++;
		// def levels for null/empty: 2*(depth-1) and 2*(depth-1)+1, so depth=1 → 0,1; depth=2 → 2,3.
		int nullDef  = 2 * (listDepthHere - 1);
		int emptyDef = 2 * (listDepthHere - 1) + 1;
		if (list == null) {
			out.add(new FlattenedEntry(null, nullDef, firstInRow ? 0 : currentRep));
			return;
		}
		if (list.isEmpty()) {
			out.add(new FlattenedEntry(null, emptyDef, firstInRow ? 0 : currentRep));
			return;
		}
		int level = partIndex / 2 + 1;
		// Use currentRep for the first-element rep level so nested lists carry the correct rep from their parent.
		int repLevel = firstInRow ? 0 : currentRep;
			int idx = 0;
			for (var item : list) {
				int r = idx == 0 ? repLevel : level;
				var val = unwrapOptional(item);
				int def = val == null ? Math.max(0, maxDef - 1) : maxDef;
				if (partIndex + 2 >= parts.length) {
					out.add(new FlattenedEntry(val, def, r));
				} else {
					flattenListValues(item, path, partIndex + 2, firstInRow && idx == 0, r, listDepth, maxDef, out, seen);
				}
				firstInRow = false;
				idx++;
			}
			return;
		}
		Object next = obj;
		if (next != null) {
			if (next instanceof Optional<?> opt && "value".equals(parts[partIndex]))
				next = opt.orElse(null);
			else if (next instanceof BeanMap<?> bm)
				next = bm.get(parts[partIndex]);
			else if (next instanceof Map<?,?> m)
				next = m.get(parts[partIndex]);
			else
				next = toBeanMap(next).get(parts[partIndex]);
		}
		flattenListValues(next, path, partIndex + 1, firstInRow, currentRep, listDepth, maxDef, out, seen);
	}

	private List<Object> extractColumnValues(List<?> rows, String path) throws SerializeException {
		var result = new ArrayList<>(rows.size());
		for (var row : rows)
			result.add(getValueByPath(row, path));
		return result;
	}

	private Object getValueByPath(Object row, String path) throws SerializeException {
		if (path == null || path.isEmpty())
			return row;
		var parts = path.split("\\.");
		var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
		seen.add(row);
		if (row instanceof BeanMap<?> bm) {
			var bean = bm.getBean();
			if (bean != null)
				seen.add(bean);
		}
		Object current = row;
		for (var part : parts) {
			if (current == null)
				return null;
			if (current instanceof Optional<?> opt && "value".equals(part))
				current = opt.orElse(null);
			else if (current instanceof BeanMap<?> bm)
				current = bm.get(part);
			else if (current instanceof Map<?,?> m)
				current = m.get(part);
			else
				current = toBeanMap(current).get(part);
			if (current != null && seen.contains(current))
				return handleCycle(path);
			if (current != null)
				seen.add(current);
		}
		return current;
	}

	private Object handleCycle(String path) throws SerializeException {
		if (ctx.cycleHandling == ParquetCycleHandling.THROW)
			throw new SerializeException("Cyclic reference at path ''{0}''. Set cycleHandling(NULL) to serialize as null.", path);
		return null;
	}

	/** Unwraps Optional (including nested Optional<Optional<T>>) to inner value or null. */
	private static Object unwrapOptional(Object v) {
		while (v instanceof Optional<?> opt) {
			v = opt.orElse(null);
		}
		return v;
	}

	private void writeValue(ParquetColumnWriter w, ParquetSchemaElement col, Object v) throws IOException, SerializeException {
		// Unwrap handled by callers (writeColumnChunk, writeListColumnChunk) for correct def levels
		v = unwrapOptional(v);
		Integer type = col.type;
		if (type == null)
			return;
		switch (type) {
			case TYPE_BOOLEAN:
				w.writeBoolean(v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)));
				break;
			case TYPE_INT32:
				w.writeInt32(toInt32(v));
				break;
			case TYPE_INT64:
				w.writeInt64(toInt64(v));
				break;
			case TYPE_FLOAT:
				w.writeFloat(v instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(v)));
				break;
			case TYPE_DOUBLE:
				w.writeDouble(v instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(v)));
				break;
			case TYPE_BYTE_ARRAY:
				w.writeByteArray(toByteArray(v, col));
				break;
			case TYPE_FIXED_LEN_BYTE_ARRAY:
				w.writeFixedLenByteArray(toFixedLenByteArray(v));
				break;
			default:
				w.writeByteArray(String.valueOf(v).getBytes(StandardCharsets.UTF_8));
				break;
		}
	}

	private static int toInt32(Object v) {
		if (v instanceof Number n)
			return n.intValue();
		return Integer.parseInt(String.valueOf(v));
	}

	private static long toInt64(Object v) throws SerializeException {
		if (v instanceof Date d)
			return d.getTime();
		if (v instanceof Calendar c)
			return c.getTimeInMillis();
		if (v instanceof Instant i)
			return i.toEpochMilli();
		if (v instanceof LocalDate ld)
			return ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		if (v instanceof LocalDateTime ldt)
			return ldt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
		if (v instanceof LocalTime lt)
			return Duration.ofNanos(lt.toNanoOfDay()).toMillis();
		if (v instanceof ZonedDateTime zdt)
			return zdt.toInstant().toEpochMilli();
		if (v instanceof OffsetDateTime odt)
			return odt.toInstant().toEpochMilli();
		if (v instanceof OffsetTime ot)
			return Duration.ofNanos(ot.toLocalTime().toNanoOfDay()).toMillis();
		if (v instanceof Year y)
			return y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		if (v instanceof YearMonth ym)
			return ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		if (v instanceof TemporalAccessor ta) {
			try {
				return Instant.from(ta).toEpochMilli();
			} catch (@SuppressWarnings("unused") Exception e) {
				return Long.parseLong(String.valueOf(v));
			}
		}
		if (v instanceof Number n)
			return n.longValue();
		return Long.parseLong(String.valueOf(v));
	}

	private byte[] toByteArray(Object v, ParquetSchemaElement col) throws SerializeException {
		if (v instanceof byte[] b)
			return b;
		if (v instanceof String s)
			return s.getBytes(StandardCharsets.UTF_8);
		if (v instanceof Enum<?> e)
			return (ctx.getBeanContext().isUseEnumNames() ? e.name() : e.toString()).getBytes(StandardCharsets.UTF_8);
		if (v instanceof Duration d)
			return d.toString().getBytes(StandardCharsets.UTF_8);
	// BigDecimal is stored as a UTF-8 string (same as JSON/CBOR) so fall through to String.valueOf below.
		if (col.convertedType != null && col.convertedType == CONVERTED_TIMESTAMP_MILLIS)
			return String.valueOf(toInt64(v)).getBytes(StandardCharsets.UTF_8);
		if (col.convertedType != null && (col.convertedType == CONVERTED_UTF8 || col.convertedType == CONVERTED_ENUM))
			return String.valueOf(v).getBytes(StandardCharsets.UTF_8);
		return String.valueOf(v).getBytes(StandardCharsets.UTF_8);
	}

	private static byte[] toFixedLenByteArray(Object v) {
		if (v instanceof UUID u) {
			byte[] b = new byte[16];
			var msb = u.getMostSignificantBits();
			var lsb = u.getLeastSignificantBits();
			for (int i = 0; i < 8; i++)
				b[i] = (byte)((msb >> (56 - i * 8)) & 0xFF);
			for (int i = 0; i < 8; i++)
				b[8 + i] = (byte)((lsb >> (56 - i * 8)) & 0xFF);
			return b;
		}
		return new byte[16];
	}

	private byte[] writeFileMetaData(List<ParquetSchemaElement> schema, long numRows, List<RowGroupMeta> rowGroups) throws IOException {
		return ThriftCompactEncoder.encodeToBytes(enc -> {
			enc.writeStructBegin();
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 1);
			enc.writeI32(1);
			enc.writeFieldBegin(ThriftCompactEncoder.LIST, 2);
			enc.writeListBegin(ThriftCompactEncoder.STRUCT, schema.size());
			for (var e : schema)
				e.writeTo(enc);
			enc.writeFieldBegin(ThriftCompactEncoder.I64, 3);
			enc.writeI64(numRows);
			enc.writeFieldBegin(ThriftCompactEncoder.LIST, 4);
			enc.writeListBegin(ThriftCompactEncoder.STRUCT, rowGroups.size());
			for (var rg : rowGroups) {
				enc.writeStructBegin();
				enc.writeFieldBegin(ThriftCompactEncoder.LIST, 1);
				enc.writeListBegin(ThriftCompactEncoder.STRUCT, rg.columns.size());
				for (var cc : rg.columns) {
					enc.writeStructBegin();
					enc.writeFieldBegin(ThriftCompactEncoder.I64, 2);
					enc.writeI64(cc.fileOffset);
					enc.writeFieldBegin(ThriftCompactEncoder.STRUCT, 3);
					writeColumnMetaData(enc, cc);
					enc.writeStructEnd();
				}
				enc.writeFieldBegin(ThriftCompactEncoder.I64, 2);
				enc.writeI64(rg.totalByteSize);
				enc.writeFieldBegin(ThriftCompactEncoder.I64, 3);
				enc.writeI64(rg.numRows);
				enc.writeFieldBegin(ThriftCompactEncoder.I64, 5);
				enc.writeI64(rg.fileOffset);
				enc.writeFieldBegin(ThriftCompactEncoder.I64, 6);
				enc.writeI64(rg.totalCompressedSize);
				enc.writeStructEnd();
			}
			enc.writeFieldBegin(ThriftCompactEncoder.BINARY, 6);
			enc.writeString("Apache Juneau");
			enc.writeStructEnd();
		});
	}

	private void writeColumnMetaData(ThriftCompactEncoder enc, ColumnChunkMeta cc) throws IOException {
		enc.writeStructBegin();
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 1);
		enc.writeI32(cc.parquetType);
		enc.writeFieldBegin(ThriftCompactEncoder.LIST, 2);
		enc.writeListBegin(ThriftCompactEncoder.I32, 2);
		enc.writeI32(0);
		enc.writeI32(3);
		enc.writeFieldBegin(ThriftCompactEncoder.LIST, 3);
		enc.writeListBegin(ThriftCompactEncoder.BINARY, cc.pathInSchema.size());
		for (var p : cc.pathInSchema)
			enc.writeString(p);
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 4);
		enc.writeI32(ctx.compressionCodec.thriftValue);
		enc.writeFieldBegin(ThriftCompactEncoder.I64, 5);
		enc.writeI64(cc.numValues);
		enc.writeFieldBegin(ThriftCompactEncoder.I64, 6);
		enc.writeI64(cc.totalUncompressedSize);
		enc.writeFieldBegin(ThriftCompactEncoder.I64, 7);
		enc.writeI64(cc.totalCompressedSize);
		enc.writeFieldBegin(ThriftCompactEncoder.I64, 9);
		enc.writeI64(cc.fileOffset);
		enc.writeStructEnd();
	}

	private record RowGroupMeta(List<ColumnChunkMeta> columns, long totalByteSize, long numRows, long fileOffset, long totalCompressedSize) {}
	private record ColumnChunkMeta(String path, List<String> pathInSchema, int parquetType, long numValues, long totalUncompressedSize, long totalCompressedSize, long fileOffset, int headerSize) {}
}
