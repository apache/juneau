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
package org.apache.juneau.rest.auth.oauth.oidc;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Builder tests for {@link OidcDiscoveryClient}, focused on the issuer's HTTPS-or-loopback requirement.
 *
 * @since 10.0.0
 */
class OidcDiscoveryClient_Builder_Test extends TestBase {

	@Test void a01_requireIssuer() {
		assertThrows(IllegalStateException.class, () -> OidcDiscoveryClient.create().build());
	}

	@Test void a02_build_httpsAccepted() {
		var c = OidcDiscoveryClient.create()
			.issuer(URI.create("https://idp.example.com"))
			.build();
		assertEquals(URI.create("https://idp.example.com"), c.getIssuer());
	}

	@Test void a03_build_loopbackHttpAccepted() {
		var c1 = OidcDiscoveryClient.create().issuer(URI.create("http://localhost:8080")).build();
		assertEquals(URI.create("http://localhost:8080"), c1.getIssuer());

		var c2 = OidcDiscoveryClient.create().issuer(URI.create("http://127.0.0.1:8080")).build();
		assertEquals(URI.create("http://127.0.0.1:8080"), c2.getIssuer());

		var c3 = OidcDiscoveryClient.create().issuer(URI.create("http://[::1]:8080")).build();
		assertEquals(URI.create("http://[::1]:8080"), c3.getIssuer());
	}

	@Test void a04_build_remotePlaintextRejected() {
		assertThrowsWithMessage(IllegalArgumentException.class,
			"URI must use https or target a loopback host: http://idp.example.com",
			() -> OidcDiscoveryClient.create().issuer(URI.create("http://idp.example.com")).build());
	}
}
