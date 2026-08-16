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
package org.apache.juneau.rest.server.auth.saml;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.concurrent.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.opensaml.saml.common.*;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.*;

/**
 * Tests that one-time-use enforcement in {@link SamlAssertionValidator} holds for the assertion's full validity
 * window, including when {@code Conditions/NotOnOrAfter} is absent.
 *
 * <p>
 * An assertion with no {@code NotOnOrAfter} anywhere (neither {@code Conditions/NotOnOrAfter} nor a bearer
 * {@code SubjectConfirmationData/NotOnOrAfter}) has an unbounded validity window and must be rejected rather than
 * pinned to a short replay-cache fallback that later expires and re-opens replay.  When a bounding
 * {@code NotOnOrAfter} <i>is</i> present, the replay-cache record must be retained until that resolved instant
 * (plus clock skew), not a fixed default.
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_ReplayExpiry_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final String ACS = "https://sp.example.com/saml/acs";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final Duration SKEW = Duration.ofSeconds(60);
	private static final Instant NBF = NOW.minusSeconds(60);

	private static SamlAssertionValidator.Builder base(Credential cred) {
		return SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.clock(CLOCK);
	}

	/** A replay cache that always reports first-seen and records the expiry it was handed. */
	private static final class CapturingCache implements ReplayCache {
		private final AtomicLong lastExpiry = new AtomicLong(Long.MIN_VALUE);

		@Override /* ReplayCache */
		public boolean checkAndRecord(String id, long expiresAtMs) {
			lastExpiry.set(expiresAtMs);
			return true;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Missing NotOnOrAfter everywhere → rejected (unbounded validity window).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_noNotOnOrAfterAnywhere_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		// Conditions carries an audience but no NotOnOrAfter; no bearer SubjectConfirmation at all.
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, null);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var validator = base(cred).build();
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void a02_conditionsNotOnOrAfterPresent_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOW.plusSeconds(300));
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		assertEquals("alice", base(cred).build().validate(xml).getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Replay-cache retention uses the resolved NotOnOrAfter (plus skew), not a fixed default.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_missingConditionsNoa_fallsBackToBearerConfirmationNoa() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var confNoa = NOW.plusSeconds(600);
		// Conditions has no NotOnOrAfter, but a bearer confirmation supplies one; recipient(...) is NOT configured,
		// so the confirmation is not enforced during validation, yet must still bound the replay-cache retention.
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, confNoa, null, null);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", sc);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, null);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		assertEquals("alice", base(cred).replayCache(cache).build().validate(xml).getName());
		assertEquals(confNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	@Test void b02_conditionsNoa_usedForRetention() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var condNoa = NOW.plusSeconds(300);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, condNoa);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		base(cred).replayCache(cache).build().validate(xml);
		assertEquals(condNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	@Test void b03_laterBearerConfirmationNoa_wins() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var condNoa = NOW.plusSeconds(300);
		var confNoa = NOW.plusSeconds(900);  // later than the Conditions window
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, confNoa, null, null);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", sc);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, condNoa);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		base(cred).replayCache(cache).build().validate(xml);
		assertEquals(confNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	@Test void b04_laterConditionsNoa_wins() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var condNoa = NOW.plusSeconds(900);  // later than the confirmation window
		var confNoa = NOW.plusSeconds(300);
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, confNoa, null, null);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", sc);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, condNoa);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		base(cred).replayCache(cache).build().validate(xml);
		assertEquals(condNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Confirmations that do not contribute a NotOnOrAfter fall through to the Conditions value.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_nonBearerConfirmation_ignoredForRetention() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var condNoa = NOW.plusSeconds(300);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", holderOfKeyConfirmation());
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, condNoa);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		base(cred).replayCache(cache).build().validate(xml);
		assertEquals(condNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	@Test void c02_bearerConfirmationWithNoData_ignoredForRetention() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var condNoa = NOW.plusSeconds(300);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", bearerConfirmationNoData());
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, condNoa);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		base(cred).replayCache(cache).build().validate(xml);
		assertEquals(condNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	@Test void c03_bearerConfirmationWithNullNoa_ignoredForRetention() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var condNoa = NOW.plusSeconds(300);
		// Bearer confirmation carries a SubjectConfirmationData but no NotOnOrAfter → contributes nothing.
		var sc = SamlTestSupport.bearerConfirmation(null, null, null, null, null);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", sc);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, condNoa);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var cache = new CapturingCache();
		base(cred).replayCache(cache).build().validate(xml);
		assertEquals(condNoa.plus(SKEW).toEpochMilli(), cache.lastExpiry.get());
	}

	@SuppressWarnings("unchecked")
	private static SubjectConfirmation holderOfKeyConfirmation() {
		var scb = (SAMLObjectBuilder<SubjectConfirmation>) SamlTestSupport.bf().getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
		var sc = scb.buildObject();
		sc.setMethod(SubjectConfirmation.METHOD_HOLDER_OF_KEY);
		return sc;
	}

	@SuppressWarnings("unchecked")
	private static SubjectConfirmation bearerConfirmationNoData() {
		var scb = (SAMLObjectBuilder<SubjectConfirmation>) SamlTestSupport.bf().getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
		var sc = scb.buildObject();
		sc.setMethod(SubjectConfirmation.METHOD_BEARER);
		return sc;  // no SubjectConfirmationData
	}
}
