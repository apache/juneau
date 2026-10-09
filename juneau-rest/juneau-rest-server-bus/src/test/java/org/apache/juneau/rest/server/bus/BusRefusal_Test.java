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
package org.apache.juneau.rest.server.bus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BusRefusal}: the named refusals, {@code Retry-After}, and the refusal body.
 */
class BusRefusal_Test extends TestBase {

	@Test void a01_namedRefusals() {
		var d = BusRefusal.topicDenied(List.of("ops.secret"));
		assertEquals(403, d.status());
		assertEquals("bus:topic-denied", d.code());
		assertEquals(List.of("ops.secret"), d.denied());
		assertEquals(400, BusRefusal.badRequest("m").status());
		assertEquals("bus:bad-request", BusRefusal.badRequest("m").code());
		assertEquals(400, BusRefusal.badFrame("m").status());
		assertEquals(501, BusRefusal.transportUnavailable().status());
		assertEquals("bus:transport-unavailable", BusRefusal.transportUnavailable().code());
	}

	@Test void a02_retryAfter() {
		var r = BusRefusal.tooManySessions();
		assertEquals(429, r.status());
		assertEquals("bus:too-many-sessions", r.code());
		assertEquals(OptionalInt.of(BusRefusal.TOO_MANY_SESSIONS_RETRY_AFTER_SECONDS), r.retryAfterSeconds());
		assertEquals(OptionalInt.empty(), BusRefusal.badFrame("m").retryAfterSeconds());
	}

	@Test void a03_toJson() {
		assertEquals("{\"code\":\"bus:topic-denied\",\"message\":\"the bus policy denies a requested topic\",\"denied\":[\"ops.a\",\"ops.b\"]}",
			BusRefusal.topicDenied(List.of("ops.a", "ops.b")).toJson().toString());
		var m = BusRefusal.badFrame("frame has no topic").toJson();
		assertEquals("{\"code\":\"bus:bad-frame\",\"message\":\"frame has no topic\"}", m.toString());
		assertFalse(m.containsKey("denied"));
	}

	@Test void a04_noStackTrace() {
		assertEquals(0, BusRefusal.badFrame("m").getStackTrace().length);
		assertNull(BusRefusal.badFrame("m").getCause());
	}
}
