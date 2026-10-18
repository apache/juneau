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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.junit.jupiter.api.*;

/**
 * Tests the per-client {@link RestClient.Builder#allowAddressRanges(String...)} option: the same narrow semantics and
 * validation as the {@code RestClient.allowAddressRanges} system property, threaded to the literal-host check, the
 * redirect revalidation and the JDK transport's pinned connect.
 */
@SuppressWarnings({
	"resource" // Transports and clients are short-lived fixtures closed by try-with-resources; no request reaches the network except where noted.
})
class RestClient_AllowAddressRanges_Test extends TestBase {

	@Remote
	interface Api {
		@RemoteGet
		String get(@Url String url);
	}

	/** A policy-capable transport that records the requests it is asked to send. */
	private static final class Recording implements HttpTransport {
		final List<TransportRequest> requests = new ArrayList<>();

		@Override
		public TransportResponse execute(TransportRequest request) {
			requests.add(request);
			return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream(new byte[0])).build();
		}

		@Override
		public boolean supportsUrlPolicy() {
			return true;
		}
	}



	//------------------------------------------------------------------------------------------------------------------
	// a - builder validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_invalidEntry_failsAtBuild() {
		for (var bad : new String[]{"10.0.0.0/8", "127.0.0.1", "169.254.169.254", "0.0.0.0/0", "::/0", "example.com", "198.18.0.0/14"}) {
			var builder = RestClient.builder().transport(new Recording()).allowAddressRanges(bad);
			var ex = assertThrows(IllegalArgumentException.class, builder::build, bad);
			assertTrue(ex.getMessage().contains("allowed address range"), ex.getMessage());
		}
	}

	@Test void a02_validEntries_exposedOnClient() throws Exception {
		try (var client = RestClient.builder().transport(new Recording()).allowAddressRanges("198.18.0.0/15", "192.0.0.8").build()) {
			assertEquals(RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15,192.0.0.8"), client.getAllowedAddressRanges());
		}
		try (var client = RestClient.builder().transport(new Recording()).build()) {
			assertTrue(client.getAllowedAddressRanges().isEmpty());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - literal-host check (RemoteClient) and the TransportRequest handed to the transport
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_withOption_specialPurposeLiteral_reachesTransport_carryingRanges() throws Exception {
		var transport = new Recording();
		try (var client = RestClient.builder().transport(transport).allowAddressRanges("198.18.0.0/15").build()) {
			client.remote(Api.class).get("http://198.18.0.5/x");
			assertEquals(1, transport.requests.size());
			assertEquals(RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15"), transport.requests.get(0).getAllowedAddressRanges());
		}
	}

	@Test void b02_withoutOption_sameClassOfClient_stillDenied_inSameJvm() throws Exception {
		var withOption = new Recording();
		var without = new Recording();
		try (var a = RestClient.builder().transport(withOption).allowAddressRanges("198.18.0.0/15").build();
			var b = RestClient.builder().transport(without).build()) {
			a.remote(Api.class).get("http://198.18.0.5/x");
			var api = b.remote(Api.class);
			var ex = assertThrows(IllegalArgumentException.class, () -> api.get("http://198.18.0.5/x"));
			assertTrue(ex.getMessage().contains("denied by default"), ex.getMessage());
			assertTrue(without.requests.isEmpty());
			assertEquals(1, withOption.requests.size());
		}
	}

	@Test void b03_option_neverReopensLoopbackOrMetadataOrOtherRanges() throws Exception {
		var transport = new Recording();
		try (var client = RestClient.builder().transport(transport).allowAddressRanges("198.18.0.0/15").build()) {
			var api = client.remote(Api.class);
			for (var url : new String[]{"http://127.0.0.1/x", "http://169.254.169.254/latest", "http://10.0.0.1/x", "http://192.0.0.1/x", "http://224.0.0.1/x", "http://localhost/x"}) {
				var ex = assertThrows(IllegalArgumentException.class, () -> api.get(url), url);
				assertTrue(ex.getMessage().contains("denied by default"), ex.getMessage());
			}
			assertTrue(transport.requests.isEmpty());
		}
	}

	@Test void b04_unionWithSystemProperty() throws Exception {
		var previous = System.getProperty(RemoteUrlPolicy.ALLOW_ADDRESS_RANGES_PROPERTY);
		System.setProperty(RemoteUrlPolicy.ALLOW_ADDRESS_RANGES_PROPERTY, "192.0.0.0/24");
		try {
			var transport = new Recording();
			try (var client = RestClient.builder().transport(transport).allowAddressRanges("198.18.0.0/15").build()) {
				client.remote(Api.class).get("http://192.0.0.5/x");
				client.remote(Api.class).get("http://198.18.0.5/x");
				assertEquals(2, transport.requests.size());
				var api = client.remote(Api.class);
				assertThrows(IllegalArgumentException.class, () -> api.get("http://224.0.0.5/x"));
			}
		} finally {
			if (previous == null)
				System.clearProperty(RemoteUrlPolicy.ALLOW_ADDRESS_RANGES_PROPERTY);
			else
				System.setProperty(RemoteUrlPolicy.ALLOW_ADDRESS_RANGES_PROPERTY, previous);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - redirect revalidation (PolicyEnforcedRedirects)
	//------------------------------------------------------------------------------------------------------------------

	private static final class Hops implements PolicyEnforcedRedirects.HopExecutor {
		final List<TransportRequest> hops = new ArrayList<>();
		final Deque<String> locations;

		Hops(String...locations) {
			this.locations = new ArrayDeque<>(List.of(locations));
		}

		@Override
		public TransportResponse execute(TransportRequest hop) {
			hops.add(hop);
			var location = locations.poll();
			if (location == null)
				return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream(new byte[0])).build();
			return TransportResponse.builder().statusCode(302).header("Location", location).build();
		}
	}

	private static TransportRequest guarded(String uri, RemoteUrlPolicy.AllowedRanges ranges) {
		return TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false).allowedAddressRanges(ranges).build();
	}

	@Test void c01_redirectToAllowedSpecialPurpose_followed_andRangesCarriedAcrossHops() throws Exception {
		var ranges = RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15");
		var hops = new Hops("http://198.18.0.9/a", "http://198.18.0.10/b");
		PolicyEnforcedRedirects.execute(guarded("http://example.com/start", ranges), hops);
		assertEquals(3, hops.hops.size());
		for (var h : hops.hops)
			assertEquals(ranges, h.getAllowedAddressRanges());
	}

	@Test void c02_redirectToSpecialPurpose_withoutRanges_refused() {
		var hops = new Hops("http://198.18.0.9/a");
		var ex = assertThrows(TransportException.class, () -> PolicyEnforcedRedirects.execute(guarded("http://example.com/start", RemoteUrlPolicy.AllowedRanges.NONE), hops));
		assertTrue(ex.getMessage().contains("denied by default"), ex.getMessage());
	}

	@Test void c03_redirectToLoopbackOrMetadata_refused_evenWithRanges() {
		var ranges = RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15");
		for (var target : new String[]{"http://127.0.0.1/a", "http://169.254.169.254/a"}) {
			var hops = new Hops(target);
			var ex = assertThrows(TransportException.class, () -> PolicyEnforcedRedirects.execute(guarded("http://example.com/start", ranges), hops), target);
			assertTrue(ex.getMessage().contains("denied by default"), ex.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// d - JDK transport: literal-host pinned connect
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_javaHttpTransport_withRanges_notRefusedByPolicy_whileWithoutIsRefused() {
		var transport = JavaHttpTransport.create();
		var without = assertThrows(TransportException.class, () -> transport.execute(guarded("http://198.18.0.1:1/x", RemoteUrlPolicy.AllowedRanges.NONE)).close());
		assertInstanceOf(IllegalArgumentException.class, without.getCause());
		assertTrue(without.getMessage().contains("No allowed address for host '198.18.0.1'"), without.getMessage());

		var request = TransportRequest.builder().method("GET").uri("http://198.18.0.1:1/x").remoteUrlPolicy(true, false)
			.allowedAddressRanges(RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15")).timeout(java.time.Duration.ofSeconds(2)).build();
		try {
			transport.execute(request).close();
		} catch (TransportException e) {
			// The connect itself may fail (nothing listens at 198.18.0.1); it must not be the policy refusal.
			assertFalse(e.getMessage().contains("No allowed address"), e.getMessage());
		} catch (IOException e) {
			// Closing the response is irrelevant here.
		}
	}

	@Test void d02_javaHttpTransport_withRanges_neverReopensLoopback() {
		var transport = JavaHttpTransport.create();
		for (var host : new String[]{"127.0.0.1", "169.254.169.254"}) {
			var request = guarded("http://" + host + ":1/x", RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15"));
			var ex = assertThrows(TransportException.class, () -> transport.execute(request).close(), host);
			assertTrue(ex.getMessage().contains("No allowed address for host '" + host + "'"), ex.getMessage());
		}
	}
}
