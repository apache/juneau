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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 smoke &mdash; verifies that a mixin sub-context's var resolver reaches the host's Messages bundle
 * for {@code $L{key}}-style lookups.
 *
 * <p>
 * The mixin's {@link RestContext#getVarResolver() var resolver} is built from the bootstrap resolver plus the
 * mixin's own {@link org.apache.juneau.cp.Messages} bean.  Because the mixin's Messages bundle is now chained
 * to the host's via {@code Messages.chain(...)}, {@code $L{host.key}} resolves through the inherited host
 * bundle from the mixin context.  This test is a smoke check on that end-to-end seam &mdash; the deeper
 * acceptance criteria are covered by {@code MixinInheritance_Messages_Test}.
 */
class MixinInheritance_VarResolver_Test extends TestBase {

	@Rest
	public static class M_NoMessages {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(messages="HostMessages", mixins={M_NoMessages.class})
	public static class HostWithHostMessages extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinVarResolverSeesHostLocalizedMessages() {
		MockRestClient.buildLax(HostWithHostMessages.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithHostMessages.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoMessages.class);
		assertNotNull(mixinCtx);

		assertEquals("host-value", hostCtx.getVarResolver().resolve("$L{host.key}"),
			"Host var resolver must resolve $L{host.key} from its own bundle");
		assertEquals("host-value", mixinCtx.getVarResolver().resolve("$L{host.key}"),
			"Mixin var resolver must resolve $L{host.key} via the inherited host Messages chain");
	}
}
