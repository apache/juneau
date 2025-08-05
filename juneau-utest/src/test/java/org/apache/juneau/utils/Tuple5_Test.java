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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Tuple5_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		Tuple5<String,Integer,Integer,Integer,Integer> x = Tuple5.of("foo",1,2,3,4);
		assertString(x.getA()).is("foo");
		assertInteger(x.getB()).is(1);
		assertInteger(x.getC()).is(2);
		assertInteger(x.getD()).is(3);
		assertInteger(x.getE()).is(4);
	}

	@Test void a02_equality() {
		Tuple5<String,Integer,Integer,Integer,Integer> x1 = Tuple5.of("foo",1,2,3,4), x2 = Tuple5.of("foo",1,2,3,4), x3 = Tuple5.of(null,1,2,3,4), x4 = Tuple5.of("foo",null,2,3,4), x5 = Tuple5.of("foo",1,null,3,4), x6 = Tuple5.of("foo",1,2,null,4), x7 = Tuple5.of("foo",1,2,3,null);
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