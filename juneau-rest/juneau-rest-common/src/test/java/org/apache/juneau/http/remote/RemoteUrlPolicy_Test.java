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
package org.apache.juneau.http.remote;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Red-on-broken verification-gate tests for {@link RemoteUrlPolicy}: the shared deny-private + pin-on-connect +
 * redirect-revalidation SSRF guardrail used by both the next-generation and classic {@code @Remote}-proxy engines.
 *
 * <p>
 * Every test here fails against the pre-guardrail scheme-only behavior and passes with the deny-private policy in
 * place; see {@code TODO-392-remote-url-ssrf-resolved-address.md} "Test notes" for the source checklist.
 */
class RemoteUrlPolicy_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// requireAllowedUrl(String, boolean) -- scheme requirement (always enforced)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_requireAllowedUrl_http_accepted() {
		assertEquals("http://example.com", RemoteUrlPolicy.requireAllowedUrl("http://example.com", false));
	}

	@Test void a02_requireAllowedUrl_https_accepted() {
		assertEquals("HTTPS://example.com", RemoteUrlPolicy.requireAllowedUrl("HTTPS://example.com", false));
	}

	@Test void a03_requireAllowedUrl_relativeScheme_less_passesThrough() {
		assertEquals("/relative/path", RemoteUrlPolicy.requireAllowedUrl("/relative/path", false));
	}

	@Test void a04_requireAllowedUrl_fileScheme_rejected_evenWithAllowPrivateUrls() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Unsupported URL scheme",
			() -> RemoteUrlPolicy.requireAllowedUrl("file:///etc/passwd", true));
	}

	@Test void a05_requireAllowedUrl_ftpScheme_rejected_evenWithAllowPrivateUrls() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Unsupported URL scheme",
			() -> RemoteUrlPolicy.requireAllowedUrl("ftp://evil/x", true));
	}

	@Test void a06_requireAllowedUrl_nonCanonicalOpaqueForm_rejected() {
		// "http:foo" has a scheme but no authority/host -- URI.getHost() is null.
		assertThrowsWithMessage(IllegalArgumentException.class, "no host",
			() -> RemoteUrlPolicy.requireAllowedUrl("http:foo", false));
	}

	//------------------------------------------------------------------------------------------------------------------
	// requireAllowedUrl(String, boolean) -- deny-private (default allowPrivateUrls=false)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_requireAllowedUrl_loopbackIpv4_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://127.0.0.1/", false));
	}

	@Test void b02_requireAllowedUrl_loopbackIpv4_acceptedWithAllowPrivateUrls() {
		assertEquals("http://127.0.0.1/", RemoteUrlPolicy.requireAllowedUrl("http://127.0.0.1/", true));
	}

	@Test void b03_requireAllowedUrl_rfc1918_rejectedByDefault_acceptedWithOptIn() {
		for (var host : new String[]{"10.1.2.3", "172.16.0.1", "192.168.1.1"}) {
			var url = "http://" + host + "/";
			assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback", () -> RemoteUrlPolicy.requireAllowedUrl(url, false));
			assertEquals(url, RemoteUrlPolicy.requireAllowedUrl(url, true));
		}
	}

	@Test void b04_requireAllowedUrl_linkLocalImds_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://169.254.169.254/latest/meta-data", false));
	}

	@Test void b05_requireAllowedUrl_linkLocalEcs_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://169.254.170.2/", false));
	}

	@Test void b06_requireAllowedUrl_localhostName_rejectedByDefault_acceptedWithOptIn() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://localhost:8080/", false));
		assertEquals("http://localhost:8080/", RemoteUrlPolicy.requireAllowedUrl("http://localhost:8080/", true));
	}

	@Test void b07_requireAllowedUrl_localhostLocaldomain_caseInsensitive_rejected() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://LocalHost.LocalDomain/", false));
	}

	@Test void b08_requireAllowedUrl_gceMetadataHost_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://metadata.google.internal/computeMetadata/v1/", false));
	}

	@Test void b09_requireAllowedUrl_gceMetadataSubdomain_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://foo.metadata.google.internal/", false));
	}

	@Test void b10_requireAllowedUrl_ipv6Loopback_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://[::1]/", false));
	}

	@Test void b11_requireAllowedUrl_ipv6LinkLocal_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://[fe80::1]/", false));
	}

	@Test void b12_requireAllowedUrl_ipv6UniqueLocal_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://[fc00::1]/", false));
	}

	@Test void b13_requireAllowedUrl_ipv4MappedIpv6Loopback_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://[::ffff:127.0.0.1]/", false));
	}

	@Test void b14_requireAllowedUrl_publicHost_acceptedByDefault() {
		assertEquals("http://example.com/api", RemoteUrlPolicy.requireAllowedUrl("http://example.com/api", false));
	}

	@Test void b15_requireAllowedUrl_publicIpv4Literal_acceptedByDefault() {
		assertEquals("http://93.184.216.34/", RemoteUrlPolicy.requireAllowedUrl("http://93.184.216.34/", false));
	}

	@Test void b16_requireAllowedUrl_cgnat_notDenied() {
		// CGNAT (100.64.0.0/10) is explicitly NOT in the deny-list per the locked design.
		assertEquals("http://100.64.1.1/", RemoteUrlPolicy.requireAllowedUrl("http://100.64.1.1/", false));
	}

	//------------------------------------------------------------------------------------------------------------------
	// isDeniedHost / isDeniedAddress -- direct classification
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_isDeniedHost_unspecifiedIpv4() {
		assertTrue(RemoteUrlPolicy.isDeniedHost("0.0.0.0"));
	}

	@Test void c02_isDeniedHost_ordinaryDomainName_notDenied() {
		assertFalse(RemoteUrlPolicy.isDeniedHost("example.com"));
	}

	@Test void c04_isDeniedAddress_ipv4Unspecified() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("0.0.0.0")));
	}

	@Test void c05_isDeniedAddress_publicAddress_notDenied() throws UnknownHostException {
		assertFalse(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByAddress(new byte[]{93, (byte) 184, (byte) 216, 34})));
	}

	//------------------------------------------------------------------------------------------------------------------
	// selectAllowedAddress(String, boolean, AddressResolver) -- pin-on-connect
	//------------------------------------------------------------------------------------------------------------------

	private static InetAddress addr(int a, int b, int c, int d) throws UnknownHostException {
		return InetAddress.getByAddress(new byte[]{(byte) a, (byte) b, (byte) c, (byte) d});
	}

	@Test void d01_selectAllowedAddress_dnsNameResolvesToBlockedAddress_rejected() throws Exception {
		RemoteUrlPolicy.AddressResolver resolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertThrowsWithMessage(IllegalArgumentException.class, "No allowed address",
			() -> RemoteUrlPolicy.selectAllowedAddress("internal.example.com", false, resolver));
	}

	@Test void d02_selectAllowedAddress_dnsNameResolvesToBlockedAddress_acceptedWithAllowPrivateUrls() throws Exception {
		RemoteUrlPolicy.AddressResolver resolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertEquals(addr(127, 0, 0, 1), RemoteUrlPolicy.selectAllowedAddress("internal.example.com", true, resolver));
	}

	@Test void d03_selectAllowedAddress_mixedCandidates_oneLoopbackOnePublic_selectsPublic() throws Exception {
		var loopback = addr(127, 0, 0, 1);
		var pub = addr(93, 184, 216, 34);
		RemoteUrlPolicy.AddressResolver resolver = host -> new InetAddress[]{loopback, pub};
		assertEquals(pub, RemoteUrlPolicy.selectAllowedAddress("mixed.example.com", false, resolver));
	}

	@Test void d04_selectAllowedAddress_mixedCandidates_publicFirst_stillSelectsPublic_neverBlocked() throws Exception {
		var pub = addr(93, 184, 216, 34);
		var loopback = addr(127, 0, 0, 1);
		var selected = RemoteUrlPolicy.selectAllowedAddress("mixed2.example.com", false, host -> new InetAddress[]{pub, loopback});
		assertEquals(pub, selected);
		assertNotEquals(loopback, selected);
	}

	@Test void d05_selectAllowedAddress_rebinding_stringCheckPassed_butConnectTimeBlocked() throws Exception {
		// Simulates DNS rebinding: the hostname itself is not in the lexical deny-list (isDeniedHost("rebind.example.com")
		// is false), so requireAllowedUrl's pre-check passes -- but the address actually resolved at connect time is
		// loopback, so pin-on-connect must still reject it.
		assertFalse(RemoteUrlPolicy.isDeniedHost("rebind.example.com"));
		RemoteUrlPolicy.AddressResolver rebindingResolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertThrowsWithMessage(IllegalArgumentException.class, "No allowed address",
			() -> RemoteUrlPolicy.selectAllowedAddress("rebind.example.com", false, rebindingResolver));
	}

	@Test void d06_selectAllowedAddress_decimalIpLiteralForLoopback_rejectedViaResolver() throws Exception {
		// http://2130706433 is the decimal form of 127.0.0.1. isDeniedHost() intentionally does not special-case
		// decimal/octal/hex literals (they don't look like a dotted IPv4 literal); pin-on-connect via the resolver
		// (which is what a real InetAddress.getAllByName("2130706433") would resolve to) is what catches this.
		RemoteUrlPolicy.AddressResolver decimalResolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertFalse(RemoteUrlPolicy.isDeniedHost("2130706433"));
		assertThrowsWithMessage(IllegalArgumentException.class, "No allowed address",
			() -> RemoteUrlPolicy.selectAllowedAddress("2130706433", false, decimalResolver));
	}

	@Test void d07_selectAllowedAddress_unresolvable_propagatesUnknownHostException() {
		RemoteUrlPolicy.AddressResolver failing = host -> { throw new UnknownHostException(host); };
		assertThrows(UnknownHostException.class, () -> RemoteUrlPolicy.selectAllowedAddress("nope.invalid", false, failing));
	}

	//------------------------------------------------------------------------------------------------------------------
	// resolveRedirectLocation(URI, String) -- redirect-hop resolution
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_resolveRedirectLocation_absoluteLocation() {
		var target = RemoteUrlPolicy.resolveRedirectLocation(URI.create("http://example.com/a"), "http://169.254.169.254/latest/meta-data");
		assertEquals("http://169.254.169.254/latest/meta-data", target.toString());
	}

	@Test void e02_resolveRedirectLocation_relativeLocation_resolvedAgainstCurrentRequestUri() {
		var target = RemoteUrlPolicy.resolveRedirectLocation(URI.create("http://example.com/a/b"), "../c");
		assertEquals("http://example.com/c", target.toString());
	}

	@Test void e03_resolveRedirectLocation_publicUrlRedirectsToBlockedAddress_rejectedByFullPolicy() {
		// End-to-end: resolve the hop, then require the resolved URL pass the full deny-private policy -- this is
		// the "public @Url that 302s to http://169.254.169.254/" scenario from the design's test notes.
		var target = RemoteUrlPolicy.resolveRedirectLocation(URI.create("http://example.com/a"), "http://169.254.169.254/latest/meta-data");
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl(target.toString(), false));
	}

	@Test void e04_resolveRedirectLocation_missingLocation_rejected() {
		assertThrowsWithMessage(IllegalArgumentException.class, "missing a Location",
			() -> RemoteUrlPolicy.resolveRedirectLocation(URI.create("http://example.com/a"), null));
	}

	@Test void e05_resolveRedirectLocation_blankLocation_rejected() {
		assertThrowsWithMessage(IllegalArgumentException.class, "missing a Location",
			() -> RemoteUrlPolicy.resolveRedirectLocation(URI.create("http://example.com/a"), "   "));
	}

	@Test void e06_resolveRedirectLocation_nonAbsoluteAfterResolution_rejected() {
		// A schemeless, non-absolute Location can only happen for a "from" URI that is itself not absolute
		// (should not occur in practice -- @Remote redirect hops always start from an absolute URI), but the guard
		// must still reject rather than silently proceed.
		assertThrowsWithMessage(IllegalArgumentException.class, "did not resolve to an absolute URI",
			() -> RemoteUrlPolicy.resolveRedirectLocation(URI.create("/relative-base"), "/still-relative"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// schemeOf(String) -- retained lexical scheme parse (used by the pre-check)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_schemeOf_emptyString_isNull() {
		assertNull(RemoteUrlPolicy.schemeOf(""));
	}

	@Test void f02_schemeOf_leadingNonLetter_isNull() {
		assertNull(RemoteUrlPolicy.schemeOf("1abc/path"));
		assertNull(RemoteUrlPolicy.schemeOf("{var}/path"));
	}

	@Test void f03_schemeOf_colonBeforeSlash_returnsScheme() {
		assertEquals("http", RemoteUrlPolicy.schemeOf("http://host/path"));
		assertEquals("https", RemoteUrlPolicy.schemeOf("https://host/path"));
	}

	@Test void f04_schemeOf_noColonAnywhere_isNull() {
		assertNull(RemoteUrlPolicy.schemeOf("plain-relative-path"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// AddressResolver.DEFAULT -- real DNS wiring sanity (does not require live rebinding DNS)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_addressResolverDefault_resolvesLoopbackLiteral() throws Exception {
		var addrs = RemoteUrlPolicy.AddressResolver.DEFAULT.resolve("127.0.0.1");
		assertTrue(addrs.length > 0);
		assertTrue(RemoteUrlPolicy.isDeniedAddress(addrs[0]));
	}
}
