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
package org.apache.juneau.marshall.protobuf;

/**
 * The set of protobuf scalar types that a bean property can be mapped to via {@link Protobuf#type()}.
 *
 * <p>
 * Each scalar type knows the {@link WireType} it uses on the wire.  {@link #AUTO} is a sentinel meaning
 * "derive the scalar type from the Java type" (the default behavior); it is never written to the wire.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 */
public enum ProtobufScalarType {

	/** Sentinel:  derive the scalar type from the Java property type. */
	AUTO(WireType.VARINT),

	/** Signed 32-bit integer, plain varint (negative values encode as 10 bytes). */
	INT32(WireType.VARINT),

	/** Signed 64-bit integer, plain varint. */
	INT64(WireType.VARINT),

	/** Unsigned 32-bit integer, plain varint. */
	UINT32(WireType.VARINT),

	/** Unsigned 64-bit integer, plain varint. */
	UINT64(WireType.VARINT),

	/** Signed 32-bit integer, zigzag varint (efficient for negatives). */
	SINT32(WireType.VARINT),

	/** Signed 64-bit integer, zigzag varint. */
	SINT64(WireType.VARINT),

	/** Unsigned 32-bit integer, 4-byte little-endian. */
	FIXED32(WireType.I32),

	/** Unsigned 64-bit integer, 8-byte little-endian. */
	FIXED64(WireType.I64),

	/** Signed 32-bit integer, 4-byte little-endian. */
	SFIXED32(WireType.I32),

	/** Signed 64-bit integer, 8-byte little-endian. */
	SFIXED64(WireType.I64),

	/** IEEE-754 single precision, 4-byte little-endian. */
	FLOAT(WireType.I32),

	/** IEEE-754 double precision, 8-byte little-endian. */
	DOUBLE(WireType.I64),

	/** Boolean, varint (0/1). */
	BOOL(WireType.VARINT),

	/** UTF-8 string, length-delimited. */
	STRING(WireType.LEN),

	/** Raw bytes, length-delimited. */
	BYTES(WireType.LEN),

	/** Enum encoded as its {@code ordinal()} via int32 varint. */
	ENUM_INT(WireType.VARINT),

	/** Enum encoded as its {@code name()} via a UTF-8 string. */
	ENUM_STRING(WireType.LEN);

	private final WireType wireType;

	ProtobufScalarType(WireType wireType) {
		this.wireType = wireType;
	}

	/**
	 * Returns the wire type used by this scalar type.
	 *
	 * @return The wire type used by this scalar type.
	 */
	public WireType wireType() {
		return wireType;
	}
}
