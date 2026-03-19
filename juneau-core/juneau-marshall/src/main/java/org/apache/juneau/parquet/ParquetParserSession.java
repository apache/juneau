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
import static org.apache.juneau.parquet.ParquetSchemaElement.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.juneau.collections.JsonMap;

import org.apache.juneau.ClassMeta;
import org.apache.juneau.parser.InputStreamParserSession;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.parser.ParserPipe;

/**
 * Session for {@link ParquetParser}.
 */
@SuppressWarnings({
	"java:S110",
	"java:S115",
	"java:S2583", // parquetDebug() is runtime-configurable via setDebugEnabled/-Djuneau.parquet.debug
	"java:S3776",
	"java:S6541", // Brain Method: Parquet parsing/serialization flows are inherently branchy
	"java:S1192"  // Duplicated literals (.list.element, root.list.element., value) are schema keys; constants would obscure
})
public class ParquetParserSession extends InputStreamParserSession {

	private static final byte[] MAGIC = "PAR1".getBytes(StandardCharsets.UTF_8);
	private static final String ARG_ctx = "ctx";

	/** Enable via {@link #setDebugEnabled} or -Djuneau.parquet.debug=true. */
	private static boolean parquetDebug = "true".equals(System.getProperty("juneau.parquet.debug"));

	/**
	 * Enable debug output for schema/column tracing.
	 *
	 * @param enabled <jk>true</jk> to enable debug logging to temp file.
	 */
	public static void setDebugEnabled(boolean enabled) {
		parquetDebug = enabled;
	}

	private static boolean parquetDebug() {
		return parquetDebug;
	}

	private static void parquetDebugLog(String msg) {
		try {
			var f = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir", "/tmp"), "juneau-parquet-debug.log");
			java.nio.file.Files.writeString(f, "[Parquet DEBUG] " + msg + "\n",
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (@SuppressWarnings("unused") IOException ignored) {
			// ignore
		}
	}

	/**
	 * Builder for parser sessions.
	 */
	public static class Builder extends InputStreamParserSession.Builder {

		private ParquetParser ctx;

		/**
		 * Constructor.
		 *
		 * @param ctx The parser context.
		 */
		protected Builder(ParquetParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public ParquetParserSession build() {
			return new ParquetParserSession(this);
		}
	}

	/**
	 * Creates a new session builder.
	 *
	 * @param ctx The parser context.
	 * @return A new builder.
	 */
	public static Builder create(ParquetParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final ParquetParser ctx;

	protected ParquetParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override
	@SuppressWarnings({
		"unchecked" // Generic (T) casts required due to type erasure in doParse
	})
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		var bytes = readAllBytes(pipe);
		if (bytes.length < 12)
			throw new ParseException("Parquet file too small");
		if (!startsWith(bytes, MAGIC, 0) || !startsWith(bytes, MAGIC, bytes.length - 4))
			throw new ParseException("Invalid Parquet magic");
		int footerLen = readLe4(bytes, bytes.length - 8);
		int footerStart = bytes.length - 8 - footerLen;
		if (footerStart < 4)
			throw new ParseException("Invalid footer length");
		var footer = Arrays.copyOfRange(bytes, footerStart, bytes.length - 8);
		var meta = parseFileMetaData(footer);
		if (meta.numRows() < 0 || meta.numRows() > MAX_NUM_ROWS)
			throw new ParseException("Invalid numRows: {0}", meta.numRows());
		ClassMeta<?> effectiveType = type;
		if (type.isOptional())
			effectiveType = type.getElementType();
		var elementType = effectiveType.isCollection() ? effectiveType.getElementType() : effectiveType;
		if (elementType == null || elementType.isObject())
			elementType = ctx.getBeanContext().getClassMeta(Map.class);
		// For scalar types, use Map so reassembleRows keeps raw map rows for unwrap logic
		if (!effectiveType.isMap() && !effectiveType.isCollection() && !effectiveType.isArray()) {
			var inner = effectiveType.inner();
			if (inner != null && (inner.isPrimitive() || inner == String.class || inner == Boolean.class
				|| inner == Character.class || Number.class.isAssignableFrom(inner)))
				elementType = ctx.getBeanContext().getClassMeta(Map.class);
		}
		var rows = readAllRows(bytes, meta, elementType, meta.schemaRepetition());
		// Unwrap ValueHolder {value: X} when single row has "value" key and target expects scalar (not Map/List<Map>)
		boolean targetWantsScalar = !type.isMap()
			&& !(type.isCollection() && type.getElementType() != null && type.getElementType().isMap())
			&& !(type.isArray() && type.getElementType() != null && type.getElementType().isMap());
		if (rows.size() == 1 && rows.get(0) instanceof Map<?, ?> m && m.containsKey("value") && targetWantsScalar) {
			var inner = m.get("value");
			if (type.isOptional())
				return (T)(inner == null ? Optional.empty() : Optional.of(convertToType(inner, type.getElementType())));
			if (type.isArray()) {
				var list = new ArrayList<>();
				if (inner != null)
					list.add(inner);
				return (T)convertToType(list, type);
			}
			if (type.isCollection()) {
				Object coll = type.newInstance();
				if (coll == null)
					coll = new ArrayList<>();
				if (inner != null)
					((Collection<Object>)coll).add(convertToType(prepareMapForBean(inner, type.getElementType()), type.getElementType()));
				return (T)coll;
			}
			return (T)convertToType(prepareMapForBean(inner, type), type);
		}
		if (type.isMap()) {
			// Unwrap ValueHolder {value: Map} when addRootType wrapped root Map (2.2)
			if (rows.size() == 1 && rows.get(0) instanceof Map<?, ?> m && m.containsKey("value") && m.size() == 1) {
				var inner = m.get("value");
				if (inner instanceof Map<?, ?>)
					return (T)convertToType(inner, type);
			}
			var keyType = type.getKeyType();
			if (keyType != null && !keyType.isAssignableTo(String.class) && isKeyValuePairFormat(rows)) {
				var valueType = type.getValueType();
				if (valueType == null) valueType = ctx.getBeanContext().getClassMeta(Object.class);
				var result = new LinkedHashMap<>();
				for (var row : rows) {
					var rowMap = (Map<?, ?>)row;
					var rawKey = rowMap.get("key");
					var k = (rawKey != null && ctx.nullKeyString.equals(String.valueOf(rawKey)))
						? null
							: convertToType(rawKey, keyType);
					var v = convertToType(rowMap.get("value"), valueType);
					result.put(k, v);
				}
				return (T)convertToType(result, type);
			}
			if (rows.size() == 1 && rows.get(0) instanceof Map<?, ?> m) {
				// Unwrap {root: {...}} from buildSchemaFromMap flat format
				if (m.size() == 1 && m.containsKey("root") && m.get("root") instanceof Map<?, ?> inner)
					return (T)convertToType(prepareMapForBean(inner, type), type);
				return (T)convertToType(prepareMapForBean(m, type), type);
			}
		}
		if (type.isOptional()) {
			if (rows.isEmpty())
				return (T)Optional.empty();
			var inner = unwrapValueHolder(rows.get(0), type.getElementType());
			return (T)Optional.of(inner);
		}
		if (type.isArray()) {
			var componentMeta = type.getElementType();
			var unwrapped = new ArrayList<>();
			for (var row : rows) {
				var v = unwrapValueHolder(row, componentMeta);
				unwrapped.add(v);
			}
			return (T)toArray(unwrapped, componentMeta);
		}
		if (type.isCollection()) {
			var collElemType = type.getElementType();
			var unwrapped = new ArrayList<>();
			for (var row : rows) {
				var v = unwrapValueHolder(row, collElemType);
				unwrapped.add(v);
			}
			return (T)toCollection(unwrapped, type);
		}
		if (rows.size() == 1) {
			var row0 = rows.get(0);
			// Explicit unwrap for ValueHolder {value: X} when target expects scalar
			if (row0 instanceof Map<?, ?> mapRow && mapRow.containsKey("value") && targetWantsScalar) {
				var inner = mapRow.get("value");
				return (T)convertToType(prepareMapForBean(inner == null ? "" : inner, type), type);
			}
			var unwrapped = unwrapValueHolder(row0, type);
			return (T)unwrapped;
		}
		if (rows.isEmpty())
			return null;
		return (T)rows;
	}

	private static byte[] readAllBytes(ParserPipe pipe) throws IOException {
		try (var is = pipe.getInputStream()) {
			var baos = new java.io.ByteArrayOutputStream();
			var buf = new byte[8192];
			int n;
			while ((n = is.read(buf)) >= 0)
				baos.write(buf, 0, n);
			return baos.toByteArray();
		}
	}

	private static boolean startsWith(byte[] a, byte[] b, int off) {
		if (off + b.length > a.length) return false;
		for (int i = 0; i < b.length; i++)
			if (a[off + i] != b[i]) return false;
		return true;
	}

	private static int readLe4(byte[] b, int off) {
		return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
	}

	private static final long MAX_NUM_ROWS = 10_000_000;
	private static final long MAX_NUM_VALUES = 10_000_000;

	private record FileMeta(long numRows, List<RowGroupMeta> rowGroups, Map<String, Integer> schemaRepetition) {}
	private record RowGroupMeta(List<ColumnChunkMeta> columns) {}
	private record ColumnChunkMeta(int type, List<String> pathInSchema, int codec, long numValues, long dataPageOffset, long totalCompressedSize) {}

	private static FileMeta parseFileMetaData(byte[] footer) throws ParseException {
		try {
			var dec = new ThriftCompactDecoder(footer);
			dec.readStructBegin();
			long numRows = 0;
			List<RowGroupMeta> rowGroups = null;
			Map<String, Integer> schemaRepetition = Map.of();
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				switch (fh.fieldId) {
					case 1 -> dec.readI32(); // version - consumed but not used
					case 2 -> schemaRepetition = readSchema(dec);
					case 3 -> numRows = dec.readI64();
					case 4 -> rowGroups = readRowGroups(dec);
					default -> dec.skipField(fh.type);
				}
			}
			dec.readStructEnd();
			return new FileMeta(numRows, rowGroups != null ? rowGroups : List.of(), schemaRepetition);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	private record SchemaStackFrame(String name, int remaining) {}

	private static Map<String, Integer> readSchema(ThriftCompactDecoder dec) throws IOException {
		var lh = dec.readListHeader();
		var pathStack = new ArrayList<SchemaStackFrame>();
		var result = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < lh.size; i++) {
			dec.readStructBegin();
			Integer type = null;
			Integer repetitionType = null;
			String name = null;
			Integer numChildren = null;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				switch (fh.fieldId) {
					case 1 -> type = dec.readI32();
					case 2 -> dec.readI32();
					case 3 -> repetitionType = dec.readI32();
					case 4 -> name = dec.readString();
					case 5 -> numChildren = dec.readI32();
					default -> dec.skipField(fh.type);
				}
			}
			dec.readStructEnd();
			if (name == null)
				continue;
			var parts = new ArrayList<String>();
			for (var f : pathStack)
				parts.add(f.name);
			parts.add(name);
			var path = String.join(".", parts);
			if (numChildren != null && numChildren > 0) {
				pathStack.add(new SchemaStackFrame(name, numChildren));
			} else if (type != null) {
				// Leaf element: record it and decrement parent's child count
				int rep = repetitionType != null ? repetitionType : OPTIONAL;
				result.put(path, rep);
				if (parquetDebug())
					parquetDebugLog("readSchema leaf: path=" + path + " name=" + name + " rep=" + rep + " pathStack=" + pathStack);
				while (!pathStack.isEmpty()) {
					var top = pathStack.get(pathStack.size() - 1);
					var newRemaining = top.remaining - 1;
					if (newRemaining <= 0)
						pathStack.remove(pathStack.size() - 1);
					else {
						pathStack.set(pathStack.size() - 1, new SchemaStackFrame(top.name, newRemaining));
						break;
					}
				}
			}
		}
		if (parquetDebug())
			parquetDebugLog("readSchema result: " + result);
		return result;
	}

	private static void skipList(ThriftCompactDecoder dec) throws IOException {
		var lh = dec.readListHeader();
		for (int i = 0; i < lh.size; i++)
			dec.skipField(lh.elemType);
	}

	private static List<RowGroupMeta> readRowGroups(ThriftCompactDecoder dec) throws IOException {
		var lh = dec.readListHeader();
		var list = new ArrayList<RowGroupMeta>(lh.size);
		for (int i = 0; i < lh.size; i++) {
			dec.readStructBegin();
			List<ColumnChunkMeta> columns = null;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				if (fh.fieldId == 1)
					columns = readColumnChunks(dec);
				else
					dec.skipField(fh.type);
			}
			dec.readStructEnd();
			list.add(new RowGroupMeta(columns != null ? columns : List.of()));
		}
		return list;
	}

	private static List<ColumnChunkMeta> readColumnChunks(ThriftCompactDecoder dec) throws IOException {
		var lh = dec.readListHeader();
		var list = new ArrayList<ColumnChunkMeta>(lh.size);
		for (int i = 0; i < lh.size; i++) {
			dec.readStructBegin();
			ColumnChunkMeta meta = null;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				if (fh.fieldId == 2)
					dec.readI64(); // file offset - consumed but not used
				else if (fh.fieldId == 3)
					meta = readColumnMetaData(dec);
				else
					dec.skipField(fh.type);
			}
			dec.readStructEnd();
			if (meta != null)
				list.add(new ColumnChunkMeta(meta.type(), meta.pathInSchema(), meta.codec(), meta.numValues(), meta.dataPageOffset(), meta.totalCompressedSize()));
		}
		return list;
	}

	private static ColumnChunkMeta readColumnMetaData(ThriftCompactDecoder dec) throws IOException {
		dec.readStructBegin();
		int type = 6;
		List<String> pathInSchema = List.of();
		int codec = 0;
		long numValues = 0;
		long totalCompressedSize = 0;
		long dataPageOffset = 0;
		ThriftCompactDecoder.FieldHeader fh;
		while (!(fh = dec.readFieldHeader()).isStop) {
			switch (fh.fieldId) {
				case 1 -> type = dec.readI32();
				case 2 -> skipList(dec);
				case 3 -> pathInSchema = readStringList(dec);
				case 4 -> codec = dec.readI32();
				case 5 -> numValues = dec.readI64();
				case 6 -> dec.readI64();
				case 7 -> totalCompressedSize = dec.readI64();
				case 9 -> dataPageOffset = dec.readI64();
				default -> dec.skipField(fh.type);
			}
		}
		dec.readStructEnd();
		return new ColumnChunkMeta(type, pathInSchema, codec, numValues, dataPageOffset, totalCompressedSize);
	}

	private static List<String> readStringList(ThriftCompactDecoder dec) throws IOException {
		var lh = dec.readListHeader();
		var list = new ArrayList<String>(lh.size);
		for (int i = 0; i < lh.size; i++)
			list.add(dec.readString());
		return list;
	}

	private List<?> readAllRows(byte[] fileBytes, FileMeta meta, ClassMeta<?> elementType, Map<String, Integer> schemaRepetition) throws ParseException {
		if (meta.numRows() == 0)
			return List.of();
		var firstGroup = meta.rowGroups().isEmpty() ? null : meta.rowGroups().get(0);
		if (firstGroup == null)
			return List.of();
		int numRows = (int)meta.numRows();
		var columnData = new LinkedHashMap<String, List<Object>>();
		for (var cc : firstGroup.columns()) {
			var path = String.join(".", cc.pathInSchema());
			List<Object> values;
			var trim = isTrimStrings();
			if (isListColumnPath(path))
				values = readListColumnChunk(fileBytes, cc, numRows, trim);
			else if (isMapKeyValueColumnPath(path))
				values = readMapKeyValueColumnChunk(fileBytes, cc, numRows, trim);
			else
				values = readColumnChunk(fileBytes, cc, numRows, schemaRepetition, trim);
			columnData.put(path, values);
		}
		if (parquetDebug()) {
			var summary = columnData.entrySet().stream()
				.map(e -> e.getKey() + "->" + e.getValue())
				.toList();
			parquetDebugLog("readAllRows: numRows=" + numRows + " columnData=" + summary);
		}
		return reassembleRows(columnData, numRows, elementType);
	}

	private static boolean isListColumnPath(String path) {
		return path != null && (path.endsWith(".list.element") || path.contains(".list.element."));
	}

	private static boolean isMapKeyValueColumnPath(String path) {
		return path != null && (path.contains(".key_value.key") || path.contains(".key_value.value"));
	}

	private static String mapPropertyPath(String mapKeyValuePath) {
		if (mapKeyValuePath == null)
			return mapKeyValuePath;
		int idx = mapKeyValuePath.indexOf(".key_value.");
		return idx < 0 ? mapKeyValuePath : mapKeyValuePath.substring(0, idx);
	}

	/** Returns true if rows are key-value pairs (Map with non-String keys format). */
	private static boolean isKeyValuePairFormat(List<?> rows) {
		if (rows == null || rows.isEmpty())
			return false;
		var first = rows.get(0);
		if (!(first instanceof Map<?, ?> m))
			return false;
		return m.containsKey("key") && m.containsKey("value") && m.size() == 2;
	}

	/** Path relative to the row (bean). Schema paths may include root prefix for list roots. */
	private static String rowRelativePath(String fullPath) {
		if (fullPath == null)
			return fullPath;
		if (fullPath.startsWith("root.list.element."))
			return fullPath.substring("root.list.element.".length());
		if (fullPath.startsWith("root."))
			return fullPath.substring("root.".length());
		return fullPath;
	}

	/** For list column path like "tags.list.element" or "members.list.element.name", returns the bean property name ("tags" or "members"). */
	private static String listPropertyPath(String listColumnPath) {
		if (listColumnPath == null)
			return listColumnPath;
		int idx = listColumnPath.indexOf(".list.element");
		return idx < 0 ? listColumnPath : listColumnPath.substring(0, idx);
	}

	private static List<Object> readListColumnChunk(byte[] fileBytes, ColumnChunkMeta cc, int numRows, boolean trimStrings) throws ParseException {
		try {
			var path = String.join(".", cc.pathInSchema());
			var rowRelPath = rowRelativePath(path);
			int listDepth = listDepth(rowRelPath);
			int maxDef = 3 * listDepth;
			int maxRep = listDepth;
			int defBitWidth = maxDef <= 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxDef);
			int repBitWidth = maxRep <= 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxRep);

			int off = (int)cc.dataPageOffset();
			var bais = new ByteArrayInputStream(fileBytes, off, fileBytes.length - off);
			var dec = new ThriftCompactDecoder(bais);
			dec.readStructBegin();
			int uncompressedSize = 0;
			int compressedSize = 0;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				if (fh.fieldId == 2)
					uncompressedSize = dec.readI32();
				else if (fh.fieldId == 3)
					compressedSize = dec.readI32();
				else if (fh.fieldId == 5) {
					dec.readStructBegin();
					ThriftCompactDecoder.FieldHeader fh2;
					while (!(fh2 = dec.readFieldHeader()).isStop)
						dec.skipField(fh2.type);
					dec.readStructEnd();
				} else
					dec.skipField(fh.type);
			}
			dec.readStructEnd();
			int headerConsumed = (fileBytes.length - off) - bais.available();
			int pageStart = off + headerConsumed;
			int maxPageSize = 256 * 1024 * 1024;
			if (uncompressedSize < 0 || uncompressedSize > maxPageSize
				|| compressedSize < 0 || compressedSize > maxPageSize
				|| pageStart + compressedSize > fileBytes.length)
				throw new ParseException("Invalid page header: uncompressed=''{0}'', compressed=''{1}''",
					uncompressedSize, compressedSize);
			var compressedData = Arrays.copyOfRange(fileBytes, pageStart, pageStart + compressedSize);
			if (cc.numValues() < 0 || cc.numValues() > MAX_NUM_VALUES)
				throw new ParseException("Invalid numValues for column ''{0}'': {1}", String.join(".", cc.pathInSchema()), cc.numValues());
			var codec = CompressionCodec.fromThrift(cc.codec());
			var decompressed = codec.decompress(compressedData, uncompressedSize);

			int numValues = (int)cc.numValues();
			int off2 = 0;
			int repLen = 0;
			if (maxRep > 0 && decompressed.length >= 4) {
				repLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
					| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
				off2 += 4 + repLen;
			}
			int defStart = off2;
			int defLen = 0;
			if (decompressed.length - off2 >= 4) {
				defLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
					| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
				off2 += 4 + defLen;
			}
			var valueBytes = Arrays.copyOfRange(decompressed, off2, decompressed.length);

			var repBytes = maxRep > 0 && repLen > 0
				? Arrays.copyOfRange(decompressed, 4, 4 + repLen)
					: null;
			var defBytes = Arrays.copyOfRange(decompressed, defStart + 4, defStart + 4 + defLen);
			var repDecoder = repBytes != null ? new RleBitPackingDecoder(repBytes, repBitWidth) : null;
			var defDecoder = new RleBitPackingDecoder(defBytes, defBitWidth);
			var valueReader = new ParquetColumnReader(valueBytes, numValues, 0);

			var flattened = new ArrayList<Object[]>();
			for (int i = 0; i < numValues; i++) {
				int rep = repDecoder != null ? repDecoder.readInt() : 0;
				int def = defDecoder.readInt();
				Object val;
				if (def >= maxDef) {
					val = readValue(valueReader, cc.type(), trimStrings);
				} else {
					val = null;
				}
				flattened.add(new Object[] { val, rep, def });
			}

			if (listDepth == 1)
				return reconstructRowsFromListColumn(flattened, numRows, maxDef);
			return reconstructNestedListColumn(flattened, numRows, listDepth, maxDef);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Reconstructs depth-N nested list columns (depth &ge; 2), replacing the former depth-2-only method.
	 *
	 * <p>This unified algorithm supports arbitrarily deep nesting (e.g. {@code boolean[][]},
	 * {@code List<boolean[]>}, {@code List<Long>[][]}, {@code List<Long[][]>}).
	 *
	 * <h5>Rep level semantics (depth=N, maxRep=N):</h5>
	 * <ul>
	 *   <li>rep=0 — new row; flush the entire stack and start fresh
	 *   <li>rep=k (0 &lt; k &lt; N) — new element at nesting level k; flush inner levels k+1..N
	 *   <li>rep=N — continue the innermost list (append another element)
	 * </ul>
	 *
	 * <h5>Def level semantics (maxDef = 3*N):</h5>
	 * <ul>
	 *   <li>def=2*(k-1) — null list at level k (1 &le; k &le; N)
	 *   <li>def=2*(k-1)+1 — empty list at level k (1 &le; k &le; N)
	 *   <li>def=maxDef-1 — leaf element is null
	 *   <li>def=maxDef — leaf element is present
	 * </ul>
	 *
	 * <p>The "stacks" array tracks the currently open list at each nesting level.
	 * {@code stacks[0]} is the outermost list (what will be placed in the row),
	 * {@code stacks[N-1]} is the innermost list being appended to.
	 */
	@SuppressWarnings({"unchecked"}) // Generic array creation for stacks
	private static List<Object> reconstructNestedListColumn(List<Object[]> flattened, int numRows, int depth, int maxDef) {
		var result = new ArrayList<>(numRows);
		var stacks = new ArrayList[depth];

		for (var e : flattened) {
			Object val = e[0];
			int rep = (Integer)e[1];
			int def = (Integer)e[2];

			// Flush: close inner lists from the deepest level down to `rep`, adding each to its parent.
			// For rep=0 this flushes everything including stacks[0] into result.
			for (int k = depth - 1; k >= rep; k--) {
				if (stacks[k] != null) {
					if (k > 0)
						stacks[k - 1].add(stacks[k]);
					else
						result.add(stacks[k]);
					stacks[k] = null;
				}
			}

			// Null at level 1 is the only case where def==0 AND rep==0; it was flushed above.
			if (rep == 0 && def == 0) {
				result.add(null);   // whole field is null
				continue;
			}

			if (def <= 2 * (depth - 1) + 1) {
				// Null or empty list at nesting level k (1-indexed): def=2*(k-1) → null; def=2*(k-1)+1 → empty.
				int level = def / 2 + 1;
				boolean isNull = (def % 2 == 0);
				// Ensure stacks[start..level-2] exist (parents of the null/empty list).
				int startK = (rep == 0) ? 0 : rep;
				for (int k = startK; k < level - 1; k++)
					if (stacks[k] == null) stacks[k] = new ArrayList<>();
				if (level == 1) {
					// Level-1 null is handled above (def==0); level-1 empty reaches here (def==1).
					result.add(new ArrayList<>());
				} else if (isNull) {
					stacks[level - 2].add(null);   // null list at level k → parent gets null slot
				} else {
					stacks[level - 2].add(new ArrayList<>());  // empty list at level k
				}
			} else {
				// Element (null if def < maxDef, present if def == maxDef).
				if (rep < depth) {
					// Initialize stacks from the first missing level down to the innermost.
					int startK = (rep == 0) ? 0 : rep;
					for (int k = startK; k < depth; k++)
						if (stacks[k] == null) stacks[k] = new ArrayList<>();
				} else {
					// rep == depth: continue innermost list (defensive init in case it's missing).
					if (stacks[depth - 1] == null) stacks[depth - 1] = new ArrayList<>();
				}
				stacks[depth - 1].add(def >= maxDef ? val : null);
			}
		}

		// Flush the final row's stacks.
		for (int k = depth - 1; k >= 0; k--) {
			if (stacks[k] != null) {
				if (k > 0)
					stacks[k - 1].add(stacks[k]);
				else
					result.add(stacks[k]);
				stacks[k] = null;
			}
		}
		while (result.size() < numRows)
			result.add(new ArrayList<>());
		return result;
	}

	private static int listDepth(String path) {
		if (path == null)
			return 0;
		int depth = 0;
		for (int idx = 0; (idx = path.indexOf(".list.element", idx)) >= 0; idx += 13)
			depth++;
		return depth;
	}

	private static final int MAP_MAX_DEF = 2;
	private static final int MAP_MAX_REP = 1;

	private static List<Object> readMapKeyValueColumnChunk(byte[] fileBytes, ColumnChunkMeta cc, int numRows, boolean trimStrings) throws ParseException {
		try {
			int maxDef = MAP_MAX_DEF;
			int maxRep = MAP_MAX_REP;
			int defBitWidth = maxDef <= 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxDef);
			int repBitWidth = maxRep <= 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxRep);

			int off = (int)cc.dataPageOffset();
			var bais = new ByteArrayInputStream(fileBytes, off, fileBytes.length - off);
			var dec = new ThriftCompactDecoder(bais);
			dec.readStructBegin();
			int uncompressedSize = 0;
			int compressedSize = 0;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				if (fh.fieldId == 2)
					uncompressedSize = dec.readI32();
				else if (fh.fieldId == 3)
					compressedSize = dec.readI32();
				else if (fh.fieldId == 5) {
					dec.readStructBegin();
					ThriftCompactDecoder.FieldHeader fh2;
					while (!(fh2 = dec.readFieldHeader()).isStop)
						dec.skipField(fh2.type);
					dec.readStructEnd();
				} else
					dec.skipField(fh.type);
			}
			dec.readStructEnd();
			int headerConsumed = (fileBytes.length - off) - bais.available();
			int pageStart = off + headerConsumed;
			int maxPageSize = 256 * 1024 * 1024;
			if (uncompressedSize < 0 || uncompressedSize > maxPageSize
				|| compressedSize < 0 || compressedSize > maxPageSize
				|| pageStart + compressedSize > fileBytes.length)
				throw new ParseException("Invalid page header: uncompressed=''{0}'', compressed=''{1}''",
					uncompressedSize, compressedSize);
			var compressedData = Arrays.copyOfRange(fileBytes, pageStart, pageStart + compressedSize);
			if (cc.numValues() < 0 || cc.numValues() > MAX_NUM_VALUES)
				throw new ParseException("Invalid numValues for column ''{0}'': {1}", String.join(".", cc.pathInSchema()), cc.numValues());
			var codec = CompressionCodec.fromThrift(cc.codec());
			var decompressed = codec.decompress(compressedData, uncompressedSize);

			int numValues = (int)cc.numValues();
			int off2 = 0;
			int repLen = 0;
			if (maxRep > 0 && decompressed.length >= 4) {
				repLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
					| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
				off2 += 4 + repLen;
			}
			int defStart = off2;
			int defLen = 0;
			if (decompressed.length - off2 >= 4) {
				defLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
					| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
				off2 += 4 + defLen;
			}
			var repBytes = maxRep > 0 && repLen > 0
				? Arrays.copyOfRange(decompressed, 4, 4 + repLen)
					: null;
			var defBytes = Arrays.copyOfRange(decompressed, defStart + 4, defStart + 4 + defLen);
			var repDecoder = repBytes != null ? new RleBitPackingDecoder(repBytes, repBitWidth) : null;
			var defDecoder = new RleBitPackingDecoder(defBytes, defBitWidth);
			var valueBytes = Arrays.copyOfRange(decompressed, off2, decompressed.length);
			var valueReader = new ParquetColumnReader(valueBytes, numValues, 0);

			var flattened = new ArrayList<Object[]>();
			for (int i = 0; i < numValues; i++) {
				int rep = repDecoder != null ? repDecoder.readInt() : 0;
				int def = defDecoder.readInt();
				Object val;
				if (def >= maxDef) {
					val = readValue(valueReader, cc.type(), trimStrings);
				} else {
					val = null;
				}
				flattened.add(new Object[] { val, rep, def });
			}

			return reconstructRowsFromListColumn(flattened, numRows, maxDef);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	private static List<Object> reconstructRowsFromListColumn(List<Object[]> flattened, int numRows, int maxDef) {
		var result = new ArrayList<>(numRows);
		var current = new ArrayList<>();
		// def=0: list null; def=1: list present empty; def>=maxDef-1: element slot (value can be null)
		int elemPresentDef = Math.max(0, maxDef - 1);
		for (var e : flattened) {
			Object val = e[0];
			int rep = (Integer)e[1];
			int def = (Integer)e[2];
			if (rep == 0) {
				if (!current.isEmpty()) {
					result.add(new ArrayList<>(current));
					current.clear();
				}
				if (def == 0)
					result.add(null);
				else if (def < elemPresentDef)
					result.add(new ArrayList<>());
				else if (def >= elemPresentDef)
					current.add(val);
			} else if (def >= elemPresentDef) {
				current.add(val);
			}
		}
		if (!current.isEmpty())
			result.add(current);
		while (result.size() < numRows)
			result.add(List.of());
		return result;
	}

	private static List<Object> readColumnChunk(byte[] fileBytes, ColumnChunkMeta cc, int numRows, Map<String, Integer> schemaRepetition, boolean trimStrings) throws ParseException {
		try {
			int off = (int)cc.dataPageOffset();
			var bais = new ByteArrayInputStream(fileBytes, off, fileBytes.length - off);
			var dec = new ThriftCompactDecoder(bais);
			dec.readStructBegin();
			int uncompressedSize = 0;
			int compressedSize = 0;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				if (fh.fieldId == 2)
					uncompressedSize = dec.readI32();
				else if (fh.fieldId == 3)
					compressedSize = dec.readI32();
				else if (fh.fieldId == 5) {
					dec.readStructBegin();
					ThriftCompactDecoder.FieldHeader fh2;
					while (!(fh2 = dec.readFieldHeader()).isStop)
						dec.skipField(fh2.type);
					dec.readStructEnd();
				} else
					dec.skipField(fh.type);
			}
			dec.readStructEnd();
			int headerConsumed = (fileBytes.length - off) - bais.available();
			int pageStart = off + headerConsumed;
			int maxPageSize = 256 * 1024 * 1024;
			if (uncompressedSize < 0 || uncompressedSize > maxPageSize
				|| compressedSize < 0 || compressedSize > maxPageSize
				|| pageStart + compressedSize > fileBytes.length)
				throw new ParseException("Invalid page header: uncompressed=''{0}'', compressed=''{1}''",
					uncompressedSize, compressedSize);
			var compressedData = Arrays.copyOfRange(fileBytes, pageStart, pageStart + compressedSize);
			if (cc.numValues() < 0 || cc.numValues() > MAX_NUM_VALUES)
				throw new ParseException("Invalid numValues for column ''{0}'': {1}", String.join(".", cc.pathInSchema()), cc.numValues());
			var codec = CompressionCodec.fromThrift(cc.codec());
			var decompressed = codec.decompress(compressedData, uncompressedSize);
			var path = String.join(".", cc.pathInSchema());
			int rep = schemaRepetition.getOrDefault(path, OPTIONAL);
			boolean isUnderListRoot = path != null && path.startsWith("root.list.element.");
			boolean isPrimitiveType = cc.type() == TYPE_INT32 || cc.type() == TYPE_INT64 || cc.type() == TYPE_FLOAT || cc.type() == TYPE_DOUBLE || cc.type() == TYPE_BOOLEAN;
			int maxDefLevel = (rep == REQUIRED || (isUnderListRoot && isPrimitiveType)) ? 0 : 1;
			if (parquetDebug())
				parquetDebugLog("readColumnChunk: path=" + path + " pathInSchema=" + cc.pathInSchema()
				+ " rep=" + rep + " (REQ=" + REQUIRED + ") maxDefLevel=" + maxDefLevel + " type=" + cc.type() + " isPrimitive=" + isPrimitiveType);
			int valuesToRead = (int)Math.min(cc.numValues(), numRows);
			var reader = new ParquetColumnReader(decompressed, valuesToRead, maxDefLevel);
			var values = new ArrayList<>();
			while (reader.hasNext()) {
				values.add(readValue(reader, cc.type(), trimStrings));
			}
			if (parquetDebug())
				parquetDebugLog("readColumnChunk values: path=" + path + " values=" + values);
			return values;
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	private static Object readValue(ParquetColumnReader reader, int type, boolean trimStrings) throws IOException {
		reader.advance();
		if (reader.isNull())
			return null;
		return switch (type) {
			case TYPE_BOOLEAN -> reader.readBoolean();
			case TYPE_INT32 -> reader.readInt32();
			case TYPE_INT64 -> reader.readInt64();
			case TYPE_FLOAT -> reader.readFloat();
			case TYPE_DOUBLE -> reader.readDouble();
			case TYPE_BYTE_ARRAY -> {
				var s = reader.readByteArrayAsString();
				yield trimStrings && s != null ? s.trim() : s;
			}
			case TYPE_FIXED_LEN_BYTE_ARRAY -> reader.readFixedLenByteArray(16);
			default -> {
				var s = reader.readByteArrayAsString();
				yield trimStrings && s != null ? s.trim() : s;
			}
		};
	}

	private List<Object> reassembleRows(Map<String, List<Object>> columnData, int numRows, ClassMeta<?> elementType) throws ParseException {
		var result = new ArrayList<>(numRows);
		var listBeanColumns = groupListBeanColumns(columnData.keySet(), elementType);
		var mapColumns = groupMapColumns(columnData.keySet());
		for (int i = 0; i < numRows; i++) {
			var row = new JsonMap();
			for (var e : columnData.entrySet()) {
				var fullPath = e.getKey();
				var rowRelPath = rowRelativePath(fullPath);
				var listProp = listPropertyPath(rowRelPath);
				if (listBeanColumns.containsKey(listProp) || isMapKeyValueColumnPath(rowRelPath))
					continue;
				var values = e.getValue();
				var v = i < values.size() ? values.get(i) : null;
				var path = isListColumnPath(fullPath) ? listProp : rowRelPath;
				if (isTrimStrings())
					path = path.trim();
				setByPath(row, path, v);
			}
			for (var e : listBeanColumns.entrySet()) {
				var listProp = e.getKey();
				var columnsInGroup = e.getValue();
				var mergedList = mergeListBeanColumns(columnsInGroup, columnData, i);
				setByPath(row, listProp, mergedList);
			}
			for (var e : mapColumns.entrySet()) {
				var mapProp = e.getKey();
				var columnsInGroup = e.getValue();
				var mergedMap = mergeMapColumns(columnsInGroup, columnData, i);
				setByPath(row, mapProp, mergedMap);
			}
			replaceNullKeySentinel(row);
			if (elementType.isBean())
				collapseOptionalWrappers(row, elementType);
			Object rowOrBean = elementType.isBean() ? convertToType(prepareMapForBean(row, elementType), elementType) : row;
			result.add(rowOrBean);
		}
		return result;
	}

	private static Map<String, List<ListColumnInfo>> groupListBeanColumns(Set<String> columnPaths, ClassMeta<?> elementType) {
		var groups = new LinkedHashMap<String, List<ListColumnInfo>>();
		if (!elementType.isBean())
			return groups;
		var bm = elementType.getBeanMeta();
		if (bm == null)
			return groups;
		for (var fullPath : columnPaths) {
			if (!isListColumnPath(fullPath))
				continue;
			var rowRelPath = rowRelativePath(fullPath);
			var listProp = listPropertyPath(rowRelPath);
			var suffix = ".list.element.";
			var suffixStart = listProp.length() + suffix.length();
			var pMeta = rowRelPath.length() > suffixStart ? bm.getPropertyMeta(listProp) : null;
			var propClassMeta = pMeta != null ? pMeta.getClassMeta() : null;
			var elemType = propClassMeta != null && (propClassMeta.isCollection() || propClassMeta.isArray()) ? propClassMeta.getElementType() : null;
			if (elemType != null && elemType.isBean()) {
				var elementProp = rowRelPath.substring(suffixStart);
				groups.computeIfAbsent(listProp, k -> new ArrayList<>()).add(new ListColumnInfo(fullPath, elementProp, elemType));
			}
		}
		return groups;
	}

	private record ListColumnInfo(String fullPath, String elementProp, ClassMeta<?> elementType) {}

	private record MapColumnInfo(String keyColumnPath, String valueColumnPath) {}

	private static Map<String, MapColumnInfo> groupMapColumns(Set<String> columnPaths) {
		var groups = new LinkedHashMap<String, MapColumnInfo>();
		for (var fullPath : columnPaths) {
			var rowRelPath = rowRelativePath(fullPath);
			if (!isMapKeyValueColumnPath(rowRelPath))
				continue;
			var prop = mapPropertyPath(rowRelPath);
			var existing = groups.get(prop);
			if (rowRelPath.endsWith(".key_value.key"))
				groups.put(prop, new MapColumnInfo(fullPath, existing != null ? existing.valueColumnPath() : null));
			else if (rowRelPath.endsWith(".key_value.value"))
				groups.put(prop, new MapColumnInfo(existing != null ? existing.keyColumnPath() : null, fullPath));
		}
		return groups;
	}

	private Map<Object, Object> mergeMapColumns(MapColumnInfo info, Map<String, List<Object>> columnData, int rowIndex) throws ParseException {
		var keyValues = columnData.get(info.keyColumnPath());
		var valueValues = columnData.get(info.valueColumnPath());
		if (keyValues == null || rowIndex >= keyValues.size())
			return new LinkedHashMap<>();
		var keyRow = keyValues.get(rowIndex);
		if (keyRow == null)
			return new LinkedHashMap<>();
		if (!(keyRow instanceof List<?> keyList))
			return new LinkedHashMap<>();
		if (keyList.isEmpty())
			return new LinkedHashMap<>();
		var valueList = valueValues != null && rowIndex < valueValues.size() && valueValues.get(rowIndex) instanceof List<?> vl
			? vl
				: Collections.emptyList();
		var result = new LinkedHashMap<>();
		var valueIter = valueList.iterator();
		for (var k : keyList) {
			var v = valueIter.hasNext() ? valueIter.next() : null;
			Object key = (k != null && ctx.nullKeyString.equals(String.valueOf(k))) ? null : k;
			result.put(key, v);
		}
		return result;
	}

	private List<Object> mergeListBeanColumns(List<ListColumnInfo> columnsInGroup, Map<String, List<Object>> columnData,
		int rowIndex) throws ParseException {
		if (columnsInGroup.isEmpty())
			return List.of();
		var elemType = columnsInGroup.get(0).elementType();
		var firstColValues = columnData.get(columnsInGroup.get(0).fullPath());
		if (firstColValues == null || rowIndex >= firstColValues.size())
			return List.of();
		var firstRowList = firstColValues.get(rowIndex);
		if (!(firstRowList instanceof List<?> firstList))
			return List.of();
		int listSize = firstList.size();
		var result = new ArrayList<>(listSize);
		for (int j = 0; j < listSize; j++) {
			var elemMap = new JsonMap();
			for (var col : columnsInGroup) {
				var colValues = columnData.get(col.fullPath());
				var rowList = (colValues != null && rowIndex < colValues.size()) ? colValues.get(rowIndex) : null;
				if (!(rowList instanceof List<?> values))
					continue;
				var val = j < values.size() ? values.get(j) : null;
				elemMap.put(col.elementProp(), val);
			}
			result.add(elemType.isBean() ? convertToType(prepareMapForBean(elemMap, elemType), elemType) : elemMap);
		}
		return result;
	}

	/**
	 * Replaces keys equal to {@link ParquetParser#nullKeyString} (default <js>"&lt;NULL&gt;"</js>)
	 * with actual <jk>null</jk> in maps. Enables round-trip of maps with null keys in flat (column-per-key) format.
	 */
	@SuppressWarnings("unchecked")
	private void replaceNullKeySentinel(Object obj) {
		if (obj == null)
			return;
		if (obj instanceof Map<?, ?> m) {
			if (m.containsKey(ctx.nullKeyString)) {
				var val = m.get(ctx.nullKeyString);
				((Map<Object, Object>)m).remove(ctx.nullKeyString);
				((Map<Object, Object>)m).put(null, val);
			}
			for (var v : m.values())
				replaceNullKeySentinel(v);
		} else if (obj instanceof List<?> list) {
			for (var e : list)
				replaceNullKeySentinel(e);
		}
	}

	/** Collapses Parquet optional-group {value: X} wrappers so BeanSession receives unwrapped values for Optional properties. */
	@SuppressWarnings({
		"unchecked" // (Map<String,Object>) cast for mutating optional-group structure
	})
	private void collapseOptionalWrappers(Map<?,?> row, ClassMeta<?> elementType) {
		if (row == null || !elementType.isBean())
			return;
		var bm = elementType.getBeanMeta();
		if (bm == null)
			return;
		boolean hasOptional = bm.getProperties().values().stream().anyMatch(p -> p.getClassMeta() != null && p.getClassMeta().isOptional());
		if (!hasOptional)
			return;
		for (var pMeta : bm.getProperties().values()) {
			var name = pMeta.getName();
			var val = row.get(name);
			var propType = pMeta.getClassMeta();
			if (val == null)
				continue;
			if (propType.isOptional() && val instanceof Map<?,?> m) {
				var elemType = propType.getElementType();
				boolean absent = isAbsentOptionalMap(m, elemType);
				// Detect absent inner Optional: all-null map, empty map, or nested {value:{all-null}} structure.
				// Use null so BeanSession's null→Optional<Optional<X>> conversion produces Optional.of(Optional.empty()).
				if (absent) {
					((Map<String,Object>)row).put(name, null);
				} else if (m.size() == 1 && m.containsKey("value")) {
					var inner = m.get("value");
					var collapsed = collapseOptionalValue(inner, elemType);
					((Map<String,Object>)row).put(name, collapsed);
				}
			} else if (propType.isBean() && val instanceof Map<?,?> m2)
				collapseOptionalWrappers(m2, propType);
		}
	}

	/**
	 * Returns true if the map represents an absent Optional value that should be treated as null,
	 * allowing BeanSession's null→Optional<Optional<X>> conversion to produce Optional.of(Optional.empty()).
	 *
	 * <p>Absent patterns:
	 * <ul>
	 *   <li>Empty map {}
	 *   <li>Map whose values are all null (e.g. {value: null})
	 *   <li>Map {value: M} where M is itself absent (recursively): handles Optional<Optional<X>>
	 *   <li>Map {value: M} where M has all null leaves (bean group for absent inner type)
	 * </ul>
	 */
	private boolean isAbsentOptionalMap(Map<?,?> m, ClassMeta<?> elemType) {
		if (m.isEmpty())
			return true;
		if (m.values().stream().allMatch(v -> v == null))
			return true;
		if (m.size() == 1 && m.containsKey("value")) {
			var inner = m.get("value");
			if (inner == null)
				return true;
			if (inner instanceof Map<?,?> innerMap) {
				if (elemType != null && elemType.isOptional())
					return isAbsentOptionalMap(innerMap, elemType.getElementType());
				// For bean (non-Optional) inner type: check if all nested leaf values are null (absent bean group)
				return allNullLeaves(innerMap);
			}
		}
		return false;
	}

	/** Returns true if all leaf values in the nested map structure are null. */
	private boolean allNullLeaves(Object obj) {
		if (obj == null)
			return true;
		if (!(obj instanceof Map<?,?> m))
			return false;
		return m.values().stream().allMatch(this::allNullLeaves);
	}

	/** Recursively unwraps {value: X} when target is Optional; collapses nested beans. */
	private Object collapseOptionalValue(Object val, ClassMeta<?> targetType) {
		if (val == null)
			return null;
		if (targetType != null && targetType.isOptional() && val instanceof Map<?,?> m) {
			if (isAbsentOptionalMap(m, targetType))
				return null;  // Let BeanSession convert null to Optional.of(Optional.empty())
			if (m.size() == 1 && m.containsKey("value"))
				return collapseOptionalValue(m.get("value"), targetType.getElementType());
		}
		if (targetType != null && targetType.isBean() && val instanceof Map<?,?> m2) {
			collapseOptionalWrappers(m2, targetType);
			return m2;
		}
		return val;
	}

	@SuppressWarnings({
		"unchecked" // (Map<String,Object>) cast when creating nested maps at path segments
	})
	private static void setByPath(Map<String,Object> target, String path, Object value) throws ParseException {
		if (target == null)
			return;
		var parts = path.split("\\.");
		if (parts.length == 1) {
			target.put(path, value);
			return;
		}
		Object current = target;
		for (int i = 0; i < parts.length - 1; i++) {
			var key = parts[i];
			Object next = current instanceof Map<?,?> m ? m.get(key) : null;
			if (next == null) {
				if (current instanceof Map<?,?> m) {
					next = new JsonMap();
					((Map<String,Object>)m).put(key, next);
				} else {
					throw new ParseException("Missing nested object for path ''{0}''", path);
				}
			}
			current = next;
		}
		var lastKey = parts[parts.length - 1];
		if (current instanceof Map<?,?> m)
			((Map<String,Object>)m).put(lastKey, value);
	}

	/** Wraps Map in JsonMap when target expects JsonMap (e.g. bean with constructor A(JsonMap)). */
	private static Object prepareMapForBean(Object value, ClassMeta<?> type) {
		if (value == null || type == null)
			return value;
		if (value instanceof JsonMap)
			return value;
		if (value instanceof Map<?, ?> m && !type.isMap())
			return new JsonMap(m);
		return value;
	}

	/** Unwraps ValueHolder map {value: X} to X when target is scalar; else converts via BeanSession. */
	private Object unwrapValueHolder(Object row, ClassMeta<?> targetType) throws ParseException {
		if (row == null)
			return null;
		if (!(row instanceof Map<?, ?> m))
			return convertToType(row, targetType);
		// Unwrap {value: X} when target is scalar (or Object) - allow maps with extra keys (e.g. _type)
		if (m.containsKey("value") && (targetType == null || targetType.isObject()
			|| (!targetType.isMap() && !targetType.isCollection() && !targetType.isArray()))) {
			var v = m.get("value");
			if (targetType == null || targetType.isObject())
				return v;
			return convertToType(prepareMapForBean(v, targetType), targetType);
		}
		return convertToType(prepareMapForBean(row, targetType), targetType);
	}

	private Object toArray(List<Object> values, ClassMeta<?> componentType) throws ParseException {
		var ct = componentType.inner();
		var arr = Array.newInstance(ct, values.size());
		for (int i = 0; i < values.size(); i++)
			Array.set(arr, i, convertToType(values.get(i), componentType));
		return arr;
	}

	@SuppressWarnings({
		"unchecked" // Collection.newInstance() returns raw type; cast to Collection<Object> for add
	})
	private Object toCollection(List<Object> values, ClassMeta<?> collectionType) throws ParseException {
		var elemType = collectionType.getElementType();
		if (elemType == null)
			elemType = ctx.getBeanContext().getClassMeta(Object.class);
		Collection<Object> result;
		try {
			result = collectionType.canCreateNewInstance() ? (Collection<Object>)collectionType.newInstance() : null;
		} catch (@SuppressWarnings("unused") Exception ignored) {
			result = null;
		}
		if (result == null)
			result = new ArrayList<>();
		for (var v : values)
			result.add((Object)convertToType(v, elemType));
		return result;
	}
}

