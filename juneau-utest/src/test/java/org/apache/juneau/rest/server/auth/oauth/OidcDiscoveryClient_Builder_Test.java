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
package org.apache.juneau.rest.server.auth.oauth;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.oauth.oidc.*;
import org.junit.jupiter.api.*;

/**
 * Builder tests for {@link OidcDiscoveryClient} and the {@link OidcMetadata} record's defensive copies.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class OidcDiscoveryClient_Builder_Test extends TestBase {

	@Test void a01_requireIssuer() {
		assertThrows(IllegalStateException.class, () -> OidcDiscoveryClient.create().build());
	}

	@Test void a02_build_happyPath() {
		var c = OidcDiscoveryClient.create()
			.issuer(URI.create("https://idp.example.com"))
			.build();
		assertEquals(URI.create("https://idp.example.com"), c.getIssuer());
	}

	@Test void b01_metadata_record_defensiveCopy() {
		var supportedScopes = new HashSet<>(Set.of("openid", "profile"));
		var extras = new HashMap<String,Object>();
		extras.put("foo", "bar");
		var md = new OidcMetadata(
			URI.create("https://idp.example.com"),
			URI.create("https://idp.example.com/token"),
			null, null, null, null, null,
			supportedScopes, extras);
		assertEquals(Set.of("openid", "profile"), md.supportedScopes());
		assertEquals(Map.of("foo", "bar"), md.extras());
		assertThrows(UnsupportedOperationException.class, () -> md.supportedScopes().add("z"));
		assertThrows(UnsupportedOperationException.class, () -> md.extras().put("a", "b"));
	}
}
