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
package org.apache.juneau.rest.server.auth.saml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.opensaml.security.credential.*;

import jakarta.servlet.http.*;

/**
 * Tests for the bounded-inflate output cap and ratio guard on the {@link SamlBinding#REDIRECT} binding of
 * {@link SamlAuthFilter}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class SamlAuthFilter_MaxInflate_Test extends TestBase {

	private static SamlAssertionValidator validator(java.util.function.Function<String,Principal> impl) throws Exception {
		var kp = KeyPairGenerator.getInstance("RSA");
		kp.initialize(2048);
		var pair = kp.generateKeyPair();
		return new SamlAssertionValidator(
				SamlAssertionValidator.create()
					.spEntityId("https://sp.example.com")
					.expectedIssuer("https://idp.example.com")
					.signingCredential(new BasicCredential(pair.getPublic(), pair.getPrivate()))) {
			@Override public Principal validate(String xml) {
				return impl.apply(xml);
			}
		};
	}

	private static HttpServletRequest req(String samlResponse) {
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn("/saml/acs");
		when(r.getServletPath()).thenReturn("/saml/acs");
		when(r.getParameter("SAMLResponse")).thenReturn(samlResponse);
		return r;
	}

	/** Raw-DEFLATE (nowrap) + base64, matching the SAML REDIRECT binding encoding. */
	private static String deflateBase64(byte[] raw) {
		var deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		deflater.setInput(raw);
		deflater.finish();
		var out = new java.io.ByteArrayOutputStream();
		var buf = new byte[8192];
		while (!deflater.finished()) {
			int n = deflater.deflate(buf);
			out.write(buf, 0, n);
		}
		deflater.end();
		return Base64.getEncoder().encodeToString(out.toByteArray());
	}

	@Test void a01_highlyCompressiblePayload_exceedsDefaultCap_rejected() throws Exception {
		// 2 MiB of zeros deflates to a few KB; inflating exceeds the 1 MiB default cap → rejected.
		var bomb = new byte[2 * 1024 * 1024];
		var b64 = deflateBase64(bomb);
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT)
			.validator(validator(x -> new ClaimsPrincipal("x", Map.of())))
			.build();
		assertThrows(AuthenticationException.class, () -> f.authenticate(req(b64)));
	}

	@Test void a02_customLowByteCap_rejectsModeratePayload() throws Exception {
		// 64 KiB of zeros inflates well past a 4 KiB cap → rejected.
		var payload = new byte[64 * 1024];
		var b64 = deflateBase64(payload);
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT)
			.maxInflatedBytes(4 * 1024)
			.validator(validator(x -> new ClaimsPrincipal("x", Map.of())))
			.build();
		assertThrows(AuthenticationException.class, () -> f.authenticate(req(b64)));
	}

	@Test void a03_ratioGuard_rejectsEvenUnderByteCap() throws Exception {
		// 256 KiB of zeros deflates to well under 2 KiB; a ratio cap of 5 trips long before the byte cap.
		var payload = new byte[256 * 1024];
		var b64 = deflateBase64(payload);
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT)
			.maxInflatedBytes(0)   // disable absolute cap; rely solely on ratio guard
			.maxInflateRatio(5)
			.validator(validator(x -> new ClaimsPrincipal("x", Map.of())))
			.build();
		assertThrows(AuthenticationException.class, () -> f.authenticate(req(b64)));
	}

	@Test void b01_legitimateSmallPayload_stillDecodes() throws Exception {
		var xml = "<samlp:Response>ok</samlp:Response>";
		var b64 = deflateBase64(xml.getBytes(StandardCharsets.UTF_8));
		var received = new String[1];
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT)
			.validator(validator(x -> { received[0] = x; return new ClaimsPrincipal("alice", Map.of()); }))
			.build();
		var result = f.authenticate(req(b64));
		assertTrue(result.isPresent());
		assertEquals(xml, received[0]);
	}

	@Test void b02_payloadAtCustomCapBoundary_accepted() throws Exception {
		// A small payload comfortably under a generous custom cap is accepted.
		var xml = "<r>" + "a".repeat(500) + "</r>";
		var b64 = deflateBase64(xml.getBytes(StandardCharsets.UTF_8));
		var received = new String[1];
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT)
			.maxInflatedBytes(64 * 1024)
			.maxInflateRatio(0)  // disable ratio guard for this boundary check
			.validator(validator(x -> { received[0] = x; return new ClaimsPrincipal("alice", Map.of()); }))
			.build();
		var result = f.authenticate(req(b64));
		assertTrue(result.isPresent());
		assertEquals(xml, received[0]);
	}
}
