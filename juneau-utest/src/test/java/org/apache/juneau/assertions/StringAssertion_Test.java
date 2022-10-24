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
		return assertString(value).setSilent();
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

	@Test
	public void a03_javaStrings() throws Exception {
		test(null).asJavaStrings();
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
		WriterSerializer s = Json5Serializer.DEFAULT;
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
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test
	public void bb01_replaceAll() throws Exception {
		String x = "foobar", nil = null;
		test(x).asReplaceAll("fo+","bar").is("barbar").is("foobar");
		test(nil).asReplaceAll("fo+","bar").isNull();
		assertThrown(()->test(x).asReplaceAll(null,"")).asMessage().is("Argument 'regex' cannot be null.");
		assertThrown(()->test(x).asReplaceAll("",null)).asMessage().is("Argument 'replacement' cannot be null.");
	}

	@Test
	public void bb02_replace() throws Exception {
		String x = "foobar", nil = null;
		test(x).asReplace("foo","bar").is("barbar").is("foobar");
		test(nil).asReplace("foo","bar").isNull();
		assertThrown(()->test(x).asReplace(null,"bar").isNull()).asMessage().is("Argument 'target' cannot be null.");
		assertThrown(()->test(x).asReplace("foo",null).isNull()).asMessage().is("Argument 'replacement' cannot be null.");
	}

	@Test
	public void bb03_urlDecode() throws Exception {
		String x = "foo%20bar", nil = null;
		test(x).asUrlDecode().is("foo bar").is("foo%20bar");
		test(nil).asUrlDecode().isNull();
	}

	@Test
	public void bb04_lc() throws Exception {
		String x = "FOOBAR", nil = null;
		test(x).asLc().is("foobar").is("FOOBAR");
		test(nil).asLc().isNull();
	}

	@Test
	public void bb05_uc() throws Exception {
		String x = "foobar", nil = null;
		test(x).asUc().is("FOOBAR").is("foobar");
		test(nil).asUc().isNull();
	}

	@Test
	public void bb06_lines() throws Exception {
		String x = "foo\nbar", nil = null;
		test(x).asLines().isHas("foo","bar");
		test(nil).asLines().isNull();
	}

	@Test
	public void bb07_oneLine() throws Exception {
		String x = "foo  bar", nil = null;
		test(x).asOneLine().is("foo  bar");
		test(nil).asOneLine().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		String x = "1", nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		String x = "1", nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		String x = "1", nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("String differed at position 0.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("String differed at position 0.  Expect='2'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		String x1 = "1";
		test(x1).is(x->x.length()==1);
		assertThrown(()->test(x1).is(x->x.length()==2)).asMessage().asOneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='1'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("String equaled unexpected.  Value='1'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("String equaled unexpected.  Value='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='1'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		String x1 = new String("1"), x1a = new String("1"), nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(String@*)'.  Actual='1(String@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(String@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='1(String@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual=''1''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''1''.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual=''1''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''1''.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		String x1 = "1", x1a = "1", x2 = "2", nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual=''1''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''1''.");
	}

	@Test
	public void ca12_isType() throws Exception {
		String x = "1", nil = null;
		test(x).isType(String.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(Integer.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->test(nil).isType(Integer.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		String x = "1", nil = null;
		test(x).isExactType(String.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.lang.String'.");
		assertThrown(()->test(x).isExactType(Integer.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->test(nil).isExactType(Integer.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		String x = "1", nil = null;
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		String x = "1", nil = null;
		test(x).isJson("'1'");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual=''1''.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual=''1''.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isIc() throws Exception {
		String x = "foobar", nil = null;
		test(x).isIc("FOOBAR");
		assertThrown(()->test(x).isIc("FOOBAZ")).asMessage().asOneLine().is("String differed at position 5.  Expect='FOOBAZ'.  Actual='foobar'.");
		assertThrown(()->test(nil).isIc("FOOBAZ")).asMessage().asOneLine().is("String differed at position 0.  Expect='FOOBAZ'.  Actual='null'.");
		assertThrown(()->test(x).isIc(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='foobar'.");
	}

	@Test
	public void cb02_isNotIc() throws Exception {
		String x = "foobar", nil = null;
		test(x).isNotIc("foobaz");
		assertThrown(()->test(x).isNotIc("Foobar")).asMessage().asOneLine().is("String equaled unexpected.  Value='foobar'.");
		assertThrown(()->test(nil).isNotIc(null)).asMessage().asOneLine().is("String equaled unexpected.  Value='null'.");
		test(x).isNotIc(null);
		test(nil).isNotIc("foobar");
	}

	@Test
	public void cb03_isLines() throws Exception {
		String x = "foo\nbar\nbaz", nil = null;
		test(x).isLines("foo","bar","baz");
		assertThrown(()->test(nil).isLines((String[])null)).asMessage().is("Argument 'lines' cannot be null.");
		assertThrown(()->test(nil).isLines((String)null)).asMessage().is("Value was null.");
		assertThrown(()->test(x).asJavaStrings().isLines("foo","bar","bar")).asMessage().asOneLine().is("String differed at position 10.  Expect='foo\\nbar\\nbar'.  Actual='foo\\nbar\\nbaz'.");
	}

	@Test
	public void cb04_isSortedLines() throws Exception {
		String x1 = "foo\nbar\nbaz", x2 = "foo", empty = "", nil = null;
		test(x1).isSortedLines("bar","foo","baz");
		assertThrown(()->test(nil).isSortedLines((String[])null)).asMessage().is("Argument 'lines' cannot be null.");
		test(empty).isSortedLines((String)null);
		assertThrown(()->test(nil).isSortedLines()).asMessage().is("Value was null.");
		assertThrown(()->test(x1).isSortedLines("bar","foo","bar")).asMessage().asOneLine().is("Expected string had different values at line 2.  Expect='bar'.  Actual='baz'.");
		assertThrown(()->test(x1).isSortedLines("bar","foo")).asMessage().asOneLine().is("Expected string had different numbers of lines.  Expect='2'.  Actual='3'.");
		assertThrown(()->test(nil).isSortedLines("foo")).asMessage().is("Value was null.");
		assertThrown(()->test(x2).isSortedLines((String)null)).asMessage().asOneLine().is("Expected string had different values at line 1.  Expect=''.  Actual='foo'.");
	}

	@Test
	public void cb05_contains() throws Exception {
		String x = "foobar", nil = null;
		test(x).isContains("foo","bar");
		assertThrown(()->test(x).isContains("foo","baz")).asMessage().asOneLine().is("String did not contain expected substring.  Substring='baz'.  Value='foobar'.");
		test(nil).isContains();
		assertThrown(()->test(x).isContains((String[])null)).asMessage().is("Argument 'values' cannot be null.");
		test(x).isContains((String)null);
		assertThrown(()->test(nil).isContains("foobar")).asMessage().asOneLine().is("String did not contain expected substring.  Substring='foobar'.  Value='null'.");
	}

	@Test
	public void cb06_doesNotContain() throws Exception {
		String x = "foobar", nil = null;
		test(x).isNotContains("baz","qux");
		assertThrown(()->test(x).isNotContains("foo","baz")).asMessage().asOneLine().is("String contained unexpected substring.  Substring='foo'.  Value='foobar'.");
		test(nil).isNotContains();
		assertThrown(()->test(x).isNotContains((String[])null)).asMessage().is("Argument 'values' cannot be null.");
		test(x).isNotContains((String)null);
		test(nil).isNotContains("foobar");
	}

	@Test
	public void cb07_isEmpty() throws Exception {
		String x = "1", empty = "", nil = null;
		test(empty).isEmpty();
		test(nil).isEmpty();
		assertThrown(()->test(x).isEmpty()).asMessage().asOneLine().is("String was not empty.  Value='1'.");
	}

	@Test
	public void cb08_isNotEmpty() throws Exception {
		String x = "1", empty = "", nil = null;
		test(x).isNotEmpty();
		assertThrown(()->test(empty).isNotEmpty()).asMessage().is("String was empty.");
		assertThrown(()->test(nil).isNotEmpty()).asMessage().is("String was null.");
	}

	@Test
	public void cb09_matches() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).isMatches("fo*");
		assertThrown(()->test(x).isMatches("b*")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='\\Qb\\E.*\\Q\\E'.  Value='foo'.");
		assertThrown(()->test(nil).isMatches("b*")).asMessage().is("Value was null.");
		assertThrown(()->test(empty).isMatches(null)).asMessage().is("Argument 'searchPattern' cannot be null.");
	}

	@Test
	public void cb10_regex() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).isPattern("fo+");
		assertThrown(()->test(x).isPattern("bar")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='bar'.  Value='foo'.");
		assertThrown(()->test(nil).isPattern("fo+")).asMessage().is("Value was null.");
		assertThrown(()->test(empty).isPattern((String)null)).asMessage().is("Argument 'regex' cannot be null.");
	}

	@Test
	public void cb10b_regex_wFlags() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).isPattern("FO+", Pattern.CASE_INSENSITIVE);
		assertThrown(()->test(x).isPattern("bar")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='bar'.  Value='foo'.");
		assertThrown(()->test(nil).isPattern("fo+")).asMessage().is("Value was null.");
		assertThrown(()->test(empty).isPattern((String)null)).asMessage().is("Argument 'regex' cannot be null.");
	}

	@Test
	public void cb10c_regex_wPattern() throws Exception {
		String x = "foo", empty = "", nil = null;
		test(x).isPattern(Pattern.compile("FO+", Pattern.CASE_INSENSITIVE));
		assertThrown(()->test(x).isPattern("bar")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='bar'.  Value='foo'.");
		assertThrown(()->test(nil).isPattern("fo+")).asMessage().is("Value was null.");
		assertThrown(()->test(empty).isPattern((String)null)).asMessage().is("Argument 'regex' cannot be null.");
	}

	@Test
	public void cb12_startsWith() throws Exception {
		String x = "foo", nil = null;
		test(x).isStartsWith("fo");
		assertThrown(()->test(x).isStartsWith("x")).asMessage().asOneLine().is("String did not start with expected substring.  Substring='x'.  Value='foo'.");
		assertThrown(()->test(nil).isStartsWith("x")).asMessage().is("Value was null.");
	}

	@Test
	public void cb13_endsWith() throws Exception {
		String x = "foo", nil = null;
		test(x).isEndsWith("oo");
		assertThrown(()->test(x).isEndsWith("x")).asMessage().asOneLine().is("String did not end with expected substring.  Substring='x'.  Value='foo'.");
		assertThrown(()->test(nil).isEndsWith("x")).asMessage().is("Value was null.");
	}
}
