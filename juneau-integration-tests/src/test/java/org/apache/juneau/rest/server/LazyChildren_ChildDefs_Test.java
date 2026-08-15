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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 6 &mdash; proves a {@code @Rest(lazyChildren="true", childrenDefs=@Child(...))} child receives its
 * host-seeded settings on first-request materialization, not just when eagerly built. Mirrors
 * {@link LazyChildren_Test}'s style/package (same package so it lives alongside the base lazy-children suite).
 */
class LazyChildren_ChildDefs_Test extends TestBase {

	public static class SeedPS extends FakeWriterSerializer {
		public SeedPS(FakeWriterSerializer.Builder b) { super(b); }
	}

	@Rest(path="/lazychild")
	public static class LazyChildResource {
		@RestGet("/ping") public String ping() { return "lazy-pong"; }
	}

	@Rest(
		path="/root",
		lazyChildren="true",
		childrenDefs=@Child(type=LazyChildResource.class, partSerializer=SeedPS.class)
	)
	public static class LazySeedParent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_seedAppliesAfterLazyMaterialization() throws Exception {
		var parent = new LazySeedParent();
		var client = MockRestClient.createLax(parent).build();
		var rc = parent.getContext();

		assertTrue(rc.isLazyChildren());

		// Before first request: registered for routing but not yet materialized.
		var entries = rc.getRestChildren().getLazyEntries();
		var entry = entries.get("lazychild");
		assertNotNull(entry, "Lazy entry key should be 'lazychild'");
		assertFalse(entry.isMaterialized(), "Should not be materialized before first request");

		// First request triggers materialization.
		client.get("/lazychild/ping").run().assertStatus(200).assertContent("lazy-pong");
		assertTrue(entry.isMaterialized(), "Should be materialized after first request");

		// The materialized context must carry the host's seeded partSerializer -- proving the ResolvedChild
		// (including its @Child seed) survived deferred construction, not just eager construction.
		// (Materialized lazy children live on the LazyChildEntry itself, not in RestChildren.asMap() --
		// that map holds only eagerly-built children.)
		var childCtx = entry.materialized;
		assertNotNull(childCtx);
		assertInstanceOf(SeedPS.class, childCtx.getPartSerializer(),
			"Lazily-materialized child must receive the host's seeded SeedPS, same as an eager child would");
	}
}
