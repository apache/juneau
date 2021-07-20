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

import java.util.regex.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private StringAssertion test(Object value) {
		return assertString(value).silent();
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

	@Test
	public void a03_javaStrings() throws Exception {
		test(null).javaStrings();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() throws Exception {
		String x = "1", nil = null;
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		String x = "1", nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x).asString(s).is("'1'");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		String x1 = "1";
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		String x = "1", nil = null;
		test(x).asJson().is("'1'");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		String x = "1", nil = null;
		test(x).asJsonSorted().is("'1'");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		String x1 = "1", x2 = "2";
		test(x1).apply(x -> x2).is(x2);
	}

	@Test
	public void bb01_replaceAll() throws Exception {
		String x = "foobar", nil = null;
		test(x).replaceAll("fo+","bar").is("barbar").is("foobar");
		test(nil).replaceAll("fo+","bar").isNull();
		assertThrown(()->test(x).replaceAll(null,"")).message().is("Argument 'regex' cannot be null.");
		assertThrown(()->test(x).replaceAll("",null)).message().is("Argument 'replacement' cannot be null.");
	}

	@Test
	public void bb02_replace() throws Exception {
		String x = "foobar", nil = null;
		test(x).replace("foo","bar").is("barbar").is("foobar");
		test(nil).replace("foo","bar").isNull();
		assertThrown(()->test(x).replace(null,"bar").isNull()).message().is("Argument 'target' cannot be null.");
		assertThrown(()->test(x).replace("foo",null).isNull()).message().is("Argument 'replacement' cannot be null.");
	}

	@Test
	public void bb03_urlDecode() throws Exception {
		String x = "foo%20bar", nil = null;
		test(x).urlDecode().is("foo bar").is("foo%20bar");
		test(nil).urlDecode().isNull();
	}

	@Test
	public void bb04_lc() throws Exception {
		String x = "FOOBAR", nil = null;
		test(x).lc().is("foobar").is("FOOBAR");
		test(nil).lc().isNull();
	}

	@Test
	public void bb05_uc() throws Exception {
		String x = "foobar", nil = null;
		test(x).uc().is("FOOBAR").is("foobar");
		test(nil).uc().isNull();
	}

	@Test
	public void bb06_lines() throws Exception {
		String x = "foo\nbar", nil = null;
		test(x).lines().has("foo","bar");
		test(nil).lines().isNull();
	}

	@Test
	public void bb07_oneLine() throws Exception {
		String x = "foo\n\tbar", nil = null;
		test(x).oneLine().is("foobar");
		test(nil).oneLine().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		String x = "1", nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		String x = "1", nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		String x = "1", nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("String differed at position 0.Expect='2'.Actual='1'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='1'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("String differed at position 0.Expect='2'.Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		String x1 = "1";
		test(x1).is(x->x.length()==1);
		assertThrown(()->test(x1).is(x->x.length()==2)).message().oneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.Value='1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("String equaled unexpected.Value='1'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("String equaled unexpected.Value='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[2]'.Actual='1'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.Expect='[]'.Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.Expect='[2]'.Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.Unexpected='1'.Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.Unexpected='null'.Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		String x1 = new String("1"), x1a = new String("1"), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='1(String@*)'.Actual='1(String@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.Expect='1(String@*)'.Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.Expect='null(null)'.Actual='1(String@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''2''.Actual=''1''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''2''.Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual=''1''.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''2''.Actual=''1''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.Expect=''2''.Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual=''1''.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect=''2''.Actual=''1''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.Expect=''2''.Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.Expect='null'.Actual=''1''.");
	}

	@Test
	public void ca12_isType() throws Exception {
		String x = "1", nil = null;
		test(x).isType(String.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(Integer.class)).message().oneLine().is("Unexpected type.Expect='java.lang.Integer'.Actual='java.lang.String'.");
		assertThrown(()->test(nil).isType(Integer.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		String x = "1", nil = null;
		test(x).isExactType(String.class);
		assertThrown(()->test(x).isExactType(Object.class)).message().oneLine().is("Unexpected type.Expect='java.lang.Object'.Actual='java.lang.String'.");
		assertThrown(()->test(x).isExactType(Integer.class)).message().oneLine().is("Unexpected type.Expect='java.lang.Integer'.Actual='java.lang.String'.");
		assertThrown(()->test(nil).isExactType(Integer.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		String x = "1", nil = null;
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='1'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		String x = "1", nil = null;
		test(x).isJson("'1'");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual=''1''.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.Expect='null'.Actual=''1''.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.Expect='bad'.Actual='null'.");
	}

	@Test
	public void cb01_isIc() throws Exception {
		String x = "foobar", nil = null;
		test(x).isIc("FOOBAR");
		assertThrown(()->test(x).isIc("FOOBAZ")).message().is("String differed at position 5.\n\tExpect='FOOBAZ'.\n\tActual='foobar'.");
		assertThrown(()->test(nil).isIc("FOOBAZ")).message().is("String differed at position 0.\n\tExpect='FOOBAZ'.\n\tActual='null'.");
		assertThrown(()->test(x).isIc(null)).message().is("String differed at position 0.\n\tExpect='null'.\n\tActual='foobar'.");
	}

	public void cb02_isNotIc() throws Exception {
		String x = "foobar", nil = null;
		test(x).isNotIc("foobaz");
		assertThrown(()->test(x).isNotIc("Foobar")).message().is("String equaled unexpected.\n\tValue='foobar'.");
		assertThrown(()->test(nil).isNotIc(null)).message().is("String equaled unexpected.\n\tValue='null'.");
		test(x).isNotIc(null);
		test(nil).isNotIc("foobar");
	}

	@Test
	public void cb03_isLines() throws Exception {
		String x = "foo\nbar\nbaz", nil = null;
		test(x).isLines("foo","bar","baz");
		assertThrown(()->test(nil).isLines((String[])null)).message().is("Argument 'lines' cannot be null.");
		assertThrown(()->test(nil).isLines((String)null)).message().is("Value was null.");
		assertThrown(()->test(x).javaStrings().isLines("foo","bar","bar")).message().is("String differed at position 10.\n\tExpect='foo\\nbar\\nbar'.\n\tActual='foo\\nbar\\nbaz'.");
	}

	@Test
	public void cb04_isSortedLines() throws Exception {
		String x1 = "foo\nbar\nbaz", x2 = "foo", empty = "", nil = null;
		test(x1).isSortedLines("bar","foo","baz");
		assertThrown(()->test(nil).isSortedLines((String[])null)).message().is("Argument 'lines' cannot be null.");
		test(empty).isSortedLines((String)null);
		assertThrown(()->test(nil).isSortedLines()).message().is("Value was null.");
		assertThrown(()->test(x1).isSortedLines("bar","foo","bar")).message().is("Expected string had different values at line 2.\n\tExpect='bar'.\n\tActual='baz'.");
		assertThrown(()->test(x1).isSortedLines("bar","foo")).message().is("Expected string had different numbers of lines.\n\tExpect='2'.\n\tActual='3'.");
		assertThrown(()->test(nil).isSortedLines("foo")).message().is("Value was null.");
		assertThrown(()->test(x2).isSortedLines((String)null)).message().is("Expected string had different values at line 1.\n\tExpect=''.\n\tActual='foo'.");
	}

	@Test
	public void cb05_contains() throws Exception {
		String x = "foobar", nil = null;
		test(x).contains("foo","bar");
		assertThrown(()->test(x).contains("foo","baz")).message().is("String did not contain expected substring.\n\tSubstring='baz'.\n\tValue='foobar'.");
		test(nil).contains();
		assertThrown(()->test(x).contains((String[])null)).message().is("Argument 'values' cannot be null.");
		test(x).contains((String)null);
		assertThrown(()->test(nil).contains("foobar")).message().is("String did not contain expected substring.\n\tSubstring='foobar'.\n\tValue='null'.");
	}

	@Test
	public void cb06_doesNotContain() throws Exception {
		String x = "foobar", nil = null;
		test(x).doesNotContain("baz","qux");
		assertThrown(()->test(x).doesNotContain("foo","baz")).message().is("String contained unexpected substring.\n\tSubstring='foo'.\n\tValue='foobar'.");
		test(nil).doesNotContain();
		assertThrown(()->test(x).doesNotContain((String[])null)).message().is("Argument 'values' cannot be null.");
		test(x).doesNotContain((String)null);
		test(nil).doesNotContain("foobar");
	}

	@Test
	public void cb07_isEmpty() throws Exception {
		String x = "1", empty = "", nil = null;
		test(empty).isEmpty();
		test(nil).isEmpty();
		assertThrown(()->test(x).isEmpty()).message().is("String was not empty.\n\tValue='1'.");
	}

	@Test
	public void cb08_isNotEmpty() throws Exception {
		String x = "1", empty = "", nil = null;
		test(x).isNotEmpty();
		assertThrown(()->test(empty).isNotEmpty()).message().is("String was empty.");
		assertThrown(()->test(nil).isNotEmpty()).message().is("String was null.");
	}

	@Test
	public void cb09_matches() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).matches("fo*");
		assertThrown(()->test(x).matches("b*")).message().is("String did not match expected pattern.\n\tPattern='\\Qb\\E.*\\Q\\E'.\n\tValue='foo'.");
		assertThrown(()->test(nil).matches("b*")).message().is("Value was null.");
		assertThrown(()->test(empty).matches(null)).message().is("Argument 'searchPattern' cannot be null.");
	}

	@Test
	public void cb10_regex() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).regex("fo+");
		assertThrown(()->test(x).regex("bar")).message().is("String did not match expected pattern.\n\tPattern='bar'.\n\tValue='foo'.");
		assertThrown(()->test(nil).regex("fo+")).message().is("Value was null.");
		assertThrown(()->test(empty).regex((String)null)).message().is("Argument 'regex' cannot be null.");
	}

	@Test
	public void cb10b_regex_wFlags() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).regex("FO+", Pattern.CASE_INSENSITIVE);
		assertThrown(()->test(x).regex("bar")).message().is("String did not match expected pattern.\n\tPattern='bar'.\n\tValue='foo'.");
		assertThrown(()->test(nil).regex("fo+")).message().is("Value was null.");
		assertThrown(()->test(empty).regex((String)null)).message().is("Argument 'regex' cannot be null.");
	}

	@Test
	public void cb10c_regex_wPattern() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).regex(Pattern.compile("FO+", Pattern.CASE_INSENSITIVE));
		assertThrown(()->test(x).regex("bar")).message().is("String did not match expected pattern.\n\tPattern='bar'.\n\tValue='foo'.");
		assertThrown(()->test(nil).regex("fo+")).message().is("Value was null.");
		assertThrown(()->test(empty).regex((String)null)).message().is("Argument 'regex' cannot be null.");
	}

	@Test
	public void cb12_startsWith() throws Exception {
		String x = "foo", nil = null;
		test(x).startsWith("fo");
		assertThrown(()->test(x).startsWith("x")).message().is("String did not start with expected substring.\n\tSubstring='x'.\n\tValue='foo'.");
		assertThrown(()->test(nil).startsWith("x")).message().is("Value was null.");
	}

	@Test
	public void cb13_endsWith() throws Exception {
		String x = "foo", nil = null;
		test(x).endsWith("oo");
		assertThrown(()->test(x).endsWith("x")).message().is("String did not end with expected substring.\n\tSubstring='x'.\n\tValue='foo'.");
		assertThrown(()->test(nil).endsWith("x")).message().is("Value was null.");
	}
}
