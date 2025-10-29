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

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
class ByteArrayAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private static ByteArrayAssertion test(byte[] value) {
		return assertBytes(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_msg() {
		assertThrows(BasicAssertionError.class, ()->test(null).setMsg("A {0}", 1).isExists(), "A 1");
		assertThrows(RuntimeException.class, ()->test(null).setMsg("A {0}", 1).setThrowable(RuntimeException.class).isExists(), "A 1");
	}

	@Test void a02_stdout() {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ba01a_asString() {
		byte[] x = {'a'}, nil = null;
		test(x).asString().is("a");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		byte[] x = {1}, nil = null;
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("[1]");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		byte[] x1 = {1};
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		byte[] x = {1}, nil = null;
		test(x).asJson().is("[1]");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		byte[] x1 = {2,1}, nil = null;
		test(x1).asJsonSorted().is("[1,2]");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		byte[] x1 = {1}, x2 = {2};
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_item() {
		byte[] x = {1}, nil = null;
		test(x).asItem(0).is((byte)1);
		test(x).asItem(1).isNull();
		test(x).asItem(-1).isNull();
		test(nil).asItem(0).isNull();
	}

	@Test void bb02_length() {
		byte[] x = {1}, nil = null;
		test(x).asLength().is(1);
		test(nil).asLength().isNull();
	}

	@Test void bc01_asString_wCharset() {
		byte[] x = {'a','b'}, nil = null;
		test(x).asString(UTF8).is("ab");
		test(nil).asString(UTF8).isNull();
		assertThrown(()->test(x).asString(UTF8).is("xx")).asMessage().asOneLine().is("String differed at position 0.  Expect='xx'.  Actual='ab'.");
	}

	@Test void bc02_asBase64() {
		byte[] x = {'a','b'}, nil = null;
		test(x).asBase64().is("YWI=");
		test(nil).asBase64().isNull();
		assertThrown(()->test(x).asBase64().is("xx")).asMessage().asOneLine().is("String differed at position 0.  Expect='xx'.  Actual='YWI='.");
	}

	@Test void bc03_asHex() {
		byte[] x = {'a','b'}, nil = null;
		test(x).asHex().is("6162");
		test(nil).asHex().isNull();
		assertThrown(()->test(x).asHex().is("xx")).asMessage().asOneLine().is("String differed at position 0.  Expect='xx'.  Actual='6162'.");
	}

	@Test void bc04_asSpacedHex() {
		byte[] x = {'a','b'}, nil = null;
		test(x).asSpacedHex().is("61 62");
		test(nil).asSpacedHex().isNull();
		assertThrown(()->test(x).asSpacedHex().is("xx")).asMessage().asOneLine().is("String differed at position 0.  Expect='xx'.  Actual='61 62'.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		byte[] x = {}, nil = null;
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		byte[] x = {}, nil = null;
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		byte[] x = {}, nil = null;
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {3,4}, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[3, 4]'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[3, 4]'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		byte[] x1 = {1,2};
		test(x1).is(x->x.length==2);
		assertThrown(()->test(x1).is(x->x.length==3)).asMessage().asOneLine().is("Unexpected value: '[1, 2]'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[1, 2]'.");
	}

	@Test void ca05_isNot() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[2, 3]]'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[2, 3]]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		byte[] x1 = {1,2}, x1a = {1,2}, nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1, 2](byte[]@*)'.  Actual='[1, 2](byte[]@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1, 2](byte[]@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='[1, 2](byte[]@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		byte[] x1 = {1,2}, x1a = {2,1}, x2 = {1,3}, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[1,3]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[1,3]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test void ca11_isSameSerializedAs() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {1,3}, nil = null;
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[1,3]'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[1,3]'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test void ca12_isType() {
		byte[] x = {1,2}, nil = null;
		test(x).isType(byte[].class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='[B'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		byte[] x = {1,2}, nil = null;
		test(x).isExactType(byte[].class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='[B'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='[B'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		byte[] x = {'a','b'}, nil = null;
		test(x).isString("ab");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='ab'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='ab'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		byte[] x = {1,2}, nil = null;
		test(x).isJson("[1,2]");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1,2]'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1,2]'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_isEmpty() {
		byte[] x1 = {}, x2 = {1,2}, nil = null;
		test(x1).isEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x2).isEmpty(), "Array was not empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isEmpty(), "Value was null.");
	}

	@Test void cb02_isNotEmpty() {
		byte[] x1={}, x2={1,2}, nil = null;
		test(x2).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->test(x1).isNotEmpty(), "Array was empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotEmpty(), "Value was null.");
	}

	@Test void cb03_contains() {
		byte[] x1 = {1,2}, nil = null;
		test(x1).isContains((byte)1);
		assertThrown(()->test(x1).isContains((byte)3)).asMessage().asOneLine().is("Array did not contain expected value.  Expect='3'.  Actual='[1, 2]'.");
		assertThrown(()->test(x1).isContains(null)).asMessage().asOneLine().is("Array did not contain expected value.  Expect='null'.  Actual='[1, 2]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isContains((byte)3), "Value was null.");
	}

	@Test void cb04_doesNotContain() {
		byte[] x1 = {1,2}, nil = null;
		test(x1).isNotContains((byte)3);
		test(x1).isNotContains(null);
		assertThrown(()->test(x1).isNotContains((byte)1)).asMessage().asOneLine().is("Array contained unexpected value.  Unexpected='1'.  Actual='[1, 2]'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotContains((byte)3), "Value was null.");
	}

	@Test void cb05_isSize() {
		byte[] x1 = {}, x2={1,2}, nil = null;
		test(x1).isSize(0);
		test(x2).isSize(2);
		assertThrown(()->test(x1).isSize(2)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=2.  Actual=0.");
		assertThrown(()->test(x2).isSize(0)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=0.  Actual=2.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isSize(0), "Value was null.");
	}
}