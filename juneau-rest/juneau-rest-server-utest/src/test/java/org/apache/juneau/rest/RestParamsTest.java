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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.config.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestParamsTest {

	//=================================================================================================================
	// Various parameters
	//=================================================================================================================

	@Rest(messages="RestParamsTest")
	public static class A {
		@RestMethod
		public String a01(ResourceBundle t) {
			return t == null ? null : t.getString("foo");
		}
		@RestMethod
		public String a02(Messages t) {
			return t == null ? null : t.getString("foo");
		}
		@RestMethod(name=POST)
		public String a03(InputStream t) throws IOException {
			return read(t);
		}
		@RestMethod(name=POST)
		public String a04(ServletInputStream t) throws IOException {
			return read(t);
		}
		@RestMethod(name=POST)
		public String a05(Reader t) throws IOException {
			return read(t);
		}
		@RestMethod
		public void a06(OutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestMethod
		public void a07(ServletOutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestMethod
		public void a08(Writer t) throws IOException {
			t.write("OK");
		}
		@RestMethod
		public boolean a09(RequestHeaders t) {
			return t != null;
		}
		@RestMethod
		public boolean a10(RequestQuery t) {
			return t != null;
		}
		@RestMethod
		public boolean a11(RequestFormData t) {
			return t != null;
		}
		@RestMethod
		public String a12(@Method String t) {
			return t;
		}
		@SuppressWarnings("deprecation")
		@RestMethod
		public boolean a13(RestLogger t) {
			return t != null;
		}
		@RestMethod
		public boolean a14(RestContext t) {
			return t != null;
		}
		@RestMethod(parsers={JsonParser.class})
		public String a15(Parser t) {
			return t.getClass().getName();
		}
		@RestMethod
		public String a16(Locale t) {
			return t.toString();
		}
		@RestMethod
		public boolean a17(Swagger t) {
			return t != null;
		}
		@RestMethod
		public boolean a18(RequestPath t) {
			return t != null;
		}
		@RestMethod
		public boolean a19(RequestBody t) {
			return t != null;
		}
		@RestMethod
		public boolean a20(Config t) {
			return t != null;
		}
	}
	static MockRestClient a = MockRestClient.build(A.class);

	@Test
	public void a01_ResourceBundle() throws Exception {
		a.get("/a01").acceptLanguage("en-US").run().assertBody().is("bar");
		a.get("/a01").acceptLanguage("ja-JP").run().assertBody().is("baz");
	}
	@Test
	public void a02_MessageBundle() throws Exception {
		a.get("/a02").acceptLanguage("en-US").run().assertBody().is("bar");
		a.get("/a02").acceptLanguage("ja-JP").run().assertBody().is("baz");
	}
	@Test
	public void a03_InputStream() throws Exception {
		a.post("/a03", "foo").run().assertBody().is("foo");
	}
	@Test
	public void a04_ServletInputStream() throws Exception {
		a.post("/a04", "foo").run().assertBody().is("foo");
	}
	@Test
	public void a05_Reader() throws Exception {
		a.post("/a05", "foo").run().assertBody().is("foo");
	}
	@Test
	public void a06_OutputStream() throws Exception {
		a.get("/a06").run().assertBody().is("OK");
	}
	@Test
	public void a07_ServletOutputStream() throws Exception {
		a.get("/a07").run().assertBody().is("OK");
	}
	@Test
	public void a08_Writer() throws Exception {
		a.get("/a08").run().assertBody().is("OK");
	}
	@Test
	public void a09_RequestHeaders() throws Exception {
		a.get("/a09").run().assertBody().is("true");
	}
	@Test
	public void a10_RequestQuery() throws Exception {
		a.get("/a10").run().assertBody().is("true");
	}
	@Test
	public void a11_RequestFormData() throws Exception {
		a.get("/a11").run().assertBody().is("true");
	}
	@Test
	public void a12_HttpMethod() throws Exception {
		a.get("/a12").run().assertBody().is("GET");
	}
	@Test
	public void a13_RestLogger() throws Exception {
		a.get("/a13").run().assertBody().is("true");
	}
	@Test
	public void a14_RestContext() throws Exception {
		a.get("/a14").run().assertBody().is("true");
	}
	@Test
	public void a15_Parser() throws Exception {
		a.get("/a15").contentType("application/json").run().assertBody().is("org.apache.juneau.json.JsonParser");
	}
	@Test
	public void a16_Locale() throws Exception {
		a.get("/a16").acceptLanguage("en-US").run().assertBody().is("en_US");
		a.get("/a16").acceptLanguage("ja-JP").run().assertBody().is("ja_JP");
	}
	@Test
	public void a17_Swagger() throws Exception {
		a.get("/a17").run().assertBody().is("true");
	}
	@Test
	public void a18_RequestPathMatch() throws Exception {
		a.get("/a18").run().assertBody().is("true");
	}
	@Test
	public void a19_RequestBody() throws Exception {
		a.get("/a19").run().assertBody().is("true");
	}
	@Test
	public void a20_Config() throws Exception {
		a.get("/a20").run().assertBody().is("true");
	}
}
