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
import static org.apache.juneau.assertions.AssertionPredicates.eq;
import static org.apache.juneau.assertions.AssertionPredicates.ne;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
public class ArrayAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	public static final A A = new A();
	public static class A {
		public int a = 1, b = 2;
		@Override public String toString() { return "a="+a+",b="+b; }
	}

	private <E> ArrayAssertion<E> test(E[] value) {
		return assertArray(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_msg() {
		assertThrows(BasicAssertionError.class, ()->test(null).setMsg("Foo {0}", 1).isExists(), "Foo 1");
		assertThrows(RuntimeException.class, ()->test(null).setMsg("Foo {0}", 1).setThrowable(RuntimeException.class).isExists(), "Foo 1");
	}

	@Test void a02_stdout() {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ba01a_asString() {
		var x = a(1,2);
		var nil = na(Integer.class);
		test(x).asString().is("[1, 2]");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = a(1,2);
		var nil = na(Integer.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("[1,2]");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = a(1,2);
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = a(1,2);
		var nil = na(Integer.class);
		test(x).asJson().is("[1,2]");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x1 = a(2,1);
		var nil = na(Integer.class);
		test(x1).asJsonSorted().is("[1,2]");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = a(1,2);
		var x2 = a(3,4);
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_asStrings() {
		var x1 = a(1,2);
		var nil = na(Integer.class);
		test(x1).asStrings().asJoin().is("12");
		test(nil).asStrings().isNull();
	}

	@Test void bb02_asBeanList() {
		var x = a(A,A);
		var nil = na(Integer.class);
		test(x).asBeanList().asProperty("a").asJson().is("[1,1]");
		test(nil).asBeanList().isNull();
	}

	@Test void bb03_item() {
		var x = a(A,A);
		var nil = na(Integer.class);
		test(x).asItem(0).asBean().asProperty("a").is(1);
		test(x).asItem(-1).isNull();
		test(x).asItem(2).isNull();
		test(nil).asItem(0).isNull();
	}

	@Test void bb04a_sorted() {
		var x = a(2,3,1);
		var nil = na(Integer.class);
		test(x).asSorted().asJson().is("[1,2,3]");
		test(x).asSorted(Comparator.reverseOrder()).asJson().is("[3,2,1]");
		test(x).asSorted(null).asJson().is("[1,2,3]");
		test(nil).asSorted().isNull();
	}

	@Test void bb04b_sorted_wComparator() {
		var x = a(2,3,1);
		var nil = na(Integer.class);
		test(x).asSorted().asJson().is("[1,2,3]");
		test(x).asSorted(Comparator.reverseOrder()).asJson().is("[3,2,1]");
		test(x).asSorted(null).asJson().is("[1,2,3]");
		test(nil).asSorted().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = ea(Integer.class);
		var nil = na(Integer.class);
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = ea(Integer.class);
		var nil = na(Integer.class);
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = ea(Integer.class);
		var nil = na(Integer.class);
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var x1b = a(null,1,3);
		var nil = na(Integer.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x1b)).asMessage().asOneLine().is("Unexpected value.  Expect='[null, 1, 3]'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(nil).is(x1b)).asMessage().asOneLine().is("Unexpected value.  Expect='[null, 1, 3]'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = a(null,1,2);
		test(x1).is(x->x.length==3);
		assertThrown(()->test(x1).is(x->x.length==2)).asMessage().asOneLine().is("Unexpected value: '[null, 1, 2]'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[null, 1, 2]'.");
	}

	@Test void ca05_isNot() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var x2 = a(null,1,3);
		var nil = na(Integer.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[null, 1, 2]'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var x2 = a(null,1,3);
		var nil = na(Integer.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[null, 1, 3]]'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[null, 1, 3]]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var x2 = a(null,1,3);
		var nil = na(Integer.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='[null, 1, 2]'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var nil = na(Integer.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[null, 1, 2](Integer[]@*)'.  Actual='[null, 1, 2](Integer[]@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[null, 1, 2](Integer[]@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='[null, 1, 2](Integer[]@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var x2 = a(null,1,3);
		var nil = na(Integer.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[null,1,3]'.  Actual='[null,1,2]'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[null,1,3]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[null,1,2]'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = a(1,2);
		var x1a = a(2,1);
		var x2 = a(1,3);
		var nil = na(Integer.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[1,3]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[1,3]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = a(null,1,2);
		var x1a = a(null,1,2);
		var x2 = a(null,1,3);
		var nil = na(Integer.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[null,1,3]'.  Actual='[null,1,2]'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[null,1,3]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[null,1,2]'.");
	}

	@Test void ca12_isType() {
		var x = ea(Integer.class);
		var nil = na(Integer.class);
		test(x).isType(Integer[].class);
		test(x).isType(Object[].class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String[].class)).asMessage().asOneLine().is("Unexpected type.  Expect='[Ljava.lang.String;'.  Actual='[Ljava.lang.Integer;'.");
		assertThrown(()->test(nil).isType(Integer[].class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = ea(Integer.class);
		var nil = na(Integer.class);
		test(x).isExactType(Integer[].class);
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='[Ljava.lang.Integer;'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = a(null,1,2);
		var nil = na(Integer.class);
		test(x).isString("[null, 1, 2]");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = a(null,1,2);
		var nil = na(Integer.class);
		test(x).isJson("[null,1,2]");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[null,1,2]'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[null,1,2]'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_is_predicates() {
		var x1 = a(null,1,2);
		var nil = na(Integer.class);
		test(x1).is(isNull(),eq("1"),eq("2"));
		test(x1).is(isNull(),eq(1),eq(2));
		assertThrown(()->test(x1).is(isNull(),eq("1"),eq("3"))).asMessage().asOneLine().is("Array did not contain expected value at index 2.  Value did not match expected.  Expect='3'.  Actual='2'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).is(isNull(),eq("1"),eq("3")), "Value was null.");
		test(x1).is((Predicate<Integer>)null,null,null);
	}

	@Test void cb02_any() {
		var x1 = a(2,3,1);
		var nil = na(Integer.class);
		test(x1).isAny(x -> x .equals(3));
		test(x1).isAny(eq(3));
		assertThrown(()->test(x1).isAny(x -> x.equals(4))).asMessage().asOneLine().is("Array did not contain any matching value.  Value='[2, 3, 1]'.");
		assertThrows(IllegalArgumentException.class, ()->test(x1).isAny((Predicate<Integer>)null), "Argument 'test' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAny(x->true), "Value was null.");
	}

	@Test void cb03_all() {
		var x1 = a(2,3,1);
		var nil = na(Integer.class);
		test(x1).isAll(x -> x < 4);
		assertThrown(()->test(x1).isAll(x -> x < 3)).asMessage().asOneLine().is("Array contained non-matching value at index 1.  Unexpected value: '3'.");
		assertThrown(()->test(x1).isAll(ne(3))).asMessage().asOneLine().is("Array contained non-matching value at index 1.  Value unexpectedly matched.  Value='3'.");
		assertThrows(IllegalArgumentException.class, ()->test(x1).isAll(null), "Argument 'test' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAll(x->true), "Value was null.");
	}

	@Test void cb04_isEmpty() {
		var x1 = ea(String.class);
		var x2 = a("foo","bar");
		var nil = na(String.class);
		test(x1).isEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x2).isEmpty(), "Array was not empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isEmpty(), "Value was null.");
	}

	@Test void cb05_isNotEmpty() {
		var x1 = ea(String.class);
		var x2 = a("foo","bar");
		var nil = na(String.class);
		test(x2).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x1).isNotEmpty(), "Array was empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotEmpty(), "Value was null.");
	}

	@Test void cb06_contains() {
		var x1 = a(null,1,2);
		var nil = na(Integer.class);
		test(x1).isContains(null);
		test(x1).isContains(1);
		assertThrown(()->test(x1).isContains(3)).asMessage().asOneLine().is("Array did not contain expected value.  Expect='3'.  Actual='[null, 1, 2]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContains(3), "Value was null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContains(null), "Value was null.");
	}

	@Test void cb07_doesNotContain() {
		var x1 = a(null,1,2);
		var nil = na(Integer.class);
		test(x1).isNotContains(3);
		assertThrown(()->test(x1).isNotContains(1)).asMessage().asOneLine().is("Array contained unexpected value.  Unexpected='1'.  Actual='[null, 1, 2]'.");
		assertThrown(()->test(x1).isNotContains(null)).asMessage().asOneLine().is("Array contained unexpected value.  Unexpected='null'.  Actual='[null, 1, 2]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotContains(3), "Value was null.");
	}

	@Test void cb08_isSize() {
		var x1 = ea(String.class);
		var x2 = a("foo","bar");
		var nil = na(String.class);
		test(x1).isSize(0);
		test(x2).isSize(2);
		assertThrown(()->test(x1).isSize(2)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=2.  Actual=0.");
		assertThrown(()->test(x2).isSize(0)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=0.  Actual=2.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isSize(0), "Value was null.");
	}

	@Test void cb09_has() {
		var x= a("foo","bar");
		var nil = na(String.class);
		test(x).isHas("foo","bar");
		assertThrown(()->test(x).isHas("foo","baz")).asMessage().asOneLine().is("Array did not contain expected value at index 1.  Value did not match expected.  Expect='baz'.  Actual='bar'.");
		assertThrown(()->test(x).isHas("foo")).asMessage().asOneLine().is("Array did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x).isHas(nil)).asMessage().asOneLine().is("Argument 'entries' cannot be null.");
		assertThrown(()->test(nil).isHas("foo")).asMessage().asOneLine().is("Value was null.");
	}
}