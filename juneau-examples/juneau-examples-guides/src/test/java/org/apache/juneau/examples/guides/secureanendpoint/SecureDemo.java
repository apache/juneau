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

// region demo
import java.util.*;

import org.apache.juneau.rest.mock.classic.*;

import com.nimbusds.jose.jwk.source.*;

/**
 * Calls the secured endpoint in-process with no token, good tokens and bad tokens.
 */
public class SecureDemo {

	public static void main(String[] args) throws Exception {
		System.out.println(run());
	}

	public static String run() throws Exception {
		var issuer = new TokenIssuer();
		try (var authServer = new TokenIntrospectionStub()) {
			var validator = SecureValidators.either(
				SecureValidators.jwt(new ImmutableJWKSet<>(issuer.publicKeys())),
				SecureValidators.introspection(authServer.endpoint(), "orders-api", "change-me"));

			try (var client = MockRestClient.buildLax(new SecureResource(validator))) {
				var lines = new ArrayList<String>();

				var res = client.get("/orders").run();
				lines.add("no token:        " + res.getStatusCode() + " " + res.getHeader("WWW-Authenticate").asString().orElse(""));

				var good = issuer.issue("alice", "orders:read", SecureValidators.AUDIENCE);
				lines.add("valid JWT:       " + call(client, good));

				var otherApi = issuer.issue("alice", "orders:read", "https://other.example.com");
				lines.add("wrong audience:  " + call(client, otherApi));

				var noScope = issuer.issue("bob", "profile", SecureValidators.AUDIENCE);
				lines.add("missing scope:   " + call(client, noScope));

				lines.add("opaque token:    " + call(client, "opaque-good"));
				lines.add("revoked token:   " + call(client, "opaque-revoked"));
				return String.join("\n", lines);
			}
		}
	}

	private static String call(MockRestClient client, String token) throws Exception {
		var res = client.get("/orders").header("Authorization", "Bearer " + token).run();
		return res.getStatusCode() == 200 ? "200 " + res.getContent().asString() : String.valueOf(res.getStatusCode());
	}
}
// endregion demo
