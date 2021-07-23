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
import static java.time.temporal.ChronoUnit.*;

import java.time.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ZonedDateTimeAssertion_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	private ZonedDateTimeAssertion test(ZonedDateTime value) {
		return assertZonedDateTime(value).silent();
	}

	private static ZonedDateTime zdt(String s) {
		return ZonedDateTime.parse(s);
	}

	private static final ZonedDateTime
		MIN = zdt("1900-06-01T12:34:56Z"),
		MAX = zdt("2100-06-01T12:34:56Z"),
		MID1 = zdt("2000-06-01T12:34:56Z"),
		MID1a = zdt("2000-06-01T12:34:56Z"),
		MID2 = zdt("2010-06-01T12:34:56Z")
	;
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

	//-----------------------------------------------------------------------------------------------------------------
	// Transform tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ba01a_asString() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).asString().is("2000-06-01T12:34:56Z");
		test(nil).asString().isNull();
	}

	@Test
	public void ba01b_asString_wSerializer() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x).asString(s).is("'2000-06-01T12:34:56Z'");
		test(nil).asString(s).is("null");
	}

	@Test
	public void ba01c_asString_wPredicate() throws Exception {
		ZonedDateTime x1 = MID1;
		test(x1).asString(x -> "foo").is("foo");
	}

	@Test
	public void ba02_asJson() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).asJson().is("'2000-06-01T12:34:56Z'");
		test(nil).asJson().is("null");
	}

	@Test
	public void ba03_asJsonSorted() throws Exception {
		ZonedDateTime x1 = MID1, nil = null;
		test(x1).asJsonSorted().is("'2000-06-01T12:34:56Z'");
		test(nil).asJsonSorted().is("null");
	}

	@Test
	public void ba04_apply() throws Exception {
		ZonedDateTime x1 = MID1, x2 = MID2;
		test(x1).apply(x -> x2).is(x2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void ca01_exists() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).exists().exists();
		assertThrown(()->test(nil).exists()).message().is("Value was null.");
	}

	@Test
	public void ca02_isNull() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(nil).isNull();
		assertThrown(()->test(x).isNull()).message().is("Value was not null.");
	}

	@Test
	public void ca03_isNotNull() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).isNotNull();
		assertThrown(()->test(nil).isNotNull()).message().is("Value was null.");
	}

	@Test
	public void ca04a_is_T() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).is(x1);
		test(x1).is(x1a);
		test(nil).is(nil);
		assertThrown(()->test(x1).is(x2)).message().oneLine().is("Unexpected value.  Expect='2010-06-01T12:34:56Z'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(x1).is(nil)).message().oneLine().is("Unexpected value.  Expect='null'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(nil).is(x2)).message().oneLine().is("Unexpected value.  Expect='2010-06-01T12:34:56Z'.  Actual='null'.");
	}

	@Test
	public void ca04b_is_predicate() throws Exception {
		ZonedDateTime x1 = MID1;
		test(x1).is(x->x!=null);
		assertThrown(()->test(x1).is(x->x==null)).message().oneLine().is("Unexpected value: '2000-06-01T12:34:56Z'.");
		assertThrown(()->test(x1).is(ne(x1))).message().oneLine().is("Value unexpectedly matched.  Value='2000-06-01T12:34:56Z'.");
	}

	@Test
	public void ca05_isNot() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isNot(x2);
		test(x1).isNot(nil);
		test(nil).isNot(x1);
		assertThrown(()->test(x1).isNot(x1a)).message().oneLine().is("Unexpected value.  Did not expect='2000-06-01T12:34:56Z'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(nil).isNot(nil)).message().oneLine().is("Unexpected value.  Did not expect='null'.  Actual='null'.");
	}

	@Test
	public void ca06_isAny() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isAny(x1a, x2);
		assertThrown(()->test(x1).isAny(x2)).message().oneLine().is("Expected value not found.  Expect='[2010-06-01T12:34:56Z]'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(x1).isAny()).message().oneLine().is("Expected value not found.  Expect='[]'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(nil).isAny(x2)).message().oneLine().is("Expected value not found.  Expect='[2010-06-01T12:34:56Z]'.  Actual='null'.");
	}

	@Test
	public void ca07_isNotAny() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isNotAny(x2);
		test(x1).isNotAny();
		test(nil).isNotAny(x2);
		assertThrown(()->test(x1).isNotAny(x1a)).message().oneLine().is("Unexpected value found.  Unexpected='2000-06-01T12:34:56Z'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(nil).isNotAny(nil)).message().oneLine().is("Unexpected value found.  Unexpected='null'.  Actual='null'.");
	}

	@Test
	public void ca08_isSame() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, nil = null;
		test(x1).isSame(x1);
		test(nil).isSame(nil);
		assertThrown(()->test(x1).isSame(x1a)).message().oneLine().matches("Not the same value.  Expect='2000-06-01T12:34:56Z(ZonedDateTime@*)'.  Actual='2000-06-01T12:34:56Z(ZonedDateTime@*)'.");
		assertThrown(()->test(nil).isSame(x1a)).message().oneLine().matches("Not the same value.  Expect='2000-06-01T12:34:56Z(ZonedDateTime@*)'.  Actual='null(null)'.");
		assertThrown(()->test(x1).isSame(nil)).message().oneLine().matches("Not the same value.  Expect='null(null)'.  Actual='2000-06-01T12:34:56Z(ZonedDateTime@*)'.");
	}

	@Test
	public void ca09_isSameJsonAs() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isSameJsonAs(x1a);
		test(nil).isSameJsonAs(nil);
		assertThrown(()->test(x1a).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect=''2010-06-01T12:34:56Z''.  Actual=''2000-06-01T12:34:56Z''.");
		assertThrown(()->test(nil).isSameJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect=''2010-06-01T12:34:56Z''.  Actual='null'.");
		assertThrown(()->test(x1).isSameJsonAs(nil)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual=''2000-06-01T12:34:56Z''.");
	}

	@Test
	public void ca10_isSameSortedJsonAs() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		test(x1).isSameSortedJsonAs(x1a);
		test(nil).isSameSortedJsonAs(nil);
		assertThrown(()->test(x1a).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect=''2010-06-01T12:34:56Z''.  Actual=''2000-06-01T12:34:56Z''.");
		assertThrown(()->test(nil).isSameSortedJsonAs(x2)).message().oneLine().is("Unexpected comparison.  Expect=''2010-06-01T12:34:56Z''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSortedJsonAs(nil)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual=''2000-06-01T12:34:56Z''.");
	}

	@Test
	public void ca11_isSameSerializedAs() throws Exception {
		ZonedDateTime x1 = MID1, x1a = MID1a, x2 = MID2, nil = null;
		WriterSerializer s = SimpleJsonSerializer.DEFAULT;
		test(x1).isSameSerializedAs(x1a, s);
		test(nil).isSameSerializedAs(nil, s);
		assertThrown(()->test(x1a).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.  Expect=''2010-06-01T12:34:56Z''.  Actual=''2000-06-01T12:34:56Z''.");
		assertThrown(()->test(nil).isSameSerializedAs(x2, s)).message().oneLine().is("Unexpected comparison.  Expect=''2010-06-01T12:34:56Z''.  Actual='null'.");
		assertThrown(()->test(x1).isSameSerializedAs(nil, s)).message().oneLine().is("Unexpected comparison.  Expect='null'.  Actual=''2000-06-01T12:34:56Z''.");
	}

	@Test
	public void ca12_isType() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).isType(ZonedDateTime.class);
		test(x).isType(Object.class);
		assertThrown(()->test(x).isType(String.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.time.ZonedDateTime'.");
		assertThrown(()->test(nil).isType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca13_isExactType() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).isExactType(ZonedDateTime.class);
		assertThrown(()->test(x).isExactType(Object.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.Object'.  Actual='java.time.ZonedDateTime'.");
		assertThrown(()->test(x).isExactType(String.class)).message().oneLine().is("Unexpected type.  Expect='java.lang.String'.  Actual='java.time.ZonedDateTime'.");
		assertThrown(()->test(nil).isExactType(String.class)).message().oneLine().is("Value was null.");
		assertThrown(()->test(x).isExactType(null)).message().oneLine().is("Argument 'parent' cannot be null.");
	}

	@Test
	public void ca14_isString() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).isString("2000-06-01T12:34:56Z");
		test(nil).isString(null);
		assertThrown(()->test(x).isString("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(x).isString(null)).message().oneLine().is("String differed at position 0.  Expect='null'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(nil).isString("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void ca15_isJson() throws Exception {
		ZonedDateTime x = MID1, nil = null;
		test(x).isJson("'2000-06-01T12:34:56Z'");
		test(nil).isJson("null");
		assertThrown(()->test(x).isJson("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual=''2000-06-01T12:34:56Z''.");
		assertThrown(()->test(x).isJson(null)).message().oneLine().is("String differed at position 0.  Expect='null'.  Actual=''2000-06-01T12:34:56Z''.");
		assertThrown(()->test(nil).isJson("bad")).message().oneLine().is("String differed at position 0.  Expect='bad'.  Actual='null'.");
	}

	@Test
	public void cb01_isGt() throws Exception {
		ZonedDateTime x1 = MIN, x2 = MID1, nil = null;
		test(x2).isGt(x1);
		assertThrown(()->test(x1).isGt(x1)).message().oneLine().is("Value was not greater than expected.  Expect='1900-06-01T12:34:56Z'.  Actual='1900-06-01T12:34:56Z'.");
		assertThrown(()->test(x1).isGt(x2)).message().oneLine().is("Value was not greater than expected.  Expect='2000-06-01T12:34:56Z'.  Actual='1900-06-01T12:34:56Z'.");
		assertThrown(()->test(x1).isGt(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isGt(x2)).message().is("Value was null.");
	}

	@Test
	public void cb02_isGte() throws Exception {
		ZonedDateTime x1 = MIN, x2 = MID1, nil = null;
		test(x2).isGte(x1);
		test(x1).isGte(x1);
		assertThrown(()->test(x1).isGte(x2)).message().oneLine().is("Value was not greater than or equals to expected.  Expect='2000-06-01T12:34:56Z'.  Actual='1900-06-01T12:34:56Z'.");
		assertThrown(()->test(x1).isGte(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isGte(x2)).message().is("Value was null.");
	}

	@Test
	public void cb03_isLt() throws Exception {
		ZonedDateTime x1 = MIN, x2 = MID1, nil = null;
		test(x1).isLt(x2);
		assertThrown(()->test(x1).isLt(x1)).message().oneLine().is("Value was not less than expected.  Expect='1900-06-01T12:34:56Z'.  Actual='1900-06-01T12:34:56Z'.");
		assertThrown(()->test(x2).isLt(x1)).message().oneLine().is("Value was not less than expected.  Expect='1900-06-01T12:34:56Z'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(x2).isLt(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isLt(x1)).message().is("Value was null.");
	}

	@Test
	public void cb04_isLte() throws Exception {
		ZonedDateTime x1 = MIN, x2 = MID1, nil = null;
		test(x1).isLte(x2);
		test(x1).isLte(x1);
		assertThrown(()->test(x2).isLte(x1)).message().oneLine().is("Value was not less than or equals to expected.  Expect='1900-06-01T12:34:56Z'.  Actual='2000-06-01T12:34:56Z'.");
		assertThrown(()->test(x2).isLte(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isLte(x1)).message().is("Value was null.");
	}

	@Test
	public void cb05_isBetween() throws Exception {
		ZonedDateTime x1 = MIN, x2 = MID1, x3 = MID2, x4 = MAX, nil = null;
		test(x1).isBetween(x1, x3);
		test(x2).isBetween(x1, x3);
		test(x3).isBetween(x1, x3);
		assertThrown(()->test(x4).isBetween(x1, x3)).message().oneLine().is("Value was not less than or equals to expected.  Expect='2010-06-01T12:34:56Z'.  Actual='2100-06-01T12:34:56Z'.");
		assertThrown(()->test(nil).isBetween(x1, x3)).message().is("Value was null.");
		assertThrown(()->test(x1).isBetween(nil, x3)).message().is("Argument 'lower' cannot be null.");
		assertThrown(()->test(x1).isBetween(x1, nil)).message().oneLine().is("Argument 'upper' cannot be null.");
	}

	@Test
	public void cc01_is_wChrono() throws Exception {
		ZonedDateTime x1 = zdt("1950-01-01T12:34:56Z"), x1a = zdt("1950-01-01T12:35:55Z"), nil = null;
		test(x1).is(x1a, HOURS);
		test(x1).is(x1a, MINUTES);
		test(nil).is(nil, MINUTES);
		assertThrown(()->test(x1).is(x1a, SECONDS)).message().oneLine().is("Unexpected value.  Expect='1950-01-01T12:35:55Z'.  Actual='1950-01-01T12:34:56Z'.");
		assertThrown(()->test(x1).is(nil, SECONDS)).message().oneLine().is("Unexpected value.  Expect='null'.  Actual='1950-01-01T12:34:56Z'.");
		assertThrown(()->test(nil).is(x1a, SECONDS)).message().oneLine().is("Unexpected value.  Expect='1950-01-01T12:35:55Z'.  Actual='null'.");
		assertThrown(()->test(x1).is(x1a, null)).message().oneLine().is("Argument 'precision' cannot be null.");
	}

	@Test
	public void cc02_isAfter() throws Exception {
		ZonedDateTime x1 = zdt("1950-01-01T12:34:56Z"), x2 = zdt("2050-01-01T12:34:56Z"), nil = null;
		test(x2).isAfter(x1);
		assertThrown(()->test(x1).isAfter(x2)).message().contains("Value was not after expected.");
		assertThrown(()->test(x1).isAfter(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isAfter(x2)).message().contains("Value was null.");
	}

	@Test
	public void cc03_isAfterNow() throws Exception {
		ZonedDateTime x1 = zdt("1950-01-01T12:34:56Z"), x2 = zdt("2050-01-01T12:34:56Z"), nil = null;
		test(x2).isAfterNow();
		assertThrown(()->test(x1).isAfterNow()).message().contains("Value was not after expected.");
		assertThrown(()->test(nil).isAfterNow()).message().contains("Value was null.");
	}

	@Test
	public void cc04_isBefore() throws Exception {
		ZonedDateTime x1 = zdt("1950-01-01T12:34:56Z"), x2 = zdt("2050-01-01T12:34:56Z"), nil = null;
		test(x1).isBefore(x2);
		assertThrown(()->test(x2).isBefore(x1)).message().contains("Value was not before expected.");
		assertThrown(()->test(x1).isBefore(nil)).message().is("Argument 'value' cannot be null.");
		assertThrown(()->test(nil).isBefore(x2)).message().contains("Value was null.");
	}

	@Test
	public void cc05_isBeforeNow() throws Exception {
		ZonedDateTime x1 = zdt("1950-01-01T12:34:56Z"), x2 = zdt("2050-01-01T12:34:56Z"), nil = null;
		test(x1).isBeforeNow();
		assertThrown(()->test(x2).isBeforeNow()).message().contains("Value was not before expected.");
		assertThrown(()->test(nil).isBeforeNow()).message().contains("Value was null.");
	}
}
