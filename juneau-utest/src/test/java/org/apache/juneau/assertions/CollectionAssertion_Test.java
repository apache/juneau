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
import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@Deprecated
public class CollectionAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <T> CollectionAssertion<T> test(Collection<T> value) {
		return assertCollection(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() {
		assertThrows(BasicAssertionError.class, ()->test(null).setMsg("A {0}", 1).isExists(), "A 1");
		assertThrows(RuntimeException.class, ()->test(null).setMsg("A {0}", 1).setThrowable(RuntimeException.class).isExists(), "A 1");
	}

	@Test
	public void a02_stdout() {
		test(null).setStdOut().setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() {
		List<Integer> x = ulist(1), nil = null;
		test(x).asString().is("[1]");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() {
		List<Integer> x = ulist(1), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("[1]");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() {
		List<Integer> x1 = ulist(1);
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() {
		List<Integer> x = ulist(1), nil = null;
		test(x).asJson().is("[1]");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() {
		List<Integer> x1 = ulist(2,1), nil = null;
		test(x1).asJsonSorted().is("[1,2]");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() {
		List<Integer> x1 = ulist(1), x2 = ulist(2);
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_asStrings() {
		List<Integer> x1 = ulist(1), nil = null;
		test(x1).asStrings().asJoin().is("1");
		test(nil).asStrings().isNull();
	}

	@Test
	public void bb02_size() {
		List<Integer> x1 = ulist(1), nil = null;
		test(x1).asSize().is(1);
		test(nil).asSize().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() {
		List<Integer> x = ulist(), nil = null;
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test
	public void ca02_isNull() {
		List<Integer> x = ulist(), nil = null;
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test
	public void ca03_isNotNull() {
		List<Integer> x = ulist(), nil = null;
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test
	public void ca04a_is_T() {
		List<Integer> x1 = ulist(1,2), x1a = ulist(1,2), x2 = ulist(3,4), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[3, 4]'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[3, 4]'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() {
		List<Integer> x1 = ulist(1,2);
		test(x1).is(x->x.size()==2);
		assertThrown(()->test(x1).is(x->x.size()==3)).asMessage().asOneLine().is("Unexpected value: '[1, 2]'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[1, 2]'.");
	}

	@Test
	public void ca05_isNot() {
		List<Integer> x1 = ulist(1,2), x1a = ulist(1,2), x2 = ulist(3,4), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() {
		List<Integer> x1 = ulist(1,2), x1a = ulist(1,2), x2 = ulist(3,4), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[3, 4]]'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[3, 4]]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() {
		List<Integer> x1 = ulist(1,2), x1a = ulist(1,2), x2 = ulist(3,4), nil = null;
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
		List<Integer> x1 = ulist(1,2), x1a = ulist(1,2), x2 = ulist(3,4), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() {
		List<Integer> x1 = ulist(1,2), x1a = ulist(2,1), x2 = ulist(3,4), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[3,4]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca11_isSameSerializedAs() {
		List<Integer> x1 = ulist(1,2), x1a = ulist(1,2), x2 = ulist(3,4), nil = null;
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
		assertThrown(()->test(nil).isType(Integer.class)).asMessage().asOneLine().is("Value was null.");
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
		List<Integer> x = ulist(1,2), nil = null;
		test(x).isString("[1, 2]");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1, 2]'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() {
		List<Integer> x = ulist(1,2), nil = null;
		test(x).isJson("[1,2]");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1,2]'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() {
		List<String> x1 = ulist(), x2 = ulist("a","b"), nil = null;
		test(x1).isEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x2).isEmpty(), "Collection was not empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isEmpty(), "Value was null.");
	}

	@Test
	public void cb02_isNotEmpty() {
		List<String> x1 = ulist(), x2 = ulist("a","b"), nil = null;
		test(x2).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x1).isNotEmpty(), "Collection was empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotEmpty(), "Value was null.");
	}

	@Test
	public void cb03_contains() {
		List<String> x = ulist("a","b"), nil = null;
		test(x).isContains("a");
		assertThrown(()->test(x).isContains("z")).asMessage().asOneLine().is("Collection did not contain expected value.  Expect='z'.  Value='[a, b]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContains("z"), "Value was null.");
	}

	@Test
	public void cb04_doesNotContain() {
		List<String> x = ulist("a","b"), nil = null;
		test(x).isNotContains("z");
		assertThrown(()->test(x).isNotContains("a")).asMessage().asOneLine().is("Collection contained unexpected value.  Unexpected='a'.  Value='[a, b]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotContains("z"), "Value was null.");
	}

	@Test
	public void cb05_any() {
		List<String> x1 = ulist("a","b"), nil = null;
		test(x1).isAny(x->x.equals("a"));
		test(x1).isAny((Predicate<String>)null);
		assertThrown(()->test(x1).isAny(x->x.equals("z"))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[a, b]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAny(x->x.equals("z")), "Value was null.");
	}

	@Test
	public void cb06_all() {
		List<String> x1 = ulist("a","b"), nil = null;
		test(x1).isAll(x->x!=null);
		test(x1).isAll(null);
		assertThrown(()->test(x1).isAll(x->x.equals("z"))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[a, b]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAll(x->x.equals("z")), "Value was null.");
	}

	@Test
	public void cb07_isSize() {
		List<String> x = ulist("a","b"), nil = null;
		test(x).isSize(2);
		assertThrown(()->test(x).isSize(0)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=0.  Actual=2.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isSize(0), "Value was null.");
	}
}