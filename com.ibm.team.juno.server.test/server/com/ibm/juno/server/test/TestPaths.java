/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.core.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

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
