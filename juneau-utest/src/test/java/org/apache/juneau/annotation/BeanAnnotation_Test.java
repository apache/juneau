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
import org.apache.juneau.swap.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanAnnotation_Test {

	private static final String CNAME = BeanAnnotation_Test.class.getName();

	private static class X1 {}
	private  static class X2 extends BeanInterceptor<BeanAnnotation_Test> {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Bean a1 = BeanAnnotation.create()
        .description("description")
		.dictionary(X1.class)
		.example("example")
		.excludeProperties("excludeProperties")
		.findFluentSetters(true)
		.implClass(X1.class)
		.interceptor(X2.class)
		.interfaceClass(X1.class)
		.on("on")
		.onClass(X1.class)
		.p("p")
		.properties("properties")
		.propertyNamer(BasicPropertyNamer.class)
		.readOnlyProperties("readOnlyProperties")
		.ro("ro")
		.sort(true)
		.stopClass(X1.class)
		.typeName("typeName")
		.typePropertyName("typePropertyName")
		.wo("wo")
		.writeOnlyProperties("writeOnlyProperties")
		.xp("xp")
		.build();

	Bean a2 = BeanAnnotation.create()
        .description("description")
		.dictionary(X1.class)
		.example("example")
		.excludeProperties("excludeProperties")
		.findFluentSetters(true)
		.implClass(X1.class)
		.interceptor(X2.class)
		.interfaceClass(X1.class)
		.on("on")
		.onClass(X1.class)
		.p("p")
		.properties("properties")
		.propertyNamer(BasicPropertyNamer.class)
		.readOnlyProperties("readOnlyProperties")
		.ro("ro")
		.sort(true)
		.stopClass(X1.class)
		.typeName("typeName")
		.typePropertyName("typePropertyName")
		.wo("wo")
		.writeOnlyProperties("writeOnlyProperties")
		.xp("xp")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asJson().is(""
			+ "{"
                + "description:['description'],"
				+ "dictionary:['"+CNAME+"$X1'],"
				+ "example:'example',"
				+ "excludeProperties:'excludeProperties',"
				+ "findFluentSetters:true,"
				+ "implClass:'"+CNAME+"$X1',"
				+ "interceptor:'"+CNAME+"$X2',"
				+ "interfaceClass:'"+CNAME+"$X1',"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"$X1'],"
				+ "p:'p',"
				+ "properties:'properties',"
				+ "propertyNamer:'org.apache.juneau.BasicPropertyNamer',"
				+ "readOnlyProperties:'readOnlyProperties',"
				+ "ro:'ro',"
				+ "sort:true,"
				+ "stopClass:'"+CNAME+"$X1',"
				+ "typeName:'typeName',"
				+ "typePropertyName:'typePropertyName',"
				+ "wo:'wo',"
				+ "writeOnlyProperties:'writeOnlyProperties',"
				+ "xp:'xp'"
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
		BeanContext b1 = BeanContext.create().annotations(a1).build();
		BeanContext b2 = BeanContext.create().annotations(a2).build();
		assertTrue(b1 == b2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {}
	public static class C2 {}

	@Test
	public void c01_otherMethods() {
		Bean c1 = BeanAnnotation.create(C1.class).on(C2.class).build();
		Bean c2 = BeanAnnotation.create("a").on("b").build();

		assertObject(c1).asJson().isContains("on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertObject(c2).asJson().isContains("on:['a','b']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Bean(
        description={ "description" },
		dictionary=X1.class,
		example="example",
		excludeProperties="excludeProperties",
		findFluentSetters=true,
		implClass=X1.class,
		interceptor=X2.class,
		interfaceClass=X1.class,
		on="on",
		onClass=X1.class,
		p="p",
		properties="properties",
		propertyNamer=BasicPropertyNamer.class,
		readOnlyProperties="readOnlyProperties",
		ro="ro",
		sort=true,
		stopClass=X1.class,
		typeName="typeName",
		typePropertyName="typePropertyName",
		wo="wo",
		writeOnlyProperties="writeOnlyProperties",
		xp="xp"
	)
	public static class D1 {}
	Bean d1 = D1.class.getAnnotationsByType(Bean.class)[0];

	@Bean(
        description={ "description" },
		dictionary=X1.class,
		example="example",
		excludeProperties="excludeProperties",
		findFluentSetters=true,
		implClass=X1.class,
		interceptor=X2.class,
		interfaceClass=X1.class,
		on="on",
		onClass=X1.class,
		p="p",
		properties="properties",
		propertyNamer=BasicPropertyNamer.class,
		readOnlyProperties="readOnlyProperties",
		ro="ro",
		sort=true,
		stopClass=X1.class,
		typeName="typeName",
		typePropertyName="typePropertyName",
		wo="wo",
		writeOnlyProperties="writeOnlyProperties",
		xp="xp"
	)
	public static class D2 {}
	Bean d2 = D2.class.getAnnotationsByType(Bean.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}

