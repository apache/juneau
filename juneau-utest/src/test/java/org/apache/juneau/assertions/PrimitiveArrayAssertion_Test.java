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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class PrimitiveArrayAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private PrimitiveArrayAssertion<Integer,int[]> intArray(int[] value) {
		return assertIntArray(value).silent();
	}

	private PrimitiveArrayAssertion<Byte,byte[]> byteArray(byte[] value) {
		return assertByteArray(value).silent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_msg() throws Exception {
		assertThrown(()->byteArray(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->byteArray(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
	}

	@Test
	public void a02_stdout() throws Exception {
		byteArray(null).stdout();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() throws Exception {
		byte[] x = {1}, nil = null;
		byteArray(x).asString().is("[1]");
		byteArray(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		byte[] x = {1}, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		byteArray(x).asString(s).is("[1]");
		byteArray(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		byte[] x1 = {1};
		byteArray(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		byte[] x = {1}, nil = null;
		byteArray(x).asJson().is("[1]");
		byteArray(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		byte[] x = {2,1}, nil = null;
		byteArray(x).asJsonSorted().is("[1,2]");
		byteArray(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		byte[] x1 = {1}, x2 = {2};
		byteArray(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bb01_item() throws Exception {
		byte[] x = {1}, nil = null;
		byteArray(x).item(0).is((byte)1);
		byteArray(x).item(1).isNull();
		byteArray(x).item(-1).isNull();
		byteArray(nil).item(0).isNull();
	}

	@Test
	public void bb02_length() throws Exception {
		byte[] x = {1}, nil = null;
		byteArray(x).length().is(1);
		byteArray(nil).length().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		int[] x = {}, nil = null;
		intArray(x).exists().exists();
		assertThrown(()->intArray(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		int[] x = {}, nil = null;
		intArray(nil).isNull();
		assertThrown(()->intArray(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		int[] x = {}, nil = null;
		intArray(x).isNotNull();
		assertThrown(()->intArray(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		int[] x1 = {1,2}, x1a = {1,2}, x2 = {3,4}, nil = null;
		intArray(x1).is(x1);
		intArray(x1).is(x1a);
		intArray(nil).is(nil);
		assertThrown(()->intArray(x1).is(x2)).message().oneLine().is("Unexpected value.Expect='[3, 4]'.Actual='[1, 2]'.");
		assertThrown(()->intArray(x1).is(nil)).message().oneLine().is("Unexpected value.Expect='null'.Actual='[1, 2]'.");
		assertThrown(()->intArray(nil).is(x2)).message().oneLine().is("Unexpected value.Expect='[3, 4]'.Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		byte[] x1 = {1,2};
		byteArray(x1).is(x->x.length==2);
		assertThrown(()->byteArray(x1).is(x->x.length==3)).message().oneLine().is("Unexpected value: '[1, 2]'.");
		assertThrown(()->byteArray(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.Value='[1, 2]'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isNot(x2);
		byteArray(x1).isNot(nil);
		byteArray(nil).isNot(x1);
		assertThrown(()->byteArray(x1).isNot(x1a)).message().oneLine().is("Unexpected value.Did not expect='[1, 2]'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isNot(nil)).message().oneLine().is("Unexpected value.Did not expect='null'.Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isAny(x1a, x2);
		assertThrown(()->byteArray(x1).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[[2, 3]]'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(x1).isAny()).message().oneLine().is("Expected value not found.Expect='[]'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[[2, 3]]'.Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isNotAny(x2);
		byteArray(x1).isNotAny();
		byteArray(nil).isNotAny(x2);
		assertThrown(()->byteArray(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.Unexpected='[1, 2]'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.Unexpected='null'.Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, nil = null;
		byteArray(x1).isSame(x1);
		byteArray(nil).isSame(nil);
		assertThrown(()->byteArray(x1).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='[1, 2](byte[]@*)'.Actual='[1, 2](byte[]@*)'.");
		assertThrown(()->byteArray(nil).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='[1, 2](byte[]@*)'.Actual='null(null)'.");
		assertThrown(()->byteArray(x1).isSame(nil)).message().oneLine().matches("Not the same value.Expect='null(null)'.Actual='[1, 2](byte[]@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		byteArray(x1).isSameJsonAs(x1a);
		byteArray(nil).isSameJsonAs(nil);
		assertThrown(()->byteArray(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='null'.");
		assertThrown(()->byteArray(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='[1,2]'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		byte[] x1 = {1,2}, x1a = {2,1}, x2 = {2,3}, nil = null;
		byteArray(x1).isSameSortedJsonAs(x1a);
		byteArray(nil).isSameSortedJsonAs(nil);
		assertThrown(()->byteArray(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='null'.");
		assertThrown(()->byteArray(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='[1,2]'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		byteArray(x1).isSameSerializedAs(x1a, s);
		byteArray(nil).isSameSerializedAs(nil, s);
		assertThrown(()->byteArray(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='null'.");
		assertThrown(()->byteArray(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='[1,2]'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isType(byte[].class);
		byteArray(x).isType(Object.class);
		assertThrown(()->byteArray(x).isType(String.class)).message().oneLine().is("Unexpected type.Expect='java.lang.String'.Actual='[B'.");
		assertThrown(()->byteArray(nil).isType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->byteArray(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isExactType(byte[].class);
		assertThrown(()->byteArray(x).isExactType(Object.class)).message().oneLine().is("Unexpected type.Expect='java.lang.Object'.Actual='[B'.");
		assertThrown(()->byteArray(x).isExactType(String.class)).message().oneLine().is("Unexpected type.Expect='java.lang.String'.Actual='[B'.");
		assertThrown(()->byteArray(nil).isExactType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->byteArray(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isString("[1, 2]");
		byteArray(nil).isString(null);
		assertThrown(()->byteArray(x).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(x).isString(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		byte[] x = {1,2}, nil = null;
		byteArray(x).isJson("[1,2]");
		byteArray(nil).isJson("null");
		assertThrown(()->byteArray(x).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='[1,2]'.");
		assertThrown(()->byteArray(x).isJson(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='[1,2]'.");
		assertThrown(()->byteArray(nil).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() throws Exception {
		byte[] x1={}, x2={1,2}, nil = null;
		assertThrown(()->byteArray(nil).isEmpty()).message().is("Value was null.");
		byteArray(x1).isEmpty();
		assertThrown(()->byteArray(x2).isEmpty()).message().is("Array was not empty.");
	}

	@Test
	public void cb02_isNotEmpty() throws Exception {
		byte[] x1={}, x2={1,2}, nil = null;
		assertThrown(()->byteArray(nil).isNotEmpty()).message().is("Value was null.");
		assertThrown(()->byteArray(x1).isNotEmpty()).message().is("Array was empty.");
		byteArray(x2).isNotEmpty();
	}

	@Test
	public void cb03_contains() throws Exception {
		byte[] x1 = {1,2}, nil = null;
		byteArray(x1).contains((byte)1);
		assertThrown(()->byteArray(x1).contains((byte)3)).message().oneLine().is("Array did not contain expected value.Expect='3'.Actual='[1, 2]'.");
		assertThrown(()->byteArray(nil).contains((byte)3)).message().is("Value was null.");
		assertThrown(()->byteArray(nil).contains(null)).message().is("Value was null.");
	}

	@Test
	public void cb04_doesNotContain() throws Exception {
		byte[] x1 = {1,2}, nil = null;
		byteArray(x1).doesNotContain((byte)3);
		byteArray(x1).doesNotContain(null);
		assertThrown(()->byteArray(nil).doesNotContain((byte)3)).message().is("Value was null.");
		assertThrown(()->byteArray(x1).doesNotContain((byte)1)).message().oneLine().is("Array contained unexpected value.Unexpected='1'.Actual='[1, 2]'.");
	}

	@Test
	public void cb05_isSize() throws Exception {
		byte[] x1={}, x2={1,2}, nil = null;
		byteArray(x1).isSize(0);
		byteArray(x2).isSize(2);
		assertThrown(()->byteArray(nil).isSize(0)).message().is("Value was null.");
		assertThrown(()->byteArray(x1).isSize(2)).message().is("Array did not have the expected size.\n\tExpect=2.\n\tActual=0.");
		assertThrown(()->byteArray(x2).isSize(0)).message().is("Array did not have the expected size.\n\tExpect=0.\n\tActual=2.");
	}
}
