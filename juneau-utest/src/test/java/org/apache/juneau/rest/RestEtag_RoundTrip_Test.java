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

import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end "round trip" tests for the ETag / conditional-GET helpers in a realistic handler:
 *
 * <ul>
 *   <li>First request gets the resource with {@code 200 OK} plus {@code ETag} and {@code Last-Modified} headers.
 *   <li>Second request re-sends the same {@code ETag} via {@code If-None-Match} and gets {@code 304 Not Modified}.
 *   <li>An update bumps the version + timestamp; the next request gets {@code 200 OK} with the new tag/date.
 *   <li>A {@code PUT} with a stale {@code If-Match} is rejected with {@code 412 Precondition Failed}.
 * </ul>
 */
class RestEtag_RoundTrip_Test extends TestBase {

	/** Trivial in-memory order with a version + updated-instant for ETag/Last-Modified. */
	public static class Order {
		public String id, payload;
		public long version;
		public Instant updated;
		public Order() {}
		public Order(String id, String payload, long version, Instant updated) {
			this.id = id; this.payload = payload; this.version = version; this.updated = updated;
		}
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class R {
		private final ConcurrentMap<String,Order> repo = new ConcurrentHashMap<>();

		public R() {
			repo.put("1", new Order("1", "v1-payload", 1L, Instant.parse("2026-05-22T00:00:00Z")));
		}

		@RestGet("/orders/{id}")
		public Order get(@Path("id") String id, RestRequest req, RestResponse res) {
			var order = repo.get(id);
			res.eTag("\"" + order.version + "\"").lastModified(order.updated);
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			return order;
		}

		@RestPut("/orders/{id}")
		public Order put(@Path("id") String id, @Content Order in, RestRequest req, RestResponse res) {
			var current = repo.get(id);
			res.eTag("\"" + current.version + "\"").lastModified(current.updated);
			req.checkPreconditions(res).ifPresent(e -> { throw e; });
			var next = new Order(id, in.payload, current.version + 1L, Instant.parse("2026-05-23T00:00:00Z"));
			repo.put(id, next);
			res.eTag("\"" + next.version + "\"").lastModified(next.updated);
			return next;
		}
	}

	@Test void a01_firstGetReturnsEtagAndLastModified() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/orders/1").run().assertStatus(200)
			.assertHeader("ETag").is("\"1\"")
			.assertHeader("Last-Modified").is("Fri, 22 May 2026 00:00:00 GMT");
	}

	@Test void a02_secondGetWithIfNoneMatchReturns304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/orders/1").header("If-None-Match", "\"1\"").run().assertStatus(304);
	}

	@Test void a03_getWithIfModifiedSinceEqualReturns304() throws Exception {
		var c = MockRestClient.buildLax(R.class);
		c.get("/orders/1").header("If-Modified-Since", "Fri, 22 May 2026 00:00:00 GMT").run().assertStatus(304);
	}

	@Test void a04_putWithStaleIfMatchReturns412() throws Exception {
		var c = MockRestClient.create(R.class).ignoreErrors().json().build();
		// Stale version: client thinks the order is at version 0 (its real version is 1).
		c.put("/orders/1", new Order("1", "bad", 0L, Instant.parse("2026-01-01T00:00:00Z")))
			.header("If-Match", "\"0\"").run().assertStatus(412);
	}

	@Test void a05_putWithCorrectIfMatchSucceedsAndBumpsEtag() throws Exception {
		var c = MockRestClient.create(R.class).ignoreErrors().json().build();
		c.put("/orders/1", new Order("1", "updated", 1L, Instant.parse("2026-01-01T00:00:00Z")))
			.header("If-Match", "\"1\"").run().assertStatus(200)
			.assertHeader("ETag").is("\"2\"")
			.assertHeader("Last-Modified").is("Sat, 23 May 2026 00:00:00 GMT");
		// After the bump, the original tag no longer matches — the next GET with the old tag returns 200.
		c.get("/orders/1").header("If-None-Match", "\"1\"").run().assertStatus(200);
		// And the new tag matches → 304.
		c.get("/orders/1").header("If-None-Match", "\"2\"").run().assertStatus(304);
	}
}
