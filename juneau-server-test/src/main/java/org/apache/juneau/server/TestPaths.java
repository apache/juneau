/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import org.apache.juneau.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 * Tests the URL-related methods on RestRequest.
 */
@RestResource(
	path="/testPaths",
	children={
		TestPaths.A.class
	}
)
public class TestPaths extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/*")
	public ObjectMap doGet1(RestRequest req, @PathRemainder String r) {
		return getPaths(req).append("pathRemainder2", r).append("method",1);
	}

	@RestMethod(name="GET", path="/test2/*")
	public ObjectMap doGet2(RestRequest req, @PathRemainder String r) {
		return getPaths(req).append("pathRemainder2", r).append("method",2);
	}

	@RestResource(
		path="/a"
	)
	public static class A extends RestServletDefault {
		private static final long serialVersionUID = 1L;
		@RestMethod(name="GET", path="/*")
		public ObjectMap doGet1(RestRequest req, @PathRemainder String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",3);
		}
		@RestMethod(name="GET", path="/test2/*")
		public ObjectMap doGet2(RestRequest req, @PathRemainder String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",4);
		}
	}

	private static ObjectMap getPaths(RestRequest req) {
		return new ObjectMap()
			.append("pathInfo", req.getPathInfo())
			.append("pathInfoUndecoded", req.getPathInfoUndecoded())
			.append("pathInfoParts", req.getPathInfoParts())
			.append("pathRemainder", req.getPathRemainder())
			.append("pathRemainderUndecoded", req.getPathRemainderUndecoded())
			.append("requestURI", req.getRequestURI())
			.append("requestParentURI", req.getRequestParentURI())
			.append("requestURL", req.getRequestURL())
			.append("servletPath", req.getServletPath())
			.append("servletURI", req.getServletURI())
			.append("servletParentURI", req.getServletParentURI())
			.append("relativeServletURI", req.getRelativeServletURI());

	}
}
