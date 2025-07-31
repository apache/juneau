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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ListAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <E> ListAssertion<E> test(List<E> value) {
		return assertList(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() {
		assertThrown(()->test(null).setMsg("Foo {0}", 1).isExists()).asMessage().is("Foo 1");
		assertThrown(()->test(null).setMsg("Foo {0}", 1).setThrowable(RuntimeException.class).isExists()).isExactType(RuntimeException.class).asMessage().is("Foo 1");
	}

	@Test
	public void a02_stdout() {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() {
		List<Integer> x = alist(1), nil = null;
		test(x).asString().is("[1]");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() {
		List<Integer> x = alist(1), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("[1]");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() {
		List<Integer> x1 = alist(1);
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() {
		List<Integer> x = alist(1), nil = null;
		test(x).asJson().is("[1]");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() {
		List<Integer> x = alist(2,1), nil = null;
		test(x).asJsonSorted().is("[1,2]");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() {
		List<Integer> x1 = alist(1), x2 = alist(2);
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_asStrings() {
		List<Integer> x1 = alist(1), nil = null;
		test(x1).asStrings().asJoin().is("1");
		test(nil).asStrings().isNull();
	}

	@Test
	public void bb02_size() {
		List<Integer> x1 = alist(1), nil = null;
		test(x1).asSize().is(1);
		test(nil).asSize().isNull();
	}

	@Test
	public void bc01_apply2() {
		List<Integer> x1 = alist(1), x2 = alist(2);
		test(x1).asApplied2(x -> x2).is(x2);
	}

	@Test
	public void bc02_item() {
		List<String> x = alist("a"), nil = null;
		test(x).asItem(0).isNotNull();
		test(x).asItem(1).isNull();
		test(x).asItem(-1).isNull();
		test(nil).asItem(0).isNull();
	}

	@Test
	public void bc03a_sorted() {
		List<Integer> x = alist(1,3,2), nil = null;
		test(x).asSorted().isString("[1, 2, 3]");
		test(nil).asSorted().isNull();
	}

	@Test
	public void bc03b_sorted_wComparator() {
		List<Integer> x = alist(1,3,2), nil = null;
		test(x).asSorted(null).isString("[1, 2, 3]");
		test(nil).asSorted(null).isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() {
		List<Integer> x = alist(), nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() {
		List<Integer> x = alist(), nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() {
		List<Integer> x = alist(), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() {
		List<Integer> x1 = alist(1,2), x1a = alist(1,2), x2 = alist(2,3), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[2, 3]'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[2, 3]'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() {
		List<Integer> x1 = alist(1,2);
		test(x1).is(x->x.size()==2);
		assertThrown(()->test(x1).is(x->x.size()==3)).asMessage().asOneLine().is("Unexpected value: '[1, 2]'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[1, 2]'.");
	}

	@Test
	public void ca05_isNot() {
		List<Integer> x1 = alist(1,2), x1a = alist(1,2), x2 = alist(3,4), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() {
		List<Integer> x1 = alist(1,2), x1a = alist(1,2), x2 = alist(3,4), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[3, 4]]'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[3, 4]]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() {
		List<Integer> x1 = alist(1,2), x1a = alist(1,2), x2 = alist(3,4), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() {
		List<Integer> x1 = list(1,2), x1a = list(1,2), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1, 2](ArrayList@*)'.  Actual='[1, 2](ArrayList@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1, 2](ArrayList@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='[1, 2](ArrayList@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() {
		List<Integer> x1 = alist(1,2), x1a = alist(1,2), x2 = alist(3,4), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() {
		List<Integer> x1 = alist(1,2), x1a = alist(2,1), x2 = alist(3,4), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca11_isSameSerializedAs() {
		List<Integer> x1 = alist(1,2), x1a = alist(1,2), x2 = alist(3,4), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca12_isType() {
		List<Integer> x = list(1,2), nil = null;
		test(x).isType(List.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() {
		List<Integer> x = list(1,2), nil = null;
		test(x).isExactType(ArrayList.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() {
		List<Integer> x = alist(1,2), nil = null;
		test(x).isString("[1, 2]");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1, 2]'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() {
		List<Integer> x = alist(1,2), nil = null;
		test(x).isJson("[1,2]");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1,2]'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() {
		List<String> x1 = alist(), x2 = alist("a","b"), nil = null;
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).asMessage().is("Collection was not empty.");
		assertThrown(()->test(nil).isEmpty()).asMessage().is("Value was null.");
	}

	@Test
	public void cb02_isNotEmpty() {
		List<String> x1 = alist(), x2 = alist("a","b"), nil = null;
		test(x2).isNotEmpty();
		assertThrown(()->test(x1).isNotEmpty()).asMessage().is("Collection was empty.");
		assertThrown(()->test(nil).isNotEmpty()).asMessage().is("Value was null.");
	}

	@Test
	public void cb03_contains() {
		List<String> x = alist("a","b"), nil = null;
		test(x).isContains("a");
		assertThrown(()->test(x).isContains("z")).asMessage().asOneLine().is("Collection did not contain expected value.  Expect='z'.  Value='[a, b]'.");
		assertThrown(()->test(nil).isContains("z")).asMessage().is("Value was null.");
	}

	@Test
	public void cb04_doesNotContain() {
		List<String> x = alist("a","b"), nil = null;
		test(x).isNotContains("z");
		assertThrown(()->test(x).isNotContains("a")).asMessage().asOneLine().is("Collection contained unexpected value.  Unexpected='a'.  Value='[a, b]'.");
		assertThrown(()->test(nil).isNotContains("z")).asMessage().is("Value was null.");
	}

	@Test
	public void cb05_any() {
		List<String> x1 = alist("a","b"), nil = null;
		test(x1).isAny(x->x.equals("a"));
		assertThrown(()->test(x1).isAny(x->x.equals("z"))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[a, b]'.");
		assertThrown(()->test(nil).isAny(x->x.equals("z"))).asMessage().is("Value was null.");
	}

	@Test
	public void cb06_all() {
		List<String> x1 = alist("a","b"), nil = null;
		test(x1).isAll(x->x!=null);
		assertThrown(()->test(x1).isAll(x->x.equals("z"))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[a, b]'.");
		assertThrown(()->test(nil).isAll(x->x.equals("z"))).asMessage().is("Value was null.");
	}

	@Test
	public void cb07_isSize() {
		List<String> x = alist("a","b"), nil = null;
		test(x).isSize(2);
		assertThrown(()->test(x).isSize(0)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=0.  Actual=2.");
		assertThrown(()->test(nil).isSize(0)).asMessage().is("Value was null.");
	}

	@Test
	public void cc01_has() {
		List<String> x = alist("a","b"), nil = null;
		test(x).isHas("a","b");
		assertThrown(()->test(x).isHas("a")).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x).isHas("a","c")).asMessage().asOneLine().is("List did not contain expected value at index 1.  Value did not match expected.  Expect='c'.  Actual='b'.");
		assertThrown(()->test(nil).isHas("a","c")).asMessage().is("Value was null.");
	}

	@Test
	public void cc02_each() {
		List<String> x1 = alist("a","b"), nil = null;
		test(x1).isEach(x->x!=null,x->x!=null);
		assertThrown(()->test(x1).isEach(x->x==null)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x1).isEach(x->x==null,x->x==null)).asMessage().asOneLine().is("List did not contain expected value at index 0.  Unexpected value: 'a'.");
		assertThrown(()->test(nil).isEach(x->x==null)).asMessage().is("Value was null.");
	}
}