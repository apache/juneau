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

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ObjectAssertion_Test {

	private <T> ObjectAssertion<T> test(T value) {
		return assertObject(value).silent();
	}

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
		test((Object)null).isNull();
		test(1).exists();

		assertThrown(()->test("foo").isType(null)).message().is("Argument 'parent' cannot be null.");
		test("foo").isType(String.class);
		test("foo").isType(CharSequence.class);
		test("foo").isType(Comparable.class);
		assertThrown(()->test(1).isType(String.class)).message().is("Unexpected type.\n\tExpect='java.lang.String'.\n\tActual='java.lang.Integer'.");

		test("foo").asString(JsonSerializer.DEFAULT).is("\"foo\"");

		assertThrown(()->test(new A1()).asJson()).messages().any(contains("Could not call getValue() on property 'foo'"));

		test("foo").asJson().is("'foo'");

		test(new A2()).asJsonSorted().is("{bar:2,foo:1}");

		int[] x1 = {1,2}, x2 = {2,1};
		test(x2).asJsonSorted().is("[1,2]");
		assertThrown(()->test(x2).asJsonSorted().is("[2,1]")).message().is("Unexpected value.\n\tExpect='[2,1]'.\n\tActual='[1,2]'.");

		test(x1).isSameJsonAs(x1);
		assertThrown(()->test(x1).isSameJsonAs(x2)).message().is("Unexpected comparison.\n\tExpect='[2,1]'.\n\tActual='[1,2]'.");
		assertThrown(()->test(new A1()).isSameJsonAs(null)).messages().any(contains("Could not call getValue() on property 'foo'"));

		test(x1).isSameSortedAs(x1);
		test(x1).isSameSortedAs(x2);
		assertThrown(()->test(x1).isSameJsonAs(null)).message().is("Unexpected comparison.\n\tExpect='null'.\n\tActual='[1,2]'.");

		test(x1).isNotEqual(null);
		test(x1).isNotEqual(x2);

		assertThrown(()->test(x1).isNotEqual(x1)).message().is("Unexpected value.\n\tDid not expect='[1, 2]'.\n\tActual='[1, 2]'.");

		test(x1).passes(x->x != null);
		assertThrown(()->test(x1).passes(x->x == null)).message().is("Unexpected value: '[1, 2]'.");

		test(x1).passes(x->x[0] == 1);
		assertThrown(()->test(x1).passes(x->x[0]==2)).message().is("Unexpected value: '[1, 2]'.");

		test(x1).isNot(null);

		test(x1).isAny(x1,x2);
		assertThrown(()->test(x1).isAny(x2)).message().is("Expected value not found.\n\tExpect='[[2, 1]]'.\n\tActual='[1, 2]'.");

		test(x1).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1,x2)).message().is("Unexpected value found.\n\tUnexpected='[1, 2]'.\n\tActual='[1, 2]'.");

		Date d1 = new Date(0), d2 = new Date(0);
		test(d1).is(d2);

		test(123).asString().is("123");
		test((Object)null).asString().isNull();

		test(123).asString(x -> x.toString()).is("123");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((Object)null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((Object)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((Object)null).stdout();
	}

	@Test
	public void a03_conversions() throws Exception {
		test(new String[]{"foo"}).asArray(String.class).item(0).is("foo");
		assertThrown(()->test("foo").asArray(String.class)).message().contains("Object was not type 'java.lang.String[]'.\n\tActual='java.lang.String'.");

		test(true).asBoolean().isTrue();
		assertThrown(()->test("foo").asBoolean()).message().contains("Object was not type 'java.lang.Boolean'.\n\tActual='java.lang.String'.");

		test(new byte[]{123}).asByteArray().asJson().is("[123]");
		test(AList.of(123)).asCollection().asJson().is("[123]");
		test(123).asComparable().asJson().is("123");
		test(new Date()).asDate().isType(Date.class);
		test(123).asInteger().asJson().is("123");
		test(AList.of(123)).asList().asJson().is("[123]");
		test(123l).asLong().asJson().is("123");
		test(AMap.create()).asMap().asJson().is("{}");
		test(ZonedDateTime.now()).asZonedDateTime().isType(ZonedDateTime.class);
	}
}
