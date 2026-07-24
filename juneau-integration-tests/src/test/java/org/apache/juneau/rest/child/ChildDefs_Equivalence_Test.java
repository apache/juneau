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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Feature test for the {@link Rest#childrenDefs() @Rest(childrenDefs=@Child(...))} bare-equivalence rule.
 *
 * <p>
 * Verifies the Phase 1 motivating capability &mdash; {@code @Child(type=X)} with zero overrides routes
 * identically to a bare {@code @Rest(children=X)} entry &mdash; mirroring
 * {@code MixinDefs_Overrides_Test.f01_bareEqualsEmptyDef}.
 */
class ChildDefs_Equivalence_Test extends TestBase {

	@Rest(path="/plain")
	public static class PlainChild {
		@RestGet("/ping") public String ping() { return "pong"; }
	}

	@Rest(children=PlainChild.class)
	public static class Bare extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(childrenDefs=@Child(type=PlainChild.class))
	public static class EmptyDef extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_bareEqualsEmptyChildDef() throws Exception {
		var bare = MockRestClient.buildLax(Bare.class);
		var def = MockRestClient.buildLax(EmptyDef.class);
		bare.get("/plain/ping").accept("text/plain").run().assertStatus(200).assertContent("pong");
		def.get("/plain/ping").accept("text/plain").run().assertStatus(200).assertContent("pong");
	}
}
