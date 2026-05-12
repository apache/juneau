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
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class BeanPropAnnotation_Test extends TestBase {

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	BeanProp a1 = BeanPropAnnotation.create()
		.description("a")
		.name("c")
		.params(X1.class)
		.ro("f")
		.type(X1.class)
		.value("g")
		.wo("h")
		.build();

	BeanProp a2 = BeanPropAnnotation.create()
		.description("a")
		.name("c")
		.params(X1.class)
		.ro("f")
		.type(X1.class)
		.value("g")
		.wo("h")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "description,name,params,ro,type,value,wo", "[a],c,[X1],f,X1,g,h");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a2.hashCode(), 0, -1);
		assertEquals(a2.hashCode(), a1.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	public static class D1 {
		@BeanProp(description={ "a" }, name="c", params=X1.class, ro="f", type=X1.class, value="g", wo="h")
		public int f;
	}

	public static class D2 {
		@BeanProp(description={ "a" }, name="c", params=X1.class, ro="f", type=X1.class, value="g", wo="h")
		public int f;
	}

	BeanProp d1, d2;
	{
		try {
			d1 = D1.class.getField("f").getAnnotationsByType(BeanProp.class)[0];
			d2 = D2.class.getField("f").getAnnotationsByType(BeanProp.class)[0];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}
