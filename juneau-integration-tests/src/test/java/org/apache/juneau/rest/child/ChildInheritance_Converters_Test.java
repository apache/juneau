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
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 3 &mdash; proves the additive-security PREPEND shape for {@code @Child(converters=...)}: the
 * host-seeded converter applies when the child declares none of its own, and runs BEFORE the child's own
 * declared converter when both are present (prepend order, verified via string-append ordering).
 */
class ChildInheritance_Converters_Test extends TestBase {

	public static class SeedConv implements RestConverter {
		@Override public Object convert(RestRequest req, Object res) throws BasicHttpException, SerializeException {
			return res + "+seed";
		}
	}

	public static class ChildConv implements RestConverter {
		@Override public Object convert(RestRequest req, Object res) throws BasicHttpException, SerializeException {
			return res + "+child";
		}
	}

	//------------------------------------------------------------------------------------------------------------
	// a01: child with no converters of its own -> the seeded converter applies.
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/noconv")
	public static class ChildNoConv {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildNoConv.class, converters=SeedConv.class))
	public static class HostSeedsConvOnly extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_seededConverterAppliesWhenChildHasNone() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsConvOnly.class);
		c.get("/noconv/me").accept("text/plain").run().assertStatus(200).assertContent("me+seed");
	}

	//------------------------------------------------------------------------------------------------------------
	// a02: child declares its own converter too -> BOTH apply, seed's converter runs first (prepend order).
	//------------------------------------------------------------------------------------------------------------

	@Rest(path="/bothconv", converters=ChildConv.class)
	public static class ChildWithOwnConv {
		@RestGet(path="/me") public String me() { return "me"; }
	}

	@Rest(childrenDefs=@Child(type=ChildWithOwnConv.class, converters=SeedConv.class))
	public static class HostSeedsConvOrder extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_seedRunsBeforeChildsOwnConverter() throws Exception {
		var c = MockRestClient.buildLax(HostSeedsConvOrder.class);
		c.get("/bothconv/me").accept("text/plain").run().assertStatus(200).assertContent("me+seed+child");
	}
}
