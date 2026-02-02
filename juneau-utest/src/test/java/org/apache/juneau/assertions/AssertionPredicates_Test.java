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
import static org.apache.juneau.assertions.AssertionPredicates.eq;
import static org.apache.juneau.assertions.AssertionPredicates.eqic;
import static org.apache.juneau.assertions.AssertionPredicates.ne;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class AssertionPredicates_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	private static StringAssertion a1 = assertString("foo").setSilent(), a2 = assertString(opte()).setSilent();

	@Test void a01_any() {
		assertDoesNotThrow(()->a1.is(any()));
		assertDoesNotThrow(()->a2.is(any()));
	}

	@Test void a02_notNull() {
		a1.is(notNull());
		AssertionPredicate<String> predicate1 = notNull();
		assertThrows(BasicAssertionError.class, ()->a2.is(predicate1), "Value was null.");
	}

	@Test void a03_isNull() {
		AssertionPredicate<String> predicate2 = isNull();
		assertThrows(BasicAssertionError.class, ()->a1.is(predicate2), "Value was not null.");
		a2.is(isNull());
	}

	@Test void a04_eq() {
		a1.is(eq("foo"));
		a1.is(eq((Object)"foo"));
		assertThrown(()->a1.is(eq("FOO"))).asMessage().asOneLine().is("Value did not match expected.  Expect='FOO'.  Actual='foo'.");
		assertThrown(()->a1.is(eq("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		assertThrown(()->a1.is(eq((Object)"bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		a2.is(eq(null));
		assertThrown(()->a2.is(eq("foo"))).asMessage().asOneLine().is("Value did not match expected.  Expect='foo'.  Actual='null'.");
	}

	@Test void a05_eqic() {
		a1.is(eqic("foo"));
		a1.is(eqic("FOO"));
		assertThrown(()->a1.is(eqic("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		a2.is(eqic(null));
		assertThrown(()->a2.is(eqic("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='null'.");
	}

	@Test void a06_ne() {
		a1.is(ne("bar"));
		a1.is(ne((Object)"bar"));
		assertThrown(()->a1.is(ne("foo"))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='foo'.");
		assertThrown(()->a1.is(ne((Object)"foo"))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='foo'.");
		a2.is(ne("bar"));
		assertThrown(()->a2.is(ne(null))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='null'.");
	}

	@Test void a07_type() {
		a1.is(type(String.class));
		a1.is(type(Object.class));
		assertThrown(()->a1.is(type(Integer.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->a2.is(type(String.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.String'.  Actual='null'.");
	}

	@Test void a08_exactType() {
		a1.is(exactType(String.class));
		assertThrown(()->a1.is(exactType(Object.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Object'.  Actual='java.lang.String'.");
		assertThrown(()->a2.is(exactType(Object.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Object'.  Actual='null'.");
	}

	@Test void a03_() {
		a1.is(match("fo*"));
		assertThrown(()->a1.is(match("ba*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba*'.  Value='foo'.");
		assertThrown(()->a2.is(match("ba*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba*'.  Value='null'.");
	}

	@Test void a10_regex() {
		a1.is(regex("fo.*"));
		assertThrown(()->a1.is(regex("ba.*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba.*'.  Value='foo'.");
		assertThrown(()->a2.is(regex("ba.*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba.*'.  Value='null'.");
	}

	@Test void a11_regex_wFlags() {
		a1.is(regex("FO.*", CASE_INSENSITIVE));
		assertThrown(()->a1.is(regex("BA.*", CASE_INSENSITIVE))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='foo'.");
		assertThrown(()->a2.is(regex("BA.*", CASE_INSENSITIVE))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='null'.");
	}

	@Test void a12_regex_wPattern() {
		var p1 = Pattern.compile("FO.*", CASE_INSENSITIVE);
		var p2 = Pattern.compile("BA.*", CASE_INSENSITIVE);
		a1.is(regex(p1));
		assertThrown(()->a1.is(regex(p2))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='foo'.");
		assertThrown(()->a2.is(regex(p2))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='null'.");
	}

	@Test void a13_and() {
		a1.is(and(notNull(),eq("foo"),null,x->x.equals("foo")));
		assertThrown(()->a1.is(and(isNull()))).asMessage().asOneLine().is("Predicate test #1 failed.  Value was not null.");
		AssertionPredicate<String> predicate3 = and(x -> x.equals("bar"));
		assertThrows(BasicAssertionError.class, ()->a1.is(predicate3), "Predicate test #1 failed.");
	}

	@Test void a14_or() {
		a1.is(or(null,isNull(),eq("foo"),x->x.equals("bar")));
		AssertionPredicate<String> predicate4 = or(isNull());
		assertThrows(BasicAssertionError.class, ()->a1.is(predicate4), "No predicate tests passed.");
		AssertionPredicate<String> predicate5 = or(x -> x.equals("bar"));
		assertThrows(BasicAssertionError.class, ()->a1.is(predicate5), "No predicate tests passed.");
	}

	@Test void a15_not() {
		a1.is(not(ne("foo")));
		AssertionPredicate<String> predicate6 = not(ne("foo"));
		assertThrows(BasicAssertionError.class, ()->a2.is(predicate6), "Predicate test unexpectedly passed.");
		AssertionPredicate<String> predicate7 = not(eq("foo"));
		assertThrows(BasicAssertionError.class, ()->a1.is(predicate7), "Predicate test unexpectedly passed.");
		a2.is(not(x -> x != null && x.equals("bar")));
		a1.is(not(null));
		a2.is(not(null));
	}

	@Test void a16_test() {
		a1.is(test(eq("foo")));
		assertThrown(()->a1.is(test(eq("bar")))).asMessage().asOneLine().is("Value did not pass test.  Value did not match expected.  Expect='bar'.  Actual='foo'.");
	}

	@Test void a17_contains() {
		a1.is(test(contains("o")));
		assertThrown(()->a1.is(test(contains("x")))).asMessage().asOneLine().is("Value did not pass test.  Value did not contain expected.  Expect='x'.  Actual='foo'.");
	}
}