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
package org.apache.juneau.rest.auth.jwt;

import java.text.*;
import java.time.*;
import java.util.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;

/**
 * Static helpers for {@code JwtTokenValidator_*_Test} files. Generates RSA/EC/HMAC keys,
 * signs JWTs, and wraps a fixed {@link JWKSet} as a {@link JWKSource}.
 *
 * <p>
 * Lives in the test tree so production code never sees these helpers; lives in {@code juneau-utest}
 * rather than {@code juneau-rest-server-jwt/src/test} to match the centralized-tests convention
 * (every other juneau-rest module has empty {@code src/test/}; tests live here).
 */
final class JwtTestSupport {

	static final String DEFAULT_ISSUER = "https://issuer.example.com/";
	static final String DEFAULT_AUDIENCE = "https://api.example.com";

	private JwtTestSupport() {}

	/** Generate a fresh RSA-2048 JWK with the given kid. */
	static RSAKey generateRsa(String kid) throws JOSEException {
		return new RSAKeyGenerator(2048)
			.keyID(kid)
			.algorithm(JWSAlgorithm.RS256)
			.generate();
	}

	/** Generate a fresh EC P-256 JWK with the given kid. */
	static ECKey generateEc(String kid) throws JOSEException {
		return new ECKeyGenerator(Curve.P_256)
			.keyID(kid)
			.algorithm(JWSAlgorithm.ES256)
			.generate();
	}

	/** Generate a fresh symmetric HMAC key (256 bits) with the given kid. */
	static OctetSequenceKey generateHmac(String kid) throws JOSEException {
		return new OctetSequenceKeyGenerator(256)
			.keyID(kid)
			.algorithm(JWSAlgorithm.HS256)
			.generate();
	}

	/** Wrap a JWK as a fixed (no-network) JWKSource that publishes only the public material. */
	static JWKSource<SecurityContext> fixed(JWK... keys) {
		var set = new JWKSet(Arrays.asList(keys)).toPublicJWKSet();
		return new ImmutableJWKSet<>(set);
	}

	/** Wrap a list of JWKs (including secret material — for HMAC) as a fixed JWKSource. */
	static JWKSource<SecurityContext> fixedRaw(JWK... keys) {
		return new ImmutableJWKSet<>(new JWKSet(Arrays.asList(keys)));
	}

	/** Build a default claims set with all mandatory claims populated. */
	static JWTClaimsSet.Builder defaultClaims(Clock clock) {
		var now = clock.instant();
		return new JWTClaimsSet.Builder()
			.subject("alice")
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.issueTime(Date.from(now))
			.notBeforeTime(Date.from(now))
			.expirationTime(Date.from(now.plus(Duration.ofMinutes(5))));
	}

	/** Sign a claims set with an RSA key (RS256). */
	static String signRsa(RSAKey key, JWTClaimsSet claims) throws JOSEException {
		var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build();
		var jwt = new SignedJWT(header, claims);
		jwt.sign(new RSASSASigner(key));
		return jwt.serialize();
	}

	/** Sign a claims set with an EC key (ES256). */
	static String signEc(ECKey key, JWTClaimsSet claims) throws JOSEException {
		var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build();
		var jwt = new SignedJWT(header, claims);
		jwt.sign(new ECDSASigner(key));
		return jwt.serialize();
	}

	/** Sign a claims set with an HMAC key (HS256). */
	static String signHmac(OctetSequenceKey key, JWTClaimsSet claims) throws JOSEException {
		var header = new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(key.getKeyID()).build();
		var jwt = new SignedJWT(header, claims);
		jwt.sign(new MACSigner(key.toByteArray()));
		return jwt.serialize();
	}

	/**
	 * Build an HS256-signed JWT using the supplied raw secret bytes. Lets tests construct
	 * the algorithm-confusion attack token (HS256 signed with an RSA public key as the
	 * HMAC secret).
	 */
	static String signHmacRaw(byte[] secret, String kid, JWTClaimsSet claims) throws JOSEException {
		// MACSigner requires at least 32 bytes of secret for HS256. Real-world algorithm-confusion
		// attack tokens commonly use the PEM encoding of an RSA public key, which is >32 bytes; pad if needed.
		var bytes = secret;
		if (bytes.length < 32) {
			var padded = new byte[32];
			System.arraycopy(bytes, 0, padded, 0, bytes.length);
			bytes = padded;
		}
		var header = new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(kid).build();
		var jwt = new SignedJWT(header, claims);
		jwt.sign(new MACSigner(bytes));
		return jwt.serialize();
	}

	/**
	 * Build a parseable {@code alg: none} JWT (unsigned). Nimbus' {@code PlainJWT} forbids
	 * setting a signed-style header, so we hand-assemble the three Base64URL segments.
	 */
	static String unsignedToken(JWTClaimsSet claims) throws ParseException {
		var header = new com.nimbusds.jose.PlainHeader.Builder().build();
		var jwt = new com.nimbusds.jwt.PlainJWT(header, claims);
		return jwt.serialize();
	}
}
