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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/** Focused typed-debug coverage for TODO-20 migration. */
class Rest_Debug_Test extends TestBase {

	public static final CaptureLogger LOGGER = new CaptureLogger();

	public static class CaptureLogger extends BasicTestCaptureCallLogger {
		public static CaptureLogger getInstance() {
			return LOGGER;
		}
	}

	private static void assertLogged() {
		LOGGER.assertMessageAndReset().isExists();
	}

	@SuppressWarnings("unused")
	private static void assertNotLogged() {
		LOGGER.assertMessageAndReset().isNull();
	}

	@Rest(callLogger=CaptureLogger.class, debug=@Debug("always"))
	public static class A implements BasicUniversalConfig {
		@RestOp(path="/a")
		public boolean a(RestRequest req) {
			return req.isDebug();
		}

		@RestOp(path="/b", debug=@Debug("never"))
		public boolean b(RestRequest req) {
			return req.isDebug();
		}

		@RestOp(path="/c", debug=@Debug("conditional"))
		public boolean c(RestRequest req) {
			return req.isDebug();
		}

		@RestOp(path="/d")
		public boolean d(RestRequest req) throws Exception {
			req.setDebug(false);
			return req.isDebug();
		}

		@RestOp(path="/e")
		public boolean e(RestRequest req) throws Exception {
			req.setDebug();
			return req.isDebug();
		}
	}

	@Test void a01_typedRestDebugAndRestOpOverride() throws Exception {
		var c = MockRestClient.buildJson5(A.class);
		c.get("/a").run().assertContent("true");
		assertLogged();
		c.get("/b").run().assertContent("true");
		assertLogged();
	}

	@Test void a02_typedConditionalRestOp() throws Exception {
		var c = MockRestClient.buildJson5(A.class);
		c.get("/c").run().assertContent("true");
		assertLogged();
		c.get("/c").header("Debug", "true").run().assertContent("true");
		assertLogged();
	}

	@Test void a03_runtimeShortcutCompatibility() throws Exception {
		var c = MockRestClient.buildJson5(A.class);
		c.get("/d").run().assertContent("false");
		assertLogged();
		c.get("/e").run().assertContent("true");
		assertLogged();
	}
}
