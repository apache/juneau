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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class SearchExpressionParser_Test extends TestBase {

	private static final SearchOperatorSet OPS = SearchOperatorSet.standard();

	private static SearchExpression parse(String s) {
		return SearchExpressionParser.parse(s, OPS);
	}

	//====================================================================================================
	// Patterns (literals)
	//====================================================================================================

	@Test
	void a01_bareToken_isLiteral() {
		var e = parse("Bill");
		assertBean(e, "literal,value", "true,Bill");
	}

	@Test
	void a02_wildcardToken_isLiteral() {
		assertEquals("*Bill*", parse("*Bill*").value());
	}

	@Test
	void a03_dollarNumber_isPatternNotOperator() {
		var e = parse("$100");
		assertBean(e, "literal,value", "true,$100");
	}

	@Test
	void a04_quotesAreStripped() {
		assertEquals("a,b", parse("$eq(\"a,b\")").literalArgs().get(0));
	}

	@Test
	void a05_doubledDoubleQuote_decodesToOneLiteralQuote() {
		assertEquals("a\"b", parse("$eq(\"a\"\"b\")").literalArgs().get(0));
	}

	@Test
	void a06_doubledSingleQuote_decodesToOneLiteralQuote() {
		assertEquals("a'b", parse("$eq('a''b')").literalArgs().get(0));
	}

	@Test
	void a07_trailingDoubledQuoteBeforeClose() {
		assertEquals("a\"", parse("$eq(\"a\"\"\")").literalArgs().get(0));
	}

	//====================================================================================================
	// $$ literal-dollar escaping
	//====================================================================================================

	@Test
	void b01_doubleDollar_isLiteralDollar() {
		assertEquals("$100", parse("$$100").value());
	}

	@Test
	void b02_doubleDollarBeforeOperatorName_isLiteralText() {
		var e = parse("$$eq(x)");
		assertBean(e, "literal,value", "true,$eq(x)");
	}

	@Test
	void b03_doubleDollarInsideArg_decodes() {
		assertEquals("$5", parse("$eq($$5)").literalArgs().get(0));
	}

	//====================================================================================================
	// Operators
	//====================================================================================================

	@Test
	void c01_leafOperator() {
		var e = parse("$eq(OPEN)");
		assertBean(e, "function,name,argCount", "true,$eq,1");
		assertSame(OPS.get("$eq"), e.operator());
	}

	@Test
	void c02_multiArgOperator() {
		assertEquals("$in(A,B,C)", parse("$in(A,B,C)").toString());
	}

	@Test
	void c03_combinatorNestsFunctions() {
		var e = parse("$or($eq(A),$eq(B))");
		assertTrue(e.operator().isCombinator());
		assertTrue(e.args().get(0).isFunction());
		assertEquals("$or($eq(A),$eq(B))", e.toString());
	}

	@Test
	void c04_zeroArgOperator() {
		assertEquals("$blank()", parse("$blank()").toString());
	}

	//====================================================================================================
	// Errors
	//====================================================================================================

	static Stream<Arguments> syntaxErrors() {
		return Stream.of(
			Arguments.of("$nope(x)", "Unknown search operator", BeanQuerySyntaxException.Code.UNKNOWN_OPERATOR),
			Arguments.of("$eq", "operator name without arguments", BeanQuerySyntaxException.Code.MALFORMED_OPERATOR),
			Arguments.of("$eq(a", "Unclosed parenthesis", BeanQuerySyntaxException.Code.UNBALANCED),
			Arguments.of("$eq(a)x", "Unexpected text", BeanQuerySyntaxException.Code.TRAILING_TEXT),
			Arguments.of("$in(a,)", "Empty argument", BeanQuerySyntaxException.Code.EMPTY_ARGUMENT),
			Arguments.of("$eq(a,b)", "does not accept", BeanQuerySyntaxException.Code.BAD_ARG_COUNT),
			Arguments.of("   ", "Empty search expression", BeanQuerySyntaxException.Code.EMPTY_EXPRESSION),
			Arguments.of("$eq(\"a)", "Unterminated quoted value", BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE),
			Arguments.of("\"abc", "Unterminated quoted value", BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE),
			Arguments.of("'abc", "Unterminated quoted value", BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE),
			Arguments.of("\"", "Unterminated quoted value", BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE)
		);
	}

	@ParameterizedTest
	@MethodSource("syntaxErrors")
	void d01_syntaxError_throwsWithCode(String input, String message, BeanQuerySyntaxException.Code code) {
		var e = assertThrowsWithMessage(BeanQuerySyntaxException.class, message, () -> parse(input));
		assertEquals(code, e.code());
	}

	@Test
	void d08_nullOperatorSet_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "cannot be null", () -> SearchExpressionParser.parse("x", null));
	}

	//====================================================================================================
	// e - isQuoted() (quoted literals are not wildcard patterns)
	//====================================================================================================

	@Test
	void e01_bareToken_isNotQuoted() {
		var node = parse("a*");
		assertBean(node, "literal,quoted,value", "true,false,a*");
	}

	@Test
	void e02_doubleQuotedToken_isQuoted() {
		var node = parse("\"a*\"");
		assertBean(node, "literal,quoted,value", "true,true,a*");
	}

	@Test
	void e03_singleQuotedToken_isQuoted() {
		var node = parse("'a*'");
		assertBean(node, "quoted,value", "true,a*");
	}

	@Test
	void e04_functionNode_isNotQuoted() {
		var node = parse("$eq(OPEN)");
		assertBean(node, "function,quoted", "true,false");
	}

	//====================================================================================================
	// f - canonical names, $regex hint, UNTERMINATED_QUOTE (D5, D6)
	//====================================================================================================

	@Test
	void f01_badArgCount_usesCanonicalNameNotTypedCase() {
		var e = assertThrowsWithMessage(BeanQuerySyntaxException.class, "Operator '$eq'", () -> parse("$EQ(a,b,c)"));
		assertEquals(BeanQuerySyntaxException.Code.BAD_ARG_COUNT, e.code());
	}

	@Test
	void f02_regexBadArgCount_getsQuotingHint() {
		var e = assertThrowsWithMessage(BeanQuerySyntaxException.class,
			List.of("Operator '$regex'", "Quote regex patterns that contain commas or parentheses"),
			() -> parse("$regex(a,b,c)"));
		assertEquals(BeanQuerySyntaxException.Code.BAD_ARG_COUNT, e.code());
	}

	@Test
	void f03_nonRegexBadArgCount_noHint() {
		var e = assertThrowsWithMessage(BeanQuerySyntaxException.class, "Operator '$eq'", () -> parse("$eq(a,b,c)"));
		assertFalse(e.getMessage().contains("Quote regex"));
	}

	@Test
	void f04_regexUnclosedParen_getsQuotingHint() {
		var e = assertThrowsWithMessage(BeanQuerySyntaxException.class,
			List.of("Unclosed parenthesis", "Quote regex patterns that contain commas or parentheses"),
			() -> parse("$regex(a\\("));
		assertEquals(BeanQuerySyntaxException.Code.UNBALANCED, e.code());
	}

	@Test
	void f05_nonRegexUnclosedParen_noHint() {
		var e = assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unclosed parenthesis", () -> parse("$and($eq(a)"));
		assertFalse(e.getMessage().contains("Quote regex"));
	}

	@Test
	void f07_caseInsensitiveOperatorName_treeHoldsCanonical() {
		var node = parse("$EqIc(open)");
		assertEquals("$eqic", node.name());
	}

	//====================================================================================================
	// g - unterminated quote in a bare pattern, leaf-level silent absorption
	//====================================================================================================

	@Test
	void g04_properlyClosedDoubleQuote_stillParsesAsQuotedLiteral() {
		var node = parse("\"a*b\"");
		assertBean(node, "value,quoted", "a*b,true");
	}

	@Test
	void g05_properlyClosedSingleQuote_stillParsesAsQuotedLiteral() {
		var node = parse("'a*b'");
		assertBean(node, "value,quoted", "a*b,true");
	}

	@Test
	void g06_doubledQuoteEscapeInsideProperlyClosedLiteral_stillDecodesCorrectly() {
		var node = parse("\"a\"\"b\"");
		assertEquals("a\"b", node.value());
	}

	@Test
	void g07_emptyQuotedLiteral_isNotUnterminated() {
		var node = parse("\"\"");
		assertBean(node, "value,quoted", ",true");
	}
}
