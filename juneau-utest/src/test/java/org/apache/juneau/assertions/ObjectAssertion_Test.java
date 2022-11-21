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

import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ObjectAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <T> ObjectAssertion<T> test(T value) {
		return assertObject(value).setSilent();
	}

	public static final A1 A1 = new A1();
	public static class A1 {
		public int a = 1, b = 2;
		@Override public String toString() { return "a="+a+",b="+b; }
	}

	public static class A2 {
		public int getFoo() { throw new RuntimeException("foo"); }
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
		Integer x = 1, nil = null;
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Integer x = 1, nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("1");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		Integer x1 = 1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		Integer x = 1, nil = null;
		test(x).asJson().is("1");
		test(nil).asJson().is("null");
		assertThrown(()->test(new A2()).asJson()).asMessages().isAny(contains("Could not call getValue() on property 'foo'"));
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		Integer[] x1 = {2,1}, nil = null;
		Object x2 = A1;
		test(x1).asJsonSorted().is("[1,2]");
		test(x2).asJsonSorted().is("{a:1,b:2}");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		Integer x1 = 1;
		test(x1).asTransformed(x -> x + 1).is(2);
	}

	@Test
	public void ba05_asAny() throws Exception {
		Integer x = 1, nil = null;
		test(x).asAny().asInteger().is(1);
		test(nil).asAny().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Integer x = 1, nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Integer x = 1, nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Integer x = 1, nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='2'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Integer x1 = 1;
		test(x1).is(x->x==1);
		test(x1).is((Predicate<Integer>)null);
		assertThrown(()->test(x1).is(x->x==2)).asMessage().asOneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='1'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@SuppressWarnings("deprecation")
	@Test
	public void ca08_isSame() throws Exception {
		Integer x1 = new Integer(1), x1a = new Integer(1), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(Integer@*)'.  Actual='1(Integer@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(Integer@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='1(Integer@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='1'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='1'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='1'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Integer x = 1, nil = null;
		test(x).isType(Integer.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.lang.Integer'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Integer x = 1, nil = null;
		test(x).isExactType(Integer.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.lang.Integer'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.lang.Integer'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Integer x = 1, nil = null;
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Integer x = 1, nil = null;
		test(x).isJson("1");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}
}
