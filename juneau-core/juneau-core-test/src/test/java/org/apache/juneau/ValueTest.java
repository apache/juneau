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
package org.apache.juneau;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.internal.*;
import org.junit.*;

/**
 * Validates the {@link Value} class.
 */
public class ValueTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Value defined on parent class.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A extends Value<A1>{}
	public static class A1 {}

	@Test
	public void testSubclass() {
		assertEquals(A1.class, Value.getParameterType(A.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Value used in parameter.
	//-----------------------------------------------------------------------------------------------------------------
	public static class B {
		public void b(Value<B1> b1) {};
	}
	public static class B1 {}

	@Test
	public void testOnParameter() throws Exception {
		assertEquals(B1.class, Value.getParameterType(B.class.getMethod("b", Value.class), 0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Value used on parameter of parameterized-type.
	//-----------------------------------------------------------------------------------------------------------------
	public interface C {
		void m1(Value<List<Integer>> v);
	}

	@Test
	public void testOnParameterType() throws Exception {
		Type t = Value.getParameterType(C.class.getMethod("m1", Value.class), 0);
		assertEquals("List<Integer>", ClassUtils.getSimpleName(t));
	}
}
