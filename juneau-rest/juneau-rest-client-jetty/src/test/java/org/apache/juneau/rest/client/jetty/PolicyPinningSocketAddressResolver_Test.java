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
package org.apache.juneau.rest.client.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.eclipse.jetty.client.*;
import org.eclipse.jetty.util.*;
import org.junit.jupiter.api.*;

/**
 * Hermetic unit tests for {@link PolicyPinningSocketAddressResolver}: no socket is opened and no DNS lookup is made
 * (only IP literals are resolved).  The destination in the resolve context is a stand-in carrying only its origin.
 */
class PolicyPinningSocketAddressResolver_Test {

	/** Captures how the resolver completed its promise. */
	private static final class Capture implements Promise<List<InetSocketAddress>> {
		final AtomicReference<List<InetSocketAddress>> result = new AtomicReference<>();
		final AtomicReference<Throwable> failure = new AtomicReference<>();

		@Override
		public void succeeded(List<InetSocketAddress> value) {
			result.set(value);
		}

		@Override
		public void failed(Throwable x) {
			failure.set(x);
		}
	}

	// A resolve context as Jetty builds it when opening a connection for a destination with the given tag.
	private static Map<String, Object> contextFor(Object tag) {
		var origin = new Origin("http", "example.invalid", 80, tag);
		var destination = (Destination)java.lang.reflect.Proxy.newProxyInstance(Destination.class.getClassLoader(), new Class<?>[] { Destination.class }, (proxy, method, args) -> {
			if (method.getName().equals("getOrigin"))
				return origin;
			throw new UnsupportedOperationException(method.getName());
		});
		var context = new HashMap<String, Object>();
		context.put(Destination.CONTEXT_KEY, destination);
		return context;
	}

	@Test
	void a01_noDestinationInContext_delegates() {
		var delegated = new AtomicReference<String>();
		var resolver = new PolicyPinningSocketAddressResolver((host, port, context, promise) -> {
			delegated.set(host + ":" + port);
			promise.succeeded(List.of());
		});
		var c = new Capture();
		resolver.resolve("example.invalid", 8080, new HashMap<>(), c);
		assertEquals("example.invalid:8080", delegated.get());
		assertNotNull(c.result.get());
	}

	@Test
	void a02_pinnedDestination_publicLiteral_pinnedToThatAddress() {
		var resolver = new PolicyPinningSocketAddressResolver((host, port, context, promise) -> fail("must not delegate for a pinned destination"));
		var c = new Capture();
		resolver.resolve("8.8.8.8", 8443, contextFor(PolicyPinningSocketAddressResolver.PINNED_TAG), c);
		assertNull(c.failure.get());
		var addrs = c.result.get();
		assertEquals(1, addrs.size());
		assertEquals("8.8.8.8", addrs.get(0).getAddress().getHostAddress());
		assertEquals(8443, addrs.get(0).getPort());
	}

	@Test
	void a03_pinnedDestination_loopbackLiteral_failed() {
		var resolver = new PolicyPinningSocketAddressResolver((host, port, context, promise) -> fail("must not delegate for a pinned destination"));
		var c = new Capture();
		resolver.resolve("127.0.0.1", 80, contextFor(PolicyPinningSocketAddressResolver.PINNED_TAG), c);
		assertNull(c.result.get());
		assertInstanceOf(IllegalArgumentException.class, c.failure.get());
		assertTrue(c.failure.get().getMessage().contains("No allowed address for host '127.0.0.1'"), c.failure.get().getMessage());
	}

	@Test
	void a04_unpinnedDestination_delegates() {
		for (var tag : new Object[] { null, "other-tag" }) {
			var delegated = new AtomicReference<String>();
			var resolver = new PolicyPinningSocketAddressResolver((host, port, context, promise) -> {
				delegated.set(host + ":" + port);
				promise.succeeded(List.of());
			});
			var c = new Capture();
			resolver.resolve("127.0.0.1", 80, contextFor(tag), c);
			assertEquals("127.0.0.1:80", delegated.get(), String.valueOf(tag));
			assertNull(c.failure.get());
		}
	}

	@Test
	void b01_pinTagWithRanges_specialPurposeLiteral_pinned_andOtherDeniesStayDenied() {
		var resolver = new PolicyPinningSocketAddressResolver((host, port, context, promise) -> fail("must not delegate for a pinned destination"));
		var tag = PolicyPinningSocketAddressResolver.tagFor(org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15"));
		var ok = new Capture();
		resolver.resolve("198.18.0.1", 80, contextFor(tag), ok);
		assertNull(ok.failure.get());
		assertEquals("198.18.0.1", ok.result.get().get(0).getAddress().getHostAddress());
		var none = new Capture();
		resolver.resolve("198.18.0.1", 80, contextFor(PolicyPinningSocketAddressResolver.PINNED_TAG), none);
		assertInstanceOf(IllegalArgumentException.class, none.failure.get());
		for (var denied : new String[]{"127.0.0.1", "169.254.169.254"}) {
			var c = new Capture();
			resolver.resolve(denied, 80, contextFor(tag), c);
			assertInstanceOf(IllegalArgumentException.class, c.failure.get(), denied);
		}
	}

	@Test
	void b02_tagFor_isValueEqualPerRanges() {
		var a = PolicyPinningSocketAddressResolver.tagFor(org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15"));
		var b = PolicyPinningSocketAddressResolver.tagFor(org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15"));
		assertEquals(a, b);
		assertNotEquals(a, PolicyPinningSocketAddressResolver.PINNED_TAG);
		assertSame(PolicyPinningSocketAddressResolver.PINNED_TAG, PolicyPinningSocketAddressResolver.tagFor(org.apache.juneau.http.remote.RemoteUrlPolicy.AllowedRanges.NONE));
	}
}
