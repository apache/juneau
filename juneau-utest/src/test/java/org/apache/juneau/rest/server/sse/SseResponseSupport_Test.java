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
package org.apache.juneau.rest.server.sse;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

class SseResponseSupport_Test {

	@Rest(serializers = SseSerializer.class)
	public static class A {
		@RestGet("/stream")
		public void stream(RestResponse res) throws Exception {
			try (var sse = res.sse()) {
				sse.sendEvent("tick", "one");
				sse.comment("ping");
				sse.sendEvent(new SseEvent("tick", "two"));
				sse.flush();
			}
		}
	}

	@Test
	void a01_emitsEventsAndComments() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		var b = a.get("/stream").header("Accept", "text/event-stream").run();
		b.assertHeader("Content-Type").isContains("text/event-stream");
		var c = b.getContent().asString();
		assertTrue(c.contains("event: tick"));
		assertTrue(c.contains("data: one"));
		assertTrue(c.contains(": ping"));
		assertTrue(c.contains("data: two"));
	}
}
