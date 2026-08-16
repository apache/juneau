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
import java.util.List;
import org.junit.jupiter.api.Test;

class RunStateBroadcasterTest {

	@Test
	void publishDeliversToEverySubscriber() {
		var bc = new RunStateBroadcaster();
		var a = new ArrayList<String>();
		var b = new ArrayList<String>();
		bc.subscribe(a::add);
		bc.subscribe(b::add);

		bc.publish("{\"status\":\"RUNNING\"}");

		assertEquals(List.of("{\"status\":\"RUNNING\"}"), a);
		assertEquals(List.of("{\"status\":\"RUNNING\"}"), b);
	}

	@Test
	void unsubscribeStopsFurtherDelivery() throws Exception {
		var bc = new RunStateBroadcaster();
		var seen = new ArrayList<String>();
		var subscription = bc.subscribe(seen::add);

		bc.publish("one");
		subscription.close();
		bc.publish("two");

		assertEquals(List.of("one"), seen);
	}

	@Test
	void aThrowingSubscriberDoesNotBreakOthers() {
		var bc = new RunStateBroadcaster();
		var seen = new ArrayList<String>();
		bc.subscribe(s -> {
			throw new RuntimeException("dead client");
		});
		bc.subscribe(seen::add);

		bc.publish("snapshot");

		assertEquals(List.of("snapshot"), seen);
	}

	@Test
	void subscriberCountTracksSubscribeAndUnsubscribe() throws Exception {
		var bc = new RunStateBroadcaster();
		assertEquals(0, bc.subscriberCount());
		var s1 = bc.subscribe(x -> {
			// no-op sink
		});
		assertEquals(1, bc.subscriberCount());
		s1.close();
		assertEquals(0, bc.subscriberCount());
	}
}
