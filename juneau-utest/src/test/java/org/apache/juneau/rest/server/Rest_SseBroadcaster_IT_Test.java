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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.sse.*;
import org.apache.juneau.sse.*;
import org.junit.jupiter.api.*;

class Rest_SseBroadcaster_IT_Test {

	@Rest(serializers = SseSerializer.class)
	public static class A {
		@RestGet("/a")
		public void a(RestResponse res, SseBroadcaster broadcaster) throws Exception {
			var subscription = broadcaster.subscribe("a");
			broadcaster.publish(new SseEvent("tick", "one"));
			broadcaster.publish(new SseEvent("tick", "two"));
			try (var sse = res.sse()) {
				sse.sendEvent(subscription.take());
				sse.sendEvent(subscription.take());
				sse.flush();
			} finally {
				subscription.close();
			}
		}
	}

	@Test
	void a01_broadcasterArgCanBeResolved() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		var b = a.get("/a").header("Accept", "text/event-stream").run().getContent().asString();
		assertTrue(b.contains("data: one"));
		assertTrue(b.contains("data: two"));
	}
}
