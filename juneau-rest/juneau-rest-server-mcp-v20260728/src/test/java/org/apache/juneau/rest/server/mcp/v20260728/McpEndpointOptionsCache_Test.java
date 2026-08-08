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

import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage: {@link McpEndpointOptionsCache} must key by reference
 * <b>identity</b>, never by an endpoint's own {@code equals()}/{@code hashCode()}.
 */
class McpEndpointOptionsCache_Test {

	/**
	 * A resource "fixture" that overrides {@code equals}/{@code hashCode} the way a record, a Lombok
	 * {@code @Data} class, or any other value-ish endpoint type might: every instance of this class
	 * compares equal to every other instance of this class, and every instance hashes identically.
	 *
	 * <p>
	 * A {@code WeakHashMap}-backed cache (keyed by {@code equals()}/{@code hashCode()}) collapses distinct
	 * instances of this class onto ONE cache entry, so a second instance would silently observe the FIRST
	 * instance's memoized {@link McpOptions} (and thus its MRTR AES key and subscription broker) &mdash;
	 * exactly the JVM-wide sharing regression this cache's reference-identity keying eliminates.
	 */
	public static class EqualsOverridingEndpoint implements McpEndpoint {
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig(); }
		@Override public boolean equals(Object o) { return o instanceof EqualsOverridingEndpoint; }
		@Override public int hashCode() { return 42; }
	}

	@Test void a01_fixtureReallyDoesCollapseUnderEqualsHashCode() {
		var e1 = new EqualsOverridingEndpoint();
		var e2 = new EqualsOverridingEndpoint();
		assertEquals(e1, e2, "fixture sanity check: two distinct instances must compare equal");
		assertEquals(e1.hashCode(), e2.hashCode(), "fixture sanity check: two distinct instances must hash identically");
		assertNotSame(e1, e2, "fixture sanity check: they must still be distinct object instances");
	}

	/**
	 * The core regression: two distinct {@link EqualsOverridingEndpoint} instances &mdash; which compare
	 * {@code equals()} and hash identically &mdash; must still resolve to two distinct memoized
	 * {@link McpOptions}. This is the assertion that FAILS (two instances collapse onto one shared
	 * {@link McpOptions}) against a {@code WeakHashMap}-backed cache keyed by {@code equals()}/
	 * {@code hashCode()}, and PASSES against the identity-keyed {@link McpEndpointOptionsCache}.
	 */
	@Test void a02_distinctInstances_resolveToDistinctMcpOptions_despiteEqualsCollapsing() {
		var e1 = new EqualsOverridingEndpoint();
		var e2 = new EqualsOverridingEndpoint();
		var options1 = McpEndpointOptionsCache.resolve(e1);
		var options2 = McpEndpointOptionsCache.resolve(e2);
		assertNotSame(options1, options2,
			"two distinct endpoint instances must resolve to distinct McpOptions even though equals()/hashCode() "
			+ "collapse them, or the identity-key contract has regressed back to equals()-keying");
	}

	/** Same regression, surfaced through the mixin's own {@code subscriptionBroker()} seam. */
	@Test void a03_distinctInstances_resolveToDistinctBrokers_despiteEqualsCollapsing() {
		var e1 = new EqualsOverridingEndpoint();
		var e2 = new EqualsOverridingEndpoint();
		assertNotSame(e1.subscriptionBroker(), e2.subscriptionBroker(),
			"two distinct endpoint instances must NOT share a subscription broker even though equals()/hashCode() "
			+ "collapse them");
	}

	/** Same regression, surfaced through the mixin's own {@code revision()} seam (MRTR codec/AES key). */
	@Test void a04_distinctInstances_resolveToDistinctMrtrCodecs_despiteEqualsCollapsing() {
		var e1 = new EqualsOverridingEndpoint();
		var e2 = new EqualsOverridingEndpoint();
		var rev1 = (McpRevision)e1.revision();
		var rev2 = (McpRevision)e2.revision();
		assertNotSame(rev1.mrtrConfig(), rev2.mrtrConfig(),
			"two distinct endpoint instances must NOT share an MRTR config even though equals()/hashCode() collapse them");
		assertNotSame(rev1.mrtrConfig().getCodec(), rev2.mrtrConfig().getCodec(),
			"two distinct endpoint instances must NOT share an MRTR AES key even though equals()/hashCode() collapse them");
	}

	@Test void a05_resolve_isStillMemoizedPerInstance() {
		var e1 = new EqualsOverridingEndpoint();
		assertSame(McpEndpointOptionsCache.resolve(e1), McpEndpointOptionsCache.resolve(e1),
			"repeated resolution of the SAME instance must still return the identical memoized McpOptions");
	}
}
