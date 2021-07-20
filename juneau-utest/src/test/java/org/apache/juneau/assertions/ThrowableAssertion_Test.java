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

import java.io.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ThrowableAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private <V extends Throwable> ThrowableAssertion<V> test(V value) {
		return assertThrowable(value).silent();
	}

	private Throwable throwable() {
		return new RuntimeException();
	}

	private Throwable throwable(String msg) {
		return new RuntimeException(msg);
	}

	private Throwable throwable(Throwable inner) {
		return new RuntimeException(inner);
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
		Throwable x = throwable("1"), nil = null;
		test(x).asString().is("java.lang.RuntimeException: 1");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Throwable x = throwable("1"), nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x).asString(s).is("'java.lang.RuntimeException: 1'");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		Throwable x1 = throwable();
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		Throwable x = throwable("1"), nil = null;
		test(x).asJson().is("'java.lang.RuntimeException: 1'");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		Throwable x1 = throwable("1"), nil = null;
		test(x1).asJsonSorted().is("'java.lang.RuntimeException: 1'");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable("2");
		test(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bb01_message() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(), nil = null;
		test(x1).message().is("1");
		test(x2).message().isNull();
		test(nil).message().isNull();
	}

	@Test
	public void bb02_messages() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(x1), nil = null;
		test(x1).messages().isString("[1]");
		test(x2).messages().isString("[java.lang.RuntimeException: 1, 1]");
		test(nil).messages().isNull();
	}

	@Test
	public void bb03_localizedMessage() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(), nil = null;
		test(x1).localizedMessage().is("1");
		test(x2).localizedMessage().isNull();
		test(nil).localizedMessage().isNull();
	}

	@Test
	public void bb04_localizedMessages() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(x1), nil = null;
		test(x1).localizedMessages().isString("[1]");
		test(x2).localizedMessages().isString("[java.lang.RuntimeException: 1, 1]");
		test(nil).localizedMessages().isNull();
	}

	@Test
	public void bb05_stackTrace() throws Exception {
		Throwable x1 = throwable(), nil = null;
		test(x1).stackTrace().join().contains("RuntimeException");
		test(nil).stackTrace().isNull();
	}

	@Test
	public void bb06a_causedBy() throws Exception {
		Throwable x1 = throwable(throwable("1")), x2 = throwable(), nil = null;
		test(x1).causedBy().message().is("1");
		test(x2).message().isNull();
		test(nil).causedBy().message().isNull();
	}

	@Test
	public void bb06b_causedBy_wType() throws Exception {
		Throwable x1 = throwable(throwable("1")), x2 = throwable(), nil = null;
		test(x1).causedBy(RuntimeException.class).message().is("1");
		test(x2).causedBy(RuntimeException.class).isNull();
		test(nil).causedBy(RuntimeException.class).isNull();
	}

	@Test
	public void bb07_find() throws Exception {
		Throwable x1 = throwable(new IOException()), x2 = throwable(), nil = null;
		test(x1).find(RuntimeException.class).exists();
		test(x1).find(IOException.class).exists();
		test(x1).find(Exception.class).exists();
		test(x1).find(FileNotFoundException.class).isNull();
		test(x2).find(RuntimeException.class).exists();
		test(x2).find(IOException.class).isNull();
		test(nil).find(RuntimeException.class).isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Exception was not thrown.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Throwable x = throwable(), nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("Unexpected value.Expect='java.lang.RuntimeException: 2'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("Unexpected value.Expect='null'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("Unexpected value.Expect='java.lang.RuntimeException: 2'.Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Throwable x1 = throwable("1");
		test(x1).is(x->x.getMessage().equals("1"));
		assertThrown(()->test(x1).is(x->x.getMessage().length()==4)).message().oneLine().is("Unexpected value: 'java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.Value='java.lang.RuntimeException: 1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("Unexpected value.Did not expect='java.lang.RuntimeException: 1'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("Unexpected value.Did not expect='null'.Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[java.lang.RuntimeException: 2]'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.Expect='[]'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[java.lang.RuntimeException: 2]'.Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.Unexpected='java.lang.RuntimeException: 1'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.Unexpected='null'.Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='java.lang.RuntimeException: 1(RuntimeException@*)'.Actual='java.lang.RuntimeException: 1(RuntimeException@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='java.lang.RuntimeException: 1(RuntimeException@*)'.Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.Expect='null(null)'.Actual='java.lang.RuntimeException: 1(RuntimeException@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''java.lang.RuntimeException: 2''.Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''java.lang.RuntimeException: 2''.Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual=''java.lang.RuntimeException: 1''.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''java.lang.RuntimeException: 2''.Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''java.lang.RuntimeException: 2''.Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual=''java.lang.RuntimeException: 1''.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect=''java.lang.RuntimeException: 2''.Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect=''java.lang.RuntimeException: 2''.Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual=''java.lang.RuntimeException: 1''.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isType(Exception.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).message().oneLine().is("Exception was not expected type.Expect='java.lang.String'.Actual='java.lang.RuntimeException'.");
		assertThrown(()->test(nil).isType(String.class)).message().oneLine().is("Exception was not thrown.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isExactType(RuntimeException.class);
		assertThrown(()->test(x).isExactType(Object.class)).message().oneLine().is("Exception was not expected type.Expect='java.lang.Object'.Actual='java.lang.RuntimeException'.");
		assertThrown(()->test(x).isExactType(String.class)).message().oneLine().is("Exception was not expected type.Expect='java.lang.String'.Actual='java.lang.RuntimeException'.");
		assertThrown(()->test(nil).isExactType(String.class)).message().oneLine().is("Exception was not thrown.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'type' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Throwable x = throwable("1"), nil = null;
		test(x).isString("java.lang.RuntimeException: 1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Throwable x = throwable("1"), nil = null;
		test(x).isJson("'java.lang.RuntimeException: 1'");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}
}
