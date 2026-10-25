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

// region issuer
import java.time.*;
import java.util.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;

/**
 * Stands in for your identity provider: owns an RSA key pair and signs short-lived JWTs with it.
 */
public class TokenIssuer {

	private final RSAKey key;

	public TokenIssuer() throws JOSEException {
		key = new RSAKeyGenerator(2048).keyID("demo-key").algorithm(JWSAlgorithm.RS256).generate();
	}

	/** The public half, as a JWKS document would publish it. */
	public JWKSet publicKeys() {
		return new JWKSet(key.toPublicJWK());
	}

	public String issue(String subject, String scope, String audience) throws JOSEException {
		var now = Instant.now();
		var claims = new JWTClaimsSet.Builder()
			.issuer(SecureValidators.ISSUER)
			.audience(audience)
			.subject(subject)
			.claim("scope", scope)
			.issueTime(Date.from(now))
			.notBeforeTime(Date.from(now))
			.expirationTime(Date.from(now.plus(Duration.ofMinutes(5))))
			.build();
		var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
		jwt.sign(new RSASSASigner(key));
		return jwt.serialize();
	}
}
// endregion issuer
