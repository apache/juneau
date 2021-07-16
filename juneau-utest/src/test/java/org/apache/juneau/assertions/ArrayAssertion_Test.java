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
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ArrayAssertion_Test {

	private <E> ArrayAssertion<E> test(E[] value) {
		return assertArray(value).silent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		String[] x1={}, x2={"foo","bar"};

		assertThrown(()->test(null).exists()).message().is("Value was null.");
		test(x1).exists();

		test(null).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).message().is("Value was not null.");

		test(null).item(0).doesNotExist();
		test(x1).item(0).doesNotExist();
		test(x2).item(0).exists();

	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test(null).stdout();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_apply() throws Exception {
		String[] x1={}, x2={"foo","bar"};
		test(x1).apply(x -> x2).asJson().is("['foo','bar']");
	}

	@Test
	public void b02_asString() throws Exception {
		String[] x={"foo","bar"};
		test(x).asString().is("[foo, bar]");
		test(null).asString().isNull();
	}

	@Test
	public void b03_asBeanList() throws Exception {
		ABean[] x = new ABean[]{ABean.get(),ABean.get()};
		test(x).asBeanList().property("a").asJson().is("[1,1]");
		test(null).asBeanList().isNull();
	}

	@Test
	public void b04_item() throws Exception {
		ABean[] x = new ABean[]{ABean.get(),ABean.get()};
		test(x).item(0).asBean().property("a").is(1);
		test(x).item(-1).doesNotExist();
		test(x).item(2).doesNotExist();
		test((Object[])null).item(0).doesNotExist();
	}

	@Test
	public void b05_sorted() throws Exception {
		Integer[] x = new Integer[]{2,3,1};
		test(x).sorted().asJson().is("[1,2,3]");
		test(x).sorted(Comparator.reverseOrder()).asJson().is("[3,2,1]");
		test(x).sorted(null).asJson().is("[1,2,3]");
		test(null).sorted().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_any() throws Exception {
		Integer[] x = new Integer[]{2,3,1};
		test(x).any(y -> y .equals(3));
		test(x).any(eq(3));
		assertThrown(()->test(x).any(y -> y.equals(4))).message().is("Array did not contain any matching value.\n\tValue='[2, 3, 1]'.");
		assertThrown(()->test(x).any(null)).message().is("Argument 'test' cannot be null.");
	}

	@Test
	public void c02_all() throws Exception {
		Integer[] x = new Integer[]{2,3,1};
		test(x).all(y -> y < 4);
		assertThrown(()->test(x).all(y -> y < 3)).message().is("Array contained non-matching value at index 1.\n\tUnexpected value: '3'.");
		assertThrown(()->test(x).all(ne(3))).message().is("Array contained non-matching value at index 1.\n\tValue unexpectedly matched.\n\tValue='3'.");
		assertThrown(()->test(x).all(null)).message().is("Argument 'test' cannot be null.");
	}

	@Test
	public void c03_isEmpty() throws Exception {
		String[] x1={}, x2={"foo","bar"};
		assertThrown(()->test(null).isEmpty()).message().is("Value was null.");
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).message().is("Array was not empty.");
	}

	@Test
	public void c04_isNotEmpty() throws Exception {
		String[] x1={}, x2={"foo","bar"};
		assertThrown(()->test(null).isNotEmpty()).message().is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).message().is("Array was empty.");
		test(x2).isNotEmpty();
	}

	@Test
	public void c05_isSize() throws Exception {
		String[] x1={}, x2={"foo","bar"};
		assertThrown(()->test(null).isSize(0)).message().is("Value was null.");
		test(x1).isSize(0);
		assertThrown(()->test(x1).isSize(2)).message().is("Array did not have the expected size.\n\tExpect=2.\n\tActual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(x2).isSize(0)).message().is("Array did not have the expected size.\n\tExpect=0.\n\tActual=2.");
	}

	@Test
	public void c06_contains() throws Exception {
		String[] x1={null,"foo","bar"};
		test(x1).contains("foo");
		test(x1).contains((String)null);
		assertThrown(()->test(x1).contains("z")).message().is("Array did not contain expected value.\n\tExpect='z'.\n\tActual='[null, foo, bar]'.");
		test(x1).contains((Object)"foo");
		test(x1).contains((Object)null);
		assertThrown(()->test(x1).contains((Object)"z")).message().is("Array did not contain expected value.\n\tExpect='z'.\n\tActual='[null, foo, bar]'.");

		Integer[] x2={null,1,2};
		test(x2).contains("1");
		assertThrown(()->test(x2).contains("3")).message().is("Array did not contain expected value.\n\tExpect='3'.\n\tActual='[null, 1, 2]'.");
		test(x2).contains(1);
		assertThrown(()->test(x2).contains(3)).message().is("Array did not contain expected value.\n\tExpect='3'.\n\tActual='[null, 1, 2]'.");
	}

	@Test
	public void c07_doesNotContain() throws Exception {
		String[] x1 = {null,"foo","bar"};
		test(x1).doesNotContain("baz");
		assertThrown(()->test(x1).doesNotContain("foo")).message().is("Array contained unexpected value.\n\tUnexpected='foo'.\n\tActual='[null, foo, bar]'.");
		assertThrown(()->test(x1).doesNotContain((String)null)).message().is("Array contained unexpected value.\n\tUnexpected='null'.\n\tActual='[null, foo, bar]'.");
		test(x1).doesNotContain((Object)"baz");
		assertThrown(()->test(x1).doesNotContain((Object)"foo")).message().is("Array contained unexpected value.\n\tUnexpected='foo'.\n\tActual='[null, foo, bar]'.");
		assertThrown(()->test(x1).doesNotContain((Object)null)).message().is("Array contained unexpected value.\n\tUnexpected='null'.\n\tActual='[null, foo, bar]'.");

		Integer[] x2={null,1,2};
		test(x2).doesNotContain("3");
		assertThrown(()->test(x2).doesNotContain("1")).message().is("Array contained unexpected value.\n\tUnexpected='1'.\n\tActual='[null, 1, 2]'.");
		test(x2).doesNotContain(3);
		assertThrown(()->test(x2).doesNotContain(1)).message().is("Array contained unexpected value.\n\tUnexpected='1'.\n\tActual='[null, 1, 2]'.");
	}

	@Test
	public void c08_equals() throws Exception {
		String[] x1 = {null,"foo","bar"};
		test(x1).equals(null,"foo","bar");
		assertThrown(()->test(x1).equals(null,"foo","baz")).message().is("Array did not contain expected value at index 2.\n\tValue did not match expected.\n\tExpect='baz'.\n\tActual='bar'.");

		Integer[] x2={null,1,2};
		test(x2).equals(null,1,2);
		test(x2).equals(null,"1","2");
		assertThrown(()->test(x2).equals(null,1,3)).message().is("Array did not contain expected value at index 2.\n\tValue did not match expected.\n\tExpect='3'.\n\tActual='2'.");
	}

	@Test
	public void c09_is() throws Exception {
		String[] x1 = {null,"foo","bar"};
		test(x1).is(null,"foo","bar");
		assertThrown(()->test(x1).is(null,"foo","baz")).message().is("Array did not contain expected value at index 2.\n\tValue did not match expected.\n\tExpect='baz'.\n\tActual='bar'.");
		test(x1).is(isNull(),eq("foo"),eq("bar"));
		assertThrown(()->test(x1).is(isNull(),eq("foo"),eq("baz"))).message().is("Array did not contain expected value at index 2.\n\tValue did not match expected.\n\tExpect='baz'.\n\tActual='bar'.");
		test(x1).is((Predicate<String>)null,null,null);

		Integer[] x2={null,1,2};
		test(x2).is(null,1,2);
		test(x2).is(null,"1","2");
		assertThrown(()->test(x2).is(null,1,3)).message().is("Array did not contain expected value at index 2.\n\tValue did not match expected.\n\tExpect='3'.\n\tActual='2'.");
		test(x2).is(isNull(),eq(1),eq(2));
		test(x2).is(null,eq("1"),eq("2"));
		assertThrown(()->test(x2).is(isNull(),eq(1),eq(3))).message().is("Array did not contain expected value at index 2.\n\tValue did not match expected.\n\tExpect='3'.\n\tActual='2'.");
	}
}
