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

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BeanAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <T> BeanAssertion<T> test(T value) {
		return assertBean(value).setSilent();
	}

	public static A A1 = new A(), A1a = new A(), A2 = new A(2,3);

	public static class A implements Comparable<A> {
		public int a = 1, b = 2;
		public A() {}
		public A(int a, int b) {this.a = a; this.b = b;}
		@Override public String toString() {return "a="+a+",b="+b;}
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
		A x = A1, nil = null;
		test(x).asString().is("a=1,b=2");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		A x = A1, nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("{a:1,b:2}");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		A x1 = A1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		A x = A1, nil = null;
		test(x).asJson().is("{a:1,b:2}");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		A[] x1 = {A2,A1}, nil = null;
		test(x1).asJsonSorted().is("[{a:1,b:2},{a:2,b:3}]");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		A x1 = A1, x2 = A2;
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_extract() throws Exception {
		A x = A1, nil = null;
		test(x).asPropertyMap("b,a").asJson().is("{b:2,a:1}");
		test(x).asPropertyMap("x").asJson().is("{}");
		assertThrown(()->test(nil).asPropertyMap("x")).asMessage().is("Value was null.");
	}

	@Test
	public void bb02_property() throws Exception {
		A x = A1, nil = null;
		test(x).asProperty("a").asInteger().is(1);
		test(x).asProperty("x").asInteger().isNull();
		assertThrown(()->test(nil).asProperty("x")).asMessage().is("Value was null.");
	}

	@Test
	public void bb03_properties() throws Exception {
		A x = A1, nil = null;
		test(x).asProperties("a").asJson().is("[1]");
		test(x).asProperties("x").asJson().is("[null]");
		assertThrown(()->test(nil).asProperties("x")).asMessage().is("Value was null.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		A x = A1, nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		A x = A1, nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		A x = A1, nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='a=2,b=3'.  Actual='a=1,b=2'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='a=2,b=3'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		A x1 = A1;
		test(x1).is(x->x.a==1);
		assertThrown(()->test(x1).is(x->x.a==2)).asMessage().asOneLine().is("Unexpected value: 'a=1,b=2'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='a=1,b=2'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='a=1,b=2'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[a=2,b=3]'.  Actual='a=1,b=2'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[a=2,b=3]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='a=1,b=2'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		A x1 = A1, x1a = A1a, nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='a=1,b=2(BeanAssertion_Test$A@*)'.  Actual='a=1,b=2(BeanAssertion_Test$A@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='a=1,b=2(BeanAssertion_Test$A@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='a=1,b=2(BeanAssertion_Test$A@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{a:1,b:2}'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{a:1,b:2}'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		A x1 = A1, x1a = A1a, x2 = A2, nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{a:1,b:2}'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		A x = A1, nil = null;
		test(x).isType(A.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.assertions.BeanAssertion_Test$A'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		A x = A1, nil = null;
		test(x).isExactType(A.class);
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.assertions.BeanAssertion_Test$A'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		A x = A1, nil = null;
		test(x).isString("a=1,b=2");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='a=1,b=2'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		A x = A1, nil = null;
		test(x).isJson("{a:1,b:2}");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}
}
