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

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringAssertion_Test {

	@Test
	public void a01_basic() throws Exception {
		assertString(null).doesNotExist();
		assertString(1).exists();

		assertString("foobar").replaceAll("fo+","bar").is("barbar").is("foobar");
		assertString(null).replaceAll("fo+","bar").isNull();
		assertThrown(()->assertString("foobar").replaceAll(null,"")).is("Parameter 'regex' cannot be null.");
		assertThrown(()->assertString("foobar").replaceAll("",null)).is("Parameter 'replacement' cannot be null.");

		assertString("foobar").replace("foo","bar").is("barbar").is("foobar");
		assertString(null).replace("foo","bar").isNull();
		assertThrown(()->assertString("").replace(null,"bar").isNull()).is("Parameter 'target' cannot be null.");
		assertThrown(()->assertString("").replace("foo",null).isNull()).is("Parameter 'replacement' cannot be null.");

		assertString("foo%20bar").urlDecode().is("foo bar").is("foo%20bar");
		assertString(null).urlDecode().isNull();

		assertString("foo\nbar\nbaz").sort().is("bar\nbaz\nfoo").is("foo\nbar\nbaz");
		assertString(null).sort().doesNotExist();

		assertString("FOOBAR").lc().is("foobar").is("FOOBAR");
		assertString(null).lc().isNull();

		assertString("foobar").uc().is("FOOBAR").is("foobar");
		assertString(null).uc().isNull();

		assertString("foo\nbar\nbaz").isEqualLines("foo","bar","baz");
		assertThrown(()->assertString(null).isEqualLines((String[])null)).is("Parameter 'lines' cannot be null.");
		assertThrown(()->assertString(null).isEqualLines((String)null)).is("Text differed at position -1.\n\tExpect=[]\n\tActual=[null]");
		assertThrown(()->assertString("foo\nbar\nbaz").javaStrings().isEqualLines("foo","bar","bar")).stderr().is("Text differed at position 10.\n\tExpect=[foo\\nbar\\nbar]\n\tActual=[foo\\nbar\\nbaz]");

		assertString("foo\nbar\nbaz").isEqualSortedLines("bar","foo","baz");
		assertThrown(()->assertString(null).isEqualSortedLines((String[])null)).is("Parameter 'lines' cannot be null.");
		assertString("").isEqualSortedLines((String)null);
		assertThrown(()->assertString(null).isEqualSortedLines()).is("Value was null.");
		assertThrown(()->assertString("foo\nbar\nbaz").isEqualSortedLines("bar","foo","bar")).stderr().is("Expected text had different values at line 2.\n\tExpect=[bar]\n\tActual=[baz]");
		assertThrown(()->assertString("foo\nbar\nbaz").isEqualSortedLines("bar","foo")).stderr().is("Expected text had different numbers of lines.\n\tExpect=[2]\n\tActual=[3]");
		assertThrown(()->assertString(null).isEqualSortedLines("foo")).stderr().is("Value was null.");
		assertThrown(()->assertString("foo").isEqualSortedLines((String)null)).stderr().is("Expected text had different values at line 1.\n\tExpect=[]\n\tActual=[foo]");

		assertString("foo\nbar\nbaz").isEqualLines("foo","bar","baz");

		assertString("foobar").isEqualIc("FOOBAR");
		assertString(null).isEqualIc(null);
		assertThrown(()->assertString("foobar").isEqualIc("FOOBAZ")).stderr().is("Text differed at position 5.\n\tExpect=[FOOBAZ]\n\tActual=[foobar]");
		assertThrown(()->assertString(null).isEqualIc("FOOBAZ")).stderr().is("Text differed at position 0.\n\tExpect=[FOOBAZ]\n\tActual=[null]");
		assertThrown(()->assertString("foobar").isEqualIc(null)).stderr().is("Text differed at position 0.\n\tExpect=[null]\n\tActual=[foobar]");

		assertString("foobar").doesNotEqual("foobaz");
		assertThrown(()->assertString("foobar").doesNotEqual("foobar")).stderr().is("Text equaled unexpected.\n\tText=[foobar]");

		assertThrown(()->assertString("foobar").isEqual("foobaz")).stderr().is("Text differed at position 5.\n\tExpect=[foobaz]\n\tActual=[foobar]");

		assertString("foobar").isNot("foobaz");
		assertThrown(()->assertString("foobar").isNot("foobar")).is("Text equaled unexpected.\n\tText=[foobar]");
		assertThrown(()->assertString(null).isNot(null)).is("Text equaled unexpected.\n\tText=[null]");
		assertString("foobar").isNot(null);
		assertString(null).isNot("foobar");

		assertString("foobar").doesNotEqualIc("foobaz");
		assertThrown(()->assertString("foobar").doesNotEqualIc("Foobar")).is("Text equaled unexpected.\n\tText=[foobar]");
		assertThrown(()->assertString(null).doesNotEqualIc(null)).is("Text equaled unexpected.\n\tText=[null]");
		assertString("foobar").doesNotEqualIc(null);
		assertString(null).doesNotEqualIc("foobar");

		assertString("foobar").contains("foo","bar");
		assertThrown(()->assertString("foobar").contains("foo","baz")).is("Text did not contain expected substring.\n\tSubstring=[baz]\n\tText=[foobar]");
		assertString(null).contains();
		assertThrown(()->assertString("foobar").contains((String[])null)).is("Parameter 'values' cannot be null.");
		assertString("foobar").contains((String)null);
		assertThrown(()->assertString(null).contains("foobar")).is("Text did not contain expected substring.\n\tSubstring=[foobar]\n\tText=[null]");

		assertString("foobar").doesNotContain("baz","qux");
		assertThrown(()->assertString("foobar").doesNotContain("foo","baz")).is("Text contained unexpected substring.\n\tSubstring=[foo]\n\tText=[foobar]");
		assertString(null).doesNotContain();
		assertThrown(()->assertString("foobar").doesNotContain((String[])null)).is("Parameter 'values' cannot be null.");
		assertString("foobar").doesNotContain((String)null);
		assertString(null).doesNotContain("foobar");

		assertString("").isEmpty();
		assertString(null).isEmpty();
		assertThrown(()->assertString("foo").isEmpty()).is("Text was not empty.\n\tText=[foo]");

		assertString("foo").isNotEmpty();
		assertThrown(()->assertString("").isNotEmpty()).is("Text was empty.");
		assertThrown(()->assertString(null).isNotEmpty()).is("Text was null.");

		assertString("foo").matches("fo+");
		assertThrown(()->assertString("foo").matches("bar")).is("Text did not match expected pattern.\n\tPattern=[bar]\n\tText=[foo]");
		assertThrown(()->assertString(null).matches("fo+")).is("Value was null.");
		assertThrown(()->assertString("").matches((String)null)).is("Parameter 'regex' cannot be null.");

		assertString("foo").matchesSimple("fo*");
		assertThrown(()->assertString("foo").matchesSimple("b*")).stderr().is("Text did not match expected pattern.\n\tPattern=[\\Qb\\E.*\\Q\\E]\n\tText=[foo]");
		assertThrown(()->assertString(null).matchesSimple("b*")).is("Value was null.");
		assertThrown(()->assertString("").matchesSimple(null)).is("Parameter 'searchPattern' cannot be null.");

		assertString("foo").doesNotMatch("b.*");
		assertThrown(()->assertString("foo").doesNotMatch("fo+")).is("Text matched unexpected pattern.\n\tPattern=[fo+]\n\tText=[foo]");
		assertString(null).doesNotMatch("fo+");
		assertThrown(()->assertString("").doesNotMatch((String)null)).is("Parameter 'regex' cannot be null.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->StringAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		StringAssertion.create(null).stdout().stderr().javaStrings();
	}
}
