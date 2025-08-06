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
package org.apache.juneau.objecttools;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the PojoPaginator class.
 */
class ObjectSorter_Test extends SimpleTestBase {

	ObjectSorter os = new ObjectSorter();
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullInput() {
		assertNull(os.run(bs, null, null));
	}

	@Test void a02_emptySort() {
		Object in = set(A.create("c"),A.create("a"),A.create("b"));
		SortArgs sa = SortArgs.create("");
		assertJson(os.run(bs, in, sa), "[{f:'c'},{f:'a'},{f:'b'}]");
		assertJson(os.run(in, ""), "[{f:'c'},{f:'a'},{f:'b'}]");
	}

	@Test void a03_invalidDataType() {
		Object in = map("a","b");
		SortArgs sa = SortArgs.create("x");
		assertJson(os.run(bs, in, sa), "{a:'b'}");
		assertNull(os.run(in, "x"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Arrays
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public String f;

		public static A create(String f) {
			A a = new A();
			a.f = f;
			return a;
		}
	}

	@Test void b01_beanArray() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertJson(os.run(in, "f"), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test void b02_beanArray_reverse() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = SortArgs.create("f-");
		assertJson(os.run(bs, in, sa), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertJson(os.run(in, "f-"), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	@Test void b03_beanArrayContainingNulls() {
		Object in = new A[]{A.create("c"),A.create("a"),null,null,A.create("b")};
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[null,null,{f:'a'},{f:'b'},{f:'c'}]");
		assertJson(os.run(in, "f"), "[null,null,{f:'a'},{f:'b'},{f:'c'}]");
	}

	@Test void b04_beanArrayContainingDups() {
		Object in = new A[]{A.create("c"),A.create("a"),null,A.create("a"),A.create("b")};
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
		assertJson(os.run(in, "f"), "[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_beanList() {
		Object in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertJson(os.run(in, "f"), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test void c02_beanList_reverse() {
		Object in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f-");
		assertJson(os.run(bs, in, sa), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertJson(os.run(in, "f-"), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	@Test void c03_beanListContainingNull() {
		Object in = list(A.create("c"),A.create("a"),null,null,A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[null,null,{f:'a'},{f:'b'},{f:'c'}]");
		assertJson(os.run(in, "f"), "[null,null,{f:'a'},{f:'b'},{f:'c'}]");
	}

	@Test void c04_beanListContainingDups() {
		Object in = list(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
		assertJson(os.run(in, "f"), "[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sets
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_beanSet() {
		Object in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertJson(os.run(in, "f"), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test void d02_beanSet_reverse() {
		Object in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f-");
		assertJson(os.run(bs, in, sa), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertJson(os.run(in, "f-"), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	@Test void d03_beanSetContainingNull() {
		Object in = set(A.create("c"),A.create("a"),null,null,A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[null,{f:'a'},{f:'b'},{f:'c'}]");
		assertJson(os.run(in, "f"), "[null,{f:'a'},{f:'b'},{f:'c'}]");
	}

	@Test void d04_beanSetContainingDups() {
		Object in = set(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
		assertJson(os.run(in, "f"), "[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_listOfMaps() {
		Object in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertJson(os.run(in, "f"), "[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test void e02_listOfMaps_reverse() {
		Object in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		SortArgs sa = SortArgs.create("f-");
		assertJson(os.run(bs, in, sa), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertJson(os.run(in, "f-"), "[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_listOfOther() {
		Object in = list(list("c"),list("a"),list("b"));
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[['c'],['a'],['b']]");
		assertJson(os.run(in, "f"), "[['c'],['a'],['b']]");
	}

	@Test void f02_listOfOther_reverse() {
		Object in = list(list("c"),list("a"),list("b"));
		SortArgs sa = SortArgs.create("f-");
		assertJson(os.run(bs, in, sa), "[['c'],['a'],['b']]");
		assertJson(os.run(in, "f-"), "[['c'],['a'],['b']]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_nonExistentField() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = SortArgs.create("fx");
		assertJson(os.run(bs, in, sa), "[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]");
		assertJson(os.run(in, "fx"), "[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]");
	}

	public static class B {
		public Object f;

		public static B create(Object f) {
			B b = new B();
			b.f = f;
			return b;
		}
	}

	// Should gracefully handle different sorting data types.
	@Test void g02_mixtureOfTypes() {
		Object in = new B[]{B.create(1),B.create(true),B.create("a")};
		SortArgs sa = SortArgs.create("f");
		assertJson(os.run(bs, in, sa), "[{f:1},{f:true},{f:'a'}]");
		assertJson(os.run(in, "f"), "[{f:1},{f:true},{f:'a'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sort by multiple columns.
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public int f1;
		public float f2;

		public static C create(int f1, float f2) {
			C c = new C();
			c.f1 = f1;
			c.f2 = f2;
			return c;
		}
	}

	@Test void h01_sortMultipleColumns() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = SortArgs.create("f1,f2");
		assertJson(os.run(bs, in, sa), "[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]");
		assertJson(os.run(in, "f1,f2"), "[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]");
	}

	@Test void h02_sortMultipleColumns_descending() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = SortArgs.create("f1-,f2-");
		assertJson(os.run(bs, in, sa), "[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]");
		assertJson(os.run(in, "f1-,f2-"), "[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]");
	}

	@Test void h03_sortMultipleColumns_ascendingAndDescending() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = SortArgs.create("f1-,f2+");
		assertJson(os.run(bs, in, sa), "[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]");
		assertJson(os.run(in, "f1-,f2+"), "[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]");
	}
}