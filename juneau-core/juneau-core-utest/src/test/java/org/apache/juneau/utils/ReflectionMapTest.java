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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReflectionMapTest {

	private static ReflectionMap.Builder<Number> create() {
		return ReflectionMap.create(Number.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Class names
	//------------------------------------------------------------------------------------------------------------------

	static class A1 {}
	static class A2 {}

	static ReflectionMap<Number>
		RM_A_SIMPLE = create().append("A1", 1).build(),
		RM_A_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$A1", 1).build();

	@Test
	public void a01_simpleClassName_find() {
		assertObjectEquals("[1]", RM_A_SIMPLE.find(A1.class));
		assertObjectEquals("[]", RM_A_SIMPLE.find(A2.class));
	}

	@Test
	public void a02_simpleClassName_findType() {
		assertObjectEquals("[1]", RM_A_SIMPLE.find(A1.class, Integer.class));
		assertObjectEquals("[]", RM_A_SIMPLE.find(A1.class, Long.class));
		assertObjectEquals("[]", RM_A_SIMPLE.find(A2.class, Integer.class));
	}

	@Test
	public void a03_simpleClassName_findFirst() {
		assertObjectEquals("1", RM_A_SIMPLE.findFirst(A1.class));
		assertObjectEquals("null", RM_A_SIMPLE.findFirst(A2.class));
	}

	@Test
	public void a04_simpleClassName_findFirstType() {
		assertObjectEquals("1", RM_A_SIMPLE.findFirst(A1.class, Integer.class));
		assertObjectEquals("null", RM_A_SIMPLE.findFirst(A1.class, Long.class));
		assertObjectEquals("null", RM_A_SIMPLE.findFirst(A2.class, Integer.class));
	}

	@Test
	public void a05_fullClassName_find() {
		assertObjectEquals("[1]", RM_A_FULL.find(A1.class));
		assertObjectEquals("[]", RM_A_FULL.find(A2.class));
	}

	@Test
	public void a06_fullClassName_findType() {
		assertObjectEquals("[1]", RM_A_FULL.find(A1.class, Integer.class));
		assertObjectEquals("[]", RM_A_FULL.find(A1.class, Long.class));
		assertObjectEquals("[]", RM_A_FULL.find(A2.class, Integer.class));
	}

	@Test
	public void a07_fullClassName_findFirst() {
		assertObjectEquals("1", RM_A_FULL.findFirst(A1.class));
		assertObjectEquals("null", RM_A_FULL.findFirst(A2.class));
	}

	@Test
	public void a08_fullClassName_findFirstType() {
		assertObjectEquals("1", RM_A_FULL.findFirst(A1.class, Integer.class));
		assertObjectEquals("null", RM_A_FULL.findFirst(A1.class, Long.class));
		assertObjectEquals("null", RM_A_FULL.findFirst(A2.class, Integer.class));
	}

	@Test
	public void a09_nullClass() {
		assertObjectEquals("[]", RM_A_SIMPLE.find((Class<?>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method names
	//------------------------------------------------------------------------------------------------------------------

	static class B1 {
		public void f1() {}
		public void f2() {}
	}
	static class B2 {
		public void f1() {}
		public void f2() {}
	}

	static ReflectionMap<Number>
		RM_B_SIMPLE = create().append("B1.f1", 1).build(),
		RM_B_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.f1", 1).build();

	@Test
	public void b01_simpleMethodName_find() throws Exception {
		assertObjectEquals("[1]", RM_B_SIMPLE.find(B1.class.getMethod("f1")));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B1.class.getMethod("f2")));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B2.class.getMethod("f1")));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B2.class.getMethod("f2")));
	}

	@Test
	public void b02_simpleMethodName_findType() throws Exception {
		assertObjectEquals("[1]", RM_B_SIMPLE.find(B1.class.getMethod("f1"), Integer.class));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B1.class.getMethod("f1"), Long.class));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B1.class.getMethod("f2"), Integer.class));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B2.class.getMethod("f1"), Integer.class));
		assertObjectEquals("[]", RM_B_SIMPLE.find(B2.class.getMethod("f2"), Integer.class));
	}

	@Test
	public void b03_simpleMethodName_findFirst() throws Exception {
		assertObjectEquals("1", RM_B_SIMPLE.findFirst(B1.class.getMethod("f1")));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B1.class.getMethod("f2")));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B2.class.getMethod("f1")));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B2.class.getMethod("f2")));
	}

	@Test
	public void b04_simpleMethodName_findFirstType() throws Exception {
		assertObjectEquals("1", RM_B_SIMPLE.findFirst(B1.class.getMethod("f1"), Integer.class));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B1.class.getMethod("f1"), Long.class));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B1.class.getMethod("f2"), Integer.class));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B2.class.getMethod("f1"), Integer.class));
		assertObjectEquals("null", RM_B_SIMPLE.findFirst(B2.class.getMethod("f2"), Integer.class));
	}

	@Test
	public void b05_fullMethodName_find() throws Exception {
		assertObjectEquals("[1]", RM_B_FULL.find(B1.class.getMethod("f1")));
		assertObjectEquals("[]", RM_B_FULL.find(B1.class.getMethod("f2")));
		assertObjectEquals("[]", RM_B_FULL.find(B2.class.getMethod("f1")));
		assertObjectEquals("[]", RM_B_FULL.find(B2.class.getMethod("f2")));
	}

	@Test
	public void b06_fullMethodName_findType() throws Exception {
		assertObjectEquals("[1]", RM_B_FULL.find(B1.class.getMethod("f1"), Integer.class));
		assertObjectEquals("[]", RM_B_FULL.find(B1.class.getMethod("f1"), Long.class));
		assertObjectEquals("[]", RM_B_FULL.find(B1.class.getMethod("f2"), Integer.class));
		assertObjectEquals("[]", RM_B_FULL.find(B2.class.getMethod("f1"), Integer.class));
		assertObjectEquals("[]", RM_B_FULL.find(B2.class.getMethod("f2"), Integer.class));
	}

	@Test
	public void b07_fullMethodName_findFirst() throws Exception {
		assertObjectEquals("1", RM_B_FULL.findFirst(B1.class.getMethod("f1")));
		assertObjectEquals("null", RM_B_FULL.findFirst(B1.class.getMethod("f2")));
		assertObjectEquals("null", RM_B_FULL.findFirst(B2.class.getMethod("f1")));
		assertObjectEquals("null", RM_B_FULL.findFirst(B2.class.getMethod("f2")));
	}

	@Test
	public void b08_fullMethodName_findFirstType() throws Exception {
		assertObjectEquals("1", RM_B_FULL.findFirst(B1.class.getMethod("f1"), Integer.class));
		assertObjectEquals("null", RM_B_FULL.findFirst(B1.class.getMethod("f1"), Long.class));
		assertObjectEquals("null", RM_B_FULL.findFirst(B1.class.getMethod("f2"), Integer.class));
		assertObjectEquals("null", RM_B_FULL.findFirst(B2.class.getMethod("f1"), Integer.class));
		assertObjectEquals("null", RM_B_FULL.findFirst(B2.class.getMethod("f2"), Integer.class));
	}

	@Test
	public void b09_nullMethod() {
		assertObjectEquals("[]", RM_B_SIMPLE.find((Method)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Invalid input
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_blankInput() throws Exception {
		try {
			create().append("", 1);
			fail();
		} catch (RuntimeException e) {
			assertEquals("Invalid reflection signature: []", e.getMessage());
		}
	}

	@Test
	public void c02_nullInput() throws Exception {
		try {
			create().append(null, 1);
			fail();
		} catch (RuntimeException e) {
			assertEquals("Invalid reflection signature: [null]", e.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comma-delimited list.
	//------------------------------------------------------------------------------------------------------------------
	static class D1 {}

	static ReflectionMap<Number> RM_D = create().append("D2, D1", 1).build();

	@Test
	public void d01_cdl() {
		assertObjectEquals("[1]", RM_D.find(D1.class));
	}
}
