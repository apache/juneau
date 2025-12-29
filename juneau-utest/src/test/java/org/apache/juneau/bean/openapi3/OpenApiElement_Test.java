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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link OpenApiElement}.
 * Most of the contents of this class are covered in the subclass tests.
 */
class OpenApiElement_Test extends TestBase {

	@Test void a01_basic() {
		var x = new OpenApiElement() {};
		x.set("a1", "a2");
		assertString("{'a1':'a2'}".replace('\'','"'), x);
	}

	@Test void a02_strict_withNull() {
		// Test strict(Object) with null value - should set strict to false
		var x = new OpenApiElement() {};
		x.strict(null);
		assertFalse(x.isStrict());
	}

	@Test void a03_strict_withTrue() {
		// Test strict(Object) with true value
		var x = new OpenApiElement() {};
		x.strict(true);
		assertTrue(x.isStrict());
	}

	@Test void a04_strict_withFalse() {
		// Test strict(Object) with false value
		var x = new OpenApiElement() {};
		x.strict(false);
		assertFalse(x.isStrict());
	}

	@Test void a05_strict_withStringTrue() {
		// Test strict(Object) with string "true"
		var x = new OpenApiElement() {};
		x.strict("true");
		assertTrue(x.isStrict());
	}

	@Test void a06_strict_withStringFalse() {
		// Test strict(Object) with string "false"
		var x = new OpenApiElement() {};
		x.strict("false");
		assertFalse(x.isStrict());
	}
}