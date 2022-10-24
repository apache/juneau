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
public class MapAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <K,V> MapAssertion<K,V> test(Map<K,V> value) {
		return assertMap(value).setSilent();
	}

	@SafeVarargs
	private static <K,V> Map<K,V> map(Object...objects) {
		return mapBuilder(new LinkedHashMap<K,V>()).addPairs(objects).build();
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
		Map<String,Integer> x = map("a",1), nil = null;
		test(x).asString().is("{a=1}");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Map<String,Integer> x = map("a",1), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("{a:1}");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		Map<String,Integer> x1 = map("a",1);
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		Map<String,Integer> x = map("a",1), nil = null;
		test(x).asJson().is("{a:1}");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		Map<String,Integer> x = map("b",2,"a",1), nil = null;
		test(x).asJsonSorted().is("{a:1,b:2}");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		Map<String,Integer> x1 = map("a",1), x2 = map("b",2);
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_value() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).asValue("a").asInteger().is(1);
		test(x).asValue("z").asInteger().isNull();
		test(nil).asValue("a").asInteger().isNull();
	}

	@Test
	public void bb02_values() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).asValues("b","a").isHas(2,1);
		test(x).asValues((String)null).isHas((Integer)null);
		test(nil).asValues("a","b").isNull();
	}

	@Test
	public void bb03_extract() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).asValueMap("a").isString("{a=1}");
		test(x).asValueMap((String)null).isString("{null=null}");
		test(nil).asValueMap("a").isNull();
	}

	@Test
	public void bb04_size() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).asSize().is(2);
		test(nil).asSize().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Map<Integer,Integer> x = map(), nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Map<Integer,Integer> x = map(), nil = null;
		assertMap(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Map<Integer,Integer> x = map(), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(2,3), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='{2=3}'.  Actual='{1=2}'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='{2=3}'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Map<Integer,Integer> x1 = map(1,2);
		test(x1).is(x->x.size()==1);
		assertThrown(()->test(x1).is(x->x.size()==2)).asMessage().asOneLine().is("Unexpected value: '{1=2}'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='{1=2}'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(3,4), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='{1=2}'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ca06_isAny() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(3,4), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[{3=4}]'.  Actual='{1=2}'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[{3=4}]'.  Actual='null'.");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void ca07_isNotAny() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(3,4), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='{1=2}'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='{1=2}(LinkedHashMap@*)'.  Actual='{1=2}(LinkedHashMap@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='{1=2}(LinkedHashMap@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='{1=2}(LinkedHashMap@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(3,4), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{'1':2}'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(3,4), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{'1':2}'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Map<Integer,Integer> x1 = map(1,2), x1a = map(1,2), x2 = map(3,4), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{'1':2}'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Map<Integer,Integer> x = map(1,2), nil = null;
		test(x).isType(Map.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.LinkedHashMap'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Map<Integer,Integer> x = map(1,2), nil = null;
		test(x).isExactType(LinkedHashMap.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.util.LinkedHashMap'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.LinkedHashMap'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Map<Integer,Integer> x = map(1,2), nil = null;
		test(x).isString("{1=2}");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{1=2}'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Map<Integer,Integer> x = map(1,2), nil = null;
		test(x).isJson("{'1':2}");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{'1':2}'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() throws Exception {
		Map<String,Integer> x1 = map(), x2 = map("a",1,"b",2), nil = null;
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).asMessage().is("Map was not empty.");
		assertThrown(()->test(nil).isEmpty()).asMessage().is("Value was null.");
	}

	@Test
	public void cb02_isNotEmpty() throws Exception {
		Map<String,Integer> x1 = map(), x2 = map("a",1,"b",2), nil = null;
		test(x2).isNotEmpty();
		assertThrown(()->test(x1).isNotEmpty()).asMessage().is("Map was empty.");
		assertThrown(()->test(nil).isNotEmpty()).asMessage().is("Value was null.");
	}

	@Test
	public void cb03_containsKey() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).isContainsKey("a");
		assertThrown(()->test(x).isContainsKey("x")).asMessage().asOneLine().is("Map did not contain expected key.  Expected key='x'.  Value='{a=1, b=2}'.");
		assertThrown(()->test(nil).isContainsKey("x")).asMessage().is("Value was null.");
	}

	@Test
	public void cb04_doesNotContainKey() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).isNotContainsKey("x");
		assertThrown(()->test(x).isNotContainsKey("a")).asMessage().asOneLine().is("Map contained unexpected key.  Unexpected key='a'.  Value='{a=1, b=2}'.");
		assertThrown(()->test(nil).isContainsKey("x")).asMessage().is("Value was null.");
	}

	@Test
	public void cb05_isSize() throws Exception {
		Map<String,Integer> x = map("a",1,"b",2), nil = null;
		test(x).isSize(2);
		assertThrown(()->test(x).isSize(1)).asMessage().asOneLine().is("Map did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->test(nil).isSize(0)).asMessage().is("Value was null.");
	}
}
