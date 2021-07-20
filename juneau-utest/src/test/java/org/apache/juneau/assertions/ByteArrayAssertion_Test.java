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
public class ByteArrayAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private ByteArrayAssertion test(byte[] value) {
		return assertBytes(value).silent();
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
		byte[] x = {'a'}, nil = null;
		test(x).asString().is("a");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		byte[] x = {1}, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x).asString(s).is("[1]");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		byte[] x1 = {1};
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		byte[] x = {1}, nil = null;
		test(x).asJson().is("[1]");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		byte[] x1 = {2,1}, nil = null;
		test(x1).asJsonSorted().is("[1,2]");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		byte[] x1 = {1}, x2 = {2};
		test(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bb01_item() throws Exception {
		byte[] x = {1}, nil = null;
		test(x).item(0).is((byte)1);
		test(x).item(1).isNull();
		test(x).item(-1).isNull();
		test(nil).item(0).isNull();
	}

	@Test
	public void bb02_length() throws Exception {
		byte[] x = {1}, nil = null;
		test(x).length().is(1);
		test(nil).length().isNull();
	}

	@Test
	public void bc01_asString_wCharset() throws Exception {
		byte[] x = {'a','b'}, nil = null;
		test(x).asString(IOUtils.UTF8).is("ab");
		test(nil).asString(IOUtils.UTF8).isNull();
		assertThrown(()->test(x).asString(IOUtils.UTF8).is("xx")).message().is("String differed at position 0.\n\tExpect='xx'.\n\tActual='ab'.");
	}

	@Test
	public void bc02_asBase64() throws Exception {
		byte[] x = {'a','b'}, nil = null;
		test(x).asBase64().is("YWI=");
		test(nil).asBase64().isNull();
		assertThrown(()->test(x).asBase64().is("xx")).message().is("String differed at position 0.\n\tExpect='xx'.\n\tActual='YWI='.");
	}

	@Test
	public void bc03_asHex() throws Exception {
		byte[] x = {'a','b'}, nil = null;
		test(x).asHex().is("6162");
		test(nil).asHex().isNull();
		assertThrown(()->test(x).asHex().is("xx")).message().is("String differed at position 0.\n\tExpect='xx'.\n\tActual='6162'.");
	}

	@Test
	public void bc04_asSpacedHex() throws Exception {
		byte[] x = {'a','b'}, nil = null;
		test(x).asSpacedHex().is("61 62");
		test(nil).asSpacedHex().isNull();
		assertThrown(()->test(x).asSpacedHex().is("xx")).message().is("String differed at position 0.\n\tExpect='xx'.\n\tActual='61 62'.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		byte[] x = {}, nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		byte[] x = {}, nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		byte[] x = {}, nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {3,4}, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("Unexpected value.Expect='[3, 4]'.Actual='[1, 2]'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("Unexpected value.Expect='null'.Actual='[1, 2]'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("Unexpected value.Expect='[3, 4]'.Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		byte[] x1 = {1,2};
		test(x1).is(x->x.length==2);
		assertThrown(()->test(x1).is(x->x.length==3)).message().oneLine().is("Unexpected value: '[1, 2]'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.Value='[1, 2]'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("Unexpected value.Did not expect='[1, 2]'.Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("Unexpected value.Did not expect='null'.Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[[2, 3]]'.Actual='[1, 2]'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.Expect='[]'.Actual='[1, 2]'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[[2, 3]]'.Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.Unexpected='[1, 2]'.Actual='[1, 2]'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.Unexpected='null'.Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='[1, 2](byte[]@*)'.Actual='[1, 2](byte[]@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='[1, 2](byte[]@*)'.Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.Expect='null(null)'.Actual='[1, 2](byte[]@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {2,3}, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[2,3]'.Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='[1,2]'.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		byte[] x1 = {1,2}, x1a = {2,1}, x2 = {1,3}, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[1,3]'.Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect='[1,3]'.Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='[1,2]'.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		byte[] x1 = {1,2}, x1a = {1,2}, x2 = {1,3}, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect='[1,3]'.Actual='[1,2]'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect='[1,3]'.Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual='[1,2]'.");
	}

	@Test
	public void ca12_isType() throws Exception {
		byte[] x = {1,2}, nil = null;
		test(x).isType(byte[].class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).message().oneLine().is("Unexpected type.Expect='java.lang.String'.Actual='[B'.");
		assertThrown(()->test(nil).isType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		byte[] x = {1,2}, nil = null;
		test(x).isExactType(byte[].class);
		assertThrown(()->test(x).isExactType(Object.class)).message().oneLine().is("Unexpected type.Expect='java.lang.Object'.Actual='[B'.");
		assertThrown(()->test(x).isExactType(String.class)).message().oneLine().is("Unexpected type.Expect='java.lang.String'.Actual='[B'.");
		assertThrown(()->test(nil).isExactType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		byte[] x = {'a','b'}, nil = null;
		test(x).isString("ab");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='ab'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='ab'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		byte[] x = {1,2}, nil = null;
		test(x).isJson("[1,2]");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='[1,2]'.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='[1,2]'.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void cb01_isEmpty() throws Exception {
		byte[] x1 = {}, x2 = {1,2}, nil = null;
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).message().is("Array was not empty.");
		assertThrown(()->test(nil).isEmpty()).message().is("Value was null.");
	}

	@Test
	public void cb02_isNotEmpty() throws Exception {
		byte[] x1={}, x2={1,2}, nil = null;
		test(x2).isNotEmpty();
		assertThrown(()->test(x1).isNotEmpty()).message().is("Array was empty.");
		assertThrown(()->test(nil).isNotEmpty()).message().is("Value was null.");
	}

	@Test
	public void cb03_contains() throws Exception {
		byte[] x1 = {1,2}, nil = null;
		test(x1).contains((byte)1);
		assertThrown(()->test(x1).contains((byte)3)).message().oneLine().is("Array did not contain expected value.Expect='3'.Actual='[1, 2]'.");
		assertThrown(()->test(x1).contains(null)).message().oneLine().is("Array did not contain expected value.Expect='null'.Actual='[1, 2]'.");
		assertThrown(()->test(nil).contains((byte)3)).message().is("Value was null.");
	}

	@Test
	public void cb04_doesNotContain() throws Exception {
		byte[] x1 = {1,2}, nil = null;
		test(x1).doesNotContain((byte)3);
		test(x1).doesNotContain(null);
		assertThrown(()->test(x1).doesNotContain((byte)1)).message().oneLine().is("Array contained unexpected value.Unexpected='1'.Actual='[1, 2]'.");
		assertThrown(()->test(nil).doesNotContain((byte)3)).message().is("Value was null.");
	}

	@Test
	public void cb05_isSize() throws Exception {
		byte[] x1 = {}, x2={1,2}, nil = null;
		test(x1).isSize(0);
		test(x2).isSize(2);
		assertThrown(()->test(x1).isSize(2)).message().is("Array did not have the expected size.\n\tExpect=2.\n\tActual=0.");
		assertThrown(()->test(x2).isSize(0)).message().is("Array did not have the expected size.\n\tExpect=0.\n\tActual=2.");
		assertThrown(()->test(nil).isSize(0)).message().is("Value was null.");
	}
}
