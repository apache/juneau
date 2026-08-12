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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestLogEntry} constructed directly via {@link RestLogEntry#builder()}, covering the
 * plain accessors ({@link RestLogEntry#getElapsed()}, {@link RestLogEntry#toString()}) and the builder's
 * {@link RestLogEntry.Builder#levelResolver(RestLogLevelResolver)} override, none of which require an actual
 * transport round-trip.
 */
class RestLogEntry_Test extends TestBase {

	@SuppressWarnings({
		"resource" // Factory returns a Closeable for the caller to close; Eclipse JDT @Owning warning is by design.
	})
	private static RestResponse response(int statusCode) {
		var b = TransportResponse.builder().statusCode(statusCode);
		return new RestResponse(b.build(), RestClient.create());
	}

	@Test
	void a01_getElapsed_returnsConfiguredDuration() throws Exception {
		try (var client = RestClient.create()) {
			var req = client.get("http://x/");
			try (var resp = response(200)) {
				var entry = RestLogEntry.builder().request(req).response(resp).elapsed(Duration.ofMillis(42)).build();
				assertEquals(Duration.ofMillis(42), entry.getElapsed());
			}
		}
	}

	@Test
	void a02_toString_delegatesToDefaultFormat() throws Exception {
		try (var client = RestClient.create()) {
			var req = client.get("http://x/");
			try (var resp = response(200)) {
				var entry = RestLogEntry.builder().request(req).response(resp).elapsed(Duration.ofMillis(7)).build();
				assertEquals(entry.format(), entry.toString());
				assertTrue(entry.toString().contains("GET"), "Unexpected: " + entry.toString());
			}
		}
	}

	@Test
	void a03_levelResolver_overrideIsUsedToComputeLevel() throws Exception {
		try (var client = RestClient.create()) {
			var req = client.get("http://x/");
			try (var resp = response(200)) {
				var entry = RestLogEntry.builder().request(req).response(resp)
						.levelResolver(e -> System.Logger.Level.ERROR)
						.build();
				assertEquals(System.Logger.Level.ERROR, entry.getLevel());
			}
		}
	}

	@Test
	void a04_debugFlagAndError_propagateThroughBuilder() throws Exception {
		try (var client = RestClient.create()) {
			var req = client.get("http://x/");
			var ex = new RuntimeException("boom");
			var entry = RestLogEntry.builder().request(req).error(ex).debug(true).build();
			assertTrue(entry.isDebug());
			assertSame(ex, entry.getError());
			assertTrue(entry.isError());
			assertEquals(0, entry.getStatusCode());
			assertFalse(entry.hasResponseHeader("X-Any"));
		}
	}
}
