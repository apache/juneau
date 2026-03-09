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
package org.apache.juneau.json;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class JcsNumbers_Test extends TestBase {

	@Test
	void a01_zero() throws Exception {
		assertEquals("0", JcsSerializer.DEFAULT.serialize(0.0));
	}

	@Test
	void a02_negativeZero() throws Exception {
		assertEquals("0", JcsSerializer.DEFAULT.serialize(-0.0));
	}

	@Test
	void a03_minPosNumber() throws Exception {
		// Double.MIN_VALUE is the smallest positive double (~4.9e-324)
		assertEquals("4.9e-324", JcsSerializer.DEFAULT.serialize(Double.MIN_VALUE));
	}

	@Test
	void a04_minNegNumber() throws Exception {
		assertEquals("-4.9e-324", JcsSerializer.DEFAULT.serialize(-Double.MIN_VALUE));
	}

	@Test
	void a05_maxPosNumber() throws Exception {
		assertEquals("1.7976931348623157e+308", JcsSerializer.DEFAULT.serialize(Double.MAX_VALUE));
	}

	@Test
	void a06_maxNegNumber() throws Exception {
		assertEquals("-1.7976931348623157e+308", JcsSerializer.DEFAULT.serialize(-Double.MAX_VALUE));
	}

	@Test
	void a07_maxPosInt() throws Exception {
		assertEquals("9007199254740992", JcsSerializer.DEFAULT.serialize(9007199254740992L));
	}

	@Test
	void a08_maxNegInt() throws Exception {
		assertEquals("-9007199254740992", JcsSerializer.DEFAULT.serialize(-9007199254740992L));
	}

	@Test
	void a09_largeInteger() throws Exception {
		// In [1e-6, 1e21) so ECMAScript uses fixed notation
		assertEquals("295147905179352830000", JcsSerializer.DEFAULT.serialize(295147905179352830000.0));
	}

	@Test
	void a10_noTrailingZeros() throws Exception {
		assertEquals("4.5", JcsSerializer.DEFAULT.serialize(4.50));
	}

	@Test
	void a11_scientificNotation() throws Exception {
		// RFC 8785: 1E30 → 1e+30
		assertEquals("1e+30", JcsSerializer.DEFAULT.serialize(1E30));
	}

	@Test
	void a12_smallDecimal() throws Exception {
		assertEquals("0.002", JcsSerializer.DEFAULT.serialize(2e-3));
	}

	@Test
	void a13_verySmallDecimal() throws Exception {
		// RFC 8785: 0.000...001 → 1e-27
		assertEquals("1e-27", JcsSerializer.DEFAULT.serialize(0.000000000000000000000000001));
	}

	@Test
	void a14_rejectNaN() {
		assertThrows(SerializeException.class, () -> JcsSerializer.DEFAULT.serialize(Double.NaN));
	}

	@Test
	void a15_rejectInfinity() {
		assertThrows(SerializeException.class, () -> JcsSerializer.DEFAULT.serialize(Double.POSITIVE_INFINITY));
	}

	@Test
	void a16_rejectNegInfinity() {
		assertThrows(SerializeException.class, () -> JcsSerializer.DEFAULT.serialize(Double.NEGATIVE_INFINITY));
	}

	@Test
	void a17_integerNoDecimalPoint() throws Exception {
		assertEquals("42", JcsSerializer.DEFAULT.serialize(42.0));
	}

	@Test
	void a18_roundToEven() throws Exception {
		// In [1e-6, 1e21) so ECMAScript uses fixed notation
		assertEquals("1424953923781206.2", JcsSerializer.DEFAULT.serialize(1424953923781206.2));
	}

	@Test
	void a19_edgeCasePrecision() throws Exception {
		// RFC 8785 Section 3.2.2: 333333333.33333329 → 333333333.3333333
		assertEquals("333333333.3333333", JcsSerializer.DEFAULT.serialize(333333333.33333329));
	}

	@Test
	void a20_floatValues() throws Exception {
		// Float promoted to double; 42f serializes as integer
		assertEquals("42", JcsSerializer.DEFAULT.serialize(42f));
		assertEquals("0", JcsSerializer.DEFAULT.serialize(0f));
	}
}
