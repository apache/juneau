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
package org.apache.juneau.marshall.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.msgpack.*;
import org.junit.jupiter.api.*;

/**
 * Demonstrates that a single format-agnostic consumer can branch on
 * {@link BinaryNativeKind} alone (without per-format {@code instanceof} on the cursor) and
 * extract the native metadata from both CBOR and MsgPack opt-in cursors.  This is the
 * 175ad base-role abstraction that lets a generic native-aware consumer work uniformly across
 * formats.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class BinaryNativeKind_GenericConsumer_Test extends TestBase {

	/**
	 * A format-agnostic consumer that walks a {@link TokenReader} in native mode and renders
	 * each value-bearing token as a string with its native metadata, branching purely on
	 * {@link BinaryNativeKind}.
	 */
	private static String render(TokenReader r) throws Exception {
		var sb = new StringBuilder();
		while (true) {
			var t = r.next();
			if (t == TokenType.END_OF_STREAM)
				break;
			switch (t) {
				case VALUE_NUMBER:
				case VALUE_STRING:
				case VALUE_BINARY:
				case VALUE_NULL:
				case VALUE_BOOLEAN:
					sb.append(t).append('(');
					switch (r.getNativeKind()) {
						case CBOR_TAG:
							sb.append("tagged:");
							for (var i = 0; i < r.getTagCount(); i++) {
								if (i > 0) sb.append(',');
								sb.append(r.getTag(i));
							}
							sb.append(';');
							break;
						case CBOR_SIMPLE:
							sb.append("simple:").append(r.getSimpleValue()).append(';');
							break;
						case MSGPACK_EXT:
							sb.append("ext:").append(r.getExtType()).append(';');
							break;
						case NONE:
							break;
					}
					sb.append(')');
					break;
				default:
					sb.append(t);
			}
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	@Test void a01_cborTaggedString() throws Exception {
		// CBOR: tag(0) wrapping "hi"
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
			w.writeTag(0);
			w.string("hi");
		}
		try (var r = CborParser.DEFAULT_NATIVE.parseTokens(bos.toByteArray())) {
			assertEquals("VALUE_STRING(tagged:0;)", render(r));
		}
	}

	@Test void a02_cborSimple() throws Exception {
		// Use simple value 16 (within the valid 0..19 range; 20-23 are reserved for
		// false/true/null/undefined and 25-27 for float16/32/64).
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
			w.writeSimple(16);
		}
		try (var r = CborParser.DEFAULT_NATIVE.parseTokens(bos.toByteArray())) {
			assertEquals("VALUE_NULL(simple:16;)", render(r));
		}
	}

	@Test void a03_msgpackExt() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
			w.writeExt(5, new byte[]{1, 2, 3, 4});
		}
		try (var r = MsgPackParser.DEFAULT_NATIVE.parseTokens(bos.toByteArray())) {
			assertEquals("VALUE_BINARY(ext:5;)", render(r));
		}
	}
}
