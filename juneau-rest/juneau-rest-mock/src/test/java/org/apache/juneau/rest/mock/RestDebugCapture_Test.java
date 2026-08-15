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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end REST debug capture tests through {@code mock.classic.MockRestClient}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RestDebugCapture_Test {

	@Rest(path="/api")
	public static class A_Resource {

		@RestGet(path="/who")
		public String who() {
			return "ok";
		}

		@RestGet(path="/err")
		public String err(RestResponse res) {
			res.setException(new RuntimeException("boom"));
			return "handled";
		}
	}

	public static class A05_Mixin {
		@RestGet(path="/who")
		public String who() {
			return "ok";
		}
	}

	@Rest(path="/mix", mixins=A05_Mixin.class)
	public static class A05_HostResource {}

	@Test void a01_debugEnabled_capturesAtFinestTier() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.debug()
				.build();

			client.get("/who").run().getContent().asString();

			assertFalse(c.isEmpty());
			assertEquals(Level.FINEST, c.last().getLevel());
			assertTrue(c.last().getMessage().contains("[200] HTTP GET /api/who"));
			assertNull(c.last().getThrown());
		}
	}

	@Test void a02_clear_resetsCapturedState() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.debug()
				.build();

			client.get("/who").run().getContent().asString();
			assertFalse(c.isEmpty());

			c.clear();
			assertTrue(c.isEmpty());
			assertNull(c.last());

			client.get("/who").run().getContent().asString();
			assertFalse(c.isEmpty());
		}
	}

	@Test void a03_debugDisabled_leavesResourceLoggerLevelUnchanged() throws Exception {
		var target = Logger.getLogger(A_Resource.class.getName());
		var prevLevel = target.getLevel();
		target.setLevel(Level.OFF);
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.build();

			client.get("/who").run().getContent().asString();

			assertTrue(c.isEmpty(), "No records should be captured below the resolved logger tier");
			assertEquals(Level.OFF, target.getLevel(), "Logger level should remain unchanged without .debug()");
		} finally {
			target.setLevel(prevLevel);
		}
	}

	@Test void a04_thrownExceptionIsCaptured() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.debug()
				.build();

			client.get("/err").run().assertStatus().asCode().is(200);

			assertNotNull(c.last());
			assertNotNull(c.last().getThrown());
			assertEquals("boom", c.last().getThrown().getMessage());
		}
	}

	@Test void a05_captureByHostName_observesOpAndNoOpLoggerPaths() throws Exception {
		var hostName = A05_HostResource.class.getName();
		try (var c = RichLogger.getLogger(A05_HostResource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A05_HostResource.class)
				.debug()
				.build();

			client.get("/who").run().assertStatus().asCode().is(200);
			client.get("/missing").ignoreErrors().run().assertStatus().asCode().is(404);

			var records = c.getRecords();
			assertTrue(records.stream().map(java.util.logging.LogRecord::getLoggerName).anyMatch((hostName + ".who")::equals));
			assertTrue(records.stream().map(java.util.logging.LogRecord::getLoggerName).anyMatch(hostName::equals));

			var noOpRecord = records.stream()
				.filter(x -> hostName.equals(x.getLoggerName()))
				.reduce((a, b) -> b)
				.orElse(null);
			assertNotNull(noOpRecord);
			assertTrue(noOpRecord.getMessage().contains("[404] HTTP GET /mix/missing"));
		}
	}
}
