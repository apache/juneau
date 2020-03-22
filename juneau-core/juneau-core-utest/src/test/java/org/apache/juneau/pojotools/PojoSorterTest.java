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
package org.apache.juneau.pojotools;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the PojoPaginator class.
 */
public class PojoSorterTest {

	PojoSorter p = new PojoSorter();
	BeanSession bs = BeanContext.DEFAULT.createBeanSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void nullInput() {
		assertNull(p.run(bs, null, null));
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
	public void beanArray() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArray_reverse() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = new SortArgs("f-");
		assertObjectEquals("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArrayContainingNulls() {
		Object in = new A[]{A.create("c"),A.create("a"),null,null,A.create("b")};;
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[null,null,{f:'a'},{f:'b'},{f:'c'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanArrayContainingDups() {
		Object in = new A[]{A.create("c"),A.create("a"),null,A.create("a"),A.create("b")};
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanList() {
		Object in = AList.of(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanList_reverse() {
		Object in = AList.of(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = new SortArgs("f-");
		assertObjectEquals("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanListContainingNull() {
		Object in = AList.of(A.create("c"),A.create("a"),null,null,A.create("b"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[null,null,{f:'a'},{f:'b'},{f:'c'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanListContainingDups() {
		Object in = AList.of(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sets
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanSet() {
		Object in = ASet.of(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", p.run(bs, in, sa));
	}

	@Test
	public void betSet_reverse() {
		Object in = ASet.of(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		SortArgs sa = new SortArgs("f-");
		assertObjectEquals("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSetContainingNull() {
		Object in = ASet.of(A.create("c"),A.create("a"),null,null,A.create("b"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[null,{f:'a'},{f:'b'},{f:'c'}]", p.run(bs, in, sa));
	}

	@Test
	public void beanSetContainingDups() {
		Object in = ASet.of(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[null,{f:'a'},{f:'a'},{f:'b'},{f:'c'}]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void emptySort() {
		Object in = ASet.of(A.create("c"),A.create("a"),A.create("b"));
		SortArgs sa = new SortArgs();
		assertObjectEquals("[{f:'c'},{f:'a'},{f:'b'}]", p.run(bs, in, sa));
	}

	@Test
	public void invalidDataType() {
		Object in = AMap.of("a","b");
		SortArgs sa = new SortArgs("x");
		in = p.run(bs, in, sa);
		assertObjectEquals("{a:'b'}", in);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void listOfMaps() {
		Object in = AList.of(AMap.of("f","c"),AMap.of("f","a"),AMap.of("f","b"),AMap.of("f","e"),AMap.of("f","d"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[{f:'a'},{f:'b'},{f:'c'},{f:'d'},{f:'e'}]", p.run(bs, in, sa));
	}

	@Test
	public void listOfMaps_reverse() {
		Object in = AList.of(AMap.of("f","c"),AMap.of("f","a"),AMap.of("f","b"),AMap.of("f","e"),AMap.of("f","d"));
		SortArgs sa = new SortArgs("f-");
		assertObjectEquals("[{f:'e'},{f:'d'},{f:'c'},{f:'b'},{f:'a'}]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void listOfOther() {
		Object in = AList.of(AList.of("c"),AList.of("a"),AList.of("b"));
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[['c'],['a'],['b']]", p.run(bs, in, sa));
	}

	@Test
	public void listOfOther_reverse() {
		Object in = AList.of(AList.of("c"),AList.of("a"),AList.of("b"));
		SortArgs sa = new SortArgs("f-");
		assertObjectEquals("[['c'],['a'],['b']]", p.run(bs, in, sa));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void nonExistentField() {
		Object in = new A[]{A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d")};
		SortArgs sa = new SortArgs("fx");
		assertObjectEquals("[{f:'c'},{f:'a'},{f:'b'},{f:'e'},{f:'d'}]", p.run(bs, in, sa));
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
	public void mixtureOfTypes() {
		Object in = new B[]{B.create(1),B.create(true),B.create("a")};
		SortArgs sa = new SortArgs("f");
		assertObjectEquals("[{f:1},{f:true},{f:'a'}]", p.run(bs, in, sa));
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
	public void sortMultipleColumns() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = new SortArgs("f1","f2");
		assertObjectEquals("[{f1:1,f2:1.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:3,f2:1.0},{f1:3,f2:2.0}]", p.run(bs, in, sa));
	}

	@Test
	public void sortMultipleColumns_descending() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = new SortArgs("f1-","f2-");
		assertObjectEquals("[{f1:3,f2:2.0},{f1:3,f2:1.0},{f1:2,f2:2.0},{f1:2,f2:1.0},{f1:1,f2:1.0}]", p.run(bs, in, sa));
	}

	@Test
	public void sortMultipleColumns_ascendingAndDescending() {
		Object in = new C[]{C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2)};
		SortArgs sa = new SortArgs("f1-","f2+");
		assertObjectEquals("[{f1:3,f2:1.0},{f1:3,f2:2.0},{f1:2,f2:1.0},{f1:2,f2:2.0},{f1:1,f2:1.0}]", p.run(bs, in, sa));
	}
}
