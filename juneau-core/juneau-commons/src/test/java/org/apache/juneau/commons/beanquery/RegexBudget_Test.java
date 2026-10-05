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

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class RegexBudget_Test extends TestBase {

	private static SearchExpression regex(String args) {
		return SearchExpressionParser.parse("$regex(" + args + ")", SearchOperatorSet.standard());
	}

	private static RegexBudget budget() {
		return new RegexBudget(Duration.ofSeconds(10));
	}

	@Test
	void a01_defaultIsCaseInsensitiveFullMatch() {
		var b = budget();
		var n = regex("al.*");
		assertTrue(b.matches(n, "Alice"));
		assertFalse(b.matches(n, "Bob"));
		assertFalse(b.matches(regex("lic"), "Alice"));  // Full-string match, not find().
	}

	@Test
	void a02_explicitFlagsReplaceDefault() {
		var b = budget();
		var n = regex("al.*,flags=m");
		assertFalse(b.matches(n, "Alice"));
		assertTrue(b.matches(n, "alice"));
		assertTrue(b.matches(regex("a.b,flags=is"), "A\nB"));
	}

	@Test
	void a03_invalidPatternMatchesNothing() {
		assertFalse(budget().matches(regex("+a"), "a"));  // Dangling meta character.
	}

	@Test
	void a04_deadlineExceeded() {
		var b = new RegexBudget(Duration.ofNanos(1));
		var n = regex("a*b");
		var input = "a".repeat(10_000);
		assertThrowsWithMessage(BeanQuerySyntaxException.class, "Regex search took too long.", () -> b.matches(n, input));
	}
}
