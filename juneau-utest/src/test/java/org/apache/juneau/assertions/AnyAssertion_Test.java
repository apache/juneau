/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.assertions;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.AssertionPredicates.ne;
import static org.apache.juneau.assertions.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
class AnyAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private static <T> AnyAssertion<T> test(T value) {
		return assertAny(value).setSilent();
	}

	private static ZonedDateTime zdt(String s) {
		return ZonedDateTime.parse(s);
	}

	private static Date date(String s) {
		return new Date(ZonedDateTime.parse(s).toEpochSecond()*1000);
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

	@Test void a01_msg() {
		assertThrowsWithMessage(BasicAssertionError.class, "Foo 1", ()->test(null).setMsg("Foo {0}", 1).isExists());
		assertThrowsWithMessage(RuntimeException.class, "Foo 1", ()->test(null).setMsg("Foo {0}", 1).setThrowable(RuntimeException.class).isExists());
	}

	@Test void a02_stdout() {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ba01a_asString() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = 1;
		var nil = n(Integer.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("1");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = 1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).asJson().is("1");
		test(nil).asJson().is("null");
		assertThrown(()->test(new A2()).asJson()).asMessages().isAny(contains("Could not call getValue() on property 'foo'"));
	}

	@Test void ba03_asJsonSorted() {
		var x1 = a(2,1);
		var nil = na(Integer.class);
		var x2 = new A1();
		test(x1).asJsonSorted().is("[1,2]");
		test(x2).asJsonSorted().is("{a:1,b:2}");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = 1;
		var x2 = 2;
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_asArray() {
		var x1 = a(1,2);
		var nil = na(Integer.class);
		String x2 = "";
		test(x1).asArray(Integer.class).asItem(0).is(1);
		test(nil).asArray(Integer.class).isNull();
		assertThrown(()->test(x2).asArray(Integer.class)).asMessage().asOneLine().is("Object was not type 'java.lang.Integer[]'.  Actual='java.lang.String'.");
		assertThrown(()->test(x2).asArray(null)).asMessage().asOneLine().is("Argument 'elementType' cannot be null.");
	}

	@Test void bb02_asIntArray() {
		int[] x1 = {1}, nil = null;
		var x2 = "";
		test(x1).asIntArray().isString("[1]");
		test(nil).asIntArray().isNull();
		assertThrown(()->test(x2).asIntArray()).asMessage().asOneLine().is("Object was not type 'int[]'.  Actual='java.lang.String'.");
	}

	@Test void bb03_asLongArray() {
		long[] x1 = {1}, nil = null;
		var x2 = "";
		test(x1).asLongArray().isString("[1]");
		test(nil).asLongArray().isNull();
		assertThrown(()->test(x2).asLongArray()).asMessage().asOneLine().is("Object was not type 'long[]'.  Actual='java.lang.String'.");
	}

	@Test void bb04_asShortArray() {
		short[] x1 = {1}, nil = null;
		var x2 = "";
		test(x1).asShortArray().isString("[1]");
		test(nil).asShortArray().isNull();
		assertThrown(()->test(x2).asShortArray()).asMessage().asOneLine().is("Object was not type 'short[]'.  Actual='java.lang.String'.");
	}

	@Test void bb05_asFloatArray() {
		float[] x1 = {1}, nil = null;
		var x2 = "";
		test(x1).asFloatArray().isString("[1.0]");
		test(nil).asFloatArray().isNull();
		assertThrown(()->test(x2).asFloatArray()).asMessage().asOneLine().is("Object was not type 'float[]'.  Actual='java.lang.String'.");
	}

	@Test void bb06_asDoubleArray() {
		double[] x1 = {1}, nil = null;
		var x2 = "";
		test(x1).asDoubleArray().isString("[1.0]");
		test(nil).asDoubleArray().isNull();
		assertThrown(()->test(x2).asDoubleArray()).asMessage().asOneLine().is("Object was not type 'double[]'.  Actual='java.lang.String'.");
	}

	@Test void bb07_asCharArray() {
		char[] x1 = {'a'}, nil = null;
		var x2 = "";
		test(x1).asCharArray().isString("[a]");
		test(nil).asCharArray().isNull();
		assertThrown(()->test(x2).asCharArray()).asMessage().asOneLine().is("Object was not type 'char[]'.  Actual='java.lang.String'.");
	}

	@Test void bb08_asByteArray() {
		byte[] x1 = {1}, nil = null;
		var x2 = "";
		test(x1).asByteArray().isString("[1]");
		test(nil).asByteArray().isNull();
		assertThrown(()->test(x2).asByteArray()).asMessage().asOneLine().is("Object was not type 'byte[]'.  Actual='java.lang.String'.");
	}

	@Test void bb09_asBooleanArray() {
		boolean[] x1 = {true}, nil = null;
		var x2 = "";
		test(x1).asBooleanArray().isString("[true]");
		test(nil).asBooleanArray().isNull();
		assertThrown(()->test(x2).asBooleanArray()).asMessage().asOneLine().is("Object was not type 'boolean[]'.  Actual='java.lang.String'.");
	}

	@Test void bb10_asBoolean() {
		var x1 = true;
		var nil = n(Boolean.class);
		var x2 = "";
		test(x1).asBoolean().isString("true");
		test(nil).asBoolean().isNull();
		assertThrown(()->test(x2).asBoolean()).asMessage().asOneLine().is("Object was not type 'java.lang.Boolean'.  Actual='java.lang.String'.");
	}

	@Test void bb11_asBytes() {
		byte[] x1 = {'a'}, nil = null;
		var x2 = "";
		test(x1).asBytes().isString("a");
		test(nil).asBytes().isNull();
		assertThrown(()->test(x2).asBytes()).asMessage().asOneLine().is("Object was not type 'byte[]'.  Actual='java.lang.String'.");
	}

	@Test void bb12_asCollection() {
		var x1 = alist(1);
		var nil = nlist(Integer.class);
		var x2 = "";
		test(x1).asCollection().isString("[1]");
		test(nil).asCollection().isNull();
		assertThrown(()->test(x2).asCollection()).asMessage().asOneLine().is("Object was not type 'java.util.Collection'.  Actual='java.lang.String'.");
	}

	@Test void bb13_asCollection_wType() {
		var x1 = alist(1);
		var nil = nlist(Integer.class);
		var x2 = "";
		test(x1).asCollection(Integer.class).isString("[1]");
		test(nil).asCollection(Integer.class).isNull();
		assertThrown(()->test(x2).asCollection(Integer.class)).asMessage().asOneLine().is("Object was not type 'java.util.Collection'.  Actual='java.lang.String'.");
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'elementType' cannot be null.", ()->test(x1).asCollection(null));
	}

	@Test void bb14_asComparable() {
		var x1 = 1;
		var nil = n(Integer.class);
		var x2 = list();
		test(x1).asComparable().isString("1");
		test(nil).asComparable().isNull();
		assertThrown(()->test(x2).asComparable()).asMessage().asOneLine().is("Object was not type 'java.lang.Comparable'.  Actual='java.util.ArrayList'.");
	}

	@Test void bb15_asDate() {
		var x1 = date("2000-06-01T12:34:56Z");
		var nil = n(Date.class);
		var x2 = "";
		test(x1).asDate().asString().isMatches("*2000");
		test(nil).asDate().isNull();
		assertThrown(()->test(x2).asDate()).asMessage().asOneLine().is("Object was not type 'java.util.Date'.  Actual='java.lang.String'.");
	}

	@Test void bb16_asInteger() {
		var x1 = 1;
		var nil = n(Integer.class);
		var x2 = "";
		test(x1).asInteger().isString("1");
		test(nil).asInteger().isNull();
		assertThrown(()->test(x2).asInteger()).asMessage().asOneLine().is("Object was not type 'java.lang.Integer'.  Actual='java.lang.String'.");
	}

	@Test void bb17_asLong() {
		var x1 = 1L;
		var nil = n(Long.class);
		var x2 = "";
		test(x1).asLong().isString("1");
		test(nil).asLong().isNull();
		assertThrown(()->test(x2).asLong()).asMessage().asOneLine().is("Object was not type 'java.lang.Long'.  Actual='java.lang.String'.");
	}

	@Test void bb18_asList() {
		var x1 = alist(1);
		var nil = nlist(Integer.class);
		var x2 = "";
		test(x1).asList().isString("[1]");
		test(nil).asList().isNull();
		assertThrown(()->test(x2).asList()).asMessage().asOneLine().is("Object was not type 'java.util.List'.  Actual='java.lang.String'.");
	}

	@Test void bb19_asList_wType() {
		var x1 = alist(1);
		var nil = nlist(Integer.class);
		var x2 = "";
		test(x1).asList(Integer.class).isString("[1]");
		test(nil).asList(Integer.class).isNull();
		assertThrown(()->test(x2).asList(Integer.class)).asMessage().asOneLine().is("Object was not type 'java.util.List'.  Actual='java.lang.String'.");
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'elementType' cannot be null.", ()->test(x1).asCollection(null));
	}

	@Test void bb20_asMap() {
		var x1 = map("a",2);
		var nil = (Map<String,Integer>)null;
		var x2 = "";
		test(x1).asMap().isString("{a=2}");
		test(nil).asMap().isNull();
		assertThrown(()->test(x2).asMap()).asMessage().asOneLine().is("Object was not type 'java.util.Map'.  Actual='java.lang.String'.");
	}

	@Test void bb21_asMap_wTypes() {
		var x1 = map("a",2);
		var nil = (Map<String,Integer>)null;
		var x2 = "";
		test(x1).asMap(String.class,Integer.class).isString("{a=2}");
		test(nil).asMap(String.class,Integer.class).isNull();
		assertThrown(()->test(x2).asMap(String.class,Integer.class)).asMessage().asOneLine().is("Object was not type 'java.util.Map'.  Actual='java.lang.String'.");
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'keyType' cannot be null.", ()->test(x1).asMap(null,Integer.class));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'valueType' cannot be null.", ()->test(x1).asMap(String.class,null));
	}

	@Test void bb22_asBean() {
		var x1 = A1;
		var nil = (A1)null;
		test(x1).asBean().isString("a=1,b=2");
		test(nil).asBean().isNull();
	}

	@Test void bb23_asBean_wType() {
		var x1 = A1;
		var nil = n(A1.class);
		var x2 = "";
		test(x1).asBean().isString("a=1,b=2");
		test(nil).asBean(A1.class).isNull();
		assertThrown(()->test(x2).asBean(A2.class)).asMessage().asOneLine().is("Object was not type 'org.apache.juneau.assertions.AnyAssertion_Test$A2'.  Actual='java.lang.String'.");
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'beanType' cannot be null.", ()->test(x1).asBean(null));
	}

	@Test void bb24_asBeanList() {
		var x1 = alist(A1);
		var nil = nlist(A1.class);
		var x2 = "";
		test(x1).asBeanList(A1.class).isString("[a=1,b=2]");
		test(nil).asBeanList(A1.class).isNull();
		assertThrown(()->test(x2).asBeanList(A2.class)).asMessage().asOneLine().is("Object was not type 'java.util.List'.  Actual='java.lang.String'.");
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'beanType' cannot be null.", ()->test(x1).asBeanList(null));
	}

	@Test void bb25_asZonedDateTime() {
		var x1 = zdt("2000-06-01T12:34:56Z");
		var nil = n(Object.class);
		var x2 = "";
		test(x1).asZonedDateTime().asString().isMatches("2000*");
		test(nil).asZonedDateTime().isNull();
		assertThrown(()->test(x2).asZonedDateTime()).asMessage().asOneLine().is("Object was not type 'java.time.ZonedDateTime'.  Actual='java.lang.String'.");
	}

	@Test void bb26_asStringList() {
		var x1 = alist("1");
		var nil = nlist(String.class);
		var x2 = "";
		test(x1).asStringList().isString("[1]");
		test(nil).asStringList().isNull();
		assertThrown(()->test(x2).asStringList()).asMessage().asOneLine().is("Object was not type 'java.util.List'.  Actual='java.lang.String'.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).isExists().isExists();
		assertThrowsWithMessage(BasicAssertionError.class, "Value was null.", ()->test(nil).isExists());
	}

	@Test void ca02_isNull() {
		var x = 1;
		var nil = n(Integer.class);
		test(nil).isNull();
		assertThrowsWithMessage(BasicAssertionError.class, "Value was not null.", ()->test(x).isNull());
	}

	@Test void ca03_isNotNull() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).isNotNull();
		assertThrowsWithMessage(BasicAssertionError.class, "Value was null.", ()->test(nil).isNotNull());
	}

	@Test void ca04a_is_T() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='2'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = 1;
		test(x1).is(x->x==1);
		assertThrown(()->test(x1).is(x->x==2)).asMessage().asOneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='1'.");
	}

	@Test void ca05_isNot() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='1'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = Integer.valueOf(999);
		var x1a = Integer.valueOf(999);
		var nil = n(Integer.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='999(Integer@*)'.  Actual='999(Integer@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='999(Integer@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='999(Integer@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='1'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='1'.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = 1;
		var x1a = 1;
		var x2 = 2;
		var nil = n(Integer.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='2'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='1'.");
	}

	@Test void ca12_isType() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).isType(Integer.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.lang.Integer'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).isExactType(Integer.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.lang.Integer'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.lang.Integer'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = 1;
		var nil = n(Integer.class);
		test(x).isJson("1");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}
}