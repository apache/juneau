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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.marshall.parquet.ParquetSchemaElement.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session for {@link ParquetParser}.
 */
@SuppressWarnings({
	"java:S110",
	"java:S115",
	"java:S2583", // parquetDebug() is runtime-configurable via setDebugEnabled/-Djuneau.parquet.debug
	"java:S3776",
	"java:S6541", // Brain Method: Parquet parsing/serialization flows are inherently branchy
	"java:S1192", // Duplicated literals (.list.element, root.list.element., value) are schema keys; constants would obscure
	"resource"    // RecordReader returned by RecordAdapter is a Closeable owned by the caller; Eclipse JDT @Owning warning is by design.
})
public class ParquetParserSession extends InputStreamParserSession implements RecordReadable, ArrayRecordReadable {

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
	public static class Builder extends InputStreamParserSession.Builder<Builder> {

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

	/**
	 * Opens a whole-value pull-parser cursor over a Parquet document, bound to this live session.
	 * {@link RecordReader#read(Class) read(...)} delegates to the polymorphic
	 * {@link ParserSession#parse(Object, Class)} entry point.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return RecordAdapter.reader(this, input);
	}

	/**
	 * Buffered array-element {@link RecordReader} for Parquet, bound to this live session.  Calls
	 * {@code parse(input, List.class, Object.class)} once and iterates the result.
	 *
	 * <h5 class='section'>Why this stays buffered (not O(1) streaming):</h5>
	 * <p>
	 * Although a Parquet file is logically row-shaped, it is <b>not</b> a forward, element-at-a-time
	 * wire format.  Its metadata footer (schema + row-group/column-chunk offsets) lives at the
	 * <b>end</b> of the file, so a reader must seek to the tail before it can interpret any row, and
	 * values are stored <b>column-major</b> &mdash; reconstructing one logical record requires
	 * reading one page from every column chunk.  Both properties force whole-file (or at minimum
	 * whole-row-group) buffering; a true O(1)-in-rows cursor is not achievable over the current
	 * single-row-group in-memory decoder.  Left buffered by design.
	 *
	 * @param input The input.
	 * @return A buffered {@link RecordReader}.
	 * @throws IOException If a problem occurred reading the input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader parseArrayRecords(Object input) throws IOException {
		return RecordAdapter.arrayReader(this, input);
	}

	/**
	 * The Parquet record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordReadable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The Parquet array-record cursor is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * <p>
	 * Parquet's end-of-file metadata footer and column-major value layout require whole-file
	 * buffering before any record can be reconstructed, so genuine element-at-a-time streaming is
	 * not feasible here; see {@link #parseArrayRecords(Object)} for the full rationale.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordReadable */
	public boolean isArrayRecordStreaming() { return false; }

	@Override /* InputStreamParserSession */
	public boolean hasNativeBytes() {
		// Parquet's column reader has no native byte-array primitive type — it surfaces byte[] as a raw
		// BYTE_ARRAY column (at BinaryFormat.NOT_SET; Bug #11 raw-bytes path) or as a UTF-8 string column
		// (at any other BinaryFormat, after the BinarySwap reverses the configured text wire form).
		return false;
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
			elementType = ctx.getMarshallingContext().getClassMeta(Map.class);
		// For scalar types, use Map so reassembleRows keeps raw map rows for unwrap logic
		if (!effectiveType.isMap() && !effectiveType.isCollection() && !effectiveType.isArray()) {
			var inner = effectiveType.inner();
			if (inner != null && (inner.isPrimitive() || inner == String.class || inner == Boolean.class
				|| inner == Character.class || Number.class.isAssignableFrom(inner)))
				elementType = ctx.getMarshallingContext().getClassMeta(Map.class);
		}
		var rows = readAllRows(bytes, meta, elementType, meta.schemaRepetition(), meta.rawByteArrayPaths(), meta.uuidPaths(), meta.columnLogical());
		// Unwrap ValueHolder {value: X} when single row has "value" key and target expects scalar (not Map/List<Map>)
		boolean targetWantsScalar = !type.isMap()
			&& !(type.isCollection() && type.getElementType() != null && type.getElementType().isMap())
			&& !(type.isArray() && type.getElementType() != null && type.getElementType().isMap());
		if (rows.size() == 1 && rows.get(0) instanceof Map<?, ?> m && m.containsKey("value") && targetWantsScalar) {
			var inner = m.get("value");
			if (type.isOptional())
				return (T)opt(convertToType(inner, type.getElementType()));
			if (type.isArray()) {
				var list = new ArrayList<>(List.of(inner));
				return (T)convertToType(list, type);
			}
			if (type.isCollection()) {
				Object coll = type.newInstance();
				if (coll == null)
					coll = new ArrayList<>();
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
				if (valueType == null) valueType = ctx.getMarshallingContext().getClassMeta(Object.class);
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
				return (T)opte();
			var inner = unwrapValueHolder(rows.get(0), type.getElementType());
			return (T)opt(inner);
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
		for (int i = 0; i < b.length; i++)
			if (a[off + i] != b[i]) return false;
		return true;
	}

	private static int readLe4(byte[] b, int off) {
		return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
	}

	private static final long MAX_NUM_ROWS = 10_000_000;
	private static final long MAX_NUM_VALUES = 10_000_000;

	/** Sentinel for a null intermediate OPTIONAL group at the given def level (GAP-14 multi-level nesting). */
	private record GroupNull(int defLevel) {}

	private record FileMeta(long numRows, List<RowGroupMeta> rowGroups, Map<String, Integer> schemaRepetition, Set<String> rawByteArrayPaths, Set<String> uuidPaths, Map<String, ColumnLogical> columnLogical) {}
	private record RowGroupMeta(long numRows, List<ColumnChunkMeta> columns) {}
	private record ColumnChunkMeta(int type, List<String> pathInSchema, int codec, long numValues, long dataPageOffset, long totalCompressedSize) {}

	// Parquet page types (PageHeader.type).
	private static final int PAGE_DATA = 0;
	private static final int PAGE_DICTIONARY = 2;

	// Parquet encodings (DataPageHeader.encoding).
	private static final int ENCODING_PLAIN = 0;
	private static final int ENCODING_PLAIN_DICTIONARY = 2;
	private static final int ENCODING_RLE_DICTIONARY = 8;

	/**
	 * One decoded Parquet page header plus the file offset at which its (possibly compressed) page
	 * body begins.  Covers both {@code DataPageHeader} (field 5) and {@code DictionaryPageHeader}
	 * (field 7); the unused sub-header's fields are left at their defaults.
	 *
	 * @param pageType {@code PageHeader.type} — {@link #PAGE_DATA}, {@link #PAGE_DICTIONARY}, etc.
	 * @param uncompressedSize Uncompressed page-body size in bytes.
	 * @param compressedSize Compressed page-body size in bytes (equals uncompressedSize when UNCOMPRESSED).
	 * @param numValues Value count carried by the page's own sub-header.
	 * @param encoding Data/dictionary page encoding ({@link #ENCODING_PLAIN}, {@link #ENCODING_RLE_DICTIONARY}, …).
	 * @param bodyStart File offset where the page body (compressed bytes) starts.
	 */
	private record PageHeaderInfo(int pageType, int uncompressedSize, int compressedSize, int numValues, int encoding, int bodyStart) {}

	private static final int MAX_PAGE_SIZE = 256 * 1024 * 1024;

	/**
	 * Reads a single Parquet {@code PageHeader} Thrift struct starting at {@code off} in {@code fileBytes}.
	 *
	 * @param fileBytes The whole file.
	 * @param off Offset of the page header.
	 * @param columnPath Column path (for error messages only).
	 * @return The decoded header plus the offset where the page body begins.
	 * @throws ParseException If the header is malformed or declares out-of-range sizes.
	 * @throws IOException If the underlying Thrift decode fails.
	 */
	@SuppressWarnings({
		"resource" // bais is an in-memory ByteArrayInputStream; no OS resource to close.
	})
	private static PageHeaderInfo readPageHeader(byte[] fileBytes, int off, String columnPath) throws ParseException, IOException {
		var bais = new ByteArrayInputStream(fileBytes, off, fileBytes.length - off);
		var dec = new ThriftCompactDecoder(bais);
		dec.readStructBegin();
		int pageType = PAGE_DATA;
		int uncompressedSize = 0;
		int compressedSize = 0;
		int numValues = 0;
		int encoding = ENCODING_PLAIN;
		ThriftCompactDecoder.FieldHeader fh;
		while (!(fh = dec.readFieldHeader()).isStop) {
			switch (fh.fieldId) {
				case 1 -> pageType = dec.readI32();
				case 2 -> uncompressedSize = dec.readI32();
				case 3 -> compressedSize = dec.readI32();
				case 5, 7 -> { // DataPageHeader (5) or DictionaryPageHeader (7): both carry num_values(1)+encoding(2).
					dec.readStructBegin();
					ThriftCompactDecoder.FieldHeader sub;
					while (!(sub = dec.readFieldHeader()).isStop) {
						if (sub.fieldId == 1)
							numValues = dec.readI32();
						else if (sub.fieldId == 2)
							encoding = dec.readI32();
						else
							dec.skipField(sub.type);
					}
					dec.readStructEnd();
				}
				default -> dec.skipField(fh.type);
			}
		}
		dec.readStructEnd();
		int headerConsumed = (fileBytes.length - off) - bais.available();
		int bodyStart = off + headerConsumed;
		if (uncompressedSize < 0 || uncompressedSize > MAX_PAGE_SIZE
			|| compressedSize < 0 || compressedSize > MAX_PAGE_SIZE
			|| bodyStart + compressedSize > fileBytes.length)
			throw new ParseException("Invalid page header for column ''{0}'': uncompressed=''{1}'', compressed=''{2}''",
				columnPath, uncompressedSize, compressedSize);
		if (numValues < 0 || numValues > MAX_NUM_VALUES)
			throw new ParseException("Invalid page num_values for column ''{0}'': {1}", columnPath, numValues);
		return new PageHeaderInfo(pageType, uncompressedSize, compressedSize, numValues, encoding, bodyStart);
	}

	/**
	 * Returns the offset of the first {@code DATA_PAGE} at or after {@code off}, skipping a leading
	 * {@code DICTIONARY_PAGE} if one is present.  Used by the repeated (list/map) chunk readers, which
	 * decode a single data page but must still tolerate a dictionary page in front of it.
	 */
	private static int skipToDataPage(byte[] fileBytes, int off, String columnPath) throws ParseException, IOException {
		var ph = readPageHeader(fileBytes, off, columnPath);
		if (ph.pageType() == PAGE_DICTIONARY)
			return ph.bodyStart() + ph.compressedSize();
		return off;
	}

	/**
	 * Result of {@link #readSchema(ThriftCompactDecoder)}: the per-leaf repetition map plus the set of
	 * leaf paths whose physical type is {@code TYPE_BYTE_ARRAY} with no {@code convertedType} —
	 * i.e. raw {@code byte[]} columns emitted by {@code ParquetSchemaBuilder.addLeafSchema}'s
	 * {@code cm.isByteArray()} branch (Bug #11).  The Parquet file footer drops the
	 * {@code logicalType} discriminant ({@code ParquetSchemaElement.writeTo} omits field 7), so the
	 * parser uses "{@code TYPE_BYTE_ARRAY} with no {@code convertedType}" as the unique signal that a
	 * column should be reassembled back into {@code byte[]} instead of decoded as UTF-8 text.
	 *
	 * <p>
	 * {@code uuidPaths} carries the set of leaf paths that should be decoded as {@link UUID}. The decision
	 * prefers the explicit {@code LogicalType} discriminant (field 10) when present — a column is a UUID iff
	 * its logical type is UUID — and falls back to the legacy "{@code FIXED_LEN_BYTE_ARRAY} == UUID"
	 * physical-type heuristic when no logical type is recorded (backward compatibility with files written
	 * without the opt-in discriminator).
	 */
	private record SchemaReadResult(Map<String, Integer> repetitions, Set<String> rawByteArrayPaths, Set<String> uuidPaths, Map<String, ColumnLogical> columnLogical) {}

	/**
	 * Per-leaf logical-type metadata captured from the schema, used to decode native logical-type columns
	 * (GAP-9/10): the {@code convertedType} (parquet.thrift ConvertedType), DECIMAL {@code scale}/{@code precision},
	 * and the physical {@code type}.  All fields are best-effort: {@code convertedType} is null when absent.
	 */
	record ColumnLogical(Integer convertedType, int scale, int precision) {}

	private static FileMeta parseFileMetaData(byte[] footer) throws ParseException {
		try {
			var dec = new ThriftCompactDecoder(footer);
			dec.readStructBegin();
			long numRows = 0;
			List<RowGroupMeta> rowGroups = null;
			Map<String, Integer> schemaRepetition = Map.of();
			Set<String> rawByteArrayPaths = Set.of();
			Set<String> uuidPaths = Set.of();
			Map<String, ColumnLogical> columnLogical = Map.of();
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				switch (fh.fieldId) {
					case 1 -> dec.readI32(); // version - consumed but not used
					case 2 -> {
						var sr = readSchema(dec);
						schemaRepetition = sr.repetitions();
						rawByteArrayPaths = sr.rawByteArrayPaths();
						uuidPaths = sr.uuidPaths();
						columnLogical = sr.columnLogical();
					}
					case 3 -> numRows = dec.readI64();
					case 4 -> rowGroups = readRowGroups(dec);
					default -> dec.skipField(fh.type);
				}
			}
			dec.readStructEnd();
			return new FileMeta(numRows, rowGroups != null ? rowGroups : List.of(), schemaRepetition, rawByteArrayPaths, uuidPaths, columnLogical);
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	private record SchemaStackFrame(String name, int remaining) {}

	private static SchemaReadResult readSchema(ThriftCompactDecoder dec) throws IOException {
		var lh = dec.readListHeader();
		var pathStack = new ArrayList<SchemaStackFrame>();
		var result = new LinkedHashMap<String, Integer>();
		var rawByteArrayPaths = new java.util.LinkedHashSet<String>();
		var uuidPaths = new java.util.LinkedHashSet<String>();
		var columnLogical = new LinkedHashMap<String, ColumnLogical>();
		for (int i = 0; i < lh.size; i++) {
			dec.readStructBegin();
			Integer type = null;
			Integer repetitionType = null;
			String name = null;
			Integer numChildren = null;
			Integer convertedType = null;
			Integer logicalType = null;
			int scale = 0;
			int precision = 0;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				switch (fh.fieldId) {
					case 1 -> type = dec.readI32();
					case 2 -> dec.readI32();
					case 3 -> repetitionType = dec.readI32();
					case 4 -> name = dec.readString();
					case 5 -> numChildren = dec.readI32();
					case 6 -> convertedType = dec.readI32();
					case 7 -> scale = dec.readI32();      // SchemaElement.scale (DECIMAL)
					case 8 -> precision = dec.readI32();  // SchemaElement.precision (DECIMAL)
					case 10 -> logicalType = readLogicalTypeUnion(dec);
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
				// Capture logical-type metadata for native-type decode (GAP-9/10).  Only the DECIMAL and
				// temporal converted types matter to the decoder; the UUID/string paths are handled separately.
				if (convertedType != null)
					columnLogical.put(path, new ColumnLogical(convertedType, scale, precision));
				// Bug #11 fix: a TYPE_BYTE_ARRAY column with no convertedType uniquely identifies a
				// raw byte[] column emitted by ParquetSchemaBuilder.addLeafSchema's cm.isByteArray()
				// branch.  Every other writer site that emits TYPE_BYTE_ARRAY sets a discriminator
				// (CONVERTED_UTF8 for strings/dates/durations/decimals, CONVERTED_ENUM for enums, or
				// CONVERTED_TIMESTAMP_MILLIS).  Mirrors the Bug #7a UUID parse-back assumption
				// (TYPE_FIXED_LEN_BYTE_ARRAY uniquely identifies UUIDs) at a different physical type.
				if (type == TYPE_BYTE_ARRAY && convertedType == null)
					rawByteArrayPaths.add(path);
				// Prefer the explicit LogicalType discriminant (field 10) when present; otherwise fall back to
				// the legacy "FIXED_LEN_BYTE_ARRAY == UUID" physical-type heuristic for backward compatibility
				// with files written without the opt-in discriminator (work item 134).
				if (type == TYPE_FIXED_LEN_BYTE_ARRAY)
					uuidPaths.add(path);
				if (parquetDebug())
					parquetDebugLog("readSchema leaf: path=" + path + " name=" + name + " rep=" + rep
						+ " convertedType=" + convertedType + " logicalType=" + logicalType + " pathStack=" + pathStack);
				boolean moreToDecrement = true;
				while (!pathStack.isEmpty() && moreToDecrement) {
					var top = pathStack.get(pathStack.size() - 1);
					var newRemaining = top.remaining - 1;
					if (newRemaining <= 0)
						pathStack.remove(pathStack.size() - 1);
					else {
						pathStack.set(pathStack.size() - 1, new SchemaStackFrame(top.name, newRemaining));
						moreToDecrement = false;
					}
				}
			}
		}
		if (parquetDebug())
			parquetDebugLog("readSchema result: " + result + " rawByteArrayPaths=" + rawByteArrayPaths + " uuidPaths=" + uuidPaths);
		return new SchemaReadResult(result, rawByteArrayPaths, uuidPaths, columnLogical);
	}

	/**
	 * Reads a Parquet {@code LogicalType} union (parquet.thrift field 10) and returns the discriminant of
	 * the set union member (e.g. {@link ParquetSchemaElement#LOGICAL_TYPE_UUID} for {@code UUIDType}).
	 *
	 * <p>
	 * A Thrift union is encoded like a struct with exactly one field set; the field id is the union
	 * discriminant. Returns <jk>null</jk> if the union is empty (no member set).
	 */
	private static Integer readLogicalTypeUnion(ThriftCompactDecoder dec) throws IOException {
		dec.readStructBegin();
		Integer discriminant = null;
		ThriftCompactDecoder.FieldHeader fh;
		while (!(fh = dec.readFieldHeader()).isStop) {
			if (discriminant == null)
				discriminant = fh.fieldId;
			dec.skipField(fh.type);
		}
		dec.readStructEnd();
		return discriminant;
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
			long groupNumRows = 0;
			ThriftCompactDecoder.FieldHeader fh;
			while (!(fh = dec.readFieldHeader()).isStop) {
				if (fh.fieldId == 1)
					columns = readColumnChunks(dec);
				else if (fh.fieldId == 3)
					groupNumRows = dec.readI64(); // RowGroup.num_rows
				else
					dec.skipField(fh.type);
			}
			dec.readStructEnd();
			list.add(new RowGroupMeta(groupNumRows, columns != null ? columns : List.of()));
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

	private List<?> readAllRows(byte[] fileBytes, FileMeta meta, ClassMeta<?> elementType, Map<String, Integer> schemaRepetition, Set<String> rawByteArrayPaths, Set<String> uuidPaths, Map<String, ColumnLogical> columnLogical) throws ParseException {
		if (meta.numRows() == 0 || meta.rowGroups().isEmpty())
			return List.of();
		// Multi-row-group (GAP-2): read every row group and concatenate its reassembled rows in order.
		// Each group declares its own num_rows; fall back to the file total only for a lone group whose
		// per-group count wasn't recorded (older Juneau-written files).
		var allRows = new ArrayList<Object>((int)Math.min(meta.numRows(), MAX_NUM_ROWS));
		for (var group : meta.rowGroups()) {
			int groupRows = (int)group.numRows();
			allRows.addAll(readRowGroupRows(fileBytes, group, groupRows, elementType, schemaRepetition, rawByteArrayPaths, uuidPaths, columnLogical));
		}
		return allRows;
	}

	@SuppressWarnings({
		"java:S107" // Parser-internal method threads decode state (column paths, schema repetition, logical types); parameter count is intentional.
	})
	private List<?> readRowGroupRows(byte[] fileBytes, RowGroupMeta group, int numRows, ClassMeta<?> elementType, Map<String, Integer> schemaRepetition, Set<String> rawByteArrayPaths, Set<String> uuidPaths, Map<String, ColumnLogical> columnLogical) throws ParseException {
		var columnData = new LinkedHashMap<String, List<Object>>();
		for (var cc : group.columns()) {
			var path = String.join(".", cc.pathInSchema());
			List<Object> values;
			var trim = isTrimStrings();
			if (isListColumnPath(path))
				values = readListColumnChunk(fileBytes, cc, numRows, trim);
			else if (isMapKeyValueColumnPath(path))
				values = readMapKeyValueColumnChunk(fileBytes, cc, numRows, trim);
			else
				values = readColumnChunk(fileBytes, cc, numRows, schemaRepetition, rawByteArrayPaths, uuidPaths, columnLogical, trim);
			columnData.put(path, values);
		}
		if (parquetDebug()) {
			var summary = columnData.entrySet().stream()
				.map(e -> e.getKey() + "->" + e.getValue())
				.toList();
			parquetDebugLog("readRowGroupRows: numRows=" + numRows + " columnData=" + summary);
		}
		return reassembleRows(columnData, numRows, elementType);
	}

	private static boolean isListColumnPath(String path) {
		return path.endsWith(".list.element") || path.contains(".list.element.");
	}

	private static boolean isMapKeyValueColumnPath(String path) {
		return path.contains(".key_value.key") || path.contains(".key_value.value");
	}

	private static String mapPropertyPath(String mapKeyValuePath) {
		return mapKeyValuePath.substring(0, mapKeyValuePath.indexOf(".key_value."));
	}

	/** Returns true if rows are key-value pairs (Map with non-String keys format). */
	private static boolean isKeyValuePairFormat(List<?> rows) {
		if (e(rows))
			return false;
		var first = (Map<?,?>) rows.get(0);
		return first.containsKey("key") && first.containsKey("value") && first.size() == 2;
	}

	/** Path relative to the row (bean). All schema leaf paths start with "root.". */
	private static String rowRelativePath(String fullPath) {
		return fullPath.substring("root.".length());
	}

	/** For list column path like "tags.list.element" or "members.list.element.name", returns the bean property name ("tags" or "members"). */
	private static String listPropertyPath(String listColumnPath) {
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
			int defBitWidth = 32 - Integer.numberOfLeadingZeros(maxDef);
			int repBitWidth = 32 - Integer.numberOfLeadingZeros(maxRep);

			if (cc.numValues() < 0 || cc.numValues() > MAX_NUM_VALUES)
				throw new ParseException("Invalid numValues for column ''{0}'': {1}", String.join(".", cc.pathInSchema()), cc.numValues());
			var codec = CompressionCodec.fromThrift(cc.codec());
			// Skip a leading dictionary page if present, then read the single data page.  Repeated
			// (list/map) columns carry rep+def levels with cross-page state; Juneau emits them as a single
			// data page, and dictionary-encoded repeated columns are not produced here, so a single-data-page
			// decode (via the shared page-header reader for Snappy + validation) is sufficient.
			var chunkPath = String.join(".", cc.pathInSchema());
			int dataPageOff = skipToDataPage(fileBytes, (int)cc.dataPageOffset(), chunkPath);
			var ph = readPageHeader(fileBytes, dataPageOff, chunkPath);
			var compressedData = Arrays.copyOfRange(fileBytes, ph.bodyStart(), ph.bodyStart() + ph.compressedSize());
			var decompressed = codec.decompress(compressedData, ph.uncompressedSize());

			int numValues = (int)cc.numValues();
			int off2 = 0;
			int repLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
				| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
			off2 += 4 + repLen;
			int defStart = off2;
			int defLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
				| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
			off2 += 4 + defLen;
			var valueBytes = Arrays.copyOfRange(decompressed, off2, decompressed.length);
			var repBytes = Arrays.copyOfRange(decompressed, 4, 4 + repLen);
			var defBytes = Arrays.copyOfRange(decompressed, defStart + 4, defStart + 4 + defLen);
			var repDecoder = new RleBitPackingDecoder(repBytes, repBitWidth);
			var defDecoder = new RleBitPackingDecoder(defBytes, defBitWidth);
			var valueReader = new ParquetColumnReader(valueBytes, numValues, 0);

			var flattened = new ArrayList<Object[]>();
			for (int i = 0; i < numValues; i++) {
				int rep = repDecoder.readInt();
				int def = defDecoder.readInt();
				Object val = def >= maxDef ? readValue(valueReader, cc.type(), trimStrings) : null;
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
	@SuppressWarnings({
		"unchecked" // Generic array creation for stacks.
	})
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
			} else if (def <= 2 * (depth - 1) + 1) {
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
			int defBitWidth = 32 - Integer.numberOfLeadingZeros(maxDef);
			int repBitWidth = 32 - Integer.numberOfLeadingZeros(maxRep);

			if (cc.numValues() < 0 || cc.numValues() > MAX_NUM_VALUES)
				throw new ParseException("Invalid numValues for column ''{0}'': {1}", String.join(".", cc.pathInSchema()), cc.numValues());
			var codec = CompressionCodec.fromThrift(cc.codec());
			// Skip a leading dictionary page if present, then read the single data page.  Repeated
			// (list/map) columns carry rep+def levels with cross-page state; Juneau emits them as a single
			// data page, and dictionary-encoded repeated columns are not produced here, so a single-data-page
			// decode (via the shared page-header reader for Snappy + validation) is sufficient.
			var chunkPath = String.join(".", cc.pathInSchema());
			int dataPageOff = skipToDataPage(fileBytes, (int)cc.dataPageOffset(), chunkPath);
			var ph = readPageHeader(fileBytes, dataPageOff, chunkPath);
			var compressedData = Arrays.copyOfRange(fileBytes, ph.bodyStart(), ph.bodyStart() + ph.compressedSize());
			var decompressed = codec.decompress(compressedData, ph.uncompressedSize());

			int numValues = (int)cc.numValues();
			int off2 = 0;
			int repLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
				| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
			off2 += 4 + repLen;
			int defStart = off2;
			int defLen = (decompressed[off2] & 0xFF) | ((decompressed[off2 + 1] & 0xFF) << 8)
				| ((decompressed[off2 + 2] & 0xFF) << 16) | ((decompressed[off2 + 3] & 0xFF) << 24);
			off2 += 4 + defLen;
			var repBytes = Arrays.copyOfRange(decompressed, 4, 4 + repLen);
			var defBytes = Arrays.copyOfRange(decompressed, defStart + 4, defStart + 4 + defLen);
			var repDecoder = new RleBitPackingDecoder(repBytes, repBitWidth);
			var defDecoder = new RleBitPackingDecoder(defBytes, defBitWidth);
			var valueBytes = Arrays.copyOfRange(decompressed, off2, decompressed.length);
			var valueReader = new ParquetColumnReader(valueBytes, numValues, 0);

			var flattened = new ArrayList<Object[]>();
			for (int i = 0; i < numValues; i++) {
				int rep = repDecoder.readInt();
				int def = defDecoder.readInt();
				Object val = def >= maxDef ? readValue(valueReader, cc.type(), trimStrings) : null;
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

	@SuppressWarnings({
		"java:S107" // Parser-internal method threads decode state (column paths, schema repetition, logical types); parameter count is intentional.
	})
	private static List<Object> readColumnChunk(byte[] fileBytes, ColumnChunkMeta cc, int numRows, Map<String, Integer> schemaRepetition, Set<String> rawByteArrayPaths, Set<String> uuidPaths, Map<String, ColumnLogical> columnLogical, boolean trimStrings) throws ParseException {
		try {
			if (cc.numValues() < 0 || cc.numValues() > MAX_NUM_VALUES)
				throw new ParseException("Invalid numValues for column ''{0}'': {1}", String.join(".", cc.pathInSchema()), cc.numValues());
			var codec = CompressionCodec.fromThrift(cc.codec());
			var path = String.join(".", cc.pathInSchema());
			int rep = schemaRepetition.getOrDefault(path, OPTIONAL);
			boolean isUnderListRoot = path.startsWith("root.list.element.");
			boolean isPrimitiveType = cc.type() == TYPE_INT32 || cc.type() == TYPE_INT64 || cc.type() == TYPE_FLOAT || cc.type() == TYPE_DOUBLE || cc.type() == TYPE_BOOLEAN;
			// Max def level for a non-list/non-map leaf is the number of OPTIONAL groups enclosing it, i.e. the
			// rowRelative path segment count (GAP-14).  Single-segment columns keep the historical max=1.
			int rowRelSeg = rowRelativePath(path).split("\\.").length;
			int maxDefLevel = (rep == REQUIRED || (isUnderListRoot && isPrimitiveType)) ? 0 : rowRelSeg;
			int defBitWidth = maxDefLevel <= 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxDefLevel);
			boolean isRawByteArrayColumn = rawByteArrayPaths.contains(path);
			boolean isUuidColumn = uuidPaths.contains(path);
			var logical = columnLogical.get(path);
			if (parquetDebug())
				parquetDebugLog("readColumnChunk: path=" + path + " pathInSchema=" + cc.pathInSchema()
				+ " rep=" + rep + " (REQ=" + REQUIRED + ") maxDefLevel=" + maxDefLevel + " type=" + cc.type()
				+ " isPrimitive=" + isPrimitiveType + " isRawByteArrayColumn=" + isRawByteArrayColumn
				+ " isUuidColumn=" + isUuidColumn + " logical=" + logical);

			// Page loop (GAP-1): a column chunk may hold a leading dictionary page (GAP-3) followed by
			// one or more data pages.  Walk pages from dataPageOffset until the chunk's numValues
			// level/value entries are consumed.
			int valuesToRead = (int)Math.min(cc.numValues(), numRows);
			var values = new ArrayList<>();
			List<Object> dictionary = null;
			int pageOff = (int)cc.dataPageOffset();
			int consumed = 0;
			int pageGuard = 0;
			while (consumed < valuesToRead && pageOff < fileBytes.length) {
				if (++pageGuard > MAX_PAGES_PER_CHUNK)
					throw new ParseException("Column ''{0}'' exceeds the maximum of {1} pages per chunk", path, MAX_PAGES_PER_CHUNK);
				var ph = readPageHeader(fileBytes, pageOff, path);
				var compressedData = Arrays.copyOfRange(fileBytes, ph.bodyStart(), ph.bodyStart() + ph.compressedSize());
				var decompressed = codec.decompress(compressedData, ph.uncompressedSize());
				if (ph.pageType() == PAGE_DICTIONARY) {
					dictionary = readDictionaryPage(decompressed, ph.numValues(), cc.type(), trimStrings, isRawByteArrayColumn, isUuidColumn, logical);
				} else {
					int pageValues = Math.min(ph.numValues(), valuesToRead - consumed);
					boolean dictEncoded = ph.encoding() == ENCODING_PLAIN_DICTIONARY || ph.encoding() == ENCODING_RLE_DICTIONARY;
					readDataPageValues(decompressed, pageValues, maxDefLevel, defBitWidth, cc.type(), trimStrings,
						isRawByteArrayColumn, isUuidColumn, logical, dictEncoded, dictionary, values);
					consumed += pageValues;
				}
				pageOff = ph.bodyStart() + ph.compressedSize();
			}
			if (parquetDebug())
				parquetDebugLog("readColumnChunk values: path=" + path + " values=" + values);
			return values;
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	private static final int MAX_PAGES_PER_CHUNK = 1_000_000;

	/**
	 * Decodes one PLAIN-or-dictionary-encoded data page into {@code values}, appending {@link GroupNull}
	 * sentinels for null intermediate OPTIONAL groups (GAP-14).
	 */
	@SuppressWarnings({
		"java:S107" // Parser-internal method threads decode state (page metrics, def levels, logical types, value sink); parameter count is intentional.
	})
	static void readDataPageValues(byte[] decompressed, int pageValues, int maxDefLevel, int defBitWidth,
			int type, boolean trimStrings, boolean isRawByteArrayColumn, boolean isUuidColumn, ColumnLogical logical,
			boolean dictEncoded, List<Object> dictionary, List<Object> values) throws IOException, ParseException {
		if (dictEncoded) {
			// Dictionary-encoded data page (GAP-3): def levels (if any) then a 1-byte bit-width followed by
			// RLE/bit-packed dictionary indices.  Nulls consume a def level but no index.
			readDictionaryEncodedPage(decompressed, pageValues, maxDefLevel, defBitWidth, dictionary, values);
			return;
		}
		var reader = new ParquetColumnReader(decompressed, pageValues, maxDefLevel, defBitWidth);
		while (reader.hasNext()) {
			var v = readValue(reader, type, trimStrings, isRawByteArrayColumn, isUuidColumn, logical);
			if (maxDefLevel >= 2 && reader.getDefLevel() < maxDefLevel - 1)
				values.add(new GroupNull(reader.getDefLevel()));
			else
				values.add(v);
		}
	}

	/**
	 * Decodes a {@code DICTIONARY_PAGE} body (PLAIN-encoded values, no def/rep levels) into an indexable
	 * dictionary (GAP-3).
	 *
	 * @return The dictionary entries in index order.
	 */
	static List<Object> readDictionaryPage(byte[] decompressed, int numDictValues, int type,
			boolean trimStrings, boolean isRawByteArrayColumn, boolean isUuidColumn, ColumnLogical logical) throws IOException {
		// A dictionary page is PLAIN-encoded with no definition levels (every entry present): maxDefLevel=0.
		var reader = new ParquetColumnReader(decompressed, numDictValues, 0);
		var dict = new ArrayList<>(numDictValues);
		while (reader.hasNext())
			dict.add(readValue(reader, type, trimStrings, isRawByteArrayColumn, isUuidColumn, logical));
		return dict;
	}

	/**
	 * Decodes a dictionary-encoded data page (GAP-3): optional RLE definition levels, then a leading
	 * 1-byte bit-width followed by RLE/bit-packed dictionary indices.  Present values map their index
	 * back through {@code dictionary}; nulls consume a definition level but no index.
	 */
	// Package-private for focused unit testing of the dictionary-index decode path (GAP-3).
	static void readDictionaryEncodedPage(byte[] decompressed, int pageValues, int maxDefLevel, int defBitWidth,
			List<Object> dictionary, List<Object> values) throws ParseException {
		if (dictionary == null)
			throw new ParseException("Dictionary-encoded data page encountered with no preceding dictionary page");
		try {
			int off = 0;
			RleBitPackingDecoder defDecoder = null;
			if (maxDefLevel > 0 && decompressed.length >= 4) {
				int defLen = (decompressed[0] & 0xFF) | ((decompressed[1] & 0xFF) << 8)
					| ((decompressed[2] & 0xFF) << 16) | ((decompressed[3] & 0xFF) << 24);
				if (defLen >= 0 && defLen <= decompressed.length - 4) {
					off = 4;
					defDecoder = new RleBitPackingDecoder(decompressed, off, defLen, defBitWidth);
					off += defLen;
				}
			}
			// Dictionary indices section: 1-byte bit width, then RLE/bit-packed hybrid indices.
			int indexBitWidth = decompressed[off] & 0xFF;
			off++;
			var idxDecoder = new RleBitPackingDecoder(decompressed, off, decompressed.length - off, indexBitWidth);
			for (int i = 0; i < pageValues; i++) {
				int def = defDecoder != null ? defDecoder.readInt() : maxDefLevel;
				if (def < maxDefLevel) {
					if (maxDefLevel >= 2 && def < maxDefLevel - 1)
						values.add(new GroupNull(def));
					else
						values.add(null);
				} else {
					int idx = idxDecoder.readInt();
					if (idx >= dictionary.size())
						throw new ParseException("Dictionary index {0} out of range [0,{1})", idx, dictionary.size());
					values.add(dictionary.get(idx));
				}
			}
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	private static Object readValue(ParquetColumnReader reader, int type, boolean trimStrings) throws IOException {
		// List/map element columns: no per-path discriminator is threaded, so keep the legacy
		// "FIXED_LEN_BYTE_ARRAY == UUID" assumption (isUuidColumn=true) to preserve existing round trips.
		return readValue(reader, type, trimStrings, false, true, null);
	}

	// Package-private for focused unit testing of the external-read value-decode paths (INT96/DECIMAL/TIME/etc.).
	static Object readValue(ParquetColumnReader reader, int type, boolean trimStrings, boolean isRawByteArrayColumn, boolean isUuidColumn, ColumnLogical logical) throws IOException {
		reader.advance();
		if (reader.isNull())
			return null;
		// Native logical-type decode (GAP-9/10): when a column carries a DECIMAL/DATE/TIME/TIMESTAMP-micros
		// convertedType, decode to the matching value and surface a form the framework's coercion understands.
		// Read-side decode is always on; absent a logical convertedType the physical-type switch below applies.
		if (logical != null) {
			var nativeValue = readLogicalValue(reader, type, logical);
			if (nativeValue != NOT_LOGICAL)
				return nativeValue;
		}
		return switch (type) {
			case TYPE_BOOLEAN -> reader.readBoolean();
			case TYPE_INT32 -> reader.readInt32();
			case TYPE_INT64 -> reader.readInt64();
			// INT96 (GAP-8): legacy 12-byte timestamp (Impala/Hive/older Spark).  Decode to an Instant and
			// surface its ISO-8601 string so the framework's temporal coercion reconstructs the target type.
			// Read-only — Juneau never writes INT96.
			case TYPE_INT96 -> reader.readInt96AsInstant().toString();
			case TYPE_FLOAT -> reader.readFloat();
			case TYPE_DOUBLE -> reader.readDouble();
			case TYPE_BYTE_ARRAY -> {
				// Bug #11 fix: when the schema marks this column as a raw byte[] column
				// (TYPE_BYTE_ARRAY with no convertedType — see SchemaReadResult), surface the bytes
				// as a byte[] so the framework can hand them straight to the bean-property setter.
				// Otherwise read the bytes as UTF-8 for string / enum / date / duration / decimal
				// columns (which carry CONVERTED_UTF8 / CONVERTED_ENUM / CONVERTED_TIMESTAMP_MILLIS).
				if (isRawByteArrayColumn) {
					yield reader.readByteArray();
				}
				var s = reader.readByteArrayAsString();
				yield trimStrings ? s.trim() : s;
			}
			// TYPE_FIXED_LEN_BYTE_ARRAY is emitted by ParquetSchemaBuilder for UUID columns
			// (TYPE_FIXED_LEN_BYTE_ARRAY(16) / LOGICAL_TYPE_UUID).  Routing prefers the explicit
			// LogicalType discriminant when the writer emitted it (opt-in mode), and otherwise falls
			// back to the legacy "FLBA == UUID" physical-type assumption — both resolve to isUuidColumn
			// upstream (see readSchema).  Without the UUID conversion the parser would surface a raw
			// byte[16] into the row JsonMap and the framework's generic String/byte[]→UUID coercion
			// path can't reassemble it back to a UUID.
			case TYPE_FIXED_LEN_BYTE_ARRAY -> {
				var bytes = reader.readFixedLenByteArray(16);
				// A FLBA column with an explicit non-UUID logical type is surfaced as raw bytes rather than
				// misread as a UUID. Juneau's own writer never emits such a column today, so this is a
				// forward-compat guard for future FLBA logical types. // HTT
				yield isUuidColumn ? uuidFromFixedLenBytes(bytes) : bytes;
			}
			default -> {
				var s = reader.readByteArrayAsString();
				yield trimStrings ? s.trim() : s;
			}
		};
	}

	/** Sentinel returned by {@link #readLogicalValue} when the column is not a recognized native logical type. */
	private static final Object NOT_LOGICAL = new Object();

	/**
	 * Decodes a native logical-type value (GAP-9/10) based on the column's {@code convertedType} and physical
	 * type.  Returns {@link #NOT_LOGICAL} if the convertedType is not one this decoder handles (so the caller
	 * falls back to the physical-type switch).  The reader has already been advanced and confirmed non-null.
	 *
	 * <ul>
	 * 	<li>DECIMAL (INT32/INT64) → {@link java.math.BigDecimal} with the schema scale.
	 * 	<li>DATE (INT32 days-since-epoch) → {@link java.time.LocalDate} ISO string.
	 * 	<li>TIME_MILLIS / TIME_MICROS → {@link java.time.LocalTime} ISO string.
	 * 	<li>TIMESTAMP_MICROS (INT64 micros-since-epoch) → {@link java.time.Instant} ISO string.
	 * </ul>
	 */
	// Package-private for focused unit testing of the native logical-type decode paths (GAP-9/10).
	static Object readLogicalValue(ParquetColumnReader reader, int type, ColumnLogical logical) throws IOException {
		int ct = logical.convertedType();
		switch (ct) {
			case CONVERTED_DECIMAL -> {
				// DECIMAL backed by INT32 or INT64: unscaled integer * 10^-scale.  BYTE_ARRAY/FLBA-backed
				// DECIMAL is left to the string path (not commonly emitted by Juneau).
				long unscaled;
				if (type == TYPE_INT32)
					unscaled = reader.readInt32();
				else if (type == TYPE_INT64)
					unscaled = reader.readInt64();
				else
					return NOT_LOGICAL;
				return java.math.BigDecimal.valueOf(unscaled, logical.scale());
			}
			case CONVERTED_DATE -> {
				if (type != TYPE_INT32)
					return NOT_LOGICAL;
				return java.time.LocalDate.ofEpochDay(reader.readInt32()).toString();
			}
			case CONVERTED_TIME_MILLIS -> {
				if (type != TYPE_INT32)
					return NOT_LOGICAL;
				return java.time.LocalTime.ofNanoOfDay(reader.readInt32() * 1_000_000L).toString();
			}
			case CONVERTED_TIME_MICROS -> {
				if (type != TYPE_INT64)
					return NOT_LOGICAL;
				return java.time.LocalTime.ofNanoOfDay(reader.readInt64() * 1_000L).toString();
			}
			case CONVERTED_TIMESTAMP_MICROS -> {
				if (type != TYPE_INT64)
					return NOT_LOGICAL;
				long micros = reader.readInt64();
				long secs = Math.floorDiv(micros, 1_000_000L);
				long microOfSec = Math.floorMod(micros, 1_000_000L);
				return java.time.Instant.ofEpochSecond(secs, microOfSec * 1_000L).toString();
			}
			default -> {
				return NOT_LOGICAL;
			}
		}
	}

	/**
	 * Reassembles a {@link UUID} from the 16-byte big-endian wire form written by
	 * {@code ParquetSerializerSession#toFixedLenByteArray(UUID)}.
	 *
	 * <p>
	 * Mirrors the serializer's write loop (most-significant 8 bytes then least-significant 8 bytes,
	 * both big-endian) so the round-trip is bit-exact.  Falls back to {@code new UUID(0L, 0L)} when
	 * the buffer is the wrong length — defensive guard for fuzzed input.
	 */
	// Package-private for focused unit testing of the null/wrong-length guard.
	static UUID uuidFromFixedLenBytesForTest(byte[] b) {
		return uuidFromFixedLenBytes(b);
	}

	private static UUID uuidFromFixedLenBytes(byte[] b) {
		if (b == null || b.length != 16)
			return new UUID(0L, 0L);
		long msb = 0L;
		long lsb = 0L;
		for (int i = 0; i < 8; i++)
			msb = (msb << 8) | (b[i] & 0xFFL);
		for (int i = 0; i < 8; i++)
			lsb = (lsb << 8) | (b[8 + i] & 0xFFL);
		return new UUID(msb, lsb);
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
				var v = values.get(i);
				if (v instanceof GroupNull gn) {
					// Null intermediate group: set the prefix path (up to the null level) to null without
					// synthesizing the deeper map structure (GAP-14).
					var parts = rowRelPath.split("\\.");
					var lvl = Math.min(gn.defLevel(), parts.length - 1);
					var prefix = String.join(".", Arrays.copyOfRange(parts, 0, lvl + 1));
					if (isTrimStrings())
						prefix = prefix.trim();
					setByPath(row, prefix, null);
				} else {
					var path = isListColumnPath(fullPath) ? listProp : rowRelPath;
					if (isTrimStrings())
						path = path.trim();
					setByPath(row, path, v);
				}
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
		for (var fullPath : columnPaths) {
			if (!isListColumnPath(fullPath))
				continue;
			var rowRelPath = rowRelativePath(fullPath);
			var listProp = listPropertyPath(rowRelPath);
			var suffix = ".list.element.";
			var suffixStart = listProp.length() + suffix.length();
			var pMeta = rowRelPath.length() > suffixStart ? bm.getPropertyMeta(listProp) : null;
			var propClassMeta = pMeta != null ? (ClassMeta<?>) pMeta.getBeanInfo() : null;
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
			if (rowRelPath.endsWith(".key_value.key"))
				groups.put(prop, new MapColumnInfo(fullPath, null));
			else if (rowRelPath.endsWith(".key_value.value")) {
				var prev = groups.get(prop);
				groups.put(prop, new MapColumnInfo(prev.keyColumnPath(), fullPath));
			}
		}
		return groups;
	}

	private Map<Object, Object> mergeMapColumns(MapColumnInfo info, Map<String, List<Object>> columnData, int rowIndex) throws ParseException {
		var keyList = (List<?>) columnData.get(info.keyColumnPath()).get(rowIndex);
		if (keyList.isEmpty())
			return new LinkedHashMap<>();
		var valueList = (List<?>) columnData.get(info.valueColumnPath()).get(rowIndex);
		var result = new LinkedHashMap<>();
		var valueIter = valueList.iterator();
		for (var k : keyList) {
			var v = valueIter.next();
			Object key = ctx.nullKeyString.equals(String.valueOf(k)) ? null : k;
			result.put(key, v);
		}
		return result;
	}

	private List<Object> mergeListBeanColumns(List<ListColumnInfo> columnsInGroup, Map<String, List<Object>> columnData,
		int rowIndex) throws ParseException {
		var elemType = columnsInGroup.get(0).elementType();
		var firstList = (List<?>) columnData.get(columnsInGroup.get(0).fullPath()).get(rowIndex);
		int listSize = firstList.size();
		var result = new ArrayList<>(listSize);
		for (int j = 0; j < listSize; j++) {
			var elemMap = new JsonMap();
			for (var col : columnsInGroup) {
				var rowList = (List<?>) columnData.get(col.fullPath()).get(rowIndex);
				elemMap.put(col.elementProp(), rowList.get(j));
			}
			result.add(convertToType(prepareMapForBean(elemMap, elemType), elemType));
		}
		return result;
	}

	/**
	 * Replaces keys equal to {@link ParquetParser#nullKeyString} (default <js>"&lt;NULL&gt;"</js>)
	 * with actual <jk>null</jk> in maps. Enables round-trip of maps with null keys in flat (column-per-key) format.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: Parquet schema type is verified at parse time.
	})
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

	/** Collapses Parquet optional-group {value: X} wrappers so MarshallingSession receives unwrapped values for Optional properties. */
	@SuppressWarnings({
		"unchecked" // (Map<String,Object>) cast for mutating optional-group structure
	})
	private void collapseOptionalWrappers(Map<?,?> row, ClassMeta<?> elementType) {
		var bm = elementType.getBeanMeta();
		boolean hasOptional = bm.getProperties().values().stream().anyMatch(p -> p.getBeanInfo() != null && p.getBeanInfo().isOptional());
		if (!hasOptional)
			return;
		for (var pMeta : bm.getProperties().values()) {
			var name = pMeta.getName();
			var val = row.get(name);
			var propType = (ClassMeta<?>) pMeta.getBeanInfo();
			if (val == null)
				continue;
			if (propType.isOptional() && val instanceof Map<?,?> m) {
				var elemType = propType.getElementType();
				boolean absent = isAbsentOptionalMap(m, elemType);
				// Detect absent inner Optional: all-null map, empty map, or nested {value:{all-null}} structure.
				// Use null so MarshallingSession's null→Optional<Optional<X>> conversion produces Optional.of(Optional.empty()).
				if (absent) {
					((Map<String,Object>)row).put(name, null);
				} else {
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
	 * allowing MarshallingSession's null→Optional<Optional<X>> conversion to produce Optional.of(Optional.empty()).
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
		if (m.values().stream().allMatch(v -> v == null))
			return true;
		var inner = m.get("value");
		if (inner instanceof Map<?,?> innerMap) {
			if (elemType.isOptional())
				return isAbsentOptionalMap(innerMap, elemType.getElementType());
			// For bean (non-Optional) inner type: check if all nested leaf values are null (absent bean group)
			return allNullLeaves(innerMap);
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
		if (targetType.isOptional() && val instanceof Map<?,?> m) {
			if (isAbsentOptionalMap(m, targetType))
				return null;  // Let MarshallingSession convert null to Optional.of(Optional.empty())
			return collapseOptionalValue(m.get("value"), targetType.getElementType());
		}
		if (targetType.isBean() && val instanceof Map<?,?> m2) {
			collapseOptionalWrappers(m2, targetType);
			return m2;
		}
		return val;
	}

	@SuppressWarnings({
		"unchecked" // (Map<String,Object>) cast when creating nested maps at path segments
	})
	private static void setByPath(Map<String,Object> target, String path, Object value) throws ParseException {
		var parts = path.split("\\.");
		if (parts.length == 1) {
			target.put(path, value);
			return;
		}
		Object current = target;
		for (int i = 0; i < parts.length - 1; i++) {
			var key = parts[i];
			var currentMap = (Map<?,?>) current;
			current = ((Map<String,Object>)currentMap).computeIfAbsent(key, k -> new JsonMap());
		}
		((Map<String,Object>)current).put(parts[parts.length - 1], value);
	}

	/** Wraps Map in JsonMap when target expects JsonMap (e.g. bean with constructor A(JsonMap)). */
	private static Object prepareMapForBean(Object value, ClassMeta<?> type) {
		if (value == null)
			return value;
		if (value instanceof JsonMap)
			return value;
		if (value instanceof Map<?, ?> m && !type.isMap())
			return new JsonMap(m);
		return value;
	}

	/** Unwraps ValueHolder map {value: X} to X when target is scalar; else converts via MarshallingSession. */
	private Object unwrapValueHolder(Object row, ClassMeta<?> targetType) throws ParseException {
		if (!(row instanceof Map<?, ?> m))
			return convertToType(row, targetType);
		// Unwrap {value: X} when target is scalar (or Object) - allow maps with extra keys (e.g. _type)
		if (m.containsKey("value") && (targetType.isObject()
			|| (!targetType.isMap() && !targetType.isCollection() && !targetType.isArray()))) {
			var v = m.get("value");
			if (targetType.isObject())
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

