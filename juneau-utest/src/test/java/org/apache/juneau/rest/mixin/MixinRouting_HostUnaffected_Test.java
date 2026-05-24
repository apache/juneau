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
package org.apache.juneau.rest.mixin;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.junit.jupiter.api.*;

/**
 * Phase 3 — exercises end-to-end routing through mixin-specific {@link org.apache.juneau.rest.RestContext}
 * sub-contexts.  Verifies that a mixin's appended serializer is visible on mixin endpoints but does NOT leak
 * onto host endpoints.
 *
 * <p>
 * Uses a uniquely-named test serializer so the BasicUniversalConfig default set inherited by
 * {@link BasicRestServlet} can't accidentally route to a fallback serializer.
 */
class MixinRouting_HostUnaffected_Test extends TestBase {

	public static class MixinS1 extends FakeWriterSerializer {
		public MixinS1(FakeWriterSerializer.Builder b) { super(b.produces("text/mixin-s1").accept("text/mixin-s1")); }
	}

	@Rest(serializers={MixinS1.class})
	public static class TheMixin {
		@RestGet(path="/mixin/echo") public String mixinEcho() { return "mixin"; }
	}

	@Rest(mixins={TheMixin.class})
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/host/echo") public String hostEcho() { return "host"; }
	}

	@Test void a01_mixinEndpointAcceptsCustomMediaType() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/mixin/echo").accept("text/mixin-s1").run().assertStatus(200);
	}

	@Test void a02_hostEndpointRejectsCustomMediaType() throws Exception {
		var c = MockRestClient.buildLax(Host.class);
		c.get("/host/echo").accept("text/mixin-s1").run().assertStatus(406);
	}
}
