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
package org.apache.juneau.rest.headers;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @RestResource(clientVersion) attribute.
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientVersionTest {

	//=================================================================================================================
	// Basic tests - default X-Client-Version header.
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod(name=GET, path="/")
		public String test0() {
			return "no-version";
		}
		@RestMethod(name=GET, path="/", clientVersion="[0.0,1.0)")
		public String test1() {
			return "[0.0,1.0)";
		}
		@RestMethod(name=GET, path="/", clientVersion="[1.0,1.0]")
		public String test2() {
			return "[1.0,1.0]";
		}
		@RestMethod(name=GET, path="/", clientVersion="[1.1,2)")
		public String test3() {
			return "[1.1,2)";
		}
		@RestMethod(name=GET, path="/", clientVersion="2")
		public String test4() {
			return "2";
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_defaultHeader() throws Exception {
		a.get("/").execute().assertBody("no-version");
		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			a.get("/").clientVersion(s).execute().assertBody("[1.0,1.0]");
		}
		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			a.get("/").clientVersion(s).execute().assertBody("[1.1,2)");
		}
		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			a.get("/").clientVersion(s).execute().assertBody("2");
		}
	}

	//=================================================================================================================
	// Basic tests - Custom-Client-Version header.
	//=================================================================================================================

	@RestResource(clientVersionHeader="Custom-Client-Version")
	public static class B {
		@RestMethod(name=GET, path="/")
		public String test0() {
			return "no-version";
		}
		@RestMethod(name=GET, path="/", clientVersion="[0.0,1.0)")
		public String test1() {
			return "[0.0,1.0)";
		}
		@RestMethod(name=GET, path="/", clientVersion="[1.0,1.0]")
		public String test2() {
			return "[1.0,1.0]";
		}
		@RestMethod(name=GET, path="/", clientVersion="[1.1,2)")
		public String test3() {
			return "[1.1,2)";
		}
		@RestMethod(name=GET, path="/", clientVersion="2")
		public String test4() {
			return "2";
		}
	}
	static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01_testCustomHeader() throws Exception {
		b.get("/").execute().assertBody("no-version");
		for (String s : "0, 0.0, 0.1, .1, .9, .99".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).execute().assertBody("[0.0,1.0)");
		}
		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).execute().assertBody("[1.0,1.0]");
		}
		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).execute().assertBody("[1.1,2)");
		}
		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).execute().assertBody("2");
		}
	}
}
