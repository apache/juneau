/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.core.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

@RestResource(
	path="/testuris",
	children={
		TestUris.Child.class
	}
)
public class TestUris extends RestServletDefault {
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