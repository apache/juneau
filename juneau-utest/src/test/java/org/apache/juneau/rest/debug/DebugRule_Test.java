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
package org.apache.juneau.rest.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link DebugRule} and {@link DebugRule.Builder}.
 *
 * <p>Exercises the builder fluent setters (always/never/conditional/format/level/cacheBodies),
 * accessors (getFormat/getLevel/getCacheBodies), the {@link DebugRule#isEnabled} predicate
 * matching method, and {@link DebugRule#toString()}.
 */
class DebugRule_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a. Builder factory and defaults.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_create_returnsBuilder() {
		var b = DebugRule.create();
		assertNotNull(b);
		assertInstanceOf(DebugRule.Builder.class, b);
	}

	@Test void a02_default_isDisabled() {
		var rule = DebugRule.create().build();
		assertFalse(rule.isEnabled(MockServletRequest.create("GET", "/")));
		assertNull(rule.getFormat());
		assertNull(rule.getLevel());
		assertNull(rule.getCacheBodies());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. always() / never() / conditional() shortcuts.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_always_enablesForAllRequests() {
		var b = DebugRule.create();
		assertSame(b, b.always());
		var rule = b.build();
		assertTrue(rule.isEnabled(MockServletRequest.create("GET", "/foo")));
		assertTrue(rule.isEnabled(MockServletRequest.create("POST", "/bar")));
	}

	@Test void b02_never_disablesForAllRequests() {
		var b = DebugRule.create().always().never();
		assertFalse(b.build().isEnabled(MockServletRequest.create("GET", "/")));
	}

	@Test void b03_conditional_appliesPredicate() {
		Predicate<HttpServletRequest> isPost = r -> "POST".equalsIgnoreCase(r.getMethod());
		var rule = DebugRule.create().conditional(isPost).build();
		assertTrue(rule.isEnabled(MockServletRequest.create("POST", "/foo")));
		assertFalse(rule.isEnabled(MockServletRequest.create("GET", "/foo")));
	}

	@Test void b04_conditional_nullPredicate_treatedAsFalse() {
		var rule = DebugRule.create().conditional(null).build();
		// Even though the enabled flag is set, the null-predicate replacement returns false.
		assertFalse(rule.isEnabled(MockServletRequest.create("GET", "/")));
	}

	@Test void b05_isEnabled_requiresBothFlagAndPredicate() {
		// never() sets enabled=false, so even if a passing predicate is later applied via conditional we still need enabled true.
		var rule = DebugRule.create().always().build();
		assertTrue(rule.isEnabled(MockServletRequest.create("GET", "/")));

		// Calling never() after always() flips both fields.
		var rule2 = DebugRule.create().always().never().build();
		assertFalse(rule2.isEnabled(MockServletRequest.create("GET", "/")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. format / level / cacheBodies fluent setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_format_setterAndGetter() {
		DebugFormat fmt = ctx -> "fmt";
		var b = DebugRule.create();
		assertSame(b, b.format(fmt));
		assertSame(fmt, b.build().getFormat());
	}

	@Test void c02_level_setterAndGetter() {
		var b = DebugRule.create();
		assertSame(b, b.level(Level.WARNING));
		assertEquals(Level.WARNING, b.build().getLevel());
	}

	@Test void c03_cacheBodies_setterAndGetter() {
		var b = DebugRule.create();
		assertSame(b, b.cacheBodies(true));
		assertEquals(Boolean.TRUE, b.build().getCacheBodies());

		var b2 = DebugRule.create().cacheBodies(false);
		assertEquals(Boolean.FALSE, b2.build().getCacheBodies());

		var b3 = DebugRule.create().cacheBodies(null);
		assertNull(b3.build().getCacheBodies());
	}

	@Test void c04_chainedBuilder() {
		DebugFormat fmt = ctx -> "x";
		var rule = DebugRule.create()
			.always()
			.format(fmt)
			.level(Level.FINE)
			.cacheBodies(true)
			.build();

		assertTrue(rule.isEnabled(MockServletRequest.create("GET", "/")));
		assertSame(fmt, rule.getFormat());
		assertEquals(Level.FINE, rule.getLevel());
		assertEquals(Boolean.TRUE, rule.getCacheBodies());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. toString().
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_toString_disabled_nullFormat() {
		var s = DebugRule.create().build().toString();
		assertTrue(s.contains("enabled=false"));
		assertTrue(s.contains("format=null"));
		assertTrue(s.contains("level=null"));
		assertTrue(s.contains("cacheBodies=null"));
	}

	@Test void d02_toString_enabled_withFormat() {
		DebugFormat fmt = ctx -> "x";
		var s = DebugRule.create()
			.always()
			.format(fmt)
			.level(Level.INFO)
			.cacheBodies(true)
			.build()
			.toString();
		assertTrue(s.contains("enabled=true"));
		assertTrue(s.contains("level=INFO"));
		assertTrue(s.contains("cacheBodies=true"));
		assertTrue(s.contains("format="));
		// format value is the class name of the lambda; just ensure non-null literal isn't emitted.
		assertFalse(s.contains("format=null"));
	}
}
