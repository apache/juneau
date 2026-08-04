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

import java.util.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionFilter;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class McpSubscriptionBrokerMixin_Test {

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig(); }
	}

	@Test void a01_mixinDefault_returnsNonNullBroker() {
		assertNotNull(new A().subscriptionBroker());
	}

	@Test void a02_mixinDefault_returnsSharedInstanceAcrossSeparateEndpointInstances() {
		var broker1 = new A().subscriptionBroker();
		var broker2 = new A().subscriptionBroker();
		assertSame(broker1, broker2, "the mixin default broker must be a per-process shared instance");
	}

	@Test void a03_mixinDefault_subscriptionRegisteredOnOneInstanceVisibleOnAnother() {
		McpSubscriptionBroker broker1 = new A().subscriptionBroker();
		McpSubscriptionBroker broker2 = new A().subscriptionBroker();
		var before = broker2.activeCount();
		var sub = broker1.register("mixin-shared-test-s1", new McpSubscriptionFilter(true, true, true, Set.of()));
		assertEquals(before + 1, broker2.activeCount(), "a subscription registered via one mixin instance's "
			+ "broker must be visible via another instance's broker, since both share the JVM-wide default");
		sub.close();
	}

	@Test void a04_mixinDefault_subscriptionsConfigIsFreshPerCallAndEquivalent() {
		var a = new A();
		var config1 = a.subscriptionsConfig();
		var config2 = a.subscriptionsConfig();
		assertNotSame(config1, config2, "unlike the broker, the knobs-only config has no live state to "
			+ "share, so the mixin default (matching cacheConfig()'s precedent) returns a fresh instance");
		assertEquals(config1.getQueueSize(), config2.getQueueSize());
	}

	@Test void a05_subscriptionsConfigBean_isAnnotatedAndDelegatesToSubscriptionsConfig() throws Exception {
		var a = new A();
		var m = McpEndpoint.class.getMethod("subscriptionsConfigBean");
		assertTrue(m.isAnnotationPresent(org.apache.juneau.commons.inject.Bean.class),
			"subscriptionsConfigBean() must be @Bean-annotated so the RestContext bean store discovers it, "
			+ "the same mechanism already used by mcpTraceContextExtractor()");
		assertNotNull(a.subscriptionsConfigBean());
	}
}
