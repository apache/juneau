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

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link ArithmeticFunctions}. */
class ArithmeticFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(ArithmeticFunctions.ALL).build();

	@Test void add_int() { assertEquals("5", vr.resolve("#{add(2, 3)}")); }
	@Test void add_float() { assertEquals("5.5", vr.resolve("#{add(2.0, 3.5)}")); }
	@Test void subtract() { assertEquals("3", vr.resolve("#{subtract(10, 7)}")); }
	@Test void multiply() { assertEquals("21", vr.resolve("#{multiply(3, 7)}")); }
	@Test void divide() { assertEquals("2.5", vr.resolve("#{divide(5, 2)}")); }
	@Test void divide_intResult() { assertEquals("3", vr.resolve("#{divide(9, 3)}")); }
	@Test void modulo() { assertEquals("1", vr.resolve("#{modulo(10, 3)}")); }
	@Test void min() { assertEquals("2", vr.resolve("#{min(5, 2)}")); }
	@Test void max() { assertEquals("5", vr.resolve("#{max(5, 2)}")); }
	@Test void abs_neg() { assertEquals("5", vr.resolve("#{abs(-5)}")); }
	@Test void abs_pos() { assertEquals("5", vr.resolve("#{abs(5)}")); }
}
