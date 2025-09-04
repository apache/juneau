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
package org.apache.juneau.xml.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class XmlAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = XmlAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Xml a1 = XmlAnnotation.create()
		.description("description")
		.childName("a")
		.format(XmlFormat.ATTR)
		.namespace("c")
		.on("d")
		.prefix("e")
		.build();

	Xml a2 = XmlAnnotation.create()
		.description("description")
		.childName("a")
		.format(XmlFormat.ATTR)
		.namespace("c")
		.on("d")
		.prefix("e")
		.build();

	@Test void a01_basic() {
		assertJson(""
			+ "{"
				+ "childName:'a',"
				+ "description:['description'],"
				+ "format:'ATTR',"
				+ "namespace:'c',"
				+ "on:['d'],"
				+ "onClass:[],"
				+ "prefix:'e'"
			+ "}", a1
		);
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
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}  // NOSONAR
	}
	public static class C2 {
		public int f2;
		public void m2() {}  // NOSONAR
	}

	@Test void c01_otherMethods() throws Exception {
		var c1 = XmlAnnotation.create(C1.class).on(C2.class).build();
		var c2 = XmlAnnotation.create("a").on("b").build();
		var c3 = XmlAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		var c4 = XmlAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertBean(c1, "on", "["+CNAME+"$C1,"+CNAME+"$C2]");
		assertBean(c2, "on", "[a,b]");
		assertBean(c3, "on", "["+CNAME+"$C1.f1,"+CNAME+"$C2.f2]");
		assertBean(c4, "on", "["+CNAME+"$C1.m1(),"+CNAME+"$C2.m2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Xml(
		childName="a",
		description={ "description" },
		format=XmlFormat.ATTR,
		namespace="c",
		on="d",
		prefix="e"
	)
	public static class D1 {}
	Xml d1 = D1.class.getAnnotationsByType(Xml.class)[0];

	@Xml(
		childName="a",
		description={ "description" },
		format=XmlFormat.ATTR,
		namespace="c",
		on="d",
		prefix="e"
	)
	public static class D2 {}
	Xml d2 = D2.class.getAnnotationsByType(Xml.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}