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
package org.apache.juneau.commons.function;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Tuple5_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var x = Tuple5.of("foo",1,2,3,4);
		assertEquals("foo", x.getA());
		assertEquals(1, x.getB());
		assertEquals(2, x.getC());
		assertEquals(3, x.getD());
		assertEquals(4, x.getE());
	}

	@Test void a02_equality() {
		var x1 = Tuple5.of("foo",1,2,3,4);
		var x2 = Tuple5.of("foo",1,2,3,4);
		var x3 = Tuple5.of(null,1,2,3,4);
		var x4 = Tuple5.of("foo",null,2,3,4);
		var x5 = Tuple5.of("foo",1,null,3,4);
		var x6 = Tuple5.of("foo",1,2,null,4);
		var x7 = Tuple5.of("foo",1,2,3,null);
		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1, x3);
		assertNotEquals(x1.hashCode(), x3.hashCode());
		assertNotEquals(x1, x4);
		assertNotEquals(x1.hashCode(), x4.hashCode());
		assertNotEquals(x1, x5);
		assertNotEquals(x1.hashCode(), x5.hashCode());
		assertNotEquals(x1, x6);
		assertNotEquals(x1.hashCode(), x6.hashCode());
		assertNotEquals(x1, x7);
		assertNotEquals(x1.hashCode(), x7.hashCode());
	}
}