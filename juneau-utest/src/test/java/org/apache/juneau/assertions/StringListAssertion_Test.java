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

import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringListAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private StringListAssertion test(List<String> value) {
		return assertStringList(value).silent();
	}

	@SafeVarargs
	private static <T> List<T> list(T...objects) {
		return AList.of(objects);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
	}

	@Test
	public void a02_stdout() throws Exception {
		test(null).stdout();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).asString().is("[1]");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		List<String> x = list("1"), nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x).asString(s).is("['1']");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		List<String> x1 = list("1");
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).asJson().is("['1']");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		List<String> x1 = list("2","1"), nil = null;
		test(x1).asJsonSorted().is("['1','2']");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		List<String> x1 = list("1"), x2 = list("2");
		test(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bb01_asStrings() throws Exception {
		List<String> x1 = list("1"), nil = null;
		test(x1).asStrings().join().is("1");
		test(nil).asStrings().isNull();
	}

	@Test
	public void bb02_size() {
		List<String> x1 = list("1"), nil = null;
		test(x1).size().is(1);
		test(nil).size().isNull();
	}

	@Test
	public void bc01_apply2() throws Exception {
		List<String> x1 = list("1"), x2 = list("2");
		test(x1).apply2(x -> x2).is(x2);
	}

	@Test
	public void bc02_item() throws Exception {
		List<String> x = list("a"), nil = null;
		test(x).item(0).isNotNull();
		test(x).item(1).isNull();
		test(x).item(-1).isNull();
		test(nil).item(0).isNull();
	}

	@Test
	public void bc03a_sorted() throws Exception {
		List<String> x = list("2","1"), nil = null;
		test(x).sorted().isString("[1, 2]");
		test(nil).sorted().isNull();
	}

	@Test
	public void bc03b_sorted_wComparator() throws Exception {
		List<String> x = list("2","1"), nil = null;
		test(x).sorted(null).isString("[1, 2]");
		test(nil).sorted(null).isNull();
	}

	@Test
	public void bd01a_join() throws Exception {
		List<String> x = list("1","2"), nil = null;
		test(x).join().isString("12");
		test(nil).join().isNull();
	}

	@Test
	public void bd01b_join_wDelim() throws Exception {
		List<String> x = list("1","2"), nil = null;
		test(x).join(",").isString("1,2");
		test(nil).join(",").isNull();
	}

	@Test
	public void bd01c_join_wDelim_wXfix() throws Exception {
		List<String> x = list("1","2"), nil = null;
		test(x).join(",","[","]").isString("[1,2]");
		test(nil).join(",","[","]").isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		List<String> x = list(), nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		List<String> x = list(), nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		List<String> x = list(), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		List<String> x1 = list("1"), x1a = list(new String("1")), x2 = list("2"), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("Unexpected value.  Expect='[2]'.  Actual='[1]'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("Unexpected value.  Expect='null'.  Actual='[1]'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("Unexpected value.  Expect='[2]'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		List<String> x1 = list("1");
		test(x1).is(x->x.size()==1);
		assertThrown(()->test(x1).is(x->x.size()==2)).message().oneLine().is("Unexpected value: '[1]'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.  Value='[1]'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		List<String> x1 = list("1"), x1a = list(new String("1")), x2 = list("2"), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("Unexpected value.  Did not expect='[1]'.  Actual='[1]'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ca06_isAny() throws Exception {
		List<String> x1 = list("1"), x1a = list(new String("1")), x2 = list("2"), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.  Expect='[[2]]'.  Actual='[1]'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.  Expect='[]'.  Actual='[1]'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.  Expect='[[2]]'.  Actual='null'.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ca07_isNotAny() throws Exception {
		List<String> x1 = list("1"), x1a = list(new String("1")), x2 = list("2"), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.  Unexpected='[1]'.  Actual='[1]'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		List<String> x1 = list("1"), x1a = list("1"), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.  Expect='[1](AList@*)'.  Actual='[1](AList@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.  Expect='[1](AList@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.  Expect='null(null)'.  Actual='[1](AList@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		List<String> x1 = list("1"), x1a = list("1"), x2 = list("2"), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='['1']'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual='['1']'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		List<String> x1 = list("1"), x1a = list("1"), x2 = list("2"), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='['1']'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual='['1']'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		List<String> x1 = list("1"), x1a = list("1"), x2 = list("2"), nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='['1']'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual='['1']'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).isType(List.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.internal.AList'.");
		assertThrown(()->test(nil).isType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).isExactType(AList.class);
		assertThrown(()->test(x).isExactType(String.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.internal.AList'.");
		assertThrown(()->test(nil).isExactType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).isString("[1]");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1]'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.  Expect='null'.  Actual='[1]'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).isJson("['1']");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='['1']'.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.  Expect='null'.  Actual='['1']'.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() throws Exception {
		List<String> x1 = list(), x2 = list("1"), nil = null;
		test(x1).isEmpty();
		assertThrown(()->test(nil).isEmpty()).message().is("Value was null.");
		assertThrown(()->test(x2).isEmpty()).message().is("Collection was not empty.");
	}

	@Test
	public void cb02_isNotEmpty() throws Exception {
		List<String> x1 = list(), x2 = list("1"), nil = null;
		test(x2).isNotEmpty();
		assertThrown(()->test(nil).isNotEmpty()).message().is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).message().is("Collection was empty.");
	}

	@Test
	public void cb03_contains() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).contains("1");
		assertThrown(()->test(x).contains("2")).message().oneLine().is("Collection did not contain expected value.  Expect='2'.  Value='[1]'.");
		assertThrown(()->test(nil).contains("2")).message().is("Value was null.");
	}

	@Test
	public void cb04_doesNotContain() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).doesNotContain("2");
		assertThrown(()->test(x).doesNotContain("1")).message().oneLine().is("Collection contained unexpected value.  Unexpected='1'.  Value='[1]'.");
		assertThrown(()->test(nil).doesNotContain("2")).message().is("Value was null.");
	}

	@Test
	public void cb05_any() throws Exception {
		List<String> x1 = list("1"), nil = null;
		test(x1).any(x->x.equals("1"));
		assertThrown(()->test(x1).any(x->x.equals("2"))).message().oneLine().is("Collection did not contain tested value.  Value='[1]'.");
		assertThrown(()->test(nil).any(x->x.equals("2"))).message().is("Value was null.");
	}

	@Test
	public void cb06_all() throws Exception {
		List<String> x1 = list("1"), nil = null;
		test(x1).all(x->x!=null);
		assertThrown(()->test(x1).all(x->x.equals("2"))).message().oneLine().is("Collection did not contain tested value.  Value='[1]'.");
		assertThrown(()->test(nil).all(x->x.equals("2"))).message().is("Value was null.");
	}

	@Test
	public void cb07_isSize() throws Exception {
		List<String> x = list("1"), nil = null;
		test(x).isSize(1);
		assertThrown(()->test(x).isSize(0)).message().oneLine().is("Collection did not have the expected size.  Expect=0.  Actual=1.");
		assertThrown(()->test(nil).isSize(0)).message().is("Value was null.");
	}

	@Test
	public void cc01_has() throws Exception {
		List<String> x = list("1","2"), nil = null;
		test(x).has("1","2");
		assertThrown(()->test(x).has("1")).message().oneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x).has("1","3")).message().oneLine().is("List did not contain expected value at index 1.  Value did not match expected.  Expect='3'.  Actual='2'.");
		assertThrown(()->test(nil).has("1","3")).message().is("Value was null.");
	}

	@Test
	public void cc02_each() throws Exception {
		List<String> x1 = list("1","2"), nil = null;
		test(x1).each(x->x!=null,x->x!=null);
		assertThrown(()->test(x1).each(x->x==null)).message().oneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x1).each(x->x==null,x->x==null)).message().oneLine().is("List did not contain expected value at index 0.  Unexpected value: '1'.");
		assertThrown(()->test(nil).each(x->x==null)).message().is("Value was null.");
	}
}
