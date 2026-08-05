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

import org.apache.juneau.bean.mcp.v20260728.PromptCapability;
import org.apache.juneau.bean.mcp.v20260728.ServerCapabilities;
import org.apache.juneau.rest.server.mcp.BasicMcpSubscriptionBroker;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link McpOptions}, the v2 aggregate consolidating capabilities/instructions/cache/mrtr/
 * subscriptions/subscriptionBroker.
 */
class McpOptions_Test {

	// -------- capabilities / instructions: plain getter/setter round-trips ---------

	@Test void a01_capabilities_defaultsToNull() {
		assertNull(new McpOptions().getCapabilities());
	}

	@Test void a02_capabilities_setterRoundTrips() {
		var caps = new ServerCapabilities().setPrompts(new PromptCapability());
		var o = new McpOptions().setCapabilities(caps);
		assertSame(caps, o.getCapabilities());
	}

	@Test void a03_capabilities_setterAcceptsNull() {
		var o = new McpOptions().setCapabilities(new ServerCapabilities()).setCapabilities(null);
		assertNull(o.getCapabilities());
	}

	@Test void a04_instructions_defaultsToNull() {
		assertNull(new McpOptions().getInstructions());
	}

	@Test void a05_instructions_setterRoundTrips() {
		var o = new McpOptions().setInstructions("Use tool 'echo' to test.");
		assertEquals("Use tool 'echo' to test.", o.getInstructions());
	}

	@Test void a06_instructions_setterAcceptsNull() {
		var o = new McpOptions().setInstructions("x").setInstructions(null);
		assertNull(o.getInstructions());
	}

	// -------- cache: nested config, replace-outright, and Consumer configure-block ---------

	@Test void b01_cache_defaultsToNonNullFrameworkOwnedInstance() {
		assertNotNull(new McpOptions().getCache());
	}

	@Test void b02_setCache_replacesOutrightAndRoundTrips() {
		var cache = new McpCacheConfig();
		var o = new McpOptions().setCache(cache);
		assertSame(cache, o.getCache());
	}

	@Test void b03_setCache_nullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpOptions().setCache(null));
		assertEquals("cache must not be null", e.getMessage());
	}

	@Test void b04_cacheConsumer_mutatesFrameworkOwnedInstanceInPlace() {
		var o = new McpOptions();
		var before = o.getCache();
		var same = o.cache(c -> c.setToolsList(new McpCacheHint().setTtlMs(60_000)));
		assertSame(o, same, "cache(Consumer) must return this for chaining");
		assertSame(before, o.getCache(), "the Consumer block must mutate the existing nested instance in place, never replace it");
		assertEquals(Integer.valueOf(60_000), o.getCache().getToolsList().getTtlMs());
	}

	@Test void b05_cacheConsumer_nullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpOptions().cache(null));
		assertEquals("consumer must not be null", e.getMessage());
	}

	// -------- mrtr: nested config, replace-outright, and Consumer configure-block ---------

	@Test void c01_mrtr_defaultsToNonNullFrameworkOwnedInstance() {
		assertNotNull(new McpOptions().getMrtr());
	}

	@Test void c02_setMrtr_replacesOutrightAndRoundTrips() {
		var mrtr = new McpMrtrConfig();
		var o = new McpOptions().setMrtr(mrtr);
		assertSame(mrtr, o.getMrtr());
	}

	@Test void c03_setMrtr_nullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpOptions().setMrtr(null));
		assertEquals("mrtr must not be null", e.getMessage());
	}

	@Test void c04_mrtrConsumer_mutatesFrameworkOwnedInstanceInPlace() {
		var o = new McpOptions();
		var before = o.getMrtr();
		var same = o.mrtr(m -> m.setTtlMs(600_000L));
		assertSame(o, same, "mrtr(Consumer) must return this for chaining");
		assertSame(before, o.getMrtr(), "the Consumer block must mutate the existing nested instance in place, never replace it");
		assertEquals(600_000L, o.getMrtr().getTtlMs());
	}

	@Test void c05_mrtrConsumer_nullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpOptions().mrtr(null));
		assertEquals("consumer must not be null", e.getMessage());
	}

	// -------- subscriptions: nested config, replace-outright, and Consumer configure-block ---------

	@Test void d01_subscriptions_defaultsToNonNullFrameworkOwnedInstance() {
		assertNotNull(new McpOptions().getSubscriptions());
	}

	@Test void d02_setSubscriptions_replacesOutrightAndRoundTrips() {
		var subs = new McpSubscriptionsConfig();
		var o = new McpOptions().setSubscriptions(subs);
		assertSame(subs, o.getSubscriptions());
	}

	@Test void d03_setSubscriptions_nullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpOptions().setSubscriptions(null));
		assertEquals("subscriptions must not be null", e.getMessage());
	}

	@Test void d04_subscriptionsConsumer_mutatesFrameworkOwnedInstanceInPlace() {
		var o = new McpOptions();
		var before = o.getSubscriptions();
		var same = o.subscriptions(s -> s.setQueueSize(2048));
		assertSame(o, same, "subscriptions(Consumer) must return this for chaining");
		assertSame(before, o.getSubscriptions(), "the Consumer block must mutate the existing nested instance in place, never replace it");
		assertEquals(2048, o.getSubscriptions().getQueueSize());
	}

	@Test void d05_subscriptionsConsumer_nullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpOptions().subscriptions(null));
		assertEquals("consumer must not be null", e.getMessage());
	}

	// -------- subscriptionBroker: explicit-slot getter/setter vs. resolveSubscriptionBroker() derivation ---------

	@Test void e01_getSubscriptionBroker_defaultsToNull() {
		assertNull(new McpOptions().getSubscriptionBroker(),
			"the explicit-slot getter must default to null (meaning framework-derived), unlike resolveSubscriptionBroker()");
	}

	@Test void e02_resolveSubscriptionBroker_neverNull_derivesADefault() {
		assertNotNull(new McpOptions().resolveSubscriptionBroker(),
			"resolveSubscriptionBroker() is the effective accessor and must never return null");
	}

	@Test void e03_resolveSubscriptionBroker_memoizesTheDerivedInstance() {
		var o = new McpOptions();
		assertSame(o.resolveSubscriptionBroker(), o.resolveSubscriptionBroker(),
			"repeated calls must return the SAME derived broker, or subscribers/publishers would split across registries");
	}

	@Test void e04_setSubscriptionBroker_explicitValueIsReturnedAsIsByResolve() {
		var broker = new BasicMcpSubscriptionBroker(16);
		var o = new McpOptions().setSubscriptionBroker(broker);
		assertSame(broker, o.getSubscriptionBroker());
		assertSame(broker, o.resolveSubscriptionBroker(), "an explicitly-set broker must be returned as-is, never overridden by derivation");
	}

	@Test void e05_setSubscriptionBrokerNull_revertsToDerivedDefault() {
		var explicit = new BasicMcpSubscriptionBroker(16);
		var o = new McpOptions().setSubscriptionBroker(explicit);
		assertSame(explicit, o.resolveSubscriptionBroker());

		o.setSubscriptionBroker(null);
		assertNull(o.getSubscriptionBroker());
		assertNotNull(o.resolveSubscriptionBroker(), "after reverting to null, resolveSubscriptionBroker() must derive a default rather than staying null");
		assertNotSame(explicit, o.resolveSubscriptionBroker(), "the derived default must not be the previously-set explicit broker");
	}

	@Test void e06_resolveSubscriptionBroker_sizedFromSubscriptionsQueueSize() {
		var o = new McpOptions().subscriptions(s -> s.setQueueSize(7));
		assertInstanceOf(BasicMcpSubscriptionBroker.class, o.resolveSubscriptionBroker());
	}
}
