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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/** Tests for {@link BooleanFunctions}. */
class BooleanFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(BooleanFunctions.ALL).build();

	@Test void and_allTrue() { assertEquals("true", vr.resolve("#{and(true, true, true)}")); }
	@Test void and_anyFalse() { assertEquals("false", vr.resolve("#{and(true, false, true)}")); }
	@Test void or_anyTrue() { assertEquals("true", vr.resolve("#{or(false, true, false)}")); }
	@Test void or_allFalse() { assertEquals("false", vr.resolve("#{or(false, false, false)}")); }

	@Test void not_true() { assertEquals("false", vr.resolve("#{not(true)}")); }
	@Test void not_false() { assertEquals("true", vr.resolve("#{not(false)}")); }
	@Test void xor_diff() { assertEquals("true", vr.resolve("#{xor(true, false)}")); }
	@Test void xor_same() { assertEquals("false", vr.resolve("#{xor(true, true)}")); }

	@Test void eq_true() { assertEquals("true", vr.resolve("#{eq(hello, hello)}")); }
	@Test void eq_false() { assertEquals("false", vr.resolve("#{eq(hello, world)}")); }
	@Test void neq_true() { assertEquals("true", vr.resolve("#{neq(hello, world)}")); }
	@Test void neq_false() { assertEquals("false", vr.resolve("#{neq(hello, hello)}")); }

	@Test void lt() { assertEquals("true", vr.resolve("#{lt(2, 5)}")); }
	@Test void lte_eq() { assertEquals("true", vr.resolve("#{lte(5, 5)}")); }
	@Test void gt() { assertEquals("true", vr.resolve("#{gt(5, 2)}")); }
	@Test void gte_eq() { assertEquals("true", vr.resolve("#{gte(5, 5)}")); }
}
