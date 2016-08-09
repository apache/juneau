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

import org.apache.juneau.microservice.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testClientVersion",
	children={
		TestClientVersion.DefaultHeader.class,
		TestClientVersion.CustomHeader.class
	}
)
@SuppressWarnings("serial")
public class TestClientVersion extends Resource {

	@RestResource(
		path="/defaultHeader"
	)
	public static class DefaultHeader extends Resource {

		@RestMethod(name="GET", path="/")
		public String test0() {
			return "no-version";
		}

		@RestMethod(name="GET", path="/", clientVersion="[0.0,1.0)")
		public String test1() {
			return "[0.0,1.0)";
		}

		@RestMethod(name="GET", path="/", clientVersion="[1.0,1.0]")
		public String test2() {
			return "[1.0,1.0]";
		}

		@RestMethod(name="GET", path="/", clientVersion="[1.1,2)")
		public String test3() {
			return "[1.1,2)";
		}

		@RestMethod(name="GET", path="/", clientVersion="2")
		public String test4() {
			return "2";
		}
	}

	@RestResource(
		path="/customHeader",
		clientVersionHeader="Custom-Client-Version"
	)
	public static class CustomHeader extends Resource {

		@RestMethod(name="GET", path="/")
		public String test0() {
			return "no-version";
		}

		@RestMethod(name="GET", path="/", clientVersion="[0.0,1.0)")
		public String test1() {
			return "[0.0,1.0)";
		}

		@RestMethod(name="GET", path="/", clientVersion="[1.0,1.0]")
		public String test2() {
			return "[1.0,1.0]";
		}

		@RestMethod(name="GET", path="/", clientVersion="[1.1,2)")
		public String test3() {
			return "[1.1,2)";
		}

		@RestMethod(name="GET", path="/", clientVersion="2")
		public String test4() {
			return "2";
		}
	}
}
