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
package org.apache.juneau.examples.guides.secureanendpoint;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.examples.guides.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.jwk.source.*;

class SecureAnEndpoint_Test extends TestBase {

	@Test void a01_runOutputMatchesGuide() throws Exception {
		assertEquals(GuideRegion.read("SecureAnEndpoint/run-output.txt", "output"), GuideRegion.normalize(SecureDemo.run()));
	}

	@Test void a02_tokenFromAnotherKeyIsRejected() throws Exception {
		var trusted = new TokenIssuer();
		var stranger = new TokenIssuer();
		var validator = SecureValidators.jwt(new ImmutableJWKSet<>(trusted.publicKeys()));
		try (var client = MockRestClient.buildLax(new SecureResource(validator))) {
			var token = stranger.issue("mallory", "orders:read", SecureValidators.AUDIENCE);
			assertEquals(401, client.get("/orders").header("Authorization", "Bearer " + token).run().getStatusCode());
		}
	}
}
