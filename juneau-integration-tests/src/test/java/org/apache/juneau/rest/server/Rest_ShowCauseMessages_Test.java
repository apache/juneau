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
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@code showCauseMessages} setting: messages taken from an exception's cause are hidden by default.
 */
class Rest_ShowCauseMessages_Test extends TestBase {

	public static class Ops extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/wrapped") public String wrapped() { throw new InternalServerError(new IllegalStateException("cause-detail")); }
		@RestGet(path="/explicit") public String explicit() { throw new InternalServerError(new IllegalStateException("cause-detail"), "Explicit %s", "text"); }
		@RestGet(path="/notFound") public String notFound() { throw new NotFound("Unknown pet '%s'", 7); }
		@RestGet(path="/plain") public String plain() { throw new IllegalStateException("cause-detail"); }
	}

	@Rest
	public static class A extends Ops {
		private static final long serialVersionUID = 1L;
	}

	@Rest(showCauseMessages="true")
	public static class B extends Ops {
		private static final long serialVersionUID = 1L;
	}

	@Rest(problemDetails="true")
	public static class C extends Ops {
		private static final long serialVersionUID = 1L;
	}

	@Rest(problemDetails="true", showCauseMessages="true")
	public static class D extends Ops {
		private static final long serialVersionUID = 1L;
	}

	private static String body(Class<?> c, String path) throws Exception {
		return MockRestClient.buildLax(c).get(path).run().assertStatus(500).getContent().asString();
	}

	@Test void a01_off_causeMessageHidden() throws Exception {
		var b = body(A.class, "/wrapped");
		assertFalse(b.contains("cause-detail"), b);
		assertTrue(b.contains("Internal Server Error"), b);
	}

	@Test void a02_off_unplannedExceptionHidden() throws Exception {
		assertFalse(body(A.class, "/plain").contains("cause-detail"));
	}

	@Test void a03_on_causeMessageShown() throws Exception {
		var b = body(B.class, "/wrapped");
		assertTrue(b.contains("cause-detail"), b);
	}

	@Test void a04_explicitMessageAlwaysShown() throws Exception {
		assertTrue(body(A.class, "/explicit").contains("Explicit text"));
		assertTrue(body(B.class, "/explicit").contains("Explicit text"));
		var nf = MockRestClient.buildLax(A.class).get("/notFound").run().assertStatus(404).getContent().asString();
		assertTrue(nf.contains("Unknown pet '7'"), nf);
	}

	@Test void a05_problemDetails_off_hidden() throws Exception {
		var b = body(C.class, "/wrapped");
		assertFalse(b.contains("cause-detail"), b);
	}

	@Test void a06_problemDetails_on_shown() throws Exception {
		var b = body(D.class, "/wrapped");
		assertTrue(b.contains("cause-detail"), b);
	}

	@Test void a07_off_thrownHeaderHasNoCauseMessage() throws Exception {
		var h = MockRestClient.buildLax(A.class).get("/wrapped").run().assertStatus(500).getHeader("Thrown").asString().orElse("");
		assertFalse(h.contains("cause-detail"), h);
	}

	@Test void b01_hideKeepsMessageAndCauseForLogs() {
		var cause = new IllegalStateException("cause-detail");
		var e = new InternalServerError(cause);
		assertTrue(e.isMessageFromCause());
		e.hideCauseMessage();
		assertEquals("cause-detail", e.getMessage());
		assertSame(cause, e.getCause());
		assertEquals("Internal Server Error", e.getResponseMessage());
	}

	@Test void b02_explicitMessageIsNotFromCause() {
		var e = new InternalServerError(new IllegalStateException("x"), "Explicit %s", "y");
		assertFalse(e.isMessageFromCause());
		assertEquals("Explicit y", e.hideCauseMessage().getResponseMessage());
	}

	@Test void b03_noCauseMessage_notFromCause() {
		assertFalse(new InternalServerError(new IllegalStateException()).isMessageFromCause());
		assertFalse(new InternalServerError("m").isMessageFromCause());
	}
}
