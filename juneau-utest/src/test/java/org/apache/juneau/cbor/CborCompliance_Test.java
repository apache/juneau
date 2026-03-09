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
package org.apache.juneau.cbor;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * RFC 8949 Appendix A compliance tests.
 */
class CborCompliance_Test extends TestBase {

	private static void enc(Object o, String expected) throws Exception {
		var b = CborSerializer.DEFAULT.serialize(o);
		assertEquals(expected, toSpacedHex(b));
	}

	@Test
	void g01_integer_0() throws Exception { enc(0, "00"); }

	@Test
	void g02_integer_1() throws Exception { enc(1, "01"); }

	@Test
	void g03_integer_23() throws Exception { enc(23, "17"); }

	@Test
	void g04_integer_24() throws Exception { enc(24, "18 18"); }

	@Test
	void g05_integer_100() throws Exception { enc(100, "18 64"); }

	@Test
	void g06_integer_1000() throws Exception { enc(1000, "19 03 E8"); }

	@Test
	void g07_integer_1000000() throws Exception { enc(1000000, "1A 00 0F 42 40"); }

	@Test
	void g08_negativeInteger_1() throws Exception { enc(-1, "20"); }

	@Test
	void g09_negativeInteger_100() throws Exception { enc(-100, "38 63"); }

	@Test
	void g10_float_0_0() throws Exception { enc(0.0f, "FA 00 00 00 00"); }

	@Test
	void g11_float_1_1() throws Exception { enc(1.1, "FB 3F F1 99 99 99 99 99 9A"); }

	@Test
	void g12_float_infinity() throws Exception { enc(Double.POSITIVE_INFINITY, "FB 7F F0 00 00 00 00 00 00"); }

	@Test
	void g13_float_nan() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(Double.NaN);
		assertEquals(9, bytes.length);
		assertEquals(0xFB, bytes[0] & 0xFF);
	}

	@Test
	void g14_emptyString() throws Exception { enc("", "60"); }

	@Test
	void g15_shortString() throws Exception { enc("a", "61 61"); }

	@Test
	void g16_emptyArray() throws Exception { enc(ints(), "80"); }

	@Test
	void g17_simpleArray() throws Exception { enc(ints(1, 2, 3), "83 01 02 03"); }

	@Test
	void g18_emptyMap() throws Exception { enc(JsonMap.ofJson("{}"), "A0"); }

	@Test
	void g19_false() throws Exception { enc(false, "F4"); }

	@Test
	void g20_true() throws Exception { enc(true, "F5"); }

	@Test
	void g21_null() throws Exception { enc(null, "F6"); }
}
