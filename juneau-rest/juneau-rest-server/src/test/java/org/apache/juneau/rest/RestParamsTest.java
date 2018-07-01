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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.config.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests various aspects of parameters passed to methods annotated with @RestMethod.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestParamsTest {

	//=================================================================================================================
	// Various parameters
	//=================================================================================================================

	@RestResource(messages="RestParamsTest")
	public static class A {
		@RestMethod(name=GET, path="/ResourceBundle")
		public String a01(ResourceBundle t) {
			return t == null ? null : t.getString("foo");
		}
		@RestMethod(name=GET, path="/MessageBundle")
		public String a02(MessageBundle t) {
			return t == null ? null : t.getString("foo");
		}
		@RestMethod(name=POST, path="/InputStream")
		public String a03(InputStream t) throws IOException {
			return read(t);
		}
		@RestMethod(name=POST, path="/ServletInputStream")
		public String a04(ServletInputStream t) throws IOException {
			return read(t);
		}
		@RestMethod(name=POST, path="/Reader")
		public String a05(Reader t) throws IOException {
			return read(t);
		}
		@RestMethod(name=GET, path="/OutputStream")
		public void a06(OutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestMethod(name=GET, path="/ServletOutputStream")
		public void a07(ServletOutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestMethod(name=GET, path="/Writer")
		public void a08(Writer t) throws IOException {
			t.write("OK");
		}
		@RestMethod(name=GET, path="/RequestHeaders")
		public boolean a09(RequestHeaders t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/RequestQuery")
		public boolean a10(RequestQuery t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/RequestFormData")
		public boolean a11(RequestFormData t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/HttpMethod")
		public String a12(HttpMethod t) {
			return t.toString();
		}
		@RestMethod(name=GET, path="/RestLogger")
		public boolean a13(RestLogger t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/RestContext")
		public boolean a14(RestContext t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/Parser",parsers={JsonParser.class})
		public String a15(Parser t) {
			return t.getClass().getName();
		}
		@RestMethod(name=GET, path="/Locale")
		public String a16(Locale t) {
			return t.toString();
		}
		@RestMethod(name=GET, path="/Swagger")
		public boolean a17(Swagger t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/RequestPathMatch")
		public boolean a18(RequestPathMatch t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/RequestBody")
		public boolean a19(RequestBody t) {
			return t != null;
		}
		@RestMethod(name=GET, path="/Config")
		public boolean a20(Config t) {
			return t != null;
		}
	}
	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_ResourceBundle() throws Exception {
		a.get("/ResourceBundle").acceptLanguage("en-US").execute().assertBody("bar");
		a.get("/ResourceBundle").acceptLanguage("ja-JP").execute().assertBody("baz");
	}
	@Test
	public void a02_MessageBundle() throws Exception {
		a.get("/MessageBundle").acceptLanguage("en-US").execute().assertBody("bar");
		a.get("/MessageBundle").acceptLanguage("ja-JP").execute().assertBody("baz");
	}
	@Test
	public void a03_InputStream() throws Exception {
		a.post("/InputStream", "foo").execute().assertBody("foo");
	}
	@Test
	public void a04_ServletInputStream() throws Exception {
		a.post("/ServletInputStream", "foo").execute().assertBody("foo");
	}
	@Test
	public void a05_Reader() throws Exception {
		a.post("/Reader", "foo").execute().assertBody("foo");
	}
	@Test
	public void a06_OutputStream() throws Exception {
		a.get("/OutputStream").execute().assertBody("OK");
	}
	@Test
	public void a07_ServletOutputStream() throws Exception {
		a.get("/ServletOutputStream").execute().assertBody("OK");
	}
	@Test
	public void a08_Writer() throws Exception {
		a.get("/Writer").execute().assertBody("OK");
	}
	@Test
	public void a09_RequestHeaders() throws Exception {
		a.get("/RequestHeaders").execute().assertBody("true");
	}
	@Test
	public void a10_RequestQuery() throws Exception {
		a.get("/RequestQuery").execute().assertBody("true");
	}
	@Test
	public void a11_RequestFormData() throws Exception {
		a.get("/RequestFormData").execute().assertBody("true");
	}
	@Test
	public void a12_HttpMethod() throws Exception {
		a.get("/HttpMethod").execute().assertBody("GET");
	}
	@Test
	public void a13_RestLogger() throws Exception {
		a.get("/RestLogger").execute().assertBody("true");
	}
	@Test
	public void a14_RestContext() throws Exception {
		a.get("/RestContext").execute().assertBody("true");
	}
	@Test
	public void a15_Parser() throws Exception {
		a.get("/Parser").contentType("application/json").execute().assertBody("org.apache.juneau.json.JsonParser");
	}
	@Test
	public void a16_Locale() throws Exception {
		a.get("/Locale").acceptLanguage("en-US").execute().assertBody("en_US");
		a.get("/Locale").acceptLanguage("ja-JP").execute().assertBody("ja_JP");
	}
	@Test
	public void a17_Swagger() throws Exception {
		a.get("/Swagger").execute().assertBody("true");
	}
	@Test
	public void a18_RequestPathMatch() throws Exception {
		a.get("/RequestPathMatch").execute().assertBody("true");
	}
	@Test
	public void a19_RequestBody() throws Exception {
		a.get("/RequestBody").execute().assertBody("true");
	}
	@Test
	public void a20_Config() throws Exception {
		a.get("/Config").execute().assertBody("true");
	}
}
