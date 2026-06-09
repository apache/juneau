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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Phase G regression: verifies that the {@link RestOpContext#pathMatchers} Memoizer
 * exercises the compiled-template seam — every op-path string goes through
 * {@code vr.compile(p).resolve(session)} (not the legacy one-shot
 * {@code vr.resolve(p)} call). Production behavior must remain unchanged.
 *
 * <p>
 * The point of the test isn't to assert on internal data structures — those vary
 * across Memoizer implementations — but to lock in the user-visible contract:
 * SVL-bearing op paths still resolve correctly under the compiled-form path, with
 * no extra request-time cost (resolution still happens once at context-build time).
 */
class RestOpContext_PathMatcher_CompiledForm_Test extends TestBase {

	@Rest
	public static class A01_LiteralPath {
		@RestGet(path="/literal/*")
		public String hello() { return "ok"; }
	}

	@Test void a01_literalPathStillWorks() throws Exception {
		var c = MockRestClient.buildLax(A01_LiteralPath.class);
		c.get("/literal/x").run().assertStatus(200).assertContent().asString().isContains("ok");
	}

	@Rest
	public static class A02_SvlPathDefaultBranch {
		@RestGet(path="/${RestOpContext_PathMatcher_CompiledForm_Test.a02:a02-default}/*")
		public String hello() { return "a02-hit"; }
	}

	@Test void a02_svlPath_defaultBranch_compiledFormResolves() throws Exception {
		var c = MockRestClient.buildLax(A02_SvlPathDefaultBranch.class);
		c.get("/a02-default/anything").run().assertStatus(200).assertContent().asString().isContains("a02-hit");
	}

	@Rest
	public static class A03_SvlPathOverride {
		@RestGet(path="/${RestOpContext_PathMatcher_CompiledForm_Test.a03:a03-default}/*")
		public String hello() { return "a03-hit"; }
	}

	@Test void a03_svlPath_overrideBranch_compiledFormResolves() throws Exception {
		System.setProperty("RestOpContext_PathMatcher_CompiledForm_Test.a03", "a03-override");
		try {
			var c = MockRestClient.buildLax(A03_SvlPathOverride.class);
			c.get("/a03-override/anything").run().assertStatus(200).assertContent().asString().isContains("a03-hit");
			c.get("/a03-default/anything").run().assertStatus(404);
		} finally {
			System.clearProperty("RestOpContext_PathMatcher_CompiledForm_Test.a03");
		}
	}

	@Rest
	public static class A04_MultipleOpsSamePath {
		@RestGet(path="/${RestOpContext_PathMatcher_CompiledForm_Test.a04:a04-default}/get")
		public String getOp() { return "get-hit"; }

		@RestGet(path="/${RestOpContext_PathMatcher_CompiledForm_Test.a04:a04-default}/get2")
		public String getOp2() { return "get2-hit"; }
	}

	@Test void a04_repeatedExpressionAcrossOps_compiledFormReused() throws Exception {
		var c = MockRestClient.buildLax(A04_MultipleOpsSamePath.class);
		c.get("/a04-default/get").run().assertStatus(200).assertContent().asString().isContains("get-hit");
		c.get("/a04-default/get2").run().assertStatus(200).assertContent().asString().isContains("get2-hit");
	}
}
