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
		return assertThrowable(value).setSilent();
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
		Throwable x = throwable("1"), nil = null;
		test(x).asString().is("java.lang.RuntimeException: 1");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Throwable x = throwable("1"), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
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
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_message() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(), nil = null;
		test(x1).asMessage().is("1");
		test(x2).asMessage().isNull();
		test(nil).asMessage().isNull();
	}

	@Test
	public void bb02_messages() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(x1), nil = null;
		test(x1).asMessages().isString("[1]");
		test(x2).asMessages().isString("[java.lang.RuntimeException: 1, 1]");
		test(nil).asMessages().isNull();
	}

	@Test
	public void bb03_localizedMessage() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(), nil = null;
		test(x1).asLocalizedMessage().is("1");
		test(x2).asLocalizedMessage().isNull();
		test(nil).asLocalizedMessage().isNull();
	}

	@Test
	public void bb04_localizedMessages() throws Exception {
		Throwable x1 = throwable("1"), x2 = throwable(x1), nil = null;
		test(x1).asLocalizedMessages().isString("[1]");
		test(x2).asLocalizedMessages().isString("[java.lang.RuntimeException: 1, 1]");
		test(nil).asLocalizedMessages().isNull();
	}

	@Test
	public void bb05_stackTrace() throws Exception {
		Throwable x1 = throwable(), nil = null;
		test(x1).asStackTrace().asJoin().isContains("RuntimeException");
		test(nil).asStackTrace().isNull();
	}

	@Test
	public void bb06a_causedBy() throws Exception {
		Throwable x1 = throwable(throwable("1")), x2 = throwable(), nil = null;
		test(x1).asCausedBy().asMessage().is("1");
		test(x2).asMessage().isNull();
		test(nil).asCausedBy().asMessage().isNull();
	}

	@Test
	public void bb06b_causedBy_wType() throws Exception {
		Throwable x1 = throwable(throwable("1")), x2 = throwable(), nil = null;
		test(x1).asCausedBy(RuntimeException.class).asMessage().is("1");
		test(x2).asCausedBy(RuntimeException.class).isNull();
		test(nil).asCausedBy(RuntimeException.class).isNull();
		assertThrown(()->test(x1).asCausedBy(IOException.class)).asMessage().asOneLine().is("Caused-by exception not of expected type.  Expected='java.io.IOException'.  Actual='java.lang.RuntimeException'.");
	}

	@Test
	public void bb07_find() throws Exception {
		Throwable x1 = throwable(new IOException()), x2 = throwable(), nil = null;
		test(x1).asFind(RuntimeException.class).isExists();
		test(x1).asFind(IOException.class).isExists();
		test(x1).asFind(Exception.class).isExists();
		test(x1).asFind(FileNotFoundException.class).isNull();
		test(x2).asFind(RuntimeException.class).isExists();
		test(x2).asFind(IOException.class).isNull();
		test(nil).asFind(RuntimeException.class).isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Exception was not thrown.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Throwable x = throwable(), nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='java.lang.RuntimeException: 2'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='java.lang.RuntimeException: 2'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Throwable x1 = throwable("1");
		test(x1).is(x->x.getMessage().equals("1"));
		assertThrown(()->test(x1).is(x->x.getMessage().length()==4)).asMessage().asOneLine().is("Unexpected value: 'java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='java.lang.RuntimeException: 1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='java.lang.RuntimeException: 1'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[java.lang.RuntimeException: 2]'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[java.lang.RuntimeException: 2]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='java.lang.RuntimeException: 1'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='java.lang.RuntimeException: 1(RuntimeException@*)'.  Actual='java.lang.RuntimeException: 1(RuntimeException@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='java.lang.RuntimeException: 1(RuntimeException@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='java.lang.RuntimeException: 1(RuntimeException@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''java.lang.RuntimeException: 2''.  Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''java.lang.RuntimeException: 2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''java.lang.RuntimeException: 1''.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''java.lang.RuntimeException: 2''.  Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''java.lang.RuntimeException: 2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''java.lang.RuntimeException: 1''.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Throwable x1 = throwable("1"), x1a = throwable("1"), x2 = throwable("2"), nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''java.lang.RuntimeException: 2''.  Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''java.lang.RuntimeException: 2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''java.lang.RuntimeException: 1''.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isType(Exception.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Exception was not expected type.  Expect='java.lang.String'.  Actual='java.lang.RuntimeException'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Exception was not thrown.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Throwable x = throwable(), nil = null;
		test(x).isExactType(RuntimeException.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Exception was not expected type.  Expect='java.lang.Object'.  Actual='java.lang.RuntimeException'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Exception was not expected type.  Expect='java.lang.String'.  Actual='java.lang.RuntimeException'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Exception was not thrown.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'type' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Throwable x = throwable("1"), nil = null;
		test(x).isString("java.lang.RuntimeException: 1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='java.lang.RuntimeException: 1'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Throwable x = throwable("1"), nil = null;
		test(x).isJson("'java.lang.RuntimeException: 1'");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual=''java.lang.RuntimeException: 1''.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}
}
