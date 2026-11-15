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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.StringUtils.parseNumber;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests the length cap on integer parsing.
 */
class StringUtils_BigInteger_Test extends TestBase {

	@Test void a01_shortValueParsed() {
		assertEquals(new BigInteger("12345678901234567890"), parseNumber("12345678901234567890", BigInteger.class));
	}

	@Test void a02_overlongValueRejected() {
		var s = "1".repeat(5000);
		assertThrows(NumberFormatException.class, () -> parseNumber(s, BigInteger.class));
	}
}
