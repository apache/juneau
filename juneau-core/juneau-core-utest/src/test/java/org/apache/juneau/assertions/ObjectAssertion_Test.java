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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ObjectAssertion_Test {

	public static class A1 {
		public int getFoo() {
			throw new RuntimeException("foo");
		}
	}

	public static class A2 {
		public int getFoo() {
			return 1;
		}
		public int getBar() {
			return 2;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		assertObject(null).doesNotExist();
		assertObject(1).exists();

		assertThrown(()->assertObject(null).isType(null)).is("Value was null.");
		assertThrown(()->assertObject("foo").isType(null)).is("Parameter 'parent' cannot be null.");
		assertObject("foo").isType(String.class);
		assertObject("foo").isType(CharSequence.class);
		assertObject("foo").isType(Comparable.class);
		assertThrown(()->assertObject(1).isType(String.class)).is("Unexpected class.\n\tExpected=[java.lang.String]\n\tActual=[java.lang.Integer]");

		assertObject("foo").serialized(JsonSerializer.DEFAULT).is("\"foo\"");
		assertObject(null).serialized(JsonSerializer.DEFAULT).is("null");

		assertThrown(()->assertObject(new A1()).json()).contains("Could not call getValue() on property 'foo'");

		assertObject("foo").json().is("'foo'");
		assertObject(null).serialized(JsonSerializer.DEFAULT).is("null");

		assertObject(new A2()).jsonSorted().is("{bar:2,foo:1}");

		int[] x1 = {1,2}, x2 = {2,1};
		assertObject(x2).jsonSorted().is("[1,2]");
		assertThrown(()->assertObject(x2).jsonSorted().is("[2,1]")).stderr().is("Unexpected value.\n\tExpected=[[2,1]]\n\tActual=[[1,2]]");
		assertObject(null).jsonSorted().is("null");

		assertObject(x1).sameAs(x1);
		assertThrown(()->assertObject(x1).sameAs(x2)).stderr().is("Unexpected comparison.\n\tExpected=[[2,1]]\n\tActual=[[1,2]]");
		assertObject(null).sameAs(null);
		assertThrown(()->assertObject(new A1()).sameAs(null)).contains("Could not call getValue() on property 'foo'");

		assertObject(x1).sameAsSorted(x1);
		assertObject(x1).sameAsSorted(x2);
		assertThrown(()->assertObject(x1).sameAs(null)).stderr().is("Unexpected comparison.\n\tExpected=[null]\n\tActual=[[1,2]]");
		assertObject(null).sameAsSorted(null);

		assertObject(x1).doesNotEqual(null);
		assertObject(null).doesNotEqual(x1);
		assertObject(x1).doesNotEqual(x2);
		assertThrown(()->assertObject(null).doesNotEqual(null)).is("Unexpected value.\n\tExpected not=[null]\n\tActual=[null]");
		assertThrown(()->assertObject(x1).doesNotEqual(x1)).is("Unexpected value.\n\tExpected not=[[1,2]]\n\tActual=[[1,2]]");

		assertObject(x1).passes(x->x != null);
		assertThrown(()->assertObject(x1).passes(x->x == null)).stderr().is("Value did not pass predicate test.\n\tValue=[[1,2]]");

		assertObject(x1).passes(int[].class, x->x[0] == 1);
		assertThrown(()->assertObject(x1).passes(int[].class, x->x[0]==2)).stderr().is("Value did not pass predicate test.\n\tValue=[[1,2]]");

		assertObject(x1).isNot(null);

		assertObject(x1).isAny(x1,x2);
		assertThrown(()->assertObject(x1).isAny(x2)).stderr().is("Expected value not found.\n\tExpected=[[[2,1]]]\n\tActual=[[1,2]]");

		assertObject(x1).isNotAny(x2);
		assertThrown(()->assertObject(x1).isNotAny(x1,x2)).stderr().is("Unexpected value found.\n\tUnexpected=[[1,2]]\n\tActual=[[1,2]]");

		Date d1 = new Date(0), d2 = new Date(0);
		assertObject(d1).is(d2);
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ObjectAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ObjectAssertion.create(null).stdout().stderr();
	}
}
