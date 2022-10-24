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

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanListAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private BeanListAssertion<A> test(List<A> value) {
		return assertBeanList(value).setSilent();
	}
	private static A A1 = A.of(1,2), A1a = A.of(1,2), A2 = A.of(3,4), A3 = A.of(5,6);

	public static class A implements Comparable<A> {
		public Integer a, b;
		public A() {}
		public A(Integer a, Integer b) { this.a = a; this.b = b; }
		public static A of(Integer a, Integer b) { return new A(a, b); }
		@Override public String toString() { return "(a="+a+",b="+b+")"; }
		@Override public boolean equals(Object o) { return ObjectUtils.eq(this, (A)o, (x,y)->ObjectUtils.eq(x.a,y.a) && ObjectUtils.eq(x.b,y.b)); }
		@Override public int compareTo(A o) { return a-o.a; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() throws Exception {
		assertThrown(()->test(null).setMsg("Foo {0}", 1).isExists()).asMessage().is("Foo 1");
		assertThrown(()->test(null).setMsg("Foo {0}", 1).setThrowable(RuntimeException.class).isExists()).isExactType(RuntimeException.class).asMessage().is("Foo 1");
	}

	@Test
	public void a02_stdout() throws Exception {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() throws Exception {
		List<A> x = alist(A1), nil = null;
		test(x).asString().is("[(a=1,b=2)]");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		List<A> x = alist(A1), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("[{a:1,b:2}]");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		List<A> x1 = alist(A1);
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		List<A> x = alist(A1), nil = null;
		test(x).asJson().is("[{a:1,b:2}]");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		List<A> x1 = alist(A2,A1), nil = null;
		test(x1).asJsonSorted().is("[{a:1,b:2},{a:3,b:4}]");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		List<A> x1 = alist(A1), x2 = alist(A2);
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_asStrings() throws Exception {
		List<A> x1 = alist(A1), nil = null;
		test(x1).asStrings().asJoin().is("(a=1,b=2)");
		test(nil).asStrings().isNull();
	}

	@Test
	public void bb02_size() {
		List<A> x1 = alist(A1), nil = null;
		test(x1).asSize().is(1);
		test(nil).asSize().isNull();
	}

	@Test
	public void bc01_apply2() throws Exception {
		List<A> x1 = alist(A1), x2 = alist(A2);
		test(x1).asApplied2(x -> x2).is(x2);
	}

	@Test
	public void bc02_item() throws Exception {
		List<A> x = alist(A1), nil = null;
		test(x).asItem(0).isNotNull();
		test(x).asItem(1).isNull();
		test(x).asItem(-1).isNull();
		test(nil).asItem(0).isNull();
	}

	@Test
	public void bc03a_sorted() throws Exception {
		List<A> x = alist(A2,A1), nil = null;
		test(x).asSorted().isString("[(a=1,b=2), (a=3,b=4)]");
		test(nil).asSorted().isNull();
	}

	@Test
	public void bc03b_sorted_wComparator() throws Exception {
		List<A> x = alist(A2,A1), nil = null;
		test(x).asSorted(null).isString("[(a=1,b=2), (a=3,b=4)]");
		test(nil).asSorted(null).isNull();
	}

	@Test
	public void bd01_extract() throws Exception {
		List<A> x = alist(A1,A2);
		test(x)
			.asPropertyMaps("a").asJson().is("[{a:1},{a:3}]")
			.asPropertyMaps("a,b").asJson().is("[{a:1,b:2},{a:3,b:4}]")
			.asPropertyMaps("a","b").asJson().is("[{a:1,b:2},{a:3,b:4}]")
			.asPropertyMaps("bad").asJson().is("[{},{}]")
			.asPropertyMaps((String)null).asJson().is("[{},{}]");
	}

	@Test
	public void bd02_property() throws Exception {
		List<A> x = alist(A1,A2);
		test(x)
			.asProperty("a").asJson().is("[1,3]")
			.asProperty("bad").asJson().is("[null,null]")
			.asProperty(null).asJson().is("[null,null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		List<A> x = alist(), nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		List<A> x = alist(), nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		List<A> x = alist(), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[(a=1,b=2), (a=5,b=6)]'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[(a=1,b=2), (a=5,b=6)]'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		List<A> x1 = alist(A1,A2);
		test(x1).is(x->x.size()==2);
		assertThrown(()->test(x1).is(x->x.size()==3)).asMessage().asOneLine().is("Unexpected value: '[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[(a=1,b=2), (a=3,b=4)]'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[(a=1,b=2), (a=3,b=4)]'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ca06_isAny() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[(a=1,b=2), (a=5,b=6)]]'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[(a=1,b=2), (a=5,b=6)]]'.  Actual='null'.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ca07_isNotAny() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='[(a=1,b=2), (a=3,b=4)]'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		List<A> x1 = list(A1,A2), x1a = list(A1a,A2), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[(a=1,b=2), (a=3,b=4)](ArrayList@*)'.  Actual='[(a=1,b=2), (a=3,b=4)](ArrayList@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[(a=1,b=2), (a=3,b=4)](ArrayList@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='[(a=1,b=2), (a=3,b=4)](ArrayList@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[{a:1,b:2},{a:5,b:6}]'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[{a:1,b:2},{a:5,b:6}]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[{a:1,b:2},{a:5,b:6}]'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[{a:1,b:2},{a:5,b:6}]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		List<A> x1 = alist(A1,A2), x1a = alist(A1a,A2), x2 = alist(A1,A3), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[{a:1,b:2},{a:5,b:6}]'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[{a:1,b:2},{a:5,b:6}]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		List<A> x = list(A1,A2), nil = null;
		test(x).isType(List.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		List<A> x = list(A1,A2), nil = null;
		test(x).isExactType(ArrayList.class);
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.ArrayList'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		List<A> x = alist(A1,A2), nil = null;
		test(x).isString("[(a=1,b=2), (a=3,b=4)]");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[(a=1,b=2), (a=3,b=4)]'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		List<A> x = alist(A1,A2), nil = null;
		test(x).isJson("[{a:1,b:2},{a:3,b:4}]");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[{a:1,b:2},{a:3,b:4}]'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() throws Exception {
		List<A> x1 = alist(), x2 = alist(A1), nil = null;
		test(x1).isEmpty();
		assertThrown(()->test(nil).isEmpty()).asMessage().is("Value was null.");
		assertThrown(()->test(x2).isEmpty()).asMessage().is("Collection was not empty.");
	}

	@Test
	public void cb02_isNotEmpty() throws Exception {
		List<A> x1 = alist(), x2 = alist(A1), nil = null;
		test(x2).isNotEmpty();
		assertThrown(()->test(nil).isNotEmpty()).asMessage().is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).asMessage().is("Collection was empty.");
	}

	@Test
	public void cb03_contains() throws Exception {
		List<A> x = alist(A1), nil = null;
		test(x).isContains(A1);
		assertThrown(()->test(x).isContains(A2)).asMessage().asOneLine().is("Collection did not contain expected value.  Expect='(a=3,b=4)'.  Value='[(a=1,b=2)]'.");
		assertThrown(()->test(nil).isContains(A2)).asMessage().is("Value was null.");
	}

	@Test
	public void cb04_doesNotContain() throws Exception {
		List<A> x = alist(A1), nil = null;
		test(x).isNotContains(A2);
		assertThrown(()->test(x).isNotContains(A1)).asMessage().asOneLine().is("Collection contained unexpected value.  Unexpected='(a=1,b=2)'.  Value='[(a=1,b=2)]'.");
		assertThrown(()->test(nil).isNotContains(A2)).asMessage().is("Value was null.");
	}

	@Test
	public void cb05_any() throws Exception {
		List<A> x1 = alist(A1), nil = null;
		test(x1).isAny(x->x.equals(A1));
		assertThrown(()->test(x1).isAny(x->x.equals(A2))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[(a=1,b=2)]'.");
		assertThrown(()->test(nil).isAny(x->x.equals(A2))).asMessage().is("Value was null.");
	}

	@Test
	public void cb06_all() throws Exception {
		List<A> x1 = alist(A1), nil = null;
		test(x1).isAll(x->x!=null);
		assertThrown(()->test(x1).isAll(x->x.equals(A2))).asMessage().asOneLine().is("Collection did not contain tested value.  Value='[(a=1,b=2)]'.");
		assertThrown(()->test(nil).isAll(x->x.equals(A2))).asMessage().is("Value was null.");
	}

	@Test
	public void cb07_isSize() throws Exception {
		List<A> x = alist(A1), nil = null;
		test(x).isSize(1);
		assertThrown(()->test(x).isSize(0)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=0.  Actual=1.");
		assertThrown(()->test(nil).isSize(0)).asMessage().is("Value was null.");
	}

	@Test
	public void cc01_has() throws Exception {
		List<A> x = alist(A1,A2), nil = null;
		test(x).isHas(A1,A2);
		assertThrown(()->test(x).isHas(A1)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x).isHas(A1,A3)).asMessage().asOneLine().is("List did not contain expected value at index 1.  Value did not match expected.  Expect='(a=5,b=6)'.  Actual='(a=3,b=4)'.");
		assertThrown(()->test(nil).isHas(A1,A3)).asMessage().is("Value was null.");
	}

	@Test
	public void cc02_each() throws Exception {
		List<A> x1 = alist(A1,A2), nil = null;
		test(x1).isEach(x->x!=null,x->x!=null);
		assertThrown(()->test(x1).isEach(x->x==null)).asMessage().asOneLine().is("Collection did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(x1).isEach(x->x==null,x->x==null)).asMessage().asOneLine().is("List did not contain expected value at index 0.  Unexpected value: '(a=1,b=2)'.");
		assertThrown(()->test(nil).isEach(x->x==null)).asMessage().is("Value was null.");
	}
}
