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
package org.apache.juneau.rest.child;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Discovery-order and dedup tests for {@link Rest#childrenDefs() @Rest(childrenDefs=@Child(...))}, mirroring
 * {@code MixinDefs_Overrides_Test} sections F/I/M for the {@code mixins}/{@code mixinDefs} discovery rules.
 */
class ChildDefs_Discovery_Test extends TestBase {

	@Rest(path="/plain")
	public static class PlainChild {
		@RestGet("/ping") public String ping() { return "pong"; }
	}

	@Rest(path="/plain2")
	public static class PlainChild2 {
		@RestGet("/ping") public String ping() { return "pong2"; }
	}

	// A class named in both children= and childrenDefs= must be mounted exactly once.
	@Rest(children=PlainChild.class, childrenDefs=@Child(type=PlainChild.class))
	public static class DedupHost extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_dedupedToFirstOccurrence() throws Exception {
		var c = MockRestClient.buildLax(DedupHost.class);
		c.get("/plain/ping").accept("text/plain").run().assertStatus(200).assertContent("pong");
		var hostCtx = RestContext.getGlobalRegistry().get(DedupHost.class);
		assertEquals(1, hostCtx.getRestChildren().asMap().size());
	}

	// Bare children= entries are discovered before childrenDefs= entries.
	@Rest(children=PlainChild.class, childrenDefs=@Child(type=PlainChild2.class))
	public static class OrderHost extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_bareDiscoveredBeforeChildrenDefs() {
		MockRestClient.buildLax(OrderHost.class);
		var hostCtx = RestContext.getGlobalRegistry().get(OrderHost.class);
		var keys = new ArrayList<>(hostCtx.getRestChildren().asMap().keySet());
		assertEquals(List.of("plain", "plain2"), keys);
	}
}
