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

package org.apache.juneau.releng.nexus;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.http.entity.StringBody;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.Test;

class NexusMockRestRoutingTest {

	private static final String BODY = "{\"data\":{\"stagedRepositoryIds\":[\"%s\"]}}";

	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources).
	})
	private static MockRestClient client(NexusMockRest rest) {
		return MockRestClient.builder(rest).overridingBeanStore(new StackOverlay()).build();
	}

	@Test
	void a01_unmappedRequestFails() {
		var m = new NexusMockModel("p");
		var e = assertThrows(IllegalStateException.class, () -> NexusMockRest.route(m, "GET", "/nope", null));
		assertTrue(e.getMessage().contains("Unmapped"), e.getMessage());
		assertThrows(IllegalStateException.class, () -> NexusMockRest.route(m, "POST", "/service/x/bulk/other", "{}"));
		assertThrows(IllegalStateException.class, () -> NexusMockRest.route(m, "DELETE", "/service/local/staging/repository/x", null));
	}

	@Test
	void a02_routeWalksCloseThenPromote() {
		var m = new NexusMockModel("p");
		NexusMockRest.route(m, "GET", "/service/local/staging/profile_repositories/p", null);
		var id = m.currentRepoId();
		assertTrue(NexusMockRest.route(m, "GET", "/service/local/staging/repository/" + id, null).contains(id));
		assertEquals("", NexusMockRest.route(m, "POST", "/service/local/staging/bulk/close", BODY.formatted(id)));
		assertEquals("", NexusMockRest.route(m, "POST", "/service/local/staging/bulk/promote", BODY.formatted(id)));
		assertEquals(NexusMockModel.RELEASED, m.currentState());
	}

	@Test
	void a03_routeDrop() {
		var m = new NexusMockModel("p");
		NexusMockRest.route(m, "GET", "/service/local/staging/profile_repositories/p", null);
		NexusMockRest.route(m, "POST", "/service/local/staging/bulk/drop", BODY.formatted(m.currentRepoId()));
		assertEquals(NexusMockModel.DROPPED, m.currentState());
	}

	@Test
	void b01_httpBulkEndpointsAndModelErrorsBecome400() throws Exception {
		var rest = new NexusMockRest("p");
		try (var client = client(rest)) {
			try (var r = client.request("GET", "/service/local/staging/profile_repositories/p").run()) {
				assertEquals(200, r.getStatusCode());
			}
			var id = rest.model().currentRepoId();
			try (var r = client.request("GET", "/service/local/staging/repository/" + id).run()) {
				assertEquals(200, r.getStatusCode());
			}
			try (var r = client.request("POST", "/service/local/staging/bulk/close")
					.body(StringBody.of(BODY.formatted(id), "application/json")).run()) {
				assertEquals(200, r.getStatusCode());
			}
			try (var r = client.request("POST", "/service/local/staging/bulk/promote")
					.body(StringBody.of(BODY.formatted(id), "application/json")).run()) {
				assertEquals(200, r.getStatusCode());
			}
			// a released repo cannot be dropped
			try (var r = client.request("POST", "/service/local/staging/bulk/drop")
					.body(StringBody.of(BODY.formatted(id), "application/json")).run()) {
				assertEquals(400, r.getStatusCode());
				assertTrue(r.getBodyAsString().contains("illegalTransition"), r.getBodyAsString());
			}
			try (var r = client.request("GET", "/service/local/staging/repository/bogus").run()) {
				assertEquals(400, r.getStatusCode());
			}
		}
	}

	@Test
	void b02_httpDropOfOpenRepoSucceeds() throws Exception {
		var rest = new NexusMockRest("p");
		try (var client = client(rest)) {
			try (var r = client.request("GET", "/service/local/staging/profile_repositories/p").run()) {
				assertEquals(200, r.getStatusCode());
			}
			try (var r = client.request("POST", "/service/local/staging/bulk/drop")
					.body(StringBody.of(BODY.formatted(rest.model().currentRepoId()), "application/json")).run()) {
				assertEquals(200, r.getStatusCode());
			}
			assertEquals(NexusMockModel.DROPPED, rest.model().currentState());
		}
	}
}
