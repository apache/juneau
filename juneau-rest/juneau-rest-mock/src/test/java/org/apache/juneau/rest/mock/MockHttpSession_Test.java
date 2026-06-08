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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Coverage tests for {@link MockHttpSession} — fluent setters, attribute API, and
 * the {@code HttpSession} method passthroughs.
 */
class MockHttpSession_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A. Fluent setters round-trip through getters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_creationTime_setterAndGetter() {
		var s = MockHttpSession.create().creationTime(123L);
		assertEquals(123L, s.getCreationTime());
	}

	@Test void a02_id_setterAndGetter() {
		var s = MockHttpSession.create().id("abc");
		assertEquals("abc", s.getId());
	}

	@Test void a03_lastAccessedTime_setterAndGetter() {
		var s = MockHttpSession.create().lastAccessedTime(456L);
		assertEquals(456L, s.getLastAccessedTime());
	}

	@Test void a04_maxInactiveInterval_setterAndGetter() {
		var s = MockHttpSession.create().maxInactiveInterval(30);
		assertEquals(30, s.getMaxInactiveInterval());
	}

	@Test void a05_isNew_setterAndGetter() {
		var s = MockHttpSession.create().isNew(true);
		assertTrue(s.isNew());
	}

	@Test void a06_setMaxInactiveInterval_overridesFluent() {
		// HttpSession.setMaxInactiveInterval is the standard non-fluent setter.
		var s = MockHttpSession.create().maxInactiveInterval(10);
		s.setMaxInactiveInterval(99);
		assertEquals(99, s.getMaxInactiveInterval());
	}

	@Test void a07_servletContext_setterAndGetter() {
		// Use a stub ServletContext via a Mockito-free dynamic proxy isn't worth it — just pass null and verify round-trip.
		var s = MockHttpSession.create().servletContext((ServletContext) null);
		assertNull(s.getServletContext());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. Attribute API.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_setAttribute_thenGet() {
		var s = MockHttpSession.create();
		s.setAttribute("k", "v");
		assertEquals("v", s.getAttribute("k"));
	}

	@Test void b02_getAttribute_unknown_returnsNull() {
		assertNull(MockHttpSession.create().getAttribute("nope"));
	}

	@Test void b03_removeAttribute_clearsIt() {
		var s = MockHttpSession.create();
		s.setAttribute("k", "v");
		s.removeAttribute("k");
		assertNull(s.getAttribute("k"));
	}

	@Test void b04_getAttributeNames_enumeratesKeys() {
		var s = MockHttpSession.create();
		s.setAttribute("a", 1);
		s.setAttribute("b", 2);
		var names = s.getAttributeNames();
		assertNotNull(names);
		var seenA = false;
		var seenB = false;
		while (names.hasMoreElements()) {
			var n = names.nextElement();
			if ("a".equals(n)) seenA = true;
			if ("b".equals(n)) seenB = true;
		}
		assertTrue(seenA);
		assertTrue(seenB);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. invalidate() is a documented no-op — just verify it doesn't throw.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_invalidate_isNoOp() {
		var s = MockHttpSession.create();
		s.setAttribute("k", "v");
		assertDoesNotThrow(s::invalidate);
		// Mock implementation does NOT clear the attributes — verify that contract.
		assertEquals("v", s.getAttribute("k"));
	}
}
