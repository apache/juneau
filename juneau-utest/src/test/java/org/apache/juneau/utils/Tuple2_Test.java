// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

public class Tuple2_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		Tuple2<String,Integer> x = Tuple2.of("foo",1);
		assertEquals("foo", x.getA());
		assertEquals(1, x.getB());
	}

	@Test void a02_equality() {
		Tuple2<String,Integer> x1 = Tuple2.of("foo",1), x2 = Tuple2.of("foo",1), x3 = Tuple2.of(null,1), x4 = Tuple2.of("foo",null);
		assertEquals(x1, x2);
		assertEquals(x1.hashCode(), x2.hashCode());
		assertNotEquals(x1, x3);
		assertNotEquals(x1.hashCode(), x3.hashCode());
		assertNotEquals(x1, x4);
		assertNotEquals(x1.hashCode(), x4.hashCode());
	}
}