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
package org.apache.juneau.msgpack.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.msgpack.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class MsgPackAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	MsgPack a1 = MsgPackAnnotation.create()
		.description("a")
		.build();

	MsgPack a2 = MsgPackAnnotation.create()
		.description("a")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "description", "[a]");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var bc1 = MarshallingContext.create().annotations(a1).build();
		var bc2 = MarshallingContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@MsgPack(
		description="a"
	)
	public static class D1 {}
	MsgPack d1 = D1.class.getAnnotationsByType(MsgPack.class)[0];

	@MsgPack(
		description="a"
	)
	public static class D2 {}
	MsgPack d2 = D2.class.getAnnotationsByType(MsgPack.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// MsgPackApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(MsgPackApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(MsgPackApplyAnnotation.empty(null));
		assertTrue(MsgPackApplyAnnotation.empty(MsgPackApplyAnnotation.DEFAULT));
		assertFalse(MsgPackApplyAnnotation.empty(MsgPackApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = MsgPackApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = MsgPackApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = MsgPackApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var msgPack = MsgPackAnnotation.create().description("test").build();
		var a = MsgPackApplyAnnotation.create().value(msgPack).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// MsgPackBeanPropertyMeta / MsgPackClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_msgPackBeanPropertyMeta_default() {
		assertNotNull(MsgPackBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_msgPackBeanPropertyMeta_lookup() {
		var s = MsgPackSerializer.DEFAULT;
		var bm = MarshallingContext.DEFAULT.getBeanMeta(F02_Bean.class);
		var bpm = bm.getPropertyMeta("name");
		var meta = s.getMsgPackBeanPropertyMeta(bpm);
		assertNotNull(meta);
	}

	@Test void f03_msgPackBeanPropertyMeta_lookupNull() {
		var s = MsgPackSerializer.DEFAULT;
		var meta = s.getMsgPackBeanPropertyMeta(null);
		assertNotNull(meta);
		assertSame(MsgPackBeanPropertyMeta.DEFAULT, meta);
	}

	@Test void f04_msgPackClassMeta_lookup() {
		var s = MsgPackSerializer.DEFAULT;
		var cm = MarshallingContext.DEFAULT.getClassMeta(F02_Bean.class);
		var meta = s.getMsgPackClassMeta(cm);
		assertNotNull(meta);
	}

	//------------------------------------------------------------------------------------------------------------------
	// MsgPackConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@MsgPackConfig(addBeanTypes = "true")
	public static class G01_Bean {}

	@Test void g01_msgPackConfigAddBeanTypes() {
		var s = MsgPackSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@MsgPackConfig(rank = 1)
	public static class G02_Bean {}

	@Test void g02_msgPackConfigRank() {
		var s = MsgPackSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@MsgPackConfig
	public static class G03_Bean {}

	@Test void g03_msgPackConfigParserApply() {
		var p = MsgPackParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}
}
