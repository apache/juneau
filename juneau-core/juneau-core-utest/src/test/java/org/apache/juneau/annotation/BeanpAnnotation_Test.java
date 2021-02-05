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
package org.apache.juneau.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanpAnnotation_Test {

	private static final String CNAME = BeanpAnnotation_Test.class.getName();

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Beanp a1 = BeanpAnnotation.create()
		.dictionary(X1.class)
		.format("format")
		.name("name")
		.on("on")
		.params(X1.class)
		.properties("properties")
		.ro("ro")
		.type(X1.class)
		.value("value")
		.wo("wo")
		.build();

	Beanp a2 = BeanpAnnotation.create()
		.dictionary(X1.class)
		.format("format")
		.name("name")
		.on("on")
		.params(X1.class)
		.properties("properties")
		.ro("ro")
		.type(X1.class)
		.value("value")
		.wo("wo")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asJson().is(""
			+ "{"
				+ "dictionary:['"+CNAME+"$X1'],"
				+ "format:'format',"
				+ "name:'name',"
				+ "on:['on'],"
				+ "params:['"+CNAME+"$X1'],"
				+ "properties:'properties',"
				+ "ro:'ro',"
				+ "type:'"+CNAME+"$X1',"
				+ "value:'value',"
				+ "wo:'wo'"
			+ "}"
		);
	}

	@Test
	public void a02_testEquivalency() {
		assertObject(a1).is(a2);
		assertInteger(a1.hashCode()).is(a2.hashCode()).isNotAny(0,-1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_testEquivalencyInPropertyStores() {
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
		assertTrue(bc1 == bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}
	}
	public static class C2 {
		public int f2;
		public void m2() {}
	}

	@Test
	public void c01_otherMethods() throws Exception {
		Beanp c1 = BeanpAnnotation.create("a").on("b").build();
		Beanp c2 = BeanpAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		Beanp c3 = BeanpAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertObject(c1).asJson().contains("on:['a','b']");
		assertObject(c2).asJson().contains("on:['"+CNAME+"$C1.f1','"+CNAME+"$C2.f2']");
		assertObject(c3).asJson().contains("on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Beanp(
		dictionary=X1.class,
		format="format",
		name="name",
		on="on",
		params=X1.class,
		properties="properties",
		ro="ro",
		type=X1.class,
		value="value",
		wo="wo"
	)
	public static class D1 {}
	Beanp d1 = D1.class.getAnnotationsByType(Beanp.class)[0];

	@Beanp(
		dictionary=X1.class,
		format="format",
		name="name",
		on="on",
		params=X1.class,
		properties="properties",
		ro="ro",
		type=X1.class,
		value="value",
		wo="wo"
	)
	public static class D2 {}
	Beanp d2 = D2.class.getAnnotationsByType(Beanp.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}

