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
package org.apache.juneau.rest.guards;

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.guard.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests {@link RoleMatcher}.
 */
class RoleMatcher_Test extends TestBase {

	private static void test(String expression, String toString, String expressionRoles, String[] shouldMatch, String[] shouldNotMatch) {
		var m = safe(()->new RoleMatcher(expression));
		assertEquals(toString, m.toString(), "m.toString() didn't match.");
		assertEquals(expressionRoles, StringUtils.join(m.getRolesInExpression(), ","), "m.getRolesInExpression() didn't match.");
		for (String i : shouldMatch)
			if (! m.matches(toSet(i)))
				fail("Matcher "+m+" should have matched '"+i+"' but didn't.");
		for (String i : shouldNotMatch)
			if (m.matches(toSet(i)))
				fail("Matcher "+m+" should not have matched '"+i+"' but did.");
	}

	private static Set<String> toSet(String input) {
		if (input == null)
			return null;
		if (input.indexOf(',') == -1)
			return Collections.singleton(input);
		if (input.isEmpty())
			return Collections.emptySet();
		return CollectionUtils2.sortedSet(StringUtils.splita(input));
	}

	//------------------------------------------------------------------------------------------------------------------
	// No operand
	//------------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "  ", "()", "(())", "  (  (  )  )  "})
	void a01_noOperandCases(String operand) {
		test(operand, "(NEVER)", "", a(), a("foo", "foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Single operand equals
	//------------------------------------------------------------------------------------------------------------------

	@Test void a02_singleOperand() {
		test("foo", "[= foo]", "foo", a("foo"), a("foox", "xfoo", "fo", null));
		test("foo", "[= foo]", "foo", a("foo,bar"), a("foox,bar", "xfoo,bar", "fo,bar", ""));
		test("foo", "[= foo]", "foo", a(), a(""));
		test("  foo  ", "[= foo]", "foo", a("foo"), a("foox", "xfoo", "fo", null));
		test("(foo)", "[= foo]", "foo", a("foo"), a("foox", "xfoo", "fo", null));
		test("((foo))", "[= foo]", "foo", a("foo"), a("foox", "xfoo", "fo", null));
		test(" ( foo ) ", "[= foo]", "foo", a("foo"), a("foox", "xfoo", "fo", null));
		test("  (  (  foo  )  )  ", "[= foo]", "foo", a("foo"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Single operand equals
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_singleOperand() {
		test("foo*", "[* foo.*]", "foo*", a("foo", "foox"), a("xfoo", "fo", null));
		test("foo*", "[* foo.*]", "foo*", a("foo,bar", "foox,bar"), a("xfoo,bar", "fo,bar"));
		test("foo*", "[* foo.*]", "foo*", a(), a(""));
		test("  foo*  ", "[* foo.*]", "foo*", a("foo", "foox"), a("xfoo", "fo", null));
		test("(foo*)", "[* foo.*]", "foo*", a("foo", "foox"), a("xfoo", "fo", null));
		test("((foo*))", "[* foo.*]", "foo*", a("foo", "foox"), a("xfoo", "fo", null));
		test(" ( foo* ) ", "[* foo.*]", "foo*", a("foo", "foox"), a("xfoo", "fo", null));
		test("  (  (  foo*  )  )  ", "[* foo.*]", "foo*", a("foo", "foox"), a("xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// | operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_or_singlePipe() {
		test("foo|bar|baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("foo|bar|baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo,bar", "bar,baz", "baz,qux"), a("foox,barx", "xfoo,xbar", "fo,ba"));
		test("foo|bar|baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a(), a(""));
		test("  foo  |  bar  |  baz  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo|bar|baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo)|(bar)|(baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  foo  |  bar  |  baz  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((foo)|(bar)|(baz))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((((foo))|((bar))|((baz))))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  (  (  (  foo  )  )  |  (  (  bar  )  )  |  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// || operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_or_doublePipe() {
		test("foo||bar||baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("foo||bar||baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo,bar", "bar,baz", "baz,qux"), a("foox,barx", "xfoo,xbar", "fo,ba"));
		test("foo||bar||baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a(), a(""));
		test("  foo  ||  bar  ||  baz  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo||bar||baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo)||(bar)||(baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  foo  ||  bar  ||  baz  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((foo)||(bar)||(baz))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((((foo))||((bar))||((baz))))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  (  (  (  foo  )  )  ||  (  (  bar  )  )  ||  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// , operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_or_comma() {
		test("foo,bar,baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("foo,bar,baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo,bar", "bar,baz", "baz,qux"), a("foox,barx", "xfoo,xbar", "fo,ba"));
		test("foo,bar,baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a(), a(""));
		test("  foo  ,  bar  ,  baz  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo,bar,baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo),(bar),(baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  foo  ,  bar  ,  baz  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((foo),(bar),(baz))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((((foo)),((bar)),((baz))))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  (  (  (  foo  )  )  ,  (  (  bar  )  )  ,  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// & operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a07_and_singleAmp() {
		test("fo*&*oo&foo", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("foo&bar", "(& [= foo] [= bar])", "bar,foo", a("foo,bar", "foo,bar,baz"), a("foo", "foo,baz", "bar", "bar,baz"));
		test("foo&bar", "(& [= foo] [= bar])", "bar,foo", a(), a(""));
		test("  fo*  &  *oo  &  foo  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&*oo&foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&(*oo)&(foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &  *oo  &  foo  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&(*oo)&(foo))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&((*oo))&((foo))))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &  (  (  *oo  )  )  &  (  (  foo  )  )  )  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// && operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a08_and_doubleAmp() {
		test("fo*&&*oo&&foo", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("foo&&bar", "(& [= foo] [= bar])", "bar,foo", a("foo,bar", "foo,bar,baz"), a("foo", "foo,baz", "bar", "bar,baz"));
		test("foo&&bar", "(& [= foo] [= bar])", "bar,foo", a(), a(""));
		test("  fo*  &&  *oo  &&  foo  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&&*oo&&foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&&(*oo)&&(foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &&  *oo  &&  foo  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&&(*oo)&&(foo))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&&((*oo))&&((foo))))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &&  (  (  *oo  )  )  &&  (  (  foo  )  )  )  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// | and & operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a09_and_singleMixed() {
		test("fo*&*oo|bar", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("foo|bar&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo", a("foo", "bar,baz", "foo,bar,baz", "foo,qux", "bar,baz,qux"), a("bar", "baz", "bar,qux", "baz,qux"));
		test("foo|bar&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo", a(), a(""));
		test("bar|fo*&*oo", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  fo*  &  *oo  |  bar  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  bar  |  fo*  &  *oo  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*&*oo|bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar|fo*&*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*)&(*oo)|(bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar)|(fo*)&(*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  fo*  &  *oo  |  bar  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  bar  |  fo*  &  *oo  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((fo*)&(*oo)|(bar))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((bar)|(fo*)&(*oo))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((fo*))&((*oo))|((bar))))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((bar))|((fo*))&((*oo))))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  fo*  )  )  &  (  (  *oo  )  )  |  (  (  bar  )  )  )  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  bar  )  )  |  (  (  fo*  )  )  &  (  (  *oo  )  )  )  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// "||" and "&&" operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a10_and_doubleMixed() {
		test("fo*&&*oo||bar", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("foo||bar&&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo", a("foo", "bar,baz", "foo,bar,baz", "foo,qux", "bar,baz,qux"), a("bar", "baz", "bar,qux", "baz,qux"));
		test("foo||bar&&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo", a(), a(""));
		test("bar||fo*&&*oo", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  fo*  &&  *oo  ||  bar  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  bar  ||  fo*  &&  *oo  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*&&*oo||bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar||fo*&&*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*)&&(*oo)||(bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar)||(fo*)&&(*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  fo*  &&  *oo  ||  bar  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  bar  ||  fo*  &&  *oo  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((fo*)&&(*oo)||(bar))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((bar)||(fo*)&&(*oo))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((fo*))&&((*oo))||((bar))))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((bar))||((fo*))&&((*oo))))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  fo*  )  )  &&  (  (  *oo  )  )  ||  (  (  bar  )  )  )  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  bar  )  )  ||  (  (  fo*  )  )  &&  (  (  *oo  )  )  )  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// | and & and () operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a11_and_singleMixedParentheses() {
		test("fo*&(*oo|bar)", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("foo&(bar|baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo", a("foo,bar", "foo,baz", "foo,bar,baz,qux"), a("foo", "bar", "baz", "bar,baz", "qux", null));
		test("foo&(bar|baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo", a(), a(""));
		test("(bar|fo*)&*oo", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  fo*  &  (  *oo  |  bar  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  bar  |  fo*  )  &  *oo  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&(*oo|bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar|fo*)&*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&((*oo)|(bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar)|(fo*))&(*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &  (  *oo  |  bar  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  bar  |  fo*  )  &  *oo  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&((*oo)|(bar)))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((bar)|(fo*))&(*oo))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&(((*oo))|((bar)))))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((((bar))|((fo*)))&((*oo))))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &  (  (  (  *oo  )  )  |  (  (  bar  )  )  )  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  (  bar  )  )  |  (  (  fo*  )  )  )  &  (  (  *oo  )  )  )  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// "||" and "&&" and "()" operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a12_and_doubleMixedParentheses() {
		test("fo*&&(*oo||bar)", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("foo&&(bar||baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo", a("foo,bar", "foo,baz", "foo,bar,baz,qux"), a("foo", "bar", "baz", "bar,baz", "qux", null));
		test("foo&&(bar||baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo", a(), a(""));
		test("(bar||fo*)&&*oo", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  fo*  &&  (  *oo  ||  bar  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  bar  ||  fo*  )  &&  *oo  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&&(*oo||bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar||fo*)&&*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&((*oo)||(bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar)||(fo*))&&(*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &&  (  *oo  ||  bar  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  bar  ||  fo*  )  &&  *oo  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&&((*oo)||(bar)))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((bar)||(fo*))&&(*oo))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&&(((*oo))||((bar)))))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((((bar))||((fo*)))&&((*oo))))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &&  (  (  (  *oo  )  )  ||  (  (  bar  )  )  )  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  (  bar  )  )  ||  (  (  fo*  )  )  )  &&  (  (  *oo  )  )  )  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error conditions
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_errors() {
		assertThrows(ParseException.class, ()->new RoleMatcher("&foo"));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo bar"));
		assertThrows(ParseException.class, ()->new RoleMatcher("(foo"));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo &"));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo ||"));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo ,"));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo & "));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo || "));
		assertThrows(ParseException.class, ()->new RoleMatcher("foo , "));
	}
}