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

import org.apache.juneau.plaintext.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testOverlappingMethods",
	serializers=PlainTextSerializer.class
)
public class OverlappingMethodsResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Overlapping guards
	//====================================================================================================
	@RestMethod(name="GET", path="/testOverlappingGuards1", guards=Test1Guard.class)
	public String testOverlappingGuards1() {
		return "test1_doGet";
	}

	//====================================================================================================
	// Overlapping guards
	//====================================================================================================
	@RestMethod(name="GET", path="/testOverlappingGuards2", guards={Test1Guard.class, Test2Guard.class})
	public String testOverlappingGuards2() {
		return "test2_doGet";
	}

	public static class Test1Guard extends RestGuard {
		@Override /* RestGuard */
		public boolean isRequestAllowed(RestRequest req) {
			return req.getParameter("t1","").equals("1");
		}
	}

	public static class Test2Guard extends RestGuard {
		@Override /* RestGuard */
		public boolean isRequestAllowed(RestRequest req) {
			return req.getParameter("t2","").equals("2");
		}
	}

	//====================================================================================================
	// Overlapping matchers
	//====================================================================================================
	@RestMethod(name="GET", path="/testOverlappingMatchers1", matchers=Test3aMatcher.class)
	public String testOverlappingMatchers1() {
		return "test3a";
	}

	@RestMethod(name="GET", path="/testOverlappingMatchers1", matchers=Test3bMatcher.class)
	public String test3b_doGet() {
		return "test3b";
	}

	@RestMethod(name="GET", path="/testOverlappingMatchers1")
	public String test3c_doGet() {
		return "test3c";
	}

	public static class Test3aMatcher extends RestMatcher {
		@Override /* RestMatcher */
		public boolean matches(RestRequest req) {
			return req.getParameter("t1","").equals("1");
		}
	}

	public static class Test3bMatcher extends RestMatcher {
		@Override /* RestMatcher */
		public boolean matches(RestRequest req) {
			return req.getParameter("t2","").equals("2");
		}
	}

	//====================================================================================================
	// Overlapping matchers
	//====================================================================================================
	@RestMethod(name="GET", path="/testOverlappingMatchers2")
	public String test4a_doGet() {
		return "test4a";
	}

	@RestMethod(name="GET", path="/testOverlappingMatchers2", matchers={Test3aMatcher.class, Test3bMatcher.class})
	public String test4b_doGet() {
		return "test4b";
	}

	//====================================================================================================
	// Overlapping URL patterns
	//====================================================================================================
	@RestMethod(name="GET", path="/testOverlappingUrlPatterns")
	public String testOverlappingUrlPatterns1() {
		return "test5a";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/*")
	public String testOverlappingUrlPatterns2() {
		return "test5b";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/foo")
	public String testOverlappingUrlPatterns3() {
		return "test5c";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/foo/*")
	public String testOverlappingUrlPatterns4() {
		return "test5d";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/{id}")
	public String testOverlappingUrlPatterns5() {
		return "test5e";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/{id}/*")
	public String testOverlappingUrlPatterns6() {
		return "test5f";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/{id}/foo")
	public String testOverlappingUrlPatterns7() {
		return "test5g";
	}

	@RestMethod(name="GET", path="/testOverlappingUrlPatterns/{id}/foo/*")
	public String testOverlappingUrlPatterns8() {
		return "test5h";
	}
}
