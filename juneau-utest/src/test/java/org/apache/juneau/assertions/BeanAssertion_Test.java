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

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
class BeanAssertion_Test extends SimpleTestBase {

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
		@Override public boolean equals(Object o) { return Utils.eq(this, (A)o, (x,y)->Utils.eq(x.a,y.a) && Utils.eq(x.b,y.b)); }
		@Override public int compareTo(A o) { return a-o.a; }
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
		var x = A1;
		var nil = (A)null;
		test(x).asString().is("a=1,b=2");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = A1;
		var nil = (A)null;
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("{a:1,b:2}");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = A1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = A1;
		var nil = (A)null;
		test(x).asJson().is("{a:1,b:2}");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x1 = a(A2,A1);
		var nil = (A)null;
		test(x1).asJsonSorted().is("[{a:1,b:2},{a:2,b:3}]");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = A1;
		var x2 = A2;
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_extract() {
		var x = A1;
		var nil = (A)null;
		test(x).asPropertyMap("b,a").asJson().is("{b:2,a:1}");
		test(x).asPropertyMap("x").asJson().is("{}");
		assertThrows(BasicAssertionError.class, ()->test(nil).asPropertyMap("x"), "Value was null.");
	}

	@Test void bb02_property() {
		var x = A1;
		var nil = (A)null;
		test(x).asProperty("a").asInteger().is(1);
		test(x).asProperty("x").asInteger().isNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).asProperty("x"), "Value was null.");
	}

	@Test void bb03_properties() {
		var x = A1;
		var nil = (A)null;
		test(x).asProperties("a").asJson().is("[1]");
		test(x).asProperties("x").asJson().is("[null]");
		assertThrows(BasicAssertionError.class, ()->test(nil).asProperties("x"), "Value was null.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = A1;
		var nil = (A)null;
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = A1;
		var nil = (A)null;
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = A1;
		var nil = (A)null;
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='a=2,b=3'.  Actual='a=1,b=2'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='a=2,b=3'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = A1;
		test(x1).is(x->x.a==1);
		assertThrown(()->test(x1).is(x->x.a==2)).asMessage().asOneLine().is("Unexpected value: 'a=1,b=2'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='a=1,b=2'.");
	}

	@Test void ca05_isNot() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='a=1,b=2'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[a=2,b=3]'.  Actual='a=1,b=2'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[a=2,b=3]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='a=1,b=2'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = A1;
		var x1a = A1a;
		var nil = (A)null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='a=1,b=2(BeanAssertion_Test$A@*)'.  Actual='a=1,b=2(BeanAssertion_Test$A@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='a=1,b=2(BeanAssertion_Test$A@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='a=1,b=2(BeanAssertion_Test$A@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{a:1,b:2}'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{a:1,b:2}'.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = A1;
		var x1a = A1a;
		var x2 = A2;
		var nil = (A)null;
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{a:2,b:3}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{a:1,b:2}'.");
	}

	@Test void ca12_isType() {
		var x = A1;
		var nil = (A)null;
		test(x).isType(A.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.assertions.BeanAssertion_Test$A'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = A1;
		var nil = (A)null;
		test(x).isExactType(A.class);
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.assertions.BeanAssertion_Test$A'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = A1;
		var nil = (A)null;
		test(x).isString("a=1,b=2");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='a=1,b=2'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='a=1,b=2'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = A1;
		var nil = (A)null;
		test(x).isJson("{a:1,b:2}");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{a:1,b:2}'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}
}