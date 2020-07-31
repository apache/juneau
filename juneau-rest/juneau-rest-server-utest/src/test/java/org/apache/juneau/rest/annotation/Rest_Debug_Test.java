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
package org.apache.juneau.rest.annotation;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_Debug_Test {

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
	public static class A1 implements BasicUniversalRest {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a01_debugDefault() throws Exception {
		RestClient a1 = MockRestClient.buildSimpleJson(A1.class);
		RestClient a1d = MockRestClient.create(A1.class).simpleJson().header("X-Debug", true).build();

		a1.get("/a").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/a").run().assertBody().is("false");
		assertLogged(false);

		a1.get("/b").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/b").run().assertBody().is("false");
		assertLogged(false);

		a1.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");
		a1d.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");

		a1.get("/d").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d");

		a1.get("/e").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/e").run().assertBody().is("false");
		assertLogged(false);

		a1.get("/f").run().assertBody().is("true");
		assertLogged(true);
		a1d.get("/f").run().assertBody().is("true");
		assertLogged(true);

		a1.get("/g").run().assertBody().is("false");
		assertLogged(false);
		a1d.get("/g").run().assertBody().is("false");
		assertLogged(false);
	}

	@Rest(callLogger=CaptureCallLogger.class)
	public static class A1a extends BasicRest {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a01a_debugDefault() throws Exception {
		RestClient a1a = MockRestClient.buildSimpleJson(A1a.class);
		RestClient a1ad = MockRestClient.create(A1a.class).simpleJson().header("X-Debug", true).build();

		a1a.get("/a").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/a").run().assertBody().is("false");
		assertLogged(false);

		a1a.get("/b").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/b").run().assertBody().is("false");
		assertLogged(false);

		a1a.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");
		a1ad.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");

		a1a.get("/d").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d");

		a1a.get("/e").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/e").run().assertBody().is("false");
		assertLogged(false);

		a1a.get("/f").run().assertBody().is("true");
		assertLogged(true);
		a1ad.get("/f").run().assertBody().is("true");
		assertLogged(true);

		a1a.get("/g").run().assertBody().is("false");
		assertLogged(false);
		a1ad.get("/g").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="true"), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class, debug="true")
	public static class A2 implements BasicUniversalRest {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a02_debugTrue() throws Exception {
		RestClient a2 = MockRestClient.buildSimpleJson(A2.class);
		RestClient a2d = MockRestClient.create(A2.class).simpleJson().header("X-Debug", true).build();

		a2.get("/a").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/a").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/b").run().assertBody().is("false");
		assertLogged(false);
		a2d.get("/b").run().assertBody().is("false");
		assertLogged(false);

		a2.get("/c").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/c").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/d").run().assertBody().is("false");
		assertLogged(false);
		a2d.get("/d").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/e").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/e").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/f").run().assertBody().is("true");
		assertLogged(true);
		a2d.get("/f").run().assertBody().is("true");
		assertLogged(true);

		a2.get("/g").run().assertBody().is("false");
		assertLogged(false);
		a2d.get("/g").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="false"), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class,debug="false")
	public static class A3 implements BasicUniversalRest {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a03_restDebugFalse() throws Exception {
		RestClient a3 = MockRestClient.buildSimpleJson(A3.class);
		RestClient a3d = MockRestClient.create(A3.class).simpleJson().header("X-Debug", true).build();

		a3.get("/a").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/a").run().assertBody().is("false");
		assertLogged(false);

		a3.get("/b").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/b").run().assertBody().is("false");
		assertLogged(false);

		a3.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");
		a3d.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");

		a3.get("/d").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d");

		a3.get("/e").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/e").run().assertBody().is("false");
		assertLogged(false);

		a3.get("/f").run().assertBody().is("true");
		assertLogged(true);
		a3d.get("/f").run().assertBody().is("true");
		assertLogged(true);

		a3.get("/g").run().assertBody().is("false");
		assertLogged(false);
		a3d.get("/g").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="per-request"), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureCallLogger.class,debug="per-request")
	public static class A4 implements BasicUniversalRest {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a04_debugPerRequest() throws Exception {
		RestClient a4 = MockRestClient.buildSimpleJson(A4.class);
		RestClient a4d = MockRestClient.create(A4.class).simpleJson().header("X-Debug", true).build();

		a4.get("/a").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/a").run().assertBody().is("true");
		assertLogged(true);

		a4.get("/b").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/b").run().assertBody().is("false");
		assertLogged(false);

		a4.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");
		a4d.get("/c").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c");

		a4.get("/d").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/d").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d");

		a4.get("/e").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/e").run().assertBody().is("true");
		assertLogged(true);

		a4.get("/f").run().assertBody().is("true");
		assertLogged(true);
		a4d.get("/f").run().assertBody().is("true");
		assertLogged(true);

		a4.get("/g").run().assertBody().is("false");
		assertLogged(false);
		a4d.get("/g").run().assertBody().is("false");
		assertLogged(false);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Implement RestCallLogger directly.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B1 implements BasicUniversalRest, RestCallLogger {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@Override
		public void log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res) {
			LOGGER.log(config, req, res);
		}
	}

	@Test
	public void b01_debugDefault() throws Exception {
		RestClient b1 = MockRestClient.buildSimpleJson(B1.class);

		b1.get("/a").run().assertBody().is("false");
		assertLogged(false);
		b1.get("/b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /b");
	}

	@Rest
	public static class B2 extends BasicRest {
		@RestMethod
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@Override
		public void log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res) {
			LOGGER.log(config, req, res);
		}
	}

	@Test
	public void b02_debugDefault() throws Exception {
		RestClient b2 = MockRestClient.buildSimpleJson(B2.class);

		b2.get("/a").run().assertBody().is("false");
		assertLogged(false);
		b2.get("/b").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /b");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debugOn=""), various @RestMethod(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		callLogger=CaptureCallLogger.class,
		debugOn=""
			+ "C1.b1=false,C1.b2=false,C1.b3=FALSE,C1.b4=FALSE,C1.b5=FALSE,C1.b6=FALSE,"
			+ " C1.c1 , C1.c2 = true , C1.c3 = TRUE , C1.c4 = TRUE , C1.c5 = TRUE , C1.c6 = TRUE , "
			+ "C1.d1=per-request,C1.d2=per-request,C1.d3=PER-REQUEST,C1.d4=PER-REQUEST,C1.d5=PER-REQUEST,C1.d6=PER-REQUEST,"
			+ "C1.e1=foo,C1.e2=,C1.e3=foo,C1.e4=foo,C1.e5=foo,C1.e6=foo,"
	)
	public static class C1 implements BasicUniversalRest {

		@RestMethod
		public boolean a1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean a2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean a3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean a4(RestRequest req) {
			return req.isDebug();
		}

		// debug=false
		@RestMethod
		public boolean b1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean b2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean b3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean b5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean b6(RestRequest req) {
			return req.isDebug();
		}

		// debug=true
		@RestMethod
		public boolean c1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean c2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean c3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean c4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean c6(RestRequest req) {
			return req.isDebug();
		}

		// debug=per-request
		@RestMethod
		public boolean d1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean d2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean d3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean d4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean d5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d6(RestRequest req) {
			return req.isDebug();
		}

		// debug=foo
		@RestMethod
		public boolean e1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean e2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean e3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean e4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean e5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean e6(RestRequest req) {
			return req.isDebug();
		}

		@RestMethod
		public boolean f1(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean f2(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean f3(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean f4(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}

		@RestMethod
		public boolean g1(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean g2(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean g3(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean g4(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void c01_debugDefault() throws Exception {
		RestClient c1 = MockRestClient.buildSimpleJson(C1.class);
		RestClient c1d = MockRestClient.create(C1.class).simpleJson().header("X-Debug", true).build();

		c1.get("/a1").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/a1").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/a2").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/a2").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/a3").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/a3").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/a4").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/a4").run().assertBody().is("true");
		assertLogged(true);

		c1.get("/b1").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/b1").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/b2").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/b2").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/b3").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/b3").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/b4").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/b4").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/b5").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/b5").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/b6").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/b6").run().assertBody().is("false");
		assertLogged(false);

		c1.get("/c1").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c1");
		c1d.get("/c1").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c1");
		c1.get("/c2").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c2");
		c1d.get("/c2").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c2");
		c1.get("/c3").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c3");
		c1d.get("/c3").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c3");
		c1.get("/c4").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c4");
		c1d.get("/c4").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c4");
		c1.get("/c5").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c5");
		c1d.get("/c5").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c5");
		c1.get("/c6").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c6");
		c1d.get("/c6").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c6");

		c1.get("/d1").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/d1").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d1");
		c1.get("/d2").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/d2").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d2");
		c1.get("/d3").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/d3").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d3");
		c1.get("/d4").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/d4").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d4");
		c1.get("/d5").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/d5").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d5");
		c1.get("/d6").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/d6").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d6");

		c1.get("/e1").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/e1").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/e2").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/e2").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/e3").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/e3").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/e4").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/e4").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/e5").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/e5").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/e6").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/e6").run().assertBody().is("false");
		assertLogged(false);

		c1.get("/f1").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/f1").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/f2").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/f2").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/f3").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/f3").run().assertBody().is("true");
		assertLogged(true);
		c1.get("/f4").run().assertBody().is("true");
		assertLogged(true);
		c1d.get("/f4").run().assertBody().is("true");
		assertLogged(true);

		c1.get("/g1").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/g1").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/g2").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/g2").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/g3").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/g3").run().assertBody().is("false");
		assertLogged(false);
		c1.get("/g4").run().assertBody().is("false");
		assertLogged(false);
		c1d.get("/g4").run().assertBody().is("false");
		assertLogged(false);
	}

	static {
		System.setProperty("C2DebugEnabled", "C2=true");
	}
	@Rest(
		callLogger=CaptureCallLogger.class,
		debugOn="$S{C2DebugEnabled},"
			+ "C2.b1=false,C2.b2=false,C2.b3=FALSE,C2.b4=FALSE,C2.b5=FALSE,C2.b6=FALSE,"
			+ " C2.c1 , C2.c2 = true , C2.c3 = TRUE , C2.c4 = TRUE , C2.c5 = TRUE , C2.c6 = TRUE , "
			+ "C2.d1=per-request,C2.d2=per-request,C2.d3=PER-REQUEST,C2.d4=PER-REQUEST,C2.d5=PER-REQUEST,C2.d6=PER-REQUEST,"
			+ "C2.e1=foo,C2.e2=,C2.e3=foo,C2.e4=foo,C2.e5=foo,C2.e6=foo,"
	)
	public static class C2 implements BasicUniversalRest {

		@RestMethod
		public boolean a1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean a2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean a3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean a4(RestRequest req) {
			return req.isDebug();
		}

		// debug=false
		@RestMethod
		public boolean b1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean b2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean b3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean b4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean b5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean b6(RestRequest req) {
			return req.isDebug();
		}

		// debug=true
		@RestMethod
		public boolean c1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean c2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean c3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean c4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean c5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean c6(RestRequest req) {
			return req.isDebug();
		}

		// debug=per-request
		@RestMethod
		public boolean d1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean d2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean d3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean d4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean d5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean d6(RestRequest req) {
			return req.isDebug();
		}

		// debug=foo
		@RestMethod
		public boolean e1(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean e2(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod
		public boolean e3(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean e4(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean e5(RestRequest req) {
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean e6(RestRequest req) {
			return req.isDebug();
		}

		@RestMethod
		public boolean f1(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean f2(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean f3(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean f4(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}

		@RestMethod
		public boolean g1(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="false")
		public boolean g2(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="true")
		public boolean g3(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestMethod(debug="per-request")
		public boolean g4(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void c02_debugTrue() throws Exception {
		RestClient c2 = MockRestClient.buildSimpleJson(C2.class);
		RestClient c2d = MockRestClient.create(C2.class).simpleJson().header("X-Debug", true).build();

		c2.get("/a1").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/a1").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/a2").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/a2").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/a3").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/a3").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/a4").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/a4").run().assertBody().is("true");
		assertLogged(true);

		c2.get("/b1").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/b1").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/b2").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/b2").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/b3").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/b3").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/b4").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/b4").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/b5").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/b5").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/b6").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/b6").run().assertBody().is("false");
		assertLogged(false);

		c2.get("/c1").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c1");
		c2d.get("/c1").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c1");
		c2.get("/c2").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c2");
		c2d.get("/c2").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c2");
		c2.get("/c3").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c3");
		c2d.get("/c3").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c3");
		c2.get("/c4").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c4");
		c2d.get("/c4").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c4");
		c2.get("/c5").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c5");
		c2d.get("/c5").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c5");
		c2.get("/c6").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c6");
		c2d.get("/c6").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /c6");

		c2.get("/d1").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/d1").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d1");
		c2.get("/d2").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/d2").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d2");
		c2.get("/d3").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/d3").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d3");
		c2.get("/d4").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/d4").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d4");
		c2.get("/d5").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/d5").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d5");
		c2.get("/d6").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/d6").run().assertBody().is("true");
		assertLoggedContains("[200] HTTP GET /d6");

		c2.get("/e1").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/d1").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/e2").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/e2").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/e3").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/e3").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/e4").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/e4").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/e5").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/e5").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/e6").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/e6").run().assertBody().is("true");
		assertLogged(true);

		c2.get("/f1").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/f1").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/f2").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/f2").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/f3").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/f3").run().assertBody().is("true");
		assertLogged(true);
		c2.get("/f4").run().assertBody().is("true");
		assertLogged(true);
		c2d.get("/f4").run().assertBody().is("true");
		assertLogged(true);

		c2.get("/g1").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/g1").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/g2").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/g2").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/g3").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/g3").run().assertBody().is("false");
		assertLogged(false);
		c2.get("/g4").run().assertBody().is("false");
		assertLogged(false);
		c2d.get("/g4").run().assertBody().is("false");
		assertLogged(false);
	}

}
