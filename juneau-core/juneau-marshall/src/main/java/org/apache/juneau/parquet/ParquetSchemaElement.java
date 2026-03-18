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

import java.io.IOException;

/**
 * A Parquet schema element representing a field in the Parquet schema.
 *
 * <p>
 * Maps to the SchemaElement struct in the
 * <a class="doclink" href="https://github.com/apache/parquet-format/blob/master/src/main/thrift/parquet.thrift">parquet.thrift</a>
 * specification. Used for both primitive (leaf) and group (non-leaf) nodes.
 *
 * <h5 class='section'>Parquet Type Enum (Type):</h5>
 * <ul>
 * 	<li>BOOLEAN = 0</li>
 * 	<li>INT32 = 1</li>
 * 	<li>INT64 = 2</li>
 * 	<li>FLOAT = 4</li>
 * 	<li>DOUBLE = 5</li>
 * 	<li>BYTE_ARRAY = 6</li>
 * 	<li>FIXED_LEN_BYTE_ARRAY = 7</li>
 * </ul>
 *
 * <h5 class='section'>Field Repetition Type:</h5>
 * <ul>
 * 	<li>REQUIRED = 0</li>
 * 	<li>OPTIONAL = 1</li>
 * 	<li>REPEATED = 2</li>
 * </ul>
 *
 * <h5 class='section'>Converted Type (common values):</h5>
 * <ul>
 * 	<li>UTF8 = 0, ENUM = 4, DECIMAL = 5, TIMESTAMP_MILLIS = 9, INT_8 = 15, INT_16 = 16, INT_32 = 17, INT_64 = 18, MAP = 1, LIST = 3</li>
 * </ul>
 */
public final class ParquetSchemaElement {

	/** Parquet physical type: BOOLEAN. */
	public static final int TYPE_BOOLEAN = 0;
	/** Parquet physical type: INT32. */
	public static final int TYPE_INT32 = 1;
	/** Parquet physical type: INT64. */
	public static final int TYPE_INT64 = 2;
	/** Parquet physical type: FLOAT. */
	public static final int TYPE_FLOAT = 4;
	/** Parquet physical type: DOUBLE. */
	public static final int TYPE_DOUBLE = 5;
	/** Parquet physical type: BYTE_ARRAY. */
	public static final int TYPE_BYTE_ARRAY = 6;
	/** Parquet physical type: FIXED_LEN_BYTE_ARRAY. */
	public static final int TYPE_FIXED_LEN_BYTE_ARRAY = 7;

	/** Field repetition type: REQUIRED (parquet.thrift FieldRepetitionType). */
	public static final int REQUIRED = 0;
	/** Field repetition type: OPTIONAL (parquet.thrift FieldRepetitionType). */
	public static final int OPTIONAL = 1;
	/** Field repetition type: REPEATED (parquet.thrift FieldRepetitionType). */
	public static final int REPEATED = 2;

	/** Converted type: UTF8 (parquet.thrift ConvertedType). */
	public static final int CONVERTED_UTF8 = 0;
	/** Converted type: MAP. */
	public static final int CONVERTED_MAP = 1;
	/** Converted type: LIST. */
	public static final int CONVERTED_LIST = 3;
	/** Converted type: ENUM. */
	public static final int CONVERTED_ENUM = 4;
	/** Converted type: DECIMAL. */
	public static final int CONVERTED_DECIMAL = 5;
	/** Converted type: TIMESTAMP_MILLIS. */
	public static final int CONVERTED_TIMESTAMP_MILLIS = 9;
	/** Converted type: INT_8. */
	public static final int CONVERTED_INT_8 = 15;
	/** Converted type: INT_16. */
	public static final int CONVERTED_INT_16 = 16;
	/** Converted type: INT_32. */
	public static final int CONVERTED_INT_32 = 17;
	/** Converted type: INT_64. */
	public static final int CONVERTED_INT_64 = 18;

	/** LogicalType union discriminant for STRING (StringType). */
	static final int LOGICAL_TYPE_STRING = 1;
	/** LogicalType union discriminant for UUID (UUIDType). */
	static final int LOGICAL_TYPE_UUID = 14;
	/** LogicalType union discriminant for TIMESTAMP (TimestampType). */
	static final int LOGICAL_TYPE_TIMESTAMP = 8;
	/** LogicalType union discriminant for DECIMAL (DecimalType). */
	static final int LOGICAL_TYPE_DECIMAL = 5;

	/** Field name. Required. */
	public final String name;
	/** Parquet physical type. Null for group nodes. */
	public final Integer type;
	/** Byte length for FIXED_LEN_BYTE_ARRAY. */
	public final Integer typeLength;
	/** Repetition type. Null for root. */
	public final Integer repetitionType;
	/** Number of child elements. Non-null for group nodes. */
	public final Integer numChildren;
	/** Deprecated converted type. Used for forward compatibility. */
	public final Integer convertedType;
	/** Logical type discriminant (1=STRING, 5=DECIMAL, 8=TIMESTAMP, 14=UUID, etc.). Null if none. */
	public final Integer logicalType;
	/** Scale for DECIMAL. */
	public final Integer scale;
	/** Precision for DECIMAL. */
	public final Integer precision;
	/** Path from root (e.g. "host" or "db.port") for leaf columns. */
	public final String path;

	/**
	 * Creates a schema element.
	 *
	 * @param name Field name (required).
	 * @param type Parquet physical type (null for groups).
	 * @param typeLength Byte length for FIXED_LEN_BYTE_ARRAY (null otherwise).
	 * @param repetitionType REQUIRED/OPTIONAL/REPEATED (null for root).
	 * @param numChildren Child count for groups (null for primitives).
	 * @param convertedType ConvertedType enum value (null if none).
	 * @param logicalType LogicalType union discriminant (null if none).
	 * @param scale Scale for DECIMAL (null otherwise).
	 * @param precision Precision for DECIMAL (null otherwise).
	 * @param path Dotted path for leaf columns (e.g. "db.port").
	 */
	@SuppressWarnings({
		"java:S107" // Constructor mirrors Parquet schema fields; refactor would obscure mapping
	})
	public ParquetSchemaElement(String name, Integer type, Integer typeLength, Integer repetitionType,
			Integer numChildren, Integer convertedType, Integer logicalType, Integer scale, Integer precision,
			String path) {
		this.name = name;
		this.type = type;
		this.typeLength = typeLength;
		this.repetitionType = repetitionType;
		this.numChildren = numChildren;
		this.convertedType = convertedType;
		this.logicalType = logicalType;
		this.scale = scale;
		this.precision = precision;
		this.path = path != null ? path : name;
	}

	/**
	 * Writes this schema element to the Thrift Compact Protocol encoder.
	 *
	 * @param enc The encoder.
	 * @throws IOException If an I/O error occurs.
	 */
	public void writeTo(ThriftCompactEncoder enc) throws IOException {
		enc.writeStructBegin();
		if (type != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 1);
			enc.writeI32(type);
		}
		if (typeLength != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 2);
			enc.writeI32(typeLength);
		}
		if (repetitionType != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 3);
			enc.writeI32(repetitionType);
		}
		enc.writeFieldBegin(ThriftCompactEncoder.BINARY, 4);
		enc.writeString(name);
		if (numChildren != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 5);
			enc.writeI32(numChildren);
		}
		if (convertedType != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 6);
			enc.writeI32(convertedType);
		}
		if (scale != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 7);
			enc.writeI32(scale);
		}
		if (precision != null) {
			enc.writeFieldBegin(ThriftCompactEncoder.I32, 8);
			enc.writeI32(precision);
		}
		// ConvertedType is sufficient for the current implementation.
		// LogicalType encoding can be added back once the nested Thrift union
		// layout is verified against the Parquet spec.
		enc.writeStructEnd();
	}

	/**
	 * Returns true if this is a leaf (primitive) column.
	 *
	 * @return <jk>true</jk> if this element has a type and no children.
	 */
	public boolean isLeaf() {
		return type != null && (numChildren == null || numChildren == 0);
	}
}
