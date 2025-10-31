// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.resources.DirectoryResource.*;
import org.junit.jupiter.api.*;

/**
 * Tests for DirectoryResource.Action fluent setter overrides.
 */
class DirectoryResource_Action_Test extends TestBase {

	@Test void a01_fluentChaining_setName() {
		var action = new Action("view", "/view", "file.txt");

		// Test that setName() returns Action (not LinkString)
		Action result = action.setName("download");

		assertSame(action, result);
		assertInstanceOf(Action.class, result);
		assertEquals("download", result.getName());
	}

	@Test void a02_fluentChaining_setUri_URI() throws Exception {
		var action = new Action("view", "/view", "file.txt");

		// Test that setUri(URI) returns Action (not LinkString)
		var newUri = new URI("http://example.com/file");
		Action result = action.setUri(newUri);

		assertSame(action, result);
		assertInstanceOf(Action.class, result);
		assertEquals(newUri, result.getUri());
	}

	@Test void a03_fluentChaining_setUri_String() {
		var action = new Action("view", "/view", "file.txt");

		// Test that setUri(String) returns Action (not LinkString)
		Action result = action.setUri("/download");

		assertSame(action, result);
		assertInstanceOf(Action.class, result);
		assertEquals("/download", result.getUri().toString());
	}

	@Test void a04_fluentChaining_setUri_StringWithArgs() {
		var action = new Action("view", "/view", "file.txt");

		// Test that setUri(String, Object...) returns Action (not LinkString)
		Action result = action.setUri("/files/{0}/download", "myfile.txt");

		assertSame(action, result);
		assertInstanceOf(Action.class, result);
		assertTrue(result.getUri().toString().contains("myfile.txt"));
	}

	@Test void a05_fluentChaining_complex() {
		// Test chaining multiple fluent calls
		var result = new Action("view", "/view", "file.txt")
			.setName("download")
			.setUri("/download/{0}", "newfile.txt");

		assertInstanceOf(Action.class, result);
		assertEquals("download", result.getName());
		assertTrue(result.getUri().toString().contains("newfile.txt"));
	}

	@Test void a06_constructor() {
		// Test basic constructor functionality
		var action = new Action("view", "/files/{0}/view", "test.txt");

		assertEquals("view", action.getName());
		assertTrue(action.getUri().toString().contains("test.txt"));
	}
}