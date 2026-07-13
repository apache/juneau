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
package org.apache.juneau.marshall;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

class ParentPropertyApplyAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	ParentPropertyApply a1 = ParentPropertyApplyAnnotation.create()
		.on("u")
		.value(ParentPropertyAnnotation.create().build())
		.build();

	ParentPropertyApply a2 = ParentPropertyApplyAnnotation.create()
		.on("u")
		.value(ParentPropertyAnnotation.create().build())
		.build();

	@Test void a01_basic() {
		assertBean(a1, "on", "[u]");
	}

	@Test void a02_equivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_propertyStoreEquivalency() {
		var bc1 = MarshallingContext.create().annotations(a1).build();
		var bc2 = MarshallingContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Targeting — empty on() array skips application (Applier.apply false branch)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_emptyOn_applierSkips() {
		var a = ParentPropertyApplyAnnotation.create().build();
		var bc = MarshallingContext.create().annotations(a).build();
		assertNotNull(bc);
	}

	//------------------------------------------------------------------------------------------------------------------
	// empty() helper
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_empty_null() {
		assertTrue(ParentPropertyApplyAnnotation.empty(null));
	}

	@Test void d02_empty_default() {
		assertTrue(ParentPropertyApplyAnnotation.empty(ParentPropertyApplyAnnotation.DEFAULT));
	}

	@Test void d03_empty_nonDefault() {
		assertFalse(ParentPropertyApplyAnnotation.empty(a1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// create(String...) convenience constructor
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_createWithOnTargets() {
		var a = ParentPropertyApplyAnnotation.create("MyClass.myField").build();
		assertEquals(1, a.on().length);
		assertEquals("MyClass.myField", a.on()[0]);
	}

	@Test void b05_applier_emptyOn_returnsEarly() {
		var vr = VarResolver.DEFAULT.createSession();
		var applier = new ParentPropertyApplyAnnotation.Applier(vr);
		var b = MarshallingContext.DEFAULT.copy();
		var a = ParentPropertyApplyAnnotation.DEFAULT;
		applier.apply(AnnotationInfo.of(ClassInfo.of(Object.class), a), b);
		assertNotNull(b); // early-return branch taken; builder unmodified
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder.on(Method.../Field.../FieldInfo.../MethodInfo...) overloads
	//------------------------------------------------------------------------------------------------------------------

	public static class OnTarget {
		public String field1;
		public void method1() { /* intentionally empty */ }
	}

	@Test void f01_on_method() throws Exception {
		var m = OnTarget.class.getMethod("method1");
		var a = ParentPropertyApplyAnnotation.create().on(m).build();
		assertEquals(1, a.on().length);
	}

	@Test void f02_on_methodInfo() throws Exception {
		var m = MethodInfo.of(OnTarget.class.getMethod("method1"));
		var a = ParentPropertyApplyAnnotation.create().on(m).build();
		assertEquals(1, a.on().length);
	}

	@Test void f03_on_field() throws Exception {
		var f = OnTarget.class.getField("field1");
		var a = ParentPropertyApplyAnnotation.create().on(f).build();
		assertEquals(1, a.on().length);
	}

	@Test void f04_on_fieldInfo() throws Exception {
		var f = FieldInfo.of(OnTarget.class.getField("field1"));
		var a = ParentPropertyApplyAnnotation.create().on(f).build();
		assertEquals(1, a.on().length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// value() accessor
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_value_returnsExpected() {
		var ppa = ParentPropertyAnnotation.create().build();
		var a = ParentPropertyApplyAnnotation.create().on("MyClass.myField").value(ppa).build();
		assertSame(ppa, a.value());
	}
}
