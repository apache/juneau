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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.junit.bct.NestedTokenizer.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.junit.bct.NestedTokenizer.*;
import org.junit.jupiter.api.*;

/**
 * Comprehensive unit tests for {@link NestedTokenizer} class.
 *
 * <p>Tests cover all aspects of the state machine parser including:</p>
 * <ul>
 *    <li>Simple token parsing</li>
 *    <li>Nested token structures</li>
 *    <li>Escape sequence handling</li>
 *    <li>Deep nesting scenarios</li>
 *    <li>Edge cases and error conditions</li>
 *    <li>Token object functionality</li>
 * </ul>
 */
class NestedTokenizer_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tokenization tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_simpleTokens() {
		new NestedTokenizer();

		// Single token
		var tokens = tokenize("foo");
		assertList(tokens, token("foo"));

		// Multiple tokens
		tokens = tokenize("foo,bar,baz");
		assertList(tokens, token("foo"), token("bar"), token("baz"));

		// Tokens with whitespace
		tokens = tokenize("  foo  ,  bar  ,  baz  ");
		assertList(tokens, token("foo"), token("bar"), token("baz"));
	}

	@Test void a02_nestedTokens() {
		// Simple nested structure
		var tokens = tokenize("foo{a,b}");
		assertEquals(1, tokens.size());
		assertToken(tokens.get(0), "foo", "a", "b");

		// Multiple tokens with nesting
		tokens = tokenize("foo{a,b},bar{c,d}");
		assertEquals(2, tokens.size());
		assertToken(tokens.get(0), "foo", "a", "b");
		assertToken(tokens.get(1), "bar", "c", "d");

		// Empty nested content
		tokens = tokenize("foo{}");
		assertEquals(1, tokens.size());
		assertToken(tokens.get(0), "foo");
	}

	@Test void a03_deepNesting() {
		// Two levels deep
		var tokens = tokenize("root{level1{a,b},level2}");
		assertEquals(1, tokens.size());
		var root = tokens.get(0);
		assertEquals("root", root.getValue());
		assertEquals(2, root.getNested().size());
		assertToken(root.getNested().get(0), "level1", "a", "b");
		assertToken(root.getNested().get(1), "level2");

		// Three levels deep
		tokens = tokenize("root{level1{level2{a,b}}}");
		assertEquals(1, tokens.size());
		root = tokens.get(0);
		assertEquals("root", root.getValue());
		assertEquals(1, root.getNested().size());
		var level1 = root.getNested().get(0);
		assertEquals("level1", level1.getValue());
		assertEquals(1, level1.getNested().size());
		assertToken(level1.getNested().get(0), "level2", "a", "b");
	}

	@Test void a04_escapeSequences() {
		// Escaped comma
		var tokens = tokenize("foo\\,bar");
		assertList(tokens, token("foo,bar"));

		// Escaped braces
		tokens = tokenize("foo\\{bar\\}");
		assertList(tokens, token("foo{bar}"));

		// Escaped backslash
		tokens = tokenize("foo\\\\bar");
		assertList(tokens, token("foo\\bar"));

		// Multiple escapes
		tokens = tokenize("foo\\,bar\\{baz\\}");
		assertList(tokens, token("foo,bar{baz}"));

		// Escape in nested content
		tokens = tokenize("root{foo\\,bar,baz}");
		assertEquals(1, tokens.size());
		assertToken(tokens.get(0), "root", "foo,bar", "baz");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Complex scenarios
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_complexNestedStructures() {
		// Real-world example: user configuration
		var tokens = tokenize("user{name,email,address{street,city,zipcode{main,plus4}}},config{timeout,retries}");
		assertEquals(2, tokens.size());

		// Validate user token
		var user = tokens.get(0);
		assertEquals("user", user.getValue());
		assertEquals(3, user.getNested().size());
		assertEquals("name", user.getNested().get(0).getValue());
		assertEquals("email", user.getNested().get(1).getValue());

		var address = user.getNested().get(2);
		assertEquals("address", address.getValue());
		assertEquals(3, address.getNested().size());
		assertEquals("street", address.getNested().get(0).getValue());
		assertEquals("city", address.getNested().get(1).getValue());

		var zipcode = address.getNested().get(2);
		assertEquals("zipcode", zipcode.getValue());
		assertEquals(2, zipcode.getNested().size());
		assertEquals("main", zipcode.getNested().get(0).getValue());
		assertEquals("plus4", zipcode.getNested().get(1).getValue());

		// Validate config token
		var config = tokens.get(1);
		assertToken(config, "config", "timeout", "retries");
	}

	@Test void b02_mixedEscapingAndNesting() {
		// Escaped characters within nested structures
		var tokens = tokenize("data{key\\,name,value\\{test\\}},info{desc\\,important}");
		assertEquals(2, tokens.size());

		assertToken(tokens.get(0), "data", "key,name", "value{test}");
		assertToken(tokens.get(1), "info", "desc,important");
	}

	@Test void b03_extremeNesting() {
		// Very deep nesting
		var tokens = tokenize("l1{l2{l3{l4{l5{value}}}}}");
		assertEquals(1, tokens.size());

		var current = tokens.get(0);
		for (int i = 1; i <= 5; i++) {
			assertEquals("l" + i, current.getValue());
			assertEquals(1, current.getNested().size());
			current = current.getNested().get(0);
		}
		assertEquals("value", current.getValue());
		assertFalse(current.hasNested());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases and error conditions
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_edgeCases() {
		// Single character
		var tokens = tokenize("a");
		assertList(tokens, token("a"));

		// Just comma
		tokens = tokenize(",");
		assertEquals(2, tokens.size());
		assertEquals("", tokens.get(0).getValue());
		assertEquals("", tokens.get(1).getValue());

		// Multiple commas
		tokens = tokenize("a,,b");
		assertEquals(3, tokens.size());
		assertEquals("a", tokens.get(0).getValue());
		assertEquals("", tokens.get(1).getValue());
		assertEquals("b", tokens.get(2).getValue());

		// Trailing comma
		tokens = tokenize("a,b,");
		assertEquals(3, tokens.size());
		assertEquals("a", tokens.get(0).getValue());
		assertEquals("b", tokens.get(1).getValue());
		assertEquals("", tokens.get(2).getValue());

		// Leading comma
		tokens = tokenize(",a,b");
		assertEquals(3, tokens.size());
		assertEquals("", tokens.get(0).getValue());
		assertEquals("a", tokens.get(1).getValue());
		assertEquals("b", tokens.get(2).getValue());
	}

	@Test void c02_whitespaceHandling() {
		// Various whitespace scenarios
		var tokens = tokenize("  a  ,  b  ");
		assertList(tokens, token("a"), token("b"));

		// Tabs and newlines
		tokens = tokenize("\ta\t,\nb\n");
		assertList(tokens, token("a"), token("b"));

		// Whitespace in nested content
		tokens = tokenize("root{  a  ,  b  }");
		assertToken(tokens.get(0), "root", "a", "b");

		// Whitespace around braces
		tokens = tokenize("root  {  a,b  }  ,  other");
		assertEquals(2, tokens.size());
		assertToken(tokens.get(0), "root", "a", "b");
		assertToken(tokens.get(1), "other");
	}

	@Test void c03_errorConditions() {
		// Null input
		assertThrows(IllegalArgumentException.class, () -> tokenize(null));

		// Empty input
		assertThrows(IllegalArgumentException.class, () -> tokenize(""));

		// Blank input
		assertThrows(IllegalArgumentException.class, () -> tokenize("   "));
	}

	@Test void c04_finalTokenLogic() {
		// Test line 136: final token addition logic

		// Case 1: Empty final value with trailing comma (lastWasComma = true)
		var tokens = tokenize("a,");
		assertEquals(2, tokens.size());
		assertEquals("a", tokens.get(0).getValue());
		assertEquals("", tokens.get(1).getValue()); // Empty token added due to trailing comma

		// Case 2: No tokens yet and empty input should create one empty token
		// This is handled by error conditions, but let's test a whitespace-only case after comma
		tokens = tokenize(",   ");
		assertEquals(2, tokens.size());
		assertEquals("", tokens.get(0).getValue());
		assertEquals("", tokens.get(1).getValue()); // Empty final value but added because of lastWasComma

		// Case 3: Non-empty final value should always be added
		tokens = tokenize("a,b");
		assertEquals(2, tokens.size());
		assertEquals("a", tokens.get(0).getValue());
		assertEquals("b", tokens.get(1).getValue());

		// Case 4: Test with nested content and trailing comma
		tokens = tokenize("root{a,},next");
		assertEquals(2, tokens.size());
		assertToken(tokens.get(0), "root", "a", ""); // Empty token in nested due to trailing comma
		assertEquals("next", tokens.get(1).getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Token object tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_tokenConstruction() {
		// Normal construction
		var token = new Token("test");
		assertEquals("test", token.getValue());
		assertFalse(token.hasNested());
		assertTrue(token.getNested().isEmpty());

		// Null value handling
		token = new Token(null);
		assertEquals("", token.getValue());
		assertFalse(token.hasNested());
	}

	@Test void d02_tokenEquality() {
		// Simple tokens
		var token1 = new Token("test");
		var token2 = new Token("test");
		var token3 = new Token("other");

		assertEquals(token1, token2);
		assertNotEquals(token1, token3);
		assertEquals(token1.hashCode(), token2.hashCode());

		// Tokens with nested content
		var nested1 = new Token("parent");
		nested1.setNested(Arrays.asList(new Token("child1"), new Token("child2")));

		var nested2 = new Token("parent");
		nested2.setNested(Arrays.asList(new Token("child1"), new Token("child2")));

		assertEquals(nested1, nested2);
		assertEquals(nested1.hashCode(), nested2.hashCode());

		// Different nested content
		var nested3 = new Token("parent");
		nested3.setNested(Arrays.asList(new Token("child1"), new Token("different")));

		assertNotEquals(nested1, nested3);
	}

	@Test void d06_tokenEqualsEdgeCases() {
		// Test line 229: equals() method edge cases
		var token = new Token("test");

		// Case 1: Self equality
		assertEquals(token, token);

		// Case 2: Null comparison
		assertNotEquals(token, null);

		// Case 3: Different object type
		assertNotEquals(token, "not a token");
		assertNotEquals(token, Integer.valueOf(42));

		// Case 4: Different value, same nested (null)
		var other = new Token("different");
		assertNotEquals(token, other);

		// Case 5: Same value, different nested content
		var token1 = new Token("same");
		var token2 = new Token("same");
		token1.setNested(Arrays.asList(new Token("child1")));
		token2.setNested(Arrays.asList(new Token("child2")));
		assertNotEquals(token1, token2);

		// Case 6: Same value, one has nested, other doesn't
		var token3 = new Token("same");
		var token4 = new Token("same");
		token3.setNested(Arrays.asList(new Token("child")));
		// token4 has no nested content
		assertNotEquals(token3, token4);

		// Case 7: Both have null nested
		var token5 = new Token("same");
		var token6 = new Token("same");
		token5.setNested(null);
		token6.setNested(null);
		assertEquals(token5, token6);
	}

	@Test void d03_tokenToString() {
		// Simple token
		var token = new Token("test");
		assertEquals("test", token.toString());

		// Token with nested content
		token = new Token("parent");
		token.setNested(Arrays.asList(new Token("child1"), new Token("child2")));
		assertEquals("parent{child1,child2}", token.toString());

		// Deep nesting
		var child = new Token("child");
		child.setNested(Arrays.asList(new Token("grandchild")));
		token = new Token("parent");
		token.setNested(Arrays.asList(child));
		assertEquals("parent{child{grandchild}}", token.toString());
	}

	@Test void d04_tokenNestedAccess() {
		var parent = new Token("parent");

		// Initially no nested content
		assertFalse(parent.hasNested());
		assertTrue(parent.getNested().isEmpty());

		// Add nested content
		parent.setNested(Arrays.asList(new Token("child1"), new Token("child2")));
		assertTrue(parent.hasNested());
		assertEquals(2, parent.getNested().size());

		// Verify unmodifiable
		var nested = parent.getNested();
		assertThrows(UnsupportedOperationException.class, () -> nested.add(new Token("child3")));
	}

	@Test void d05_hasNestedEdgeCases() {
		// Test line 201: hasNested() method edge cases
		var token = new Token("test");

		// Case 1: null nested list
		token.setNested(null);
		assertFalse(token.hasNested()); // Should return false when nested is null

		// Case 2: empty nested list
		token.setNested(new ArrayList<>());
		assertFalse(token.hasNested()); // Should return false when nested is empty

		// Case 3: non-empty nested list
		token.setNested(Arrays.asList(new Token("child")));
		assertTrue(token.hasNested()); // Should return true when nested has content

		// Case 4: nested list with multiple items
		token.setNested(Arrays.asList(new Token("child1"), new Token("child2")));
		assertTrue(token.hasNested()); // Should return true when nested has multiple items
	}

	//------------------------------------------------------------------------------------------------------------------
	// Integration and round-trip tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_roundTripTests() {
		// Simple cases
		assertRoundTrip("foo");
		assertRoundTrip("foo,bar,baz");

		// Nested cases
		assertRoundTrip("foo{a,b}");
		assertRoundTrip("foo{a,b},bar{c,d}");

		// Deep nesting
		assertRoundTrip("root{level1{level2{a,b}}}");

		// Complex real-world case
		assertRoundTrip("user{name,email},config{timeout,retries}");
	}

	@Test void e02_performanceTest() {
		// Test with large input to ensure reasonable performance
		var sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			if (i > 0) sb.append(",");
			sb.append("token").append(i);
			if (i % 10 == 0) {
				sb.append("{nested").append(i).append(",value").append(i).append("}");
			}
		}

		var start = System.currentTimeMillis();
		var tokens = tokenize(sb.toString());
		var elapsed = System.currentTimeMillis() - start;

		assertTrue(tokens.size() > 900); // Should have many tokens
		assertTrue(elapsed < 1000); // Should complete within 1 second
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a simple token for testing.
	 */
	private static Token token(String value) {
		return new Token(value);
	}

	/**
	 * Asserts that a token has the expected value and nested tokens.
	 */
	private static void assertToken(Token actual, String expectedValue, String... expectedNested) {
		assertEquals(expectedValue, actual.getValue());
		if (expectedNested.length == 0) {
			assertFalse(actual.hasNested());
		} else {
			assertTrue(actual.hasNested());
			assertEquals(expectedNested.length, actual.getNested().size());
			for (int i = 0; i < expectedNested.length; i++) {
				assertEquals(expectedNested[i], actual.getNested().get(i).getValue());
			}
		}
	}

	/**
	 * Tests that parsing and toString are inverse operations.
	 */
	private static void assertRoundTrip(String input) {
		var tokens = tokenize(input);
		var rebuilt = tokens.stream()
			.map(Token::toString)
			.collect(java.util.stream.Collectors.joining(","));
		assertEquals(input, rebuilt);
	}
}