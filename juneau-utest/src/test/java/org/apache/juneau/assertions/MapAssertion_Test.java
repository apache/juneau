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
class MapAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <K,V> MapAssertion<K,V> test(Map<K,V> value) {
		return assertMap(value).setSilent();
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
		var x = map("a",1);
		var nil = nmap(Object.class, Object.class);
		test(x).asString().is("{a=1}");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = map("a",1);
		var nil = nmap(Object.class, Object.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("{a:1}");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = map("a",1);
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = map("a",1);
		var nil = nmap(Object.class, Object.class);
		test(x).asJson().is("{a:1}");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x = map("b",2,"a",1);
		var nil = nmap(Object.class, Object.class);
		test(x).asJsonSorted().is("{a:1,b:2}");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = map("a",1);
		var x2 = map("b",2);
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_value() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).asValue("a").asInteger().is(1);
		test(x).asValue("z").asInteger().isNull();
		test(nil).asValue("a").asInteger().isNull();
	}

	@Test void bb02_values() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).asValues("b","a").isHas(2,1);
		test(x).asValues((String)null).isHas((Integer)null);
		test(nil).asValues("a","b").isNull();
	}

	@Test void bb03_extract() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).asValueMap("a").isString("{a=1}");
		test(x).asValueMap((String)null).isString("{null=null}");
		test(nil).asValueMap("a").isNull();
	}

	@Test void bb04_size() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).asSize().is(2);
		test(nil).asSize().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = map();
		var nil = nmap(Object.class, Object.class);
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = map();
		var nil = nmap(Object.class, Object.class);
		assertMap(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = map();
		var nil = nmap(Object.class, Object.class);
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(2,3);
		var nil = nmap(Object.class, Object.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='{2=3}'.  Actual='{1=2}'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='{2=3}'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = map(1,2);
		test(x1).is(x->x.size()==1);
		assertThrown(()->test(x1).is(x->x.size()==2)).asMessage().asOneLine().is("Unexpected value: '{1=2}'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='{1=2}'.");
	}

	@Test void ca05_isNot() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(3,4);
		var nil = nmap(Object.class, Object.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='{1=2}'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(3,4);
		var nil = nmap(Object.class, Object.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[{3=4}]'.  Actual='{1=2}'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[{3=4}]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(3,4);
		var nil = nmap(Object.class, Object.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='{1=2}'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var nil = nmap(Object.class, Object.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='{1=2}(LinkedHashMap@*)'.  Actual='{1=2}(LinkedHashMap@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='{1=2}(LinkedHashMap@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='{1=2}(LinkedHashMap@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(3,4);
		var nil = nmap(Object.class, Object.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{'1':2}'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(3,4);
		var nil = nmap(Object.class, Object.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{'1':2}'.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = map(1,2);
		var x1a = map(1,2);
		var x2 = map(3,4);
		var nil = nmap(Object.class, Object.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{'3':4}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{'1':2}'.");
	}

	@Test void ca12_isType() {
		var x = map(1,2);
		var nil = nmap(Object.class, Object.class);
		test(x).isType(Map.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.LinkedHashMap'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = map(1,2);
		var nil = nmap(Object.class, Object.class);
		test(x).isExactType(LinkedHashMap.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.util.LinkedHashMap'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.LinkedHashMap'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = map(1,2);
		var nil = nmap(Object.class, Object.class);
		test(x).isString("{1=2}");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{1=2}'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{1=2}'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = map(1,2);
		var nil = nmap(Object.class, Object.class);
		test(x).isJson("{'1':2}");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{'1':2}'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{'1':2}'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_isEmpty() {
		var x1 = map();
		var x2 = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x1).isEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x2).isEmpty(), "Map was not empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isEmpty(), "Value was null.");
	}

	@Test void cb02_isNotEmpty() {
		var x1 = map();
		var x2 = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x2).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x1).isNotEmpty(), "Map was empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotEmpty(), "Value was null.");
	}

	@Test void cb03_containsKey() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).isContainsKey("a");
		assertThrown(()->test(x).isContainsKey("x")).asMessage().asOneLine().is("Map did not contain expected key.  Expected key='x'.  Value='{a=1, b=2}'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContainsKey("x"), "Value was null.");
	}

	@Test void cb04_doesNotContainKey() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).isNotContainsKey("x");
		assertThrown(()->test(x).isNotContainsKey("a")).asMessage().asOneLine().is("Map contained unexpected key.  Unexpected key='a'.  Value='{a=1, b=2}'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContainsKey("x"), "Value was null.");
	}

	@Test void cb05_isSize() {
		var x = map("a",1,"b",2);
		var nil = nmap(Object.class, Object.class);
		test(x).isSize(2);
		assertThrown(()->test(x).isSize(1)).asMessage().asOneLine().is("Map did not have the expected size.  Expect=1.  Actual=2.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isSize(0), "Value was null.");
	}
}