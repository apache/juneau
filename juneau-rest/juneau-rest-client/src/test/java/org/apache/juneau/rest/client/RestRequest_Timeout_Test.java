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

import org.junit.jupiter.api.*;

/**
 * Verifies the positive-duration rule for per-request timeouts (WORK-J0596).
 */
@SuppressWarnings({
	"resource" // Client is a short-lived test fixture.
})
class RestRequest_Timeout_Test {

	private static RestClient client() {
		return RestClient.builder().transport(req -> TransportResponse.builder().statusCode(200).build()).build();
	}

	@Test void a01_zeroTimeout_rejected() throws Exception {
		try (var c = client()) {
			var req = c.get("http://x/");
			var e = assertThrows(IllegalArgumentException.class, () -> req.timeout(Duration.ZERO));
			assertTrue(e.getMessage().contains("must be positive"));
		}
	}

	@Test void a02_negativeTimeout_rejected() throws Exception {
		try (var c = client()) {
			var req = c.get("http://x/");
			var negative = Duration.ofMillis(-1);
			assertThrows(IllegalArgumentException.class, () -> req.timeout(negative));
		}
	}

	@Test void a03_nullAndPositive_accepted() throws Exception {
		try (var c = client()) {
			assertDoesNotThrow(() -> c.get("http://x/").timeout(null));
			assertDoesNotThrow(() -> c.get("http://x/").timeout(Duration.ofMillis(1)));
		}
	}

	@Test void a04_transportRequestBuilder_rejectsNonPositive() {
		var builder = TransportRequest.builder();
		var negative = Duration.ofSeconds(-1);
		assertThrows(IllegalArgumentException.class, () -> builder.timeout(Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> builder.timeout(negative));
	}
}
