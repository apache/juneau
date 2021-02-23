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
import static java.util.Optional.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringAssertion_Test {

	@Test
	public void a01_basic() throws Exception {
		assertString((String)null).doesNotExist();
		assertString(empty()).doesNotExist();
		assertString(1).exists();

		assertString("foobar").replaceAll("fo+","bar").is("barbar").is("foobar");
		assertString(of("foobar")).replaceAll("fo+","bar").is("barbar").is("foobar");
		assertString(empty()).replaceAll("fo+","bar").isNull();
		assertThrown(()->assertString("foobar").replaceAll(null,"")).is("Parameter 'regex' cannot be null.");
		assertThrown(()->assertString("foobar").replaceAll("",null)).is("Parameter 'replacement' cannot be null.");

		assertString("foobar").replace("foo","bar").is("barbar").is("foobar");
		assertString(empty()).replace("foo","bar").isNull();
		assertThrown(()->assertString("").replace(null,"bar").isNull()).is("Parameter 'target' cannot be null.");
		assertThrown(()->assertString("").replace("foo",null).isNull()).is("Parameter 'replacement' cannot be null.");

		assertString("foo%20bar").urlDecode().is("foo bar").is("foo%20bar");
		assertString(empty()).urlDecode().isNull();

		assertString("foo\nbar\nbaz").sort().is("bar\nbaz\nfoo").is("foo\nbar\nbaz");
		assertString(empty()).sort().doesNotExist();

		assertString("FOOBAR").lc().is("foobar").is("FOOBAR");
		assertString(empty()).lc().isNull();

		assertString("foobar").uc().is("FOOBAR").is("foobar");
		assertString(empty()).uc().isNull();

		assertString("foo\nbar\nbaz").isEqualLinesTo("foo","bar","baz");
		assertThrown(()->assertString(empty()).isEqualLinesTo((String[])null)).is("Parameter 'lines' cannot be null.");
		assertThrown(()->assertString(empty()).isEqualLinesTo((String)null)).is("Text differed at position -1.\n\tExpect=[]\n\tActual=[null]");
		assertThrown(()->assertString("foo\nbar\nbaz").javaStrings().isEqualLinesTo("foo","bar","bar")).is("Text differed at position 10.\n\tExpect=[foo\\nbar\\nbar]\n\tActual=[foo\\nbar\\nbaz]");

		assertString("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo","baz");
		assertThrown(()->assertString(empty()).isEqualSortedLinesTo((String[])null)).is("Parameter 'lines' cannot be null.");
		assertString("").isEqualSortedLinesTo((String)null);
		assertThrown(()->assertString(empty()).isEqualSortedLinesTo()).is("Value was null.");
		assertThrown(()->assertString("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo","bar")).is("Expected text had different values at line 2.\n\tExpect=[bar]\n\tActual=[baz]");
		assertThrown(()->assertString("foo\nbar\nbaz").isEqualSortedLinesTo("bar","foo")).is("Expected text had different numbers of lines.\n\tExpect=[2]\n\tActual=[3]");
		assertThrown(()->assertString(empty()).isEqualSortedLinesTo("foo")).is("Value was null.");
		assertThrown(()->assertString("foo").isEqualSortedLinesTo((String)null)).is("Expected text had different values at line 1.\n\tExpect=[]\n\tActual=[foo]");

		assertString("foo\nbar\nbaz").isEqualLinesTo("foo","bar","baz");

		assertString("foobar").isEqualIgnoreCaseTo("FOOBAR");
		assertString(empty()).isEqualIgnoreCaseTo(null);
		assertThrown(()->assertString("foobar").isEqualIgnoreCaseTo("FOOBAZ")).is("Text differed at position 5.\n\tExpect=[FOOBAZ]\n\tActual=[foobar]");
		assertThrown(()->assertString(empty()).isEqualIgnoreCaseTo("FOOBAZ")).is("Text differed at position 0.\n\tExpect=[FOOBAZ]\n\tActual=[null]");
		assertThrown(()->assertString("foobar").isEqualIgnoreCaseTo(null)).is("Text differed at position 0.\n\tExpect=[null]\n\tActual=[foobar]");

		assertString("foobar").doesNotEqual("foobaz");
		assertThrown(()->assertString("foobar").doesNotEqual("foobar")).is("Text equaled unexpected.\n\tText=[foobar]");

		assertThrown(()->assertString("foobar").isEqualTo("foobaz")).is("Text differed at position 5.\n\tExpect=[foobaz]\n\tActual=[foobar]");

		assertString("foobar").isNot("foobaz");
		assertThrown(()->assertString("foobar").isNot("foobar")).is("Text equaled unexpected.\n\tText=[foobar]");
		assertThrown(()->assertString(empty()).isNot(null)).is("Text equaled unexpected.\n\tText=[null]");
		assertString("foobar").isNot(null);
		assertString(empty()).isNot("foobar");

		assertString("foobar").doesNotEqualIc("foobaz");
		assertThrown(()->assertString("foobar").doesNotEqualIc("Foobar")).is("Text equaled unexpected.\n\tText=[foobar]");
		assertThrown(()->assertString(empty()).doesNotEqualIc(null)).is("Text equaled unexpected.\n\tText=[null]");
		assertString("foobar").doesNotEqualIc(null);
		assertString(empty()).doesNotEqualIc("foobar");

		assertString("foobar").contains("foo","bar");
		assertThrown(()->assertString("foobar").contains("foo","baz")).is("Text did not contain expected substring.\n\tSubstring=[baz]\n\tText=[foobar]");
		assertString(empty()).contains();
		assertThrown(()->assertString("foobar").contains((String[])null)).is("Parameter 'values' cannot be null.");
		assertString("foobar").contains((String)null);
		assertThrown(()->assertString(empty()).contains("foobar")).is("Text did not contain expected substring.\n\tSubstring=[foobar]\n\tText=[null]");

		assertString("foobar").doesNotContain("baz","qux");
		assertThrown(()->assertString("foobar").doesNotContain("foo","baz")).is("Text contained unexpected substring.\n\tSubstring=[foo]\n\tText=[foobar]");
		assertString(empty()).doesNotContain();
		assertThrown(()->assertString("foobar").doesNotContain((String[])null)).is("Parameter 'values' cannot be null.");
		assertString("foobar").doesNotContain((String)null);
		assertString(empty()).doesNotContain("foobar");

		assertString("").isEmpty();
		assertString(empty()).isEmpty();
		assertThrown(()->assertString("foo").isEmpty()).is("Text was not empty.\n\tText=[foo]");

		assertString("foo").isNotEmpty();
		assertThrown(()->assertString("").isNotEmpty()).is("Text was empty.");
		assertThrown(()->assertString(empty()).isNotEmpty()).is("Text was null.");

		assertString("foo").matches("fo+");
		assertThrown(()->assertString("foo").matches("bar")).is("Text did not match expected pattern.\n\tPattern=[bar]\n\tText=[foo]");
		assertThrown(()->assertString(empty()).matches("fo+")).is("Value was null.");
		assertThrown(()->assertString("").matches((String)null)).is("Parameter 'regex' cannot be null.");

		assertString("foo").matchesSimple("fo*");
		assertThrown(()->assertString("foo").matchesSimple("b*")).is("Text did not match expected pattern.\n\tPattern=[\\Qb\\E.*\\Q\\E]\n\tText=[foo]");
		assertThrown(()->assertString(empty()).matchesSimple("b*")).is("Value was null.");
		assertThrown(()->assertString("").matchesSimple(null)).is("Parameter 'searchPattern' cannot be null.");

		assertString("foo").doesNotMatch("b.*");
		assertThrown(()->assertString("foo").doesNotMatch("fo+")).is("Text matched unexpected pattern.\n\tPattern=[fo+]\n\tText=[foo]");
		assertString(empty()).doesNotMatch("fo+");
		assertThrown(()->assertString("").doesNotMatch((String)null)).is("Parameter 'regex' cannot be null.");

		assertString("foo").startsWith("fo");
		assertThrown(()->assertString("foo").startsWith("x")).is("Text did not start with expected string.\n\tString=[x]\n\tText=[foo]");

		assertString("foo").endsWith("oo");
		assertThrown(()->assertString("foo").endsWith("x")).is("Text did not end with expected string.\n\tString=[x]\n\tText=[foo]");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->StringAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		StringAssertion.create(null).stdout().silent().javaStrings();
	}
}
