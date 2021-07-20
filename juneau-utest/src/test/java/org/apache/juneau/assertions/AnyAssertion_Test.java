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

import java.time.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AnyAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <T> AnyAssertion<T> test(T value) {
		return assertAny(value).silent();
	}

	private static ZonedDateTime zdt(String s) {
		return ZonedDateTime.parse(s);
	}

	private static Date date(String s) {
		return new Date(ZonedDateTime.parse(s).toEpochSecond()*1000);
	}

	@SafeVarargs
	private static <T> List<T> list(T...objects) {
		return AList.of(objects);
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
		Integer x = 1, nil = null;
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Integer x = 1, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
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
		assertThrown(()->test(new A2()).asJson()).messages().any(contains("Could not call getValue() on property 'foo'"));
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		Integer[] x1 = {2,1}, nil = null;
		Object x2 = new A1();
		test(x1).asJsonSorted().is("[1,2]");
		test(x2).asJsonSorted().is("{a:1,b:2}");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		Integer x1 = 1, x2 = 2;
		test(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bb01_asArray() throws Exception {
		Integer[] x1 = {1,2}, nil = null;
		String x2 = "";
		test(x1).asArray(Integer.class).item(0).is(1);
		test(nil).asArray(Integer.class).isNull();
		assertThrown(()->test(x2).asArray(Integer.class)).message().oneLine().is("Object was not type 'java.lang.Integer[]'.Actual='java.lang.String'.");
		assertThrown(()->test(x2).asArray(null)).message().oneLine().is("Argument 'elementType' cannot be null.");
	}

	@Test
	public void bb02_asIntArray() throws Exception {
		int[] x1 = {1}, nil = null;
		Object x2 = "";
		test(x1).asIntArray().isString("[1]");
		test(nil).asIntArray().isNull();
		assertThrown(()->test(x2).asIntArray()).message().oneLine().is("Object was not type 'int[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb03_asLongArray() throws Exception {
		long[] x1 = {1}, nil = null;
		Object x2 = "";
		test(x1).asLongArray().isString("[1]");
		test(nil).asLongArray().isNull();
		assertThrown(()->test(x2).asLongArray()).message().oneLine().is("Object was not type 'long[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb04_asShortArray() throws Exception {
		short[] x1 = {1}, nil = null;
		Object x2 = "";
		test(x1).asShortArray().isString("[1]");
		test(nil).asShortArray().isNull();
		assertThrown(()->test(x2).asShortArray()).message().oneLine().is("Object was not type 'short[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb05_asFloatArray() throws Exception {
		float[] x1 = {1}, nil = null;
		Object x2 = "";
		test(x1).asFloatArray().isString("[1.0]");
		test(nil).asFloatArray().isNull();
		assertThrown(()->test(x2).asFloatArray()).message().oneLine().is("Object was not type 'float[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb06_asDoubleArray() throws Exception {
		double[] x1 = {1}, nil = null;
		Object x2 = "";
		test(x1).asDoubleArray().isString("[1.0]");
		test(nil).asDoubleArray().isNull();
		assertThrown(()->test(x2).asDoubleArray()).message().oneLine().is("Object was not type 'double[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb07_asCharArray() throws Exception {
		char[] x1 = {'a'}, nil = null;
		Object x2 = "";
		test(x1).asCharArray().isString("[a]");
		test(nil).asCharArray().isNull();
		assertThrown(()->test(x2).asCharArray()).message().oneLine().is("Object was not type 'char[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb08_asByteArray() throws Exception {
		byte[] x1 = {1}, nil = null;
		Object x2 = "";
		test(x1).asByteArray().isString("[1]");
		test(nil).asByteArray().isNull();
		assertThrown(()->test(x2).asByteArray()).message().oneLine().is("Object was not type 'byte[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb09_asBooleanArray() throws Exception {
		boolean[] x1 = {true}, nil = null;
		Object x2 = "";
		test(x1).asBooleanArray().isString("[true]");
		test(nil).asBooleanArray().isNull();
		assertThrown(()->test(x2).asBooleanArray()).message().oneLine().is("Object was not type 'boolean[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb10_asBoolean() throws Exception {
		Boolean x1 = true, nil = null;
		Object x2 = "";
		test(x1).asBoolean().isString("true");
		test(nil).asBoolean().isNull();
		assertThrown(()->test(x2).asBoolean()).message().oneLine().is("Object was not type 'java.lang.Boolean'.Actual='java.lang.String'.");
	}

	@Test
	public void bb11_asBytes() throws Exception {
		byte[] x1 = {'a'}, nil = null;
		Object x2 = "";
		test(x1).asBytes().isString("a");
		test(nil).asBytes().isNull();
		assertThrown(()->test(x2).asBytes()).message().oneLine().is("Object was not type 'byte[]'.Actual='java.lang.String'.");
	}

	@Test
	public void bb12_asCollection() throws Exception {
		List<Integer> x1 = list(1), nil = null;
		Object x2 = "";
		test(x1).asCollection().isString("[1]");
		test(nil).asCollection().isNull();
		assertThrown(()->test(x2).asCollection()).message().oneLine().is("Object was not type 'java.util.Collection'.Actual='java.lang.String'.");
	}

	@Test
	public void bb13_asCollection_wType() throws Exception {
		List<Integer> x1 = list(1), nil = null;
		Object x2 = "";
		test(x1).asCollection(Integer.class).isString("[1]");
		test(nil).asCollection(Integer.class).isNull();
		assertThrown(()->test(x2).asCollection(Integer.class)).message().oneLine().is("Object was not type 'java.util.Collection'.Actual='java.lang.String'.");
		assertThrown(()->test(x1).asCollection(null)).message().is("Argument 'elementType' cannot be null.");
	}

	@Test
	public void bb14_asComparable() throws Exception {
		Integer x1 = 1, nil = null;
		Object x2 = list();
		test(x1).asComparable().isString("1");
		test(nil).asComparable().isNull();
		assertThrown(()->test(x2).asComparable()).message().oneLine().is("Object was not type 'java.lang.Comparable'.Actual='org.apache.juneau.collections.AList'.");
	}

	@Test
	public void bb15_asDate() throws Exception {
		Date x1 = date("2000-06-01T12:34:56Z"), nil = null;
		Object x2 = "";
		test(x1).asDate().asString().matches("*2000");
		test(nil).asDate().isNull();
		assertThrown(()->test(x2).asDate()).message().oneLine().is("Object was not type 'java.util.Date'.Actual='java.lang.String'.");
	}

	@Test
	public void bb16_asInteger() throws Exception {
		Integer x1 = 1, nil = null;
		Object x2 = "";
		test(x1).asInteger().isString("1");
		test(nil).asInteger().isNull();
		assertThrown(()->test(x2).asInteger()).message().oneLine().is("Object was not type 'java.lang.Integer'.Actual='java.lang.String'.");
	}

	@Test
	public void bb17_asLong() throws Exception {
		Long x1 = 1l, nil = null;
		Object x2 = "";
		test(x1).asLong().isString("1");
		test(nil).asLong().isNull();
		assertThrown(()->test(x2).asLong()).message().oneLine().is("Object was not type 'java.lang.Long'.Actual='java.lang.String'.");
	}

	@Test
	public void bb18_asList() throws Exception {
		List<Integer> x1 = list(1), nil = null;
		Object x2 = "";
		test(x1).asList().isString("[1]");
		test(nil).asList().isNull();
		assertThrown(()->test(x2).asList()).message().oneLine().is("Object was not type 'java.util.List'.Actual='java.lang.String'.");
	}

	@Test
	public void bb19_asList_wType() throws Exception {
		List<Integer> x1 = list(1), nil = null;
		Object x2 = "";
		test(x1).asList(Integer.class).isString("[1]");
		test(nil).asList(Integer.class).isNull();
		assertThrown(()->test(x2).asList(Integer.class)).message().oneLine().is("Object was not type 'java.util.List'.Actual='java.lang.String'.");
		assertThrown(()->test(x1).asCollection(null)).message().is("Argument 'elementType' cannot be null.");
	}

	@Test
	public void bb20_asMap() throws Exception {
		Map<String,Integer> x1 = AMap.of("a",2), nil = null;
		Object x2 = "";
		test(x1).asMap().isString("{a=2}");
		test(nil).asMap().isNull();
		assertThrown(()->test(x2).asMap()).message().oneLine().is("Object was not type 'java.util.Map'.Actual='java.lang.String'.");
	}

	@Test
	public void bb21_asMap_wTypes() throws Exception {
		Map<String,Integer> x1 = AMap.of("a",2), nil = null;
		Object x2 = "";
		test(x1).asMap(String.class,Integer.class).isString("{a=2}");
		test(nil).asMap(String.class,Integer.class).isNull();
		assertThrown(()->test(x2).asMap(String.class,Integer.class)).message().oneLine().is("Object was not type 'java.util.Map'.Actual='java.lang.String'.");
		assertThrown(()->test(x1).asMap(null,Integer.class)).message().is("Argument 'keyType' cannot be null.");
		assertThrown(()->test(x1).asMap(String.class,null)).message().is("Argument 'valueType' cannot be null.");
	}

	@Test
	public void bb22_asBean() throws Exception {
		A1 x1 = A1, nil = null;
		test(x1).asBean().isString("a=1,b=2");
		test(nil).asBean().isNull();
	}

	@Test
	public void bb23_asBean_wType() throws Exception {
		A1 x1 = A1, nil = null;
		Object x2 = "";
		test(x1).asBean().isString("a=1,b=2");
		test(nil).asBean().isNull();
		assertThrown(()->test(x2).asBean(A2.class)).message().oneLine().is("Object was not type 'org.apache.juneau.assertions.AnyAssertion_Test$A2'.Actual='java.lang.String'.");
		assertThrown(()->test(x1).asBean(null)).message().is("Argument 'beanType' cannot be null.");
	}

	@Test
	public void bb24_asBeanList() throws Exception {
		List<A1> x1 = list(A1), nil = null;
		Object x2 = "";
		test(x1).asBeanList(A1.class).isString("[a=1,b=2]");
		test(nil).asBeanList(A1.class).isNull();
		assertThrown(()->test(x2).asBeanList(A2.class)).message().oneLine().is("Object was not type 'java.util.List'.Actual='java.lang.String'.");
		assertThrown(()->test(x1).asBeanList(null)).message().is("Argument 'beanType' cannot be null.");
	}

	@Test
	public void bb25_asZonedDateTime() {
		Object x1 = zdt("2000-06-01T12:34:56Z"), nil = null;
		Object x2 = "";
		test(x1).asZonedDateTime().asString().matches("2000*");
		test(nil).asZonedDateTime().isNull();
		assertThrown(()->test(x2).asZonedDateTime()).message().oneLine().is("Object was not type 'java.time.ZonedDateTime'.Actual='java.lang.String'.");
	}

	@Test
	public void bb26_asStringList() throws Exception {
		List<String> x1 = list("1"), nil = null;
		Object x2 = "";
		test(x1).asStringList().isString("[1]");
		test(nil).asStringList().isNull();
		assertThrown(()->test(x2).asStringList()).message().oneLine().is("Object was not type 'java.util.List'.Actual='java.lang.String'.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Integer x = 1, nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Integer x = 1, nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Integer x = 1, nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("Unexpected value.Expect='2'.Actual='1'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("Unexpected value.Expect='null'.Actual='1'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("Unexpected value.Expect='2'.Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Integer x1 = 1;
		test(x1).is(x->x==1);
		assertThrown(()->test(x1).is(x->x==2)).message().oneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.Value='1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("Unexpected value.Did not expect='1'.Actual='1'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("Unexpected value.Did not expect='null'.Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[2]'.Actual='1'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.Expect='[]'.Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[2]'.Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.Unexpected='1'.Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.Unexpected='null'.Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		Integer x1 = new Integer(1), x1a = new Integer(1), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='1(Integer@*)'.Actual='1(Integer@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='1(Integer@*)'.Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.Expect='null(null)'.Actual='1(Integer@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='2'.Actual='1'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='2'.Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='1'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='2'.Actual='1'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='2'.Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='1'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Integer x1 = 1, x1a = 1, x2 = 2, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect='2'.Actual='1'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect='2'.Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='1'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Integer x = 1, nil = null;
		test(x).isType(Integer.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).message().oneLine().is("Unexpected type.Expect='java.lang.String'.Actual='java.lang.Integer'.");
		assertThrown(()->test(nil).isType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Integer x = 1, nil = null;
		test(x).isExactType(Integer.class);
		assertThrown(()->test(x).isExactType(Object.class)).message().oneLine().is("Unexpected type.Expect='java.lang.Object'.Actual='java.lang.Integer'.");
		assertThrown(()->test(x).isExactType(String.class)).message().oneLine().is("Unexpected type.Expect='java.lang.String'.Actual='java.lang.Integer'.");
		assertThrown(()->test(nil).isExactType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Integer x = 1, nil = null;
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='1'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Integer x = 1, nil = null;
		test(x).isJson("1");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='1'.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='1'.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}
}
