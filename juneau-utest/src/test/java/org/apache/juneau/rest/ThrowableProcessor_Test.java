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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that exception class and message detail is not sent to clients for unplanned exceptions unless explicitly enabled.
 */
class ThrowableProcessor_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet
		public String unplanned() { throw new IllegalStateException("internal-detail-xyz"); }

		@RestGet
		public String planned() { throw new BadRequest("client-facing-message"); }
	}

	@Rest(renderResponseStackTraces="true")
	public static class B extends A {}

	@Test void a01_unplannedDefaultHidesDetail() throws Exception {
		var r = MockRestClient.buildLax(A.class).get("/unplanned").run();
		r.assertStatus(500);
		assertFalse(r.getHeader("Thrown").isPresent());
		var body = r.getContent().asString();
		assertFalse(body.contains("internal-detail-xyz"), body);
		assertFalse(body.contains("IllegalStateException"), body);
	}

	@Test void a02_unplannedOptInShowsDetail() throws Exception {
		var r = MockRestClient.buildLax(B.class).get("/unplanned").run();
		r.assertStatus(500);
		assertTrue(r.getHeader("Thrown").isPresent());
	}

	@Test void a03_plannedKeepsBehavior() throws Exception {
		var r = MockRestClient.buildLax(A.class).get("/planned").run();
		r.assertStatus(400);
		assertTrue(r.getContent().asString().contains("client-facing-message"));
	}
}
