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

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestPostInit_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestPostInit
	//------------------------------------------------------------------------------------------------------------------
	@Rest(children={A_Super.class,A_Sub.class})
	public static class A {}

	@Rest(path="/super")
	public static class A_Super {
		protected JsonList events = new JsonList();
		@RestPostInit
		public void postInit1c(RestContext context) {
			events.add("super-1c");
		}
		@RestPostInit
		public void postInit1a(RestContext context) {
			events.add("super-1a");
		}
		@RestPostInit
		public void postInit1b() {
			events.add("super-1b");
		}
		@RestPostInit
		public void postInit2a() {
			events.add("super-2a");
		}
		@RestGet
		public JsonList getEvents() {
			return events;
		}
	}

	@Rest(path="/sub",children={A_Child.class})
	public static class A_Sub extends A_Super {
		protected static String LAST_CALLED;
		@Override
		@RestPostInit
		public void postInit1c(RestContext context) {
			events.add("sub-1c");
		}
		@Override
		@RestPostInit
		public void postInit1a(RestContext context) {
			events.add("sub-1a");
		}
		@Override
		@RestPostInit
		public void postInit1b() {
			events.add("sub-1b");
		}
		@RestPostInit
		public void postInit2b() {
			events.add("sub-2b");
		}
		@RestPostInit
		public void postInitOrderTestSub() {
			LAST_CALLED = "PARENT";
		}
		@RestGet
		public String getLastCalled() {
			return LAST_CALLED;
		}
	}

	@Rest(path="/child")
	public static class A_Child extends A_Super {
		@Override
		@RestPostInit
		public void postInit1c(RestContext context) {
			events.add("child-1c");
		}
		@RestPostInit
		public void postInit2b() {
			events.add("child-2b");
		}
		@RestPostInit
		public void postInitOrderTestSub() {
			A_Sub.LAST_CALLED = "CHILD";
		}
	}

	@Test
	public void a01_postInit() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/super/events").run().assertContent("['super-1a','super-1b','super-1c','super-2a']");
		a.get("/sub/events").run().assertContent("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		a.get("/sub/child/events").run().assertContent("['super-1a','super-1b','child-1c','super-2a','child-2b']");
		a.get("/sub/lastCalled").run().assertContent("CHILD");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @RestPostInit(childFirst=true)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		children={
			B_Super.class,
			B_Sub.class
		}
	)
	public static class B {}

	@Rest(path="/super")
	public static class B_Super {
		protected JsonList events = new JsonList();
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1c(RestContext context) {
			events.add("super-1c");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1a(RestContext context) {
			events.add("super-1a");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1b() {
			events.add("super-1b");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirst2a() {
			events.add("super-2a");
		}
		@RestGet
		public JsonList getPostInitChildFirstEvents() {
			return events;
		}
	}

	@Rest(path="/sub", children={B_Child.class})
	public static class B_Sub extends B_Super {
		protected static String LAST_CALLED;
		@Override
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1c(RestContext context) {
			events.add("sub-1c");
		}
		@Override
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1a(RestContext context) {
			events.add("sub-1a");
		}
		@Override
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1b() {
			events.add("sub-1b");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirst2b() {
			events.add("sub-2b");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirstOrderTestSub() {
			LAST_CALLED = "PARENT";
		}
		@RestGet
		public String getLastCalled() {
			return LAST_CALLED;
		}
	}

	@Rest(path="/child")
	public static class B_Child extends B_Super {
		@Override
		@RestPostInit(childFirst=true)
		public void postInitChildFirst1c(RestContext context) {
			events.add("child-1c");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirst2b() {
			events.add("child-2b");
		}
		@RestPostInit(childFirst=true)
		public void postInitChildFirstOrderTestSub() {
			B_Sub.LAST_CALLED = "CHILD";
		}
	}

	@Test
	public void b01_postInitChildFirst() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.get("/super/postInitChildFirstEvents").run().assertContent("['super-1a','super-1b','super-1c','super-2a']");
		b.get("/sub/postInitChildFirstEvents").run().assertContent("['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']");
		b.get("/sub/child/postInitChildFirstEvents").run().assertContent("['super-1a','super-1b','child-1c','super-2a','child-2b']");
		b.get("/sub/lastCalled").run().assertContent("PARENT");
	}
}
