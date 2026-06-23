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
package org.apache.juneau.marshall.jena;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class RdfApplyAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a - Builder / factory methods
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_create_returnsNonNull() {
		assertNotNull(RdfApplyAnnotation.create());
	}

	@Test void a02_create_stringTargets_setsOn() {
		var a = RdfApplyAnnotation.create("com.example.Foo", "com.example.Bar").build();
		assertEquals(2, a.on().length);
		assertEquals("com.example.Foo", a.on()[0]);
		assertEquals("com.example.Bar", a.on()[1]);
	}

	@Test void a03_builder_value_setsValue() {
		var rdf = RdfAnnotation.create().beanUri(true).build();
		var a = RdfApplyAnnotation.create().value(rdf).build();
		assertTrue(a.value().beanUri());
	}

	@Test void a04_builder_on_string_setsTargets() {
		var a = RdfApplyAnnotation.create().on("MyClass").build();
		assertEquals(1, a.on().length);
		assertEquals("MyClass", a.on()[0]);
	}

	@Test void a05_build_returnsNonNull() {
		var a = RdfApplyAnnotation.create().build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - Applier
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_applier_constructor() {
		var vr = VarResolver.DEFAULT.createSession();
		assertNotNull(new RdfApplyAnnotation.Applier(vr));
	}

	@Test void b02_applier_apply_emptyOn_skips() {
		// Both on() and onClass() are empty — early-return branch is taken, builder is not modified.
		var vr = VarResolver.DEFAULT.createSession();
		var applier = new RdfApplyAnnotation.Applier(vr);
		var b = MarshallingContext.DEFAULT.copy();
		var a = RdfApplyAnnotation.DEFAULT;
		// Must not throw; early-return means b.annotations() is never called.
		assertDoesNotThrow(() -> applier.apply(AnnotationInfo.of(ClassInfo.of(Object.class), a), b));
	}

	@Test void b03_applier_apply_nonEmptyOn_registersAnnotation() {
		// on() is non-empty — isEmptyArray(a.on()) is false so the &&-short-circuit fires and
		// the early-return is NOT taken; b.annotations(a) is called.
		var vr = VarResolver.DEFAULT.createSession();
		var applier = new RdfApplyAnnotation.Applier(vr);
		var b = MarshallingContext.DEFAULT.copy();
		var rdf = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.BAG).build();
		var a = RdfApplyAnnotation.create().on("com.example.Target").value(rdf).build();
		assertDoesNotThrow(() -> applier.apply(AnnotationInfo.of(ClassInfo.of(Object.class), a), b));
	}

	@Test void b04_applier_apply_onClass_nonEmpty_registersAnnotation() {
		// on() is empty but onClass() is non-empty — isEmptyArray(a.on()) is true but
		// isEmptyArray(a.onClass()) is false so the full condition is false and b.annotations(a) is called.
		var vr = VarResolver.DEFAULT.createSession();
		var applier = new RdfApplyAnnotation.Applier(vr);
		var b = MarshallingContext.DEFAULT.copy();
		var rdf = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.SEQ).build();
		var a = RdfApplyAnnotation.create().onClass(String.class).value(rdf).build();
		assertDoesNotThrow(() -> applier.apply(AnnotationInfo.of(ClassInfo.of(Object.class), a), b));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - empty() helper
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_empty_null_returnsTrue() {
		assertTrue(RdfApplyAnnotation.empty(null));
	}

	@Test void c02_empty_default_returnsTrue() {
		assertTrue(RdfApplyAnnotation.empty(RdfApplyAnnotation.DEFAULT));
	}

	@Test void c03_empty_nonDefault_returnsFalse() {
		var a = RdfApplyAnnotation.create(String.class).build();
		assertFalse(RdfApplyAnnotation.empty(a));
	}
}
