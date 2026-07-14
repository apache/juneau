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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Guards that unexpected server-side exception detail (message, class name, stack trace) is not leaked to
 * HTTP clients in production, while remaining available in development when the
 * {@code renderResponseStackTraces} flag is enabled.
 */
class RestServer_ErrorExposure_Test extends TestBase {

	private static final String SECRET = "SECRET-DB-PASSWORD=hunter2";

	// Production (default): renderResponseStackTraces is off.
	@Rest
	public static class A {
		@RestGet public String boom() { throw new RuntimeException(SECRET); }
	}

	// Development: renderResponseStackTraces explicitly enabled.
	@Rest(renderResponseStackTraces="true")
	public static class B {
		@RestGet public String boom() { throw new RuntimeException(SECRET); }
	}

	@Test void a01_productionMode_hidesInternalExceptionDetail() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		var body = a.get("/boom").run().assertStatus(500).getContent().asString();
		assertFalse(body.contains(SECRET), "Production body leaked the exception message: " + body);
		assertFalse(body.contains("RuntimeException"), "Production body leaked the exception class name: " + body);
		assertTrue(body.contains("Internal Server Error"), "Production body should carry only the generic status text: " + body);
	}

	@Test void a02_debugMode_rendersDetail() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		var body = b.get("/boom").run().assertStatus(500).getContent().asString();
		assertTrue(body.contains(SECRET), "Debug mode (renderResponseStackTraces=true) should render exception detail: " + body);
	}
}
