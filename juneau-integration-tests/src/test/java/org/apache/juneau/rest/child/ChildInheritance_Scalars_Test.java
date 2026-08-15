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

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 &mdash; proves the CHILD-WINS scalar shape for {@code @Child(defaultCharset=...)} and
 * {@code @Child(maxInput=...)}, both of which resolve through {@code RestContext.mergeReplacedStringAttribute}
 * (last-non-sentinel-wins over the annotation chain), exactly like {@code restDebugFormatter}/{@code partSerializer}/
 * {@code partParser}.
 *
 * <p>
 * {@code defaultAccept}/{@code defaultContentType} are intentionally NOT seed slots on {@code @Child} (see
 * {@link Child} javadoc): they resolve through the {@code defaultRequestHeaders} memoizer's
 * {@code HttpHeaderList.setDefault(...)} first-wins semantics rather than {@code mergeReplacedStringAttribute},
 * which would make the host's seed win over the child's own explicit declaration &mdash; the opposite of the
 * child-wins contract. That's pre-existing framework behavior unrelated to {@code @Child} (a plain
 * superclass/subclass {@code @Rest} hierarchy exhibits the same ancestor-wins-over-descendant behavior), so
 * these two members are deferred rather than shipped as a contract violation.
 */
class ChildInheritance_Scalars_Test extends TestBase {

	//==================================================================================================================
	// defaultCharset -- true child-wins (via mergeReplacedStringAttribute)
	//==================================================================================================================

	@Rest(path="/nocharset")
	public static class ChildNoCharsetDeclared {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoCharsetDeclared.class, defaultCharset="utf-16"))
	public static class HostSeedsCharset extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_charset_childWithNoDeclarationReceivesSeed() {
		MockRestClient.buildLax(HostSeedsCharset.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsCharset.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("nocharset");
		assertNotNull(childCtx);
		var opCtx = childCtx.getRestOperations().getOpContexts().get(0);
		assertEquals(StandardCharsets.UTF_16, opCtx.getDefaultCharset(),
			"Child with no defaultCharset declaration must receive the host's seeded utf-16");
	}

	@Rest(path="/owncharset", defaultCharset="utf-16")
	public static class ChildWithOwnCharset {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnCharset.class, defaultCharset="iso-8859-1"))
	public static class HostSeedsCharsetButChildWins extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_charset_childsOwnDeclarationWins() {
		MockRestClient.buildLax(HostSeedsCharsetButChildWins.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsCharsetButChildWins.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("owncharset");
		assertNotNull(childCtx);
		var opCtx = childCtx.getRestOperations().getOpContexts().get(0);
		assertEquals(StandardCharsets.UTF_16, opCtx.getDefaultCharset(),
			"Child's own explicit defaultCharset must win over the host's seeded iso-8859-1");
	}

	//==================================================================================================================
	// maxInput -- true child-wins (via mergeReplacedStringAttribute); seed-applies-when-silent already covered by
	// ChildContext_Seed_Test.a04 (seed vs. DefaultConfig fallback) -- this file adds the child-wins-when-explicit half.
	//==================================================================================================================

	@Rest(path="/ownmax", maxInput="111")
	public static class ChildWithOwnMaxInput {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnMaxInput.class, maxInput="222"))
	public static class HostSeedsMaxInputButChildWins extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_maxInput_childsOwnDeclarationWins() {
		MockRestClient.buildLax(HostSeedsMaxInputButChildWins.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostSeedsMaxInputButChildWins.class);
		var childCtx = hostCtx.getRestChildren().asMap().get("ownmax");
		assertNotNull(childCtx);
		var opCtx = childCtx.getRestOperations().getOpContexts().get(0);
		assertEquals(111L, opCtx.getMaxInput(),
			"Child's own explicit maxInput must win over the host's seeded 222");
	}
}
