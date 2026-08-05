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
import java.util.concurrent.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.BasicMcpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.junit.jupiter.api.*;

class McpSubscriptionBrokerBinding_Test {

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected org.apache.juneau.rest.server.mcp.McpServerConfig createMcpConfig() {
			return new org.apache.juneau.rest.server.mcp.McpServerConfig();
		}
	}

	@Test void a01_servletDefault_createsBasicBrokerSizedFromSubscriptionsConfig() {
		var servlet = new A();
		assertInstanceOf(BasicMcpSubscriptionBroker.class, servlet.getSubscriptionBroker());
	}

	@Test void a02_servletBroker_isLazilyCachedAcrossCalls() {
		var servlet = new A();
		assertSame(servlet.getSubscriptionBroker(), servlet.getSubscriptionBroker());
	}

	@Test void a03_servletSubscriptionsConfig_isLazilyCachedAcrossCalls() {
		var servlet = new A();
		assertSame(servlet.getMcpOptions().getSubscriptions(), servlet.getMcpOptions().getSubscriptions());
	}

	@Test void a03b_getMcpOptions_isAnnotatedAsBean() throws Exception {
		var m = McpRestServlet.class.getMethod("getMcpOptions");
		assertTrue(m.isAnnotationPresent(org.apache.juneau.commons.inject.Bean.class),
			"getMcpOptions() must be @Bean-annotated so the RestContext bean store discovers it, "
			+ "the same mechanism already used by mcpTraceContextExtractor()");
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class D extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected org.apache.juneau.rest.server.mcp.McpServerConfig createMcpConfig() {
			return new org.apache.juneau.rest.server.mcp.McpServerConfig();
		}
		@Override protected McpOptions createMcpOptions() {
			return new McpOptions().setSubscriptionBroker(new BasicMcpSubscriptionBroker(42));
		}
	}

	@Test void a04_servletExplicitBroker_isReturnedAsIs() {
		var servlet = new D();
		var broker = servlet.getSubscriptionBroker();
		assertSame(servlet.getMcpOptions().getSubscriptionBroker(), broker);
	}

	/**
	 * Mirrors {@code McpBindings_Test#e02_servletOptions_concurrentFirstAccessPublishesExactlyOneInstance}, but
	 * exercises {@link McpOptions#resolveSubscriptionBroker()}'s own inner double-checked-locking directly: a
	 * plain lazy read would let two racing threads each publish a distinct derived broker, silently splitting
	 * live subscription state across two registries. Every concurrent first-access caller must observe the same
	 * derived instance.
	 */
	@Test void a05_derivedBroker_concurrentFirstAccessPublishesExactlyOneInstance() throws Exception {
		var options = new McpOptions();
		var threads = 16;
		var pool = Executors.newFixedThreadPool(threads);
		try {
			var start = new CountDownLatch(1);
			var results = new ArrayList<Future<McpSubscriptionBroker>>();
			for (var i = 0; i < threads; i++)
				results.add(pool.submit(() -> { start.await(); return options.resolveSubscriptionBroker(); }));
			start.countDown();
			var first = results.get(0).get();
			for (var f : results)
				assertSame(first, f.get(), "every concurrent first-access caller must observe the same derived broker");
		} finally {
			pool.shutdownNow();
		}
	}
}
