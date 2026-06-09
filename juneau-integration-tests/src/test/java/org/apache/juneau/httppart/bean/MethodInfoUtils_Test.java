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
package org.apache.juneau.httppart.bean;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MethodInfoUtils} package-private validation helpers.
 *
 * <p>These helpers are reachable through the public bean-marshaller pipelines (RequestBeanMeta,
 * ResponseBeanMeta) but are tested here directly since they are package-private and only the
 * exceptional branches matter for coverage.</p>
 */
class MethodInfoUtils_Test extends TestBase {

	// --------------------------------------------------------------------------------
	// Fixtures
	// --------------------------------------------------------------------------------

	@SuppressWarnings({
		"unused"  // Unused in this context; kept for API consistency or future use.
	})
	static class Fixtures {
		// One-arg matching methods
		public void oneStringArg(String x) { /* no-op */ }
		public void oneIntArg(int x) { /* no-op */ }

		// Multi-arg method
		public void twoArgs(String a, String b) { /* no-op */ }

		// No-arg method
		public void noArgs() { /* no-op */ }

		// Return-type methods
		public void returnsVoid() { /* no-op */ }
		public String returnsString() { return ""; }
		public int returnsInt() { return 0; }
	}

	private static MethodInfo m(String name, Class<?>... params) throws Exception {
		return MethodInfo.of(Fixtures.class.getMethod(name, params));
	}

	// --------------------------------------------------------------------------------
	// assertArgType
	// --------------------------------------------------------------------------------

	@Test
	void a01_assertArgType_matchingType_returns() throws Exception {
		// Should not throw — String matches one of the allowed types.
		MethodInfoUtils.assertArgType(m("oneStringArg", String.class), Query.class, String.class, Integer.class);
	}

	@Test
	void a02_assertArgType_matchingTypeSecond_returns() throws Exception {
		// Loop iterates and second class matches.
		MethodInfoUtils.assertArgType(m("oneIntArg", int.class), Query.class, String.class, int.class);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void a03_assertArgType_wrongType_throws() {
		// Loop completes without match -> throws.
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertArgType(m("oneStringArg", String.class), Query.class, Integer.class, Long.class));
		assertTrue(thrown.getMessage().contains("Invalid return type"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void a04_assertArgType_zeroParams_throws() {
		// params.size() == 0 != 1 -> throws.
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertArgType(m("noArgs"), Query.class, String.class));
		assertTrue(thrown.getMessage().contains("Only one parameter"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void a05_assertArgType_multipleParams_throws() {
		// params.size() == 2 != 1 -> throws.
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertArgType(m("twoArgs", String.class, String.class), Header.class, String.class));
		assertTrue(thrown.getMessage().contains("Only one parameter"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void a06_assertArgType_emptyAllowed_throws() {
		// No allowed classes -> always throws.
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertArgType(m("oneStringArg", String.class), Query.class));
		assertTrue(thrown.getMessage().contains("Invalid return type"));
	}

	// --------------------------------------------------------------------------------
	// assertNoArgs
	// --------------------------------------------------------------------------------

	@Test
	void b01_assertNoArgs_zeroParams_returns() throws Exception {
		MethodInfoUtils.assertNoArgs(m("noArgs"), Query.class);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void b02_assertNoArgs_hasParams_throws() {
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertNoArgs(m("oneStringArg", String.class), Query.class));
		assertTrue(thrown.getMessage().contains("cannot have arguments"));
	}

	// --------------------------------------------------------------------------------
	// assertReturnNotVoid
	// --------------------------------------------------------------------------------

	@Test
	void c01_assertReturnNotVoid_nonVoid_returns() throws Exception {
		MethodInfoUtils.assertReturnNotVoid(m("returnsString"), Query.class);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void c02_assertReturnNotVoid_void_throws() {
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertReturnNotVoid(m("returnsVoid"), Query.class));
		assertTrue(thrown.getMessage().contains("Invalid return type"));
	}

	// --------------------------------------------------------------------------------
	// assertReturnType
	// --------------------------------------------------------------------------------

	@Test
	void d01_assertReturnType_matching_returns() throws Exception {
		MethodInfoUtils.assertReturnType(m("returnsString"), Query.class, String.class);
	}

	@Test
	void d02_assertReturnType_matchingSecond_returns() throws Exception {
		// Loop matches second class.
		MethodInfoUtils.assertReturnType(m("returnsInt"), Query.class, String.class, int.class);
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void d03_assertReturnType_noMatch_throws() {
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertReturnType(m("returnsString"), Query.class, Integer.class, Long.class));
		assertTrue(thrown.getMessage().contains("Invalid return type"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test
	void d04_assertReturnType_emptyAllowed_throws() {
		var thrown = assertThrows(InvalidAnnotationException.class,
			() -> MethodInfoUtils.assertReturnType(m("returnsString"), Query.class));
		assertTrue(thrown.getMessage().contains("Invalid return type"));
	}

	// --------------------------------------------------------------------------------
	// Sanity: explicit annotation type to match the method signature contract
	// --------------------------------------------------------------------------------

	@Test
	void e01_assertArgType_acceptsAnyAnnotationType() throws Exception {
		// Confirms the Class<? extends Annotation> bound works with arbitrary annotation types.
		Class<? extends Annotation> a = Header.class;
		MethodInfoUtils.assertArgType(m("oneStringArg", String.class), a, String.class);
	}
}
