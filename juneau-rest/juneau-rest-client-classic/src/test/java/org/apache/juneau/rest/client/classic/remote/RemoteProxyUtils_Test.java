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
package org.apache.juneau.rest.client.classic.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Parity tests pinning that the <b>classic</b> engine's {@link RemoteProxyUtils#requireHttpScheme(String)} enforces
 * the same SSRF guardrail as the next-generation engine's {@code RemoteClient$RemoteInvocationHandler}
 * (see {@code RemoteClient_InvocationHandlerInternals_Test} in {@code juneau-rest-client}) &mdash; both now delegate
 * to the shared {@link RemoteUrlPolicy}.
 *
 * <p>
 * Unlike the next-gen counterpart, {@code requireHttpScheme} here is {@code public static}, so these tests call it
 * directly rather than through reflection.
 *
 * <p>
 * <b>[TODO-396]/[TODO-392]:</b> `b01`-`b05` originated as 396's scheme-only parity tests; `c01`-`c04` were added by
 * 392 to extend that parity coverage to the deny-private + {@code allowPrivateUrls} opt-in policy. See
 * {@code RemoteUrlPolicy_Test} (juneau-rest-common) for the full deny-list/pin-on-connect/redirect unit coverage.
 */
class RemoteProxyUtils_Test {

	// ==========================================================================
	// b - requireHttpScheme(String) -- parity with next-gen's b01-b04
	// ==========================================================================

	@Test void b01_requireHttpScheme_http_accepted() {
		assertEquals("http://x", RemoteProxyUtils.requireHttpScheme("http://x"));
	}

	@Test void b02_requireHttpScheme_https_accepted() {
		assertEquals("HTTPS://x", RemoteProxyUtils.requireHttpScheme("HTTPS://x"));
	}

	@Test void b03_requireHttpScheme_noScheme_passesThrough() {
		assertEquals("/relative/path", RemoteProxyUtils.requireHttpScheme("/relative/path"));
	}

	@Test void b04_requireHttpScheme_otherScheme_rejected() {
		assertThrows(IllegalArgumentException.class, () -> RemoteProxyUtils.requireHttpScheme("ftp://evil/x"));
	}

	@Test void b05_requireHttpScheme_fileScheme_rejected() {
		// The classic proxy's own dynamic-URL/baseUrl resolution (RestClient.resolveRemoteUri) applies an
		// equivalent guard at the fully-resolved-URL level (see RemoteProxyParity_Test#t2d), but requireHttpScheme
		// itself must independently reject file: too -- it's the same helper the resolution path delegates to.
		assertThrows(IllegalArgumentException.class, () -> RemoteProxyUtils.requireHttpScheme("file:///etc/passwd"));
	}

	// ==========================================================================
	// c - requireHttpScheme(String, boolean) -- [TODO-392] deny-private + allowPrivateUrls parity
	// ==========================================================================

	@Test void c01_requireHttpScheme_loopback_rejectedByDefault() {
		assertThrows(IllegalArgumentException.class, () -> RemoteProxyUtils.requireHttpScheme("http://127.0.0.1/", false));
	}

	@Test void c02_requireHttpScheme_loopback_acceptedWithAllowPrivateUrls() {
		assertEquals("http://127.0.0.1/", RemoteProxyUtils.requireHttpScheme("http://127.0.0.1/", true));
	}

	@Test void c03_requireHttpScheme_singleArgOverload_delegatesToDenyPrivateDefault() {
		// requireHttpScheme(String) == requireHttpScheme(url, false); confirms the classic engine's default call
		// site (no allowPrivateUrls plumbed through) still enforces deny-private, not just scheme.
		assertThrows(IllegalArgumentException.class, () -> RemoteProxyUtils.requireHttpScheme("http://169.254.169.254/"));
	}

	@Test void c04_requireHttpScheme_fileScheme_rejected_evenWithAllowPrivateUrls() {
		assertThrows(IllegalArgumentException.class, () -> RemoteProxyUtils.requireHttpScheme("file:///etc/passwd", true));
	}
}
