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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link McpStepUpAuthorizer} (SEP-2350 client step-up state machine).
 *
 * @since 10.0.0
 */
class McpStepUpAuthorizer_Test extends TestBase {

	private static final URI RES = URI.create("https://mcp.example.com");
	private static final URI ISS = URI.create("https://as.example.com");

	private static McpInsufficientScopeException challenge(String scopes) {
		var c = WwwAuthenticateChallenge.parse("Bearer error=\"insufficient_scope\", scope=\"" + scopes + "\"").orElseThrow();
		return new McpInsufficientScopeException(c);
	}

	/** Throws a step-up challenge; declared to return String so it can sit in a ternary inside a ScopedCall&lt;String&gt;. */
	private static String raise(String scopes) {
		throw challenge(scopes);
	}

	@Test void a01_succeedsOnSecondAttemptAfterReauth() {
		var reauthScopes = new ArrayList<Set<String>>();
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.previouslyRequestedScopes("read")
			.reauthorizer(reauthScopes::add)
			.build();
		var calls = new AtomicInteger();
		var result = authorizer.execute("tools/call:deploy", () -> {
			if (calls.incrementAndGet() == 1)
				throw challenge("write");
			return "ok";
		});
		assertEquals("ok", result);
		assertEquals(2, calls.get());
		assertEquals(1, reauthScopes.size());
		assertEquals(List.of("read", "write"), List.copyOf(reauthScopes.get(0)), "reauth union = previously-requested ∪ challenge");
	}

	@Test void a02_accumulatesAcrossMultipleChallenges() {
		var reauthScopes = new ArrayList<Set<String>>();
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.previouslyRequestedScopes("read")
			.maxAttempts(5)
			.reauthorizer(reauthScopes::add)
			.build();
		var calls = new AtomicInteger();
		var result = authorizer.execute("op", () -> {
			var n = calls.incrementAndGet();
			if (n == 1)
				throw challenge("write");
			if (n == 2)
				throw challenge("admin");
			return "ok";
		});
		assertEquals("ok", result);
		assertEquals(List.of("read", "write"), List.copyOf(reauthScopes.get(0)));
		assertEquals(List.of("read", "write", "admin"), List.copyOf(reauthScopes.get(1)), "union grows monotonically (not replaced)");
	}

	@Test void a03_reauthUnionsNotReplaces() {
		// Guard against a "replace instead of union" regression: the challenge scope must be ADDED to prior scopes.
		var last = new AtomicReference<Set<String>>();
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.previouslyRequestedScopes("read", "write")
			.reauthorizer(last::set)
			.build();
		var calls = new AtomicInteger();
		authorizer.execute("op", () -> {
			if (calls.incrementAndGet() == 1)
				throw challenge("admin");
			return "ok";
		});
		assertTrue(last.get().containsAll(List.of("read", "write", "admin")));
	}

	@Test void b01_exhaustingRetriesThrowsPermanent() {
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.maxAttempts(2)
			.reauthorizer(s -> {})
			.build();
		assertThrowsWithMessage(McpAuthException.class, "permanent authorization failure", () ->
			authorizer.execute("op", () -> { throw challenge("write"); }));
	}

	@Test void b02_attemptTrackingPersistsAcrossFailingCalls() {
		// M4: WITHOUT an intervening success, the per-(resource, operation) counter persists, so a second call for a
		// permanently-failing operation fails fast (cap already reached) with no further reauths.
		var reauths = new AtomicInteger();
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.maxAttempts(2)
			.reauthorizer(s -> reauths.incrementAndGet())
			.build();
		assertThrows(McpAuthException.class, () -> authorizer.execute("op", () -> { throw challenge("write"); }));
		var reauthsAfterFirst = reauths.get();
		assertThrows(McpAuthException.class, () -> authorizer.execute("op", () -> { throw challenge("write"); }));
		assertEquals(reauthsAfterFirst, reauths.get(), "attempt tracking persists per (resource, operation) across failing calls");
		assertTrue(authorizer.attempts("op") > 2);
	}

	@Test void b04_successClearsCounterEnablingLaterStepUp() {
		// M4: a successful execute clears the operation's counter, so a LATER legitimate step-up for the same operation
		// is not permanently refused by a long-lived authorizer.
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.maxAttempts(1)
			.reauthorizer(s -> {})
			.build();
		var calls = new AtomicInteger();
		// First burst: one challenge then success (within cap) → counter cleared on success.
		assertEquals("ok", authorizer.execute("op", () -> calls.incrementAndGet() == 1 ? raise("write") : "ok"));
		assertEquals(0, authorizer.attempts("op"), "success must clear the counter");
		// Second burst for the same op must succeed again (it would fail if the counter had not been cleared).
		calls.set(0);
		assertEquals("ok", authorizer.execute("op", () -> calls.incrementAndGet() == 1 ? raise("write") : "ok"));
	}

	@Test void b05_resetClearsOperationCounter() {
		// M4: reset(operation) clears a permanently-failed operation's counter so a subsequent step-up may proceed.
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.maxAttempts(1)
			.reauthorizer(s -> {})
			.build();
		assertThrows(McpAuthException.class, () -> authorizer.execute("op", () -> { throw challenge("write"); }));
		assertTrue(authorizer.attempts("op") > 1);
		authorizer.reset("op");
		assertEquals(0, authorizer.attempts("op"));
		var calls = new AtomicInteger();
		assertEquals("ok", authorizer.execute("op", () -> calls.incrementAndGet() == 1 ? raise("write") : "ok"));
	}

	@Test void b06_resetAllClearsEveryOperation() {
		// M4: resetAll() clears all operations' counters.
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.maxAttempts(1)
			.reauthorizer(s -> {})
			.build();
		assertThrows(McpAuthException.class, () -> authorizer.execute("op1", () -> { throw challenge("write"); }));
		assertThrows(McpAuthException.class, () -> authorizer.execute("op2", () -> { throw challenge("write"); }));
		authorizer.resetAll();
		assertEquals(0, authorizer.attempts("op1"));
		assertEquals(0, authorizer.attempts("op2"));
	}

	@Test void b03_clientCredentialsCanAbortImmediately() {
		var reauths = new AtomicInteger();
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.attemptStepUp(false)
			.reauthorizer(s -> reauths.incrementAndGet())
			.build();
		assertThrowsWithMessage(McpAuthException.class, "aborted", () ->
			authorizer.execute("op", () -> { throw challenge("write"); }));
		assertEquals(0, reauths.get(), "attemptStepUp(false) must not re-authorize");
	}

	@Test void c01_builderValidation() {
		assertThrowsWithMessage(IllegalStateException.class, "resource", () ->
			McpStepUpAuthorizer.create().issuer(ISS).reauthorizer(s -> {}).build());
		assertThrowsWithMessage(IllegalStateException.class, "issuer", () ->
			McpStepUpAuthorizer.create().resource(RES).reauthorizer(s -> {}).build());
		assertThrowsWithMessage(IllegalStateException.class, "reauthorizer", () ->
			McpStepUpAuthorizer.create().resource(RES).issuer(ISS).build());
	}

	@Test void c02_noChallengeReturnsDirectly() {
		var reauths = new AtomicInteger();
		var authorizer = McpStepUpAuthorizer.create()
			.resource(RES).issuer(ISS)
			.reauthorizer(s -> reauths.incrementAndGet())
			.build();
		assertEquals("immediate", authorizer.execute("op", () -> "immediate"));
		assertEquals(0, reauths.get());
	}

	// NIT: McpInsufficientScopeException.challenge() javadoc promises "Never null"; the field is now non-transient so the
	// challenge (and its scopes) survives Java serialization rather than deserializing to null.
	@Test void d01_insufficientScopeExceptionSerializesChallenge() throws Exception {
		var original = challenge("read write");
		var baos = new ByteArrayOutputStream();
		try (var oos = new ObjectOutputStream(baos)) { oos.writeObject(original); }
		McpInsufficientScopeException restored;
		try (var ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
			restored = (McpInsufficientScopeException) ois.readObject();
		}
		assertNotNull(restored.challenge(), "challenge must survive serialization (non-transient)");
		assertEquals(original.challenge().scopes(), restored.challenge().scopes());
	}
}
