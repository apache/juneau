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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

class RestInit_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// @RestInit
	//
	// Pre-9.5 this test also covered `@RestInit public void initX(RestContext.Builder b)` — the framework
	// injected the in-flight RestContext.Builder so user code could imperatively configure the context.
	// That protocol was deleted in TODO-16 Phase C-3 (Builder is no longer a user-visible bean), so the
	// `init1c(RestContext.Builder)` cases were dropped along with it. The remaining cases still cover
	// no-arg invocation, ServletConfig injection, and inheritance / override ordering.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={A_Super.class,A_Sub.class})
	public static class A {}

	@Rest(path="/super")
	public static class A_Super {
		protected JsonList events = new JsonList();
		@RestInit
		public void init1d(MarkerBean marker) {
			events.add("super-1d");
		}
		@RestInit
		public void init1a(ServletConfig config) {
			events.add("super-1a");
		}
		@RestInit
		public void init1b() {
			events.add("super-1b");
		}
		@RestInit
		public void init2a() {
			events.add("super-2a");
		}
		@RestGet
		public JsonList getEvents() {
			return events;
		}

		// Bean injected via @RestInject so we can also exercise non-built-in @RestInit parameter resolution.
		@RestInject
		public MarkerBean marker() {
			return new MarkerBean();
		}

		public static class MarkerBean {}
	}

	@Rest(path="/sub", children={A_Child.class})
	public static class A_Sub extends A_Super {
		@Override
		@RestInit
		public void init1d(MarkerBean marker) {
			events.add("sub-1d");
		}
		@Override
		@RestInit
		public void init1a(ServletConfig config) {
			events.add("sub-1a");
		}
		@Override
		@RestInit
		public void init1b() {
			events.add("sub-1b");
		}
		@RestInit
		public void init2b() {
			events.add("sub-2b");
		}
	}

	@Rest(path="/child")
	public static class A_Child extends A_Super {
		@Override
		@RestInit
		public void init1d(MarkerBean marker) {
			events.add("child-1d");
		}
		@RestInit
		public void init2b() {
			events.add("child-2b");
		}
	}

	@Test void a01_init() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/super/events").run().assertContent("['super-1a','super-1b','super-1d','super-2a']");
		a.get("/sub/events").run().assertContent("['sub-1a','sub-1b','sub-1d','super-2a','sub-2b']");
		a.get("/sub/child/events").run().assertContent("['super-1a','super-1b','child-1d','super-2a','child-2b']");
	}
}