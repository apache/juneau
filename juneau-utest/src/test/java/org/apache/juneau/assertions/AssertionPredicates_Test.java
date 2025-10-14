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

import static java.util.regex.Pattern.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.AssertionPredicates.contains;
import static org.apache.juneau.assertions.AssertionPredicates.eq;
import static org.apache.juneau.assertions.AssertionPredicates.eqic;
import static org.apache.juneau.assertions.AssertionPredicates.ne;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@Deprecated
class AssertionPredicates_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	private static StringAssertion A1 = assertString("foo").setSilent(), A2 = assertString(empty()).setSilent();

	@Test void a01_any() {
		assertDoesNotThrow(()->A1.is(any()));
		assertDoesNotThrow(()->A2.is(any()));
	}

	@Test void a02_notNull() {
		A1.is(notNull());
		assertThrows(BasicAssertionError.class, ()->A2.is(notNull()), "Value was null.");
	}

	@Test void a03_isNull() {
		assertThrows(BasicAssertionError.class, ()->A1.is(isNull()), "Value was not null.");
		A2.is(isNull());
	}

	@Test void a04_eq() {
		A1.is(eq("foo"));
		A1.is(eq((Object)"foo"));
		assertThrown(()->A1.is(eq("FOO"))).asMessage().asOneLine().is("Value did not match expected.  Expect='FOO'.  Actual='foo'.");
		assertThrown(()->A1.is(eq("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		assertThrown(()->A1.is(eq((Object)"bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		A2.is(eq(null));
		assertThrown(()->A2.is(eq("foo"))).asMessage().asOneLine().is("Value did not match expected.  Expect='foo'.  Actual='null'.");
	}

	@Test void a05_eqic() {
		A1.is(eqic("foo"));
		A1.is(eqic("FOO"));
		assertThrown(()->A1.is(eqic("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		A2.is(eqic(null));
		assertThrown(()->A2.is(eqic("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='null'.");
	}

	@Test void a06_ne() {
		A1.is(ne("bar"));
		A1.is(ne((Object)"bar"));
		assertThrown(()->A1.is(ne("foo"))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='foo'.");
		assertThrown(()->A1.is(ne((Object)"foo"))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='foo'.");
		A2.is(ne("bar"));
		assertThrown(()->A2.is(ne(null))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='null'.");
	}

	@Test void a07_type() {
		A1.is(type(String.class));
		A1.is(type(Object.class));
		assertThrown(()->A1.is(type(Integer.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->A2.is(type(String.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.String'.  Actual='null'.");
	}

	@Test void a08_exactType() {
		A1.is(exactType(String.class));
		assertThrown(()->A1.is(exactType(Object.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Object'.  Actual='java.lang.String'.");
		assertThrown(()->A2.is(exactType(Object.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Object'.  Actual='null'.");
	}

	@Test void a03_() {
		A1.is(match("fo*"));
		assertThrown(()->A1.is(match("ba*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba*'.  Value='foo'.");
		assertThrown(()->A2.is(match("ba*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba*'.  Value='null'.");
	}

	@Test void a10_regex() {
		A1.is(regex("fo.*"));
		assertThrown(()->A1.is(regex("ba.*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba.*'.  Value='foo'.");
		assertThrown(()->A2.is(regex("ba.*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba.*'.  Value='null'.");
	}

	@Test void a11_regex_wFlags() {
		A1.is(regex("FO.*", CASE_INSENSITIVE));
		assertThrown(()->A1.is(regex("BA.*", CASE_INSENSITIVE))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='foo'.");
		assertThrown(()->A2.is(regex("BA.*", CASE_INSENSITIVE))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='null'.");
	}

	@Test void a12_regex_wPattern() {
		var p1 = Pattern.compile("FO.*", CASE_INSENSITIVE);
		var p2 = Pattern.compile("BA.*", CASE_INSENSITIVE);
		A1.is(regex(p1));
		assertThrown(()->A1.is(regex(p2))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='foo'.");
		assertThrown(()->A2.is(regex(p2))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='null'.");
	}

	@Test void a13_and() {
		A1.is(and(notNull(),eq("foo"),null,x->x.equals("foo")));
		assertThrown(()->A1.is(and(isNull()))).asMessage().asOneLine().is("Predicate test #1 failed.  Value was not null.");
		assertThrows(BasicAssertionError.class, ()->A1.is(and(x -> x.equals("bar"))), "Predicate test #1 failed.");
	}

	@Test void a14_or() {
		A1.is(or(null,isNull(),eq("foo"),x->x.equals("bar")));
		assertThrows(BasicAssertionError.class, ()->A1.is(or(isNull())), "No predicate tests passed.");
		assertThrows(BasicAssertionError.class, ()->A1.is(or(x -> x.equals("bar"))), "No predicate tests passed.");
	}

	@Test void a15_not() {
		A1.is(not(ne("foo")));
		assertThrows(BasicAssertionError.class, ()->A2.is(not(ne("foo"))), "Predicate test unexpectedly passed.");
		assertThrows(BasicAssertionError.class, ()->A1.is(not(eq("foo"))), "Predicate test unexpectedly passed.");
		A2.is(not(x -> x != null && x.equals("bar")));
		A1.is(not(null));
		A2.is(not(null));
	}

	@Test void a16_test() {
		A1.is(test(eq("foo")));
		assertThrown(()->A1.is(test(eq("bar")))).asMessage().asOneLine().is("Value did not pass test.  Value did not match expected.  Expect='bar'.  Actual='foo'.");
	}

	@Test void a17_contains() {
		A1.is(test(contains("o")));
		assertThrown(()->A1.is(test(contains("x")))).asMessage().asOneLine().is("Value did not pass test.  Value did not contain expected.  Expect='x'.  Actual='foo'.");
	}
}