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
package org.apache.juneau.marshall.soap;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class SoapXmlAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	SoapXml a1 = SoapXmlAnnotation.create()
		.description("a")
		.build();

	SoapXml a2 = SoapXmlAnnotation.create()
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

	@SoapXml(
		description={ "a" }
	)
	public static class D1 {}
	SoapXml d1 = D1.class.getAnnotationsByType(SoapXml.class)[0];

	@SoapXml(
		description={ "a" }
	)
	public static class D2 {}
	SoapXml d2 = D2.class.getAnnotationsByType(SoapXml.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// SoapXmlApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(SoapXmlApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(SoapXmlApplyAnnotation.empty(null));
		assertTrue(SoapXmlApplyAnnotation.empty(SoapXmlApplyAnnotation.DEFAULT));
		assertFalse(SoapXmlApplyAnnotation.empty(SoapXmlApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = SoapXmlApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = SoapXmlApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = SoapXmlApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var sx = SoapXmlAnnotation.create().description("test").build();
		var a = SoapXmlApplyAnnotation.create().value(sx).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// SoapXmlBeanPropertyMeta + SoapXmlClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_soapXmlBeanPropertyMeta_default() {
		assertNotNull(SoapXmlBeanPropertyMeta.DEFAULT);
	}

	public static class F02_Bean { public String name; }

	@Test void f02_soapXmlBeanPropertyMeta_lookup() {
		var s = SoapXmlSerializer.create().build();
		var bc = s.getMarshallingContext();
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getSoapXmlBeanPropertyMeta(bpm));
		assertNotNull(s.getSoapXmlBeanPropertyMeta(null));
	}

	@Test void f03_soapXmlClassMeta_lookup() {
		var s = SoapXmlSerializer.create().build();
		var bc = s.getMarshallingContext();
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getSoapXmlClassMeta(cm));
	}
}
