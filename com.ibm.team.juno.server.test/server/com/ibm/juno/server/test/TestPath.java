/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 * Tests the RestServlet.getPath() method.
 */
@RestResource(
	path="/testPath",
	children={
		TestPath.TestPath2.class
	}
)
public class TestPath extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@RestMethod(name="GET", path="/")
	public String doGet() {
		return getPath();
	}

	@RestResource(
		path="/testPath2",
		children={
			TestPath.TestPath3.class
		}
	)
	public static class TestPath2 extends RestServletDefault {
		private static final long serialVersionUID = 1L;
		// Basic tests
		@RestMethod(name="GET", path="/")
		public String doGet() {
			return getPath();
		}
	}

	@RestResource(
		path="/testPath3"
	)
	public static class TestPath3a extends RestServletDefault {
		private static final long serialVersionUID = 1L;
		// Basic tests
		@RestMethod(name="GET", path="/")
		public String doGet() {
			return getPath();
		}
	}

	public static class TestPath3 extends TestPath3a {
		private static final long serialVersionUID = 1L;
	}
}
