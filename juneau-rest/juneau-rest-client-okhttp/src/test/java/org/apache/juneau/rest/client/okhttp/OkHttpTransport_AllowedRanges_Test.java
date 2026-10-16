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
package org.apache.juneau.rest.client.okhttp;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

import okhttp3.*;

/**
 * Verifies that {@link OkHttpTransport} builds its pin-on-connect client from the request's per-client allowed address
 * ranges: a request with ranges gets a client whose {@link PolicyPinningDns} carries them, and a request without ranges
 * keeps the shared default.  Reads the transport's private fields reflectively, because a hostname that resolves into a
 * special-purpose range cannot be produced without DNS.
 */
class OkHttpTransport_AllowedRanges_Test {

	private static final RemoteUrlPolicy.AllowedRanges LAB = RemoteUrlPolicy.AllowedRanges.of("224.0.0.0/4");

	private static Object field(Object o, String name) throws Exception {
		Field f = o.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(o);
	}

	private static void send(OkHttpTransport transport, RemoteUrlPolicy.AllowedRanges ranges) {
		var req = TransportRequest.builder().method("GET").uri("http://224.0.0.1:1/x").remoteUrlPolicy(true, false)
			.allowedAddressRanges(ranges).build();
		try {
			transport.execute(req).close();
		} catch (Exception e) {
			// Policy refusal or connect failure; only the client the transport selected matters here.
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<RemoteUrlPolicy.AllowedRanges,OkHttpClient> rangedClients(OkHttpTransport t) throws Exception {
		return (Map<RemoteUrlPolicy.AllowedRanges,OkHttpClient>)field(t, "rangedPolicyHttpClients");
	}

	@Test void a01_withoutRanges_usesSharedPolicyClient_noRangedClientCreated() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			send(transport, RemoteUrlPolicy.AllowedRanges.NONE);
			assertTrue(rangedClients(transport).isEmpty());
			var shared = (OkHttpClient)field(transport, "policyHttpClient");
			assertSame(PolicyPinningDns.INSTANCE, shared.dns());
		}
	}

	@Test void a02_withRanges_getsPinnedClientBuiltWithThoseRanges_ownPool() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			send(transport, LAB);
			send(transport, RemoteUrlPolicy.AllowedRanges.of("224.0.0.0/4"));
			var map = rangedClients(transport);
			assertEquals(1, map.size(), "one client per distinct ranges");
			var ranged = map.get(LAB);
			assertNotNull(ranged);
			var dns = assertInstanceOf(PolicyPinningDns.class, ranged.dns());
			assertEquals(LAB, field(dns, "allowedRanges"));
			var shared = (OkHttpClient)field(transport, "policyHttpClient");
			assertNotSame(shared.connectionPool(), ranged.connectionPool());
			assertSame(PolicyPinningDns.INSTANCE, shared.dns());
		}
	}
}
