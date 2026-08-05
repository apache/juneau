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
import org.apache.juneau.rest.mock.classic.MockRestClient;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionFilter;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class McpSubscriptionBrokerMixin_Test {

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig(); }
	}

	@Test void a01_mixinDefault_returnsNonNullBroker() {
		assertNotNull(new A().subscriptionBroker());
	}

	@Test void a02_mixinDefault_isStableAcrossCallsOnSameInstance() {
		var a = new A();
		assertSame(a.subscriptionBroker(), a.subscriptionBroker(), "the mixin default broker must be memoized per-binding");
	}

	/**
	 * TODO-330 regression: the pre-consolidation mixin default accidentally shared its broker JVM-wide
	 * ({@code SharedSubscriptionBroker}) across every distinct endpoint instance. Post-consolidation, the
	 * broker is derived per-binding (memoized via {@link McpEndpointOptionsCache}): two distinct endpoint
	 * instances must resolve to two distinct brokers, proving the accidental JVM-wide sharing is gone.
	 */
	@Test void a03_mixinDefault_distinctAcrossDistinctEndpointInstances() {
		var broker1 = new A().subscriptionBroker();
		var broker2 = new A().subscriptionBroker();
		assertNotSame(broker1, broker2, "two distinct endpoint instances must NOT share the same broker (no JVM-wide sharing)");
	}

	@Test void a04_mixinDefault_subscriptionRegisteredOnOneInstanceNotVisibleOnAnother() {
		McpSubscriptionBroker broker1 = new A().subscriptionBroker();
		McpSubscriptionBroker broker2 = new A().subscriptionBroker();
		var before = broker2.activeCount();
		var sub = broker1.register("mixin-isolated-test-s1", new McpSubscriptionFilter(true, true, true, Set.of()));
		assertEquals(before, broker2.activeCount(), "a subscription registered via one mixin instance's "
			+ "broker must NOT be visible via another instance's broker, since each binding now derives its own");
		sub.close();
	}

	@Test void a05_mixinDefault_subscriptionsConfigIsStableAcrossCallsOnSameInstance() {
		var a = new A();
		var config1 = a.mcpOptionsBean().getSubscriptions();
		var config2 = a.mcpOptionsBean().getSubscriptions();
		assertSame(config1, config2, "McpOptions (and its nested subscriptions config) is memoized per-binding via "
			+ "the mcpOptionsBean()/McpEndpointOptionsCache seam, so repeated access on the same endpoint instance "
			+ "returns the identical instance, even though the raw getMcpOptions() override point may return a "
			+ "fresh instance per call");
	}

	@Test void a06_mcpOptionsBean_isAnnotatedAndMemoizedPerInstance() throws Exception {
		var a = new A();
		var m = McpEndpoint.class.getMethod("mcpOptionsBean");
		assertTrue(m.isAnnotationPresent(org.apache.juneau.commons.inject.Bean.class),
			"mcpOptionsBean() must be @Bean-annotated so the RestContext bean store discovers it, "
			+ "the same mechanism already used by mcpTraceContextExtractor()");
		assertSame(a.mcpOptionsBean(), a.mcpOptionsBean(),
			"mcpOptionsBean() must return the SAME memoized McpOptions on every call for a given endpoint instance");
		assertNotSame(a.mcpOptionsBean(), new A().mcpOptionsBean(),
			"a distinct endpoint instance must resolve to a distinct memoized McpOptions");
	}

	/**
	 * IMPORTANT-5 code-review regression: {@code a06} above only checks the {@code @Bean} annotation
	 * reflectively, which would NOT catch a future break in interface-default {@code @Bean} discovery (for
	 * example, if the {@code RestContext} bean-collection walk stopped considering default methods on mixin
	 * interfaces). This test proves the wiring end-to-end through a real {@code RestContext}: the resource's
	 * bean store must resolve {@link McpOptions} to the exact SAME instance {@link McpEndpoint#mcpOptionsBean()}
	 * returns, so {@code subscriptions/listen} dispatch (which reads {@code McpOptions} from the bean store,
	 * not by calling {@code mcpOptionsBean()} directly) never silently falls back to defaults.
	 */
	@Test void a07_mcpOptionsBean_isPublishedInRestContextBeanStore_sameInstanceAsMemoized() {
		var a = new A();
		MockRestClient.create(a).json().contentType("application/json").accept("application/json").build();
		var fromBeanStore = a.getContext().getBeanStore().getBean(McpOptions.class)
			.orElseThrow(() -> new AssertionError("McpOptions bean not found in RestContext's bean store"));
		assertSame(a.mcpOptionsBean(), fromBeanStore,
			"the RestContext bean store's published McpOptions must be the SAME instance as the mixin's memoized "
			+ "mcpOptionsBean(), proving the @Bean interface-default discovery wires the memoized instance end-to-end");
	}
}
