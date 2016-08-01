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
 * Validates correct parser is used.
 */
@RestResource(
	path="/testErrorConditions"
)
@SuppressWarnings("unused")
public class TestErrorConditions extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test non-existent properties
	//====================================================================================================
	@RestMethod(name="PUT", path="/testNonExistentBeanProperties")
	public String testNonExistentBeanProperties(@Content Test1 in) {
		return "OK";
	}

	public static class Test1 {
		public String f1;
	}

	//====================================================================================================
	// Test trying to set properties to wrong data type
	//====================================================================================================
	@RestMethod(name="PUT", path="/testWrongDataType")
	public String testWrongDataType(@Content Test2 in) {
		return "OK";
	}

	public static class Test2 {
		public int f1;
	}

	//====================================================================================================
	// Test trying to parse into class with non-public no-arg constructor.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParseIntoNonConstructableBean")
	public String testParseIntoNonConstructableBean(@Content Test3a in) {
		return "OK";
	}

	public static class Test3a {
		public int f1;
		private Test3a(){}
	}

	//====================================================================================================
	// Test trying to parse into non-static inner class
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParseIntoNonStaticInnerClass")
	public String testParseIntoNonStaticInnerClass(@Content Test3b in) {
		return "OK";
	}

	public class Test3b {
		public Test3b(){}
	}

	//====================================================================================================
	// Test trying to parse into non-public inner class
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParseIntoNonPublicInnerClass")
	public String testParseIntoNonPublicInnerClass(@Content Test3b1 in) {
		return "OK";
	}

	static class Test3b1 {
		public Test3b1(){}
	}

	//====================================================================================================
	// Test exception thrown during bean construction.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testThrownConstructorException")
	public String testThrownConstructorException(@Content Test3c in) {
		return "OK";
	}

	public static class Test3c {
		public int f1;
		private Test3c(){}
		public static Test3c valueOf(String s) {
			throw new RuntimeException("Test error");
		}
	}

	//====================================================================================================
	// Test trying to set parameters to invalid types.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testSetParameterToInvalidTypes/{a1}")
	public String testSetParameterToInvalidTypes(@Param("p1") int t1, @Attr int a1, @Header("h1") int h1) {
		return "OK";
	}

	//====================================================================================================
	// Test SC_NOT_FOUND & SC_METHOD_NOT_ALLOWED
	//====================================================================================================
	@RestMethod(name="GET", path="/test404and405")
	public String test404and405() {
		return "OK";
	}

	//====================================================================================================
	// Test SC_PRECONDITION_FAILED
	//====================================================================================================
	@RestMethod(name="GET", path="/test412", matchers=NeverMatcher.class)
	public String test412() {
		return "OK";
	}

	public static class NeverMatcher extends RestMatcher {
		@Override /* RestMatcher */
		public boolean matches(RestRequest req) {
			return false;
		}
	}
}
