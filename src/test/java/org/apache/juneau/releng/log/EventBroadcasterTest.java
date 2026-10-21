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

package org.apache.juneau.releng.log;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventBroadcasterTest {

	@Test
	void a01_publishesTypedJsonToEverySubscriberUntilClosed() throws Exception {
		var bc = new EventBroadcaster();
		var got = new ArrayList<String>();
		var sub = bc.subscribe(got::add);
		bc.publish("run", Map.of("version", "9.2.1"));
		sub.close();
		bc.publish("run", Map.of("version", "9.2.2"));
		assertEquals(1, got.size());
		assertEquals("{\"type\":\"run\",\"version\":\"9.2.1\"}", got.get(0).replace(" ", ""));
	}

	@Test
	void a03_throwingSinkDoesNotStopOthers() {
		var bc = new EventBroadcaster();
		var got = new ArrayList<String>();
		bc.subscribe(s -> { throw new IllegalStateException("bad sink"); });
		bc.subscribe(got::add);
		bc.publish("run", Map.of("version", "9.2.1"));
		assertEquals(1, got.size());
	}

	@Test
	void a02_payloadTypeFieldNeverOverridesTheEventType() {
		var bc = new EventBroadcaster();
		var got = new ArrayList<String>();
		bc.subscribe(got::add);
		bc.publish("prompt", Map.of("type", "prompt", "id", "p1"));
		assertTrue(got.get(0).startsWith("{\"type\":\"prompt\""), got.get(0));
	}
}
