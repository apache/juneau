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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Phase-2 conformance regression tests guarding the Parquet codec audit fixes (GAP-4/5/12/13/14).
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types; explicit casts required for typed assertions
})
class ParquetConformance_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// GAP-12: a statically-typed java.lang.Number leaf must not be silently narrowed to INT32.
	//-----------------------------------------------------------------------------------------------------------------

	public static class NumBean {
		public Number n;
	}

	@Test
	void gap12_numberFieldAboveIntRangeNotTruncated() throws Exception {
		// > Integer.MAX_VALUE: the old INT32 mapping corrupted this to 705032704.  DOUBLE preserves it exactly.
		var big = new NumBean();
		big.n = 5_000_000_000L;
		var bytes = ParquetSerializer.DEFAULT.serialize(big);
		var parsed = (List<NumBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NumBean.class);

		assertEquals(1, parsed.size());
		assertNotNull(parsed.get(0).n);
		assertEquals(5_000_000_000L, parsed.get(0).n.longValue());
	}

	@Test
	void gap12_fractionalNumberFieldNotTruncated() throws Exception {
		// The old INT32 mapping truncated 3.14 to 3.
		var frac = new NumBean();
		frac.n = 3.14d;
		var bytes = ParquetSerializer.DEFAULT.serialize(frac);
		var parsed = (List<NumBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NumBean.class);

		assertEquals(3.14d, parsed.get(0).n.doubleValue(), 0.0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GAP-13: acyclic data flowing through a recursive *type* must round-trip as nested beans, not toString garbage.
	//-----------------------------------------------------------------------------------------------------------------

	public static class Node {
		public String val;
		public Node next;
	}

	@Test
	void gap13_acyclicRecursiveTypeRoundTrips() throws Exception {
		var a = new Node();
		a.val = "a";
		a.next = new Node();
		a.next.val = "b";
		a.next.next = new Node();
		a.next.next.val = "c";

		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		var parsed = (List<Node>) ParquetParser.DEFAULT.parse(bytes, List.class, Node.class);

		assertEquals(1, parsed.size());
		var n = parsed.get(0);
		assertEquals("a", n.val);
		assertNotNull(n.next);
		assertEquals("b", n.next.val);
		assertNotNull(n.next.next);
		assertEquals("c", n.next.next.val);
		assertNull(n.next.next.next);
	}

	@Test
	void gap13_recursionBeyondMaxDepthTruncatesToNull() throws Exception {
		// Depth-2 limit: only the root + one level of "next" survive; deeper links truncate to null.
		var ser = ParquetSerializer.create().maxRecursionDepth(2).build();
		var a = new Node();
		a.val = "a";
		a.next = new Node();
		a.next.val = "b";
		a.next.next = new Node();
		a.next.next.val = "c";

		var bytes = ser.serialize(a);
		var parsed = (List<Node>) ParquetParser.DEFAULT.parse(bytes, List.class, Node.class);

		var n = parsed.get(0);
		assertEquals("a", n.val);
		assertNotNull(n.next);
		assertEquals("b", n.next.val);
		assertNull(n.next.next);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GAP-14: multi-level optional nesting must distinguish a null intermediate group from a present group with a
	// null leaf.
	//-----------------------------------------------------------------------------------------------------------------

	public static class B {
		public String v;
	}

	public static class A {
		public B b;
	}

	@Test
	void gap14_multiLevelOptionalNullDistinction() throws Exception {
		var nullGroup = new A(); // a.b == null
		var nullLeaf = new A();
		nullLeaf.b = new B(); // a.b != null, a.b.v == null
		var present = new A();
		present.b = new B();
		present.b.v = "x";

		var bytes = ParquetSerializer.DEFAULT.serialize(list(nullGroup, nullLeaf, present));
		var parsed = (List<A>) ParquetParser.DEFAULT.parse(bytes, List.class, A.class);

		assertEquals(3, parsed.size());
		assertNull(parsed.get(0).b, "null intermediate group must reconstruct as null");
		assertNotNull(parsed.get(1).b, "present group with null leaf must not collapse to null group");
		assertNull(parsed.get(1).b.v);
		assertNotNull(parsed.get(2).b);
		assertEquals("x", parsed.get(2).b.v);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GAP-4: PLAIN BOOLEAN must be bit-packed 1 bit/value (LSB-first), not 1 byte/value.
	//-----------------------------------------------------------------------------------------------------------------

	private static ParquetSchemaElement boolColumn() {
		return new ParquetSchemaElement("b", ParquetSchemaElement.TYPE_BOOLEAN, null, ParquetSchemaElement.REQUIRED, null, null, null, null, null, "b");
	}

	@Test
	void gap4_booleanPlainIsBitPacked() {
		var w = new ParquetColumnWriter(boolColumn());
		// true,false,true,false,false,false,false,false -> bits 0b00000101 = 0x05
		var pattern = new boolean[]{true, false, true, false, false, false, false, false};
		for (var v : pattern)
			w.writeBoolean(v);
		var page = w.finalizePage();
		assertEquals(1, page.length, "8 booleans must pack into a single byte");
		assertEquals((byte) 0x05, page[0]);
	}

	@Test
	void gap4_booleanPartialTrailingByteFlushed() {
		var w = new ParquetColumnWriter(boolColumn());
		w.writeBoolean(true);
		w.writeBoolean(true);
		w.writeBoolean(false);
		var page = w.finalizePage();
		assertEquals(1, page.length, "3 booleans still occupy exactly one packed byte");
		assertEquals((byte) 0x03, page[0]);
	}

	@Test
	void gap4_booleanBitPackRoundTrip() throws Exception {
		var w = new ParquetColumnWriter(boolColumn());
		var pattern = new boolean[]{true, true, false, true, false, false, true, false, true, true};
		for (var v : pattern)
			w.writeBoolean(v);
		var page = w.finalizePage();
		assertEquals(2, page.length, "10 booleans pack into 2 bytes");

		var r = new ParquetColumnReader(page, pattern.length, 0);
		for (var expected : pattern) {
			assertTrue(r.hasNext());
			r.advance();
			assertEquals(expected, r.readBoolean());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GAP-5: an unsupported compression codec must be a hard error, never a silent fallback to UNCOMPRESSED.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void gap5_supportedCodecsResolve() throws Exception {
		assertEquals(CompressionCodec.UNCOMPRESSED, CompressionCodec.fromThrift(0));
		assertEquals(CompressionCodec.GZIP, CompressionCodec.fromThrift(2));
		// SNAPPY (1) now resolves — decode-only support was added for external-read interop.
		assertEquals(CompressionCodec.SNAPPY, CompressionCodec.fromThrift(1));
	}

	@Test
	void gap5_unsupportedCodecThrows() {
		// Every known-but-unsupported codec id (and an unknown id) is a hard error with a descriptive name.
		assertCodecError(3, "LZO");
		assertCodecError(4, "BROTLI");
		assertCodecError(5, "LZ4");
		assertCodecError(6, "ZSTD");
		assertCodecError(7, "LZ4_RAW");
		var unknown = assertThrows(IOException.class, () -> CompressionCodec.fromThrift(99));
		assertTrue(unknown.getMessage().contains("99"), "Expected raw id in message: " + unknown.getMessage());
	}

	private static void assertCodecError(int thriftValue, String expectedName) {
		var ex = assertThrows(IOException.class, () -> CompressionCodec.fromThrift(thriftValue));
		assertTrue(ex.getMessage().contains(expectedName), "Expected '" + expectedName + "' in message: " + ex.getMessage());
	}
}
