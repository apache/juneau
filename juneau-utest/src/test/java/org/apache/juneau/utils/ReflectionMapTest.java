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
import static org.junit.runners.MethodSorters.*;

import java.lang.reflect.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ReflectionMapTest {

	private static ReflectionMap.Builder<Number> create() {
		return ReflectionMap.create(Number.class);
	}

	private void checkEntries(ReflectionMap<?> m, boolean hasClass, boolean hasMethods, boolean hasFields, boolean hasConstructors) {
		assertEquals(m.classEntries.length == 0, ! hasClass);
		assertEquals(m.methodEntries.length == 0, ! hasMethods);
		assertEquals(m.fieldEntries.length == 0, ! hasFields);
		assertEquals(m.constructorEntries.length == 0, ! hasConstructors);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Class names
	//------------------------------------------------------------------------------------------------------------------

	static class A1 {}
	static class A2 {}

	static ReflectionMap<Number>
		A1_SIMPLE = create().append("A1", 1).build(),
		A1b_SIMPLE = create().append("ReflectionMapTest$A1", 1).build(),
		A1_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$A1", 1).build();  // Note this could be a static field.

	@Test
	public void a01_classNames_checkEntries() {
		checkEntries(A1_SIMPLE, true, false, false, false);
		checkEntries(A1_FULL, true, false, true, false);
	}

	private void checkA(Class<?> c, boolean match_A1_SIMPLE, boolean match_A1b_SIMPLE, boolean match_A1_FULL) {
		assertEquals(match_A1_SIMPLE, A1_SIMPLE.find(c, null).isPresent());
		assertEquals(match_A1b_SIMPLE, A1b_SIMPLE.find(c, null).isPresent());
		assertEquals(match_A1_FULL, A1_FULL.find(c, null).isPresent());

		assertEquals(match_A1_SIMPLE, A1_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_A1b_SIMPLE, A1b_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_A1_FULL, A1_FULL.find(c, Integer.class).isPresent());

		assertFalse(A1_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(A1b_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(A1_FULL.find(c, Long.class).isPresent());

		assertEquals(match_A1_SIMPLE, !A1_SIMPLE.findAll(c, null).isEmpty());
		assertEquals(match_A1b_SIMPLE, !A1b_SIMPLE.findAll(c, null).isEmpty());
		assertEquals(match_A1_FULL, !A1_FULL.findAll(c, null).isEmpty());

		assertEquals(match_A1_SIMPLE, !A1_SIMPLE.findAll(c, Integer.class).isEmpty());
		assertEquals(match_A1b_SIMPLE, !A1b_SIMPLE.findAll(c, Integer.class).isEmpty());
		assertEquals(match_A1_FULL, !A1_FULL.findAll(c, Integer.class).isEmpty());

		assertFalse(A1_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(A1b_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(A1_FULL.find(c, Long.class).isPresent());
	}

	@Test
	public void a02_classNames_find() {
		checkA(A1.class, true, true, true);
		checkA(A2.class, false, false, false);
		checkA(null, false, false, false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method names
	//------------------------------------------------------------------------------------------------------------------

	static class B1 {
		public void m1() {}
		public void m1(int x) {}
		public void m1(String x) {}
		public void m1(String x, int y) {}
		public void m2(int x) {}
	}
	static class B2 {
		public void m1() {}
	}

	static ReflectionMap<Number>
		B1m1_SIMPLE = create().append("B1.m1", 1).build(),
		B1m1i_SIMPLE = create().append("B1.m1(int)", 1).build(),
		B1m1s_SIMPLE = create().append("B1.m1(String)", 1).build(),
		B1m1ss_SIMPLE = create().append("B1.m1(java.lang.String)", 1).build(),
		B1m1si_SIMPLE = create().append("B1.m1(String,int)", 1).build(),
		B1m1ssi_SIMPLE = create().append("B1.m1(java.lang.String , int)", 1).build(),
		B1m1_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.m1", 1).build(),
		B1m1i_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.m1(int)", 1).build(),
		B1m1s_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.m1(String)", 1).build(),
		B1m1ss_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.m1(java.lang.String)", 1).build(),
		B1m1si_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.m1(String,int)", 1).build(),
		B1m1ssi_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$B1.m1(java.lang.String , int)", 1).build();

	@Test
	public void b01_methodNames_checkEntries() {
		checkEntries(B1m1_SIMPLE, false, true, true, false);
		checkEntries(B1m1i_SIMPLE, false, true, false, false);
		checkEntries(B1m1s_SIMPLE, false, true, false, false);
		checkEntries(B1m1ss_SIMPLE, false, true, false, false);
		checkEntries(B1m1si_SIMPLE, false, true, false, false);
		checkEntries(B1m1ssi_SIMPLE, false, true, false, false);
		checkEntries(B1m1_FULL, false, true, true, false);
		checkEntries(B1m1i_FULL, false, true, false, false);
		checkEntries(B1m1s_FULL, false, true, false, false);
		checkEntries(B1m1ss_FULL, false, true, false, false);
		checkEntries(B1m1si_FULL, false, true, false, false);
		checkEntries(B1m1ssi_FULL, false, true, false, false);
	}

	private void checkB(Method m, boolean match_B1m1_SIMPLE, boolean match_B1m1i_SIMPLE, boolean match_B1m1s_SIMPLE, boolean match_B1m1ss_SIMPLE,
			boolean match_B1m1si_SIMPLE, boolean match_B1m1ssi_SIMPLE, boolean match_B1m1_FULL, boolean match_B1m1i_FULL, boolean match_B1m1s_FULL,
			boolean match_B1m1ss_FULL, boolean match_B1m1si_FULL, boolean match_B1m1ssi_FULL) {

		assertEquals(match_B1m1_SIMPLE, B1m1_SIMPLE.find(m, null).isPresent());
		assertEquals(match_B1m1i_SIMPLE, B1m1i_SIMPLE.find(m, null).isPresent());
		assertEquals(match_B1m1s_SIMPLE, B1m1s_SIMPLE.find(m, null).isPresent());
		assertEquals(match_B1m1ss_SIMPLE, B1m1ss_SIMPLE.find(m, null).isPresent());
		assertEquals(match_B1m1si_SIMPLE, B1m1si_SIMPLE.find(m, null).isPresent());
		assertEquals(match_B1m1ssi_SIMPLE, B1m1ssi_SIMPLE.find(m, null).isPresent());
		assertEquals(match_B1m1_FULL, B1m1_FULL.find(m, null).isPresent());
		assertEquals(match_B1m1i_FULL, B1m1i_FULL.find(m, null).isPresent());
		assertEquals(match_B1m1s_FULL, B1m1s_FULL.find(m, null).isPresent());
		assertEquals(match_B1m1ss_FULL, B1m1ss_FULL.find(m, null).isPresent());
		assertEquals(match_B1m1si_FULL, B1m1si_FULL.find(m, null).isPresent());
		assertEquals(match_B1m1ssi_FULL, B1m1ssi_FULL.find(m, null).isPresent());

		assertEquals(match_B1m1_SIMPLE, B1m1_SIMPLE.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1i_SIMPLE, B1m1i_SIMPLE.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1s_SIMPLE, B1m1s_SIMPLE.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1ss_SIMPLE, B1m1ss_SIMPLE.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1si_SIMPLE, B1m1si_SIMPLE.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1ssi_SIMPLE, B1m1ssi_SIMPLE.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1_FULL, B1m1_FULL.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1i_FULL, B1m1i_FULL.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1s_FULL, B1m1s_FULL.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1ss_FULL, B1m1ss_FULL.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1si_FULL, B1m1si_FULL.find(m, Integer.class).isPresent());
		assertEquals(match_B1m1ssi_FULL, B1m1ssi_FULL.find(m, Integer.class).isPresent());

		assertFalse(B1m1_SIMPLE.find(m, Long.class).isPresent());
		assertFalse(B1m1i_SIMPLE.find(m, Long.class).isPresent());
		assertFalse(B1m1s_SIMPLE.find(m, Long.class).isPresent());
		assertFalse(B1m1ss_SIMPLE.find(m, Long.class).isPresent());
		assertFalse(B1m1si_SIMPLE.find(m, Long.class).isPresent());
		assertFalse(B1m1ssi_SIMPLE.find(m, Long.class).isPresent());
		assertFalse(B1m1_FULL.find(m, Long.class).isPresent());
		assertFalse(B1m1i_FULL.find(m, Long.class).isPresent());
		assertFalse(B1m1s_FULL.find(m, Long.class).isPresent());
		assertFalse(B1m1ss_FULL.find(m, Long.class).isPresent());
		assertFalse(B1m1si_FULL.find(m, Long.class).isPresent());
		assertFalse(B1m1ssi_FULL.find(m, Long.class).isPresent());
	}

	@Test
	public void b02_methodName_find() throws Exception {
		checkB(B1.class.getMethod("m1"), true, false, false, false, false, false, true, false, false, false, false, false);
		checkB(B1.class.getMethod("m1", int.class), true, true, false, false, false, false, true, true, false, false, false, false);
		checkB(B1.class.getMethod("m1", String.class), true, false, true, true, false, false, true, false, true, true, false, false);
		checkB(B1.class.getMethod("m1", String.class, int.class), true, false, false, false, true, true, true, false, false, false, true, true);
		checkB(B1.class.getMethod("m2", int.class), false, false, false, false, false, false, false, false, false, false, false, false);
		checkB(B2.class.getMethod("m1"), false, false, false, false, false, false, false, false, false, false, false, false);
		checkB(null, false, false, false, false, false, false, false, false, false, false, false, false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Field names
	//------------------------------------------------------------------------------------------------------------------

	static class C1 {
		public int f1;
		public int f2;
	}
	static class C2 {
		public int f1;
	}

	static ReflectionMap<Number>
		C1f1_SIMPLE = create().append("C1.f1", 1).build(),
		C1f1_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$C1.f1", 1).build();

	@Test
	public void c01_fieldNames_checkEntries() {
		checkEntries(C1f1_SIMPLE, false, true, true, false);
		checkEntries(C1f1_FULL, false, true, true, false);
	}

	private void checkC(Field f, boolean match_C1f1_SIMPLE, boolean match_C1f1_FULL) {
		assertEquals(match_C1f1_SIMPLE, C1f1_SIMPLE.find(f, null).isPresent());
		assertEquals(match_C1f1_FULL, C1f1_FULL.find(f, null).isPresent());

		assertEquals(match_C1f1_SIMPLE, C1f1_SIMPLE.find(f, Integer.class).isPresent());
		assertEquals(match_C1f1_FULL, C1f1_FULL.find(f, Integer.class).isPresent());

		assertFalse(C1f1_SIMPLE.find(f, Long.class).isPresent());
		assertFalse(C1f1_FULL.find(f, Long.class).isPresent());
	}

	@Test
	public void c02_fieldName_find() throws Exception {
		checkC(C1.class.getField("f1"), true, true);
		checkC(C1.class.getField("f2"), false, false);
		checkC(C2.class.getField("f1"), false, false);
		checkC(null, false, false);
	}


	//------------------------------------------------------------------------------------------------------------------
	// Constructor names
	//------------------------------------------------------------------------------------------------------------------

	static class D1 {
		public D1() {}
		public D1(int x) {}
		public D1(String x) {}
		public D1(String x, int y) {}
	}
	static class D2 {
		public D2() {}
	}

	static ReflectionMap<Number>
		D_SIMPLE = create().append("D1()", 1).build(),
		Di_SIMPLE = create().append("D1(int)", 1).build(),
		Ds_SIMPLE = create().append("D1(String)", 1).build(),
		Dss_SIMPLE = create().append("D1(java.lang.String)", 1).build(),
		Dsi_SIMPLE = create().append("D1(String, int)", 1).build(),
		Dssi_SIMPLE = create().append("D1(java.lang.String, int)", 1).build(),
		D_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$D1()", 1).build(),
		Di_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$D1(int)", 1).build(),
		Ds_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$D1(String)", 1).build(),
		Dss_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$D1(java.lang.String)", 1).build(),
		Dsi_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$D1(String, int)", 1).build(),
		Dssi_FULL = create().append("org.apache.juneau.utils.ReflectionMapTest$D1(java.lang.String, int)", 1).build();

	@Test
	public void d01_constructorNames_checkEntries() {
		checkEntries(D_SIMPLE, false, false, false, true);
		checkEntries(Di_SIMPLE, false, false, false, true);
		checkEntries(Ds_SIMPLE, false, false, false, true);
		checkEntries(Dss_SIMPLE, false, false, false, true);
		checkEntries(Dsi_SIMPLE, false, false, false, true);
		checkEntries(Dssi_SIMPLE, false, false, false, true);
		checkEntries(D_FULL, false, false, false, true);
		checkEntries(Di_FULL, false, false, false, true);
		checkEntries(Ds_FULL, false, false, false, true);
		checkEntries(Dss_FULL, false, false, false, true);
		checkEntries(Dsi_FULL, false, false, false, true);
		checkEntries(Dssi_FULL, false, false, false, true);
	}

	private void checkD(Constructor<?> c, boolean match_D_SIMPLE, boolean match_Di_SIMPLE, boolean match_Ds_SIMPLE, boolean match_Dss_SIMPLE,
			boolean match_Dsi_SIMPLE, boolean match_Dssi_SIMPLE, boolean match_D_FULL, boolean match_Di_FULL, boolean match_Ds_FULL,
			boolean match_Dss_FULL, boolean match_Dsi_FULL, boolean match_Dssi_FULL) {

		assertEquals(match_D_SIMPLE, D_SIMPLE.find(c, null).isPresent());
		assertEquals(match_Di_SIMPLE, Di_SIMPLE.find(c, null).isPresent());
		assertEquals(match_Ds_SIMPLE, Ds_SIMPLE.find(c, null).isPresent());
		assertEquals(match_Dss_SIMPLE, Dss_SIMPLE.find(c, null).isPresent());
		assertEquals(match_Dsi_SIMPLE, Dsi_SIMPLE.find(c, null).isPresent());
		assertEquals(match_Dssi_SIMPLE, Dssi_SIMPLE.find(c, null).isPresent());
		assertEquals(match_D_FULL, D_FULL.find(c, null).isPresent());
		assertEquals(match_Di_FULL, Di_FULL.find(c, null).isPresent());
		assertEquals(match_Ds_FULL, Ds_FULL.find(c, null).isPresent());
		assertEquals(match_Dss_FULL, Dss_FULL.find(c, null).isPresent());
		assertEquals(match_Dsi_FULL, Dsi_FULL.find(c, null).isPresent());
		assertEquals(match_Dssi_FULL, Dssi_FULL.find(c, null).isPresent());

		assertEquals(match_D_SIMPLE, D_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_Di_SIMPLE, Di_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_Ds_SIMPLE, Ds_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_Dss_SIMPLE, Dss_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_Dsi_SIMPLE, Dsi_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_Dssi_SIMPLE, Dssi_SIMPLE.find(c, Integer.class).isPresent());
		assertEquals(match_D_FULL, D_FULL.find(c, Integer.class).isPresent());
		assertEquals(match_Di_FULL, Di_FULL.find(c, Integer.class).isPresent());
		assertEquals(match_Ds_FULL, Ds_FULL.find(c, Integer.class).isPresent());
		assertEquals(match_Dss_FULL, Dss_FULL.find(c, Integer.class).isPresent());
		assertEquals(match_Dsi_FULL, Dsi_FULL.find(c, Integer.class).isPresent());
		assertEquals(match_Dssi_FULL, Dssi_FULL.find(c, Integer.class).isPresent());

		assertFalse(D_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(Di_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(Ds_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(Dss_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(Dsi_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(Dssi_SIMPLE.find(c, Long.class).isPresent());
		assertFalse(D_FULL.find(c, Long.class).isPresent());
		assertFalse(Di_FULL.find(c, Long.class).isPresent());
		assertFalse(Ds_FULL.find(c, Long.class).isPresent());
		assertFalse(Dss_FULL.find(c, Long.class).isPresent());
		assertFalse(Dsi_FULL.find(c, Long.class).isPresent());
		assertFalse(Dssi_FULL.find(c, Long.class).isPresent());
	}

	@Test
	public void d02_constructorName_find() throws Exception {
		checkD(D1.class.getConstructor(), true, false, false, false, false, false, true, false, false, false, false, false);
		checkD(D1.class.getConstructor(int.class), false, true, false, false, false, false, false, true, false, false, false, false);
		checkD(D1.class.getConstructor(String.class), false, false, true, true, false, false, false, false, true, true, false, false);
		checkD(D1.class.getConstructor(String.class, int.class), false, false, false, false, true, true, false, false, false, false, true, true);
		checkD(D2.class.getConstructor(), false, false, false, false, false, false, false, false, false, false, false, false);
		checkD(null, false, false, false, false, false, false, false, false, false, false, false, false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Invalid input
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_blankInput() throws Exception {
		assertThrown(()->create().append("", 1)).asMessage().is("Invalid reflection signature: []");
	}

	@Test
	public void e02_nullInput() throws Exception {
		assertThrown(()->create().append(null, 1)).asMessage().is("Invalid reflection signature: [null]");
	}

	@Test
	public void e03_badInput() throws Exception {
		assertThrown(()->create().append("foo)", 1)).asMessage().is("Invalid reflection signature: [foo)]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comma-delimited list.
	//------------------------------------------------------------------------------------------------------------------
	static class F1 {}

	static ReflectionMap<Number> RM_F = create().append("F2, F1", 1).build();

	@Test
	public void f01_cdl() {
		assertOptional(RM_F.find(F1.class, null)).asJson().is("1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Empty reflection map.
	//------------------------------------------------------------------------------------------------------------------

	static ReflectionMap<Number> RM_G = create().build();

	@Test
	public void g01_emptyReflectionMap() throws Exception {
		assertFalse(RM_G.find(A1.class, null).isPresent());
		assertFalse(RM_G.find(B1.class.getMethod("m1"), null).isPresent());
		assertFalse(RM_G.find(C1.class.getField("f1"), null).isPresent());
		assertFalse(RM_G.find(D1.class.getConstructor(), null).isPresent());
	}
}
