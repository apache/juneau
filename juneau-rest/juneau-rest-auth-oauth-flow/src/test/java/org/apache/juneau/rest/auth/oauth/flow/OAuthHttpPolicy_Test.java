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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.nimbusds.oauth2.sdk.http.*;

/**
 * Tests for {@link OAuthHttpPolicy}.
 */
class OAuthHttpPolicy_Test extends TestBase {

	private static final OAuthHttpPolicy POLICY = OAuthHttpPolicy.DEFAULT;

	private static HTTPRequest get(String url) throws Exception {
		return new HTTPRequest(HTTPRequest.Method.GET, new URL(url));
	}

	@Test void a01_httpsPublicHostAccepted() {
		assertDoesNotThrow(() -> POLICY.requireAllowed(URI.create("https://idp.example.com/token")));
	}

	@Test void a02_plainHttpRefusedForRemoteHost() {
		var uri = URI.create("http://idp.example.com/token");
		assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(uri));
	}

	@Test void a03_loopbackExemptOverPlainHttp() {
		assertDoesNotThrow(() -> POLICY.requireAllowed(URI.create("http://127.0.0.1:8080/token")));
		assertDoesNotThrow(() -> POLICY.requireAllowed(URI.create("http://localhost:8080/token")));
	}

	@Test void a04_privateAndMetadataLiteralsRefused() {
		var privateUri = URI.create("https://10.0.0.5/token");
		var metadataUri = URI.create("https://169.254.169.254/token");
		assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(privateUri));
		assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(metadataUri));
	}

	@Test void a05_nonCanonicalNumericHostsRefused() {
		for (var h : new String[]{"127.1", "2130706433", "0x7f000001", "0177.0.0.1", "0"}) {
			var uri = URI.create("https://" + h + "/token");
			assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(uri), h);
		}
	}

	@Test void a06_trailingDotAndLocalhostSubdomainRefused() {
		var trailingDot = URI.create("https://idp.example.com./token");
		var subdomain = URI.create("https://foo.localhost/token");
		assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(trailingDot));
		assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(subdomain));
	}

	@Test void a07_userInfoRefused() {
		var uri = URI.create("https://u:p@idp.example.com/token");
		assertThrows(IllegalArgumentException.class, () -> POLICY.requireAllowed(uri));
	}

	@Test void a08_insecureHttpOptIn() {
		var p = OAuthHttpPolicy.of(false, true);
		var uri = URI.create("http://idp.example.com/token");
		assertDoesNotThrow(() -> p.requireAllowed(uri));
	}

	@Test void a09_privateOptInAllowsPrivateHttpsAndLoopbackHttp() {
		var p = OAuthHttpPolicy.of(true, false);
		var privateHttps = URI.create("https://10.0.0.5/token");
		var loopbackHttp = URI.create("http://127.0.0.1:1/token");
		var privateHttp = URI.create("http://10.0.0.5/token");
		assertDoesNotThrow(() -> p.requireAllowed(privateHttps));
		assertDoesNotThrow(() -> p.requireAllowed(loopbackHttp));
		assertThrows(IllegalArgumentException.class, () -> p.requireAllowed(privateHttp));
	}

	@Test void b01_applyDisablesRedirects() throws Exception {
		var http = get("http://127.0.0.1:1/token");
		http.setFollowRedirects(true);
		POLICY.apply(http);
		assertFalse(http.getFollowRedirects());
	}

	@Test void b02_applyRefusesNameResolvingToPrivateAddress() throws Exception {
		var p = POLICY.withResolver(h -> new InetAddress[]{InetAddress.getByAddress(new byte[]{10, 0, 0, 5})});
		var http = get("https://idp.example.com/token");
		assertThrows(RuntimeException.class, () -> p.apply(http));
	}

	@Test void b03_applyAcceptsNameResolvingToPublicAddress() throws Exception {
		var p = POLICY.withResolver(h -> new InetAddress[]{InetAddress.getByAddress(new byte[]{93, (byte)184, (byte)216, 34})});
		var http = get("https://idp.example.com/token");
		p.apply(http);
		assertNotNull(http.getSSLSocketFactory());
	}

	@Test void b04_wrapRunsCallerThenPolicy() throws Exception {
		var seen = new boolean[1];
		var http = get("http://127.0.0.1:1/token");
		http.setFollowRedirects(true);
		POLICY.wrap(h -> seen[0] = true).accept(http);
		assertTrue(seen[0]);
		assertFalse(http.getFollowRedirects());
	}

	@Test void b05_wrapRefusesUnpermittedUrlAfterCallerHook() throws Exception {
		var http = get("http://idp.example.com/token");
		var wrapped = POLICY.wrap(h -> {});
		assertThrows(IllegalArgumentException.class, () -> wrapped.accept(http));
	}
}
