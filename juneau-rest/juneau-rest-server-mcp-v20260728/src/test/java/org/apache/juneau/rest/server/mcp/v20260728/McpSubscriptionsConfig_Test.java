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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class McpSubscriptionsConfig_Test {

	@Test void a01_defaultsMatchDocumentedConstants() {
		var a = new McpSubscriptionsConfig();
		assertEquals(256, McpSubscriptionsConfig.DEFAULT_MAX_CONCURRENT_SUBSCRIPTIONS);
		assertEquals(1024, McpSubscriptionsConfig.DEFAULT_QUEUE_SIZE);
		assertEquals(15_000L, McpSubscriptionsConfig.DEFAULT_HEARTBEAT_INTERVAL_MS);
		assertEquals(0L, McpSubscriptionsConfig.DEFAULT_IDLE_TIMEOUT_MS);
		assertEquals(McpSubscriptionsConfig.DEFAULT_MAX_CONCURRENT_SUBSCRIPTIONS, a.getMaxConcurrentSubscriptions());
		assertEquals(McpSubscriptionsConfig.DEFAULT_QUEUE_SIZE, a.getQueueSize());
		assertEquals(McpSubscriptionsConfig.DEFAULT_HEARTBEAT_INTERVAL_MS, a.getHeartbeatIntervalMs());
		assertEquals(McpSubscriptionsConfig.DEFAULT_IDLE_TIMEOUT_MS, a.getIdleTimeoutMs());
	}

	@Test void a02_setMaxConcurrentSubscriptionsZeroThrows() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> new McpSubscriptionsConfig().setMaxConcurrentSubscriptions(0));
		assertEquals("maxConcurrentSubscriptions 0 must be > 0", e.getMessage());
	}

	@Test void a03_setQueueSizeNegativeThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpSubscriptionsConfig().setQueueSize(-1));
		assertEquals("queueSize -1 must be > 0", e.getMessage());
	}

	@Test void a04_setHeartbeatIntervalMsZeroThrows() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> new McpSubscriptionsConfig().setHeartbeatIntervalMs(0));
		assertEquals("heartbeatIntervalMs 0 must be > 0", e.getMessage());
	}

	@Test void a05_setIdleTimeoutMsNegativeThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpSubscriptionsConfig().setIdleTimeoutMs(-1));
		assertEquals("idleTimeoutMs -1 must be >= 0", e.getMessage());
	}

	@Test void a06_idleTimeoutMsZeroIsAllowed_meansDisabled() {
		assertDoesNotThrow(() -> new McpSubscriptionsConfig().setIdleTimeoutMs(0));
	}

	@Test void a07_validChainRoundTripsThroughGetters() {
		var a = new McpSubscriptionsConfig()
			.setMaxConcurrentSubscriptions(10).setQueueSize(64).setHeartbeatIntervalMs(5_000L).setIdleTimeoutMs(30_000L);
		assertEquals(10, a.getMaxConcurrentSubscriptions());
		assertEquals(64, a.getQueueSize());
		assertEquals(5_000L, a.getHeartbeatIntervalMs());
		assertEquals(30_000L, a.getIdleTimeoutMs());
	}
}
