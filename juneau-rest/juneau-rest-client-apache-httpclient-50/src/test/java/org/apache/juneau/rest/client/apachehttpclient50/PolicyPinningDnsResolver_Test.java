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
package org.apache.juneau.rest.client.apachehttpclient50;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link PolicyPinningDnsResolver}, the resolver of {@link ApacheHc5Transport}'s pinned connection pool.
 */
class PolicyPinningDnsResolver_Test {

	@Test
	void a01_resolve_publicAddress_pinnedToThatAddress() throws Exception {
		var pinned = PolicyPinningDnsResolver.INSTANCE.resolve("192.0.2.1");
		assertEquals(1, pinned.length);
		assertEquals("192.0.2.1", pinned[0].getHostAddress());
	}

	@Test
	void a02_resolve_loopback_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PolicyPinningDnsResolver.INSTANCE.resolve("127.0.0.1"));
		assertTrue(ex.getMessage().contains("No allowed address for host '127.0.0.1'"), ex.getMessage());
	}

	@Test
	void a03_resolveCanonicalHostname_delegatesToSystemResolver() throws Exception {
		assertNotNull(PolicyPinningDnsResolver.INSTANCE.resolveCanonicalHostname("127.0.0.1"));
	}
}
