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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.junit.*;

/**
 * Tests the PojoPaginator class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ObjectSorter_Test {

	ObjectSorter os = new ObjectSorter();
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_nullInput() {
		assertNull(os.run(bs, null, null));
	}

	@Test
	public void a02_emptySort() {
		Object in = set(A.create("c"),A.create("a"),A.create("b"));
		SortArgs sa = SortArgs.create("");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'c'},{f:'a'},{f:'b'}]");
		assertObject(os.run(in, "")).asJson().is("[{f:'c'},{f:'a'},{f:'b'}]");
	}

	@Test
	public void a03_invalidDataType() {
		Object in = map("a","b");
		SortArgs sa = SortArgs.create("x");
		assertObject(os.run(bs, in, sa)).asJson().is("{a:'b'}");
		assertObject(os.run(in, "x")).isNull();
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

	@Test
	public void b01_beanArray() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertObject(os.run(in, "f")).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test
	public void b02_beanArray_reverse() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = SortArgs.create("f-");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertObject(os.run(in, "f-")).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	@Test
	public void b03_beanArrayContainingNulls() {
		Object in = new A[]{A.create("c"),A.create("a"),null,null,A.create("b")};;
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[null,null,{f:'a'},{f:'b'},{f:'c'}]");
		assertObject(os.run(in, "f")).asJson().is("[null,null,{f:'a'},{f:'b'},{f:'c'}]");
	}

	@Test
	public void b04_beanArrayContainingDups() {
		Object in = new A[]{A.create("c"),A.create("a"),null,A.create("a"),A.create("b")};
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
		assertObject(os.run(in, "f")).asJson().is("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_beanList() {
		Object in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertObject(os.run(in, "f")).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test
	public void c02_beanList_reverse() {
		Object in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f-");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertObject(os.run(in, "f-")).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	@Test
	public void c03_beanListContainingNull() {
		Object in = list(A.create("c"),A.create("a"),null,null,A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[null,null,{f:'a'},{f:'b'},{f:'c'}]");
		assertObject(os.run(in, "f")).asJson().is("[null,null,{f:'a'},{f:'b'},{f:'c'}]");
	}

	@Test
	public void c04_beanListContainingDups() {
		Object in = list(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
		assertObject(os.run(in, "f")).asJson().is("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sets
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_beanSet() {
		Object in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertObject(os.run(in, "f")).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test
	public void d02_beanSet_reverse() {
		Object in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = SortArgs.create("f-");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertObject(os.run(in, "f-")).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	@Test
	public void d03_beanSetContainingNull() {
		Object in = set(A.create("c"),A.create("a"),null,null,A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[null,{f:'a'},{f:'b'},{f:'c'}]");
		assertObject(os.run(in, "f")).asJson().is("[null,{f:'a'},{f:'b'},{f:'c'}]");
	}

	@Test
	public void d04_beanSetContainingDups() {
		Object in = set(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
		assertObject(os.run(in, "f")).asJson().is("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_listOfMaps() {
		Object in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
		assertObject(os.run(in, "f")).asJson().is("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]");
	}

	@Test
	public void e02_listOfMaps_reverse() {
		Object in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		SortArgs sa = SortArgs.create("f-");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
		assertObject(os.run(in, "f-")).asJson().is("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_listOfOther() {
		Object in = list(list("c"),list("a"),list("b"));
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[['c'],['a'],['b']]");
		assertObject(os.run(in, "f")).asJson().is("[['c'],['a'],['b']]");
	}

	@Test
	public void f02_listOfOther_reverse() {
		Object in = list(list("c"),list("a"),list("b"));
		SortArgs sa = SortArgs.create("f-");
		assertObject(os.run(bs, in, sa)).asJson().is("[['c'],['a'],['b']]");
		assertObject(os.run(in, "f-")).asJson().is("[['c'],['a'],['b']]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_nonExistentField() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = SortArgs.create("fx");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]");
		assertObject(os.run(in, "fx")).asJson().is("[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]");
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
	@Test
	public void g02_mixtureOfTypes() {
		Object in = new B[]{B.create(1),B.create(true),B.create("a")};
		SortArgs sa = SortArgs.create("f");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f:1},{f:true},{f:'a'}]");
		assertObject(os.run(in, "f")).asJson().is("[{f:1},{f:true},{f:'a'}]");
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

	@Test
	public void h01_sortMultipleColumns() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = SortArgs.create("f1,f2");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]");
		assertObject(os.run(in, "f1,f2")).asJson().is("[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]");
	}

	@Test
	public void h02_sortMultipleColumns_descending() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = SortArgs.create("f1-,f2-");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]");
		assertObject(os.run(in, "f1-,f2-")).asJson().is("[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]");
	}

	@Test
	public void h03_sortMultipleColumns_ascendingAndDescending() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = SortArgs.create("f1-,f2+");
		assertObject(os.run(bs, in, sa)).asJson().is("[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]");
		assertObject(os.run(in, "f1-,f2+")).asJson().is("[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]");
	}
}
