// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.annotation;

import static org.junit.runners.MethodSorters.*;

import javax.servlet.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestInit_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestInit
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={A_Super.class,A_Sub.class})
	public static class A {}

	@Rest(path="/super")
	public static class A_Super {
		protected JsonList events = new JsonList();
		@RestInit
		public void init1c(RestContext.Builder builder) {
			events.add("super-1c");
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
	}

	@Rest(path="/sub", children={A_Child.class})
	public static class A_Sub extends A_Super {
		@Override
		@RestInit
		public void init1c(RestContext.Builder builder) {
			events.add("sub-1c");
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
		public void init1c(RestContext.Builder builder) {
			events.add("child-1c");
		}
		@RestInit
		public void init2b() {
			events.add("child-2b");
		}
	}

	@Test
	public void a01_init() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/super/events").run().assertContent("['super-1a','super-1b','super-1c','super-2a']");
		a.get("/sub/events").run().assertContent("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		a.get("/sub/child/events").run().assertContent("['super-1a','super-1b','child-1c','super-2a','child-2b']");
	}
}
