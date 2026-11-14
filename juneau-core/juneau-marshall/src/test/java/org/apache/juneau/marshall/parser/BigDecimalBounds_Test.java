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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests that decimal parsing rejects values with very large exponents or very long digit strings.
 */
class BigDecimalBounds_Test extends TestBase {

	@Test void a01_normalValues() {
		assertEquals(new BigDecimal("1.5e100"), parseBigDecimal("1.5e100"));
		assertEquals(new BigDecimal("12345.678"), parseBigDecimal("12345.678"));
		assertEquals(new BigDecimal("1e10000"), parseBigDecimal("1e10000"));
	}

	@Test void a02_hugeExponentRejected() {
		assertThrows(NumberFormatException.class, () -> parseBigDecimal("1e100000"));
		assertThrows(NumberFormatException.class, () -> parseBigDecimal("1e-100000"));
		assertThrows(NumberFormatException.class, () -> parseNumber("1e999999999", BigDecimal.class));
	}

	@Test void a03_tooLongRejected() {
		var tooLong = "1".repeat(1001);
		assertThrows(NumberFormatException.class, () -> parseBigDecimal(tooLong));
	}
}
