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

import java.math.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class SearchExpression_Test extends TestBase {

	private static final SearchOperatorSet OPS = SearchOperatorSet.standard();

	private static SearchExpression parse(String s) {
		return SearchExpressionParser.parse(s, OPS);
	}

	@Test void a01_literal_untyped_typedValueIsText() {
		assertEquals("Bill", parse("Bill").typedValue());
	}

	@Test void a02_withTypedValue_returnsCopyNotMutation() {
		var e = parse("42");
		var typed = e.withTypedValue(new BigDecimal("42"));
		assertEquals("42", e.typedValue());
		assertEquals(new BigDecimal("42"), typed.typedValue());
		assertEquals("42", typed.value());
	}

	@Test void a03_withTypedValue_preservesQuoted() {
		var e = parse("\"a*\"");
		assertTrue(e.isQuoted());
		assertTrue(e.withTypedValue("x").isQuoted());
	}

	@Test void a04_withTypedValue_rejectsNull() {
		var e = parse("42");
		assertThrows(NullPointerException.class, () -> e.withTypedValue(null));
	}

	@Test void a05_function_typedArgs_mapsEachArgsTypedValue() {
		var e = parse("$eq(42)");
		var typedArg = e.args().get(0).withTypedValue(new BigDecimal("42"));
		assertEquals(List.of(new BigDecimal("42")), SearchExpression.func(e.operator(), List.of(typedArg)).typedArgs());
	}

	@Test void a06_function_typedArgs_untypedFallsBackToLiteralText() {
		assertEquals(List.of("a", "b", "c"), parse("$in(a,b,c)").typedArgs());
	}

	@Test void a07_between_typedArgs_bothArguments() {
		var e = parse("$between(1,100)");
		var typed = SearchExpression.func(e.operator(), List.of(
			e.args().get(0).withTypedValue(new BigDecimal("1")),
			e.args().get(1).withTypedValue(new BigDecimal("100"))));
		assertEquals(List.of(new BigDecimal("1"), new BigDecimal("100")), typed.typedArgs());
	}

	@Test void a08_withTypedValue_onFunction_throws() {
		var e = parse("$eq(42)");
		var hoistedArg1 = new BigDecimal("42");
		assertThrows(IllegalStateException.class, () -> e.withTypedValue(hoistedArg1));
	}

	@Test void a09_typedArgs_nestedFunction_contributesName() {
		var e = parse("$and($eq(1),$gt(2))");
		assertList(e.typedArgs(), "$eq", "$gt");
		assertList(e.literalArgs(), "$eq", "$gt");
		assertEquals(e.argCount(), e.typedArgs().size());
	}

	@Test void a10_argLists_areImmutable() {
		var e = parse("$in(a,b)");
		var hoistedTarget1 = e.literalArgs();
		assertThrows(UnsupportedOperationException.class, () -> hoistedTarget1.add("c"));
		var hoistedTarget2 = e.typedArgs();
		assertThrows(UnsupportedOperationException.class, () -> hoistedTarget2.add("c"));
		var hoistedTarget3 = e.args();
		assertThrows(UnsupportedOperationException.class, () -> hoistedTarget3.add(e));
	}

	@Test
	@SuppressWarnings({
		"java:S3415" // Deliberate: the parsed expression must be the equals() receiver when compared to a String.
	})
	void a11_equalsHashCode_valueBased() {
		assertEquals(parse("$eq(42)"), parse("$eq(42)"));
		assertEquals(parse("$eq(42)").hashCode(), parse("$eq(42)").hashCode());
		assertNotEquals(parse("$eq(42)"), parse("$eq(43)"));
		assertNotEquals(parse("$eq(42)"), parse("$ne(42)"));
		assertNotEquals(parse("42"), "42");
	}

	@Test void a12_equalsHashCode_quotedAndTyped() {
		assertEquals(parse("\"a\""), parse("\"a\""));
		assertNotEquals(parse("\"a\""), parse("a"));
		var typed = parse("42").withTypedValue(new BigDecimal("42"));
		assertEquals(typed, parse("42").withTypedValue(new BigDecimal("42")));
		assertEquals(typed.hashCode(), parse("42").withTypedValue(new BigDecimal("42")).hashCode());
		assertNotEquals(typed, parse("42"));
		assertNotEquals(typed, parse("42").withTypedValue(new BigDecimal("43")));
	}
}
