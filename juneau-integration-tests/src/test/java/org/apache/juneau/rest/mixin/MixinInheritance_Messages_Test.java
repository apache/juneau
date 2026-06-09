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
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 &mdash; exercises mixin-sub-context inheritance for the {@code @Rest(messages=...)} property.
 *
 * <p>
 * Messages don't ride the standard {@code getRestAnnotationsForProperty(...)} walk &mdash; they use
 * {@link RestContext#getRestAnnotationsTopDown()} which reads only the local class hierarchy.  Instead, mixin
 * sub-contexts compose their own bundle with the host's bundle as the parent via
 * {@link Messages#chain(Messages, Messages)} during the {@code messages} memoizer.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Mixin without {@code messages} declaration inherits the host's bundle &mdash; {@code host.key} resolves via mixin.</li>
 * 	<li>Mixin's own {@code @Rest(messages=...)} chains on top of the inherited host bundle:
 * 		mixin keys resolve locally, host keys still resolve via the parent chain, and the mixin's own value
 * 		for a shared key takes precedence over the host's value.</li>
 * 	<li>{@code @Rest(noInherit="messages", messages=...)} on a mixin cuts off the host bundle &mdash;
 * 		{@code host.key} no longer resolves through the mixin.</li>
 * 	<li>Host endpoint is unaffected by any mixin's contributions.</li>
 * </ul>
 */
class MixinInheritance_Messages_Test extends TestBase {

	@Rest
	public static class M_NoMessages {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(messages="MixinMessages")
	public static class M_WithMessages {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="messages", messages="MixinMessages")
	public static class M_NoInheritMessages {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(messages="HostMessages", mixins={M_NoMessages.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(messages="HostMessages", mixins={M_WithMessages.class})
	public static class HostWithMixinAppends extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(messages="HostMessages", mixins={M_NoInheritMessages.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	/** Returns {@code null} when the key isn't resolved (Messages signals miss with a {@code "{!key}"} marker). */
	private static String resolved(Messages m, String key) {
		var v = m.getString(key);
		return (v == null || v.startsWith("{!")) ? null : v;
	}

	@Test void a01_mixinInheritsHostMessages() {
		MockRestClient.buildLax(HostInheritsToMixin.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostInheritsToMixin.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoMessages.class);
		assertNotNull(mixinCtx);

		assertEquals("host-value", resolved(hostCtx.getMessages(), "host.key"),
			"Host bundle must resolve its own host.key");
		assertEquals("host-value", resolved(mixinCtx.getMessages(), "host.key"),
			"Mixin with no @Rest(messages=) must inherit host.key via the host's parent-chained bundle");
	}

	@Test void a02_mixinAppendsMessagesOverInheritedHost() {
		MockRestClient.buildLax(HostWithMixinAppends.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithMixinAppends.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_WithMessages.class);
		assertNotNull(mixinCtx);

		var hostMsgs = hostCtx.getMessages();
		var mixinMsgs = mixinCtx.getMessages();

		assertEquals("host-value", resolved(hostMsgs, "host.key"));
		assertNull(resolved(hostMsgs, "mixin.key"),
			"Host bundle must NOT see mixin.key &mdash; mixin contributions are scoped to mixin sub-context");

		assertEquals("mixin-value", resolved(mixinMsgs, "mixin.key"),
			"Mixin bundle must resolve its own mixin.key");
		assertEquals("host-value", resolved(mixinMsgs, "host.key"),
			"Mixin bundle must still resolve host.key via the parent-chained host bundle");
		assertEquals("mixin-shared", resolved(mixinMsgs, "shared.key"),
			"Shared key must take the mixin's value (local lookup wins before parent fallthrough)");
		assertEquals("host-shared", resolved(hostMsgs, "shared.key"),
			"Host bundle's shared.key must remain the host's value");
	}

	@Test void a03_mixinNoInheritShadowsHostMessages() {
		MockRestClient.buildLax(HostWithNoInherit.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostWithNoInherit.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M_NoInheritMessages.class);
		assertNotNull(mixinCtx);

		var mixinMsgs = mixinCtx.getMessages();

		assertEquals("mixin-value", resolved(mixinMsgs, "mixin.key"),
			"Mixin must still resolve its own mixin.key");
		assertNull(resolved(mixinMsgs, "host.key"),
			"Mixin with @Rest(noInherit=\"messages\") must NOT see host.key");
		assertEquals("host-value", resolved(hostCtx.getMessages(), "host.key"),
			"Host bundle unchanged by mixin's noInherit");
	}
}
