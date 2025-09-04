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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

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
		var in = set(A.create("c"),A.create("a"),A.create("b"));
		var sa = SortArgs.create("");
		assertJson("[{f:'c'},{f:'a'},{f:'b'}]", os.run(bs, in, sa));
		assertJson("[{f:'c'},{f:'a'},{f:'b'}]", os.run(in, ""));
	}

	@Test void a03_invalidDataType() {
		var in = map("a","b");
		var sa = SortArgs.create("x");
		assertJson("{a:'b'}", os.run(bs, in, sa));
		assertNull(os.run(in, "x"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Arrays
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public String f;

		public static A create(String f) {
			var a = new A();
			a.f = f;
			return a;
		}
	}

	@Test void b01_beanArray() {
		var in = a(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("f");
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(bs, in, sa));
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(in, "f"));
	}

	@Test void b02_beanArray_reverse() {
		var in = a(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("f-");
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(bs, in, sa));
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(in, "f-"));
	}

	@Test void b03_beanArrayContainingNulls() {
		var in = a(A.create("c"),A.create("a"),null,null,A.create("b"));
		var sa = SortArgs.create("f");
		assertJson("[null,null,{f:'a'},{f:'b'},{f:'c'}]", os.run(bs, in, sa));
		assertJson("[null,null,{f:'a'},{f:'b'},{f:'c'}]", os.run(in, "f"));
	}

	@Test void b04_beanArrayContainingDups() {
		var in = a(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		var sa = SortArgs.create("f");
		assertJson("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", os.run(bs, in, sa));
		assertJson("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", os.run(in, "f"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_beanList() {
		var in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("f");
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(bs, in, sa));
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(in, "f"));
	}

	@Test void c02_beanList_reverse() {
		var in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("f-");
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(bs, in, sa));
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(in, "f-"));
	}

	@Test void c03_beanListContainingNull() {
		var in = list(A.create("c"),A.create("a"),null,null,A.create("b"));
		var sa = SortArgs.create("f");
		assertJson("[null,null,{f:'a'},{f:'b'},{f:'c'}]", os.run(bs, in, sa));
		assertJson("[null,null,{f:'a'},{f:'b'},{f:'c'}]", os.run(in, "f"));
	}

	@Test void c04_beanListContainingDups() {
		var in = list(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		var sa = SortArgs.create("f");
		assertJson("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", os.run(bs, in, sa));
		assertJson("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", os.run(in, "f"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sets
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_beanSet() {
		var in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("f");
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(bs, in, sa));
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(in, "f"));
	}

	@Test void d02_beanSet_reverse() {
		var in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("f-");
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(bs, in, sa));
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(in, "f-"));
	}

	@Test void d03_beanSetContainingNull() {
		var in = set(A.create("c"),A.create("a"),null,null,A.create("b"));
		var sa = SortArgs.create("f");
		assertJson("[null,{f:'a'},{f:'b'},{f:'c'}]", os.run(bs, in, sa));
		assertJson("[null,{f:'a'},{f:'b'},{f:'c'}]", os.run(in, "f"));
	}

	@Test void d04_beanSetContainingDups() {
		var in = set(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		var sa = SortArgs.create("f");
		assertJson("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", os.run(bs, in, sa));
		assertJson("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", os.run(in, "f"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_listOfMaps() {
		var in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		var sa = SortArgs.create("f");
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(bs, in, sa));
		assertJson("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", os.run(in, "f"));
	}

	@Test void e02_listOfMaps_reverse() {
		var in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		var sa = SortArgs.create("f-");
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(bs, in, sa));
		assertJson("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", os.run(in, "f-"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_listOfOther() {
		var in = list(list("c"),list("a"),list("b"));
		var sa = SortArgs.create("f");
		assertJson("[['c'],['a'],['b']]", os.run(bs, in, sa));
		assertJson("[['c'],['a'],['b']]", os.run(in, "f"));
	}

	@Test void f02_listOfOther_reverse() {
		var in = list(list("c"),list("a"),list("b"));
		var sa = SortArgs.create("f-");
		assertJson("[['c'],['a'],['b']]", os.run(bs, in, sa));
		assertJson("[['c'],['a'],['b']]", os.run(in, "f-"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_nonExistentField() {
		var in = a(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		var sa = SortArgs.create("fx");
		assertJson("[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]", os.run(bs, in, sa));
		assertJson("[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]", os.run(in, "fx"));
	}

	public static class B {
		public Object f;

		public static B create(Object f) {
			var b = new B();
			b.f = f;
			return b;
		}
	}

	// Should gracefully handle different sorting data types.
	@Test void g02_mixtureOfTypes() {
		var in = a(B.create(1),B.create(true),B.create("a"));
		var sa = SortArgs.create("f");
		assertJson("[{f:1},{f:true},{f:'a'}]", os.run(bs, in, sa));
		assertJson("[{f:1},{f:true},{f:'a'}]", os.run(in, "f"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sort by multiple columns.
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public int f1;
		public float f2;

		public static C create(int f1, float f2) {
			var c = new C();
			c.f1 = f1;
			c.f2 = f2;
			return c;
		}
	}

	@Test void h01_sortMultipleColumns() {
		var in = a(C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2));
		var sa = SortArgs.create("f1,f2");
		assertJson("[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]", os.run(bs, in, sa));
		assertJson("[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]", os.run(in, "f1,f2"));
	}

	@Test void h02_sortMultipleColumns_descending() {
		var in = a(C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2));
		var sa = SortArgs.create("f1-,f2-");
		assertJson("[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]", os.run(bs, in, sa));
		assertJson("[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]", os.run(in, "f1-,f2-"));
	}

	@Test void h03_sortMultipleColumns_ascendingAndDescending() {
		var in = a(C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2));
		var sa = SortArgs.create("f1-,f2+");
		assertJson("[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]", os.run(bs, in, sa));
		assertJson("[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]", os.run(in, "f1-,f2+"));
	}
}