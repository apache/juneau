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
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class BeanpAnnotation_Test extends TestBase {

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Beanp a1 = BeanpAnnotation.create()
		.description("a")
		.dictionary(X1.class)
		.format("b")
		.name("c")
		.params(X1.class)
		.properties("e")
		.ro("f")
		.type(X1.class)
		.value("g")
		.wo("h")
		.build();

	Beanp a2 = BeanpAnnotation.create()
		.description("a")
		.dictionary(X1.class)
		.format("b")
		.name("c")
		.params(X1.class)
		.properties("e")
		.ro("f")
		.type(X1.class)
		.value("g")
		.wo("h")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "description,dictionary,format,name,params,properties,ro,type,value,wo", "[a],[X1],b,c,[X1],e,f,X1,g,h");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a2.hashCode(), 0, -1);
		assertEquals(a2.hashCode(), a1.hashCode());
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
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	public static class D1 {
		@Beanp(
			description={ "a" },
			dictionary=X1.class,
			format="b",
			name="c",
			params=X1.class,
			properties="e",
			ro="f",
			type=X1.class,
			value="g",
			wo="h"
		)
		public int f;
	}

	public static class D2 {
		@Beanp(
			description={ "a" },
			dictionary=X1.class,
			format="b",
			name="c",
			params=X1.class,
			properties="e",
			ro="f",
			type=X1.class,
			value="g",
			wo="h"
		)
		public int f;
	}

	Beanp d1, d2;
	{
		try {
			d1 = D1.class.getField("f").getAnnotationsByType(Beanp.class)[0];
			d2 = D2.class.getField("f").getAnnotationsByType(Beanp.class)[0];
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