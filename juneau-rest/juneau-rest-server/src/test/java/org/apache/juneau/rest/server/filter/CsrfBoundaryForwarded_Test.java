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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.rest.server.filter.BoundaryReason.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.filter.CsrfBoundary.*;
import org.apache.juneau.rest.server.filter.CsrfBoundary_Test.R;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests for {@link CsrfBoundary.Builder#trustedForwarding(ForwardedSource)} on HTTP requests and WebSocket handshakes.
 *
 * <p>
 * Reasons are asserted by direct calls, because the bus configurator collapses every handshake refusal to 4403.
 *
 * @since 10.0.0
 */
class CsrfBoundaryForwarded_Test extends TestBase {

	private static final String PUBLIC = "https://app.example";

	private static CsrfBoundary.Builder xf() {
		return CsrfBoundary.create().trustedForwarding(ForwardedSource.X_FORWARDED);
	}

	private static CsrfBoundary.Builder fwd() {
		return CsrfBoundary.create().trustedForwarding(ForwardedSource.FORWARDED);
	}

	/** A same-origin JSON write as seen by a backend behind a proxy that rewrites {@code Host}. */
	private static R post(CsrfBoundary b, String origin) {
		return new R().method("POST").scheme("http").h("Host", "backend:8080").h("Origin", origin)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()));
	}

	private static R viaXf(CsrfBoundary b, String origin, String host, String proto) {
		return post(b, origin).h("X-Forwarded-Host", host).h("X-Forwarded-Proto", proto);
	}

	private static void allowed(CsrfBoundary b, R r) {
		var x = b.check(r.build());
		assertTrue(x.isAllowed(), () -> "expected allowed, got: " + x);
	}

	private static void refused(CsrfBoundary b, R r, BoundaryReason reason) {
		var x = b.check(r.build());
		assertFalse(x.isAllowed(), () -> "expected " + reason + ", got allowed");
		assertEquals(reason, x.reason(), x::toString);
		assertEquals(reason == HOST_NOT_ALLOWED ? 421 : 403, x.status(), x::toString);
	}

	private static Map<String,List<String>> map(String...nameValue) {
		var m = new LinkedHashMap<String,List<String>>();
		for (var i = 0; i < nameValue.length; i += 2)
			m.computeIfAbsent(nameValue[i], k -> new ArrayList<>()).add(nameValue[i + 1]);
		return m;
	}

	private static BoundaryResult hs(CsrfBoundary b, String scheme, String host, String...headers) {
		return b.checkHandshake(scheme, new HandshakeHosts(host == null ? List.of() : List.of(host), false), map(headers));
	}

	private static void hsAllowed(CsrfBoundary b, String scheme, String host, String...headers) {
		var x = hs(b, scheme, host, headers);
		assertTrue(x.isAllowed(), () -> "expected allowed, got: " + x);
	}

	private static void hsRefused(CsrfBoundary b, BoundaryReason reason, String scheme, String host, String...headers) {
		var x = hs(b, scheme, host, headers);
		assertFalse(x.isAllowed(), () -> "expected " + reason + ", got allowed");
		assertEquals(reason, x.reason(), x::toString);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTTP, X-Forwarded-*
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_httpForwardedAdmitted() {
		var b = xf().build();
		allowed(b, viaXf(b, PUBLIC, "app.example", "https"));
	}

	@Test void a02_replaceNotUnion() {
		var b = xf().build();
		refused(b, viaXf(b, "http://backend:8080", "app.example", "https"), ORIGIN_MISMATCH);
		refused(b, viaXf(b, "http://app.example", "app.example", "https"), ORIGIN_MISMATCH);
	}

	@Test void a03_missing() {
		var b = xf().build();
		refused(b, post(b, PUBLIC), FORWARDED_HOST_MISSING);
		refused(b, post(b, PUBLIC).h("X-Forwarded-Proto", "https"), FORWARDED_HOST_MISSING);
		refused(b, post(b, PUBLIC).h("X-Forwarded-Host", "app.example"), FORWARDED_PROTO_MISSING);
		refused(b, viaXf(b, PUBLIC, "", "https"), FORWARDED_HOST_MALFORMED);
		refused(b, viaXf(b, PUBLIC, "app.example", ""), FORWARDED_PROTO_MALFORMED);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"app.example,", "u@app.example", "https://app.example", "app.example/x", "app.example\\x", "app.example?x", "app.example#x",
		"app.example:0", "app.example:08080", "app.example:+80", "app.example:80a", "app.example:", "app.example:65536",
		"::1", "[fe80::1%25eth0]", "[::1", "b\u00fccher.example", "app example", "app.example\t x", "[v1.x]"
	})
	void a04_malformedHost(String host) {
		var b = xf().build();
		refused(b, viaXf(b, PUBLIC, host, host.endsWith(",") ? "https," : "https"), FORWARDED_HOST_MALFORMED);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ws", "wss", "ftp", "\"https\"", "HTTP/1.1", "https x" })
	void a05_malformedProto(String proto) {
		var b = xf().build();
		refused(b, viaXf(b, PUBLIC, "app.example", proto), FORWARDED_PROTO_MALFORMED);
	}

	@Test void a06_acceptedShapes() {
		var b = xf().build();
		allowed(b, viaXf(b, PUBLIC, "APP.Example", "HTTPS"));
		allowed(b, viaXf(b, "https://[::1]:8443", "[::1]:8443", "https"));
		allowed(b, viaXf(b, "https://app.example.", "app.example.", "https"));
		refused(b, viaXf(b, PUBLIC, "app.example.", "https"), ORIGIN_MISMATCH);
		allowed(b, viaXf(b, "https://xn--bcher-kva.example", "xn--bcher-kva.example", "https"));
	}

	@Test void a07_hops() {
		var b = xf().build();
		// Default n=1 takes the rightmost entry; the client's prefix is ignored.
		allowed(b, viaXf(b, PUBLIC, "evil.example, app.example", "http, https"));
		allowed(b, post(b, PUBLIC).h("X-Forwarded-Host", "evil.example").h("X-Forwarded-Host", "app.example")
			.h("X-Forwarded-Proto", "http, https"));
		// Alignment: proto is taken at the same index as host.
		allowed(b, viaXf(b, "https://b", "a, b", "http, https"));
		refused(b, viaXf(b, PUBLIC, "a, app.example", "https"), FORWARDED_HOPS_MISMATCH);

		var two = xf().trustedHops(2).build();
		allowed(two, viaXf(two, PUBLIC, "app.example, internal", "https, http"));
		refused(two, viaXf(two, PUBLIC, "app.example", "https"), FORWARDED_HOPS_INSUFFICIENT);
		// An overstated n selects the client's entry: the documented fail-open.
		allowed(two, viaXf(two, "http://evil.example", "evil.example, app.example", "http, https"));
		// The counted entries are all validated.
		refused(two, viaXf(two, PUBLIC, "app.example, ", "https, http"), FORWARDED_HOST_MALFORMED);
	}

	@Test void a08_otherFamilyNeverRead() {
		var b = xf().build();
		refused(b, post(b, PUBLIC).h("Forwarded", "host=app.example;proto=https"), FORWARDED_HOST_MISSING);
		var r = viaXf(b, "https://other.example", "app.example", "https").h("Forwarded", "host=other.example;proto=https");
		refused(b, r, ORIGIN_MISMATCH);
		var f = fwd().build();
		refused(f, post(f, PUBLIC).h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https"), FORWARDED_HOST_MISSING);
	}

	@Test void a09_portComesOnlyFromTheHostValueOrTheProtoDefault() {
		var b = xf().build();
		refused(b, viaXf(b, "https://app.example:8443", "app.example", "https"), ORIGIN_MISMATCH);
		allowed(b, viaXf(b, "https://app.example:8443", "app.example:8443", "https"));
		// X-Forwarded-Port is never read.
		refused(b, viaXf(b, "https://app.example:8443", "app.example", "https").h("X-Forwarded-Port", "8443"), ORIGIN_MISMATCH);
	}

	@Test void a10_allowedHostsAgainstTheForwardedHost() {
		var b = xf().allowedHosts("app.example").build();
		allowed(b, viaXf(b, PUBLIC, "app.example", "https"));
		var other = xf().allowedHosts("other.example").build();
		refused(other, viaXf(other, PUBLIC, "app.example", "https"), HOST_NOT_ALLOWED);
		// A safe GET with no forwarded header: allowed with no list, refused with one.
		var plain = xf().build();
		allowed(plain, new R().method("GET").h("Host", "backend:8080"));
		refused(b, new R().method("GET").h("Host", "backend:8080"), FORWARDED_HOST_MISSING);
	}

	@Test void a11_hostStillRequired() {
		var b = xf().build();
		var x = b.check(new R().method("POST").h("Origin", PUBLIC).h("X-Forwarded-Host", "app.example")
			.h("X-Forwarded-Proto", "https").build());
		assertEquals(HOST_MISSING, x.reason());
		assertEquals(400, x.status());
	}

	@Test void a12_offByDefaultIgnoresForwarded() {
		var b = CsrfBoundary.create().build();
		refused(b, viaXf(b, PUBLIC, "app.example", "https"), ORIGIN_MISMATCH);
		allowed(b, post(b, "http://backend:8080").h("X-Forwarded-Host", "evil.example"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HTTP, Forwarded
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_forwardedAdmitted() {
		var b = fwd().build();
		allowed(b, post(b, PUBLIC).h("Forwarded", "for=1.2.3.4;host=app.example;proto=https"));
		allowed(b, post(b, PUBLIC).h("Forwarded", "Host=\"app.example\"; PROTO = https"));
		allowed(b, post(b, "https://[::1]:8443").h("Forwarded", "host=\"[::1]:8443\";proto=https"));
		refused(b, post(b, PUBLIC).h("Forwarded", "host=app.example:8080;proto=https"), FORWARDED_HOST_MALFORMED);
	}

	@Test void b02_forwardedMissingAndMalformed() {
		var b = fwd().build();
		refused(b, post(b, PUBLIC), FORWARDED_HOST_MISSING);
		refused(b, post(b, PUBLIC).h("Forwarded", "for=1.2.3.4;proto=https"), FORWARDED_HOST_MISSING);
		refused(b, post(b, PUBLIC).h("Forwarded", "host=app.example"), FORWARDED_PROTO_MISSING);
		refused(b, post(b, PUBLIC).h("Forwarded", "host=app.example;proto=ws"), FORWARDED_PROTO_MALFORMED);
		refused(b, post(b, PUBLIC).h("Forwarded", "host=\"\";proto=https"), FORWARDED_HOST_MALFORMED);
		refused(b, post(b, PUBLIC).h("Forwarded", "host=a;host=app.example;proto=https"), FORWARDED_HOST_MALFORMED);
		refused(b, post(b, PUBLIC).h("Forwarded", "host=app.example;proto=https;proto=http"), FORWARDED_PROTO_MALFORMED);
		refused(b, post(b, PUBLIC).h("Forwarded", "host=app.example;proto=https,"), FORWARDED_HOST_MALFORMED);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"host=\"app.example;proto=https", "host=\"app\\.example\";proto=https", "host;proto=https", "host=;proto=https",
		"=app.example", "host=app.example proto=https", "host=\"a\"b;proto=https"
	})
	void b03_untokenizable(String value) {
		var b = fwd().build();
		refused(b, post(b, PUBLIC).h("Forwarded", value), FORWARDED_HOST_MALFORMED);
	}

	@Test void b04_wholeHeaderMustTokenize() {
		var b = fwd().build();
		// A client's line that does not tokenize refuses the request, even though the proxy's last line is fine.
		refused(b, post(b, PUBLIC).h("Forwarded", "for=\"x").h("Forwarded", "host=app.example;proto=https"), FORWARDED_HOST_MALFORMED);
	}

	@Test void b05_hopsAcrossLinesAndCommas() {
		var b = fwd().build();
		allowed(b, post(b, PUBLIC).h("Forwarded", "host=evil.example;proto=http")
			.h("Forwarded", "host=app.example;proto=https"));
		allowed(b, post(b, PUBLIC).h("Forwarded", "host=evil.example;proto=http, host=app.example;proto=https"));
		allowed(b, post(b, PUBLIC).h("Forwarded", "for=\"a,b\";host=evil.example;proto=http, host=app.example;proto=https"));

		var two = fwd().trustedHops(2).build();
		allowed(two, post(two, PUBLIC).h("Forwarded", "host=app.example;proto=https, host=internal;proto=http"));
		refused(two, post(two, PUBLIC).h("Forwarded", "host=app.example;proto=https"), FORWARDED_HOPS_INSUFFICIENT);
		// An incomplete counted element is never skipped, and no value is borrowed from a neighbour.
		refused(two, post(two, PUBLIC).h("Forwarded", "host=app.example;proto=https, for=10.0.0.1"), FORWARDED_HOST_MISSING);
		refused(two, post(two, PUBLIC).h("Forwarded", "for=10.0.0.1, host=app.example;proto=https"), FORWARDED_HOST_MISSING);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// The container's view
	//-----------------------------------------------------------------------------------------------------------------

	private static R container(CsrfBoundary b, String origin, String name, int port, String scheme) {
		return viaXf(b, origin, "app.example", "https").scheme(scheme).container(name, port);
	}

	@Test void c01_containerMustAgree() {
		var b = xf().originSource(OriginSource.CONTAINER).build();
		allowed(b, container(b, PUBLIC, "app.example", 443, "https"));
		allowed(b, container(b, PUBLIC, "APP.example", 443, "https"));
		refused(b, container(b, PUBLIC, "evil.example", 443, "https"), FORWARDED_CONTAINER_MISMATCH);
		// The connector port against the proto default is a mismatch, loud and fail-closed.
		refused(b, container(b, PUBLIC, "app.example", 8080, "https"), FORWARDED_CONTAINER_MISMATCH);
		// The container did no forwarding: its view is the transport.
		refused(b, container(b, PUBLIC, "backend", 8080, "http"), FORWARDED_CONTAINER_MISMATCH);
		refused(b, container(b, PUBLIC, "app.example", 443, "http"), FORWARDED_CONTAINER_MISMATCH);
		// An underivable container view is refused.
		refused(b, container(b, PUBLIC, "app.example:99", 443, "https"), FORWARDED_CONTAINER_MISMATCH);
		refused(b, container(b, PUBLIC, null, 443, "https"), FORWARDED_CONTAINER_MISMATCH);
	}

	@Test void c02_containerExplicitPort() {
		var b = xf().originSource(OriginSource.CONTAINER).build();
		var r = viaXf(b, "https://app.example:443", "app.example:443", "https").scheme("https").container("app.example", 443);
		allowed(b, r);
		var other = viaXf(b, "https://app.example:8443", "app.example:8443", "https").scheme("https").container("app.example", 8443);
		allowed(b, other);
	}

	@Test void c03_hostHeaderFingerprintLeftOfSelected() {
		var b = xf().build();
		// Host equals the client-writable left entry: the container trusted a hop it must not have.
		refused(b, new R().method("POST").scheme("http").h("Host", "left.example").h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "left.example, app.example").h("X-Forwarded-Proto", "http, https"),
			FORWARDED_CONTAINER_MISMATCH);
		// The transport name is replaced.
		allowed(b, viaXf(b, PUBLIC, "left.example, app.example", "http, https"));
		// A match to the right of the selected entry (an inner hop or the transport) is replaced, never refused.
		var two = xf().trustedHops(2).build();
		allowed(two, viaXf(two, PUBLIC, "app.example, backend", "https, http"));
		// Availability-only edge: a client can get its own request refused.
		refused(b, viaXf(b, PUBLIC, "backend, app.example", "http, https").remove("Host").h("Host", "backend"),
			FORWARDED_CONTAINER_MISMATCH);
		// Same host, different port: the TLS-terminating transport, replaced.
		allowed(b, post(b, PUBLIC).remove("Host").h("Host", "app.example:8080").h("X-Forwarded-Host", "app.example")
			.h("X-Forwarded-Proto", "https"));
	}

	@Test void c04_http2SynthesisedHostIsTheContainerView() {
		var b = xf().build();
		// HTTP/2: no Host header, the server name stands in; it is the container view and is replaced.
		var r = new R().method("POST").scheme("http").protocol("HTTP/2.0").container("backend", 8080).h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https");
		allowed(b, r);
		var left = new R().method("POST").scheme("http").protocol("HTTP/2.0").container("left.example", 8080).h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "left.example, app.example").h("X-Forwarded-Proto", "http, https");
		refused(b, left, FORWARDED_CONTAINER_MISMATCH);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Handshakes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_handshakeAdmitted() {
		var b = xf().build();
		hsAllowed(b, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
		hsAllowed(b, "ws", "backend:8080", "origin", PUBLIC, "x-forwarded-host", "app.example", "X-FORWARDED-PROTO", "https",
			"Sec-Fetch-Site", "same-origin");
		hsRefused(b, ORIGIN_MISMATCH, "ws", "backend:8080", "Origin", "http://backend:8080", "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https");
		hsRefused(b, FETCH_SITE_NOT_SAME_ORIGIN, "ws", "backend:8080", "Origin", PUBLIC, "Sec-Fetch-Site", "same-site",
			"X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
	}

	@Test void d02_handshakeMissingAndMalformed() {
		var b = xf().build();
		hsRefused(b, FORWARDED_HOST_MISSING, "ws", "backend:8080", "Origin", PUBLIC);
		hsRefused(b, FORWARDED_HOST_MISSING, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Proto", "https");
		hsRefused(b, FORWARDED_PROTO_MISSING, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example");
		hsRefused(b, FORWARDED_PROTO_MALFORMED, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "wss");
		hsRefused(b, FORWARDED_HOST_MALFORMED, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "a@app.example",
			"X-Forwarded-Proto", "https");
		hsRefused(b, HOST_MISSING, "ws", null, "Origin", PUBLIC, "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
	}

	@Test void d03_handshakeTwoSpellingsRefused() {
		var b = xf().build();
		hsRefused(b, FORWARDED_HOST_MALFORMED, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"x-forwarded-host", "app.example", "X-Forwarded-Proto", "https");
		hsRefused(b, FORWARDED_PROTO_MALFORMED, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https", "x-forwarded-proto", "https");
		// Origin is counted, not ordered: two spellings are two values, and a second value is a mismatch.
		hsRefused(b, ORIGIN_MISMATCH, "ws", "backend:8080", "Origin", PUBLIC, "origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https");
	}

	@Test void d04_listFormsAlwaysRefuseWhenTrusted() {
		var b = xf().build();
		var x = b.checkHandshake("ws", List.of("backend:8080"), List.of(PUBLIC), List.of());
		assertEquals(FORWARDED_HOST_MISSING, x.reason());
		var plain = CsrfBoundary.create().build();
		assertTrue(plain.checkHandshake("ws", List.of("app.example"), List.of("http://app.example"), List.of()).isAllowed());
	}

	@Test void d05_handshakeSchemeAndUnknownTransport() {
		var b = xf().build();
		hsAllowed(b, "wss", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
		// An unknown URI scheme is refused even with a well-formed forwarded origin.
		hsRefused(b, ORIGIN_MISMATCH, "ftp", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https");
	}

	@Test void d06_handshakeFingerprintAndHops() {
		var b = xf().build();
		hsRefused(b, FORWARDED_CONTAINER_MISMATCH, "ws", "left.example", "Origin", PUBLIC, "X-Forwarded-Host", "left.example, app.example",
			"X-Forwarded-Proto", "http, https");
		hsAllowed(b, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "left.example, app.example",
			"X-Forwarded-Proto", "http, https");
		// A real Jetty-customizer view: Host and scheme already rewritten, so they agree.
		hsAllowed(b, "wss", "app.example", "Origin", PUBLIC, "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
	}

	@Test void d07_handshakeForwardedFamilyAndAllowedHosts() {
		var b = fwd().allowedHosts("app.example").build();
		hsAllowed(b, "ws", "backend:8080", "Origin", PUBLIC, "Forwarded", "host=app.example;proto=https");
		hsRefused(b, FORWARDED_HOST_MISSING, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https");
		var other = fwd().allowedHosts("other.example").build();
		hsRefused(other, HOST_NOT_ALLOWED, "ws", "backend:8080", "Origin", PUBLIC, "Forwarded", "host=app.example;proto=https");
	}

	@Test void d08_handshakeOffIgnoresForwarded() {
		var b = CsrfBoundary.create().build();
		hsRefused(b, ORIGIN_MISMATCH, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https");
		hsAllowed(b, "ws", "backend:8080", "Origin", "http://backend:8080");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Review follow-ups
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_unenumerableHeadersAreMalformedNeverTheFirstLine() {
		var b = xf().build();
		var req = viaXf(b, "http://evil.example", "evil.example", "http").build();
		when(req.getHeaders("X-Forwarded-Host")).thenReturn(null);
		var x = b.check(req);
		assertFalse(x.isAllowed(), x::toString);
		assertEquals(FORWARDED_HOST_MALFORMED, x.reason());
		var req2 = viaXf(b, "http://evil.example", "evil.example", "http").build();
		when(req2.getHeaders("X-Forwarded-Proto")).thenReturn(null);
		assertEquals(FORWARDED_PROTO_MALFORMED, b.check(req2).reason());
		var f = fwd().build();
		var req3 = post(f, PUBLIC).h("Forwarded", "host=evil.example;proto=http").build();
		when(req3.getHeaders("Forwarded")).thenReturn(null);
		assertEquals(FORWARDED_HOST_MALFORMED, f.check(req3).reason());
		// Absent stays absent.
		refused(b, post(b, PUBLIC), FORWARDED_HOST_MISSING);
	}

	@Test void f02_hostsAndProtosOfDifferentCountsMismatchBeforeTheHopCount() {
		var two = xf().trustedHops(2).build();
		refused(two, viaXf(two, PUBLIC, "a, b", "https"), FORWARDED_HOPS_MISMATCH);
		refused(two, viaXf(two, PUBLIC, "a", "https, http"), FORWARDED_HOPS_MISMATCH);
		refused(two, viaXf(two, PUBLIC, "a", "https"), FORWARDED_HOPS_INSUFFICIENT);
		refused(two, viaXf(two, PUBLIC, "a, b, c", "https, http"), FORWARDED_HOPS_MISMATCH);
	}

	@Test void f03_unparseableLeftEntryIsPossiblyTheContainersHost() {
		var b = xf().build();
		// The container could normalize the quoted left entry into the Host it presents; refuse rather than guess.
		var r = new R().method("POST").scheme("http").h("Host", "left.example").h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "\"left.example\", app.example").h("X-Forwarded-Proto", "http, https");
		refused(b, r, FORWARDED_CONTAINER_MISMATCH);
		// Availability only: the client's own unparseable entry also refuses a request whose transport host is rewritten.
		refused(b, viaXf(b, PUBLIC, "\"left.example\", app.example", "http, https"), FORWARDED_CONTAINER_MISMATCH);
		var f = fwd().build();
		var r2 = new R().method("POST").scheme("http").h("Host", "evil.example").h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(f.csrfHeader(), f.tokenFor(new R().build()))
			.h("Forwarded", "host=\"user@evil.example\";proto=http, host=app.example;proto=https");
		refused(f, r2, FORWARDED_CONTAINER_MISMATCH);
		hsRefused(b, FORWARDED_CONTAINER_MISMATCH, "ws", "left.example", "Origin", PUBLIC, "X-Forwarded-Host", "\"left.example\", app.example",
			"X-Forwarded-Proto", "http, https");
	}

	@Test void f04_forwardedEdgeShapes() {
		var two = fwd().trustedHops(2).build();
		refused(two, post(two, PUBLIC).h("Forwarded", "host=app.example;proto=https,,host=internal;proto=http"), FORWARDED_HOST_MALFORMED);
		refused(two, post(two, PUBLIC).h("Forwarded", "host=app.example;proto=https").h("Forwarded", "")
			.h("Forwarded", "host=internal;proto=http"), FORWARDED_HOST_MALFORMED);
		var b = fwd().build();
		allowed(b, post(b, PUBLIC).h("Forwarded", "host=app.example;proto=\"https\""));
		allowed(b, post(b, PUBLIC).h("Forwarded", "host=\"app.example:443\";proto=https"));
		refused(b, post(b, PUBLIC).h("Forwarded", "host=\"app.example:444\";proto=https"), ORIGIN_MISMATCH);
		allowed(b, post(b, PUBLIC).h("Forwarded", "host=app.example;;proto=https"));
	}

	@Test void f05_xForwardedEdgeShapes() {
		var b = xf().build();
		allowed(b, post(b, PUBLIC).h("X-Forwarded-Host", "x").h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "http").h("X-Forwarded-Proto", "https"));
		refused(b, viaXf(b, PUBLIC, "app.example", "ht\ttp"), FORWARDED_PROTO_MALFORMED);
		var two = xf().trustedHops(2).build();
		refused(two, viaXf(two, PUBLIC, "app.example,,internal", "https, https, http"), FORWARDED_HOST_MALFORMED);
		refused(two, viaXf(two, PUBLIC, "app.example, internal", "https, ws"), FORWARDED_PROTO_MALFORMED);
	}

	@Test void f09_mixedCaseAndQuotedTab() {
		var b = xf().build();
		var r = post(b, PUBLIC).h("X-Forwarded-Host", "evil.example").h("x-forwarded-host", "app.example")
			.h("X-Forwarded-Proto", "http").h("x-FORWARDED-proto", "https");
		allowed(b, r);
		var f = fwd().build();
		refused(f, post(f, PUBLIC).h("Forwarded", "host=\"app.\texample\";proto=https"), FORWARDED_HOST_MALFORMED);
	}

	@Test void f06_safeMethodsAndAllowedHosts() {
		var open = xf().build();
		// No allowedHosts: a safe request is not judged, forwarded headers or not, even malformed ones.
		allowed(open, new R().method("GET").h("Host", "backend:8080"));
		allowed(open, new R().method("GET").h("Host", "backend:8080").h("X-Forwarded-Host", "\"").h("X-Forwarded-Proto", "ws"));
		var listed = xf().allowedHosts("app.example").build();
		refused(listed, new R().method("GET").h("Host", "backend:8080"), FORWARDED_HOST_MISSING);
		allowed(listed, new R().method("GET").h("Host", "backend:8080").h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https"));
	}

	@Test void f07_handshakeEdgeShapes() {
		var two = xf().trustedHops(2).build();
		hsAllowed(two, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example, internal", "X-Forwarded-Proto", "https, http");
		hsRefused(two, FORWARDED_HOPS_INSUFFICIENT, "ws", "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "https");
		var f = fwd().build();
		hsRefused(f, FORWARDED_HOST_MALFORMED, "ws", "backend:8080", "Origin", PUBLIC, "Forwarded", "host=\"app.example;proto=https");
		hsRefused(f, FORWARDED_HOST_MALFORMED, "ws", "backend:8080", "Origin", PUBLIC, "Forwarded", "host=app.example;proto=https",
			"forwarded", "host=app.example;proto=https");
		hsRefused(f, FORWARDED_HOST_MISSING, "ws", "backend:8080", "Origin", PUBLIC);
		var b = xf().build();
		var empty = b.checkHandshake("ws", new HandshakeHosts(List.of("backend:8080"), false), new LinkedHashMap<String,List<String>>());
		assertEquals(FORWARDED_HOST_MISSING, empty.reason());
	}

	@Test void f08_containerSourceOverHttp2() {
		var b = xf().originSource(OriginSource.CONTAINER).build();
		var r = new R().method("POST").scheme("https").protocol("HTTP/2.0").container("app.example", 443).h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https");
		allowed(b, r);
		var off = new R().method("POST").scheme("http").protocol("HTTP/2.0").container("backend", 8080).h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https");
		refused(b, off, FORWARDED_CONTAINER_MISMATCH);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_builder() {
		var bld = CsrfBoundary.create();
		var twoHops = CsrfBoundary.create().trustedHops(2);
		assertThrows(IllegalArgumentException.class, () -> bld.trustedHops(0));
		assertThrows(IllegalArgumentException.class, () -> bld.trustedHops(17));
		assertThrows(IllegalArgumentException.class, twoHops::build);
		assertThrows(IllegalArgumentException.class, () -> bld.trustedForwarding(null));
		var b = xf().trustedHops(16).build();
		assertEquals(ForwardedSource.X_FORWARDED, b.trustedForwarding());
		assertEquals(16, b.trustedHops());
		assertTrue(b.toString().contains("trustedForwarding=X_FORWARDED") && b.toString().contains("trustedHops=16"), b.toString());
		assertEquals(ForwardedSource.NONE, CsrfBoundary.create().build().trustedForwarding());
		assertEquals(1, CsrfBoundary.create().build().trustedHops());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Peer allow-list
	//-----------------------------------------------------------------------------------------------------------------

	private static final String TRUSTED = "10.1.2.3";
	private static final String UNTRUSTED = "203.0.113.7";

	private static CsrfBoundary.Builder tp(String...cidrs) {
		return xf().trustedProxies(cidrs);
	}

	private static R viaXfFrom(CsrfBoundary b, String peer, String origin, String host, String proto) {
		return viaXf(b, origin, host, proto).remote(peer);
	}

	private static InetAddress ip(String literal) {
		try {
			return InetAddress.getByName(literal);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/** Whether a forwarded write from {@code peer} is believed under the given ranges. */
	private static boolean trusts(String peer, String...ranges) {
		var b = tp(ranges).build();
		var x = b.check(viaXfFrom(b, peer, PUBLIC, "app.example", "https").build());
		if (! x.isAllowed())
			assertEquals(FORWARDED_PEER_NOT_TRUSTED, x.reason(), x::toString);
		return x.isAllowed();
	}

	@Test void g01_trustedPeerGetsPlainForwardedBehaviour() {
		var b = tp("10.0.0.0/8").build();
		allowed(b, viaXfFrom(b, TRUSTED, PUBLIC, "app.example", "https"));
		refused(b, post(b, PUBLIC).remote(TRUSTED), FORWARDED_HOST_MISSING);
		refused(b, post(b, PUBLIC).h("X-Forwarded-Host", "app.example").remote(TRUSTED), FORWARDED_PROTO_MISSING);
		refused(b, viaXfFrom(b, TRUSTED, PUBLIC, "", "https"), FORWARDED_HOST_MALFORMED);
		refused(b, viaXfFrom(b, TRUSTED, "http://evil.example", "app.example", "https"), ORIGIN_MISMATCH);
		var two = tp("10.0.0.0/8").trustedHops(2).build();
		refused(two, viaXfFrom(two, TRUSTED, PUBLIC, "app.example", "https"), FORWARDED_HOPS_INSUFFICIENT);
		var r = new R().method("POST").scheme("http").remote(TRUSTED).h("Host", "left.example").h("Origin", PUBLIC)
			.h("Content-Type", "application/json").h(b.csrfHeader(), b.tokenFor(new R().build()))
			.h("X-Forwarded-Host", "\"left.example\", app.example").h("X-Forwarded-Proto", "http, https");
		refused(b, r, FORWARDED_CONTAINER_MISMATCH);
	}

	@Test void g02_untrustedPeerWithHeaderRefused() {
		var b = tp("10.0.0.0/8").build();
		refused(b, viaXfFrom(b, UNTRUSTED, PUBLIC, "app.example", "https"), FORWARDED_PEER_NOT_TRUSTED);
		// The rebinding forge: a page on the attacker's name choosing its own origin.
		refused(b, viaXfFrom(b, UNTRUSTED, "http://evil.example", "evil.example", "http"), FORWARDED_PEER_NOT_TRUSTED);
		var listed = tp("10.0.0.0/8").allowedHosts("evil.example").build();
		refused(listed, viaXfFrom(listed, UNTRUSTED, "http://evil.example", "evil.example", "http"), FORWARDED_PEER_NOT_TRUSTED);
	}

	@Test void g03_untrustedPeerWithoutHeaderJudgedOnContainerView() {
		var b = tp("10.0.0.0/8").build();
		allowed(b, post(b, "http://backend:8080"));
		refused(b, post(b, PUBLIC), ORIGIN_MISMATCH);
		var listed = tp("10.0.0.0/8").allowedHosts("app.example").build();
		refused(listed, post(listed, "http://backend:8080"), HOST_NOT_ALLOWED);
		var backendListed = tp("10.0.0.0/8").allowedHosts("app.example", "backend").build();
		allowed(backendListed, post(backendListed, "http://backend:8080"));
	}

	@Test void g04_protoOnlyFromUntrustedPeerIsNotRefused() {
		var b = tp("10.0.0.0/8").build();
		allowed(b, post(b, "http://backend:8080").h("X-Forwarded-Proto", "https"));
		var f = fwd().trustedProxies("10.0.0.0/8").build();
		allowed(f, post(f, "http://backend:8080").h("Forwarded", "for=1.2.3.4;proto=https"));
	}

	@Test void g05_forwardedFamily() {
		var f = fwd().trustedProxies("10.0.0.0/8").build();
		refused(f, post(f, PUBLIC).h("Forwarded", "for=1.2.3.4;host=app.example"), FORWARDED_PEER_NOT_TRUSTED);
		// Another family is not the selected one.
		allowed(f, post(f, "http://backend:8080").h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https"));
		// A header the container will not enumerate counts as a claim.
		var req = post(f, PUBLIC).h("Forwarded", "host=app.example;proto=https").build();
		when(req.getHeaders("Forwarded")).thenReturn(null);
		assertEquals(FORWARDED_PEER_NOT_TRUSTED, f.check(req).reason());
		var b = tp("10.0.0.0/8").build();
		var req2 = post(b, PUBLIC).h("X-Forwarded-Host", "app.example").build();
		when(req2.getHeaders("X-Forwarded-Host")).thenReturn(null);
		assertEquals(FORWARDED_PEER_NOT_TRUSTED, b.check(req2).reason());
	}

	@Test void g06_safeMethods() {
		var open = tp("10.0.0.0/8").build();
		allowed(open, new R().method("GET").h("Host", "backend:8080").h("X-Forwarded-Host", "evil.example").remote(UNTRUSTED));
		var listed = tp("10.0.0.0/8").allowedHosts("app.example").build();
		refused(listed, new R().method("GET").h("Host", "backend:8080").h("X-Forwarded-Host", "app.example")
			.h("X-Forwarded-Proto", "https").remote(UNTRUSTED), FORWARDED_PEER_NOT_TRUSTED);
		refused(listed, new R().method("GET").h("Host", "backend:8080").remote(UNTRUSTED), HOST_NOT_ALLOWED);
		allowed(listed, new R().method("GET").h("Host", "backend:8080").h("X-Forwarded-Host", "app.example")
			.h("X-Forwarded-Proto", "https").remote(TRUSTED));
	}

	@ParameterizedTest
	@ValueSource(strings = { "10.1.2.3", "[::1]", "0:0:0:0:0:0:0:1", "::1", "fe80::1%eth0", "fe80::1%2", "::ffff:10.1.2.3", "::ffff:127.0.0.1" })
	void g07a_peerTrusted(String peer) {
		assertTrue(trusts(peer, "10.0.0.0/8", "::1/128", "fe80::/10", "127.0.0.0/8"), peer);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"::10.1.2.3", "64:ff9b::a01:203", "", "localhost", "proxy.internal", "/tmp/jetty.sock", "unknown", "10.1", "010.1.2.3",
		"10.1.2.3.", "[10.1.2.3]x", "fe80::1%", "::1%eth0%1", ".::1", ".:"
	})
	void g07b_peerUntrusted(String peer) {
		// "localhost" would be trusted if it were resolved.
		assertFalse(trusts(peer, "10.0.0.0/8", "::1/128", "fe80::/10", "127.0.0.0/8"), peer);
	}

	@Test void g07c_nullPeerUntrusted() {
		assertFalse(trusts(null, "0.0.0.0/0", "::/0"));
	}

	@Test void g08_prefixBoundaries() {
		assertTrue(trusts("172.16.0.0", "172.16.0.0/12"));
		assertTrue(trusts("172.31.255.255", "172.16.0.0/12"));
		assertFalse(trusts("172.32.0.0", "172.16.0.0/12"));
		assertFalse(trusts("172.15.255.255", "172.16.0.0/12"));
		assertTrue(trusts("2001:db8::", "2001:db8::/127"));
		assertTrue(trusts("2001:db8::1", "2001:db8::/127"));
		assertFalse(trusts("2001:db8::2", "2001:db8::/127"));
		assertTrue(trusts("10.0.0.1", "10.0.0.1"));
		assertFalse(trusts("10.0.0.2", "10.0.0.1"));
		assertFalse(trusts("::a01:203", "10.0.0.0/8"));
		assertFalse(trusts("10.1.2.3", "::/0"));
	}

	private static BoundaryResult hsFrom(CsrfBoundary b, InetAddress peer, String scheme, String host, String...headers) {
		return b.checkHandshake(scheme, new HandshakeHosts(host == null ? List.of() : List.of(host), false), map(headers), peer);
	}

	private static void hsFromRefused(CsrfBoundary b, BoundaryReason reason, InetAddress peer, String host, String...headers) {
		var x = hsFrom(b, peer, "ws", host, headers);
		assertFalse(x.isAllowed(), () -> "expected " + reason + ", got allowed");
		assertEquals(reason, x.reason(), x::toString);
	}

	@Test void g09_handshake() throws Exception {
		var b = tp("10.0.0.0/8").build();
		var trusted = ip(TRUSTED);
		var untrusted = ip(UNTRUSTED);
		var fwdHeaders = new String[] { "Origin", PUBLIC, "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https" };
		assertTrue(hsFrom(b, trusted, "ws", "backend:8080", fwdHeaders).isAllowed());
		assertEquals(FORWARDED_HOST_MISSING, hsFrom(b, trusted, "ws", "backend:8080", "Origin", PUBLIC).reason());
		hsFromRefused(b, FORWARDED_PEER_NOT_TRUSTED, untrusted, "backend:8080", fwdHeaders);
		hsFromRefused(b, FORWARDED_PEER_NOT_TRUSTED, untrusted, "backend:8080", "Origin", "http://evil.example",
			"X-Forwarded-Host", "evil.example", "X-Forwarded-Proto", "http");
		// Without the header: the container view.
		assertTrue(hsFrom(b, untrusted, "ws", "backend:8080", "Origin", "http://backend:8080").isAllowed());
		hsFromRefused(b, ORIGIN_MISMATCH, untrusted, "backend:8080", "Origin", PUBLIC);
		// An unknown peer is untrusted.
		hsFromRefused(b, FORWARDED_PEER_NOT_TRUSTED, null, "backend:8080", fwdHeaders);
		assertTrue(hsFrom(b, null, "ws", "backend:8080", "Origin", "http://backend:8080").isAllowed());
		// The three-argument form has no peer.
		var legacy = b.checkHandshake("ws", new HandshakeHosts(List.of("backend:8080"), false), map(fwdHeaders));
		assertEquals(FORWARDED_PEER_NOT_TRUSTED, legacy.reason());
		// Safe-method style allowedHosts applies to the container view.
		var listed = tp("10.0.0.0/8").allowedHosts("app.example").build();
		hsFromRefused(listed, HOST_NOT_ALLOWED, untrusted, "backend:8080", "Origin", "http://backend:8080");
		// A mapped Inet6Address (which getByName would have collapsed) is unwrapped.
		var mapped = Inet6Address.getByAddress(null, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte)0xff, (byte)0xff, 10, 1, 2, 3 }, -1);
		assertTrue(hsFrom(b, mapped, "ws", "backend:8080", fwdHeaders).isAllowed());
		// Presence rows.
		hsFromRefused(b, FORWARDED_PEER_NOT_TRUSTED, untrusted, "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "a",
			"x-forwarded-host", "b");
		hsFromRefused(b, FORWARDED_PEER_NOT_TRUSTED, untrusted, "backend:8080", "Origin", PUBLIC, "X-Forwarded-Host", "");
		// Host is parsed first.
		hsFromRefused(b, HOST_MALFORMED, untrusted, "bad host", fwdHeaders);
		// The list overloads refuse before any peer logic.
		var list = b.checkHandshake("ws", new HandshakeHosts(List.of("backend:8080"), false), List.of(PUBLIC), List.of());
		assertEquals(FORWARDED_HOST_MISSING, list.reason());
		// Off: the peer is never read.
		var plain = xf().build();
		assertTrue(hsFrom(plain, untrusted, "ws", "backend:8080", fwdHeaders).isAllowed());
	}

	@Test void g11_presenceAndOrder() {
		var b = tp("10.0.0.0/8").build();
		refused(b, post(b, PUBLIC).h("X-Forwarded-Host", ""), FORWARDED_PEER_NOT_TRUSTED);
		allowed(b, post(b, "http://backend:8080").h("Forwarded", "host=evil"));
		var f = fwd().trustedProxies("10.0.0.0/8").build();
		allowed(f, post(f, "http://backend:8080").h("Forwarded", "for=1.2.3.4;proto=https"));
		refused(f, post(f, PUBLIC).h("Forwarded", "for=1.2.3.4;host=app.example"), FORWARDED_PEER_NOT_TRUSTED);
		refused(f, post(f, PUBLIC).h("Forwarded", "HOST=app.example"), FORWARDED_PEER_NOT_TRUSTED);
		refused(f, post(f, PUBLIC).h("Forwarded", "host="), FORWARDED_PEER_NOT_TRUSTED);
		refused(f, post(f, PUBLIC).h("Forwarded", "host=\"\""), FORWARDED_PEER_NOT_TRUSTED);
		refused(f, post(f, PUBLIC).h("Forwarded", ";;="), FORWARDED_PEER_NOT_TRUSTED);
		refused(f, post(f, PUBLIC).h("Forwarded", "for=a").h("Forwarded", "host=x"), FORWARDED_PEER_NOT_TRUSTED);
		// The peer comes before the Host parse.
		var bad = new R().method("POST").h("Host", "bad host").h("Origin", PUBLIC).h("X-Forwarded-Host", "app.example");
		refused(b, bad, FORWARDED_PEER_NOT_TRUSTED);
		var bad2 = new R().method("POST").h("Host", "bad host").h("Origin", PUBLIC);
		var x = b.check(bad2.build());
		assertEquals(HOST_MALFORMED, x.reason());
		assertEquals(400, x.status());
	}

	@Test void g10a_builderRefusesBadEntries() {
		var proxies = CsrfBoundary.create();
		for (var bad : new String[] {
			"10.1", "010.0.0.1", "10.0.0.256", "10.0.0.1.", "[::1]", "fe80::1%eth0", "::ffff:10.0.0.0/104", "::ffff:10.0.0.1",
			"10.0.0.0/33", "::/129", "10.0.0.0/+8", "10.0.0.0/-1", "10.0.0.0/08", "10.0.0.0/a", "10.0.0.0/", "10.0.0.0/8/8",
			"localhost", "proxy.example", "", "  ", "g::1", "1:2:3:4:5:6:7:8:9", ".::1/128", ".:"
		})
			assertThrows(IllegalArgumentException.class, () -> proxies.trustedProxies(bad), bad);
		assertThrows(IllegalArgumentException.class, proxies::trustedProxies);
		assertThrows(IllegalArgumentException.class, () -> proxies.trustedProxies(new String[0]));
		assertThrows(IllegalArgumentException.class, () -> proxies.trustedProxies((String)null));
		assertThrows(IllegalArgumentException.class, () -> proxies.trustedProxies((String[])null));
		assertThrows(IllegalArgumentException.class, () -> proxies.trustedProxies("10.0.0.0/8", null));
	}

	@Test void g10b_builderAcceptsMaskedAndNormalized() {
		// Accepted, masked and normalized.
		assertEquals(List.of("10.0.0.0/8"), tp("10.0.0.1/8").build().trustedProxies());
		assertEquals(List.of("192.168.1.0/24"), tp("192.168.1.1/24", "192.168.1.0/24").build().trustedProxies());
		assertEquals(List.of("10.0.0.1/32"), tp("10.0.0.1", "10.0.0.1/32").build().trustedProxies());
		assertEquals(List.of("2001:db8:0:0:0:0:0:0/32"), tp("2001:db8::1/32").build().trustedProxies());
		assertEquals(List.of("2001:db8:0:0:0:0:0:0/32"), tp("2001:db8::/32").build().trustedProxies());
		assertEquals(List.of("0:0:0:0:0:0:0:1/128"), tp("::1").build().trustedProxies());
		assertEquals(List.of("10.0.0.0/8", "fd00:0:0:0:0:0:0:0/8"), tp(" 10.0.0.0/8 ", "fd00::/8").build().trustedProxies());
		assertEquals(List.of("172.16.0.0/12"), tp("172.31.1.1/12").build().trustedProxies());
		assertEquals(List.of("0:0:0:0:0:0:0:0/0"), tp("::/0").build().trustedProxies());
		// The last call replaces the earlier ones.
		assertEquals(List.of("10.0.0.0/8"), xf().trustedProxies("192.168.0.0/16").trustedProxies("10.0.0.0/8").build().trustedProxies());
		// Masked ranges match.
		assertTrue(trusts("10.255.255.255", "10.0.0.1/8"));
		// /0 is per family.
		assertTrue(trusts("203.0.113.7", "0.0.0.0/0"));
		assertTrue(trusts("::ffff:203.0.113.7", "0.0.0.0/0"));
		assertFalse(trusts("2001:db8::1", "0.0.0.0/0"));
		assertTrue(trusts("2001:db8::1", "::/0"));
		assertFalse(trusts("203.0.113.7", "::/0"));
	}

	@Test void g10c_builderCrossSettingChecks() {
		// Cross-setting checks.
		var plain = CsrfBoundary.create().trustedProxies("10.0.0.0/8");
		var xfContainer = xf().originSource(OriginSource.CONTAINER).trustedProxies("10.0.0.0/8");
		var fwdContainer = fwd().originSource(OriginSource.CONTAINER).trustedProxies("10.0.0.0/8");
		assertThrows(IllegalArgumentException.class, plain::build);
		assertThrows(IllegalArgumentException.class, xfContainer::build);
		assertThrows(IllegalArgumentException.class, fwdContainer::build);
		assertTrue(CsrfBoundary.create().build().trustedProxies().isEmpty());
		assertTrue(tp("10.0.0.0/8").build().toString().contains("trustedProxies=[10.0.0.0/8]"));
		var trusted = tp("10.0.0.0/8").build().trustedProxies();
		assertThrows(UnsupportedOperationException.class, () -> trusted.add("x"));

		// Not set means every peer is believed, exactly as before.
		var off = xf().build();
		allowed(off, viaXfFrom(off, UNTRUSTED, PUBLIC, "app.example", "https"));
		allowed(off, viaXfFrom(off, null, PUBLIC, "app.example", "https"));
	}

	/** A forwarded refusal logs the forwarded values at FINEST only, without line breaks and capped in length. */
	@Test void l01_forwardedRefusalLogsValuesAtFinestOnly() {
		var b = xf().build();
		var log = CsrfBoundary_Test.captureLog(() -> hsRefused(b, FORWARDED_HOST_MALFORMED, "ws", "app.example",
			"Origin", PUBLIC, "X-Forwarded-Host", "bad host\r\nforged-line" + "z".repeat(500), "X-Forwarded-Proto", "https"));
		var finest = log.stream().filter(x -> x.getLevel() == java.util.logging.Level.FINEST).map(CsrfBoundary_Test::text).toList();
		assertEquals(1, finest.size(), log::toString);
		var m = finest.get(0);
		assertTrue(m.contains("X-Forwarded-Host=bad hostforged-line"), m);
		assertTrue(m.contains("X-Forwarded-Proto=https"), m);
		assertTrue(m.chars().noneMatch(c -> c < 0x20 || c == 0x7f), m);
		assertFalse(m.contains("z".repeat(201)), m);
		for (var x : log)
			if (x.getLevel() != java.util.logging.Level.FINEST)
				assertFalse(CsrfBoundary_Test.text(x).contains("forged-line"), CsrfBoundary_Test.text(x));
	}
}
