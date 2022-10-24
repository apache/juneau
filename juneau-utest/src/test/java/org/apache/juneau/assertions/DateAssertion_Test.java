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

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class DateAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private DateAssertion test(Date value) {
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

	@Test
	public void a01_msg() throws Exception {
		assertThrown(()->test(null).setMsg("A {0}", 1).isExists()).asMessage().is("A 1");
		assertThrown(()->test(null).setMsg("A {0}", 1).setThrowable(RuntimeException.class).isExists()).isExactType(RuntimeException.class).asMessage().is("A 1");
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
		Date x = MID1, nil = null;
		test(x).asString().isMatches("*2000");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		Date x = MID1, nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x).asString(s).isMatches("'2000-*'");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		Date x1 = MID1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		Date x = MID1, nil = null;
		test(x).asJson().isMatches("'2000-*'");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		Date x1 = MID1, nil = null;
		test(x1).asJsonSorted().isMatches("'2000-*'");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		Date x1 = MID1, x2 = MID2;
		test(x1).asTransformed(x -> x2).is(MID2);
	}

	@Test
	public void bc04a_asEpochMillis() throws Exception {
		Date x = MID1, nil = null;
		test(x).asEpochMillis().is(959862896000l);
		test(nil).asEpochMillis().isNull();
	}

	@Test
	public void bc04b_asEpochSeconds() throws Exception {
		Date x = MID1, nil = null;
		test(x).asEpochSeconds().is(959862896l);
		test(nil).asEpochSeconds().isNull();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		Date x = MID1, nil = null;
		test(x).isExists().isExists();
		assertThrown(()->test(nil).isExists()).asMessage().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		Date x = MID1, nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).asMessage().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		Date x = MID1, nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).asMessage().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).asMessage().asOneLine().isMatches("Unexpected value.  Expect='*2010'.  Actual='*2000'.");
		assertThrown(()->test(x1).is(nil)).asMessage().asOneLine().isMatches("Unexpected value.  Expect='null'.  Actual='*2000'.");
		assertThrown(()->test(nil).is(x2)).asMessage().asOneLine().isMatches("Unexpected value.  Expect='*2010'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		Date x1 = MID1;
		test(x1).is(x->x!=null);
		assertThrown(()->test(x1).is(x->x==null)).asMessage().asOneLine().isMatches("Unexpected value: '*2000'.");
		assertThrown(()->test(x1).is(ne(x1))).asMessage().asOneLine().isMatches("Value unexpectedly matched.  Value='*2000'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).asMessage().asOneLine().isMatches("Unexpected value.  Did not expect='*2000'.  Actual='*2000'.");
		assertThrown(()->test(nil).isNot(nil)).asMessage().asOneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).asMessage().asOneLine().isMatches("Expected value not found.  Expect='[*2010]'.  Actual='*2000'.");
		assertThrown(()->test(x1).isAny()).asMessage().asOneLine().isMatches("Expected value not found.  Expect='[]'.  Actual='*2000'.");
		assertThrown(()->test(nil).isAny(x2)).asMessage().asOneLine().isMatches("Expected value not found.  Expect='[*2010]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).asMessage().asOneLine().isMatches("Unexpected value found.  Unexpected='*2000'.  Actual='*2000'.");
		assertThrown(()->test(nil).isNotAny(nil)).asMessage().asOneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		Date x1 = MID1, x1a = MID1a, nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='*2000(Date@*)'.  Actual='*2000(Date@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).asMessage().asOneLine().isMatches("Not the same value.  Expect='*2000(Date@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).asMessage().asOneLine().isMatches("Not the same value.  Expect='null(null)'.  Actual='*2000(Date@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual=''2000*''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect='null'.  Actual=''2000*''.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual=''2000*''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect='null'.  Actual=''2000*''.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		WriterSerializer s = Json5Serializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual=''2000*''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect=''2010*''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).asMessage().asOneLine().isMatches("Unexpected comparison.  Expect='null'.  Actual=''2000*''.");
	}

	@Test
	public void ca12_isType() throws Exception {
		Date x = MID1, nil = null;
		test(x).isType(Date.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.Date'.");
		assertThrown(()->test(nil).isType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		Date x = MID1, nil = null;
		test(x).isExactType(Date.class);
		assertThrown(()->test(x).isExactType(Object.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.util.Date'.");
		assertThrown(()->test(x).isExactType(String.class)).asMessage().asOneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.util.Date'.");
		assertThrown(()->test(nil).isExactType(String.class)).asMessage().asOneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).asMessage().asOneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		Date x = MID1, nil = null;
		test(x).isString(x.toString());
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='bad'.  Actual='*2000'.");
		assertThrown(()->test(x).isString(null)).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='null'.  Actual='*2000'.");
		assertThrown(()->test(nil).isString("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		Date x = MID1, nil = null;
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='bad'.  Actual=''2000*''.");
		assertThrown(()->test(x).isJson(null)).asMessage().asOneLine().isMatches("String differed at position 0.  Expect='null'.  Actual=''2000*''.");
		assertThrown(()->test(nil).isJson("bad")).asMessage().asOneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isGt() throws Exception {
		Date x1 = MID1, x2 = MID2, nil = null;
		test(x2).isGt(x1);
		assertThrown(()->test(x1).isGt(x1)).asMessage().asOneLine().isMatches("Value was not greater than expected.  Expect='*2000'.  Actual='*2000'.");
		assertThrown(()->test(x1).isGt(x2)).asMessage().asOneLine().isMatches("Value was not greater than expected.  Expect='*2010'.  Actual='*2000'.");
		assertThrown(()->test(x1).isGt(nil)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isGt(x2)).asMessage().is("Value was null.");
	}

	@Test
	public void cb02_isGte() throws Exception {
		Date x1 = MID1, x2 = MID2, nil = null;
		test(x2).isGte(x1);
		test(x1).isGte(x1);
		assertThrown(()->test(x1).isGte(x2)).asMessage().asOneLine().isMatches("Value was not greater than or equals to expected.  Expect='*2010'.  Actual='*2000'.");
		assertThrown(()->test(x1).isGte(nil)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isGte(x2)).asMessage().is("Value was null.");
	}

	@Test
	public void cb03_isLt() throws Exception {
		Date x1 = MID1, x2 = MID2, nil = null;
		test(x1).isLt(x2);
		assertThrown(()->test(x1).isLt(x1)).asMessage().asOneLine().isMatches("Value was not less than expected.  Expect='*2000'.  Actual='*2000'.");
		assertThrown(()->test(x2).isLt(x1)).asMessage().asOneLine().isMatches("Value was not less than expected.  Expect='*2000'.  Actual='*2010'.");
		assertThrown(()->test(x2).isLt(nil)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isLt(x1)).asMessage().is("Value was null.");
	}

	@Test
	public void cb04_isLte() throws Exception {
		Date x1 = MID1, x2 = MID2, nil = null;
		test(x1).isLte(x2);
		test(x1).isLte(x1);
		assertThrown(()->test(x2).isLte(x1)).asMessage().asOneLine().isMatches("Value was not less than or equals to expected.  Expect='*2000'.  Actual='*2010'.");
		assertThrown(()->test(x2).isLte(nil)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isLte(x1)).asMessage().is("Value was null.");
	}

	@Test
	public void cb05_isBetween() throws Exception {
		Date x1 = MIN, x2 = MID1, x3 = MID2, x4 = MAX, nil = null;
		test(x1).isBetween(x1, x3);
		test(x2).isBetween(x1, x3);
		test(x3).isBetween(x1, x3);
		assertThrown(()->test(x4).isBetween(x1, x3)).asMessage().asOneLine().isMatches("Value was not less than or equals to expected.  Expect='*2010'.  Actual='*2100'.");
		assertThrown(()->test(nil).isBetween(x1, x3)).asMessage().is("Value was null.");
		assertThrown(()->test(x1).isBetween(nil, x3)).asMessage().is("Argument 'lower' cannot be null.");
		assertThrown(()->test(x1).isBetween(x1, nil)).asMessage().asOneLine().is("Argument 'upper' cannot be null.");
	}

	@Test
	public void cc01_is_wChrono() throws Exception {
		Date x1 = MID1, x1a = MID1a, x2 = MIN, nil = null;
		test(x1).is(x1, ChronoUnit.DAYS);
		test(x1).is(x1a, ChronoUnit.DAYS);
		assertThrown(()->test(nil).is(nil, ChronoUnit.DAYS)).asMessage().isContains("Value was null.");
		assertThrown(()->test(x1).is(x2, ChronoUnit.DAYS)).asMessage().isContains("Unexpected value.");
		assertThrown(()->test(x1).is(nil, ChronoUnit.DAYS)).asMessage().isContains("Unexpected value.");
	}

	@Test
	public void cc02_isAfter() throws Exception {
		Date x1 = MIN, x2 = MID1, x3 = MAX, nil = null;
		test(x3).isAfter(x2);
		assertThrown(()->test(x1).isAfter(nil)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->test(x1).isAfter(x2)).asMessage().isContains("Value was not after expected.");
		assertThrown(()->test(nil).isAfter(x2)).asMessage().is("Value was null.");
	}

	@Test
	public void cc03_isAfterNow() throws Exception {
		Date x1 = MIN, x2 = MAX, nil = null;
		test(x2).isAfterNow();
		assertThrown(()->test(x1).isAfterNow()).asMessage().isContains("Value was not after expected.");
		assertThrown(()->test(nil).isAfterNow()).asMessage().isContains("Value was null.");
	}

	@Test
	public void cc04_isBefore() throws Exception {
		Date x1 = MIN, x2 = MID1, nil = null;
		test(x1).isBefore(x2);
		assertThrown(()->test(x1).isBefore(nil)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->test(x2).isBefore(x1)).asMessage().isContains("Value was not before expected.");
		assertThrown(()->test(nil).isBefore(x1)).asMessage().is("Value was null.");
	}

	@Test
	public void cc05_isBeforeNow() throws Exception {
		Date x1 = MIN, x2 = MAX, nil = null;
		test(x1).isBeforeNow();
		assertThrown(()->test(x2).isBeforeNow()).asMessage().isContains("Value was not before expected.");
		assertThrown(()->test(nil).isBeforeNow()).asMessage().isContains("Value was null.");
	}
}
