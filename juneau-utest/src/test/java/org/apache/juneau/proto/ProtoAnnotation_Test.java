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
package org.apache.juneau.proto;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class ProtoAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Proto a1 = ProtoAnnotation.create()
		.comment("a")
		.description("b")
		.build();

	Proto a2 = ProtoAnnotation.create()
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

	@Proto(
		comment="a",
		description={ "b" }
	)
	public static class D1 {}
	Proto d1 = D1.class.getAnnotationsByType(Proto.class)[0];

	@Proto(
		comment="a",
		description={ "b" }
	)
	public static class D2 {}
	Proto d2 = D2.class.getAnnotationsByType(Proto.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// ProtoApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(ProtoApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(ProtoApplyAnnotation.empty(null));
		assertTrue(ProtoApplyAnnotation.empty(ProtoApplyAnnotation.DEFAULT));
		assertFalse(ProtoApplyAnnotation.empty(ProtoApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = ProtoApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = ProtoApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = ProtoApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var proto = ProtoAnnotation.create().comment("test").build();
		var a = ProtoApplyAnnotation.create().value(proto).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ProtoBeanPropertyMeta + ProtoClassMeta lookup tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_protoBeanPropertyMeta_default() {
		assertNotNull(ProtoBeanPropertyMeta.DEFAULT);
	}

	public static class F02_Bean { public String name; }

	@Test void f02_protoBeanPropertyMeta_lookup() {
		var s = ProtoSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getProtoBeanPropertyMeta(bpm));
		// null path
		assertNotNull(s.getProtoBeanPropertyMeta(null));
	}

	@Test void f03_protoClassMeta_lookup() {
		var s = ProtoSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getProtoClassMeta(cm));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ProtoConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@ProtoConfig(useListSyntaxForBeans = "true")
	public static class G01_Bean {}

	@Test void g01_protoConfigUseListSyntaxForBeans() {
		var s = ProtoSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@ProtoConfig(useColonForMessages = "true")
	public static class G02_Bean {}

	@Test void g02_protoConfigUseColonForMessages() {
		var s = ProtoSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@ProtoConfig
	public static class G03_Bean {}

	@Test void g03_protoConfigParserApply() {
		var p = ProtoParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pre-existing serializer integration tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void h01_comment() throws Exception {
		// Serialize structure with name/test; @Proto(comment) on bean field emits comment when bean is used
		var a = JsonMap.of("name", "test");
		var proto = ProtoSerializer.DEFAULT.serialize(a);
		assertNotNull(proto);
		assertTrue(proto.contains("name"));
		assertTrue(proto.contains("test"));
	}

	@Test
	void h02_annotationEquivalency() {
		var a = ProtoAnnotation.create().comment("x").build();
		var b = ProtoAnnotation.create().comment("x").build();
		assertEquals(a.comment(), b.comment());
	}
}
