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
import static java.util.Optional.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.collections.*;
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
		assertObject((Object)null).doesNotExist();
		assertObject(empty()).doesNotExist();
		assertObject(1).exists();
		assertObject(of(1)).exists();

		assertThrown(()->assertObject(empty()).isType(null)).is("Value was null.");
		assertThrown(()->assertObject("foo").isType(null)).is("Parameter 'parent' cannot be null.");
		assertObject("foo").isType(String.class);
		assertObject("foo").isType(CharSequence.class);
		assertObject("foo").isType(Comparable.class);
		assertThrown(()->assertObject(1).isType(String.class)).is("Unexpected class.\n\tExpect=[java.lang.String]\n\tActual=[java.lang.Integer]");

		assertObject("foo").asString(JsonSerializer.DEFAULT).is("\"foo\"");
		assertObject(empty()).asString(JsonSerializer.DEFAULT).is("null");

		assertThrown(()->assertObject(new A1()).asJson()).contains("Could not call getValue() on property 'foo'");

		assertObject("foo").asJson().is("'foo'");
		assertObject(empty()).asString(JsonSerializer.DEFAULT).is("null");

		assertObject(new A2()).asJsonSorted().is("{bar:2,foo:1}");

		int[] x1 = {1,2}, x2 = {2,1};
		assertObject(x2).asJsonSorted().is("[1,2]");
		assertThrown(()->assertObject(x2).asJsonSorted().is("[2,1]")).is("Unexpected value.\n\tExpect=[[2,1]]\n\tActual=[[1,2]]");
		assertObject(empty()).asJsonSorted().is("null");

		assertObject(x1).isSameJsonAs(x1);
		assertThrown(()->assertObject(x1).isSameJsonAs(x2)).is("Unexpected comparison.\n\tExpect=[[2,1]]\n\tActual=[[1,2]]");
		assertObject(empty()).isSameJsonAs(null);
		assertThrown(()->assertObject(new A1()).isSameJsonAs(null)).contains("Could not call getValue() on property 'foo'");

		assertObject(x1).isSameSortedAs(x1);
		assertObject(x1).isSameSortedAs(x2);
		assertThrown(()->assertObject(x1).isSameJsonAs(null)).is("Unexpected comparison.\n\tExpect=[null]\n\tActual=[[1,2]]");
		assertObject(empty()).isSameSortedAs(null);

		assertObject(x1).doesNotEqual(null);
		assertObject(empty()).doesNotEqual(x1);
		assertObject(x1).doesNotEqual(x2);
		assertThrown(()->assertObject(empty()).doesNotEqual(null)).is("Unexpected value.\n\tExpected not=[null]\n\tActual=[null]");
		assertThrown(()->assertObject(x1).doesNotEqual(x1)).is("Unexpected value.\n\tExpected not=[[1,2]]\n\tActual=[[1,2]]");

		assertObject(x1).passes(x->x != null);
		assertThrown(()->assertObject(x1).passes(x->x == null)).is("Value did not pass predicate test.\n\tValue=[[1,2]]");

		assertObject(x1).passes(x->x[0] == 1);
		assertThrown(()->assertObject(x1).passes(x->x[0]==2)).is("Value did not pass predicate test.\n\tValue=[[1,2]]");

		assertObject(x1).isNot(null);

		assertObject(x1).isAny(x1,x2);
		assertThrown(()->assertObject(x1).isAny(x2)).is("Expected value not found.\n\tExpect=[[[2,1]]]\n\tActual=[[1,2]]");

		assertObject(x1).isNotAny(x2);
		assertThrown(()->assertObject(x1).isNotAny(x1,x2)).is("Unexpected value found.\n\tUnexpected=[[1,2]]\n\tActual=[[1,2]]");

		Date d1 = new Date(0), d2 = new Date(0);
		assertObject(d1).is(d2);

		assertObject(123).asString().is("123");
		assertObject((Object)null).asString().isNull();

		assertObject(123).asString(x -> x.toString()).is("123");
		assertObject(123).asString(Integer.class, x -> String.valueOf(x.intValue())).is("123");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ObjectAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ObjectAssertion.create(null).stdout().silent();
	}

	@Test
	public void a03_conversions() throws Exception {
		assertObject(new String[]{"foo"}).asArray().item(0).is("foo");
		assertThrown(()->assertObject("foo").asArray()).contains("Object was not an array");

		assertObject(true).asBoolean().isTrue();
		assertThrown(()->assertObject("foo").asBoolean()).contains("Object was not type 'java.lang.Boolean'.  Actual='java.lang.String'");

		assertObject(new byte[]{123}).asByteArray().asJson().is("[123]");
		assertObject(AList.of(123)).asCollection().asJson().is("[123]");
		assertObject(123).asComparable().asJson().is("123");
		assertObject(new Date()).asDate().isType(Date.class);
		assertObject(123).asInteger().asJson().is("123");
		assertObject(AList.of(123)).asList().asJson().is("[123]");
		assertObject(123l).asLong().asJson().is("123");
		assertObject(AMap.create()).asMap().asJson().is("{}");
		assertObject(ZonedDateTime.now()).asZonedDateTime().isType(ZonedDateTime.class);
	}
}
