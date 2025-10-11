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
package org.apache.juneau.utils;

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.text.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link StringExpressionMatcher}.
 */
class StringExpressionMatcher_Test extends TestBase {

	private void test(String expression, String toString, String[] shouldMatch, String[] shouldNotMatch) {
		var m = safe(()->new StringExpressionMatcher(expression));
		assertEquals(toString, m.toString());
		for (var i : shouldMatch)
			if (! m.matches(i))
				fail("Matcher "+m+" should have matched '"+i+"' but didn't.");
		for (var i : shouldNotMatch)
			if (m.matches(i))
				fail("Matcher "+m+" should not have matched '"+i+"' but did.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// No operand
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_noOperand() {
		test("", "(NEVER)", a(), a("foo", "foox", "xfoo", "fo", null));
		test(null, "(NEVER)", a(), a("foo", "foox", "xfoo", "fo", null));
		test("  ", "(NEVER)", a(), a("foo", "foox", "xfoo", "fo", null));
		test("()", "(NEVER)", a(), a("foo", "foox", "xfoo", "fo", null));
		test("(())", "(NEVER)", a(), a("foo", "foox", "xfoo", "fo", null));
		test("  (  (  )  )  ", "(NEVER)", a(), a("foo", "foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Single operand equals
	//------------------------------------------------------------------------------------------------------------------

	@Test void a02_singleOperand_eq() {
		test("foo", "[= foo]", a("foo"), a("foox", "xfoo", "fo", null));
		test("  foo  ", "[= foo]", a("foo"), a("foox", "xfoo", "fo", null));
		test("(foo)", "[= foo]", a("foo"), a("foox", "xfoo", "fo", null));
		test("((foo))", "[= foo]", a("foo"), a("foox", "xfoo", "fo", null));
		test(" ( foo ) ", "[= foo]", a("foo"), a("foox", "xfoo", "fo", null));
		test("  (  (  foo  )  )  ", "[= foo]", a("foo"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Single operand equals
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_singleOperand_pattern() {
		test("foo*", "[* foo.*]", a("foo", "foox"), a("xfoo", "fo", null));
		test("  foo*  ", "[* foo.*]", a("foo", "foox"), a("xfoo", "fo", null));
		test("(foo*)", "[* foo.*]", a("foo", "foox"), a("xfoo", "fo", null));
		test("((foo*))", "[* foo.*]", a("foo", "foox"), a("xfoo", "fo", null));
		test(" ( foo* ) ", "[* foo.*]", a("foo", "foox"), a("xfoo", "fo", null));
		test("  (  (  foo*  )  )  ", "[* foo.*]", a("foo", "foox"), a("xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// | operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_or_singlePipe() {
		test("foo|bar|baz", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  foo  |  bar  |  baz  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo|bar|baz)", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo)|(bar)|(baz)", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  foo  |  bar  |  baz  )  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((foo)|(bar)|(baz))", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((((foo))|((bar))|((baz))))", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  (  (  (  foo  )  )  |  (  (  bar  )  )  |  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// || operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_or_doublePipe() {
		test("foo||bar||baz", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  foo  ||  bar  ||  baz  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo||bar||baz)", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo)||(bar)||(baz)", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  foo  ||  bar  ||  baz  )  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((foo)||(bar)||(baz))", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((((foo))||((bar))||((baz))))", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  (  (  (  foo  )  )  ||  (  (  bar  )  )  ||  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// , operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_or_comma() {
		test("foo,bar,baz", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  foo  ,  bar  ,  baz  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo,bar,baz)", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("(foo),(bar),(baz)", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  foo  ,  bar  ,  baz  )  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((foo),(bar),(baz))", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("((((foo)),((bar)),((baz))))", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
		test("  (  (  (  (  foo  )  )  ,  (  (  bar  )  )  ,  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", a("foo", "bar", "baz"), a("foox", "xfoo", "fo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// & operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a07_and_singleAmp() {
		test("fo*&*oo&foo", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  fo*  &  *oo  &  foo  ", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&*oo&foo)", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&(*oo)&(foo)", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &  *oo  &  foo  )  ", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&(*oo)&(foo))", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&((*oo))&((foo))))", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &  (  (  *oo  )  )  &  (  (  foo  )  )  )  )  ", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// && operator
	//------------------------------------------------------------------------------------------------------------------

	@Test void a08_and_doubleAmp() {
		test("fo*&&*oo&&foo", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  fo*  &&  *oo  &&  foo  ", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&&*oo&&foo)", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&&(*oo)&&(foo)", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &&  *oo  &&  foo  )  ", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&&(*oo)&&(foo))", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&&((*oo))&&((foo))))", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &&  (  (  *oo  )  )  &&  (  (  foo  )  )  )  )  ", "(& [* fo.*] [* .*oo] [= foo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// | and & operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a09_and_singleMixed() {
		test("fo*&*oo|bar", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("bar|fo*&*oo", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  fo*  &  *oo  |  bar  ", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  bar  |  fo*  &  *oo  ", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*&*oo|bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar|fo*&*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*)&(*oo)|(bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar)|(fo*)&(*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  fo*  &  *oo  |  bar  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  bar  |  fo*  &  *oo  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((fo*)&(*oo)|(bar))", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((bar)|(fo*)&(*oo))", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((fo*))&((*oo))|((bar))))", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((bar))|((fo*))&((*oo))))", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  fo*  )  )  &  (  (  *oo  )  )  |  (  (  bar  )  )  )  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  bar  )  )  |  (  (  fo*  )  )  &  (  (  *oo  )  )  )  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// "||" and "&&" operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a10_and_doubleMixed() {
		test("fo*&&*oo||bar", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("bar||fo*&&*oo", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  fo*  &&  *oo  ||  bar  ", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  bar  ||  fo*  &&  *oo  ", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*&&*oo||bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar||fo*&&*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(fo*)&&(*oo)||(bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("(bar)||(fo*)&&(*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  fo*  &&  *oo  ||  bar  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  bar  ||  fo*  &&  *oo  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((fo*)&&(*oo)||(bar))", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((bar)||(fo*)&&(*oo))", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((fo*))&&((*oo))||((bar))))", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("((((bar))||((fo*))&&((*oo))))", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  fo*  )  )  &&  (  (  *oo  )  )  ||  (  (  bar  )  )  )  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
		test("  (  (  (  (  bar  )  )  ||  (  (  fo*  )  )  &&  (  (  *oo  )  )  )  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", a("foo", "bar"), a("foox", "xfoo", "fo", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// | and & and () operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a11_and_singleMixedParentheses() {
		test("fo*&(*oo|bar)", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(bar|fo*)&*oo", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  fo*  &  (  *oo  |  bar  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  bar  |  fo*  )  &  *oo  ", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&(*oo|bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar|fo*)&*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&((*oo)|(bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar)|(fo*))&(*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &  (  *oo  |  bar  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  bar  |  fo*  )  &  *oo  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&((*oo)|(bar)))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((bar)|(fo*))&(*oo))", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&(((*oo))|((bar)))))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((((bar))|((fo*)))&((*oo))))", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &  (  (  (  *oo  )  )  |  (  (  bar  )  )  )  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  (  bar  )  )  |  (  (  fo*  )  )  )  &  (  (  *oo  )  )  )  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// "||" and "&&" and "()" operators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a12_and_doubleMixedParentheses() {
		test("fo*&&(*oo||bar)", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(bar||fo*)&&*oo", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  fo*  &&  (  *oo  ||  bar  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  bar  ||  fo*  )  &&  *oo  ", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*&&(*oo||bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar||fo*)&&*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(fo*)&((*oo)||(bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((bar)||(fo*))&&(*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  fo*  &&  (  *oo  ||  bar  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  bar  ||  fo*  )  &&  *oo  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((fo*)&&((*oo)||(bar)))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((bar)||(fo*))&&(*oo))", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("((((fo*))&&(((*oo))||((bar)))))", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("(((((bar))||((fo*)))&&((*oo))))", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  fo*  )  )  &&  (  (  (  *oo  )  )  ||  (  (  bar  )  )  )  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
		test("  (  (  (  (  (  bar  )  )  ||  (  (  fo*  )  )  )  &&  (  (  *oo  )  )  )  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", a("foo"), a("foox", "xfoo", "fo", "bar", "baz", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error conditions
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_errors() {
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("&foo"));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo bar"));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("(foo"));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo &"));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo ||"));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo ,"));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo & "));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo || "));
		assertThrows(ParseException.class, ()->new StringExpressionMatcher("foo , "));
	}
}