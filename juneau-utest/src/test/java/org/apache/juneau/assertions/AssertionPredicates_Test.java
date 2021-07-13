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
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.junit.runners.MethodSorters.*;
import static java.util.regex.Pattern.*;

import java.util.*;
import java.util.regex.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AssertionPredicates_Test {

	private StringAssertion x1 = assertString("foo").silent(), x2 = assertString(Optional.empty()).silent();

	@Test
	public void a00_dummyConstructor() {
		new AssertionPredicates();
	}

	@Test
	public void a01_any() {
		x1.passes(any());
		x2.passes(any());
	}

	@Test
	public void a02_notNull() {
		x1.passes(notNull());
		assertThrown(()->x2.passes(notNull())).message().is("Value was null.");
	}

	@Test
	public void a03_isNull() {
		assertThrown(()->x1.passes(isNull())).message().is("Value was not null.");
		x2.passes(isNull());
	}

	@Test
	public void a04_eq() {
		x1.passes(eq("foo"));
		x1.passes(eq((Object)"foo"));
		assertThrown(()->x1.passes(eq("FOO"))).message().is("Value did not match expected.\n\tExpect=\"FOO\".\n\tActual=\"foo\".");
		assertThrown(()->x1.passes(eq("bar"))).message().is("Value did not match expected.\n\tExpect=\"bar\".\n\tActual=\"foo\".");
		assertThrown(()->x1.passes(eq((Object)"bar"))).message().is("Value did not match expected.\n\tExpect=\"bar\".\n\tActual=\"foo\".");
		x2.passes(eq(null));
		assertThrown(()->x2.passes(eq("foo"))).message().is("Value did not match expected.\n\tExpect=\"foo\".\n\tActual=\"null\".");
	}

	@Test
	public void a05_eqic() {
		x1.passes(eqic("foo"));
		x1.passes(eqic("FOO"));
		assertThrown(()->x1.passes(eqic("bar"))).message().is("Value did not match expected.\n\tExpect=\"bar\".\n\tActual=\"foo\".");
		x2.passes(eqic(null));
		assertThrown(()->x2.passes(eqic("bar"))).message().is("Value did not match expected.\n\tExpect=\"bar\".\n\tActual=\"null\".");
	}

	@Test
	public void a06_ne() {
		x1.passes(ne("bar"));
		x1.passes(ne((Object)"bar"));
		assertThrown(()->x1.passes(ne("foo"))).message().is("Value unexpectedly matched.\n\tValue=\"foo\".");
		assertThrown(()->x1.passes(ne((Object)"foo"))).message().is("Value unexpectedly matched.\n\tValue=\"foo\".");
		x2.passes(ne("bar"));
		assertThrown(()->x2.passes(ne(null))).message().is("Value unexpectedly matched.\n\tValue=\"null\".");
	}

	@Test
	public void a07_type() {
		x1.passes(type(String.class));
		x1.passes(type(Object.class));
		assertThrown(()->x1.passes(type(Integer.class))).message().is("Value was not expected type.\n\tExpect=\"java.lang.Integer\".\n\tActual=\"java.lang.String\".");
		assertThrown(()->x2.passes(type(String.class))).message().is("Value was not expected type.\n\tExpect=\"java.lang.String\".\n\tActual=\"null\".");
	}

	@Test
	public void a08_exactType() {
		x1.passes(exactType(String.class));
		assertThrown(()->x1.passes(exactType(Object.class))).message().is("Value was not expected type.\n\tExpect=\"java.lang.Object\".\n\tActual=\"java.lang.String\".");
		assertThrown(()->x2.passes(exactType(Object.class))).message().is("Value was not expected type.\n\tExpect=\"java.lang.Object\".\n\tActual=\"null\".");
	}

	@Test
	public void a03_() {
		x1.passes(match("fo*"));
		assertThrown(()->x1.passes(match("ba*"))).message().is("Value did not match pattern.\n\tPattern=\"ba*\".\n\tValue=\"foo\".");
		assertThrown(()->x2.passes(match("ba*"))).message().is("Value did not match pattern.\n\tPattern=\"ba*\".\n\tValue=\"null\".");
	}


	@Test
	public void a10_regex() {
		x1.passes(regex("fo.*"));
		assertThrown(()->x1.passes(regex("ba.*"))).message().is("Value did not match pattern.\n\tPattern=\"ba.*\".\n\tValue=\"foo\".");
		assertThrown(()->x2.passes(regex("ba.*"))).message().is("Value did not match pattern.\n\tPattern=\"ba.*\".\n\tValue=\"null\".");
	}

	@Test
	public void a11_regex_wFlags() {
		x1.passes(regex("FO.*", CASE_INSENSITIVE));
		assertThrown(()->x1.passes(regex("BA.*", CASE_INSENSITIVE))).message().is("Value did not match pattern.\n\tPattern=\"BA.*\".\n\tValue=\"foo\".");
		assertThrown(()->x2.passes(regex("BA.*", CASE_INSENSITIVE))).message().is("Value did not match pattern.\n\tPattern=\"BA.*\".\n\tValue=\"null\".");
	}

	@Test
	public void a12_regex_wPattern() {
		Pattern p1 = Pattern.compile("FO.*", CASE_INSENSITIVE), p2 = Pattern.compile("BA.*", CASE_INSENSITIVE);
		x1.passes(regex(p1));
		assertThrown(()->x1.passes(regex(p2))).message().is("Value did not match pattern.\n\tPattern=\"BA.*\".\n\tValue=\"foo\".");
		assertThrown(()->x2.passes(regex(p2))).message().is("Value did not match pattern.\n\tPattern=\"BA.*\".\n\tValue=\"null\".");
	}

	@Test
	public void a13_and() {
		x1.passes(and(notNull(),eq("foo"),null,x->x.equals("foo")));
		assertThrown(()->x1.passes(and(isNull()))).message().is("Predicate test #1 failed.\n\tValue was not null.");
		assertThrown(()->x1.passes(and(x -> x.equals("bar")))).message().is("Predicate test #1 failed.");
	}

	@Test
	public void a14_or() {
		x1.passes(or(null,isNull(),eq("foo"),x->x.equals("bar")));
		assertThrown(()->x1.passes(or(isNull()))).message().is("No predicate tests passed.");
		assertThrown(()->x1.passes(or(x -> x.equals("bar")))).message().is("No predicate tests passed.");
	}

	@Test
	public void a15_not() {
		x1.passes(not(ne("foo")));
		assertThrown(()->x2.passes(not(ne("foo")))).message().is("Predicate test unexpectedly passed.");
		assertThrown(()->x1.passes(not(eq("foo")))).message().is("Predicate test unexpectedly passed.");
		x2.passes(not(x -> x != null && x.equals("bar")));
		x1.passes(not(null));
		x2.passes(not(null));
	}

	@Test
	public void a16_test() {
		x1.passes(test(eq("foo")));
		assertThrown(()->x1.passes(test(eq("bar")))).message().is("Value did not pass test.\n\tValue did not match expected.\n\tExpect=\"bar\".\n\tActual=\"foo\".");
	}
}
