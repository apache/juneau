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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/** Regression matrix for `@Debug(config=...)` inheritance on mixins. */
class MixinInheritance_DebugEnablement_Test extends TestBase {

	private abstract static class BaseConfig extends DebugConfig {
		protected final boolean enabled;
		protected BaseConfig(BeanStore bs, boolean enabled) {
			super(bs);
			this.enabled = enabled;
		}
		@Override
		public DebugResult resolve(org.apache.juneau.rest.RestContext context, jakarta.servlet.http.HttpServletRequest req) {
			return new DebugResult(enabled, null, java.util.logging.Level.INFO, enabled);
		}
		@Override
		public DebugResult resolve(org.apache.juneau.rest.RestOpContext context, jakarta.servlet.http.HttpServletRequest req) {
			return new DebugResult(enabled, null, java.util.logging.Level.INFO, enabled);
		}
	}

	public static class HostDebug extends BaseConfig {
		public HostDebug(BeanStore bs) { super(bs, true); }
	}

	public static class MixinDebug extends BaseConfig {
		public MixinDebug(BeanStore bs) { super(bs, false); }
	}

	@Rest
	public static class M_NoDebugDeclared {
		@RestGet(path="/me") public boolean me(org.apache.juneau.rest.RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug(config=MixinDebug.class))
	public static class M_MixinDebug {
		@RestGet(path="/my") public boolean my(org.apache.juneau.rest.RestRequest req) { return req.isDebug(); }
	}

	@Rest(noInherit="debug", debug=@Debug(config=MixinDebug.class))
	public static class M_NoInheritDebug {
		@RestGet(path="/my") public boolean my(org.apache.juneau.rest.RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug(config=HostDebug.class), mixins={M_NoDebugDeclared.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public boolean h(org.apache.juneau.rest.RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug(config=HostDebug.class), mixins={M_MixinDebug.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public boolean h(org.apache.juneau.rest.RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug(config=HostDebug.class), mixins={M_NoInheritDebug.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public boolean h(org.apache.juneau.rest.RestRequest req) { return req.isDebug(); }
	}

	@Test void a01_mixinInheritsHostDebugEnablement() throws Exception {
		var c = MockRestClient.buildJson5(HostInheritsToMixin.class);
		c.get("/h").run().assertContent("false");
		c.get("/me").run().assertContent("false");
	}

	@Test void a02_mixinOverridesHostDebugEnablement() throws Exception {
		var c = MockRestClient.buildJson5(HostWithMixinOverride.class);
		c.get("/h").run().assertContent("false");
		c.get("/my").run().assertContent("false");
	}

	@Test void a03_noInheritOnMixinUsesMixinOnly() throws Exception {
		var c = MockRestClient.buildJson5(HostWithNoInherit.class);
		c.get("/h").run().assertContent("false");
		c.get("/my").run().assertContent("false");
	}
}
