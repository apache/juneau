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
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

@Deprecated
class DateAssertion_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private static DateAssertion test(Date value) {
		return assertDate(value).setSilent();
	}

	private static Date date(String s) {
		return new Date(ZonedDateTime.parse(s).toEpochSecond()*1000);
	}

	private static final Date
		MIN = date("1970-06-01T12:34:56Z"),
		MAX = date("2100-06-01T12:34:56Z"),
		MID1 = date("2000-06-01T12:34:56Z"),
		MID1a = date("2000-06-01T12:34:56Z"),
		MID2 = date("2010-06-01T12:34:56Z")
	;

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_msg() {
		assertThrows(BasicAssertionError.class, ()->test(null).setMsg("A {0}", 1).isExists(), "A 1");
		assertThrows(RuntimeException.class, ()->test(null).setMsg("A {0}", 1).setThrowable(RuntimeException.class).isExists(), "A 1");
	}

	@Test void a02_stdout() {
		test(null).setStdOut();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ba01a_asString() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).asString().isMatches("*2000");
		test(nil).asString().isNull();
	}

	@Test void ba01b_asString_wSerializer() {
		var x = MID1;
		var nil = n(Date.class);
		var s = Json5Serializer.DEFAULT;
		test(x).asString(s).isMatches("'2000-*'");
		test(nil).asString(s).is("null");
	}

	@Test void ba01c_asString_wPredicate() {
		var x1 = MID1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test void ba02_asJson() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).asJson().isMatches("'2000-*'");
		test(nil).asJson().is("null");
	}

	@Test void ba03_asJsonSorted() {
		var x1 = MID1;
		var nil = n(Date.class);
		test(x1).asJsonSorted().isMatches("'2000-*'");
		test(nil).asJsonSorted().is("null");
	}

	@Test void ba04_apply() {
		var x1 = MID1;
		var x2 = MID2;
		test(x1).asTransformed(x -> x2).is(MID2);
	}

	@Test void bc04a_asEpochMillis() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).asEpochMillis().is(959862896000L);
		test(nil).asEpochMillis().isNull();
	}

	@Test void bc04b_asEpochSeconds() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).asEpochSeconds().is(959862896L);
		test(nil).asEpochSeconds().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void ca01_exists() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).isExists().isExists();
		assertThrows(BasicAssertionError.class, ()->test(nil).isExists(), "Value was null.");
	}

	@Test void ca02_isNull() {
		var x = MID1;
		var nil = n(Date.class);
		test(nil).isNull();
		assertThrows(BasicAssertionError.class, ()->test(x).isNull(), "Value was not null.");
	}

	@Test void ca03_isNotNull() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).isNotNull();
		assertThrows(BasicAssertionError.class, ()->test(nil).isNotNull(), "Value was null.");
	}

	@Test void ca04a_is_T() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().isMatches("Unexpected value.  Expect='*2010'.  Actual='*2000'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().isMatches("Unexpected value.  Expect='null'.  Actual='*2000'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().isMatches("Unexpected value.  Expect='*2010'.  Actual='null'.");
	}

	@Test void ca04b_is_predicate() {
		var x1 = MID1;
		test(x1).is(x->x!=null);
		assertThrown(()->test(x1).is(x->x==null)).asMessage().asOneLine().isMatches("Unexpected value: '*2000*'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().isMatches("Value unexpectedly matched.  Value='*2000'.");
	}

	@Test void ca05_isNot() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().isMatches("Unexpected value.  Did not expect='*2000'.  Actual='*2000'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test void ca06_isAny() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().isMatches("Expected value not found.  Expect='[*2010]'.  Actual='*2000'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().isMatches("Expected value not found.  Expect='[]'.  Actual='*2000'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().isMatches("Expected value not found.  Expect='[*2010]'.  Actual='null'.");
	}

	@Test void ca07_isNotAny() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().isMatches("Unexpected value found.  Unexpected='*2000'.  Actual='*2000'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test void ca08_isSame() {
		var x1 = MID1;
		var x1a = MID1a;
		var nil = n(Date.class);
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='*2000(Date@*)'.  Actual='*2000(Date@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='*2000(Date@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='*2000(Date@*)'.");
	}

	@Test void ca09_isSameJsonAs() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual=''2000*''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect='null'.  Actual=''2000*''.");
	}

	@Test void ca10_isSameSortedJsonAs() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual=''2000*''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect='null'.  Actual=''2000*''.");
	}

	@Test void ca11_isSameSerializedAs() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MID2;
		var nil = n(Date.class);
		var s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual=''2000*''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect='null'.  Actual=''2000*''.");
	}

	@Test void ca12_isType() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).isType(Date.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.Date'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca13_isExactType() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).isExactType(Date.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.util.Date'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.Date'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test void ca14_isString() {
		var x = MID1;
		var nil = n(Date.class);
		test(x).isString(x.toString());
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='bad'.  Actual='*2000'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='null'.  Actual='*2000'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void ca15_isJson() {
		var x = MID1;
		var nil = n(Date.class);
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='bad'.  Actual=''2000*''.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='null'.  Actual=''2000*''.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test void cb01_isGt() {
		var x1 = MID1;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x2).isGt(x1);
		assertThrown(()->test(x1).isGt(x1)).asMessage().asOneLine().isMatches("Value was not greater than expected.  Expect='*2000'.  Actual='*2000'.");
		assertThrown(()->test(x1).isGt(x2)).asMessage().asOneLine().isMatches("Value was not greater than expected.  Expect='*2010'.  Actual='*2000'.");
		assertThrows(IllegalArgumentException.class, ()->test(x1).isGt(nil), "Argument 'value' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isGt(x2), "Value was null.");
	}

	@Test void cb02_isGte() {
		var x1 = MID1;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x2).isGte(x1);
		test(x1).isGte(x1);
		assertThrown(()->test(x1).isGte(x2)).asMessage().asOneLine().isMatches("Value was not greater than or equals to expected.  Expect='*2010'.  Actual='*2000'.");
		assertThrows(IllegalArgumentException.class, ()->test(x1).isGte(nil), "Argument 'value' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isGte(x2), "Value was null.");
	}

	@Test void cb03_isLt() {
		var x1 = MID1;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isLt(x2);
		assertThrown(()->test(x1).isLt(x1)).asMessage().asOneLine().isMatches("Value was not less than expected.  Expect='*2000'.  Actual='*2000'.");
		assertThrown(()->test(x2).isLt(x1)).asMessage().asOneLine().isMatches("Value was not less than expected.  Expect='*2000'.  Actual='*2010'.");
		assertThrows(IllegalArgumentException.class, ()->test(x2).isLt(nil), "Argument 'value' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isLt(x1), "Value was null.");
	}

	@Test void cb04_isLte() {
		var x1 = MID1;
		var x2 = MID2;
		var nil = n(Date.class);
		test(x1).isLte(x2);
		test(x1).isLte(x1);
		assertThrown(()->test(x2).isLte(x1)).asMessage().asOneLine().isMatches("Value was not less than or equals to expected.  Expect='*2000'.  Actual='*2010'.");
		assertThrows(IllegalArgumentException.class, ()->test(x2).isLte(nil), "Argument 'value' cannot be null.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isLte(x1), "Value was null.");
	}

	@Test void cb05_isBetween() {
		var x1 = MIN;
		var x2 = MID1;
		var x3 = MID2;
		var x4 = MAX;
		var nil = n(Date.class);
		test(x1).isBetween(x1, x3);
		test(x2).isBetween(x1, x3);
		test(x3).isBetween(x1, x3);
		assertThrown(()->test(x4).isBetween(x1, x3)).asMessage().asOneLine().isMatches("Value was not less than or equals to expected.  Expect='*2010'.  Actual='*2100'.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isBetween(x1, x3), "Value was null.");
		assertThrows(IllegalArgumentException.class, ()->test(x1).isBetween(nil, x3), "Argument 'lower' cannot be null.");
		assertThrown(()->test(x1).isBetween(x1, nil)).asMessage().asOneLine().is("Argument 'upper' cannot be null.");
	}

	@Test void cc01_is_wChrono() {
		var x1 = MID1;
		var x1a = MID1a;
		var x2 = MIN;
		var nil = n(Date.class);
		test(x1).is(x1, ChronoUnit.DAYS);
		test(x1).is(x1a, ChronoUnit.DAYS);
		assertThrown(()->test(nil).is(nil, ChronoUnit.DAYS)).asMessage().isContains("Value was null.");
		assertThrown(()->test(x1).is(x2, ChronoUnit.DAYS)).asMessage().isContains("Unexpected value.");
		assertThrown(()->test(x1).is(nil, ChronoUnit.DAYS)).asMessage().isContains("Unexpected value.");
	}

	@Test void cc02_isAfter() {
		var x1 = MIN;
		var x2 = MID1;
		var x3 = MAX;
		var nil = n(Date.class);
		test(x3).isAfter(x2);
		assertThrows(IllegalArgumentException.class, ()->test(x1).isAfter(nil), "Argument 'value' cannot be null.");
		assertThrown(()->test(x1).isAfter(x2)).asMessage().isContains("Value was not after expected.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isAfter(x2), "Value was null.");
	}

	@Test void cc03_isAfterNow() {
		var x1 = MIN;
		var x2 = MAX;
		var nil = n(Date.class);
		test(x2).isAfterNow();
		assertThrown(()->test(x1).isAfterNow()).asMessage().isContains("Value was not after expected.");
		assertThrown(()->test(nil).isAfterNow()).asMessage().isContains("Value was null.");
	}

	@Test void cc04_isBefore() {
		var x1 = MIN;
		var x2 = MID1;
		var nil = n(Date.class);
		test(x1).isBefore(x2);
		assertThrows(IllegalArgumentException.class, ()->test(x1).isBefore(nil), "Argument 'value' cannot be null.");
		assertThrown(()->test(x2).isBefore(x1)).asMessage().isContains("Value was not before expected.");
		assertThrows(BasicAssertionError.class, ()->test(nil).isBefore(x1), "Value was null.");
	}

	@Test void cc05_isBeforeNow() {
		var x1 = MIN;
		var x2 = MAX;
		var nil = n(Date.class);
		test(x1).isBeforeNow();
		assertThrown(()->test(x2).isBeforeNow()).asMessage().isContains("Value was not before expected.");
		assertThrown(()->test(nil).isBeforeNow()).asMessage().isContains("Value was null.");
	}
}