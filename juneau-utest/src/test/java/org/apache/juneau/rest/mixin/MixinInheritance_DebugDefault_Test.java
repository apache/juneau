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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/** Regression matrix for typed `@Debug` inheritance on mixins. */
class MixinInheritance_DebugDefault_Test extends TestBase {

	@Rest
	public static class M_NoDebugDefault {
		@RestGet(path="/me") public boolean me(RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug("always"))
	public static class M_OverridesAlways {
		@RestGet(path="/my") public boolean my(RestRequest req) { return req.isDebug(); }
	}

	@Rest(noInherit="debug", debug=@Debug("always"))
	public static class M_NoInheritAlways {
		@RestGet(path="/my") public boolean my(RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug("conditional"), mixins={M_NoDebugDefault.class})
	public static class HostInheritsToMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public boolean h(RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug("conditional"), mixins={M_OverridesAlways.class})
	public static class HostWithMixinOverride extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public boolean h(RestRequest req) { return req.isDebug(); }
	}

	@Rest(debug=@Debug("conditional"), mixins={M_NoInheritAlways.class})
	public static class HostWithNoInherit extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public boolean h(RestRequest req) { return req.isDebug(); }
	}

	@Test void a01_mixinInheritsHostDebugDefault() throws Exception {
		var c = MockRestClient.buildJson5(HostInheritsToMixin.class);
		var cd = MockRestClient.create(HostInheritsToMixin.class).json5().debug().suppressLogging().build();
		c.get("/h").run().assertContent("false");
		c.get("/me").run().assertContent("false");
		cd.get("/h").run().assertContent("true");
		cd.get("/me").run().assertContent("true");
	}

	@Test void a02_mixinDebugDefaultOverridesHost() throws Exception {
		var c = MockRestClient.buildJson5(HostWithMixinOverride.class);
		var cd = MockRestClient.create(HostWithMixinOverride.class).json5().debug().suppressLogging().build();
		c.get("/h").run().assertContent("false");
		c.get("/my").run().assertContent("true");
		cd.get("/h").run().assertContent("true");
		cd.get("/my").run().assertContent("true");
	}

	@Test void a03_noInheritOnMixinUsesMixinOnly() throws Exception {
		var c = MockRestClient.buildJson5(HostWithNoInherit.class);
		var cd = MockRestClient.create(HostWithNoInherit.class).json5().debug().suppressLogging().build();
		c.get("/h").run().assertContent("false");
		c.get("/my").run().assertContent("true");
		cd.get("/h").run().assertContent("true");
		cd.get("/my").run().assertContent("true");
	}
}
