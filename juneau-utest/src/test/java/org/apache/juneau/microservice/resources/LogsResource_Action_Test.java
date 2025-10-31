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
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.resources.LogsResource.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LogsResource.Action} fluent setter overrides.
 */
class LogsResource_Action_Test extends TestBase {

	@Test void a01_basic() {
		// Test basic constructor
		var x = new Action("view", "/logs/test.log");

		assertEquals("view", x.getName());
		assertTrue(x.getUri().toString().contains("/logs/test.log"));
	}

	@Test void a02_withArgs() {
		// Test constructor with URI args
		var x = new Action("view", "/logs/{0}", "test.log");

		assertEquals("view", x.getName());
		assertTrue(x.getUri().toString().contains("test.log"));
	}

	@Test void a03_setName() {
		// Test setName() returns correct type
		var x = new Action("view", "/logs/test.log");

		Action result = x.setName("download");

		// Verify fluent chaining
		assertSame(x, result);
		assertInstanceOf(Action.class, result);
		assertEquals("download", x.getName());
	}

	@Test void a04_setUri_String() {
		// Test setUri(String) returns correct type
		var x = new Action("view", "/logs/test.log");

		Action result = x.setUri("/logs/other.log");

		// Verify fluent chaining
		assertSame(x, result);
		assertInstanceOf(Action.class, result);
		assertTrue(x.getUri().toString().contains("/logs/other.log"));
	}

	@Test void a05_setUri_URI() {
		// Test setUri(java.net.URI) returns correct type
		var x = new Action("view", "/logs/test.log");

		try {
			Action result = x.setUri(new java.net.URI("http://example.com/logs/test.log"));

			// Verify fluent chaining
			assertSame(x, result);
			assertInstanceOf(Action.class, result);
			assertTrue(x.getUri().toString().contains("example.com"));
		} catch (Exception e) {
			fail("URI creation failed: " + e.getMessage());
		}
	}

	@Test void a06_setUri_withArgs() {
		// Test setUri(String, Object...) returns correct type
		var x = new Action("view", "/logs/test.log");

		Action result = x.setUri("/logs/{0}/{1}", "dir", "file.log");

		// Verify fluent chaining
		assertSame(x, result);
		assertInstanceOf(Action.class, result);
		assertTrue(x.getUri().toString().contains("dir"));
		assertTrue(x.getUri().toString().contains("file.log"));
	}

	@Test void a07_fluentChaining() {
		// Test fluent chaining works correctly
		var x = new Action("view", "/logs/test.log")
			.setName("download")
			.setUri("/logs/other.log");

		assertInstanceOf(Action.class, x);
		assertEquals("download", x.getName());
		assertTrue(x.getUri().toString().contains("/logs/other.log"));

		// Continue chaining
		x.setName("delete").setUri("/logs/{0}", "final.log");

		assertEquals("delete", x.getName());
		assertTrue(x.getUri().toString().contains("final.log"));
	}
}