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
package org.apache.juneau.objecttools;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the PojoPaginator class.
 */
class ObjectSorter_Test extends TestBase {

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
		assertBeans(os.run(bs, in, sa("")), "f", "c", "a", "b");
		assertBeans(os.run(in, ""), "f", "c", "a", "b");
	}

	@Test void a03_invalidDataType() {
		var in = map("a","b");
		assertBean(os.run(bs, in, sa("x")), "a", "b");
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
		assertBeans(os.run(bs, in, sa("f")), "f", "a", "b", "c", "d", "e");
		assertBeans(os.run(in, "f"), "f", "a", "b", "c", "d", "e");
	}

	@Test void b02_beanArray_reverse() {
		var in = a(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		assertBeans(os.run(bs, in, sa("f-")), "f", "e", "d", "c", "b", "a");
		assertBeans(os.run(in, "f-"), "f", "e", "d", "c", "b", "a");
	}

	@Test void b03_beanArrayContainingNulls() {
		var in = a(A.create("c"),A.create("a"),null,null,A.create("b"));
		assertBeans(os.run(bs, in, sa("f")), "f", "<null>", "<null>", "a", "b", "c");
		assertBeans(os.run(in, "f"), "f", "<null>", "<null>", "a", "b", "c");
	}

	@Test void b04_beanArrayContainingDups() {
		var in = a(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		assertBeans(os.run(bs, in, sa("f")), "f", "<null>", "a", "a", "b", "c");
		assertBeans(os.run(in, "f"), "f", "<null>", "a", "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_beanList() {
		var in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		assertBeans(os.run(bs, in, sa("f")), "f", "a", "b", "c", "d", "e");
		assertBeans(os.run(in, "f"), "f", "a", "b", "c", "d", "e");
	}

	@Test void c02_beanList_reverse() {
		var in = list(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		assertBeans(os.run(bs, in, sa("f-")), "f", "e", "d", "c", "b", "a");
		assertBeans(os.run(in, "f-"), "f", "e", "d", "c", "b", "a");
	}

	@Test void c03_beanListContainingNull() {
		var in = list(A.create("c"),A.create("a"),null,null,A.create("b"));
		assertBeans(os.run(bs, in, sa("f")), "f", "<null>", "<null>", "a", "b", "c");
		assertBeans(os.run(in, "f"), "f", "<null>", "<null>", "a", "b", "c");
	}

	@Test void c04_beanListContainingDups() {
		var in = list(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		assertBeans(os.run(bs, in, sa("f")), "f", "<null>", "a", "a", "b", "c");
		assertBeans(os.run(in, "f"), "f", "<null>", "a", "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sets
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_beanSet() {
		var in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		assertBeans(os.run(bs, in, sa("f")), "f", "a", "b", "c", "d", "e");
		assertBeans(os.run(in, "f"), "f", "a", "b", "c", "d", "e");
	}

	@Test void d02_beanSet_reverse() {
		var in = set(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		assertBeans(os.run(bs, in, sa("f-")), "f", "e", "d", "c", "b", "a");
		assertBeans(os.run(in, "f-"), "f", "e", "d", "c", "b", "a");
	}

	@Test void d03_beanSetContainingNull() {
		var in = set(A.create("c"),A.create("a"),null,null,A.create("b"));
		assertBeans(os.run(bs, in, sa("f")), "f", "<null>", "a", "b", "c");
		assertBeans(os.run(in, "f"), "f", "<null>", "a", "b", "c");
	}

	@Test void d04_beanSetContainingDups() {
		var in = set(A.create("c"),A.create("a"),null,A.create("a"),A.create("b"));
		assertBeans(os.run(bs, in, sa("f")), "f", "<null>", "a", "a", "b", "c");
		assertBeans(os.run(in, "f"), "f", "<null>", "a", "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_listOfMaps() {
		var in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		var sa = sa("f");
		assertBeans(os.run(bs, in, sa), "f", "a", "b", "c", "d", "e");
		assertBeans(os.run(in, "f"), "f", "a", "b", "c", "d", "e");
	}

	@Test void e02_listOfMaps_reverse() {
		var in = list(map("f","c"),map("f","a"),map("f","b"),map("f","e"),map("f","d"));
		assertBeans(os.run(bs, in, sa("f-")), "f", "e", "d", "c", "b", "a");
		assertBeans(os.run(in, "f-"), "f", "e", "d", "c", "b", "a");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists of Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_listOfOther() {
		var in = list(list("c"),list("a"),list("b"));
		assertList(os.run(bs, in, sa("f")), "[c]", "[a]", "[b]");
		assertList(os.run(in, "f"), "[c]", "[a]", "[b]");
	}

	@Test void f02_listOfOther_reverse() {
		var in = list(list("c"),list("a"),list("b"));
		assertList(os.run(bs, in, sa("f-")), "[c]", "[a]", "[b]");
		assertList(os.run(in, "f-"), "[c]", "[a]", "[b]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_nonExistentField() {
		var in = a(A.create("c"),A.create("a"),A.create("b"),A.create("e"),A.create("d"));
		assertBeans(os.run(bs, in, sa("fx")), "f", "c", "a", "b", "e", "d");
		assertBeans(os.run(in, "fx"), "f", "c", "a", "b", "e", "d");
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
		assertBeans(os.run(bs, in, sa("f")), "f", "1", "true", "a");
		assertBeans(os.run(in, "f"), "f", "1", "true", "a");
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
		assertBeans(os.run(bs, in, sa("f1,f2")), "f1,f2", "1,1.0", "2,1.0", "2,2.0", "3,1.0", "3,2.0");
		assertBeans(os.run(in, "f1,f2"), "f1,f2", "1,1.0", "2,1.0", "2,2.0", "3,1.0", "3,2.0");
	}

	@Test void h02_sortMultipleColumns_descending() {
		var in = a(C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2));
		assertBeans(os.run(bs, in, sa("f1-,f2-")), "f1,f2", "3,2.0", "3,1.0", "2,2.0", "2,1.0", "1,1.0");
		assertBeans(os.run(in, "f1-,f2-"), "f1,f2", "3,2.0", "3,1.0", "2,2.0", "2,1.0", "1,1.0");
	}

	@Test void h03_sortMultipleColumns_ascendingAndDescending() {
		var in = a(C.create(1,1),C.create(3,2),C.create(3,1),C.create(2,1),C.create(2,2));
		assertBeans(os.run(bs, in, sa("f1-,f2+")), "f1,f2", "3,1.0", "3,2.0", "2,1.0", "2,2.0", "1,1.0");
		assertBeans(os.run(in, "f1-,f2+"), "f1,f2", "3,1.0", "3,2.0", "2,1.0", "2,2.0", "1,1.0");
	}

	private static SortArgs sa(String value) {
		return SortArgs.create(value);
	}
}