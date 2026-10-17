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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link JobKeyLedger}: row-bound issue/verify, one refusal for every miss, claim-on-first-use, TTL and the caps.
 */
class JobKeyLedger_Test extends TestBase {

	/** A clock the test moves by hand. */
	private static final class StepClock extends Clock {
		Instant now = Instant.parse("2026-10-17T00:00:00Z");
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}

	//------------------------------------------------------------------------------------------------------------------
	// Issue and verify
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_issue_thenVerify_returnsGrantWithBinding() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		var g = l.verify(k.value(), "restock", "dog").orElseThrow();
		assertEquals("restock", g.key().action());
		assertEquals("dog", g.key().targetId());
		assertEquals(k.value(), g.key().value());
		assertEquals(Optional.empty(), g.jobId());
	}

	@Test void a02_verify_otherRowOrAction_isEmpty() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		assertTrue(l.verify(k.value(), "restock", "cat").isEmpty());
		assertTrue(l.verify(k.value(), "groom", "dog").isEmpty());
	}

	@Test void a03_verify_unknownOrBlank_isEmpty() {
		var l = JobKeyLedger.create();
		l.issue("restock", "dog");
		assertTrue(l.verify("nope", "restock", "dog").isEmpty());
		assertTrue(l.verify(null, "restock", "dog").isEmpty());
		assertTrue(l.verify(" ", "restock", "dog").isEmpty());
		assertTrue(l.verify("x", null, "dog").isEmpty());
		assertTrue(l.verify("x", "restock", "").isEmpty());
	}

	@Test void a04_issue_blankArgsThrow() {
		var l = JobKeyLedger.create();
		assertThrows(IllegalArgumentException.class, () -> l.issue(" ", "dog"));
		assertThrows(IllegalArgumentException.class, () -> l.issue("restock", null));
		assertThrows(IllegalArgumentException.class, () -> l.issue("restock", "dog", " "));
		assertThrows(IllegalArgumentException.class, () -> l.issue("restock", "dog", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Principal binding
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_principal_mustMatch() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog", "alice");
		assertTrue(l.verify(k.value(), "restock", "dog", "alice").isPresent());
		assertTrue(l.verify(k.value(), "restock", "dog", "bob").isEmpty());
		assertTrue(l.verify(k.value(), "restock", "dog").isEmpty());
	}

	@Test void b02_noPrincipalKey_doesNotVerifyForAPrincipal() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		assertTrue(l.verify(k.value(), "restock", "dog", "alice").isEmpty());
		assertTrue(l.verify(k.value(), "restock", "dog", null).isPresent());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Claim-on-first-use
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_claim_recordsJobOnLaterVerify() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		l.claim(k, "job-1");
		assertEquals(Optional.of("job-1"), l.verify(k.value(), "restock", "dog").orElseThrow().jobId());
	}

	@Test void c02_claim_sameJob_isIdempotent_otherJobThrows() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		l.claim(k, "job-1");
		l.claim(k, "job-1");
		assertThrows(IllegalStateException.class, () -> l.claim(k, "job-2"));
	}

	@Test void c03_claim_unknownKey_isIgnored() {
		var l = JobKeyLedger.create();
		l.claim(IdempotencyKey.mint("restock", "dog"), "job-1");
		assertEquals(0, l.size());
	}

	@Test void c04_claim_blankJobIdThrows() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		assertThrows(IllegalArgumentException.class, () -> l.claim(k, " "));
		assertThrows(IllegalArgumentException.class, () -> l.claim(null, "job-1"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// TTL and caps
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_ttl_expiresKeys() {
		var c = new StepClock();
		var l = JobKeyLedger.create().clock(c).ttl(Duration.ofMinutes(5));
		var k = l.issue("restock", "dog");
		c.now = c.now.plus(Duration.ofMinutes(4));
		assertTrue(l.verify(k.value(), "restock", "dog").isPresent());
		c.now = c.now.plus(Duration.ofMinutes(1));
		assertTrue(l.verify(k.value(), "restock", "dog").isEmpty());
		assertEquals(0, l.size());
	}

	@Test void d02_maxPerPrincipal_evictsThatPrincipalsOldest() {
		var l = JobKeyLedger.create().maxPerPrincipal(2);
		var a1 = l.issue("restock", "dog", "alice");
		var b1 = l.issue("restock", "dog", "bob");
		var a2 = l.issue("restock", "cat", "alice");
		var a3 = l.issue("restock", "fish", "alice");
		assertTrue(l.verify(a1.value(), "restock", "dog", "alice").isEmpty());
		assertTrue(l.verify(a2.value(), "restock", "cat", "alice").isPresent());
		assertTrue(l.verify(a3.value(), "restock", "fish", "alice").isPresent());
		assertTrue(l.verify(b1.value(), "restock", "dog", "bob").isPresent());
	}

	@Test void d03_maxPerPrincipal_countsPerAction_andSkipsSpentKeys() {
		var l = JobKeyLedger.create().maxPerPrincipal(1);
		var r = l.issue("restock", "dog", "alice");
		l.claim(r, "job-1");
		var g = l.issue("groom", "dog", "alice");
		var r2 = l.issue("restock", "cat", "alice");
		assertTrue(l.verify(r.value(), "restock", "dog", "alice").isPresent(), "a spent key is not evicted by the cap");
		assertTrue(l.verify(g.value(), "groom", "dog", "alice").isPresent());
		assertTrue(l.verify(r2.value(), "restock", "cat", "alice").isPresent());
	}

	@Test void d04_maxEntries_evictsLeastRecentlyUsed() {
		var l = JobKeyLedger.create().maxEntries(2);
		var k1 = l.issue("restock", "a");
		var k2 = l.issue("restock", "b");
		l.verify(k1.value(), "restock", "a");
		var k3 = l.issue("restock", "c");
		assertEquals(2, l.size());
		assertTrue(l.verify(k1.value(), "restock", "a").isPresent());
		assertTrue(l.verify(k2.value(), "restock", "b").isEmpty());
		assertTrue(l.verify(k3.value(), "restock", "c").isPresent());
	}

	@Test void d05_setters_rejectNonPositive() {
		var l = JobKeyLedger.create();
		assertThrows(IllegalArgumentException.class, () -> l.ttl(Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> l.ttl(null));
		assertThrows(IllegalArgumentException.class, () -> l.maxEntries(0));
		assertThrows(IllegalArgumentException.class, () -> l.maxPerPrincipal(0));
		assertThrows(IllegalArgumentException.class, () -> l.clock(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_response_isOrderedContractMap() {
		var k = IdempotencyKey.mint("restock", "dog");
		var m = JobKeyLedger.response(k);
		assertEquals(List.of("contractVersion", "action", "targetId", "idempotencyKey"), List.copyOf(m.keySet()));
		assertEquals("1", m.get("contractVersion"));
		assertEquals("restock", m.get("action"));
		assertEquals("dog", m.get("targetId"));
		assertEquals(k.value(), m.get("idempotencyKey"));
	}

	@Test void e02_toString_neverLeaksValues() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		assertFalse(l.toString().contains(k.value()));
		assertEquals("JobKeyLedger(size=1)", l.toString());
	}
}
