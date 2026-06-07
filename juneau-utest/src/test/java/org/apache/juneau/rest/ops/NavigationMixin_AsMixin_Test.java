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
package org.apache.juneau.rest.ops;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link NavigationMixin} mounted as a mixin via {@code @Rest(mixins=...)} on a vanilla
 * {@link RestServlet} host.
 *
 * <p>
 * The navigation op lives on the mixin's (empty) sub-context but must enumerate the HOST's child resources
 * via {@link RestMixin#getHostContext()}. These tests confirm the {@code GET /} navigation page lists the
 * host's children rather than the mixin's own (empty) children.
 *
 * @since 10.0.0
 */
class NavigationMixin_AsMixin_Test extends TestBase {

	@Rest(path="/c1", title="Child One") public static class Child1 extends BasicRestServlet { private static final long serialVersionUID = 1L; @RestGet(path="/") public String x() { return "c1"; } }
	@Rest(path="/c2", title="Child Two") public static class Child2 extends BasicRestServlet { private static final long serialVersionUID = 1L; @RestGet(path="/") public String x() { return "c2"; } }

	// Host extends BasicRestServlet (for serializers) and composes NavigationMixin directly, rather than
	// inheriting it from a Basic*Group base, to prove the mixin enumerates the HOST's children.
	@Rest(mixins=NavigationMixin.class, children={Child1.class, Child2.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient a = MockRestClient.buildLax(A.class);

	@Test void a01_navigationListsHostChildren() throws Exception {
		var body = a.get("/").accept("application/json").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("c1"), "Navigation page must list host child 'c1'; got: " + body);
		assertTrue(body.contains("c2"), "Navigation page must list host child 'c2'; got: " + body);
	}

	@Test void a02_navigationNotEmpty() throws Exception {
		var body = a.get("/").accept("application/json").run().assertStatus(200).getContent().asString();
		assertNotEquals("[]", body.trim(), "Navigation page must not enumerate the mixin's own (empty) children.");
	}
}
