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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.junit.bct.BctUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link BctUtils} utility class.
 *
 * <p>This test class validates BCT-specific utility methods including assertion helpers,
 * tokenization, and convenience aliases for common operations.
 */
@DisplayName("BctUtils")
class BctUtils_Test extends TestBase {

	// ====================================================================================================
	// Assertion Helper Tests
	// ====================================================================================================

	@Test
	@DisplayName("assertEqualsFailed() - Basic error creation")
	void a01_assertEqualsFailedBasicCreation() {
		var error = assertEqualsFailed("expected", "actual", null);

		assertEquals("Equals assertion failed. ==> expected: <expected> but was: <actual>", error.getMessage());
		assertEquals("expected", error.getExpected().getValue());
		assertEquals("actual", error.getActual().getValue());
	}

	@Test
	@DisplayName("assertEqualsFailed() - With custom message")
	void a02_assertEqualsFailedCustomMessage() {
		var error = assertEqualsFailed(100, 200, () -> "Custom validation failed");

		assertEquals("Custom validation failed ==> expected: <100> but was: <200>", error.getMessage());
	}

	@Test
	@DisplayName("assertEqualsFailed() - Null values")
	void a03_assertEqualsFailedNullValues() {
		// Null expected
		var error1 = assertEqualsFailed(null, "actual", null);
		assertEquals("Equals assertion failed. ==> expected: <null> but was: <actual>", error1.getMessage());

		// Null actual
		var error2 = assertEqualsFailed("expected", null, null);
		assertEquals("Equals assertion failed. ==> expected: <expected> but was: <null>", error2.getMessage());

		// Both null
		var error3 = assertEqualsFailed(null, null, null);
		assertEquals("Equals assertion failed. ==> expected: <null> but was: <null>", error3.getMessage());
	}

	// ====================================================================================================
	// Tokenization Tests
	// ====================================================================================================

	@Test
	@DisplayName("tokenize() - Basic tokenization")
	void d01_tokenizeBasic() {
		var tokens = tokenize("name,age,city");
		assertList(tokens.stream().map(t -> t.getValue()).toList(), "name", "age", "city");

		// Each should have no nested fields
		tokens.forEach(t -> assertEmpty(t.getNested()));
	}

	@Test
	@DisplayName("tokenize() - Nested fields")
	void d02_tokenizeNested() {
		var tokens = tokenize("name,address{street,city,zip},age");

		assertEquals(3, tokens.size());
		assertEquals("name", tokens.get(0).getValue());
		assertEmpty(tokens.get(0).getNested());

		assertEquals("address", tokens.get(1).getValue());
		var nested = tokens.get(1).getNested();
		assertList(nested.stream().map(t -> t.getValue()).toList(), "street", "city", "zip");

		assertEquals("age", tokens.get(2).getValue());
		assertEmpty(tokens.get(2).getNested());
	}
}

