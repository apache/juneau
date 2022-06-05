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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.junit.runners.MethodSorters.*;
import static java.util.regex.Pattern.*;

import java.util.regex.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AssertionPredicates_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	private static StringAssertion A1 = assertString("foo").setSilent(), A2 = assertString(empty()).setSilent();

	@Test
	public void a00_dummyConstructor() {
		new AssertionPredicates();
	}

	@Test
	public void a01_any() {
		A1.is(any());
		A2.is(any());
	}

	@Test
	public void a02_notNull() {
		A1.is(notNull());
		assertThrown(()->A2.is(notNull())).asMessage().is("Value was null.");
	}

	@Test
	public void a03_isNull() {
		assertThrown(()->A1.is(isNull())).asMessage().is("Value was not null.");
		A2.is(isNull());
	}

	@Test
	public void a04_eq() {
		A1.is(eq("foo"));
		A1.is(eq((Object)"foo"));
		assertThrown(()->A1.is(eq("FOO"))).asMessage().asOneLine().is("Value did not match expected.  Expect='FOO'.  Actual='foo'.");
		assertThrown(()->A1.is(eq("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		assertThrown(()->A1.is(eq((Object)"bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		A2.is(eq(null));
		assertThrown(()->A2.is(eq("foo"))).asMessage().asOneLine().is("Value did not match expected.  Expect='foo'.  Actual='null'.");
	}

	@Test
	public void a05_eqic() {
		A1.is(eqic("foo"));
		A1.is(eqic("FOO"));
		assertThrown(()->A1.is(eqic("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='foo'.");
		A2.is(eqic(null));
		assertThrown(()->A2.is(eqic("bar"))).asMessage().asOneLine().is("Value did not match expected.  Expect='bar'.  Actual='null'.");
	}

	@Test
	public void a06_ne() {
		A1.is(ne("bar"));
		A1.is(ne((Object)"bar"));
		assertThrown(()->A1.is(ne("foo"))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='foo'.");
		assertThrown(()->A1.is(ne((Object)"foo"))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='foo'.");
		A2.is(ne("bar"));
		assertThrown(()->A2.is(ne(null))).asMessage().asOneLine().is("Value unexpectedly matched.  Value='null'.");
	}

	@Test
	public void a07_type() {
		A1.is(type(String.class));
		A1.is(type(Object.class));
		assertThrown(()->A1.is(type(Integer.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Integer'.  Actual='java.lang.String'.");
		assertThrown(()->A2.is(type(String.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.String'.  Actual='null'.");
	}

	@Test
	public void a08_exactType() {
		A1.is(exactType(String.class));
		assertThrown(()->A1.is(exactType(Object.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Object'.  Actual='java.lang.String'.");
		assertThrown(()->A2.is(exactType(Object.class))).asMessage().asOneLine().is("Value was not expected type.  Expect='java.lang.Object'.  Actual='null'.");
	}

	@Test
	public void a03_() {
		A1.is(match("fo*"));
		assertThrown(()->A1.is(match("ba*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba*'.  Value='foo'.");
		assertThrown(()->A2.is(match("ba*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba*'.  Value='null'.");
	}


	@Test
	public void a10_regex() {
		A1.is(regex("fo.*"));
		assertThrown(()->A1.is(regex("ba.*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba.*'.  Value='foo'.");
		assertThrown(()->A2.is(regex("ba.*"))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='ba.*'.  Value='null'.");
	}

	@Test
	public void a11_regex_wFlags() {
		A1.is(regex("FO.*", CASE_INSENSITIVE));
		assertThrown(()->A1.is(regex("BA.*", CASE_INSENSITIVE))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='foo'.");
		assertThrown(()->A2.is(regex("BA.*", CASE_INSENSITIVE))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='null'.");
	}

	@Test
	public void a12_regex_wPattern() {
		Pattern p1 = Pattern.compile("FO.*", CASE_INSENSITIVE), p2 = Pattern.compile("BA.*", CASE_INSENSITIVE);
		A1.is(regex(p1));
		assertThrown(()->A1.is(regex(p2))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='foo'.");
		assertThrown(()->A2.is(regex(p2))).asMessage().asOneLine().is("Value did not match pattern.  Pattern='BA.*'.  Value='null'.");
	}

	@Test
	public void a13_and() {
		A1.is(and(notNull(),eq("foo"),null,x->x.equals("foo")));
		assertThrown(()->A1.is(and(isNull()))).asMessage().asOneLine().is("Predicate test #1 failed.  Value was not null.");
		assertThrown(()->A1.is(and(x -> x.equals("bar")))).asMessage().is("Predicate test #1 failed.");
	}

	@Test
	public void a14_or() {
		A1.is(or(null,isNull(),eq("foo"),x->x.equals("bar")));
		assertThrown(()->A1.is(or(isNull()))).asMessage().is("No predicate tests passed.");
		assertThrown(()->A1.is(or(x -> x.equals("bar")))).asMessage().is("No predicate tests passed.");
	}

	@Test
	public void a15_not() {
		A1.is(not(ne("foo")));
		assertThrown(()->A2.is(not(ne("foo")))).asMessage().is("Predicate test unexpectedly passed.");
		assertThrown(()->A1.is(not(eq("foo")))).asMessage().is("Predicate test unexpectedly passed.");
		A2.is(not(x -> x != null && x.equals("bar")));
		A1.is(not(null));
		A2.is(not(null));
	}

	@Test
	public void a16_test() {
		A1.is(test(eq("foo")));
		assertThrown(()->A1.is(test(eq("bar")))).asMessage().asOneLine().is("Value did not pass test.  Value did not match expected.  Expect='bar'.  Actual='foo'.");
	}

	@Test
	public void a17_contains() {
		A1.is(test(contains("o")));
		assertThrown(()->A1.is(test(contains("x")))).asMessage().asOneLine().is("Value did not pass test.  Value did not contain expected.  Expect='x'.  Actual='foo'.");
	}
}
