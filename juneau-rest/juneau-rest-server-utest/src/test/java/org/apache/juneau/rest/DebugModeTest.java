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
import static org.junit.runners.MethodSorters.*;

import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class DebugModeTest {

	public static final CaptureCallLogger LOGGER = new CaptureCallLogger();

	public static class CaptureCallLogger extends BasicRestCallLogger {

		private volatile String msg;

		public static CaptureCallLogger create() {
			return LOGGER;
		}

		private CaptureCallLogger() {
			super(null);
		}

		@Override
		protected synchronized void log(Level level, String msg, Throwable e) {
			this.msg = StringUtils.emptyIfNull(msg);
		}

		synchronized CaptureCallLogger reset() {
			this.msg = null;
			return this;
		}

		synchronized String getMessage() {
			return msg;
		}

		public synchronized CaptureCallLogger assertMessageContains(String s) {
			assertNotNull(msg);
			if (! msg.contains(s))
				assertEquals("Substring not found.", s, msg);
			return this;
		}
	}

	private static void assertLogged(boolean b) {
		assertEquals(b, LOGGER.getMessage() != null);
		LOGGER.reset();
	}

	private static void assertLoggedContains(String s) {
		String msg = LOGGER.getMessage();
		assertLogged(true);
		if (! msg.contains(s))
			assertEquals("Substring not found.", s, msg);
		LOGGER.reset();
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug=""), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class)
	public static class A1 implements BasicRestConfig {
		@RestMethod
		public boolean getA01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getA02(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getA03(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getA04(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean getA05(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getA06(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean getA07(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest a1 = MockRest.buildSimpleJson(A1.class);
	static MockRest a1d = MockRest.create(A1.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void a01_debugDefault() throws Exception {

		a1.get("/a01").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/a01").run().assertBody().is("false");
		assertLogged(false);

		a1.get("/a02").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/a02").run().assertBody().is("false");
		assertLogged(false);

		a1.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");
		a1d.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");

		a1.get("/a04").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/a04").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a04");

		a1.get("/a05").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/a05").run().assertBody().is("false");
		assertLogged(false);

		a1.get("/a06").run().assertBody().is("true");
		assertLogged(true);
		a1d.get("/a06").run().assertBody().is("true");
		assertLogged(true);

		a1.get("/a07").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/a07").run().assertBody().is("false");
		assertLogged(false);
	}

	@Rest(callLogger=CaptureCallLogger.class)
	public static class A1a extends BasicRest {
		@RestMethod
		public boolean getA01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getA02(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getA03(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getA04(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean getA05(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getA06(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean getA07(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest a1a = MockRest.buildSimpleJson(A1a.class);
	static MockRest a1ad = MockRest.create(A1a.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void a01a_debugDefault() throws Exception {

		a1a.get("/a01").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/a01").run().assertBody().is("false");
		assertLogged(false);

		a1a.get("/a02").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/a02").run().assertBody().is("false");
		assertLogged(false);

		a1a.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");
		a1ad.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");

		a1a.get("/a04").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/a04").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a04");

		a1a.get("/a05").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/a05").run().assertBody().is("false");
		assertLogged(false);

		a1a.get("/a06").run().assertBody().is("true");
		assertLogged(true);
		a1ad.get("/a06").run().assertBody().is("true");
		assertLogged(true);

		a1a.get("/a07").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/a07").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="true"), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class, debug="true")
	public static class A2 implements BasicRestConfig {
		@RestMethod
		public boolean getA01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getA02(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getA03(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getA04(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean getA05(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getA06(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean getA07(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest a2 = MockRest.buildSimpleJson(A2.class);
	static MockRest a2d = MockRest.create(A2.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void a02_debugTrue() throws Exception {

		a2.get("/a01").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/a01").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/a02").run().assertBody().is("false");
		assertLogged(false);
		a2d.get("/a02").run().assertBody().is("false");
		assertLogged(false);

		a2.get("/a03").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/a03").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/a04").run().assertBody().is("false");
		assertLogged(false);
		a2d.get("/a04").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/a05").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/a05").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/a06").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/a06").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/a07").run().assertBody().is("false");
		assertLogged(false);
		a2d.get("/a07").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="false"), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class,debug="false")
	public static class A3 implements BasicRestConfig {
		@RestMethod
		public boolean getA01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getA02(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getA03(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getA04(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean getA05(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getA06(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean getA07(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest a3 = MockRest.buildSimpleJson(A3.class);
	static MockRest a3d = MockRest.create(A3.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void a03_restDebugFalse() throws Exception {

		a3.get("/a01").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/a01").run().assertBody().is("false");
		assertLogged(false);

		a3.get("/a02").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/a02").run().assertBody().is("false");
		assertLogged(false);

		a3.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");
		a3d.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");

		a3.get("/a04").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/a04").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a04");

		a3.get("/a05").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/a05").run().assertBody().is("false");
		assertLogged(false);

		a3.get("/a06").run().assertBody().is("true");
		assertLogged(true);
		a3d.get("/a06").run().assertBody().is("true");
		assertLogged(true);

		a3.get("/a07").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/a07").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="per-request"), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class,debug="per-request")
	public static class A4 implements BasicRestConfig {
		@RestMethod
		public boolean getA01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getA02(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getA03(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getA04(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean getA05(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getA06(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean getA07(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest a4 = MockRest.buildSimpleJson(A4.class);
	static MockRest a4d = MockRest.create(A4.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void a04_debugPerRequest() throws Exception {

		a4.get("/a01").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/a01").run().assertBody().is("true");
		assertLogged(true);

		a4.get("/a02").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/a02").run().assertBody().is("false");
		assertLogged(false);

		a4.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");
		a4d.get("/a03").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a03");

		a4.get("/a04").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/a04").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /a04");

		a4.get("/a05").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/a05").run().assertBody().is("true");
		assertLogged(true);

		a4.get("/a06").run().assertBody().is("true");
		assertLogged(true);
		a4d.get("/a06").run().assertBody().is("true");
		assertLogged(true);

		a4.get("/a07").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/a07").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Implement RestCallLogger directly.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B1 implements BasicRestConfig, RestCallLogger {
		@RestMethod
		public boolean getB01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getB02(RestRequest req) {
			return req.isDebug();
		}
		@Override
		public void log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res) {
			LOGGER.log(config, req, res);
		}
	}
	static MockRest b1 = MockRest.buildSimpleJson(B1.class);

	@Test
	public void b01_debugDefault() throws Exception {

		b1.get("/b01").run().assertBody().is("false");
		assertLogged(false);
		b1.get("/b02").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /b02");
	}

	@Rest
	public static class B2 extends BasicRest {
		@RestMethod
		public boolean getB01(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getB02(RestRequest req) {
			return req.isDebug();
		}
		@Override
		public void log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res) {
			LOGGER.log(config, req, res);
		}
	}
	static MockRest b2 = MockRest.buildSimpleJson(B2.class);

	@Test
	public void b02_debugDefault() throws Exception {

		b2.get("/b01").run().assertBody().is("false");
		assertLogged(false);
		b2.get("/b02").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /b02");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debugOn=""), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		callLogger=CaptureCallLogger.class,
		debugOn=""
			+ "C1.getC02a=false,C1.getC02b=false,C1.getC02c=FALSE,C1.getC02d=FALSE,C1.getC02e=FALSE,C1.getC02f=FALSE,"
			+ " C1.getC03a , C1.getC03b = true , C1.getC03c = TRUE , C1.getC03d = TRUE , C1.getC03e = TRUE , C1.getC03f = TRUE , "
			+ "C1.getC04a=per-request,C1.getC04b=per-request,C1.getC04c=PER-REQUEST,C1.getC04d=PER-REQUEST,C1.getC04e=PER-REQUEST,C1.getC04f=PER-REQUEST,"
			+ "C1.getC05a=foo,C1.getC05b=,C1.getC05c=foo,C1.getC05d=foo,C1.getC05e=foo,C1.getC05f=foo,"
	)
	public static class C1 implements BasicRestConfig {

		@RestMethod
		public boolean getC01a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC01b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC01c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC01d(RestRequest req) {
			return req.isDebug();
		}

		// debug=false
		@RestMethod
		public boolean getC02a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC02b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC02c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC02d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC02e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC02f(RestRequest req) {
			return req.isDebug();
		}

		// debug=true
		@RestMethod
		public boolean getC03a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC03b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC03c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC03d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC03e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC03f(RestRequest req) {
			return req.isDebug();
		}

		// debug=per-request
		@RestMethod
		public boolean getC04a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC04b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC04c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC04d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC04e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC04f(RestRequest req) {
			return req.isDebug();
		}

		// debug=foo
		@RestMethod
		public boolean getC05a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC05b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC05c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC05d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC05e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC05f(RestRequest req) {
			return req.isDebug();
		}

		@RestMethod
		public boolean getC06a(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC06b(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC06c(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC06d(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}

		@RestMethod
		public boolean getC07a(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC07b(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC07c(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC07d(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest c1 = MockRest.buildSimpleJson(C1.class);
	static MockRest c1d = MockRest.create(C1.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void c01_debugDefault() throws Exception {

		c1.get("/c01a").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c01a").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c01b").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c01b").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c01c").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/c01c").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/c01d").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c01d").run().assertBody().is("true");
		assertLogged(true);

		c1.get("/c02a").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c02a").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c02b").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c02b").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c02c").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c02c").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c02d").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c02d").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c02e").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c02e").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c02f").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c02f").run().assertBody().is("false");
		assertLogged(false);

		c1.get("/c03a").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03a");
		c1d.get("/c03a").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03a");
		c1.get("/c03b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03b");
		c1d.get("/c03b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03b");
		c1.get("/c03c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03c");
		c1d.get("/c03c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03c");
		c1.get("/c03d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03d");
		c1d.get("/c03d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03d");
		c1.get("/c03e").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03e");
		c1d.get("/c03e").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03e");
		c1.get("/c03f").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03f");
		c1d.get("/c03f").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03f");

		c1.get("/c04a").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c04a").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04a");
		c1.get("/c04b").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c04b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04b");
		c1.get("/c04c").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c04c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04c");
		c1.get("/c04d").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c04d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04d");
		c1.get("/c04e").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c04e").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04e");
		c1.get("/c04f").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c04f").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04f");

		c1.get("/c05a").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c05a").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c05b").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c05b").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c05c").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c05c").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c05d").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c05d").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c05e").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c05e").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c05f").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c05f").run().assertBody().is("false");
		assertLogged(false);

		c1.get("/c06a").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/c06a").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/c06b").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/c06b").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/c06c").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/c06c").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/c06d").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/c06d").run().assertBody().is("true");
		assertLogged(true);

		c1.get("/c07a").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c07a").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c07b").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c07b").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c07c").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c07c").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/c07d").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/c07d").run().assertBody().is("false");
		assertLogged(false);
	}

	static {
		System.setProperty("C2DebugEnabled", "C2=true");
	}
	@Rest(
		callLogger=CaptureCallLogger.class,
		debugOn="$S{C2DebugEnabled},"
			+ "C2.getC02a=false,C2.getC02b=false,C2.getC02c=FALSE,C2.getC02d=FALSE,C2.getC02e=FALSE,C2.getC02f=FALSE,"
			+ " C2.getC03a , C2.getC03b = true , C2.getC03c = TRUE , C2.getC03d = TRUE , C2.getC03e = TRUE , C2.getC03f = TRUE , "
			+ "C2.getC04a=per-request,C2.getC04b=per-request,C2.getC04c=PER-REQUEST,C2.getC04d=PER-REQUEST,C2.getC04e=PER-REQUEST,C2.getC04f=PER-REQUEST,"
			+ "C2.getC05a=foo,C2.getC05b=,C2.getC05c=foo,C2.getC05d=foo,C2.getC05e=foo,C2.getC05f=foo,"
	)
	public static class C2 implements BasicRestConfig {

		@RestMethod
		public boolean getC01a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC01b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC01c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC01d(RestRequest req) {
			return req.isDebug();
		}

		// debug=false
		@RestMethod
		public boolean getC02a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC02b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC02c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC02d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC02e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC02f(RestRequest req) {
			return req.isDebug();
		}

		// debug=true
		@RestMethod
		public boolean getC03a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC03b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC03c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC03d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC03e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC03f(RestRequest req) {
			return req.isDebug();
		}

		// debug=per-request
		@RestMethod
		public boolean getC04a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC04b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC04c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC04d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC04e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC04f(RestRequest req) {
			return req.isDebug();
		}

		// debug=foo
		@RestMethod
		public boolean getC05a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC05b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean getC05c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC05d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC05e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC05f(RestRequest req) {
			return req.isDebug();
		}

		@RestMethod
		public boolean getC06a(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC06b(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC06c(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC06d(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}

		@RestMethod
		public boolean getC07a(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean getC07b(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean getC07c(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean getC07d(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}
	static MockRest c2 = MockRest.buildSimpleJson(C2.class);
	static MockRest c2d = MockRest.create(C2.class).simpleJson().header("X-Debug", true).build();

	@Test
	public void c02_debugTrue() throws Exception {

		c2.get("/c01a").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c01a").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c01b").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c01b").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c01c").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c01c").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c01d").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c01d").run().assertBody().is("true");
		assertLogged(true);

		c2.get("/c02a").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c02a").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c02b").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c02b").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c02c").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c02c").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c02d").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c02d").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c02e").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c02e").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c02f").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c02f").run().assertBody().is("false");
		assertLogged(false);

		c2.get("/c03a").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03a");
		c2d.get("/c03a").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03a");
		c2.get("/c03b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03b");
		c2d.get("/c03b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03b");
		c2.get("/c03c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03c");
		c2d.get("/c03c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03c");
		c2.get("/c03d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03d");
		c2d.get("/c03d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03d");
		c2.get("/c03e").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03e");
		c2d.get("/c03e").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03e");
		c2.get("/c03f").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03f");
		c2d.get("/c03f").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c03f");

		c2.get("/c04a").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c04a").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04a");
		c2.get("/c04b").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c04b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04b");
		c2.get("/c04c").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c04c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04c");
		c2.get("/c04d").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c04d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04d");
		c2.get("/c04e").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c04e").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04e");
		c2.get("/c04f").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c04f").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c04f");

		c2.get("/c05a").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c05a").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c05b").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c05b").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c05c").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c05c").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c05d").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c05d").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c05e").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c05e").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c05f").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c05f").run().assertBody().is("true");
		assertLogged(true);

		c2.get("/c06a").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c06a").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c06b").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c06b").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c06c").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c06c").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/c06d").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/c06d").run().assertBody().is("true");
		assertLogged(true);

		c2.get("/c07a").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c07a").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c07b").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c07b").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c07c").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c07c").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/c07d").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/c07d").run().assertBody().is("false");
		assertLogged(false);
	}

}
