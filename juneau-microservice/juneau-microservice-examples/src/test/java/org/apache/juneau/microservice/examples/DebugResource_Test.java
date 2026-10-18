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
package org.apache.juneau.microservice.examples;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link DebugResource} that don't need a running Jetty server.
 */
class DebugResource_Test extends TestBase {

	@SuppressWarnings("resource") // MockRestClient and its request/response are in-memory test doubles that hold no OS resource.
	@Test void a01_threadDumpNavlink_plainGet() throws Exception {
		var body = MockRestClient.create(DebugResource.class).ignoreErrors().build()
			.get("/").accept("text/html").run()
			.assertStatus(200)
			.getContent().asString();
		assertTrue(body.contains("/jetty/dump\""), body);
		assertFalse(body.contains("method="), body);
	}
}
