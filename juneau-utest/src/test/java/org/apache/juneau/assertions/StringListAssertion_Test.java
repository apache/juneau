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

import static org.apache.juneau.assertions.AssertionPredicates.ne;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
class StringListAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private StringListAssertion test(List<String> value) {
		return assertStringList(value).setSilent();
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
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).asString().is("[1]");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = alist("1");
		var nil = nlist(String.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("['1']");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = alist("1");
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).asJson().is("['1']");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x1 = alist("2","1");
		var nil = nlist(String.class);
		test(x1).asJsonSorted().is("['1','2']");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = alist("1");
		var x2 = alist("2");
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_asStrings() {
		var x1 = alist("1");
		var nil = nlist(String.class);
		test(x1).asStrings().asJoin().is("1");
		test(nil).asStrings().isNull();
	}

	@Test void bb02_size() {
		var x1 = alist("1");
		var nil = nlist(String.class);
		test(x1).asSize().is(1);
		test(nil).asSize().isNull();
	}

	@Test void bc01_apply2() {
		var x1 = alist("1");
		var x2 = alist("2");
		test(x1).asApplied2(x -> x2).is(x2);
	}

	@Test void bc02_item() {
		var x = alist("a");
		var nil = nlist(String.class);
		test(x).asItem(0).isNotNull();
		test(x).asItem(1).isNull();
		test(x).asItem(-1).isNull();
		test(nil).asItem(0).isNull();
	}

	@Test void bc03a_sorted() {
		var x = alist("2","1");
		var nil = nlist(String.class);
		test(x).asSorted().isString("[1, 2]");
		test(nil).asSorted().isNull();
	}

	@Test void bc03b_sorted_wComparator() {
		var x = alist("2","1");
		var nil = nlist(String.class);
		test(x).asSorted(null).isString("[1, 2]");
		test(nil).asSorted(null).isNull();
	}

	@Test void bd01a_join() {
		var x = alist("1","2");
		var nil = nlist(String.class);
		test(x).asJoin().isString("12");
		test(nil).asJoin().isNull();
	}

	@Test void bd01b_join_wDelim() {
		var x = alist("1","2");
		var nil = nlist(String.class);
		test(x).asJoin(",").isString("1,2");
		test(nil).asJoin(",").isNull();
	}

	@Test void bd01c_join_wDelim_wXfix() {
		var x = alist("1","2");
		var nil = nlist(String.class);
		test(x).asJoin(",","[","]").isString("[1,2]");
		test(nil).asJoin(",","[","]").isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = elist(String.class);
		var nil = nlist(String.class);
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = elist(String.class);
		var nil = nlist(String.class);
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = elist(String.class);
		var nil = nlist(String.class);
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = alist("1");
		var x1a = alist(new String("1"));
		var x2 = alist("2");
		var nil = nlist(String.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[2]'.  Actual='[1]'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[1]'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[2]'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = alist("1");
		test(x1).is(x->x.size()==1);
		assertThrown(()->test(x1).is(x->x.size()==2)).asMessage().asOneLine().is("Unexpected value: '[1]'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[1]'.");
	}

	@Test void ca05_isNot() {
		var x1 = alist("1");
		var x1a = alist(new String("1"));
		var x2 = alist("2");
		var nil = nlist(String.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[1]'.  Actual='[1]'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = alist("1");
		var x1a = alist(new String("1"));
		var x2 = alist("2");
		var nil = nlist(String.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[2]]'.  Actual='[1]'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[1]'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[2]]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = alist("1");
		var x1a = alist(new String("1"));
		var x2 = alist("2");
		var nil = nlist(String.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='[1]'.  Actual='[1]'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = list("1");
		var x1a = list("1");
		var nil = nlist(String.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1](ArrayList@*)'.  Actual='[1](ArrayList@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1](ArrayList@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='[1](ArrayList@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = alist("1");
		var x1a = alist("1");
		var x2 = alist("2");
		var nil = nlist(String.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='['1']'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='['1']'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = alist("1");
		var x1a = alist("1");
		var x2 = alist("2");
		var nil = nlist(String.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='['1']'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='['1']'.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = alist("1");
		var x1a = alist("1");
		var x2 = alist("2");
		var nil = nlist(String.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='['1']'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='['2']'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='['1']'.");
	}

	@Test void ca12_isType() {
		var x = list("1");
		var nil = nlist(String.class);
		test(x).isType(List.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = list("1");
		var nil = nlist(String.class);
		test(x).isExactType(ArrayList.class);
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).isString("[1]");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1]'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1]'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).isJson("['1']");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='['1']'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='['1']'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_isEmpty() {
		var x1 = elist(String.class);
		var x2 = alist("1");
		var nil = nlist(String.class);
		test(x1).isEmpty();
		assertThrows(BasicAssertionError.class, ()->test(nil).isEmpty(), "Value was null.");
		assertThrows(BasicAssertionError.class, ()->test(x2).isEmpty(), "Collection was not empty.");
	}

	@Test void cb02_isNotEmpty() {
		var x1 = elist(String.class);
		var x2 = alist("1");
		var nil = nlist(String.class);
		test(x2).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotEmpty(), "Value was null.");
		assertThrows(BasicAssertionError.class, ()->test(x1).isNotEmpty(), "Collection was empty.");
	}

	@Test void cb03_contains() {
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).isContains("1");
		assertThrown(()->test(x).isContains("2")).asMessage().asOneLine().is("Collection did not contain expected value.  Expect='2'.  Value='[1]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContains("2"), "Value was null.");
	}

	@Test void cb04_doesNotContain() {
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).isNotContains("2");
		assertThrown(()->test(x).isNotContains("1")).asMessage().asOneLine().is("Collection contained unexpected value.  Unexpected='1'.  Value='[1]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotContains("2"), "Value was null.");
	}

	@Test void cb05_any() {
		var x1 = alist("1");
		var nil = nlist(String.class);
		test(x1).isAny(x->x.equals("1"));
		assertThrown(()->test(x1).isAny(x->x.equals("2"))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[1]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAny(x->x.equals("2")), "Value was null.");
	}

	@Test void cb06_all() {
		var x1 = alist("1");
		var nil = nlist(String.class);
		test(x1).isAll(x->x!=null);
		assertThrown(()->test(x1).isAll(x->x.equals("2"))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[1]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAll(x->x.equals("2")), "Value was null.");
	}

	@Test void cb07_isSize() {
		var x = alist("1");
		var nil = nlist(String.class);
		test(x).isSize(1);
		assertThrown(()->test(x).isSize(0)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=0.  Actual=1.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isSize(0), "Value was null.");
	}

	@Test void cc01_has() {
		var x = alist("1","2");
		var nil = nlist(String.class);
		test(x).isHas("1","2");
		assertThrown(()->test(x).isHas("1")).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x).isHas("1","3")).asMessage().asOneLine().is("List did not contain expected value at index 1.  Value did not match expected.  Expect='3'.  Actual='2'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isHas("1","3"), "Value was null.");
	}

	@Test void cc02_each() {
		var x1 = alist("1","2");
		var nil = nlist(String.class);
		test(x1).isEach(x->x!=null,x->x!=null);
		assertThrown(()->test(x1).isEach(x->x==null)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x1).isEach(x->x==null,x->x==null)).asMessage().asOneLine().is("List did not contain expected value at index 0.  Unexpected value: '1'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isEach(x->x==null), "Value was null.");
	}
}