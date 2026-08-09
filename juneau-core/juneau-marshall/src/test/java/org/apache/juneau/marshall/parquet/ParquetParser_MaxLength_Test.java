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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Bounds-check tests for the Parquet length/count guards, mirroring the {@code BsonInputStream.checkLength}
 * pattern applied to the binary parsers.
 *
 * <p>
 * Two untrusted, footer/page-driven values can otherwise size an allocation before any payload is validated:
 * <ul>
 * 	<li>a per-row-group {@code num_rows} count that drives {@code ArrayList} pre-allocation and reassembly
 * 		loops (bounded against the configurable {@link ParquetParser.Builder#maxCount(int)} ceiling), and
 * 	<li>the list/map rep-level and def-level length prefixes that feed {@link java.util.Arrays#copyOfRange}
 * 		(bounded against the bytes actually available in the decompressed page body).
 * </ul>
 *
 * <p>
 * A full Parquet file embeds these values deep inside a Thrift-compact footer/page, so hand-crafting a
 * complete malformed file to exercise the end-to-end path is impractical for the low-level helpers; those
 * are covered directly (oversized rejected, in-range accepted, and the boundary).  The configurable
 * {@code maxLength}/{@code maxCount} knobs on {@link ParquetParser} themselves are covered end-to-end via
 * real serialize/parse round trips (category <b>c</b>/<b>d</b> below), mirroring
 * {@code MsgPackParser_MaxLength_Test}.
 */
@SuppressWarnings("unchecked")
class ParquetParser_MaxLength_Test extends TestBase {

	// The default row/element-count ceiling (ParquetParserSession.DEFAULT_MAX_COUNT).
	private static final int MAX_NUM_ROWS = 10_000_000;

	public static class SimpleBean {
		public String name;
		public int val;
	}

	private static List<SimpleBean> beans(int n) {
		var list = new ArrayList<SimpleBean>();
		for (var i = 0; i < n; i++) {
			var b = new SimpleBean();
			b.name = "row" + i;
			b.val = i;
			list.add(b);
		}
		return list;
	}

	// ================================================================
	// a. Page rep/def-level length bound (checkPageLength) — HIGH-2 path
	// ================================================================

	@Test
	void a01_pageLengthOversizedRejected() {
		// A tiny page (32 available bytes) declaring a ~2 GB length must be rejected before copyOfRange.
		assertThrowsWithMessage(ParseException.class, "list rep-level",
			() -> ParquetParserSession.checkPageLength(2_000_000_000, 32, "list rep-level", "root.tags.list.element"));
	}

	@Test
	void a02_pageLengthNegativeRejected() {
		// A wrapped-negative LE4 length must be rejected too (never reaches a NegativeArraySizeException).
		assertThrowsWithMessage(ParseException.class, "map def-level",
			() -> ParquetParserSession.checkPageLength(-1, 32, "map def-level", "root.m.key_value.value"));
	}

	@Test
	void a03_pageLengthInRangeAccepted() throws Exception {
		assertEquals(16, ParquetParserSession.checkPageLength(16, 32, "list rep-level", "c"));
	}

	@Test
	void a04_pageLengthBoundaryAccepted() throws Exception {
		// Exactly the available byte count is valid (the whole remainder is the payload).
		assertEquals(32, ParquetParserSession.checkPageLength(32, 32, "list def-level", "c"));
	}

	@Test
	void a05_pageLengthJustOverBoundaryRejected() {
		assertThrowsWithMessage(ParseException.class, "available: 32",
			() -> ParquetParserSession.checkPageLength(33, 32, "list rep-level", "c"));
	}

	// ================================================================
	// b. Row-count capacity clamp (clampCapacity) — CRITICAL-1 defense-in-depth
	// ================================================================

	@Test
	void b01_capacityOversizedClampedToCeiling() {
		// A ~2 GB row count clamps to the configured ceiling instead of driving new ArrayList(2_000_000_000).
		assertEquals(MAX_NUM_ROWS, ParquetParserSession.clampCapacity(2_000_000_000, MAX_NUM_ROWS));
	}

	@Test
	void b02_capacityNegativeClampedToZero() {
		assertEquals(0, ParquetParserSession.clampCapacity(-1, MAX_NUM_ROWS));
	}

	@Test
	void b03_capacityInRangeUnchanged() {
		assertEquals(1_234, ParquetParserSession.clampCapacity(1_234, MAX_NUM_ROWS));
	}

	@Test
	void b04_capacityBoundaryUnchanged() {
		assertEquals(MAX_NUM_ROWS, ParquetParserSession.clampCapacity(MAX_NUM_ROWS, MAX_NUM_ROWS));
	}

	@Test
	void b05_capacityRespectsConfiguredCeiling() {
		// The ceiling itself is now a parameter (not a hardcoded constant): a smaller configured value
		// clamps tighter than the historical default.
		assertEquals(50, ParquetParserSession.clampCapacity(1_000, 50));
	}

	// ================================================================
	// c. Configurable maxLength (per-page byte-size cap)
	// ================================================================

	@Test
	void c01_configurableMaxLengthEnforcedEndToEnd() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(beans(3));

		// A generous cap parses fine.
		var lenient = ParquetParser.create().maxLength(1_000_000).build();
		var parsed = (List<SimpleBean>) lenient.read(bytes, List.class, SimpleBean.class);
		assertEquals(3, parsed.size());

		// A 1-byte cap rejects every page in the file.
		var strict = ParquetParser.create().maxLength(1).build();
		assertThrowsWithMessage(ParseException.class, "Invalid page header",
			() -> strict.read(bytes, List.class, SimpleBean.class));
	}

	@Test
	void c02_maxLengthAffectsCacheKey() {
		// Different maxLength values must NOT collide in the parser cache (hashKey wiring).
		var p1 = ParquetParser.create().maxLength(100).build();
		var p2 = ParquetParser.create().maxLength(200).build();
		var p3 = ParquetParser.create().maxLength(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxLength());
		assertEquals(200, p2.getMaxLength());
	}

	@Test
	void c03_maxLengthNonPositiveDisablesCap() throws Exception {
		// A non-positive cap disables the per-page byte-size check.
		var bytes = ParquetSerializer.DEFAULT.write(beans(3));
		var p = ParquetParser.create().maxLength(0).build();
		var parsed = (List<SimpleBean>) p.read(bytes, List.class, SimpleBean.class);
		assertEquals(3, parsed.size());
	}

	// ================================================================
	// d. Configurable maxCount (row/element-count cap)
	// ================================================================

	@Test
	void d01_configurableMaxCountEnforcedEndToEnd() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(beans(5));

		// A generous cap parses fine.
		var lenient = ParquetParser.create().maxCount(1_000).build();
		var parsed = (List<SimpleBean>) lenient.read(bytes, List.class, SimpleBean.class);
		assertEquals(5, parsed.size());

		// A cap below the actual row count rejects the file up front.
		var strict = ParquetParser.create().maxCount(2).build();
		assertThrowsWithMessage(ParseException.class, "Invalid numRows",
			() -> strict.read(bytes, List.class, SimpleBean.class));
	}

	@Test
	void d02_maxCountAffectsCacheKey() {
		// Different maxCount values must NOT collide in the parser cache (hashKey wiring).
		var p1 = ParquetParser.create().maxCount(100).build();
		var p2 = ParquetParser.create().maxCount(200).build();
		var p3 = ParquetParser.create().maxCount(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxCount());
		assertEquals(200, p2.getMaxCount());
	}

	@Test
	void d03_maxCountNonPositiveDisablesCap() throws Exception {
		// A non-positive cap disables the row-count check.
		var bytes = ParquetSerializer.DEFAULT.write(beans(5));
		var p = ParquetParser.create().maxCount(0).build();
		var parsed = (List<SimpleBean>) p.read(bytes, List.class, SimpleBean.class);
		assertEquals(5, parsed.size());
	}

	// ================================================================
	// e. Configurable maxInputLength (whole-file body-size cap)
	// ================================================================

	@Test
	void e01_configurableMaxInputLengthEnforcedEndToEnd() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(beans(3));

		// A generous cap parses fine.
		var lenient = ParquetParser.create().maxInputLength(1_000_000).build();
		var parsed = (List<SimpleBean>) lenient.read(bytes, List.class, SimpleBean.class);
		assertEquals(3, parsed.size());

		// A cap smaller than the actual input rejects it before the whole body is buffered.
		var strict = ParquetParser.create().maxInputLength(bytes.length - 1).build();
		assertThrowsWithMessage(ParseException.class, "input length exceeds maximum",
			() -> strict.read(bytes, List.class, SimpleBean.class));
	}

	@Test
	void e02_maxInputLengthAffectsCacheKey() {
		// Different maxInputLength values must NOT collide in the parser cache (hashKey wiring).
		var p1 = ParquetParser.create().maxInputLength(100).build();
		var p2 = ParquetParser.create().maxInputLength(200).build();
		var p3 = ParquetParser.create().maxInputLength(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxInputLength());
		assertEquals(200, p2.getMaxInputLength());
	}

	@Test
	void e03_maxInputLengthNonPositiveDisablesCap() throws Exception {
		// A non-positive cap disables the whole-input size check.
		var bytes = ParquetSerializer.DEFAULT.write(beans(3));
		var p = ParquetParser.create().maxInputLength(0).build();
		var parsed = (List<SimpleBean>) p.read(bytes, List.class, SimpleBean.class);
		assertEquals(3, parsed.size());
	}
}
