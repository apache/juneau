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
package org.apache.juneau.bson;

/**
 * BSON element type codes per the
 * <a class="doclink" href="https://bsonspec.org/spec.html">BSON Specification 1.1</a>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
public enum DataType {

	/** 64-bit IEEE 754 double. */
	DOUBLE(0x01),
	/** UTF-8 string. */
	STRING(0x02),
	/** Embedded document. */
	DOCUMENT(0x03),
	/** Array (document with numeric keys). */
	ARRAY(0x04),
	/** Binary data. */
	BINARY(0x05),
	/** Deprecated undefined. */
	UNDEFINED(0x06),
	/** MongoDB ObjectId (12 bytes). */
	OBJECT_ID(0x07),
	/** Boolean. */
	BOOLEAN(0x08),
	/** UTC datetime (int64 millis). */
	DATETIME(0x09),
	/** Null value. */
	NULL(0x0A),
	/** Regular expression. */
	REGEX(0x0B),
	/** Deprecated DBPointer. */
	DB_POINTER(0x0C),
	/** JavaScript code. */
	JAVASCRIPT(0x0D),
	/** Deprecated symbol. */
	SYMBOL(0x0E),
	/** JavaScript with scope. */
	JS_WITH_SCOPE(0x0F),
	/** 32-bit integer. */
	INT32(0x10),
	/** MongoDB timestamp. */
	TIMESTAMP(0x11),
	/** 64-bit integer. */
	INT64(0x12),
	/** IEEE 754-2008 decimal128. */
	DECIMAL128(0x13),
	/** MinKey (sort order). */
	MIN_KEY(0xFF),
	/** MaxKey (sort order). */
	MAX_KEY(0x7F);

	private static final DataType[] BY_VALUE;

	static {
		var max = 256;
		BY_VALUE = new DataType[max];
		for (var dt : values()) {
			var v = dt.value & 0xFF;
			if (v < max)
				BY_VALUE[v] = dt;
		}
	}

	final int value;

	DataType(int value) {
		this.value = value;
	}

	/**
	 * Returns the DataType for the given byte value.
	 *
	 * @param b The byte value (0x01-0x13, 0x7F, 0xFF).
	 * @return The corresponding DataType, or <jk>null</jk> if unknown.
	 */
	public static DataType fromByte(int b) {
		var v = b & 0xFF;
		return v < BY_VALUE.length ? BY_VALUE[v] : null;
	}
}
