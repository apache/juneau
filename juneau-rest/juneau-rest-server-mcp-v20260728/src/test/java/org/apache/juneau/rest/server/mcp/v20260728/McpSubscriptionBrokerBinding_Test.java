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
import java.util.concurrent.atomic.*;

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
		assertSame(servlet.getSubscriptionsConfig(), servlet.getSubscriptionsConfig());
	}

	@Test void a03b_subscriptionsConfigBean_isAnnotatedAndDelegatesToGetSubscriptionsConfig() throws Exception {
		var servlet = new A();
		var m = McpRestServlet.class.getMethod("subscriptionsConfigBean");
		assertTrue(m.isAnnotationPresent(org.apache.juneau.commons.inject.Bean.class),
			"subscriptionsConfigBean() must be @Bean-annotated so the RestContext bean store discovers it, "
			+ "the same mechanism already used by mcpTraceContextExtractor()");
		assertSame(servlet.getSubscriptionsConfig(), servlet.subscriptionsConfigBean());
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected org.apache.juneau.rest.server.mcp.McpServerConfig createMcpConfig() {
			return new org.apache.juneau.rest.server.mcp.McpServerConfig();
		}
		@Override protected McpSubscriptionsConfig createSubscriptionsConfig() { return null; }
	}

	@Test void a04_servletNullSubscriptionsConfigFactoryFailsFast() {
		var e = assertThrows(IllegalStateException.class, () -> new B().getSubscriptionsConfig());
		assertEquals("createSubscriptionsConfig() returned null", e.getMessage());
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected org.apache.juneau.rest.server.mcp.McpServerConfig createMcpConfig() {
			return new org.apache.juneau.rest.server.mcp.McpServerConfig();
		}
		@Override protected McpSubscriptionBroker createSubscriptionBroker() { return null; }
	}

	@Test void a05_servletNullBrokerFactoryFailsFast() {
		var e = assertThrows(IllegalStateException.class, () -> new C().getSubscriptionBroker());
		assertEquals("createSubscriptionBroker() returned null", e.getMessage());
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class D extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		final AtomicInteger createCalls = new AtomicInteger();
		@Override protected org.apache.juneau.rest.server.mcp.McpServerConfig createMcpConfig() {
			return new org.apache.juneau.rest.server.mcp.McpServerConfig();
		}
		@Override protected McpSubscriptionBroker createSubscriptionBroker() {
			createCalls.incrementAndGet();
			return new BasicMcpSubscriptionBroker(getSubscriptionsConfig().getQueueSize());
		}
	}

	/**
	 * Mirrors {@code McpBindings_Test#e06_servletMrtrConfig_concurrentFirstAccessPublishesExactlyOneInstance}:
	 * a plain lazy read would let two racing threads each publish a distinct broker, silently splitting live
	 * subscription state across two registries. Every concurrent first-access caller must observe the same
	 * instance and {@code createSubscriptionBroker()} must run exactly once.
	 */
	@Test void a06_servletBroker_concurrentFirstAccessPublishesExactlyOneInstance() throws Exception {
		var servlet = new D();
		var threads = 16;
		var pool = Executors.newFixedThreadPool(threads);
		try {
			var start = new CountDownLatch(1);
			var results = new ArrayList<Future<McpSubscriptionBroker>>();
			for (var i = 0; i < threads; i++)
				results.add(pool.submit(() -> { start.await(); return servlet.getSubscriptionBroker(); }));
			start.countDown();
			var first = results.get(0).get();
			for (var f : results)
				assertSame(first, f.get(), "every concurrent first-access caller must observe the same published broker");
			assertEquals(1, servlet.createCalls.get(), "createSubscriptionBroker() must be invoked exactly once under the lock");
		} finally {
			pool.shutdownNow();
		}
	}
}
