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
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 2 regression matrix &mdash; verifies that {@code @Rest(converters=...)} on a mixin class inherits the host's
 * converter chain via the {@link RestContext#getRestAnnotationsForProperty(String) annotation-property walk}
 *.
 *
 * <p>
 * Each test converter appends a tag to the response string. Inherited host converters fire before mixin-declared
 * ones (parent-to-child walk order). Host endpoints are exercised in parallel to confirm the host chain isn't
 * polluted by mixin-only contributions.
 */
@SuppressWarnings("resource")  // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
class MixinInheritance_Converters_Test extends TestBase {

	public static class HostConv1 implements RestConverter {
		@Override public Object convert(RestRequest req, Object res) throws BasicHttpException, SerializeException {
			return res + "+host1";
		}
	}

	public static class MixinConv1 implements RestConverter {
		@Override public Object convert(RestRequest req, Object res) throws BasicHttpException, SerializeException {
			return res + "+mixin1";
		}
	}

	@Rest
	public static class M_Empty {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(converters={MixinConv1.class})
	public static class M_AppendsMixinConv {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(noInherit="converters", converters={MixinConv1.class})
	public static class M_NoInheritConv {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(converters={HostConv1.class}, mixins={M_Empty.class})
	public static class HostBasic extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(converters={HostConv1.class}, mixins={M_AppendsMixinConv.class})
	public static class HostWithMixinConv extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(converters={HostConv1.class}, mixins={M_NoInheritConv.class})
	public static class HostWithNoInheritMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_mixinInheritsHostConverter() throws Exception {
		var c = MockRestClient.buildLax(HostBasic.class);
		c.get("/h").accept("text/plain").run().assertStatus(200).assertContent("h+host1");
		c.get("/me").accept("text/plain").run().assertStatus(200).assertContent("me+host1");
	}

	@Test void a02_mixinAppendsConverterAfterInheritedHost() throws Exception {
		var c = MockRestClient.buildLax(HostWithMixinConv.class);
		c.get("/h").accept("text/plain").run().assertStatus(200).assertContent("h+host1");
		c.get("/my").accept("text/plain").run().assertStatus(200).assertContent("my+host1+mixin1");
	}

	@Test void a03_noInheritDropsHostConverter() throws Exception {
		var c = MockRestClient.buildLax(HostWithNoInheritMixin.class);
		c.get("/h").accept("text/plain").run().assertStatus(200).assertContent("h+host1");
		c.get("/my").accept("text/plain").run().assertStatus(200).assertContent("my+mixin1");
	}
}
