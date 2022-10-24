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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_Debug_Test {

	public static final CaptureLogger LOGGER = new CaptureLogger();

	public static class CaptureLogger extends BasicTestCaptureCallLogger {
		public static CaptureLogger getInstance() {
			return LOGGER;
		}
	}

	private static void assertLogged() {
		LOGGER.assertMessageAndReset().isExists();
	}

	private static void assertNotLogged() {
		LOGGER.assertMessageAndReset().isNull();
	}

	private static void assertLogged(String msg) {
		LOGGER.assertMessageAndReset().isContains(msg);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug=""), various @RestOp(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureLogger.class)
	public static class A1_RestOp implements BasicUniversalConfig {
		@RestOp
		public boolean aa(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean ab(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean ac(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean ad(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="foo")
		public boolean ae(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean af(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp
		public boolean ag(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestGet
		public boolean ba(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="false")
		public boolean bb(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="true")
		public boolean bc(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="conditional")
		public boolean bd(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="foo")
		public boolean be(RestRequest req) {
			return req.isDebug();
		}
		@RestGet
		public boolean bf(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestGet
		public boolean bg(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a01_restOp_debugDefault() throws Exception {
		RestClient a1 = MockRestClient.buildJson5(A1_RestOp.class);
		RestClient a1d = MockRestClient.create(A1_RestOp.class).json5().debug().suppressLogging().build();

		a1.get("/aa").run().assertContent("false");
		assertNotLogged();
		a1d.get("/aa").run().assertContent("true");
		assertLogged("[200] HTTP GET /aa");

		a1.get("/ab").run().assertContent("false");
		assertNotLogged();
		a1d.get("/ab").run().assertContent("false");
		assertNotLogged();

		a1.get("/ac").run().assertContent("true");
		assertLogged("[200] HTTP GET /ac");
		a1d.get("/ac").run().assertContent("true");
		assertLogged("[200] HTTP GET /ac");

		a1.get("/ad").run().assertContent("false");
		assertNotLogged();
		a1d.get("/ad").run().assertContent("true");
		assertLogged("[200] HTTP GET /ad");

		a1.get("/ae").run().assertContent("false");
		assertNotLogged();
		a1d.get("/ae").run().assertContent("true");
		assertLogged("[200] HTTP GET /ae");

		a1.get("/af").run().assertContent("true");
		assertLogged("[200] HTTP GET /af");
		a1d.get("/af").run().assertContent("true");
		assertLogged("[200] HTTP GET /af");

		a1.get("/ag").run().assertContent("false");
		assertNotLogged();
		a1d.get("/ag").run().assertContent("false");
		assertNotLogged();

		a1.get("/ba").run().assertContent("false");
		assertNotLogged();
		a1d.get("/ba").run().assertContent("true");
		assertLogged("[200] HTTP GET /ba");

		a1.get("/bb").run().assertContent("false");
		assertNotLogged();
		a1d.get("/bb").run().assertContent("false");
		assertNotLogged();

		a1.get("/bc").run().assertContent("true");
		assertLogged("[200] HTTP GET /bc");
		a1d.get("/bc").run().assertContent("true");
		assertLogged("[200] HTTP GET /bc");

		a1.get("/bd").run().assertContent("false");
		assertNotLogged();
		a1d.get("/bd").run().assertContent("true");
		assertLogged("[200] HTTP GET /bd");

		a1.get("/be").run().assertContent("false");
		assertNotLogged();
		a1d.get("/be").run().assertContent("true");
		assertLogged("[200] HTTP GET /be");

		a1.get("/bf").run().assertContent("true");
		assertLogged("[200] HTTP GET /bf");
		a1d.get("/bf").run().assertContent("true");
		assertLogged("[200] HTTP GET /bf");

		a1.get("/bg").run().assertContent("false");
		assertNotLogged();
		a1d.get("/bg").run().assertContent("false");
		assertNotLogged();
	}

	@Rest(callLogger=CaptureLogger.class)
	public static class A1_RestGet implements BasicUniversalConfig {
		@RestGet
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="conditional")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestGet(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestGet
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestGet
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Rest(callLogger=CaptureLogger.class)
	public static class A1a extends BasicRestObject {
		@RestOp
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a01a_debugDefault() throws Exception {
		RestClient a1a = MockRestClient.buildJson5(A1a.class);
		RestClient a1ad = MockRestClient.create(A1a.class).json5().debug().suppressLogging().build();

		a1a.get("/a").run().assertContent("false");
		assertNotLogged();
		a1ad.get("/a").run().assertContent("true");
		assertLogged("[200] HTTP GET /a");

		a1a.get("/b").run().assertContent("false");
		assertNotLogged();
		a1ad.get("/b").run().assertContent("false");
		assertNotLogged();

		a1a.get("/c").run().assertContent("true");
		assertLogged("[200] HTTP GET /c");
		a1ad.get("/c").run().assertContent("true");
		assertLogged("[200] HTTP GET /c");

		a1a.get("/d").run().assertContent("false");
		assertNotLogged();
		a1ad.get("/d").run().assertContent("true");
		assertLogged("[200] HTTP GET /d");

		a1a.get("/e").run().assertContent("false");
		assertNotLogged();
		a1ad.get("/e").run().assertContent("true");
		assertLogged("[200] HTTP GET /e");

		a1a.get("/f").run().assertContent("true");
		assertLogged();
		a1ad.get("/f").run().assertContent("true");
		assertLogged();

		a1a.get("/g").run().assertContent("false");
		assertNotLogged();
		a1ad.get("/g").run().assertContent("false");
		assertNotLogged();
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="true"), various @RestOp(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureLogger.class, debug="true")
	public static class A2 implements BasicUniversalConfig {
		@RestOp
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a02_debugTrue() throws Exception {
		RestClient a2 = MockRestClient.buildJson5(A2.class);
		RestClient a2d = MockRestClient.create(A2.class).json5().debug().suppressLogging().build();

		a2.get("/a").run().assertContent("true");
		assertLogged();
		a2d.get("/a").run().assertContent("true");
		assertLogged();

		a2.get("/b").run().assertContent("false");
		assertNotLogged();
		a2d.get("/b").run().assertContent("false");
		assertNotLogged();

		a2.get("/c").run().assertContent("true");
		assertLogged();
		a2d.get("/c").run().assertContent("true");
		assertLogged();

		a2.get("/d").run().assertContent("false");
		assertNotLogged();
		a2d.get("/d").run().assertContent("true");
		assertLogged();

		a2.get("/e").run().assertContent("true");
		assertLogged();
		a2d.get("/e").run().assertContent("true");
		assertLogged();

		a2.get("/f").run().assertContent("true");
		assertLogged();
		a2d.get("/f").run().assertContent("true");
		assertLogged();

		a2.get("/g").run().assertContent("false");
		assertNotLogged();
		a2d.get("/g").run().assertContent("false");
		assertNotLogged();
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="false"), various @RestOp(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureLogger.class,debug="false")
	public static class A3 implements BasicUniversalConfig {
		@RestOp
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a03_restDebugFalse() throws Exception {
		RestClient a3 = MockRestClient.buildJson5(A3.class);
		RestClient a3d = MockRestClient.create(A3.class).json5().debug().suppressLogging().build();

		a3.get("/a").run().assertContent("false");
		assertNotLogged();
		a3d.get("/a").run().assertContent("false");
		assertNotLogged();

		a3.get("/b").run().assertContent("false");
		assertNotLogged();
		a3d.get("/b").run().assertContent("false");
		assertNotLogged();

		a3.get("/c").run().assertContent("true");
		assertLogged("[200] HTTP GET /c");
		a3d.get("/c").run().assertContent("true");
		assertLogged("[200] HTTP GET /c");

		a3.get("/d").run().assertContent("false");
		assertNotLogged();
		a3d.get("/d").run().assertContent("true");
		assertLogged("[200] HTTP GET /d");

		a3.get("/e").run().assertContent("false");
		assertNotLogged();
		a3d.get("/e").run().assertContent("false");
		assertNotLogged();

		a3.get("/f").run().assertContent("true");
		assertLogged();
		a3d.get("/f").run().assertContent("true");
		assertLogged();

		a3.get("/g").run().assertContent("false");
		assertNotLogged();
		a3d.get("/g").run().assertContent("false");
		assertNotLogged();
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug="conditional"), various @RestOp(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(callLogger=CaptureLogger.class,debug="conditional")
	public static class A4 implements BasicUniversalConfig {
		@RestOp
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean b(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean c(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean d(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="foo")
		public boolean e(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean f(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp
		public boolean g(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void a04_debugPerRequest() throws Exception {
		RestClient a4 = MockRestClient.buildJson5(A4.class);
		RestClient a4d = MockRestClient.create(A4.class).json5().debug().suppressLogging().build();

		a4.get("/a").run().assertContent("false");
		assertNotLogged();
		a4d.get("/a").run().assertContent("true");
		assertLogged();

		a4.get("/b").run().assertContent("false");
		assertNotLogged();
		a4d.get("/b").run().assertContent("false");
		assertNotLogged();

		a4.get("/c").run().assertContent("true");
		assertLogged("[200] HTTP GET /c");
		a4d.get("/c").run().assertContent("true");
		assertLogged("[200] HTTP GET /c");

		a4.get("/d").run().assertContent("false");
		assertNotLogged();
		a4d.get("/d").run().assertContent("true");
		assertLogged("[200] HTTP GET /d");

		a4.get("/e").run().assertContent("false");
		assertNotLogged();
		a4d.get("/e").run().assertContent("true");
		assertLogged();

		a4.get("/f").run().assertContent("true");
		assertLogged();
		a4d.get("/f").run().assertContent("true");
		assertLogged();

		a4.get("/g").run().assertContent("false");
		assertNotLogged();
		a4d.get("/g").run().assertContent("false");
		assertNotLogged();
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debugOn=""), various @RestOp(debug)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		callLogger=CaptureLogger.class,
		debugOn=""
			+ "C1.b1=false,C1.b2=false,C1.b3=FALSE,C1.b4=FALSE,C1.b5=FALSE,C1.b6=FALSE,"
			+ " C1.c1 , C1.c2 = true , C1.c3 = TRUE , C1.c4 = TRUE , C1.c5 = TRUE , C1.c6 = TRUE , "
			+ "C1.d1=conditional,C1.d2=conditional,C1.d3=CONDITIONAL,C1.d4=CONDITIONAL,C1.d5=CONDITIONAL,C1.d6=CONDITIONAL,"
			+ "C1.e1=foo,C1.e2,C1.e3=foo,C1.e4=foo,C1.e5=foo,C1.e6=foo,"
	)
	public static class C1 implements BasicUniversalConfig {

		@RestOp
		public boolean a1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean a2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean a3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean a4(RestRequest req) {
			return req.isDebug();
		}

		// debug=false
		@RestOp
		public boolean b1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean b2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean b3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean b4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean b5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean b6(RestRequest req) {
			return req.isDebug();
		}

		// debug=true
		@RestOp
		public boolean c1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean c2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean c3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean c4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean c5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean c6(RestRequest req) {
			return req.isDebug();
		}

		// debug=conditional
		@RestOp
		public boolean d1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean d2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean d3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean d4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean d5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean d6(RestRequest req) {
			return req.isDebug();
		}

		// debug=foo
		@RestOp
		public boolean e1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean e2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean e3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean e4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean e5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean e6(RestRequest req) {
			return req.isDebug();
		}

		@RestOp
		public boolean f1(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean f2(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean f3(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean f4(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}

		@RestOp
		public boolean g1(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean g2(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean g3(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean g4(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void c01_debugDefault() throws Exception {
		RestClient c1 = MockRestClient.buildJson5(C1.class);
		RestClient c1d = MockRestClient.create(C1.class).json5().debug().suppressLogging().build();

		c1.get("/a1").run().assertContent("false");
		assertNotLogged();
		c1d.get("/a1").run().assertContent("true");
		assertLogged("[200] HTTP GET /a1");
		c1.get("/a2").run().assertContent("false");
		assertNotLogged();
		c1d.get("/a2").run().assertContent("false");
		assertNotLogged();
		c1.get("/a3").run().assertContent("true");
		assertLogged();
		c1d.get("/a3").run().assertContent("true");
		assertLogged();
		c1.get("/a4").run().assertContent("false");
		assertNotLogged();
		c1d.get("/a4").run().assertContent("true");
		assertLogged();

		c1.get("/b1").run().assertContent("false");
		assertNotLogged();
		c1d.get("/b1").run().assertContent("false");
		assertNotLogged();
		c1.get("/b2").run().assertContent("false");
		assertNotLogged();
		c1d.get("/b2").run().assertContent("false");
		assertNotLogged();
		c1.get("/b3").run().assertContent("false");
		assertNotLogged();
		c1d.get("/b3").run().assertContent("false");
		assertNotLogged();
		c1.get("/b4").run().assertContent("false");
		assertNotLogged();
		c1d.get("/b4").run().assertContent("false");
		assertNotLogged();
		c1.get("/b5").run().assertContent("true");
		assertLogged();
		c1d.get("/b5").run().assertContent("true");
		assertLogged();
		c1.get("/b6").run().assertContent("false");
		assertNotLogged();
		c1d.get("/b6").run().assertContent("true");
		assertLogged();

		c1.get("/c1").run().assertContent("true");
		assertLogged("[200] HTTP GET /c1");
		c1d.get("/c1").run().assertContent("true");
		assertLogged("[200] HTTP GET /c1");
		c1.get("/c2").run().assertContent("true");
		assertLogged("[200] HTTP GET /c2");
		c1d.get("/c2").run().assertContent("true");
		assertLogged("[200] HTTP GET /c2");
		c1.get("/c3").run().assertContent("true");
		assertLogged("[200] HTTP GET /c3");
		c1d.get("/c3").run().assertContent("true");
		assertLogged("[200] HTTP GET /c3");
		c1.get("/c4").run().assertContent("false");
		assertNotLogged();
		c1d.get("/c4").run().assertContent("false");
		assertNotLogged();
		c1.get("/c5").run().assertContent("true");
		assertLogged("[200] HTTP GET /c5");
		c1d.get("/c5").run().assertContent("true");
		assertLogged("[200] HTTP GET /c5");
		c1.get("/c6").run().assertContent("false");
		assertNotLogged();
		c1d.get("/c6").run().assertContent("true");
		assertLogged("[200] HTTP GET /c6");

		c1.get("/d1").run().assertContent("false");
		assertNotLogged();
		c1d.get("/d1").run().assertContent("true");
		assertLogged("[200] HTTP GET /d1");
		c1.get("/d2").run().assertContent("false");
		assertNotLogged();
		c1d.get("/d2").run().assertContent("true");
		assertLogged("[200] HTTP GET /d2");
		c1.get("/d3").run().assertContent("false");
		assertNotLogged();
		c1d.get("/d3").run().assertContent("true");
		assertLogged("[200] HTTP GET /d3");
		c1.get("/d4").run().assertContent("false");
		assertNotLogged();
		c1d.get("/d4").run().assertContent("false");
		assertNotLogged();
		c1.get("/d5").run().assertContent("true");
		assertLogged("[200] HTTP GET /d5");
		c1d.get("/d5").run().assertContent("true");
		assertLogged("[200] HTTP GET /d5");
		c1.get("/d6").run().assertContent("false");
		assertNotLogged();
		c1d.get("/d6").run().assertContent("true");
		assertLogged("[200] HTTP GET /d6");

		c1.get("/e1").run().assertContent("false");
		assertNotLogged();
		c1d.get("/e1").run().assertContent("true");
		assertLogged("[200] HTTP GET /e1");
		c1.get("/e2").run().assertContent("true");
		assertLogged("[200] HTTP GET /e2");
		c1d.get("/e2").run().assertContent("true");
		assertLogged("[200] HTTP GET /e2");
		c1.get("/e3").run().assertContent("false");
		assertNotLogged();
		c1d.get("/e3").run().assertContent("true");
		assertLogged("[200] HTTP GET /e3");
		c1.get("/e4").run().assertContent("false");
		assertNotLogged();
		c1d.get("/e4").run().assertContent("false");
		assertNotLogged();
		c1.get("/e5").run().assertContent("true");
		assertLogged("[200] HTTP GET /e5");
		c1d.get("/e5").run().assertContent("true");
		assertLogged("[200] HTTP GET /e5");
		c1.get("/e6").run().assertContent("false");
		assertNotLogged();
		c1d.get("/e6").run().assertContent("true");
		assertLogged("[200] HTTP GET /e6");

		c1.get("/f1").run().assertContent("true");
		assertLogged();
		c1d.get("/f1").run().assertContent("true");
		assertLogged();
		c1.get("/f2").run().assertContent("true");
		assertLogged();
		c1d.get("/f2").run().assertContent("true");
		assertLogged();
		c1.get("/f3").run().assertContent("true");
		assertLogged();
		c1d.get("/f3").run().assertContent("true");
		assertLogged();
		c1.get("/f4").run().assertContent("true");
		assertLogged();
		c1d.get("/f4").run().assertContent("true");
		assertLogged();

		c1.get("/g1").run().assertContent("false");
		assertNotLogged();
		c1d.get("/g1").run().assertContent("false");
		assertNotLogged();
		c1.get("/g2").run().assertContent("false");
		assertNotLogged();
		c1d.get("/g2").run().assertContent("false");
		assertNotLogged();
		c1.get("/g3").run().assertContent("false");
		assertNotLogged();
		c1d.get("/g3").run().assertContent("false");
		assertNotLogged();
		c1.get("/g4").run().assertContent("false");
		assertNotLogged();
		c1d.get("/g4").run().assertContent("false");
		assertNotLogged();
	}

	static {
		System.setProperty("C2DebugEnabled", "C2=true");
	}
	@Rest(
		callLogger=CaptureLogger.class,
		debugOn="$S{C2DebugEnabled},"
			+ "C2.b1=false,C2.b2=false,C2.b3=FALSE,C2.b4=FALSE,C2.b5=FALSE,C2.b6=FALSE,"
			+ " C2.c1 , C2.c2 = true , C2.c3 = TRUE , C2.c4 = TRUE , C2.c5 = TRUE , C2.c6 = TRUE , "
			+ "C2.d1=conditional,C2.d2=conditional,C2.d3=CONDITIONAL,C2.d4=CONDITIONAL,C2.d5=CONDITIONAL,C2.d6=CONDITIONAL,"
			+ "C2.e1=foo,C2.e2=,C2.e3=foo,C2.e4=foo,C2.e5=foo,C2.e6=foo,"
	)
	public static class C2 implements BasicUniversalConfig {

		@RestOp
		public boolean a1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean a2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean a3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean a4(RestRequest req) {
			return req.isDebug();
		}

		// debug=false
		@RestOp
		public boolean b1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean b2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean b3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean b4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean b5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean b6(RestRequest req) {
			return req.isDebug();
		}

		// debug=true
		@RestOp
		public boolean c1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean c2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean c3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean c4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean c5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean c6(RestRequest req) {
			return req.isDebug();
		}

		// debug=conditional
		@RestOp
		public boolean d1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean d2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean d3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean d4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean d5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean d6(RestRequest req) {
			return req.isDebug();
		}

		// debug=foo
		@RestOp
		public boolean e1(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean e2(RestRequest req) {
			return req.isDebug();
		}
		@RestOp
		public boolean e3(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean e4(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean e5(RestRequest req) {
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean e6(RestRequest req) {
			return req.isDebug();
		}

		@RestOp
		public boolean f1(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean f2(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean f3(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean f4(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}

		@RestOp
		public boolean g1(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestOp(debug="false")
		public boolean g2(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestOp(debug="true")
		public boolean g3(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
		@RestOp(debug="conditional")
		public boolean g4(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}
	}

	@Test
	public void c02_debugTrue() throws Exception {
		RestClient c2 = MockRestClient.buildJson5(C2.class);
		RestClient c2d = MockRestClient.create(C2.class).json5().debug().suppressLogging().build();

		c2.get("/a1").run().assertContent("true");
		assertLogged();
		c2d.get("/a1").run().assertContent("true");
		assertLogged();
		c2.get("/a2").run().assertContent("false");
		assertNotLogged();
		c2d.get("/a2").run().assertContent("false");
		assertNotLogged();
		c2.get("/a3").run().assertContent("true");
		assertLogged();
		c2d.get("/a3").run().assertContent("true");
		assertLogged();
		c2.get("/a4").run().assertContent("false");
		assertNotLogged();
		c2d.get("/a4").run().assertContent("true");
		assertLogged();

		c2.get("/b1").run().assertContent("false");
		assertNotLogged();
		c2d.get("/b1").run().assertContent("false");
		assertNotLogged();
		c2.get("/b2").run().assertContent("false");
		assertNotLogged();
		c2d.get("/b2").run().assertContent("false");
		assertNotLogged();
		c2.get("/b3").run().assertContent("false");
		assertNotLogged();
		c2d.get("/b3").run().assertContent("false");
		assertNotLogged();
		c2.get("/b4").run().assertContent("false");
		assertNotLogged();
		c2d.get("/b4").run().assertContent("false");
		assertNotLogged();
		c2.get("/b5").run().assertContent("true");
		assertLogged();
		c2d.get("/b5").run().assertContent("true");
		assertLogged();
		c2.get("/b6").run().assertContent("false");
		assertNotLogged();
		c2d.get("/b6").run().assertContent("true");
		assertLogged();

		c2.get("/c1").run().assertContent("true");
		assertLogged("[200] HTTP GET /c1");
		c2d.get("/c1").run().assertContent("true");
		assertLogged("[200] HTTP GET /c1");
		c2.get("/c2").run().assertContent("true");
		assertLogged("[200] HTTP GET /c2");
		c2d.get("/c2").run().assertContent("true");
		assertLogged("[200] HTTP GET /c2");
		c2.get("/c3").run().assertContent("true");
		assertLogged("[200] HTTP GET /c3");
		c2d.get("/c3").run().assertContent("true");
		assertLogged("[200] HTTP GET /c3");
		c2.get("/c4").run().assertContent("false");
		assertNotLogged();
		c2d.get("/c4").run().assertContent("false");
		assertNotLogged();
		c2.get("/c5").run().assertContent("true");
		assertLogged("[200] HTTP GET /c5");
		c2d.get("/c5").run().assertContent("true");
		assertLogged("[200] HTTP GET /c5");
		c2.get("/c6").run().assertContent("false");
		assertNotLogged();
		c2d.get("/c6").run().assertContent("true");
		assertLogged("[200] HTTP GET /c6");

		c2.get("/d1").run().assertContent("false");
		assertNotLogged();
		c2d.get("/d1").run().assertContent("true");
		assertLogged("[200] HTTP GET /d1");
		c2.get("/d2").run().assertContent("false");
		assertNotLogged();
		c2d.get("/d2").run().assertContent("true");
		assertLogged("[200] HTTP GET /d2");
		c2.get("/d3").run().assertContent("false");
		assertNotLogged();
		c2d.get("/d3").run().assertContent("true");
		assertLogged("[200] HTTP GET /d3");
		c2.get("/d4").run().assertContent("false");
		assertNotLogged();
		c2d.get("/d4").run().assertContent("false");
		assertNotLogged();
		c2.get("/d5").run().assertContent("true");
		assertLogged("[200] HTTP GET /d5");
		c2d.get("/d5").run().assertContent("true");
		assertLogged("[200] HTTP GET /d5");
		c2.get("/d6").run().assertContent("false");
		assertNotLogged();
		c2d.get("/d6").run().assertContent("true");
		assertLogged("[200] HTTP GET /d6");

		c2.get("/e1").run().assertContent("true");
		assertLogged();
		c2d.get("/d1").run().assertContent("true");
		assertLogged();
		c2.get("/e2").run().assertContent("true");
		assertLogged();
		c2d.get("/e2").run().assertContent("true");
		assertLogged();
		c2.get("/e3").run().assertContent("true");
		assertLogged();
		c2d.get("/e3").run().assertContent("true");
		assertLogged();
		c2.get("/e4").run().assertContent("false");
		assertNotLogged();
		c2d.get("/e4").run().assertContent("false");
		assertNotLogged();
		c2.get("/e5").run().assertContent("true");
		assertLogged();
		c2d.get("/e5").run().assertContent("true");
		assertLogged();
		c2.get("/e6").run().assertContent("false");
		assertNotLogged();
		c2d.get("/e6").run().assertContent("true");
		assertLogged();

		c2.get("/f1").run().assertContent("true");
		assertLogged();
		c2d.get("/f1").run().assertContent("true");
		assertLogged();
		c2.get("/f2").run().assertContent("true");
		assertLogged();
		c2d.get("/f2").run().assertContent("true");
		assertLogged();
		c2.get("/f3").run().assertContent("true");
		assertLogged();
		c2d.get("/f3").run().assertContent("true");
		assertLogged();
		c2.get("/f4").run().assertContent("true");
		assertLogged();
		c2d.get("/f4").run().assertContent("true");
		assertLogged();

		c2.get("/g1").run().assertContent("false");
		assertNotLogged();
		c2d.get("/g1").run().assertContent("false");
		assertNotLogged();
		c2.get("/g2").run().assertContent("false");
		assertNotLogged();
		c2d.get("/g2").run().assertContent("false");
		assertNotLogged();
		c2.get("/g3").run().assertContent("false");
		assertNotLogged();
		c2d.get("/g3").run().assertContent("false");
		assertNotLogged();
		c2.get("/g4").run().assertContent("false");
		assertNotLogged();
		c2d.get("/g4").run().assertContent("false");
		assertNotLogged();
	}

}
