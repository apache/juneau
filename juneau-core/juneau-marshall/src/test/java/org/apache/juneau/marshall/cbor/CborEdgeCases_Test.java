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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Edge case tests for CBOR.
 */
class CborEdgeCases_Test extends TestBase {

	@Test
	void h01_maxPositiveInt() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, CborParser.DEFAULT.read(bytes, Integer.class));
		bytes = CborSerializer.DEFAULT.write(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, CborParser.DEFAULT.read(bytes, Long.class));
	}

	@Test
	void h02_minNegativeInt() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(Integer.MIN_VALUE);
		assertEquals(Integer.MIN_VALUE, CborParser.DEFAULT.read(bytes, Integer.class));
	}

	@Test
	void h03_floatSpecialValues() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(Float.NaN);
		var f = CborParser.DEFAULT.read(bytes, Float.class);
		assertTrue(Float.isNaN(f));
		assertEquals(Float.POSITIVE_INFINITY, CborParser.DEFAULT.read(CborSerializer.DEFAULT.write(Float.POSITIVE_INFINITY), Float.class));
		assertEquals(Float.NEGATIVE_INFINITY, CborParser.DEFAULT.read(CborSerializer.DEFAULT.write(Float.NEGATIVE_INFINITY), Float.class));
	}

	@Test
	void h04_doubleSpecialValues() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(Double.NaN);
		var d = CborParser.DEFAULT.read(bytes, Double.class);
		assertTrue(Double.isNaN(d));
	}

	@Test
	void h05_veryLongString() throws Exception {
		var s = "x".repeat(1000);
		var bytes = CborSerializer.DEFAULT.write(s);
		assertEquals(s, CborParser.DEFAULT.read(bytes, String.class));
	}

	@Test
	void h09_emptyBinaryData() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(new byte[0]);
		var parsed = CborParser.DEFAULT.read(bytes, byte[].class);
		assertEquals(0, parsed.length);
	}

	@Test
	void h10_unicodeEdgeCases() throws Exception {
		var s = "\uD83D\uDE00";
		var bytes = CborSerializer.DEFAULT.write(s);
		assertEquals(s, CborParser.DEFAULT.read(bytes, String.class));
	}

	@Test
	void h12_optionalProperties() throws Exception {
		var m = JsonMap.of("x", o(42));
		var bytes = CborSerializer.DEFAULT.write(m);
		var parsed = CborParser.DEFAULT.read(bytes, JsonMap.class);
		assertNotNull(parsed.get("x"));
	}

	@Test
	void h13_unknownTags() throws Exception {
		var tagged = fromHex("C16161");
		var parsed = CborParser.DEFAULT.read(tagged, String.class);
		assertEquals("a", parsed);
	}

	@Test
	void h14_zeroLengthInput() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(null);
		var parsed = CborParser.DEFAULT.read(bytes, Object.class);
		assertNull(parsed);
	}
}
