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
 * Tests for {@link SamlAuthFilter} &mdash; path matching, binding selection, request-parameter extraction,
 * and the REDIRECT-binding deflate path.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class SamlAuthFilter_Test extends TestBase {

	private static SamlAssertionValidator validator(java.util.function.Function<String,java.security.Principal> impl) throws Exception {
		var kp = KeyPairGenerator.getInstance("RSA");
		kp.initialize(2048);
		var pair = kp.generateKeyPair();
		return new SamlAssertionValidator(
				SamlAssertionValidator.create()
					.spEntityId("https://sp.example.com")
					.expectedIssuer("https://idp.example.com")
					.signingCredential(new BasicCredential(pair.getPublic(), pair.getPrivate()))) {
			@Override public java.security.Principal validate(String xml) {
				return impl.apply(xml);
			}
		};
	}

	private static HttpServletRequest req(String path, String samlResponse) {
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn(path);
		when(r.getServletPath()).thenReturn(path);
		when(r.getParameter("SAMLResponse")).thenReturn(samlResponse);
		return r;
	}

	@Test void a01_wrongPath_returnsEmpty() throws Exception {
		var f = SamlAuthFilter.create().validator(validator(x -> null)).build();
		assertTrue(f.authenticate(req("/something-else", "anything")).isEmpty());
	}

	@Test void a02_missingParameter_returnsEmpty() throws Exception {
		var f = SamlAuthFilter.create().validator(validator(x -> null)).build();
		assertTrue(f.authenticate(req("/saml/acs", null)).isEmpty());
	}

	@Test void b01_post_decodes_base64() throws Exception {
		var xml = "<dummy/>";
		var b64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
		var received = new String[1];
		var v = validator(x -> { received[0] = x; return new ClaimsPrincipal("alice", Map.of()); });
		var f = SamlAuthFilter.create().validator(v).build();
		var result = f.authenticate(req("/saml/acs", b64));
		assertTrue(result.isPresent());
		assertEquals("<dummy/>", received[0]);
	}

	@Test void b02_redirect_deflateThenBase64_decodes() throws Exception {
		var xml = "<dummy>redirect-content</dummy>";
		var raw = xml.getBytes(StandardCharsets.UTF_8);
		var deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		deflater.setInput(raw);
		deflater.finish();
		var buf = new byte[256];
		int n = deflater.deflate(buf);
		deflater.end();
		var compressed = Arrays.copyOf(buf, n);
		var b64 = Base64.getEncoder().encodeToString(compressed);
		var received = new String[1];
		var v = validator(x -> { received[0] = x; return new ClaimsPrincipal("alice", Map.of()); });
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT).validator(v).build();
		var result = f.authenticate(req("/saml/acs", b64));
		assertTrue(result.isPresent());
		assertEquals("<dummy>redirect-content</dummy>", received[0]);
	}

	@Test void c01_validatorThrows_propagatesWithChallenge() throws Exception {
		var v = validator(x -> { throw new AuthenticationException("bad assertion"); });
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req("/saml/acs", b64)));
		assertTrue(ex.getHeaders().stream().anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName())));
	}

	@Test void c02_validatorReturnsNull_throws() throws Exception {
		var v = validator(x -> null);
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		assertThrows(AuthenticationException.class, () -> f.authenticate(req("/saml/acs", b64)));
	}

	@Test void d01_builder_validatorRequired() {
		assertThrows(IllegalStateException.class, () -> SamlAuthFilter.create().build());
	}

	@Test void d02_rolesClaim_extractsList() throws Exception {
		var cp = new ClaimsPrincipal("alice", Map.of("roles", List.of("admin", "user")));
		var v = validator(x -> cp);
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		var result = f.authenticate(req("/saml/acs", b64));
		assertEquals(Set.of("admin", "user"), result.get().getRoles());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: matchesPath — pathInfo null falls through to servletPath; both null → false
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_pathInfo_null_fallsToServletPath() throws Exception {
		// pathInfo==null → use servletPath; servletPath=="/saml/acs" → matches
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn(null);
		when(r.getServletPath()).thenReturn("/saml/acs");
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		when(r.getParameter("SAMLResponse")).thenReturn(b64);
		var cp = new ClaimsPrincipal("bob", Map.of());
		var f = SamlAuthFilter.create().validator(validator(x -> cp)).build();
		assertTrue(f.authenticate(r).isPresent());
	}

	@Test void e02_bothPathNull_returnsEmpty() throws Exception {
		// pathInfo==null, servletPath==null → s==null → filter does not apply
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn(null);
		when(r.getServletPath()).thenReturn(null);
		when(r.getParameter("SAMLResponse")).thenReturn("anything");
		var f = SamlAuthFilter.create().validator(validator(x -> new ClaimsPrincipal("x", Map.of()))).build();
		assertTrue(f.authenticate(r).isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: invalid base64 → AuthenticationException (decodeSamlResponse line 227)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_invalidBase64_throws() throws Exception {
		var f = SamlAuthFilter.create().validator(validator(x -> new ClaimsPrincipal("x", Map.of()))).build();
		assertThrows(AuthenticationException.class, () -> f.authenticate(req("/saml/acs", "!!!not-base64!!!")));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: validator throws RuntimeException → wrapped in AuthenticationException (runValidator line 271)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_validatorRuntimeException_wrapped() throws Exception {
		var v = validator(x -> { throw new RuntimeException("unexpected"); });
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req("/saml/acs", b64)));
		assertTrue(ex.getMessage().contains("SAML validation failed") || ex.getCause() instanceof RuntimeException);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: extractRoles — non-ClaimsPrincipal → empty roles; rolesClaim value not a List → empty roles
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_nonClaimsPrincipal_emptyRoles() throws Exception {
		// Principal is not a ClaimsPrincipal → extractRoles returns emptySet()
		var v = validator(x -> (Principal) () -> "plain-principal");
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		var result = f.authenticate(req("/saml/acs", b64));
		assertTrue(result.isPresent());
		assertTrue(result.get().getRoles().isEmpty());
	}

	@Test void h02_rolesClaim_notAList_emptyRoles() throws Exception {
		// roles claim is a String, not a List → extractRoles returns emptySet()
		var cp = new ClaimsPrincipal("alice", Map.of("roles", "admin"));
		var v = validator(x -> cp);
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		var result = f.authenticate(req("/saml/acs", b64));
		assertTrue(result.isPresent());
		assertTrue(result.get().getRoles().isEmpty());
	}

	@Test void h03_rolesClaim_listWithNonStringItem_skipped() throws Exception {
		// List contains a non-String element → item instanceof String is false → item skipped
		var roles = new ArrayList<>();
		roles.add("admin");
		roles.add(42);  // non-String item
		roles.add("user");
		var cp = new ClaimsPrincipal("alice", Map.of("roles", roles));
		var v = validator(x -> cp);
		var f = SamlAuthFilter.create().validator(v).build();
		var b64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));
		var result = f.authenticate(req("/saml/acs", b64));
		assertTrue(result.isPresent());
		assertEquals(Set.of("admin", "user"), result.get().getRoles());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// I: inflate() — malformed/truncated DEFLATE stream paths (lines 244-247)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_redirect_malformedDeflate_dataFormatException() throws Exception {
		// Random bytes → Inflater.inflate() throws DataFormatException → caught → IOException → AuthenticationException
		var v = validator(x -> new ClaimsPrincipal("x", Map.of()));
		var f = SamlAuthFilter.create().binding(SamlBinding.REDIRECT).validator(v).build();
		var b64 = Base64.getEncoder().encodeToString(new byte[]{(byte)0xff, (byte)0xfe, 0x01, 0x02, 0x03, 0x04});
		assertThrows(AuthenticationException.class, () -> f.authenticate(req("/saml/acs", b64)));
	}
}
