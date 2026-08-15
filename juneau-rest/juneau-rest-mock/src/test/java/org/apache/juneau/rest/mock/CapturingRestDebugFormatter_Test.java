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

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.logging.*;
import org.apache.juneau.test.junit.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end tests for {@link CapturingRestDebugFormatter} dispatched through
 * {@code mock.classic.MockRestClient} &mdash; exercises the classic client's {@code Builder.debug()} &rarr;
 * {@code MockServletRequest.logLevel(...)} &rarr; save/restore-around-dispatch path that replaced the removed
 * {@code BasicTestCaptureCallLogger}.
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // MockRestClient instances are short-lived test fixtures.
class CapturingRestDebugFormatter_Test {

	@Rest(path="/api")
	public static class A_Resource {

		@RestGet(path="/who")
		public String who() {
			return "ok";
		}

		@RestGet(path="/err")
		public String err(RestResponse res) {
			// Resource code explicitly reports a handled exception via RestResponse#setException(Throwable),
			// which is what CapturingRestDebugFormatter.getThrown()/assertThrown() surface after dispatch.
			res.setException(new RuntimeException("boom"));
			return "handled";
		}
	}

	@Test void a01_debugEnabled_capturesAtFinestTier() throws Exception {
		var formatter = new CapturingRestDebugFormatter();
		var overlay = new TestBeanStore().override(RestDebugFormatter.class, formatter);

		var client = org.apache.juneau.rest.mock.classic.MockRestClient
			.create(A_Resource.class)
			.overridingBeanStore(overlay)
			.debug()
			.build();

		client.get("/who").run().getContent().asString();

		assertEquals(Level.FINEST, formatter.getLevel());
		formatter.assertMessage().isContains("[200] HTTP GET /api/who");
		assertNull(formatter.getThrown());
	}

	@Test void a02_assertMessageAndReset_clearsCapturedState() throws Exception {
		var formatter = new CapturingRestDebugFormatter();
		var overlay = new TestBeanStore().override(RestDebugFormatter.class, formatter);

		var client = org.apache.juneau.rest.mock.classic.MockRestClient
			.create(A_Resource.class)
			.overridingBeanStore(overlay)
			.debug()
			.build();

		client.get("/who").run().getContent().asString();

		formatter.assertMessageAndReset().isContains("HTTP GET");
		assertNull(formatter.getMessage());
		assertNull(formatter.getLevel());
	}

	@Test void a03_debugDisabled_leavesResourceLoggerLevelUnchanged() throws Exception {
		var target = Logger.getLogger(A_Resource.class.getName());
		var prevLevel = target.getLevel();
		target.setLevel(Level.OFF);
		try {
			var formatter = new CapturingRestDebugFormatter();
			var overlay = new TestBeanStore().override(RestDebugFormatter.class, formatter);

			// No .debug() call -> logLevel(null) -> the save/restore wrapper is never engaged.
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.overridingBeanStore(overlay)
				.build();

			client.get("/who").run().getContent().asString();

			assertNull(formatter.getMessage(), "Nothing should be captured below the resource logger's resolved level");
			assertEquals(Level.OFF, target.getLevel(), "Logger level must be left untouched when no debug override is requested");
		} finally {
			target.setLevel(prevLevel);
		}
	}

	@Test void a04_thrownExceptionIsCaptured() throws Exception {
		var formatter = new CapturingRestDebugFormatter();
		var overlay = new TestBeanStore().override(RestDebugFormatter.class, formatter);

		var client = org.apache.juneau.rest.mock.classic.MockRestClient
			.create(A_Resource.class)
			.overridingBeanStore(overlay)
			.debug()
			.build();

		client.get("/err").run().assertStatus().asCode().is(200);

		assertNotNull(formatter.getThrown());
		formatter.assertThrown().asMessage().is("boom");
	}
}
