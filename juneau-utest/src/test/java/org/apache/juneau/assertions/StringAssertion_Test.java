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
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import static java.util.Optional.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringAssertion_Test {

	private StringAssertion test(Object value) {
		return assertString(value).silent();
	}

	private StringAssertion test(Optional<Object> value) {
		return assertString(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		test((String)null).isNull();
		test(empty()).isNull();
		test(1).exists();

		test("foobar").replaceAll("fo+","bar").is("barbar").is("foobar");
		test(of("foobar")).replaceAll("fo+","bar").is("barbar").is("foobar");
		test(empty()).replaceAll("fo+","bar").isNull();
		assertThrown(()->test("foobar").replaceAll(null,"")).message().is("Argument 'regex' cannot be null.");
		assertThrown(()->test("foobar").replaceAll("",null)).message().is("Argument 'replacement' cannot be null.");

		test("foobar").replace("foo","bar").is("barbar").is("foobar");
		test(empty()).replace("foo","bar").isNull();
		assertThrown(()->test("").replace(null,"bar").isNull()).message().is("Argument 'target' cannot be null.");
		assertThrown(()->test("").replace("foo",null).isNull()).message().is("Argument 'replacement' cannot be null.");

		test("foo%20bar").urlDecode().is("foo bar").is("foo%20bar");
		test(empty()).urlDecode().isNull();

		test("foo\nbar\nbaz").sort().is("bar\nbaz\nfoo").is("foo\nbar\nbaz");
		test(empty()).sort().isNull();

		test("FOOBAR").lc().is("foobar").is("FOOBAR");
		test(empty()).lc().isNull();

		test("foobar").uc().is("FOOBAR").is("foobar");
		test(empty()).uc().isNull();

		test("foo\nbar\nbaz").isEqualLinesTo("foo","bar","baz");
		assertThrown(()->test(empty()).isEqualLinesTo((String[])null)).message().is("Argument 'lines' cannot be null.");
		assertThrown(()->test(empty()).isEqualLinesTo((String)null)).message().is("Value was null.");
		assertThrown(()->test("foo\nbar\nbaz").javaStrings().isEqualLinesTo("foo","bar","bar")).message().is("String differed at position 10.\n\tExpect='foo\\nbar\\nbar'.\n\tActual='foo\\nbar\\nbaz'.");

		test("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo","baz");
		assertThrown(()->test(empty()).isEqualSortedLinesTo((String[])null)).message().is("Argument 'lines' cannot be null.");
		test("").isEqualSortedLinesTo((String)null);
		assertThrown(()->test(empty()).isEqualSortedLinesTo()).message().is("Value was null.");
		assertThrown(()->test("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo","bar")).message().is("Expected string had different values at line 2.\n\tExpect='bar'.\n\tActual='baz'.");
		assertThrown(()->test("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo")).message().is("Expected string had different numbers of lines.\n\tExpect='2'.\n\tActual='3'.");
		assertThrown(()->test(empty()).isEqualSortedLinesTo("foo")).message().is("Value was null.");
		assertThrown(()->test("foo").isEqualSortedLinesTo((String)null)).message().is("Expected string had different values at line 1.\n\tExpect=''.\n\tActual='foo'.");

		test("foo\nbar\nbaz").isEqualLinesTo("foo","bar","baz");

		test("foobar").isEqualIgnoreCaseTo("FOOBAR");
		test(empty()).isEqualIgnoreCaseTo(null);
		assertThrown(()->test("foobar").isEqualIgnoreCaseTo("FOOBAZ")).message().is("String differed at position 5.\n\tExpect='FOOBAZ'.\n\tActual='foobar'.");
		assertThrown(()->test(empty()).isEqualIgnoreCaseTo("FOOBAZ")).message().is("String differed at position 0.\n\tExpect='FOOBAZ'.\n\tActual='null'.");
		assertThrown(()->test("foobar").isEqualIgnoreCaseTo(null)).message().is("String differed at position 0.\n\tExpect='null'.\n\tActual='foobar'.");

		test("foobar").doesNotEqual("foobaz");
		assertThrown(()->test("foobar").doesNotEqual("foobar")).message().is("String equaled unexpected.\n\tValue='foobar'.");

		assertThrown(()->test("foobar").isEqualTo("foobaz")).message().is("String differed at position 5.\n\tExpect='foobaz'.\n\tActual='foobar'.");

		test("foobar").isNot("foobaz");
		assertThrown(()->test("foobar").isNot("foobar")).message().is("String equaled unexpected.\n\tValue='foobar'.");
		assertThrown(()->test(empty()).isNot(null)).message().is("String equaled unexpected.\n\tValue='null'.");
		test("foobar").isNot(null);
		test(empty()).isNot("foobar");

		test("foobar").doesNotEqualIc("foobaz");
		assertThrown(()->test("foobar").doesNotEqualIc("Foobar")).message().is("String equaled unexpected.\n\tValue='foobar'.");
		assertThrown(()->test(empty()).doesNotEqualIc(null)).message().is("String equaled unexpected.\n\tValue='null'.");
		test("foobar").doesNotEqualIc(null);
		test(empty()).doesNotEqualIc("foobar");

		test("foobar").contains("foo","bar");
		assertThrown(()->test("foobar").contains("foo","baz")).message().is("String did not contain expected substring.\n\tSubstring='baz'.\n\tValue='foobar'.");
		test(empty()).contains();
		assertThrown(()->test("foobar").contains((String[])null)).message().is("Argument 'values' cannot be null.");
		test("foobar").contains((String)null);
		assertThrown(()->test(empty()).contains("foobar")).message().is("String did not contain expected substring.\n\tSubstring='foobar'.\n\tValue='null'.");

		test("foobar").doesNotContain("baz","qux");
		assertThrown(()->test("foobar").doesNotContain("foo","baz")).message().is("String contained unexpected substring.\n\tSubstring='foo'.\n\tValue='foobar'.");
		test(empty()).doesNotContain();
		assertThrown(()->test("foobar").doesNotContain((String[])null)).message().is("Argument 'values' cannot be null.");
		test("foobar").doesNotContain((String)null);
		test(empty()).doesNotContain("foobar");

		test("").isEmpty();
		test(empty()).isEmpty();
		assertThrown(()->test("foo").isEmpty()).message().is("String was not empty.\n\tValue='foo'.");

		test("foo").isNotEmpty();
		assertThrown(()->test("").isNotEmpty()).message().is("String was empty.");
		assertThrown(()->test(empty()).isNotEmpty()).message().is("String was null.");

		test("foo").matches("fo+");
		assertThrown(()->test("foo").matches("bar")).message().is("String did not match expected pattern.\n\tPattern='bar'.\n\tValue='foo'.");
		assertThrown(()->test(empty()).matches("fo+")).message().is("Value was null.");
		assertThrown(()->test("").matches((String)null)).message().is("Argument 'regex' cannot be null.");

		test("foo").matchesSimple("fo*");
		assertThrown(()->test("foo").matchesSimple("b*")).message().is("String did not match expected pattern.\n\tPattern='\\Qb\\E.*\\Q\\E'.\n\tValue='foo'.");
		assertThrown(()->test(empty()).matchesSimple("b*")).message().is("Value was null.");
		assertThrown(()->test("").matchesSimple(null)).message().is("Argument 'searchPattern' cannot be null.");

		test("foo").doesNotMatch("b.*");
		assertThrown(()->test("foo").doesNotMatch("fo+")).message().is("String matched unexpected pattern.\n\tPattern='fo+'.\n\tValue='foo'.");
		test(empty()).doesNotMatch("fo+");
		assertThrown(()->test("").doesNotMatch((String)null)).message().is("Argument 'regex' cannot be null.");

		test("foo").startsWith("fo");
		assertThrown(()->test("foo").startsWith("x")).message().is("String did not start with expected substring.\n\tSubstring='x'.\n\tValue='foo'.");

		test("foo").endsWith("oo");
		assertThrown(()->test("foo").endsWith("x")).message().is("String did not end with expected substring.\n\tSubstring='x'.\n\tValue='foo'.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((String)null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((String)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((String)null).stdout().javaStrings();
	}
}
