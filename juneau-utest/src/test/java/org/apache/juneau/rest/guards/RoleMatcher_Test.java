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
package org.apache.juneau.rest.guards;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.rest.guard.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RoleMatcher_Test {

	private void shouldMatch(RoleMatcher m, String...input) {
		for (String i : input)
			if (! m.matches(toSet(i)))
				fail("Matcher "+m+" should have matched '"+i+"' but didn't.");
	}

	private void shouldNotMatch(RoleMatcher m, String...input) {
		for (String i : input)
			if (m.matches(toSet(i)))
				fail("Matcher "+m+" should not have matched '"+i+"' but did.");
	}

	private Set<String> toSet(String input) {
		if (input == null)
			return null;
		if (input.indexOf(',') == -1)
			return Collections.singleton(input);
		if (input.isEmpty())
			return Collections.emptySet();
		return sortedSet(split(input));
	}

	private RoleMatcher create(String expression, String toString, String expressionRoles) throws Exception {
		RoleMatcher m = new RoleMatcher(expression);
		assertEquals("m.toString() didn't match.", toString, m.toString());
		assertEquals("m.getRolesInExpression() didn't match.", expressionRoles, StringUtils.join(m.getRolesInExpression(), ","));
		return m;
	}

	//------------------------------------------------------------------------------------------------------------------
	// No operand
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_noOperand() throws Exception {
		RoleMatcher m = create("", "(NEVER)", "");
		shouldNotMatch(m, "foo", "foox", "xfoo", "fo", null);
	}

	@Test
	public void a02_noOperand_nullInput() throws Exception {
		RoleMatcher m = create(null, "(NEVER)", "");
		shouldNotMatch(m, "foo", "foox", "xfoo", "fo", null);
	}

	@Test
	public void a03_noOperand_onlySpaces() throws Exception {
		RoleMatcher m = create("  ", "(NEVER)", "");
		shouldNotMatch(m, "foo", "foox", "xfoo", "fo", null);
	}

	@Test
	public void a04_noOperand_onlyParenthesis() throws Exception {
		RoleMatcher m = create("()", "(NEVER)", "");
		shouldNotMatch(m, "foo", "foox", "xfoo", "fo", null);
	}

	@Test
	public void a05_noOperand_onlyMultipleParenthesis() throws Exception {
		RoleMatcher m = create("(())", "(NEVER)", "");
		shouldNotMatch(m, "foo", "foox", "xfoo", "fo", null);
	}

	@Test
	public void a06_noOperand_onlyMultipleParenthesisWithSpaces() throws Exception {
		RoleMatcher m = create("  (  (  )  )  ", "(NEVER)", "");
		shouldNotMatch(m, "foo", "foox", "xfoo", "fo", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Single operand equals
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_singleOperand_eq_oneRole() throws Exception {
		RoleMatcher m = create("foo", "[= foo]", "foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void b02_singleOperand_eq_multipleRoles() throws Exception {
		RoleMatcher m = create("foo", "[= foo]", "foo");
		shouldMatch(m, "foo,bar");
		shouldNotMatch(m, "foox,bar", "xfoo,bar", "fo,bar", "");
	}

	@Test
	public void b03_singleOperand_eq_noRoles() throws Exception {
		RoleMatcher m = create("foo", "[= foo]", "foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void b04_singleOperand_eq_withSpaces() throws Exception {
		RoleMatcher m = create("  foo  ", "[= foo]", "foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void b05_singleOperand_eq_withParenthesis() throws Exception {
		RoleMatcher m = create("(foo)", "[= foo]", "foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void b06_singleOperand_eq_withMultipleParenthesis() throws Exception {
		RoleMatcher m = create("((foo))", "[= foo]", "foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void b07_singleOperand_eq_withParenthesisAndSpaces() throws Exception {
		RoleMatcher m = create(" ( foo ) ", "[= foo]", "foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void b08_singleOperand_eq_withMultipleParenthesisAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  foo  )  )  ", "[= foo]", "foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Single operand equals
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_singleOperand_pattern() throws Exception {
		RoleMatcher m = create("foo*", "[* foo.*]", "foo*");
		shouldMatch(m, "foo", "foox");
		shouldNotMatch(m, "xfoo", "fo", null);
	}

	@Test
	public void c02_singleOperand_pattern_multipleRoles() throws Exception {
		RoleMatcher m = create("foo*", "[* foo.*]", "foo*");
		shouldMatch(m, "foo,bar", "foox,bar");
		shouldNotMatch(m, "xfoo,bar", "fo,bar");
	}

	@Test
	public void c03_singleOperand_pattern_NoRoles() throws Exception {
		RoleMatcher m = create("foo*", "[* foo.*]", "foo*");
		shouldNotMatch(m, "");
	}

	@Test
	public void c04_singleOperand_pattern_withSpaces() throws Exception {
		RoleMatcher m = create("  foo*  ", "[* foo.*]", "foo*");
		shouldMatch(m, "foo", "foox");
		shouldNotMatch(m, "xfoo", "fo", null);
	}

	@Test
	public void c05_singleOperand_pattern_withParenthesis() throws Exception {
		RoleMatcher m = create("(foo*)", "[* foo.*]", "foo*");
		shouldMatch(m, "foo", "foox");
		shouldNotMatch(m, "xfoo", "fo", null);
	}

	@Test
	public void c06_singleOperand_pattern_withMultipleParenthesis() throws Exception {
		RoleMatcher m = create("((foo*))", "[* foo.*]", "foo*");
		shouldMatch(m, "foo", "foox");
		shouldNotMatch(m, "xfoo", "fo", null);
	}

	@Test
	public void c07_singleOperand_pattern_withParenthesisAndSpaces() throws Exception {
		RoleMatcher m = create(" ( foo* ) ", "[* foo.*]", "foo*");
		shouldMatch(m, "foo", "foox");
		shouldNotMatch(m, "xfoo", "fo", null);
	}

	@Test
	public void c08_singleOperand_pattern_withMultipleParenthesisAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  foo*  )  )  ", "[* foo.*]", "foo*");
		shouldMatch(m, "foo", "foox");
		shouldNotMatch(m, "xfoo", "fo", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// | operator
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_or_singlePipe() throws Exception {
		RoleMatcher m = create("foo|bar|baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d02_or_singlePipe_multipleRoles() throws Exception {
		RoleMatcher m = create("foo|bar|baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo,bar", "bar,baz", "baz,qux");
		shouldNotMatch(m, "foox,barx", "xfoo,xbar", "fo,ba");
	}

	@Test
	public void d03_or_singlePipe_npRoles() throws Exception {
		RoleMatcher m = create("foo|bar|baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void d04_or_singlePipe_withSpaces() throws Exception {
		RoleMatcher m = create("  foo  |  bar  |  baz  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d05_or_singlePipe_withParentheses() throws Exception {
		RoleMatcher m = create("(foo|bar|baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d06_or_singlePipe_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(foo)|(bar)|(baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d07_or_singlePipe_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  foo  |  bar  |  baz  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d08_or_singlePipe_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((foo)|(bar)|(baz))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d09_or_singlePipe_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((foo))|((bar))|((baz))))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void d10_or_singlePipe_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  foo  )  )  |  (  (  bar  )  )  |  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// || operator
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_or_doublePipe() throws Exception {
		RoleMatcher m = create("foo||bar||baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e02_or_doublePipe_multipleRoles() throws Exception {
		RoleMatcher m = create("foo||bar||baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo,bar", "bar,baz", "baz,qux");
		shouldNotMatch(m, "foox,barx", "xfoo,xbar", "fo,ba");
	}

	@Test
	public void e03_or_doublePipe_noRoles() throws Exception {
		RoleMatcher m = create("foo||bar||baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void e04_or_doublePipe_withSpaces() throws Exception {
		RoleMatcher m = create("  foo  ||  bar  ||  baz  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e05_or_doublePipe_withParentheses() throws Exception {
		RoleMatcher m = create("(foo||bar||baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e06_or_doublePipe_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(foo)||(bar)||(baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e07_or_doublePipe_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  foo  ||  bar  ||  baz  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e08_or_doublePipe_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((foo)||(bar)||(baz))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e09_or_doublePipe_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((foo))||((bar))||((baz))))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void e10_or_doublePipe_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  foo  )  )  ||  (  (  bar  )  )  ||  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// , operator
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_or_comma() throws Exception {
		RoleMatcher m = create("foo,bar,baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f02_or_comma_multipleRoles() throws Exception {
		RoleMatcher m = create("foo,bar,baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo,bar", "bar,baz", "baz,qux");
		shouldNotMatch(m, "foox,barx", "xfoo,xbar", "fo,ba");
	}

	@Test
	public void f03_or_comma_noRoles() throws Exception {
		RoleMatcher m = create("foo,bar,baz", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void f04_or_comma_withSpaces() throws Exception {
		RoleMatcher m = create("  foo  ,  bar  ,  baz  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f05_or_comma_withParentheses() throws Exception {
		RoleMatcher m = create("(foo,bar,baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f06_or_comma_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(foo),(bar),(baz)", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f07_or_comma_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  foo  ,  bar  ,  baz  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f08_or_comma_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((foo),(bar),(baz))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f09_or_comma_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((foo)),((bar)),((baz))))", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	@Test
	public void f10_or_comma_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  foo  )  )  ,  (  (  bar  )  )  ,  (  (  baz  )  )  )  )  ", "(| [= foo] [= bar] [= baz])", "bar,baz,foo");
		shouldMatch(m, "foo", "bar", "baz");
		shouldNotMatch(m, "foox", "xfoo", "fo", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// & operator
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_and_singleAmp() throws Exception {
		RoleMatcher m = create("fo*&*oo&foo", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g02_and_singleAmp_multipleRoles() throws Exception {
		RoleMatcher m = create("foo&bar", "(& [= foo] [= bar])", "bar,foo");
		shouldMatch(m, "foo,bar", "foo,bar,baz");
		shouldNotMatch(m, "foo", "foo,baz", "bar", "bar,baz");
	}

	@Test
	public void g03_and_singleAmp_noRoles() throws Exception {
		RoleMatcher m = create("foo&bar", "(& [= foo] [= bar])", "bar,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void g04_and_singleAmp_withSpaces() throws Exception {
		RoleMatcher m = create("  fo*  &  *oo  &  foo  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g05_and_singleAmp_withParentheses() throws Exception {
		RoleMatcher m = create("(fo*&*oo&foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g06_and_singleAmp_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(fo*)&(*oo)&(foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g07_and_singleAmp_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  fo*  &  *oo  &  foo  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g08_and_singleAmp_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((fo*)&(*oo)&(foo))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g09_and_singleAmp_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((fo*))&((*oo))&((foo))))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void g10_and_singleAmp_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  fo*  )  )  &  (  (  *oo  )  )  &  (  (  foo  )  )  )  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// && operator
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_and_doubleAmp() throws Exception {
		RoleMatcher m = create("fo*&&*oo&&foo", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h02_and_singleAmp_multipleRoles() throws Exception {
		RoleMatcher m = create("foo&&bar", "(& [= foo] [= bar])", "bar,foo");
		shouldMatch(m, "foo,bar", "foo,bar,baz");
		shouldNotMatch(m, "foo", "foo,baz", "bar", "bar,baz");
	}

	@Test
	public void h03_and_singleAmp_noRoles() throws Exception {
		RoleMatcher m = create("foo&&bar", "(& [= foo] [= bar])", "bar,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void h04_and_doubleAmp_withSpaces() throws Exception {
		RoleMatcher m = create("  fo*  &&  *oo  &&  foo  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h05_and_doubleAmp_withParentheses() throws Exception {
		RoleMatcher m = create("(fo*&&*oo&&foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h06_and_doubleAmp_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(fo*)&&(*oo)&&(foo)", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h07_and_doubleAmp_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  fo*  &&  *oo  &&  foo  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h08_and_doubleAmp_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((fo*)&&(*oo)&&(foo))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h09_and_doubleAmp_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((fo*))&&((*oo))&&((foo))))", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void h10_and_doubleAmp_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  fo*  )  )  &&  (  (  *oo  )  )  &&  (  (  foo  )  )  )  )  ", "(& [* fo.*] [* .*oo] [= foo])", "*oo,fo*,foo");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// | and & operators
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void i01_and_singleMixed() throws Exception {
		RoleMatcher m = create("fo*&*oo|bar", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i02_and_singleMixed_multipleRoles() throws Exception {
		RoleMatcher m = create("foo|bar&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo");
		shouldMatch(m, "foo", "bar,baz", "foo,bar,baz", "foo,qux", "bar,baz,qux");
		shouldNotMatch(m, "bar", "baz", "bar,qux", "baz,qux");
	}

	@Test
	public void i03_and_singleMixed_noRoles() throws Exception {
		RoleMatcher m = create("foo|bar&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void i04_and_singleMixed_reversed() throws Exception {
		RoleMatcher m = create("bar|fo*&*oo", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i05_and_singleMixed_withSpaces() throws Exception {
		RoleMatcher m = create("  fo*  &  *oo  |  bar  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i06_and_singleMixed_withSpaces_reversed() throws Exception {
		RoleMatcher m = create("  bar  |  fo*  &  *oo  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i07_and_singleMixed_withParentheses() throws Exception {
		RoleMatcher m = create("(fo*&*oo|bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i08_and_singleMixed_withParentheses_reversed() throws Exception {
		RoleMatcher m = create("(bar|fo*&*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i09_and_singleMixed_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(fo*)&(*oo)|(bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i10_and_singleMixed_withInnerParentheses_reversed() throws Exception {
		RoleMatcher m = create("(bar)|(fo*)&(*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i11_and_singleMixed_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  fo*  &  *oo  |  bar  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i12_and_singleMixed_withParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  bar  |  fo*  &  *oo  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i13_and_singleMixed_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((fo*)&(*oo)|(bar))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i14_and_singleMixed_withMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("((bar)|(fo*)&(*oo))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i15_and_singleMixed_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((fo*))&((*oo))|((bar))))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i16_and_singleMixed_withMultipleMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("((((bar))|((fo*))&((*oo))))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i17_and_singleMixed_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  fo*  )  )  &  (  (  *oo  )  )  |  (  (  bar  )  )  )  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void i18_and_singleMixed_withMultipleMultipleParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  (  (  (  bar  )  )  |  (  (  fo*  )  )  &  (  (  *oo  )  )  )  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// || and && operators
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void j01_and_doubleMixed() throws Exception {
		RoleMatcher m = create("fo*&&*oo||bar", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j02_and_doubleMixed_multipleRoles() throws Exception {
		RoleMatcher m = create("foo||bar&&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo");
		shouldMatch(m, "foo", "bar,baz", "foo,bar,baz", "foo,qux", "bar,baz,qux");
		shouldNotMatch(m, "bar", "baz", "bar,qux", "baz,qux");
	}

	@Test
	public void j03_and_doubleMixed_noRoles() throws Exception {
		RoleMatcher m = create("foo||bar&&baz", "(| [= foo] (& [= bar] [= baz]))", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void j04_and_doubleMixed_reversed() throws Exception {
		RoleMatcher m = create("bar||fo*&&*oo", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j05_and_doubleMixed_withSpaces() throws Exception {
		RoleMatcher m = create("  fo*  &&  *oo  ||  bar  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j06_and_doubleMixed_withSpaces_reversed() throws Exception {
		RoleMatcher m = create("  bar  ||  fo*  &&  *oo  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j07_and_doubleMixed_withParentheses() throws Exception {
		RoleMatcher m = create("(fo*&&*oo||bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j08_and_doubleMixed_withParentheses_reversed() throws Exception {
		RoleMatcher m = create("(bar||fo*&&*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j09_and_doubleMixed_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(fo*)&&(*oo)||(bar)", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j10_and_doubleMixed_withInnerParentheses_reversed() throws Exception {
		RoleMatcher m = create("(bar)||(fo*)&&(*oo)", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j11_and_doubleMixed_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  fo*  &&  *oo  ||  bar  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j12_and_doubleMixed_withParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  bar  ||  fo*  &&  *oo  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j13_and_doubleMixed_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((fo*)&&(*oo)||(bar))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j14_and_doubleMixed_withMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("((bar)||(fo*)&&(*oo))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j15_and_doubleMixed_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((fo*))&&((*oo))||((bar))))", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j16_and_doubleMixed_withMultipleMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("((((bar))||((fo*))&&((*oo))))", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j17_and_doubleMixed_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  fo*  )  )  &&  (  (  *oo  )  )  ||  (  (  bar  )  )  )  )  ", "(| (& [* fo.*] [* .*oo]) [= bar])", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	@Test
	public void j18_and_doubleMixed_withMultipleMultipleParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  (  (  (  bar  )  )  ||  (  (  fo*  )  )  &&  (  (  *oo  )  )  )  )  ", "(| [= bar] (& [* fo.*] [* .*oo]))", "*oo,bar,fo*");
		shouldMatch(m, "foo", "bar");
		shouldNotMatch(m, "foox", "xfoo", "fo", "baz", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// | and & and () operators
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void k01_and_singleMixedParentheses() throws Exception {
		RoleMatcher m = create("fo*&(*oo|bar)", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k02_and_singleMixedParentheses_multipleRoles() throws Exception {
		RoleMatcher m = create("foo&(bar|baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo");
		shouldMatch(m, "foo,bar", "foo,baz", "foo,bar,baz,qux");
		shouldNotMatch(m, "foo", "bar", "baz", "bar,baz", "qux", null);
	}

	@Test
	public void k03_and_singleMixedParentheses_noRoles() throws Exception {
		RoleMatcher m = create("foo&(bar|baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void k04_and_singleMixedParentheses_reversed() throws Exception {
		RoleMatcher m = create("(bar|fo*)&*oo", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k05_and_singleMixedParentheses_withSpaces() throws Exception {
		RoleMatcher m = create("  fo*  &  (  *oo  |  bar  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k06_and_singleMixedParentheses_withSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  bar  |  fo*  )  &  *oo  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k07_and_singleMixedParentheses_withParentheses() throws Exception {
		RoleMatcher m = create("(fo*&(*oo|bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k08_and_singleMixedParentheses_withParentheses_reversed() throws Exception {
		RoleMatcher m = create("((bar|fo*)&*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k09_and_singleMixedParentheses_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(fo*)&((*oo)|(bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k10_and_singleMixedParentheses_withInnerParentheses_reversed() throws Exception {
		RoleMatcher m = create("((bar)|(fo*))&(*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k11_and_singleMixedParentheses_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  fo*  &  (  *oo  |  bar  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k12_and_singleMixedParentheses_withParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  (  bar  |  fo*  )  &  *oo  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k13_and_singleMixedParentheses_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((fo*)&((*oo)|(bar)))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k14_and_singleMixedParentheses_withMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("(((bar)|(fo*))&(*oo))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k15_and_singleMixedParentheses_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((fo*))&(((*oo))|((bar)))))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k16_and_singleMixedParentheses_withMultipleMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("(((((bar))|((fo*)))&((*oo))))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k17_and_singleMixedParentheses_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  fo*  )  )  &  (  (  (  *oo  )  )  |  (  (  bar  )  )  )  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void k18_and_singleMixedParentheses_withMultipleMultipleParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  (  (  (  (  bar  )  )  |  (  (  fo*  )  )  )  &  (  (  *oo  )  )  )  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// || and && and () operators
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void l01_and_doubleMixedParentheses() throws Exception {
		RoleMatcher m = create("fo*&&(*oo||bar)", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l02_and_doubleMixedParentheses_multipleRoles() throws Exception {
		RoleMatcher m = create("foo&&(bar||baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo");
		shouldMatch(m, "foo,bar", "foo,baz", "foo,bar,baz,qux");
		shouldNotMatch(m, "foo", "bar", "baz", "bar,baz", "qux", null);
	}

	@Test
	public void l03_and_doubleMixedParentheses_noRoles() throws Exception {
		RoleMatcher m = create("foo&&(bar||baz)", "(& [= foo] (| [= bar] [= baz]))", "bar,baz,foo");
		shouldNotMatch(m, "");
	}

	@Test
	public void l04_and_doubleMixedParentheses_reversed() throws Exception {
		RoleMatcher m = create("(bar||fo*)&&*oo", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l05_and_doubleMixedParentheses_withSpaces() throws Exception {
		RoleMatcher m = create("  fo*  &&  (  *oo  ||  bar  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l06_and_doubleMixedParentheses_withSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  bar  ||  fo*  )  &&  *oo  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l07_and_doubleMixedParentheses_withParentheses() throws Exception {
		RoleMatcher m = create("(fo*&&(*oo||bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l08_and_doubleMixedParentheses_withParentheses_reversed() throws Exception {
		RoleMatcher m = create("((bar||fo*)&&*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l09_and_doubleMixedParentheses_withInnerParentheses() throws Exception {
		RoleMatcher m = create("(fo*)&((*oo)||(bar))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l10_and_doubleMixedParentheses_withInnerParentheses_reversed() throws Exception {
		RoleMatcher m = create("((bar)||(fo*))&&(*oo)", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l11_and_doubleMixedParentheses_withParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  fo*  &&  (  *oo  ||  bar  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l12_and_doubleMixedParentheses_withParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  (  bar  ||  fo*  )  &&  *oo  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l13_and_doubleMixedParentheses_withMultipleParentheses() throws Exception {
		RoleMatcher m = create("((fo*)&&((*oo)||(bar)))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l14_and_doubleMixedParentheses_withMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("(((bar)||(fo*))&&(*oo))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l15_and_doubleMixedParentheses_withMultipleMultipleParentheses() throws Exception {
		RoleMatcher m = create("((((fo*))&&(((*oo))||((bar)))))", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l16_and_doubleMixedParentheses_withMultipleMultipleParentheses_reversed() throws Exception {
		RoleMatcher m = create("(((((bar))||((fo*)))&&((*oo))))", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l17_and_doubleMixedParentheses_withMultipleMultipleParenthesesAndSpaces() throws Exception {
		RoleMatcher m = create("  (  (  (  (  fo*  )  )  &&  (  (  (  *oo  )  )  ||  (  (  bar  )  )  )  )  )  ", "(& [* fo.*] (| [* .*oo] [= bar]))", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	@Test
	public void l18_and_doubleMixedParentheses_withMultipleMultipleParenthesesAndSpaces_reversed() throws Exception {
		RoleMatcher m = create("  (  (  (  (  (  bar  )  )  ||  (  (  fo*  )  )  )  &&  (  (  *oo  )  )  )  )  ", "(& (| [= bar] [* fo.*]) [* .*oo])", "*oo,bar,fo*");
		shouldMatch(m, "foo");
		shouldNotMatch(m, "foox", "xfoo", "fo", "bar", "baz", null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error conditions
	//------------------------------------------------------------------------------------------------------------------

	@Test(expected = ParseException.class)
	public void m01_startsWithOp() throws Exception {
		new RoleMatcher("&foo");
	}

	@Test(expected = ParseException.class)
	public void m02_noOperator() throws Exception {
		new RoleMatcher("foo bar");
	}

	@Test(expected = ParseException.class)
	public void m03_unmatchedParentheses() throws Exception {
		new RoleMatcher("(foo");
	}

	@Test(expected = ParseException.class)
	public void m04a_danglingExpression_1() throws Exception {
		new RoleMatcher("foo &");
	}

	@Test(expected = ParseException.class)
	public void m04b_danglingExpression_2() throws Exception {
		new RoleMatcher("foo ||");
	}

	@Test(expected = ParseException.class)
	public void m04c_danglingExpression_3() throws Exception {
		new RoleMatcher("foo ,");
	}

	@Test(expected = ParseException.class)
	public void m04d_danglingExpression_4() throws Exception {
		new RoleMatcher("foo & ");
	}

	@Test(expected = ParseException.class)
	public void m04e_danglingExpression_5() throws Exception {
		new RoleMatcher("foo || ");
	}

	@Test(expected = ParseException.class)
	public void m04f_danglingExpression_6() throws Exception {
		new RoleMatcher("foo , ");
	}
}
