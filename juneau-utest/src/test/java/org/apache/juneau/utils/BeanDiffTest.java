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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class BeanDiffTest extends SimpleTestBase {

	public static class A {
		public int f1;
		public String f2;

		static A create(int f1, String f2) {
			A a = new A();
			a.f1 = f1;
			a.f2 = f2;
			return a;
		}
	}

	@Test void testSame() {
		BeanDiff bd = BeanDiff.create(A.create(1, "a"), A.create(1, "a")).build();
		assertFalse(bd.hasDiffs());
		assertEquals("{v1:{},v2:{}}", bd.toString());
	}

	@Test void testDifferent() {
		BeanDiff bd = BeanDiff.create(A.create(1, "a"), A.create(2, "b")).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1,f2:'a'},v2:{f1:2,f2:'b'}}", bd.toString());
	}

	@Test void testFirstNull() {
		BeanDiff bd = BeanDiff.create(null, A.create(2, "b")).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{},v2:{f1:2,f2:'b'}}", bd.toString());
	}

	@Test void testSecondNull() {
		BeanDiff bd = BeanDiff.create(A.create(1, "a"), null).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1,f2:'a'},v2:{}}", bd.toString());
	}

	@Test void testBothNull() {
		BeanDiff bd = BeanDiff.create(null, null).build();
		assertFalse(bd.hasDiffs());
		assertEquals("{v1:{},v2:{}}", bd.toString());
	}

	@Test void testNullFields() {
		BeanDiff bd = BeanDiff.create(A.create(1, null), A.create(2, "b")).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1},v2:{f1:2,f2:'b'}}", bd.toString());
	}

	@Test void testIncludes() {
		BeanDiff bd = BeanDiff.create(A.create(1, null), A.create(2, "b")).include("f1").build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1},v2:{f1:2}}", bd.toString());
	}

	@Test void testIncludesSet() {
		BeanDiff bd = BeanDiff.create(A.create(1, null), A.create(2, "b")).include(set("f1")).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1},v2:{f1:2}}", bd.toString());
	}

	@Test void testExcludes() {
		BeanDiff bd = BeanDiff.create(A.create(1, null), A.create(2, "b")).exclude("f2").build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1},v2:{f1:2}}", bd.toString());
	}

	@Test void testExcludesSet() {
		BeanDiff bd = BeanDiff.create(A.create(1, null), A.create(2, "b")).exclude(set("f2")).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1},v2:{f1:2}}", bd.toString());
	}

	@Test void testDifferentBeanContext() {
		BeanDiff bd = BeanDiff.create(A.create(1, null), A.create(2, "b")).beanContext(BeanContext.DEFAULT_SORTED).build();
		assertTrue(bd.hasDiffs());
		assertEquals("{v1:{f1:1},v2:{f1:2,f2:'b'}}", bd.toString());
	}
}