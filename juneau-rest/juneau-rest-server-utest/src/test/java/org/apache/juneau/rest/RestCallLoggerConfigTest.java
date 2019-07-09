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
package org.apache.juneau.rest;

import static org.junit.Assert.*;

import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestCallLoggerConfigTest {

	private String[] strings(String...s) {
		return s;
	}

	private MockServletRequest req() {
		return MockServletRequest.create();
	}

	private MockServletResponse res() {
		return MockServletResponse.create();
	}

	private MockServletResponse res(int status) {
		return MockServletResponse.create().status(status);
	}

	private RestCallLoggerConfig.Builder config() {
		return RestCallLoggerConfig.create();
	}

	private RestCallLoggerRule.Builder rule() {
		return RestCallLoggerRule.create();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basicMatching_noRules() {
		HttpServletRequest req = req();
		HttpServletResponse res = res();

		RestCallLoggerConfig lc = config().build();

		assertNull(lc.getRule(req, res));
	}

	@Test
	public void a02_basicMatching_codeMatchingRule() {
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		RestCallLoggerConfig lc =
			config()
			.rules(
				rule()
				.codes("200")
				.build()
			)
			.build();

		assertNotNull(lc.getRule(req, res));

		res.status(201);
		assertNull(lc.getRule(req, res));
	}

	@Test
	public void a03_basicMatching_exceptionMatchingRule() {
		MockServletRequest req = req();
		MockServletResponse res = res();

		RestCallLoggerConfig lc =
			config()
			.rule(
				rule()
				.exceptions("IndexOutOfBounds*")
				.build()
			)
			.build();

		assertNull(lc.getRule(req, res));

		req.attribute("Exception", new IndexOutOfBoundsException());
		assertNotNull(lc.getRule(req, res));
	}

	@Test
	public void a04_basicMatching_debugMatching() {
		MockServletRequest req = req();
		MockServletResponse res = res();

		RestCallLoggerConfig lc =
			config()
			.debug("per-request")
			.rule(
				rule()
				.exceptions("IndexOutOfBounds*")
				.debugOnly()
				.build()
			)
			.build();

		assertNull(lc.getRule(req, res));

		req.attribute("Exception", new IndexOutOfBoundsException());
		assertNull(lc.getRule(req, res));

		req.attribute("Debug", true);
		assertNotNull(lc.getRule(req, res));

		req.attribute("Debug", null);
		req.header("X-Debug", true);
		assertNotNull(lc.getRule(req, res));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parent matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_parentMatching() {
		MockServletRequest req = req();
		MockServletResponse res = res();

		RestCallLoggerConfig lc =
			config()
			.debug("per-request")
			.rule(
				rule()
				.exceptions("IndexOutOfBounds*")
				.debugOnly()
				.build()
			)
			.build();
		lc = RestCallLoggerConfig.create().parent(lc).build();

		assertNull(lc.getRule(req, res));

		req.attribute("Exception", new IndexOutOfBoundsException());
		assertNull(lc.getRule(req, res));

		req.attribute("Debug", true);
		assertNotNull(lc.getRule(req, res));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Disabled
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_disabled() {
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		RestCallLoggerConfig lc =
			config()
			.disabled()
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();

		assertNull(lc.getRule(req, res));
	}

	@Test
	public void c02_disabled_trueValues() {
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		for (String s : strings("true", "TRUE")) {
			RestCallLoggerConfig lc =
				config()
				.disabled(s)
				.rule(
					rule()
					.codes("*")
					.build()
				)
				.build();

			assertNull(lc.getRule(req, res));
		}
	}

	@Test
	public void c03_disabled_falseValues() {
		MockServletRequest req = req();
		MockServletResponse res = res(200);

		for (String s : strings("false", "FALSE", "foo", null)) {
			RestCallLoggerConfig lc =
				config()
				.disabled(s)
				.rule(
					rule()
					.codes("*")
					.build()
				)
				.build();

			assertNotNull(lc.getRule(req, res));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Debug
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_debugAlways() {
		MockServletRequest req = req();

		RestCallLoggerConfig lc =
			config()
			.debugAlways()
			.build();

		assertTrue(lc.isDebug(req));
	}

	@Test
	public void d02_debug_trueValues() {
		MockServletRequest req = req();

		for (String s : strings("always", "ALWAYS")) {
			RestCallLoggerConfig lc =
				config()
				.debug(s)
				.build();

			assertTrue(lc.isDebug(req));
		}
	}

	@Test
	public void d03_debug_falseValues() {
		MockServletRequest req = req();

		for (String s : strings("never", "NEVER", "foo", null)) {
			RestCallLoggerConfig lc =
				config()
				.debug(s)
				.build();

			assertFalse(lc.isDebug(req));
		}
	}

	@Test
	public void d04_debug_perRequest() {
		MockServletRequest req = req();
		MockServletRequest reqDebug = req().debug();
		MockServletRequest reqDebugAttrTrue = req().attribute("Debug", true);
		MockServletRequest reqDebugAttrFalse = req().attribute("Debug", false);
		MockServletRequest reqDebugAttrOther = req().attribute("Debug", "foo");

		for (String s : strings("per-request", "PER-REQUEST")) {
			RestCallLoggerConfig lc =
				config()
				.debug(s)
				.build();

			assertFalse(lc.isDebug(req));
			assertTrue(lc.isDebug(reqDebug));
			assertTrue(lc.isDebug(reqDebugAttrTrue));
			assertFalse(lc.isDebug(reqDebugAttrFalse));
			assertFalse(lc.isDebug(reqDebugAttrOther));
		}
	}

	@Test
	public void d05_debugPerRequest() {
		MockServletRequest req = req();
		MockServletRequest reqDebug = req().debug();
		MockServletRequest reqDebugAttrTrue = req().attribute("Debug", true);
		MockServletRequest reqDebugAttrFalse = req().attribute("Debug", false);
		MockServletRequest reqDebugAttrOther = req().attribute("Debug", "foo");

		RestCallLoggerConfig lc =
			config()
			.debugPerRequest()
			.build();

		assertFalse(lc.isDebug(req));
		assertTrue(lc.isDebug(reqDebug));
		assertTrue(lc.isDebug(reqDebugAttrTrue));
		assertFalse(lc.isDebug(reqDebugAttrFalse));
		assertFalse(lc.isDebug(reqDebugAttrOther));
	}

	//------------------------------------------------------------------------------------------------------------------
	// No-trace
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_noTraceAlways() {
		MockServletRequest req = req();
		MockServletResponse res = res();

		RestCallLoggerConfig lc =
			config()
			.noTraceAlways()
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();

		assertNull(lc.getRule(req, res));
	}

	@Test
	public void e02_noTrace_trueValues() {
		MockServletRequest req = req();
		MockServletResponse res = res();

		for (String s : strings("always", "ALWAYS")) {
			RestCallLoggerConfig lc =
				config()
				.noTrace(s)
				.rule(
					rule()
					.codes("*")
					.build()
				)
				.build();

			assertNull(lc.getRule(req, res));
		}
	}

	@Test
	public void e03_noTrace_falseValues() {
		MockServletRequest req = req();
		MockServletResponse res = res();

		for (String s : strings("never", "NEVER", "foo", null)) {
			RestCallLoggerConfig lc =
				config()
				.noTrace(s)
				.rule(
					rule()
					.codes("*")
					.build()
				)
				.build();

			assertNotNull(lc.getRule(req, res));
		}
	}

	@Test
	public void e04_noTrace_perRequest() {
		MockServletRequest req = req();
		MockServletRequest reqNoTrace = req().noTrace();
		MockServletRequest reqNoTraceAttrTrue = req().attribute("NoTrace", true);
		MockServletRequest reqNoTraceAttrFalse = req().attribute("NoTrace", false);
		MockServletRequest reqNoTraceAttrOther = req().attribute("NoTrace", "foo");
		MockServletResponse res = res();

		for (String s : strings("per-request", "PER-REQUEST")) {
			RestCallLoggerConfig lc =
				config()
				.noTrace(s)
				.rule(
					rule()
					.codes("*")
					.build()
				)
				.build();

			assertNotNull(lc.getRule(req, res));
			assertNull(lc.getRule(reqNoTrace, res));
			assertNull(lc.getRule(reqNoTraceAttrTrue, res));
			assertNotNull(lc.getRule(reqNoTraceAttrFalse, res));
			assertNotNull(lc.getRule(reqNoTraceAttrOther, res));
		}
	}

	@Test
	public void e05_noTracePerRequest() {
		MockServletRequest req = req();
		MockServletRequest reqNoTrace = req().noTrace();
		MockServletRequest reqNoTraceAttrTrue = req().attribute("NoTrace", true);
		MockServletRequest reqNoTraceAttrFalse = req().attribute("NoTrace", false);
		MockServletRequest reqNoTraceAttrOther = req().attribute("NoTrace", "foo");
		MockServletResponse res = res();

		RestCallLoggerConfig lc =
			config()
			.noTracePerRequest()
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();

		assertNotNull(lc.getRule(req, res));
		assertNull(lc.getRule(reqNoTrace, res));
		assertNull(lc.getRule(reqNoTraceAttrTrue, res));
		assertNotNull(lc.getRule(reqNoTraceAttrFalse, res));
		assertNotNull(lc.getRule(reqNoTraceAttrOther, res));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Use stack trace hashing
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_stackTraceHashing() {
		RestCallLoggerConfig lc =
			config()
			.stackTraceHashing()
			.build();

		assertTrue(lc.useStackTraceHashing());
		assertEquals(Integer.MAX_VALUE, lc.getStackTraceHashingTimeout());
	}

	@Test
	public void f02_stackTraceHashing_trueValues() {
		for (String s : strings("true", "TRUE")) {
			RestCallLoggerConfig lc =
				config()
				.stackTraceHashing(s)
				.build();

			assertTrue(lc.useStackTraceHashing());
			assertEquals(Integer.MAX_VALUE, lc.getStackTraceHashingTimeout());
		}
	}

	@Test
	public void f03_stackTraceHashing_falseValues() {
		for (String s : strings("false", "FALSE", "foo", null)) {
			RestCallLoggerConfig lc =
				config()
				.stackTraceHashing(s)
				.build();

			assertFalse(lc.useStackTraceHashing());
		}
	}

	@Test
	public void f04_stackTraceHashing_numericValues() {
		RestCallLoggerConfig lc =
			config()
			.stackTraceHashing("1")
			.build();

		assertTrue(lc.useStackTraceHashing());
		assertEquals(1, lc.getStackTraceHashingTimeout());
	}

	@Test
	public void f05_stackTraceHashingTimeout() {
		RestCallLoggerConfig lc =
			config()
			.stackTraceHashingTimeout(1)
			.build();

		assertTrue(lc.useStackTraceHashing());
		assertEquals(1, lc.getStackTraceHashingTimeout());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Level
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_level_default() {
		RestCallLoggerConfig lc =
			config()
			.build();

		assertEquals(Level.INFO, lc.getLevel());
	}

	@Test
	public void g02_level_warningLevel() {
		RestCallLoggerConfig lc =
			config()
			.level(Level.WARNING)
			.build();

		assertEquals(Level.WARNING, lc.getLevel());
	}

	@Test
	public void g03_level_warningString() {
		RestCallLoggerConfig lc =
			config()
			.level("WARNING")
			.build();

		assertEquals(Level.WARNING, lc.getLevel());
	}

	@Test
	public void g04_level_nullLevel() {
		RestCallLoggerConfig lc =
			config()
			.level((Level)null)
			.build();

		assertEquals(Level.INFO, lc.getLevel());
	}

	@Test
	public void g05_level_nullString() {
		RestCallLoggerConfig lc =
			config()
			.level((String)null)
			.build();

		assertEquals(Level.INFO, lc.getLevel());
	}
}
