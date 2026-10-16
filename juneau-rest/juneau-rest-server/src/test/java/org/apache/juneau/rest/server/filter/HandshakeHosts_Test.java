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

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HandshakeHosts}.
 *
 * @since 10.0.0
 */
class HandshakeHosts_Test extends TestBase {

	private static final URI URI_ = URI.create("http://app.example/bus/ws/abc");

	private static Map<String,List<String>> h(String...pairs) {
		var m = new LinkedHashMap<String,List<String>>();
		for (var i = 0; i < pairs.length; i += 2)
			m.computeIfAbsent(pairs[i], k -> new ArrayList<>()).add(pairs[i + 1]);
		return m;
	}

	@Test void a01_hostSpellingsAreMerged() {
		var x = HandshakeHosts.of(h("Host", "a.example", "host", "b.example", "HOST", "c.example"), URI_);
		assertEquals(List.of("a.example", "b.example", "c.example"), x.values());
		assertFalse(x.synthesised());
	}

	@Test void a02_realHostWinsOverTheUri() {
		var x = HandshakeHosts.of(h("Host", "other.example"), URI_);
		assertEquals(List.of("other.example"), x.values());
		assertFalse(x.synthesised());
	}

	@Test void a03_noHostSynthesisesFromTheUri() {
		var x = HandshakeHosts.of(h("origin", "http://app.example"), URI_);
		assertEquals(List.of("app.example"), x.values());
		assertTrue(x.synthesised());
	}

	@Test void a04_secWebSocketKeyMeansRfc6455() {
		assertTrue(HandshakeHosts.of(h("Sec-WebSocket-Key", "k"), URI_).values().isEmpty());
		assertTrue(HandshakeHosts.of(h("sec-websocket-key", "k"), URI_).values().isEmpty());
		// Presence is what counts: a blank value or an empty list is still the key.
		assertTrue(HandshakeHosts.of(h("Sec-WebSocket-Key", ""), URI_).values().isEmpty());
		var empty = new LinkedHashMap<String,List<String>>();
		empty.put("sec-websocket-key", List.of());
		assertTrue(HandshakeHosts.of(empty, URI_).values().isEmpty());
		assertFalse(HandshakeHosts.of(h("Sec-WebSocket-Key", "k"), URI_).synthesised());
	}

	@Test void a05_upgradeMeansRfc6455() {
		assertTrue(HandshakeHosts.of(h("Upgrade", "websocket"), URI_).values().isEmpty());
		assertTrue(HandshakeHosts.of(h("upgrade", "websocket"), URI_).values().isEmpty());
		assertTrue(HandshakeHosts.of(h("UPGRADE", ""), URI_).values().isEmpty());
	}

	@Test void a06_aRealHostIsStillReturnedAlongsideTheKey() {
		var x = HandshakeHosts.of(h("Host", "app.example", "Sec-WebSocket-Key", "k"), URI_);
		assertEquals(List.of("app.example"), x.values());
		assertFalse(x.synthesised());
	}

	@Test void a07_nullHeadersAreEmpty() {
		var x = HandshakeHosts.of(null, URI_);
		assertEquals(List.of("app.example"), x.values());
		assertTrue(x.synthesised());
	}

	@Test void a08_userinfoIsNeverCarried() {
		assertEquals(List.of("app.example"), HandshakeHosts.of(h(), URI.create("http://u@app.example/x")).values());
		assertEquals(List.of("app.example:81"), HandshakeHosts.of(h(), URI.create("http://u:p@app.example:81/x")).values());
	}

	@Test void a09_nullUriOrNoHost() {
		assertTrue(HandshakeHosts.of(h(), null).values().isEmpty());
		assertTrue(HandshakeHosts.of(h(), URI.create("/bus/ws/abc")).values().isEmpty());
		assertTrue(HandshakeHosts.of(h(), URI.create("ws:///bus")).values().isEmpty());
		assertFalse(HandshakeHosts.of(h(), null).synthesised());
	}

	@Test void a10_ipv6KeepsItsBrackets() {
		assertEquals(List.of("[::1]:8080"), HandshakeHosts.of(h(), URI.create("ws://[::1]:8080/x")).values());
		assertEquals(List.of("[::1]"), HandshakeHosts.of(h(), URI.create("ws://[::1]/x")).values());
	}

	@Test void a11_anExplicitPortIsKeptAndNoneIsInvented() {
		assertEquals(List.of("app.example:8443"), HandshakeHosts.of(h(), URI.create("https://app.example:8443/x")).values());
		assertEquals(List.of("app.example"), HandshakeHosts.of(h(), URI.create("https://app.example/x")).values());
	}

	@Test void a12_valuesAreCopied() {
		var l = new ArrayList<>(List.of("a"));
		var x = new HandshakeHosts(l, false);
		l.add("b");
		assertEquals(List.of("a"), x.values());
		assertThrows(IllegalArgumentException.class, () -> new HandshakeHosts(null, false));
	}
}
