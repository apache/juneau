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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates correct parser is used.
 */
@RestResource(
	path="/testErrorConditions"
)
public class ErrorConditionsResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test non-existent properties
	//====================================================================================================
	@RestMethod(name=PUT, path="/testNonExistentBeanProperties")
	public String testNonExistentBeanProperties(@Body Test1 in) {
		return "OK";
	}

	public static class Test1 {
		public String f1;
	}

	//====================================================================================================
	// Test trying to set properties to wrong data type
	//====================================================================================================
	@RestMethod(name=PUT, path="/testWrongDataType")
	public String testWrongDataType(@Body Test2 in) {
		return "OK";
	}

	public static class Test2 {
		public int f1;
	}

	//====================================================================================================
	// Test trying to parse into class with non-public no-arg constructor.
	//====================================================================================================
	@RestMethod(name=PUT, path="/testParseIntoNonConstructableBean")
	public String testParseIntoNonConstructableBean(@Body Test3a in) {
		return "OK";
	}

	public static class Test3a {
		public int f1;
		private Test3a(){}
	}

	//====================================================================================================
	// Test trying to parse into non-static inner class
	//====================================================================================================
	@RestMethod(name=PUT, path="/testParseIntoNonStaticInnerClass")
	public String testParseIntoNonStaticInnerClass(@Body Test3b in) {
		return "OK";
	}

	public class Test3b {
		public Test3b(){}
	}

	//====================================================================================================
	// Test trying to parse into non-public inner class
	//====================================================================================================
	@RestMethod(name=PUT, path="/testParseIntoNonPublicInnerClass")
	public String testParseIntoNonPublicInnerClass(@Body Test3b1 in) {
		return "OK";
	}

	static class Test3b1 {
		public Test3b1(){}
	}

	//====================================================================================================
	// Test exception thrown during bean construction.
	//====================================================================================================
	@RestMethod(name=PUT, path="/testThrownConstructorException")
	public String testThrownConstructorException(@Body Test3c in) {
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
	@RestMethod(name=PUT, path="/testSetParameterToInvalidTypes/{a1}")
	public String testSetParameterToInvalidTypes(@Query("p1") int t1, @Path("a1") int a1, @Header("h1") int h1) {
		return "OK";
	}

	//====================================================================================================
	// Test SC_NOT_FOUND & SC_METHOD_NOT_ALLOWED
	//====================================================================================================
	@RestMethod(name=GET, path="/test404and405")
	public String test404and405() {
		return "OK";
	}

	//====================================================================================================
	// Test SC_PRECONDITION_FAILED
	//====================================================================================================
	@RestMethod(name=GET, path="/test412", matchers=NeverMatcher.class)
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
