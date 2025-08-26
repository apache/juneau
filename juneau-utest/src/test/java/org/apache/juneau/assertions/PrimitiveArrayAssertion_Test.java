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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@Deprecated
public class PrimitiveArrayAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private PrimitiveArrayAssertion<Integer,int[]> intArray(int[] value) {
		return assertIntArray(value).setSilent();
	}

	private PrimitiveArrayAssertion<Byte,byte[]> byteArray(byte[] value) {
		return assertByteArray(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() {
		assertThrows(BasicAssertionError.class, ()->byteArray(null).setMsg("Foo {0}", 1).isExists(), "Foo 1");
		assertThrows(RuntimeException.class, ()->byteArray(null).setMsg("Foo {0}", 1).setThrowable(RuntimeException.class).isExists(), "Foo 1");
	}

	@Test
	public void a02_stdout() {
		byteArray(null).setStdOut();
	}

	@Test
	public void a03_invalidUsage() {
		assertThrown(()->PrimitiveArrayAssertion.create("foo")).asMessage().asOneLine().is("Object was not an array.  Actual='java.lang.String'.");
		assertThrown(()->PrimitiveArrayAssertion.create(new Integer[0])).asMessage().asOneLine().is("Object was not an array.  Actual='[Ljava.lang.Integer;'.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() {
		byte[] x = {1}, nil = null;
		byteArray(x).asString().is("[1]");
		byteArray(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() {
		byte[] x = {1}, nil = null;
		var s = Json5Serializer.DEFAULT;
		byteArray(x).asString(s).is("[1]");
		byteArray(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() {
		byte[] x1 = {1};
		byteArray(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() {
		byte[] x = {1}, nil = null;
		byteArray(x).asJson().is("[1]");
		byteArray(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() {
		byte[] x = {2,1}, nil = null;
		byteArray(x).asJsonSorted().is("[1,2]");
		byteArray(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() {
		byte[] x1 = {1}, x2 = {2};
		byteArray(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_item() {
		byte[] x = {1}, nil = null;
		byteArray(x).asItem(0).is((byte)1);
		byteArray(x).asItem(1).isNull();
		byteArray(x).asItem(-1).isNull();
		byteArray(nil).asItem(0).isNull();
	}

	@Test
	public void bb02_length() {
		byte[] x = {1}, nil = null;
		byteArray(x).asLength().is(1);
		byteArray(nil).asLength().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() {
		int[] x = {}, nil = null;
		intArray(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isExists(), "Value was null.");
	}

	@Test
	public void ca02_isNull() {
		int[] x = {}, nil = null;
		intArray(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->intArray(x).isNull(), "Value was not null.");
	}

	@Test
	public void ca03_isNotNull() {
		int[] x = {}, nil = null;
		intArray(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isNotNull(), "Value was null.");
	}

	@Test
	public void ca04a_is_T() {
		int[] x1 = {1,2}, x1a = {1,2}, x2 = {3,4}, nil = null;
		intArray(x1).is(x1);
		intArray(x1).is(x1a);
		intArray(nil).is(nil);
		assertThrown(()->intArray(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[3, 4]'.  Actual='[1, 2]'.");
		assertThrown(()->intArray(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->intArray(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='[3, 4]'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() {
		byte[] x1 = {1,2};
		byteArray(x1).is(x->x.length==2);
		assertThrown(()->byteArray(x1).is(x->x.length==3)).asMessage().asOneLine().is("Unexpected value: '[1, 2]'.");
		assertThrown(()->byteArray(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='[1, 2]'.");
	}

	@Test
	public void ca05_isNot() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isNot(x2);
		byteArray(x1).isNot(nil);
		byteArray(nil).isNot(x1);
		assertThrown(()->byteArray(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isAny(x1a, x2);
		assertThrown(()->byteArray(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[2, 3]]'.  Actual='[1, 2]'.");
		assertThrown(()->byteArray(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[[2, 3]]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isNotAny(x2);
		byteArray(x1).isNotAny();
		byteArray(nil).isNotAny(x2);
		assertThrown(()->byteArray(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='[1, 2]'.  Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() {
		byte[] x1 = {1,2}, x1a = {1,2}, nil = null;
		byteArray(x1).isSame(x1);
		byteArray(nil).isSame(nil);
		assertThrown(()->byteArray(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1, 2](byte[]@*)'.  Actual='[1, 2](byte[]@*)'.");
		assertThrown(()->byteArray(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='[1, 2](byte[]@*)'.  Actual='null(null)'.");
		assertThrown(()->byteArray(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='[1, 2](byte[]@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isSameJsonAs(x1a);
		byteArray(nil).isSameJsonAs(nil);
		assertThrown(()->byteArray(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='null'.");
		assertThrown(()->byteArray(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() {
		byte[] x1 = {1,2}, x1a = {2,1}, x2 = {2,3}, nil = null;
		byteArray(x1).isSameSortedJsonAs(x1a);
		byteArray(nil).isSameSortedJsonAs(nil);
		assertThrown(()->byteArray(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='null'.");
		assertThrown(()->byteArray(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca11_isSameSerializedAs() {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		var s = Json5Serializer.DEFAULT;
		byteArray(x1).isSameSerializedAs(x1a, s);
		byteArray(nil).isSameSerializedAs(nil, s);
		assertThrown(()->byteArray(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='[2,3]'.  Actual='null'.");
		assertThrown(()->byteArray(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='[1,2]'.");
	}

	@Test
	public void ca12_isType() {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isType(byte[].class);
		byteArray(x).isType(Object.class);
		assertThrown(()->byteArray(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='[B'.");
		assertThrown(()->byteArray(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->byteArray(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isExactType(byte[].class);
		assertThrown(()->byteArray(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='[B'.");
		assertThrown(()->byteArray(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='[B'.");
		assertThrown(()->byteArray(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->byteArray(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isString("[1, 2]");
		byteArray(nil).isString(null);
		assertThrown(()->byteArray(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1, 2]'.");
		assertThrown(()->byteArray(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isJson("[1,2]");
		byteArray(nil).isJson("null");
		assertThrown(()->byteArray(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='[1,2]'.");
		assertThrown(()->byteArray(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_is_predicates() {
		int[] x1 = {1,2}, nil = null;
		intArray(x1).is(eq("1"),eq("2"));
		intArray(x1).is(eq(1),eq(2));
		assertThrown(()->intArray(x1).is(eq("1"),eq("3"))).asMessage().asOneLine().is("Array did not contain expected value at index 1.  Value did not match expected.  Expect='3'.  Actual='2'.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).is(eq("1"),eq("3")), "Value was null.");
		intArray(x1).is((Predicate<Integer>)null,null);
	}

	@Test
	public void cb02_any() {
		int[] x1 = {2,3,1}, nil = null;
		intArray(x1).isAny(x -> x .equals(3));
		intArray(x1).isAny(eq(3));
		assertThrown(()->intArray(x1).isAny(x -> x.equals(4))).asMessage().asOneLine().is("Array did not contain any matching value.  Value='[2, 3, 1]'.");
		assertThrows(IllegalArgumentException.class, ()->intArray(x1).isAny((Predicate<Integer>)null), "Argument 'test' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isAny(x->true), "Value was null.");
	}

	@Test
	public void cb03_all() {
		int[] x1 = {2,3,1}, nil = null;
		intArray(x1).isAll(x -> x < 4);
		assertThrown(()->intArray(x1).isAll(x -> x < 3)).asMessage().asOneLine().is("Array contained non-matching value at index 1.  Unexpected value: '3'.");
		assertThrown(()->intArray(x1).isAll(ne(3))).asMessage().asOneLine().is("Array contained non-matching value at index 1.  Value unexpectedly matched.  Value='3'.");
		assertThrows(IllegalArgumentException.class, ()->intArray(x1).isAll(null), "Argument 'test' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isAll(x->true), "Value was null.");
	}

	@Test
	public void cb04_isEmpty() {
		int[] x1={}, x2={1,2}, nil = null;
		intArray(x1).isEmpty();
		assertThrows(BasicAssertionError.class, ()->intArray(x2).isEmpty(), "Array was not empty.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isEmpty(), "Value was null.");
	}

	@Test
	public void cb05_isNotEmpty() {
		int[] x1={}, x2={1,2}, nil = null;
		intArray(x2).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->intArray(x1).isNotEmpty(), "Array was empty.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isNotEmpty(), "Value was null.");
	}

	@Test
	public void cb06_contains() {
		int[] x1 = {1,2}, nil = null;
		intArray(x1).isContains(1);
		assertThrown(()->intArray(x1).isContains(3)).asMessage().asOneLine().is("Array did not contain expected value.  Expect='3'.  Actual='[1, 2]'.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isContains(3), "Value was null.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isContains(null), "Value was null.");
	}

	@Test
	public void cb07_doesNotContain() {
		int[] x1 = {1,2}, nil = null;
		intArray(x1).isNotContains(3);
		assertThrown(()->intArray(x1).isNotContains(1)).asMessage().asOneLine().is("Array contained unexpected value.  Unexpected='1'.  Actual='[1, 2]'.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isNotContains(3), "Value was null.");
	}

	@Test
	public void cb08_isSize() {
		int[] x1={}, x2={1,2}, nil = null;
		intArray(x1).isSize(0);
		intArray(x2).isSize(2);
		assertThrown(()->intArray(x1).isSize(2)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=2.  Actual=0.");
		assertThrown(()->intArray(x2).isSize(0)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=0.  Actual=2.");
		assertThrows(BasicAssertionError.class, ()->intArray(nil).isSize(0), "Value was null.");
	}

	@Test
	public void cb09_has() {
		int[] x={1,2}, nil = null;
		intArray(x).isHas(1,2);
		assertThrown(()->intArray(x).isHas(1,3)).asMessage().asOneLine().is("Array did not contain expected value at index 1.  Value did not match expected.  Expect='3'.  Actual='2'.");
		assertThrown(()->intArray(x).isHas(1)).asMessage().asOneLine().is("Array did not have the expected size.  Expect=1.  Actual=2.");
		assertThrown(()->intArray(x).isHas((Integer[])null)).asMessage().asOneLine().is("Argument 'entries' cannot be null.");
		assertThrown(()->intArray(nil).isHas(1)).asMessage().asOneLine().is("Value was null.");
	}
}