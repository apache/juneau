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

import static org.apache.juneau.rest.Enablement.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestCallLoggerConfig_Test {

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

	private RestCallLoggerConfig wrapped(RestCallLoggerConfig parent) {
		return RestCallLoggerConfig.create().parent(parent).build();
	}


	private RestCallLoggerRule.Builder rule() {
		return RestCallLoggerRule.create();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basicMatching_noRules() {
		RestCallLoggerConfig lc = config().build();
		RestCallLoggerConfig lcw = wrapped(lc);

		HttpServletRequest req = req();
		HttpServletResponse res = res();

		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
	}

	@Test
	public void a02_basicMatching_codeMatchingRule() {
		RestCallLoggerConfig lc =
			config()
			.rules(
				rule()
				.codes("200")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res(200);

		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
		res.status(201);
		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
	}

	@Test
	public void a03_basicMatching_exceptionMatchingRule() {
		RestCallLoggerConfig lc =
			config()
			.rule(
				rule()
				.exceptions("IndexOutOfBounds*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res();

		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
		req.attribute("Exception", new IndexOutOfBoundsException());
		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
	}

	@Test
	public void a04_basicMatching_debugMatching() {
		RestCallLoggerConfig lc =
			config()
			.rule(
				rule()
				.exceptions("IndexOutOfBounds*")
				.debugOnly()
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res();

		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
		req.attribute("Exception", new IndexOutOfBoundsException());
		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
		req.attribute("Debug", true);
		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Disabled
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_disabled() {
		RestCallLoggerConfig lc =
			config()
			.disabled()
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res(200);

		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
	}

	@Test
	public void b02_disabled_true() {
		RestCallLoggerConfig lc =
			config()
			.disabled(TRUE)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res(200);

		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
	}

	@Test
	public void b03_disabled_false() {
		RestCallLoggerConfig lc =
			config()
			.disabled(FALSE)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res(200);

		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
	}

	@Test
	public void b04_disabled_null() {
		RestCallLoggerConfig lc =
			config()
			.disabled(null)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res(200);

		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
	}

	//------------------------------------------------------------------------------------------------------------------
	// No-trace
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_noTraceAlways() {
		RestCallLoggerConfig lc =
			config()
			.disabled(TRUE)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res();

		assertNull(lc.getRule(req, res));
		assertNull(lcw.getRule(req, res));
	}

	@Test
	public void c02_noTrace_never() {
		RestCallLoggerConfig lc =
			config()
			.disabled(FALSE)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res();

		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
	}

	@Test
	public void c03_noTrace_null() {
		RestCallLoggerConfig lc =
			config()
			.disabled(null)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletResponse res = res();

		assertNotNull(lc.getRule(req, res));
		assertNotNull(lcw.getRule(req, res));
	}

	@Test
	public void c04_noTrace_perRequest() {
		RestCallLoggerConfig lc =
			config()
			.disabled(PER_REQUEST)
			.rule(
				rule()
				.codes("*")
				.build()
			)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		MockServletRequest req = req();
		MockServletRequest reqNoTrace = req().noTrace(true);
		MockServletRequest reqNoTraceAttrTrue = req().attribute("NoTrace", true);
		MockServletRequest reqNoTraceAttrFalse = req().attribute("NoTrace", false);
		MockServletRequest reqNoTraceAttrOther = req().attribute("NoTrace", "foo");
		MockServletResponse res = res();

		assertNotNull(lc.getRule(req, res));
		assertNull(lc.getRule(reqNoTrace, res));
		assertNull(lc.getRule(reqNoTraceAttrTrue, res));
		assertNotNull(lc.getRule(reqNoTraceAttrFalse, res));
		assertNotNull(lc.getRule(reqNoTraceAttrOther, res));

		assertNotNull(lcw.getRule(req, res));
		assertNull(lcw.getRule(reqNoTrace, res));
		assertNull(lcw.getRule(reqNoTraceAttrTrue, res));
		assertNotNull(lcw.getRule(reqNoTraceAttrFalse, res));
		assertNotNull(lcw.getRule(reqNoTraceAttrOther, res));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Use stack trace hashing
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_stackTraceHashing() {
		RestCallLoggerConfig lc =
			config()
			.useStackTraceHashing()
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertTrue(lc.isUseStackTraceHashing());
		assertTrue(lcw.isUseStackTraceHashing());
	}

	@Test
	public void d02_stackTraceHashing_true() {
		RestCallLoggerConfig lc =
			config()
			.useStackTraceHashing(true)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertTrue(lc.isUseStackTraceHashing());
		assertTrue(lcw.isUseStackTraceHashing());
	}

	@Test
	public void d03_stackTraceHashing_false() {
		RestCallLoggerConfig lc =
			config()
			.useStackTraceHashing(false)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertFalse(lc.isUseStackTraceHashing());
		assertFalse(lcw.isUseStackTraceHashing());
	}

	@Test
	public void d04_stackTraceHashing_null() {
		RestCallLoggerConfig lc =
			config()
			.useStackTraceHashing(null)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertFalse(lc.isUseStackTraceHashing());
		assertFalse(lcw.isUseStackTraceHashing());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Stack trace hashing timeout
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_getStackTraceHashingTimeout() {
		RestCallLoggerConfig lc =
			config()
			.stackTraceHashingTimeout(1)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertEquals(1, lc.getStackTraceHashingTimeout());
		assertEquals(1, lcw.getStackTraceHashingTimeout());
	}

	@Test
	public void e02_getStackTraceHashingTimeout_null() {
		RestCallLoggerConfig lc =
			config()
			.stackTraceHashingTimeout(null)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertEquals(Integer.MAX_VALUE, lc.getStackTraceHashingTimeout());
		assertEquals(Integer.MAX_VALUE, lcw.getStackTraceHashingTimeout());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Level
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_level_default() {
		RestCallLoggerConfig lc =
			config()
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertEquals(Level.INFO, lc.getLevel());
		assertEquals(Level.INFO, lcw.getLevel());
	}

	@Test
	public void f02_level_warningLevel() {
		RestCallLoggerConfig lc =
			config()
			.level(Level.WARNING)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertEquals(Level.WARNING, lc.getLevel());
		assertEquals(Level.WARNING, lcw.getLevel());
	}

	@Test
	public void f03_level_nullLevel() {
		RestCallLoggerConfig lc =
			config()
			.level(null)
			.build();
		RestCallLoggerConfig lcw = wrapped(lc);

		assertEquals(Level.INFO, lc.getLevel());
		assertEquals(Level.INFO, lcw.getLevel());
	}
}
