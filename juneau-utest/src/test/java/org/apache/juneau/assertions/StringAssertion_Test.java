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
		test((String)null).doesNotExist();
		test(empty()).doesNotExist();
		test(1).exists();

		test("foobar").replaceAll("fo+","bar").is("barbar").is("foobar");
		test(of("foobar")).replaceAll("fo+","bar").is("barbar").is("foobar");
		test(empty()).replaceAll("fo+","bar").isNull();
		assertThrown(()->test("foobar").replaceAll(null,"")).is("Parameter 'regex' cannot be null.");
		assertThrown(()->test("foobar").replaceAll("",null)).is("Parameter 'replacement' cannot be null.");

		test("foobar").replace("foo","bar").is("barbar").is("foobar");
		test(empty()).replace("foo","bar").isNull();
		assertThrown(()->test("").replace(null,"bar").isNull()).is("Parameter 'target' cannot be null.");
		assertThrown(()->test("").replace("foo",null).isNull()).is("Parameter 'replacement' cannot be null.");

		test("foo%20bar").urlDecode().is("foo bar").is("foo%20bar");
		test(empty()).urlDecode().isNull();

		test("foo\nbar\nbaz").sort().is("bar\nbaz\nfoo").is("foo\nbar\nbaz");
		test(empty()).sort().doesNotExist();

		test("FOOBAR").lc().is("foobar").is("FOOBAR");
		test(empty()).lc().isNull();

		test("foobar").uc().is("FOOBAR").is("foobar");
		test(empty()).uc().isNull();

		test("foo\nbar\nbaz").isEqualLinesTo("foo","bar","baz");
		assertThrown(()->test(empty()).isEqualLinesTo((String[])null)).is("Parameter 'lines' cannot be null.");
		assertThrown(()->test(empty()).isEqualLinesTo((String)null)).is("Text differed at position -1.\n\tExpect=[]\n\tActual=[null]");
		assertThrown(()->test("foo\nbar\nbaz").javaStrings().isEqualLinesTo("foo","bar","bar")).is("Text differed at position 10.\n\tExpect=[foo\\nbar\\nbar]\n\tActual=[foo\\nbar\\nbaz]");

		test("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo","baz");
		assertThrown(()->test(empty()).isEqualSortedLinesTo((String[])null)).is("Parameter 'lines' cannot be null.");
		test("").isEqualSortedLinesTo((String)null);
		assertThrown(()->test(empty()).isEqualSortedLinesTo()).is("Value was null.");
		assertThrown(()->test("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo","bar")).is("Expected text had different values at line 2.\n\tExpect=[bar]\n\tActual=[baz]");
		assertThrown(()->test("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo")).is("Expected text had different numbers of lines.\n\tExpect=[2]\n\tActual=[3]");
		assertThrown(()->test(empty()).isEqualSortedLinesTo("foo")).is("Value was null.");
		assertThrown(()->test("foo").isEqualSortedLinesTo((String)null)).is("Expected text had different values at line 1.\n\tExpect=[]\n\tActual=[foo]");

		test("foo\nbar\nbaz").isEqualLinesTo("foo","bar","baz");

		test("foobar").isEqualIgnoreCaseTo("FOOBAR");
		test(empty()).isEqualIgnoreCaseTo(null);
		assertThrown(()->test("foobar").isEqualIgnoreCaseTo("FOOBAZ")).is("Text differed at position 5.\n\tExpect=[FOOBAZ]\n\tActual=[foobar]");
		assertThrown(()->test(empty()).isEqualIgnoreCaseTo("FOOBAZ")).is("Text differed at position 0.\n\tExpect=[FOOBAZ]\n\tActual=[null]");
		assertThrown(()->test("foobar").isEqualIgnoreCaseTo(null)).is("Text differed at position 0.\n\tExpect=[null]\n\tActual=[foobar]");

		test("foobar").doesNotEqual("foobaz");
		assertThrown(()->test("foobar").doesNotEqual("foobar")).is("Text equaled unexpected.\n\tText=[foobar]");

		assertThrown(()->test("foobar").isEqualTo("foobaz")).is("Text differed at position 5.\n\tExpect=[foobaz]\n\tActual=[foobar]");

		test("foobar").isNot("foobaz");
		assertThrown(()->test("foobar").isNot("foobar")).is("Text equaled unexpected.\n\tText=[foobar]");
		assertThrown(()->test(empty()).isNot(null)).is("Text equaled unexpected.\n\tText=[null]");
		test("foobar").isNot(null);
		test(empty()).isNot("foobar");

		test("foobar").doesNotEqualIc("foobaz");
		assertThrown(()->test("foobar").doesNotEqualIc("Foobar")).is("Text equaled unexpected.\n\tText=[foobar]");
		assertThrown(()->test(empty()).doesNotEqualIc(null)).is("Text equaled unexpected.\n\tText=[null]");
		test("foobar").doesNotEqualIc(null);
		test(empty()).doesNotEqualIc("foobar");

		test("foobar").contains("foo","bar");
		assertThrown(()->test("foobar").contains("foo","baz")).is("Text did not contain expected substring.\n\tSubstring=[baz]\n\tText=[foobar]");
		test(empty()).contains();
		assertThrown(()->test("foobar").contains((String[])null)).is("Parameter 'values' cannot be null.");
		test("foobar").contains((String)null);
		assertThrown(()->test(empty()).contains("foobar")).is("Text did not contain expected substring.\n\tSubstring=[foobar]\n\tText=[null]");

		test("foobar").doesNotContain("baz","qux");
		assertThrown(()->test("foobar").doesNotContain("foo","baz")).is("Text contained unexpected substring.\n\tSubstring=[foo]\n\tText=[foobar]");
		test(empty()).doesNotContain();
		assertThrown(()->test("foobar").doesNotContain((String[])null)).is("Parameter 'values' cannot be null.");
		test("foobar").doesNotContain((String)null);
		test(empty()).doesNotContain("foobar");

		test("").isEmpty();
		test(empty()).isEmpty();
		assertThrown(()->test("foo").isEmpty()).is("Text was not empty.\n\tText=[foo]");

		test("foo").isNotEmpty();
		assertThrown(()->test("").isNotEmpty()).is("Text was empty.");
		assertThrown(()->test(empty()).isNotEmpty()).is("Text was null.");

		test("foo").matches("fo+");
		assertThrown(()->test("foo").matches("bar")).is("Text did not match expected pattern.\n\tPattern=[bar]\n\tText=[foo]");
		assertThrown(()->test(empty()).matches("fo+")).is("Value was null.");
		assertThrown(()->test("").matches((String)null)).is("Parameter 'regex' cannot be null.");

		test("foo").matchesSimple("fo*");
		assertThrown(()->test("foo").matchesSimple("b*")).is("Text did not match expected pattern.\n\tPattern=[\\Qb\\E.*\\Q\\E]\n\tText=[foo]");
		assertThrown(()->test(empty()).matchesSimple("b*")).is("Value was null.");
		assertThrown(()->test("").matchesSimple(null)).is("Parameter 'searchPattern' cannot be null.");

		test("foo").doesNotMatch("b.*");
		assertThrown(()->test("foo").doesNotMatch("fo+")).is("Text matched unexpected pattern.\n\tPattern=[fo+]\n\tText=[foo]");
		test(empty()).doesNotMatch("fo+");
		assertThrown(()->test("").doesNotMatch((String)null)).is("Parameter 'regex' cannot be null.");

		test("foo").startsWith("fo");
		assertThrown(()->test("foo").startsWith("x")).is("Text did not start with expected string.\n\tString=[x]\n\tText=[foo]");

		test("foo").endsWith("oo");
		assertThrown(()->test("foo").endsWith("x")).is("Text did not end with expected string.\n\tString=[x]\n\tText=[foo]");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((String)null).msg("Foo {0}", 1).exists()).is("Foo 1");
		test((String)null).stdout().javaStrings();
	}
}
