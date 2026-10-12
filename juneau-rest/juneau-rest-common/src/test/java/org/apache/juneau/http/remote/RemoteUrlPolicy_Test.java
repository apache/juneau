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
 * place; see the remote-URL SSRF resolved-address design notes "Test notes" for the source checklist.
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

	@Test void b13a_requireAllowedUrl_ipv4CompatibleIpv6Loopback_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://[::127.0.0.1]/", false));
	}

	@Test void b13b_requireAllowedUrl_ipv4CompatibleIpv6PrivateWithPort_rejectedByDefault() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://[::10.0.0.1]:8080/", false));
	}

	@Test void b14_requireAllowedUrl_publicHost_acceptedByDefault() {
		assertEquals("http://example.com/api", RemoteUrlPolicy.requireAllowedUrl("http://example.com/api", false));
	}

	@Test void b15_requireAllowedUrl_publicIpv4Literal_acceptedByDefault() {
		assertEquals("http://93.184.216.34/", RemoteUrlPolicy.requireAllowedUrl("http://93.184.216.34/", false));
	}

	@Test void b16_requireAllowedUrl_cgnat_denied() {
		// CGNAT shared space (100.64.0.0/10) is often internal cloud infrastructure, so it is denied.
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://100.64.1.1/", false));
		assertEquals("http://100.128.0.1/", RemoteUrlPolicy.requireAllowedUrl("http://100.128.0.1/", false));
	}

	@Test void b17_requireAllowedUrl_thisNetwork_denied() {
		assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
			() -> RemoteUrlPolicy.requireAllowedUrl("http://0.0.0.5/", false));
	}

	// Embedded-IPv4 / special IPv6 forms, via bracketed URLs.
	@Test void b18_requireAllowedUrl_embeddedIpv4Forms_denied() {
		for (var host : new String[]{
			"[64:ff9b::7f00:1]", "[64:ff9b::a9fe:a9fe]", "[64:ff9b:1::7f00:1]",
			"[2002:7f00:1::]", "[2002:a00:1::1]", "[::ffff:0:127.0.0.1]", "[::ffff:0:a9fe:a9fe]",
			"[2001:0:4136:e378:8000:63bf:3fff:fdd2]", "[2001::1]"
		}) {
			var url = "http://" + host + "/";
			assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
				() -> RemoteUrlPolicy.requireAllowedUrl(url, false));
		}
	}

	@Test void b18a_requireAllowedUrl_nat64LocalUse_deniedOutright() {
		for (var host : new String[]{"[64:ff9b:1:808::a00:1]", "[64:ff9b:1::808:808]"}) {
			var url = "http://" + host + "/";
			assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
				() -> RemoteUrlPolicy.requireAllowedUrl(url, false));
		}
		assertEquals("http://[64:ff9b:1::808:808]/", RemoteUrlPolicy.requireAllowedUrl("http://[64:ff9b:1::808:808]/", true));
	}

	@Test void b19_requireAllowedUrl_embeddedPublicIpv4_allowed() {
		assertEquals("http://[64:ff9b::808:808]/", RemoteUrlPolicy.requireAllowedUrl("http://[64:ff9b::808:808]/", false));
		assertEquals("http://[2002:808:808::]/", RemoteUrlPolicy.requireAllowedUrl("http://[2002:808:808::]/", false));
		assertEquals("http://[::ffff:0:8.8.8.8]/", RemoteUrlPolicy.requireAllowedUrl("http://[::ffff:0:8.8.8.8]/", false));
	}

	@Test void b20_requireAllowedUrl_zoneIdLiteral_denied() {
		for (var url : new String[]{"http://[fe80::1%25en0]/", "http://[fe80::1%25nonexistent99]:8080/", "http://[::1%25lo]/"}) {
			assertThrowsWithMessage(IllegalArgumentException.class, "private/loopback",
				() -> RemoteUrlPolicy.requireAllowedUrl(url, false));
		}
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

	// IPv4-compatible IPv6 (::a.b.c.d, first 12 bytes zero) -- built from raw bytes so no resolver/DNS is involved.
	private static InetAddress compat(int a, int b, int c, int d) throws UnknownHostException {
		return InetAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) a, (byte) b, (byte) c, (byte) d});
	}

	@Test void c06_isDeniedAddress_ipv4CompatibleLoopback_denied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(compat(127, 0, 0, 1)));
	}

	@Test void c07_isDeniedAddress_ipv4CompatiblePrivate_denied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(compat(10, 0, 0, 1)));
	}

	@Test void c08_isDeniedAddress_ipv4CompatibleLinkLocalMetadata_denied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(compat(169, 254, 169, 254)));
	}

	@Test void c09_isDeniedAddress_ipv6Loopback_stillDenied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("::1")));
	}

	@Test void c10_isDeniedAddress_ipv6Unspecified_stillDenied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("::")));
	}

	@Test void c11_isDeniedAddress_ipv4CompatiblePublic_followsIpv4Rules() throws UnknownHostException {
		assertFalse(RemoteUrlPolicy.isDeniedAddress(compat(8, 8, 8, 8)));
	}

	@Test void c12_isDeniedHost_ipv4CompatibleLiterals() {
		assertTrue(RemoteUrlPolicy.isDeniedHost("::127.0.0.1"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("::10.0.0.1"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("::169.254.169.254"));
		assertFalse(RemoteUrlPolicy.isDeniedHost("::8.8.8.8"));
	}

	// Raw-byte IPv6 builder: 'prefix' bytes, zero fill, and the IPv4 octets placed at the given byte offsets.
	private static InetAddress v6(int[] prefix, int[] offsets, int a, int b, int c, int d) throws UnknownHostException {
		var bytes = new byte[16];
		for (var i = 0; i < prefix.length; i++)
			bytes[i] = (byte) prefix[i];
		var v4 = new int[]{a, b, c, d};
		for (var i = 0; i < 4; i++)
			bytes[offsets[i]] = (byte) v4[i];
		return InetAddress.getByAddress(bytes);
	}

	private static InetAddress nat64(int a, int b, int c, int d) throws UnknownHostException {
		return v6(new int[]{0x00, 0x64, 0xff, 0x9b}, new int[]{12, 13, 14, 15}, a, b, c, d);
	}

	private static InetAddress sixToFour(int a, int b, int c, int d) throws UnknownHostException {
		return v6(new int[]{0x20, 0x02}, new int[]{2, 3, 4, 5}, a, b, c, d);
	}

	private static InetAddress translated(int a, int b, int c, int d) throws UnknownHostException {
		// RFC 2765 IPv4-translated ::ffff:0:a.b.c.d (bytes 8-9 = 0xFFFF).
		return v6(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0xff, 0xff}, new int[]{12, 13, 14, 15}, a, b, c, d);
	}

	@Test void c13_isDeniedAddress_nat64_embeddedDenied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(nat64(127, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(nat64(10, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(nat64(169, 254, 169, 254)));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(nat64(8, 8, 8, 8)));
	}

	@Test void c14_isDeniedAddress_nat64LocalUse48_deniedOutright() throws UnknownHostException {
		// The embedding length of 64:ff9b:1::/48 (RFC 8215) is operator-defined (/48, /56, /64, /96), so the whole /48 is denied.
		for (var literal : new String[]{
			"64:ff9b:1:808::a00:1",  // 10.0.0.1 in a /56-style embedding (previously a bypass)
			"64:ff9b:1::808:808",    // public-looking address in a /96-style embedding
			"64:ff9b:1::7f00:1",
			"64:ff9b:1:ffff:ffff:ffff:ffff:ffff"
		})
			assertTrue(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName(literal)));
		// Neighbouring prefixes are unaffected.
		assertFalse(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("64:ff9b:2::808:808")));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("64:ff9b::808:808")));
	}

	@Test void c15_isDeniedAddress_6to4_embeddedDenied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(sixToFour(127, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(sixToFour(172, 16, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(sixToFour(169, 254, 169, 254)));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(sixToFour(8, 8, 8, 8)));
	}

	@Test void c16_isDeniedAddress_ipv4Translated_embeddedDenied() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(translated(127, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(translated(10, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(translated(169, 254, 169, 254)));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(translated(8, 8, 8, 8)));
	}

	@Test void c17_isDeniedAddress_teredo_deniedOutright() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("2001:0:4136:e378:8000:63bf:3fff:fdd2")));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("2001::1")));
		// Only 2001::/32 -- other 2001::/16 space is unaffected.
		assertFalse(RemoteUrlPolicy.isDeniedAddress(InetAddress.getByName("2001:4860:4860::8888")));
	}

	@Test void c18_isDeniedAddress_ipv4ThisNetworkAndCgnat() throws UnknownHostException {
		assertTrue(RemoteUrlPolicy.isDeniedAddress(addr(0, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(addr(0, 255, 255, 255)));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(addr(1, 0, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(addr(100, 64, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(addr(100, 127, 255, 254)));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(addr(100, 63, 255, 255)));
		assertFalse(RemoteUrlPolicy.isDeniedAddress(addr(100, 128, 0, 1)));
		// Reached through an embedded form too.
		assertTrue(RemoteUrlPolicy.isDeniedAddress(nat64(100, 64, 0, 1)));
		assertTrue(RemoteUrlPolicy.isDeniedAddress(compat(0, 0, 0, 5)));
	}

	@Test void c19_isDeniedHost_zoneIdLiterals_failClosed() {
		assertTrue(RemoteUrlPolicy.isDeniedHost("[fe80::1%en0]"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("fe80::1%nonexistent99"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("[::1%25lo]"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("[fe80::1%25nonexistent99]"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("[::ffff:127.0.0.1%x]"));
		// Any colon-containing literal that still fails to parse is denied.
		assertTrue(RemoteUrlPolicy.isDeniedHost("[zz::1]"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("1:2:3:4:5:6:7:8:9"));
		// A parseable public zoned literal is classified by the address, not blanket-denied.
		assertFalse(RemoteUrlPolicy.isDeniedHost("[2606:4700::1111%nonexistent99]"));
	}

	@Test void c20_isDeniedHost_embeddedLiterals() {
		assertTrue(RemoteUrlPolicy.isDeniedHost("64:ff9b:1:808::a00:1"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("64:ff9b::7f00:1"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("2002:7f00:1::"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("::ffff:0:10.0.0.1"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("2001:0:1::1"));
		assertFalse(RemoteUrlPolicy.isDeniedHost("64:ff9b::808:808"));
		assertFalse(RemoteUrlPolicy.isDeniedHost("2002:808:808::"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("100.64.0.1"));
		assertTrue(RemoteUrlPolicy.isDeniedHost("0.1.2.3"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// selectAllowedAddress(String, boolean, AddressResolver) -- pin-on-connect
	//------------------------------------------------------------------------------------------------------------------

	private static InetAddress addr(int a, int b, int c, int d) throws UnknownHostException {
		return InetAddress.getByAddress(new byte[]{(byte) a, (byte) b, (byte) c, (byte) d});
	}

	@Test void d01_selectAllowedAddress_dnsNameResolvesToBlockedAddress_rejected() {
		RemoteUrlPolicy.AddressResolver resolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertThrowsWithMessage(IllegalArgumentException.class, "No allowed address",
			() -> RemoteUrlPolicy.selectAllowedAddress("internal.example.com", false, resolver));
	}

	@Test void d02_selectAllowedAddress_dnsNameResolvesToBlockedAddress_acceptedWithAllowPrivateUrls() throws UnknownHostException {
		RemoteUrlPolicy.AddressResolver resolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertEquals(addr(127, 0, 0, 1), RemoteUrlPolicy.selectAllowedAddress("internal.example.com", true, resolver));
	}

	@Test void d03_selectAllowedAddress_mixedCandidates_oneLoopbackOnePublic_selectsPublic() throws UnknownHostException {
		var loopback = addr(127, 0, 0, 1);
		var pub = addr(93, 184, 216, 34);
		RemoteUrlPolicy.AddressResolver resolver = host -> new InetAddress[]{loopback, pub};
		assertEquals(pub, RemoteUrlPolicy.selectAllowedAddress("mixed.example.com", false, resolver));
	}

	@Test void d04_selectAllowedAddress_mixedCandidates_publicFirst_stillSelectsPublic_neverBlocked() throws UnknownHostException {
		var pub = addr(93, 184, 216, 34);
		var loopback = addr(127, 0, 0, 1);
		var selected = RemoteUrlPolicy.selectAllowedAddress("mixed2.example.com", false, host -> new InetAddress[]{pub, loopback});
		assertEquals(pub, selected);
		assertNotEquals(loopback, selected);
	}

	@Test void d05_selectAllowedAddress_rebinding_stringCheckPassed_butConnectTimeBlocked() {
		// Simulates DNS rebinding: the hostname itself is not in the lexical deny-list (isDeniedHost("rebind.example.com")
		// is false), so requireAllowedUrl's pre-check passes -- but the address actually resolved at connect time is
		// loopback, so pin-on-connect must still reject it.
		assertFalse(RemoteUrlPolicy.isDeniedHost("rebind.example.com"));
		RemoteUrlPolicy.AddressResolver rebindingResolver = host -> new InetAddress[]{addr(127, 0, 0, 1)};
		assertThrowsWithMessage(IllegalArgumentException.class, "No allowed address",
			() -> RemoteUrlPolicy.selectAllowedAddress("rebind.example.com", false, rebindingResolver));
	}

	@Test void d06_selectAllowedAddress_decimalIpLiteralForLoopback_rejectedViaResolver() {
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
