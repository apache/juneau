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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.util.*;

import javax.servlet.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Validates the behavior of the @RestHook(INIT/POST_INIT/POST_INIT_CHILD_FIRST) annotations.
 */
@RestResource(
	path="/testRestHooksInit",
	children={
		RestHooksInitResource.Super.class,
		RestHooksInitResource.Sub.class
	}
)
public class RestHooksInitResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestResource(
		path="/super"
	)
	public static class Super extends RestServletDefault {
		private static final long serialVersionUID = 1L;

		protected List<String> init = new ArrayList<String>();
		protected List<String> postInit = new ArrayList<String>();
		protected List<String> postInitChildFirst = new ArrayList<String>();

		@RestHook(INIT)
		public void init1c(RestConfig config) {
			init.add("super-1c");
		}

		@RestHook(INIT)
		public void init1a(ServletConfig config) {
			init.add("super-1a");
		}

		@RestHook(INIT)
		public void init1b() {
			init.add("super-1b");
		}

		@RestHook(INIT)
		public void init2a() {
			init.add("super-2a");
		}

		@RestHook(POST_INIT)
		public void postInit1c(RestContext context) {
			postInit.add("super-1c");
		}

		@RestHook(POST_INIT)
		public void postInit1a(RestContext context) {
			postInit.add("super-1a");
		}

		@RestHook(POST_INIT)
		public void postInit1b() {
			postInit.add("super-1b");
		}

		@RestHook(POST_INIT)
		public void postInit2a() {
			postInit.add("super-2a");
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1c(RestContext context) {
			postInitChildFirst.add("super-1c");
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1a(RestContext context) {
			postInitChildFirst.add("super-1a");
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1b() {
			postInitChildFirst.add("super-1b");
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst2a() {
			postInitChildFirst.add("super-2a");
		}

		@RestMethod(name="GET", path="/init")
		public List<String> getInitEvents() {
			return init;
		}

		@RestMethod(name="GET", path="/postInit")
		public List<String> getPostInitEvents() {
			return postInit;
		}

		@RestMethod(name="GET", path="/postInitChildFirst")
		public List<String> getPostInitChildFirstEvents() {
			return postInitChildFirst;
		}
	}

	@RestResource(
		path="/sub",
		children={
			Child.class
		}
	)
	public static class Sub extends Super {
		private static final long serialVersionUID = 1L;

		protected static String postInitOrderTest;
		protected static String postInitChildFirstOrderTest;

		@Override
		@RestHook(INIT)
		public void init1c(RestConfig config) {
			init.add("sub-1c");
		}

		@Override
		@RestHook(INIT)
		public void init1a(ServletConfig config) {
			init.add("sub-1a");
		}

		@Override
		@RestHook(INIT)
		public void init1b() {
			init.add("sub-1b");
		}

		@RestHook(INIT)
		public void init2b() {
			init.add("sub-2b");
		}

		@Override
		@RestHook(POST_INIT)
		public void postInit1c(RestContext context) {
			postInit.add("sub-1c");
		}

		@Override
		@RestHook(POST_INIT)
		public void postInit1a(RestContext context) {
			postInit.add("sub-1a");
		}

		@Override
		@RestHook(POST_INIT)
		public void postInit1b() {
			postInit.add("sub-1b");
		}

		@RestHook(POST_INIT)
		public void postInit2b() {
			postInit.add("sub-2b");
		}

		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1c(RestContext context) {
			postInitChildFirst.add("sub-1c");
		}

		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1a(RestContext context) {
			postInitChildFirst.add("sub-1a");
		}

		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1b() {
			postInitChildFirst.add("sub-1b");
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst2b() {
			postInitChildFirst.add("sub-2b");
		}

		@RestHook(POST_INIT)
		public void postInitOrderTestSub() {
			postInitOrderTest = "PARENT";
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirstOrderTestSub() {
			postInitChildFirstOrderTest = "PARENT";
		}

		@RestMethod(name="GET", path="/postInitOrder")
		public String postInitOrderTest() {
			return postInitOrderTest;
		}

		@RestMethod(name="GET", path="/postInitChildFirstOrder")
		public String postInitChildFirstOrderTest() {
			return postInitChildFirstOrderTest;
		}
	}

	@RestResource(
		path="/child"
	)
	public static class Child extends Super {
		private static final long serialVersionUID = 1L;

		@Override
		@RestHook(INIT)
		public void init1c(RestConfig config) {
			init.add("child-1c");
		}

		@RestHook(INIT)
		public void init2b() {
			init.add("child-2b");
		}

		@Override
		@RestHook(POST_INIT)
		public void postInit1c(RestContext context) {
			postInit.add("child-1c");
		}

		@RestHook(POST_INIT)
		public void postInit2b() {
			postInit.add("child-2b");
		}

		@Override
		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst1c(RestContext context) {
			postInitChildFirst.add("child-1c");
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirst2b() {
			postInitChildFirst.add("child-2b");
		}

		@RestHook(POST_INIT)
		public void postInitOrderTestSub() {
			Sub.postInitOrderTest = "CHILD";
		}

		@RestHook(POST_INIT_CHILD_FIRST)
		public void postInitChildFirstOrderTestSub() {
			Sub.postInitChildFirstOrderTest = "CHILD";
		}
	}
}
