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
package org.apache.juneau.microservice;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Tests for {@link ServletFilterRegistration}: builder validation, defaults and the order/collision rules applied by
 * {@link ServletFilterRegistration#collect(BeanStore)}.
 */
@SuppressWarnings({
	"java:S1192", // Repeated URL pattern literals keep each test self-contained and readable.
	"resource" // BasicBeanStore is a fluent AutoCloseable; the in-memory test stores hold nothing to close and addBean self-returns are not new resources.
})
class ServletFilterRegistration_Test extends TestBase {

	private static final Filter F = new PassThrough();

	public static class PassThrough implements Filter {
		@Override /* Filter */
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
			chain.doFilter(req, res);
		}
	}

	private static ServletFilterRegistration reg(int order, String...patterns) {
		return ServletFilterRegistration.create(new PassThrough()).urlPatterns(patterns).order(order).build();
	}

	private static AuthFilterChain auth(BeanStore store) {
		return AuthFilterChain.create(store).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - Defaults and accessors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_defaults() {
		var r = ServletFilterRegistration.create(F).urlPatterns("/a/*").build();
		assertSame(F, r.getFilter());
		assertEquals(List.of("/a/*"), r.getUrlPatterns());
		assertEquals(Set.of(DispatcherType.REQUEST), r.getDispatcherTypes());
		assertEquals(ServletFilterRegistration.DEFAULT_ORDER, r.getOrder());
		assertEquals(0, r.getOrder());
		assertTrue(r.isAsyncSupported());
		assertTrue(r.getName().isEmpty());
	}

	@Test void a02_dispatcherTypes_replacesDefault() {
		var r = ServletFilterRegistration.create(F).urlPatterns("/a/*").dispatcherTypes(DispatcherType.ASYNC).build();
		assertEquals(Set.of(DispatcherType.ASYNC), r.getDispatcherTypes());
	}

	@Test void a03_patterns_duplicatesDroppedFirstWins() {
		var r = ServletFilterRegistration.create(F).urlPatterns("/b/*", "/a/*", "/b/*").build();
		assertEquals(List.of("/b/*", "/a/*"), r.getUrlPatterns());
	}

	@Test void a04_collections_unmodifiable() {
		var r = ServletFilterRegistration.create(F).urlPatterns("/a/*").build();
		var patterns = r.getUrlPatterns();
		var types = r.getDispatcherTypes();
		assertThrows(UnsupportedOperationException.class, () -> patterns.add("/x"));
		assertThrows(UnsupportedOperationException.class, () -> types.add(DispatcherType.ERROR));
	}

	@Test void a05_optionsRoundTrip() {
		var r = ServletFilterRegistration.create(F).urlPatterns("/a/*").order(7).asyncSupported(false).name("n").build();
		assertEquals(7, r.getOrder());
		assertFalse(r.isAsyncSupported());
		assertEquals("n", r.getName().orElseThrow());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - Validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_nullFilter() {
		assertThrows(IllegalArgumentException.class, () -> ServletFilterRegistration.create(null));
	}

	@Test void b02_noPatterns() {
		var none = ServletFilterRegistration.create(F);
		var empty = ServletFilterRegistration.create(F).urlPatterns();
		assertThrows(IllegalArgumentException.class, none::build);
		assertThrows(IllegalArgumentException.class, empty::build);
	}

	@Test void b03_nullOrBlankPattern() {
		var nullPattern = ServletFilterRegistration.create(F).urlPatterns((String)null);
		var blankPattern = ServletFilterRegistration.create(F).urlPatterns("/a/*", " ");
		var tabPattern = ServletFilterRegistration.create(F).urlPatterns("\t");
		assertThrows(IllegalArgumentException.class, nullPattern::build);
		assertThrows(IllegalArgumentException.class, blankPattern::build);
		assertThrows(IllegalArgumentException.class, tabPattern::build);
	}

	@Test void b03a_emptyPattern_isTheContextRoot() {
		assertEquals(List.of(""), ServletFilterRegistration.create(F).urlPatterns("").build().getUrlPatterns());
	}

	@Test void b04_validPatterns() {
		for (var p : new String[]{"/*", "/a/*", "/a/b/*", "*.json", "/console", "/a/b.html"})
			assertEquals(List.of(p), ServletFilterRegistration.create(F).urlPatterns(p).build().getUrlPatterns(), p);
	}

	@Test void b05_invalidPatterns() {
		for (var p : new String[]{"/", "console/*", "/console*", "/a/*/b", "*.", "*.a/b", "a*.x", "regex:/a.*", "^/a", "uri-template:/a/{x}", "/a/**", "*", "*.*", "*.a*"}) {
			var b = ServletFilterRegistration.create(F).urlPatterns(p);
			var ex = assertThrows(IllegalArgumentException.class, b::build, p);
			assertTrue(ex.getMessage().contains("'" + p + "'"), "Message should name the pattern: " + ex.getMessage());
		}
	}

	@Test void b05a_blankName() {
		var b = ServletFilterRegistration.create(F).urlPatterns("/a/*");
		assertThrows(IllegalArgumentException.class, () -> b.name(""));
		assertThrows(IllegalArgumentException.class, () -> b.name("  "));
		assertTrue(b.name(null).build().getName().isEmpty());
	}

	@Test void b05b_nullStore() {
		assertThrows(IllegalArgumentException.class, () -> ServletFilterRegistration.collect(null));
	}

	@Test void b06_noDispatcherTypes() {
		var b = ServletFilterRegistration.create(F).urlPatterns("/a/*").dispatcherTypes();
		assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test void b07_nullDispatcherType() {
		var b = ServletFilterRegistration.create(F).urlPatterns("/a/*").dispatcherTypes(DispatcherType.REQUEST, null);
		assertThrows(IllegalArgumentException.class, b::build);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - collect(): order and tie-breaks
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_noBeans_empty() {
		assertTrue(ServletFilterRegistration.collect(new BasicBeanStore()).isEmpty());
	}

	@Test void c02_sortedByOrderThenName() {
		var store = new BasicBeanStore();
		var late = reg(5, "/*");
		var early = reg(-5, "/*");
		var tieB = reg(0, "/*");
		var tieA = reg(0, "/*");
		store.addBean(ServletFilterRegistration.class, late, "late");
		store.addBean(ServletFilterRegistration.class, tieB, "b");
		store.addBean(ServletFilterRegistration.class, early, "early");
		store.addBean(ServletFilterRegistration.class, tieA, "a");
		var l = ServletFilterRegistration.collect(store);
		assertEquals(List.of(early, tieA, tieB, late), l);
	}

	@Test void c03_authAutoMounted_atAuthOrder_onRequestOnly_async() {
		var store = new BasicBeanStore();
		var auth = auth(store);
		store.addBean(AuthFilterChain.class, auth);
		var l = ServletFilterRegistration.collect(store);
		assertEquals(1, l.size());
		var r = l.get(0);
		assertSame(auth, r.getFilter());
		assertEquals(ServletFilterRegistration.AUTH_ORDER, r.getOrder());
		assertEquals(List.of("/*"), r.getUrlPatterns());
		assertEquals(Set.of(DispatcherType.REQUEST), r.getDispatcherTypes());
		assertTrue(r.isAsyncSupported());
	}

	@Test void c04_authBeforeDefaultOrder_afterHighestPrecedence() {
		var store = new BasicBeanStore();
		var auth = auth(store);
		store.addBean(AuthFilterChain.class, auth);
		var first = reg(ServletFilterRegistration.HIGHEST_PRECEDENCE, "/console/*");
		var dflt = reg(ServletFilterRegistration.DEFAULT_ORDER, "/*");
		store.addBean(ServletFilterRegistration.class, dflt, "dflt");
		store.addBean(ServletFilterRegistration.class, first, "first");
		var l = ServletFilterRegistration.collect(store);
		assertSame(first, l.get(0));
		assertSame(auth, l.get(1).getFilter());
		assertSame(dflt, l.get(2));
	}

	@Test void c05_unnamedRegistrationAtAuthOrder_authWinsTie() {
		var store = new BasicBeanStore();
		var auth = auth(store);
		store.addBean(AuthFilterChain.class, auth);
		var user = reg(ServletFilterRegistration.AUTH_ORDER, "/*");
		store.addBean(ServletFilterRegistration.class, user);  // unnamed: bean name "" ties the auth entry on name
		var l = ServletFilterRegistration.collect(store);
		assertSame(auth, l.get(0).getFilter());
		assertSame(user, l.get(1));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - collect(): replace hook and collisions
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_replaceHook_registrationOfAuthInstanceReplacesAutoMount() {
		var store = new BasicBeanStore();
		var auth = auth(store);
		store.addBean(AuthFilterChain.class, auth);
		var mine = ServletFilterRegistration.create(auth).urlPatterns("/api/*").dispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD).order(3).build();
		store.addBean(ServletFilterRegistration.class, mine, "myAuth");
		var l = ServletFilterRegistration.collect(store);
		assertEquals(List.of(mine), l);
	}

	@Test void d02_differentAuthInstance_isASeparateFilter() {
		var store = new BasicBeanStore();
		var auth = auth(store);
		store.addBean(AuthFilterChain.class, auth);
		var other = ServletFilterRegistration.create(auth(store)).urlPatterns("/x/*").build();
		store.addBean(ServletFilterRegistration.class, other, "other");
		var l = ServletFilterRegistration.collect(store);
		assertEquals(2, l.size());
		assertSame(auth, l.get(0).getFilter());  // the automatic mount, at AUTH_ORDER, is still there
		assertSame(other, l.get(1));
	}

	@Test void d03_sameInstanceTwice_failsNamingBothBeans() {
		var store = new BasicBeanStore();
		store.addBean(ServletFilterRegistration.class, ServletFilterRegistration.create(F).urlPatterns("/a/*").build(), "one");
		store.addBean(ServletFilterRegistration.class, ServletFilterRegistration.create(F).urlPatterns("/b/*").build(), "two");
		var ex = assertThrows(IllegalStateException.class, () -> ServletFilterRegistration.collect(store));
		assertTrue(ex.getMessage().contains("'one'") && ex.getMessage().contains("'two'"), ex.getMessage());
	}

	@Test void d04_authInstanceTwice_fails() {
		var store = new BasicBeanStore();
		var auth = auth(store);
		store.addBean(AuthFilterChain.class, auth);
		store.addBean(ServletFilterRegistration.class, ServletFilterRegistration.create(auth).urlPatterns("/a/*").build(), "one");
		store.addBean(ServletFilterRegistration.class, ServletFilterRegistration.create(auth).urlPatterns("/b/*").build(), "two");
		assertThrows(IllegalStateException.class, () -> ServletFilterRegistration.collect(store));
	}

	@Test void d05_twoInstancesOfOneClass_overlappingPatterns_allowed() {
		var store = new BasicBeanStore();
		store.addBean(ServletFilterRegistration.class, reg(0, "/a/*"), "one");
		store.addBean(ServletFilterRegistration.class, reg(0, "/a/*", "/*"), "two");
		assertEquals(2, ServletFilterRegistration.collect(store).size());
	}

	@Test void d06_duplicateExplicitName_fails() {
		var store = new BasicBeanStore();
		store.addBean(ServletFilterRegistration.class, ServletFilterRegistration.create(new PassThrough()).urlPatterns("/a/*").name("dup").build(), "one");
		store.addBean(ServletFilterRegistration.class, ServletFilterRegistration.create(new PassThrough()).urlPatterns("/b/*").name("dup").build(), "two");
		var ex = assertThrows(IllegalStateException.class, () -> ServletFilterRegistration.collect(store));
		assertTrue(ex.getMessage().contains("dup"), ex.getMessage());
	}
}
