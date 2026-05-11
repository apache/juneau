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
package org.apache.juneau.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

class MarshalledAnnotation_Test extends TestBase {

	private static class X1 {}
	private  static class X2 extends MarshallingInterceptor<MarshalledAnnotation_Test> {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Marshalled a1 = MarshalledAnnotation.create()
		.description("a")
		.dictionary(X1.class)
		.example("b")
		.excludeProperties("c")
		.findFluentSetters(true)
		.implClass(X1.class)
		.interceptor(X2.class)
		.interfaceClass(X1.class)
		.p("e")
		.properties("f")
		.propertyNamer(BasicPropertyNamer.class)
		.readOnlyProperties("g")
		.ro("h")
		.unsorted(true)
		.stopClass(X1.class)
		.typeName("i")
		.typePropertyName("j")
		.wo("k")
		.writeOnlyProperties("l")
		.xp("m")
		.build();

	Marshalled a2 = MarshalledAnnotation.create()
		.description("a")
		.dictionary(X1.class)
		.example("b")
		.excludeProperties("c")
		.findFluentSetters(true)
		.implClass(X1.class)
		.interceptor(X2.class)
		.interfaceClass(X1.class)
		.p("e")
		.properties("f")
		.propertyNamer(BasicPropertyNamer.class)
		.readOnlyProperties("g")
		.ro("h")
		.unsorted(true)
		.stopClass(X1.class)
		.typeName("i")
		.typePropertyName("j")
		.wo("k")
		.writeOnlyProperties("l")
		.xp("m")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"description,dictionary,example,excludeProperties,findFluentSetters,implClass,interceptor,interfaceClass,p,properties,propertyNamer,readOnlyProperties,ro,stopClass,typeName,typePropertyName,unsorted,wo,writeOnlyProperties,xp",
			"[a],[X1],b,c,true,X1,X2,X1,e,f,BasicPropertyNamer,g,h,X1,i,j,true,k,l,m");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEquals(0, a1.hashCode());
		assertNotEquals(-1, a1.hashCode());
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var b1 = MarshallingContext.create().annotations(a1).build();
		var b2 = MarshallingContext.create().annotations(a2).build();
		assertSame(b1, b2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled(
		description={ "a" },
		dictionary=X1.class,
		example="b",
		excludeProperties="c",
		findFluentSetters=true,
		implClass=X1.class,
		interceptor=X2.class,
		interfaceClass=X1.class,
		p="e",
		properties="f",
		propertyNamer=BasicPropertyNamer.class,
		readOnlyProperties="g",
		ro="h",
		unsorted=true,
		stopClass=X1.class,
		typeName="i",
		typePropertyName="j",
		wo="k",
		writeOnlyProperties="l",
		xp="m"
	)
	public static class D1 {}
	Marshalled d1 = D1.class.getAnnotationsByType(Marshalled.class)[0];

	@Marshalled(
		description={ "a" },
		dictionary=X1.class,
		example="b",
		excludeProperties="c",
		findFluentSetters=true,
		implClass=X1.class,
		interceptor=X2.class,
		interfaceClass=X1.class,
		p="e",
		properties="f",
		propertyNamer=BasicPropertyNamer.class,
		readOnlyProperties="g",
		ro="h",
		unsorted=true,
		stopClass=X1.class,
		typeName="i",
		typePropertyName="j",
		wo="k",
		writeOnlyProperties="l",
		xp="m"
	)
	public static class D2 {}
	Marshalled d2 = D2.class.getAnnotationsByType(Marshalled.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}