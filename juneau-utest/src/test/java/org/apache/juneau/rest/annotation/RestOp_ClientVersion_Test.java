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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.http.HttpMethod.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class RestOp_ClientVersion_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - default Client-Version header.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A1 {
		@RestOp(method=GET, path="/")
		public String a() {
			return "no-version";
		}
		@RestOp(method=GET, path="/", clientVersion="[0.0,1.0)")
		public String b() {
			return "[0.0,1.0)";
		}
		@RestOp(method=GET, path="/", clientVersion="[1.0,1.0]")
		public String c() {
			return "[1.0,1.0]";
		}
		@RestOp(method=GET, path="/", clientVersion="[1.1,2)")
		public String d() {
			return "[1.1,2)";
		}
		@RestOp(method=GET, path="/", clientVersion="2")
		public String e() {
			return "2";
		}
	}

	@Test void a01_defaultHeader() throws Exception {
		var a = MockRestClient.build(A1.class);
		a.get("/").run().assertContent("no-version");
		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			a.get("/").header(ClientVersion.of(s)).run().assertContent().setMsg("s=[{0}]",s).is("[1.0,1.0]");
		}
		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			a.get("/").header(ClientVersion.of(s)).run().assertContent().setMsg("s=[{0}]").is("[1.1,2)");
		}
		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			a.get("/").header(ClientVersion.of(s)).run().assertContent().setMsg("s=[{0}]").is("2");
		}
	}

	@Rest
	public static class A2 {
		@RestGet(path="/")
		public String a() {
			return "no-version";
		}
		@RestGet(path="/", clientVersion="[0.0,1.0)")
		public String b() {
			return "[0.0,1.0)";
		}
		@RestGet(path="/", clientVersion="[1.0,1.0]")
		public String c() {
			return "[1.0,1.0]";
		}
		@RestGet(path="/", clientVersion="[1.1,2)")
		public String d() {
			return "[1.1,2)";
		}
		@RestGet(path="/", clientVersion="2")
		public String e() {
			return "2";
		}
	}

	@Test void a02_defaultHeader() throws Exception {
		var a = MockRestClient.build(A2.class);
		a.get("/").run().assertContent("no-version");
		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			a.get("/").header(ClientVersion.of(s)).run().assertContent().setMsg("s=[{0}]",s).is("[1.0,1.0]");
		}
		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			a.get("/").header(ClientVersion.of(s)).run().assertContent().setMsg("s=[{0}]").is("[1.1,2)");
		}
		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			a.get("/").header(ClientVersion.of(s)).run().assertContent().setMsg("s=[{0}]").is("2");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests - Custom-Client-Version header.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(clientVersionHeader="Custom-Client-Version")
	public static class B1 {
		@RestOp(method=GET, path="/")
		public String a() {
			return "no-version";
		}
		@RestOp(method=GET, path="/", clientVersion="[0.0,1.0)")
		public String b() {
			return "[0.0,1.0)";
		}
		@RestOp(method=GET, path="/", clientVersion="[1.0,1.0]")
		public String c() {
			return "[1.0,1.0]";
		}
		@RestOp(method=GET, path="/", clientVersion="[1.1,2)")
		public String d() {
			return "[1.1,2)";
		}
		@RestOp(method=GET, path="/", clientVersion="2")
		public String e() {
			return "2";
		}
	}

	@Test void b01_testCustomHeader() throws Exception {
		var b = MockRestClient.build(B1.class);
		b.get("/").run().assertContent("no-version");
		for (String s : "0, 0.0, 0.1, .1, .9, .99".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("[0.0,1.0)");
		}
		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("[1.0,1.0]");
		}
		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("[1.1,2)");
		}
		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("2");
		}
	}

	@Rest(clientVersionHeader="Custom-Client-Version")
	public static class B2 {
		@RestGet(path="/")
		public String a() {
			return "no-version";
		}
		@RestGet(path="/", clientVersion="[0.0,1.0)")
		public String b() {
			return "[0.0,1.0)";
		}
		@RestGet(path="/", clientVersion="[1.0,1.0]")
		public String c() {
			return "[1.0,1.0]";
		}
		@RestGet(path="/", clientVersion="[1.1,2)")
		public String d() {
			return "[1.1,2)";
		}
		@RestGet(path="/", clientVersion="2")
		public String e() {
			return "2";
		}
	}

	@Test void b02_testCustomHeader() throws Exception {
		var b = MockRestClient.build(B2.class);
		b.get("/").run().assertContent("no-version");
		for (String s : "0, 0.0, 0.1, .1, .9, .99".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("[0.0,1.0)");
		}
		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("[1.0,1.0]");
		}
		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("[1.1,2)");
		}
		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			b.get("/").header("Custom-Client-Version", s).run().assertContent("2");
		}
	}
}