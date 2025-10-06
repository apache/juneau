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
import static org.apache.juneau.common.internal.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
class StringAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private StringAssertion test(Object value) {
		return assertString(value).setSilent();
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

	@Test void a03_javaStrings() {
		test(null).asJavaStrings();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ba01a_asString() {
		var x = "1";
		var nil = n(String.class);
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = "1";
		var nil = n(String.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("'1'");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = "1";
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = "1";
		var nil = n(String.class);
		test(x).asJson().is("'1'");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x = "1";
		var nil = n(String.class);
		test(x).asJsonSorted().is("'1'");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = "1";
		var x2 = "2";
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bb01_replaceAll() {
		var x = "foobar";
		var nil = n(String.class);
		test(x).asReplaceAll("fo+","bar").is("barbar").is("foobar");
		test(nil).asReplaceAll("fo+","bar").isNull();
		assertThrows(IllegalArgumentException.class, ()->test(x).asReplaceAll(null,""), "Argument 'regex' cannot be null.");
		assertThrows(IllegalArgumentException.class, ()->test(x).asReplaceAll("",null), "Argument 'replacement' cannot be null.");
	}

	@Test void bb02_replace() {
		var x = "foobar";
		var nil = n(String.class);
		test(x).asReplace("foo","bar").is("barbar").is("foobar");
		test(nil).asReplace("foo","bar").isNull();
		assertThrows(IllegalArgumentException.class, ()->test(x).asReplace(null,"bar").isNull(), "Argument 'target' cannot be null.");
		assertThrows(IllegalArgumentException.class, ()->test(x).asReplace("foo",null).isNull(), "Argument 'replacement' cannot be null.");
	}

	@Test void bb03_urlDecode() {
		var x = "foo%20bar";
		var nil = n(String.class);
		test(x).asUrlDecode().is("foo bar").is("foo%20bar");
		test(nil).asUrlDecode().isNull();
	}

	@Test void bb04_lc() {
		var x = "FOOBAR";
		var nil = n(String.class);
		test(x).asLc().is("foobar").is("FOOBAR");
		test(nil).asLc().isNull();
	}

	@Test void bb05_uc() {
		var x = "foobar";
		var nil = n(String.class);
		test(x).asUc().is("FOOBAR").is("foobar");
		test(nil).asUc().isNull();
	}

	@Test void bb06_lines() {
		var x = "foo\nbar";
		var nil = n(String.class);
		test(x).asLines().isHas("foo","bar");
		test(nil).asLines().isNull();
	}

	@Test void bb07_oneLine() {
		var x = "foo  bar";
		var nil = n(String.class);
		test(x).asOneLine().is("foo  bar");
		test(nil).asOneLine().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = "1";
		var nil = n(String.class);
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = "1";
		var nil = n(String.class);
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = "1";
		var nil = n(String.class);
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("String differed at position 0.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("String differed at position 0.  Expect='2'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = "1";
		test(x1).is(x->x.length()==1);
		assertThrown(()->test(x1).is(x->x.length()==2)).asMessage().asOneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='1'.");
	}

	@Test void ca05_isNot() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("String equaled unexpected.  Value='1'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("String equaled unexpected.  Value='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='1'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = new String("1");
		var x1a = new String("1");
		var nil = n(String.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(String@*)'.  Actual='1(String@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(String@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='1(String@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual=''1''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''1''.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual=''1''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''1''.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = "1";
		var x1a = "1";
		var x2 = "2";
		var nil = n(String.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual=''1''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect=''2''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual=''1''.");
	}

	@Test void ca12_isType() {
		var x = "1";
		var nil = n(String.class);
		test(x).isType(String.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(Integer.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->test(nil).isType(Integer.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = "1";
		var nil = n(String.class);
		test(x).isExactType(String.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.lang.String'.");
		assertThrown(()->test(x).isExactType(Integer.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->test(nil).isExactType(Integer.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = "1";
		var nil = n(String.class);

		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = "1";
		var nil = n(String.class);

		test(x).isJson("'1'");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual=''1''.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual=''1''.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_isIc() {
		var x = "foobar";
		var nil = n(String.class);

		test(x).isIc("FOOBAR");
		assertThrown(()->test(x).isIc("FOOBAZ")).asMessage().asOneLine().is("String differed at position 5.  Expect='FOOBAZ'.  Actual='foobar'.");
		assertThrown(()->test(nil).isIc("FOOBAZ")).asMessage().asOneLine().is("String differed at position 0.  Expect='FOOBAZ'.  Actual='null'.");
		assertThrown(()->test(x).isIc(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='foobar'.");
	}

	@Test void cb02_isNotIc() {
		var x = "foobar";
		var nil = n(String.class);

		test(x).isNotIc("foobaz");
		assertThrown(()->test(x).isNotIc("Foobar")).asMessage().asOneLine().is("String equaled unexpected.  Value='foobar'.");
		assertThrown(()->test(nil).isNotIc(null)).asMessage().asOneLine().is("String equaled unexpected.  Value='null'.");
		test(x).isNotIc(null);
		test(nil).isNotIc("foobar");
	}

	@Test void cb03_isLines() {
		var x = "foo\nbar\nbaz";
		var nil = n(String.class);

		test(x).isLines("foo","bar","baz");
		assertThrows(IllegalArgumentException.class, ()->test(nil).isLines((String[])null), "Argument 'lines' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isLines((String)null), "Value was null.");
		assertThrown(()->test(x).asJavaStrings().isLines("foo","bar","bar")).asMessage().asOneLine().is("String differed at position 10.  Expect='foo\\nbar\\nbar'.  Actual='foo\\nbar\\nbaz'.");
	}

	@Test void cb04_isSortedLines() {
		var x1 = "foo\nbar\nbaz";
		var x2 = "foo";
		var empty = "";
		var nil = n(String.class);

		test(x1).isSortedLines("bar","foo","baz");
		assertThrows(IllegalArgumentException.class, ()->test(nil).isSortedLines((String[])null), "Argument 'lines' cannot be null.");
		test(empty).isSortedLines((String)null);
		assertThrows(BasicAssertionError.class, ()->test(nil).isSortedLines(), "Value was null.");
		assertThrown(()->test(x1).isSortedLines("bar","foo","bar")).asMessage().asOneLine().is("Expected string had different values at line 2.  Expect='bar'.  Actual='baz'.");
		assertThrown(()->test(x1).isSortedLines("bar","foo")).asMessage().asOneLine().is("Expected string had different numbers of lines.  Expect='2'.  Actual='3'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isSortedLines("foo"), "Value was null.");
		assertThrown(()->test(x2).isSortedLines((String)null)).asMessage().asOneLine().is("Expected string had different values at line 1.  Expect=''.  Actual='foo'.");
	}

	@Test void cb05_contains() {
		var x = "foobar";
		var nil = n(String.class);
		test(x).isContains("foo","bar");
		assertThrown(()->test(x).isContains("foo","baz")).asMessage().asOneLine().is("String did not contain expected substring.  Substring='baz'.  Value='foobar'.");
		test(nil).isContains();
		assertThrows(IllegalArgumentException.class, ()->test(x).isContains((String[])null), "Argument 'values' cannot be null.");
		test(x).isContains((String)null);
		assertThrown(()->test(nil).isContains("foobar")).asMessage().asOneLine().is("String did not contain expected substring.  Substring='foobar'.  Value='null'.");
	}

	@Test void cb06_doesNotContain() {
		var x = "foobar";
		var nil = n(String.class);
		test(x).isNotContains("baz","qux");
		assertThrown(()->test(x).isNotContains("foo","baz")).asMessage().asOneLine().is("String contained unexpected substring.  Substring='foo'.  Value='foobar'.");
		test(nil).isNotContains();
		assertThrows(IllegalArgumentException.class, ()->test(x).isNotContains((String[])null), "Argument 'values' cannot be null.");
		test(x).isNotContains((String)null);
		test(nil).isNotContains("foobar");
	}

	@Test void cb07_isEmpty() {
		var x = "1";
		var empty = "";
		var nil = n(String.class);
		test(empty).isEmpty();
		test(nil).isEmpty();
		assertThrown(()->test(x).isEmpty()).asMessage().asOneLine().is("String was not empty.  Value='1'.");
	}

	@Test void cb08_isNotEmpty() {
		var x = "1";
		var empty = "";
		var nil = n(String.class);
		test(x).isNotEmpty();
		assertThrows(BasicAssertionError.class, ()->test(empty).isNotEmpty(), "String was empty.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotEmpty(), "String was null.");
	}

	@Test void cb09_matches() {
		var x = "foo";
		var empty = "";
		var nil = n(String.class);
		test(x).isMatches("fo*");
		assertThrown(()->test(x).isMatches("b*")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='\\Qb\\E.*\\Q\\E'.  Value='foo'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isMatches("b*"), "Value was null.");
		assertThrows(IllegalArgumentException.class, ()->test(empty).isMatches(null), "Argument 'searchPattern' cannot be null.");
	}

	@Test void cb10_regex() {
		var x = "foo";
		var empty = "";
		var nil = n(String.class);
		test(x).isPattern("fo+");
		assertThrown(()->test(x).isPattern("bar")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='bar'.  Value='foo'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isPattern("fo+"), "Value was null.");
		assertThrows(IllegalArgumentException.class, ()->test(empty).isPattern((String)null), "Argument 'regex' cannot be null.");
	}

	@Test void cb10b_regex_wFlags() {
		var x = "foo";
		var empty = "";
		var nil = n(String.class);
		test(x).isPattern("FO+", Pattern.CASE_INSENSITIVE);
		assertThrown(()->test(x).isPattern("bar")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='bar'.  Value='foo'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isPattern("fo+"), "Value was null.");
		assertThrows(IllegalArgumentException.class, ()->test(empty).isPattern((String)null), "Argument 'regex' cannot be null.");
	}

	@Test void cb10c_regex_wPattern() {
		var x = "foo";
		var empty = "";
		var nil = n(String.class);
		test(x).isPattern(Pattern.compile("FO+", Pattern.CASE_INSENSITIVE));
		assertThrown(()->test(x).isPattern("bar")).asMessage().asOneLine().is("String did not match expected pattern.  Pattern='bar'.  Value='foo'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isPattern("fo+"), "Value was null.");
		assertThrows(IllegalArgumentException.class, ()->test(empty).isPattern((String)null), "Argument 'regex' cannot be null.");
	}

	@Test void cb12_startsWith() {
		var x = "foo";
		var nil = n(String.class);
		test(x).isStartsWith("fo");
		assertThrown(()->test(x).isStartsWith("x")).asMessage().asOneLine().is("String did not start with expected substring.  Substring='x'.  Value='foo'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isStartsWith("x"), "Value was null.");
	}

	@Test void cb13_endsWith() {
		var x = "foo";
		var nil = n(String.class);
		test(x).isEndsWith("oo");
		assertThrown(()->test(x).isEndsWith("x")).asMessage().asOneLine().is("String did not end with expected substring.  Substring='x'.  Value='foo'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isEndsWith("x"), "Value was null.");
	}
}