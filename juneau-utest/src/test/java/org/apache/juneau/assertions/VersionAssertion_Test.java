/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.AssertionPredicates.ne;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.commons.lang.Version.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

class VersionAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private static VersionAssertion test(Version value) {
		return assertVersion(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_msg() {
		var assertion1 = test(null).setMsg("Foo {0}", 1);
		assertThrows(BasicAssertionError.class, assertion1::isExists, "Foo 1");
		var assertion2 = test(null).setMsg("Foo {0}", 1).setThrowable(RuntimeException.class);
		assertThrows(RuntimeException.class, assertion2::isExists, "Foo 1");
	}

	@Test void a02_stdout() {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ba01a_asString() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).asString().is("1");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = of("1");
		var nil = no(Version.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).is("{maintenance:null,major:1,minor:null}");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = of("1");
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).asJson().is("{maintenance:null,major:1,minor:null}");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).asJsonSorted().is("{maintenance:null,major:1,minor:null}");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = of("1");
		var x2 = of("2");
		test(x1).asTransformed(x -> x2).is(x2);
	}

	@Test void bc01_part() {
		var x = of("1.2.3");
		var nil = no(Version.class);
		test(x).asPart(-1).isNull();
		test(x).asPart(0).is(1);
		test(x).asPart(1).is(2);
		test(x).asPart(2).is(3);
		test(x).asPart(3).isNull();
		test(nil).asPart(0).isNull();
	}

	@Test void bc02_major() {
		var x = of("1.2.3");
		var nil = no(Version.class);
		test(x).asMajor().is(1);
		test(nil).asMajor().isNull();
	}

	@Test void bc03_minor() {
		var x = of("1.2.3");
		var nil = no(Version.class);
		test(x).asMinor().is(2);
		test(nil).asMinor().isNull();
	}

	@Test void bc04_maintenance() {
		var x = of("1.2.3");
		var nil = no(Version.class);
		test(x).asMaintenance().is(3);
		test(nil).asMaintenance().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).isExists().isExists();
		var assertion3 = test(nil);
		assertThrows(BasicAssertionError.class, assertion3::isExists, "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = of("1");
		var nil = no(Version.class);
		test(nil).isNull();
		var assertion4 = test(x);
		assertThrows(BasicAssertionError.class, assertion4::isNull, "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).isNotNull();
		var assertion5 = test(nil);
		assertThrows(BasicAssertionError.class, assertion5::isNotNull, "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='2'.  Actual='1'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().is("Unexpected value.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().is("Unexpected value.  Expect='2'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = of("1");
		test(x1).is(x->x.getMajor().orElse(2) == 1);
		assertThrown(()->test(x1).is(x->x.getMajor().orElse(2)==2)).asMessage().asOneLine().is("Unexpected value: '1'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='1'.");
	}

	@Test void ca05_isNot() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().is("Unexpected value.  Did not expect='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='1'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().is("Expected value not found.  Expect='[]'.  Actual='1'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().is("Expected value not found.  Expect='[2]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='1'.  Actual='1'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = of("1");
		var x1a = of("1");
		var nil = no(Version.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(Version@*)'.  Actual='1(Version@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='1(Version@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='1(Version@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = of("1");
		var x1a = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='{maintenance:null,major:2,minor:null}'.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().is("Unexpected comparison.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
	}

	@Test void ca12_isType() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).isType(Version.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.commons.lang.Version'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).isExactType(Version.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='org.apache.juneau.commons.lang.Version'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='org.apache.juneau.commons.lang.Version'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).isString("1");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='1'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='1'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = of("1");
		var nil = no(Version.class);
		test(x).isJson("{maintenance:null,major:1,minor:null}");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().is("String differed at position 0.  Expect='null'.  Actual='{maintenance:null,major:1,minor:null}'.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_isGt() {
		var x1 = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x2).isGt(x1);
		assertThrown(()->test(x1).isGt(x1)).asMessage().asOneLine().is("Value was not greater than expected.  Expect='1'.  Actual='1'.");
		assertThrown(()->test(x1).isGt(x2)).asMessage().asOneLine().is("Value was not greater than expected.  Expect='2'.  Actual='1'.");
		var assertion6 = test(x1);
		assertThrows(IllegalArgumentException.class, ()->assertion6.isGt(nil), "Argument 'value' cannot be null.");
		var assertion7 = test(nil);
		assertThrows(BasicAssertionError.class, ()->assertion7.isGt(x2), "Value was null.");
	}

	@Test void cb02_isGte() {
		var x1 = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x2).isGte(x1);
		test(x1).isGte(x1);
		assertThrown(()->test(x1).isGte(x2)).asMessage().asOneLine().is("Value was not greater than or equals to expected.  Expect='2'.  Actual='1'.");
		var assertion8 = test(x1);
		assertThrows(IllegalArgumentException.class, ()->assertion8.isGte(nil), "Argument 'value' cannot be null.");
		var assertion9 = test(nil);
		assertThrows(BasicAssertionError.class, ()->assertion9.isGte(x2), "Value was null.");
	}

	@Test void cb03_isLt() {
		var x1 = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isLt(x2);
		assertThrown(()->test(x1).isLt(x1)).asMessage().asOneLine().is("Value was not less than expected.  Expect='1'.  Actual='1'.");
		assertThrown(()->test(x2).isLt(x1)).asMessage().asOneLine().is("Value was not less than expected.  Expect='1'.  Actual='2'.");
		var assertion10 = test(x2);
		assertThrows(IllegalArgumentException.class, ()->assertion10.isLt(nil), "Argument 'value' cannot be null.");
		var assertion11 = test(nil);
		assertThrows(BasicAssertionError.class, ()->assertion11.isLt(x1), "Value was null.");
	}

	@Test void cb04_isLte() {
		var x1 = of("1");
		var x2 = of("2");
		var nil = no(Version.class);
		test(x1).isLte(x2);
		test(x1).isLte(x1);
		assertThrown(()->test(x2).isLte(x1)).asMessage().asOneLine().is("Value was not less than or equals to expected.  Expect='1'.  Actual='2'.");
		var assertion12 = test(x2);
		assertThrows(IllegalArgumentException.class, ()->assertion12.isLte(nil), "Argument 'value' cannot be null.");
		var assertion13 = test(nil);
		assertThrows(BasicAssertionError.class, ()->assertion13.isLte(x1), "Value was null.");
	}

	@Test void cb05_isBetween() {
		var x1 = of("1");
		var x2 = of("2");
		var x3 = of("3");
		var x4 = of("4");
		var nil = no(Version.class);
		test(x1).isBetween(x1, x3);
		test(x2).isBetween(x1, x3);
		test(x3).isBetween(x1, x3);
		assertThrown(()->test(x4).isBetween(x1, x3)).asMessage().asOneLine().is("Value was not less than or equals to expected.  Expect='3'.  Actual='4'.");
		var assertion14 = test(nil);
		assertThrows(BasicAssertionError.class, ()->assertion14.isBetween(x1, x3), "Value was null.");
		var assertion15 = test(x1);
		assertThrows(IllegalArgumentException.class, ()->assertion15.isBetween(nil, x3), "Argument 'lower' cannot be null.");
		assertThrown(()->test(x1).isBetween(x1, nil)).asMessage().asOneLine().is("Argument 'upper' cannot be null.");
	}
}