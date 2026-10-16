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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestClient.Builder#allowAddressRanges(String...)} on the classic {@link RestClient}.
 *
 * <p>
 * The reaching target is {@code 224.0.0.1}: a multicast address cannot be connected to over TCP, so the call fails
 * immediately and deterministically at connect, after the policy (literal-host check and the DNS-pinned resolver)
 * has allowed it; the assertions are that the failure is not the policy refusal.
 */
@Timeout(60)
class RestClient_AllowAddressRanges_Test {

	private static final String LAB = "http://224.0.0.1:1/x";

	@Remote
	public interface UrlRemote {
		@RemoteOp(method = "GET")
		void call(@Url String url);
	}

	private static String messages(Throwable t) {
		var sb = new StringBuilder();
		for (var x = t; x != null; x = x.getCause()) {
			if (x instanceof UndeclaredThrowableException u && u.getUndeclaredThrowable() != x.getCause())
				sb.append(messages(u.getUndeclaredThrowable()));
			sb.append(x.getClass().getName()).append(": ").append(x.getMessage()).append('\n');
		}
		return sb.toString();
	}

	private static boolean refused(String msg) {
		return msg.contains("denied by default") || msg.contains("No allowed address");
	}

	private static String callMessages(RestClient c, String url) {
		try {
			c.getRemote(UrlRemote.class).call(url);
			return "";
		} catch (Exception e) {
			return messages(e);
		}
	}

	// =================================================================================================================
	// A: builder validation and getter
	// =================================================================================================================

	@Test void a01_invalidSpec_failsAtBuild() {
		assertThrows(IllegalArgumentException.class, () -> RestClient.create().allowAddressRanges("10.0.0.0/8").build());
		assertThrows(IllegalArgumentException.class, () -> RestClient.create().allowAddressRanges("not-an-address").build());
		assertThrows(IllegalArgumentException.class, () -> RestClient.create().allowAddressRanges("127.0.0.1").build());
	}

	@Test void a02_getter_reflectsSettingAndAccumulates() throws Exception {
		try (var c = RestClient.create().allowAddressRanges("198.18.0.0/15").allowAddressRanges("224.0.0.0/4").build()) {
			assertEquals(RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15", "224.0.0.0/4"), c.getAllowedAddressRanges());
		}
		try (var c = RestClient.create().build()) {
			assertTrue(c.getAllowedAddressRanges().isEmpty());
		}
	}

	// =================================================================================================================
	// B: @Remote call, literal-host check and DNS-pinned connect
	// =================================================================================================================

	@Test void b01_withRanges_notRefusedByPolicy_whileWithoutRangesIsRefused_sameJvm() throws Exception {
		try (var withRanges = RestClient.create().allowAddressRanges("224.0.0.0/4").build();
			var without = RestClient.create().build()) {
			assertTrue(refused(callMessages(without, LAB)));
			assertFalse(refused(callMessages(withRanges, LAB)), "policy refused an allowed range");
			// The ranged client's twin does not leak to the other client.
			assertTrue(refused(callMessages(without, LAB)));
		}
	}

	@Test void b02_withRanges_otherDeniesStayDenied() throws Exception {
		try (var c = RestClient.create().allowAddressRanges("224.0.0.0/4", "198.18.0.0/15").build()) {
			for (var host : new String[]{"127.0.0.1", "169.254.169.254", "10.1.2.3", "localhost", "192.0.0.1"})
				assertTrue(refused(callMessages(c, "http://" + host + ":1/x")), host);
		}
	}

	@Test void b03_pinnedResolver_withRanges_allowsOnlyThoseRanges() throws Exception {
		var resolver = new PolicyPinningDnsResolver(RemoteUrlPolicy.AllowedRanges.of("198.18.0.0/15"));
		assertEquals("198.18.0.1", resolver.resolve("198.18.0.1")[0].getHostAddress());
		assertThrows(IllegalArgumentException.class, () -> PolicyPinningDnsResolver.INSTANCE.resolve("198.18.0.1"));
		assertThrows(IllegalArgumentException.class, () -> resolver.resolve("127.0.0.1"));
		assertThrows(IllegalArgumentException.class, () -> resolver.resolve("169.254.169.254"));
	}
}
