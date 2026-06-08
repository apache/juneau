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
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class Rest_Mixins_Test extends TestBase {

	@Rest
	public static class A_MixinB {
		@RestGet(path="/b")
		public String b() {
			return "b";
		}
	}

	@Rest(mixins={A_MixinB.class})
	public static class A_MixinA {
		@RestGet(path="/a")
		public String a() {
			return "a";
		}
	}

	@Rest(mixins={A_MixinA.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/root")
		public String root() {
			return "root";
		}
	}

	@Rest
	public static class B_MixinC {
		@RestGet(path="/c")
		public String c() {
			return "c";
		}
	}

	@Rest(mixins={A_MixinA.class,B_MixinC.class})
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_mixinsExposeOperations() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/root").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"root\"");
		c.get("/a").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"a\"");
		c.get("/b").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"b\"");
	}

	@Test void a02_multipleMixinsExposeOperations() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/a").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"a\"");
		c.get("/b").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"b\"");
		c.get("/c").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"c\"");
	}
}
