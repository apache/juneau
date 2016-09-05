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
package org.apache.juneau.server;

import org.apache.juneau.*;
import org.apache.juneau.server.annotation.*;

@RestResource(
	path="/testuris",
	children={
		UrisResource.Child.class
	}
)
public class UrisResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/*")
	public ObjectMap test1(RestRequest req) throws Exception {
		return getPathInfoObject(req).append("testMethod", "root.test1");
	}

	@RestMethod(name="GET", path="/test2/*")
	public ObjectMap test2(RestRequest req) throws Exception {
		return getPathInfoObject(req).append("testMethod", "root.test2");
	}

	@RestMethod(name="GET", path="/test3%2Ftest3/*")
	public ObjectMap test3(RestRequest req) throws Exception {
		return getPathInfoObject(req).append("testMethod", "root.test3");
	}

	@RestMethod(name="GET", path="/test4/test4/*")
	public ObjectMap test4(RestRequest req) throws Exception {
		return getPathInfoObject(req).append("testMethod", "root.test4");
	}

	@RestResource(
		path="/child",
		children={
			GrandChild.class
		}
	)
	public static class Child extends RestServletDefault {
		private static final long serialVersionUID = 1L;

		@RestMethod(name="GET", path="/*")
		public ObjectMap test1(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "child.test1");
		}

		@RestMethod(name="GET", path="/test2/*")
		public ObjectMap test2(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "child.test2");
		}

		@RestMethod(name="GET", path="/test3%2Ftest3/*")
		public ObjectMap test3(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "child.test3");
		}

		@RestMethod(name="GET", path="/test4/test4/*")
		public ObjectMap test4(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "child.test4");
		}
	}

	@RestResource(
		path="/grandchild"
	)
	public static class GrandChild extends RestServletDefault {
		private static final long serialVersionUID = 1L;

		@RestMethod(name="GET", path="/*")
		public ObjectMap test1(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "grandchild.test1");
		}

		@RestMethod(name="GET", path="/test2/*")
		public ObjectMap test2(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "grandchild.test2");
		}

		@RestMethod(name="GET", path="/test3%2Ftest3/*")
		public ObjectMap test3(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "grandchild.test3");
		}

		@RestMethod(name="GET", path="/test4/test4/*")
		public ObjectMap test4(RestRequest req) throws Exception {
			return getPathInfoObject(req).append("testMethod", "grandchild.test4");
		}
	}

	static ObjectMap getPathInfoObject(RestRequest req) throws Exception {
		ObjectMap m = new ObjectMap();
		m.put("contextPath", req.getContextPath());
		m.put("pathInfo", req.getPathInfo());
		m.put("pathRemainder", req.getPathRemainder());
		m.put("pathTranslated", req.getPathTranslated());
		m.put("requestParentURI", req.getRequestParentURI());
		m.put("requestURI", req.getRequestURI());
		m.put("requestURL", req.getRequestURL());
		m.put("servletPath", req.getServletPath());
		m.put("servletURI", req.getServletURI());
		m.put("testURL1", req.getURL("testURL"));
		m.put("testURL2", req.getURL("/testURL"));
		m.put("testURL3", req.getURL("http://testURL"));
		return m;
	}
}