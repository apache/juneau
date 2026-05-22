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
package org.apache.juneau.rest.sse;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.sse.*;
import org.junit.jupiter.api.*;

class SseBroadcaster_Test {

	@Test
	void a01_publishFanoutToMultipleSubscribers() throws Exception {
		var a = SseBroadcaster.create();
		try (var b = a.subscribe("b"); var c = a.subscribe("c")) {
			a.publish(new SseEvent("e", "d1"));
			a.publish(new SseEvent("e", "d2"));

			assertEquals("d1", b.take().getData());
			assertEquals("d2", b.take().getData());
			assertEquals("d1", c.take().getData());
			assertEquals("d2", c.take().getData());
		}
	}

	@Test
	void a02_slowSubscriberDropsOldest() throws Exception {
		var a = new SseBroadcaster(1);
		try (var b = a.subscribe("b")) {
			a.publish(new SseEvent("e", "d1"));
			a.publish(new SseEvent("e", "d2"));

			assertEquals("d2", b.take().getData());
		}
	}

	@Test
	void a03_closeSubscriptionStopsDelivery() throws Exception {
		var a = SseBroadcaster.create();
		try (var b = a.subscribe("b")) {
			b.close();
			assertTrue(b.isClosed());
		}
	}
}
