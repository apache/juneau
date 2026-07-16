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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * The protobuf binary wire types (the low 3 bits of a field tag).
 *
 * <p>
 * Each serialized field on the protobuf binary wire is preceded by a tag computed as
 * <c>(fieldNumber &lt;&lt; 3) | wireType</c>.  The wire type tells a decoder how many bytes the value
 * occupies even when it doesn't know the field's specific scalar type.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 */
public enum WireType {

	/** Wire type 0:  varint (int32/int64/uint32/uint64/sint32/sint64/bool/enum). */
	VARINT(0),

	/** Wire type 1:  64-bit fixed (fixed64/sfixed64/double). */
	I64(1),

	/** Wire type 2:  length-delimited (string/bytes/embedded message/packed repeated). */
	LEN(2),

	/** Wire type 3:  start group (deprecated; not supported). */
	SGROUP(3),

	/** Wire type 4:  end group (deprecated; not supported). */
	EGROUP(4),

	/** Wire type 5:  32-bit fixed (fixed32/sfixed32/float). */
	I32(5);

	private final int code;

	WireType(int code) {
		this.code = code;
	}

	/**
	 * Returns the numeric wire-type code (0-5) stored in the low 3 bits of a field tag.
	 *
	 * @return The numeric wire-type code.
	 */
	public int code() {
		return code;
	}

	/**
	 * Returns the wire type for the specified low-3-bits code.
	 *
	 * @param code The wire-type code (the low 3 bits of a tag).
	 * @return The matching wire type.
	 * @throws IllegalArgumentException If the code does not map to a known wire type.
	 */
	public static WireType fromCode(int code) {
		switch (code) {
			case 0: return VARINT;
			case 1: return I64;
			case 2: return LEN;
			case 3: return SGROUP;
			case 4: return EGROUP;
			case 5: return I32;
			default: throw iaex("Invalid protobuf wire type: %s", code);
		}
	}

	/**
	 * Returns the wire type encoded in the low 3 bits of the specified tag value.
	 *
	 * @param tag The full tag value (<c>(fieldNumber &lt;&lt; 3) | wireType</c>).
	 * @return The matching wire type.
	 * @throws IllegalArgumentException If the encoded code does not map to a known wire type.
	 */
	public static WireType fromTag(int tag) {
		return fromCode(tag & 0x07);
	}
}
