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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 (TODO-364): {@code RestRequest} convenience logging methods ride the resolved per-operation
 * {@link RichLogger} and therefore auto-carry the active {@link LogContext} onto their emitted records, observable at
 * an <b>ancestor</b> (resource-class-level) capture via the {@code [TODO-365]} parent-chain propagation.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RestRequestLogContext_Test {

	@Rest(path="/p4")
	public static class A_Resource {
		@RestGet(path="/go")
		public String go(RestRequest req) {
			try (var s = RichLogger.context().with("foo", "bar")) {
				req.fine("processing %s", "item1");
			}
			return "ok";
		}
	}

	/**
	 * A convenience call inside a handler, wrapped in a {@code LogContext} scope, emits a per-operation record whose
	 * attached structured context carries the scope's entry &mdash; observed at the resource-class-level (ancestor)
	 * capture, proving §6.2's "no new plumbing; rides the existing parent-chain propagation" claim.
	 */
	@Test void a01_convenienceMethodRecordCarriesActiveContext_atAncestorCapture() throws Exception {
		var opName = A_Resource.class.getName() + ".go";
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(A_Resource.class).build();

			client.get("/go").run().assertStatus().asCode().is(200);

			var rec = c.getRecords().stream()
				.filter(r -> opName.equals(r.getLoggerName()) && Level.FINE.equals(r.getLevel()))
				.findFirst().orElse(null);
			assertNotNull(rec, "expected a FINE op-logger record named " + opName + " from req.fine(...)");
			assertEquals("bar", c.contextValue(rec, "foo"),
				"the convenience-method record must carry the LogContext active at emit time");
		}
	}

	/**
	 * The scope is auto-cleared at try-with-resources exit: a second convenience call made <i>after</i> the scope
	 * closes must not carry the entry &mdash; proves the emitted snapshot tracks the live context, not a stale copy.
	 */
	@Rest(path="/p4b")
	public static class B_Resource {
		@RestGet(path="/go")
		public String go(RestRequest req) {
			try (var s = RichLogger.context().with("foo", "bar")) {
				req.fine("in-scope");
			}
			req.fine("out-of-scope");
			return "ok";
		}
	}

	@Test void a02_afterScopeCloses_convenienceRecordDoesNotCarryEntry() throws Exception {
		var opName = B_Resource.class.getName() + ".go";
		try (var c = RichLogger.getLogger(B_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(B_Resource.class).build();

			client.get("/go").run().assertStatus().asCode().is(200);

			var inScope = c.getRecords().stream()
				.filter(r -> opName.equals(r.getLoggerName()) && "in-scope".equals(r.getMessage()))
				.findFirst().orElse(null);
			var outOfScope = c.getRecords().stream()
				.filter(r -> opName.equals(r.getLoggerName()) && "out-of-scope".equals(r.getMessage()))
				.findFirst().orElse(null);

			assertNotNull(inScope, "expected the in-scope FINE record");
			assertNotNull(outOfScope, "expected the out-of-scope FINE record");
			assertEquals("bar", c.contextValue(inScope, "foo"));
			assertNull(c.contextValue(outOfScope, "foo"),
				"the record emitted after the scope closed must not carry the entry");
		}
	}
}
