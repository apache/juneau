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
package org.apache.juneau.marshall.prototext;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class PrototextAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Prototext a1 = PrototextAnnotation.create()
		.comment("a")
		.description("b")
		.build();

	Prototext a2 = PrototextAnnotation.create()
		.comment("a")
		.description("b")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "comment,description", "a,[b]");
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

	@Prototext(
		comment="a",
		description={ "b" }
	)
	public static class D1 {}
	Prototext d1 = D1.class.getAnnotationsByType(Prototext.class)[0];

	@Prototext(
		comment="a",
		description={ "b" }
	)
	public static class D2 {}
	Prototext d2 = D2.class.getAnnotationsByType(Prototext.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PrototextApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(PrototextApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(PrototextApplyAnnotation.empty(null));
		assertTrue(PrototextApplyAnnotation.empty(PrototextApplyAnnotation.DEFAULT));
		assertFalse(PrototextApplyAnnotation.empty(PrototextApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = PrototextApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = PrototextApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = PrototextApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var proto = PrototextAnnotation.create().comment("test").build();
		var a = PrototextApplyAnnotation.create().value(proto).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PrototextBeanPropertyMeta + PrototextClassMeta lookup tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_protoBeanPropertyMeta_default() {
		assertNotNull(PrototextBeanPropertyMeta.DEFAULT);
	}

	public static class F02_Bean { public String name; }

	@Test void f02_protoBeanPropertyMeta_lookup() {
		var s = PrototextSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getPrototextBeanPropertyMeta(bpm));
		// null path
		assertNotNull(s.getPrototextBeanPropertyMeta(null));
	}

	@Test void f03_protoClassMeta_lookup() {
		var s = PrototextSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getPrototextClassMeta(cm));
	}

	//------------------------------------------------------------------------------------------------------------------
	// PrototextConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@PrototextConfig(useListSyntaxForBeans = "true")
	public static class G01_Bean {}

	@Test void g01_protoConfigUseListSyntaxForBeans() {
		var s = PrototextSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@PrototextConfig(useColonForMessages = "true")
	public static class G02_Bean {}

	@Test void g02_protoConfigUseColonForMessages() {
		var s = PrototextSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@PrototextConfig
	public static class G03_Bean {}

	@Test void g03_protoConfigParserApply() {
		var p = PrototextParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pre-existing serializer integration tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void h01_comment() throws Exception {
		// Serialize structure with name/test; @Prototext(comment) on bean field emits comment when bean is used
		var a = JsonMap.of("name", "test");
		var proto = PrototextSerializer.DEFAULT.serialize(a);
		assertNotNull(proto);
		assertTrue(proto.contains("name"));
		assertTrue(proto.contains("test"));
	}

	@Test
	void h02_annotationEquivalency() {
		var a = PrototextAnnotation.create().comment("x").build();
		var b = PrototextAnnotation.create().comment("x").build();
		assertEquals(a.comment(), b.comment());
	}

	//------------------------------------------------------------------------------------------------------------------
	// i — builder null-guard branches
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_comment_null_usesEmpty() {
		var a = PrototextAnnotation.create().comment(null).build();
		assertEquals("", a.comment());
	}
}
