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

import javax.servlet.http.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Validates the behavior of the @RestHook(START/PRE/POST) annotations.
 */
@RestResource(
	path="/testRestHooks",
	children={
		RestHooksResource.Start.class,
		RestHooksResource.Pre.class,
		RestHooksResource.Post.class,
	}
)
public class RestHooksResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestResource(
		path="/start"
	)
	public static class Start extends StartParent {
		private static final long serialVersionUID = 1L;

		private boolean start3Called;

		@RestHook(START_CALL)
		public void start3() {
			start3Called = true;
		}

		@RestHook(START_CALL)
		public void start4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("start3-called", ""+start3Called);
			start3Called = false;
			if (res.getHeader("start4-called") != null)
				throw new RuntimeException("start4 called multiple times.");
			res.setHeader("start4-called", "true");
		}

		@RestMethod(path="/")
		public Map<String,Object> getHeaders(RestRequest req, RestResponse res) {
			return new AMap<String,Object>()
				.append("1", res.getHeader("start1-called"))
				.append("2", res.getHeader("start2-called"))
				.append("3", res.getHeader("start3-called"))
				.append("4", res.getHeader("start4-called"));
		}
	}

	public static class StartParent extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		private boolean start1Called;

		@RestHook(START_CALL)
		public void start1() {
			start1Called = true;
		}

		@RestHook(START_CALL)
		public void start2(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("start1-called", ""+start1Called);
			start1Called = false;
			if (res.getHeader("start2-called") != null)
				throw new RuntimeException("start2 called multiple times.");
			res.setHeader("start2-called", "true");
		}
	}

	@RestResource(
		path="/pre"
	)
	public static class Pre extends PreParent {
		private static final long serialVersionUID = 1L;

		private boolean pre3Called;

		@RestHook(PRE_CALL)
		public void pre3() {
			pre3Called = true;
		}

		@RestHook(PRE_CALL)
		public void pre4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("pre3-called", ""+pre3Called);
			pre3Called = false;
			if (res.getHeader("pre4-called") != null)
				throw new RuntimeException("pre4 called multiple times.");
			res.setHeader("pre4-called", "true");
		}

		@RestMethod(path="/")
		public Map<String,Object> getHeaders(RestRequest req, RestResponse res) {
			return new AMap<String,Object>()
				.append("1", res.getHeader("pre1-called"))
				.append("2", res.getHeader("pre2-called"))
				.append("3", res.getHeader("pre3-called"))
				.append("4", res.getHeader("pre4-called"));
		}
	}

	public static class PreParent extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		private boolean pre1Called;

		@RestHook(PRE_CALL)
		public void pre1() {
			pre1Called = true;
		}

		@RestHook(PRE_CALL)
		public void pre2(Accept accept, RestRequest req, RestResponse res) {
			res.setHeader("pre1-called", ""+pre1Called);
			pre1Called = false;
			if (res.getHeader("pre2-called") != null)
				throw new RuntimeException("pre2 called multiple times.");
			res.setHeader("pre2-called", "true");
		}
	}

	@RestResource(
		path="/post"
	)
	public static class Post extends PostParent {
		private static final long serialVersionUID = 1L;
		private boolean post3Called;

		@RestHook(POST_CALL)
		public void post3() {
			post3Called = true;
		}

		@RestHook(POST_CALL)
		public void post4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("post3-called", ""+post3Called);
			post3Called = false;
			if (res.getHeader("post4-called") != null)
				throw new RuntimeException("post4 called multiple times.");
			res.setHeader("post4-called", "true");
		}

		@RestMethod(path="/")
		public String doGet() {
			return "OK";
		}
	}

	public static class PostParent extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private boolean post1Called;

		@RestHook(POST_CALL)
		public void post1() {
			post1Called = true;
		}

		@RestHook(POST_CALL)
		public void post2(Accept accept, RestRequest req, RestResponse res) {
			res.setHeader("post1-called", ""+post1Called);
			post1Called = false;
			if (res.getHeader("post2-called") != null)
				throw new RuntimeException("post2 called multiple times.");
			res.setHeader("post2-called", "true");
		}
	}
}
