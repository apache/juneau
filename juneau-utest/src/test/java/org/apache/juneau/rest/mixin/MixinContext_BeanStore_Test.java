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

import org.apache.juneau.TestBase;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 1 — verifies that a mixin sub-context's {@code beanStore} is parent-linked to the host's
 * {@code beanStore} so that host-declared {@code @Bean} factories resolve through the mixin's lookup chain
 * (resolved decision: bean-store layering).
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Host-declared {@code @Bean} resolvable via mixin's bean store.</li>
 * 	<li>Mixin-declared {@code @Bean} NOT resolvable via host's bean store.</li>
 * </ul>
 */
class MixinContext_BeanStore_Test extends TestBase {

	public static class HostBean { public String tag() { return "host"; } }
	public static class MixinBean { public String tag() { return "mixin"; } }

	@Rest
	public static class M {
		@RestGet(path="/m") public String m() { return "m"; }
		@Bean(name="mixinBean") public MixinBean mixinBean() { return new MixinBean(); }
	}

	@Rest(mixins={M.class})
	public static class HostBeanStore extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
		@Bean(name="hostBean") public HostBean hostBean() { return new HostBean(); }
	}

	@Test void a01_hostBeanResolvableFromMixinBeanStore() throws Exception {
		var c = MockRestClient.buildLax(HostBeanStore.class);
		c.get("/m").run().assertStatus(200);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBeanStore.class);
		var mixinCtx = hostCtx.getMixinContexts().get(M.class);
		assertNotNull(mixinCtx);

		var fromMixinStore = mixinCtx.getBeanStore().getBean(HostBean.class, "hostBean").orElse(null);
		assertNotNull(fromMixinStore, "Mixin beanStore must resolve host-declared @Bean via parent-link");
		var bulk = mixinCtx.getBeanStore().getBeansOfType(HostBean.class);
		assertTrue(bulk.containsKey("hostBean"),
			"Mixin's getBeansOfType must also surface host's named @Bean factories");
	}

	@Test void a02_mixinBeanNotResolvableFromHostBeanStore() {
		MockRestClient.buildLax(HostBeanStore.class);
		var hostCtx = RestContext.getGlobalRegistry().get(HostBeanStore.class);

		var fromHostStore = hostCtx.getBeanStore().getBean(MixinBean.class, "mixinBean").orElse(null);
		assertNull(fromHostStore, "Mixin-declared @Bean must NOT leak into host beanStore");
		var bulk = hostCtx.getBeanStore().getBeansOfType(MixinBean.class);
		assertFalse(bulk.containsKey("mixinBean"),
			"Mixin-declared @Bean must NOT appear in host's getBeansOfType output");
	}
}
