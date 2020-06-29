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
package org.apache.juneau.rest.client2;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @Body annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Remote_BodyAnnotation_Test {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}

		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	//=================================================================================================================
	// Basic tests - JSON
	//=================================================================================================================

	@Rest(parsers=JsonParser.class)
	public static class A {
		@RestMethod
		public String postX1(@Body int b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return String.valueOf(b);
		}

		@RestMethod
		public String postX2(@Body float b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return String.valueOf(b);
		}

		@RestMethod
		public String postX3(@Body Bean b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postX4(@Body Bean[] b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postX5(@Body List<Bean> b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postX6(@Body Map<String,Bean> b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postX7(@Body Reader b, @Header("Content-Type") String ct) throws Exception {
			assertEquals("text/plain",ct);
			return IOUtils.read(b);
		}

		@RestMethod
		public String postX8(@Body InputStream b, @Header("Content-Type") String ct) throws Exception {
			assertEquals("application/octet-stream",ct);
			return IOUtils.read(b);
		}

		@RestMethod
		public String postX9(@Body Reader b, @Header("Content-Type") String ct) throws Exception {
			assertTrue(ct.startsWith("text/plain"));
			return IOUtils.read(b);
		}

		@RestMethod
		public String postX10(@Body Reader b, @Header("Content-Type") String ct) throws IOException {
			assertEquals("application/x-www-form-urlencoded",ct);
			return IOUtils.read(b);
		}
	}

	@Remote
	public static interface A1 {
		String postX1(@Body int b);
		String postX2(@Body float b);
		String postX3(@Body Bean b);
		String postX4(@Body Bean[] b);
		String postX5(@Body List<Bean> b);
		String postX6(@Body Map<String,Bean> b);
		String postX7(@Body Reader b);
		String postX8(@Body InputStream b);
		String postX9(@Body HttpEntity b);
		String postX10(@Body NameValuePairs b);
	}

	@Test
	public void a01_objectTypes_json() throws Exception {
		A1 x = MockRestClient.create(A.class).serializer(JsonSerializer.class).build().getRemote(A1.class);
		assertEquals("1",x.postX1(1));
		assertEquals("1.0",x.postX2(1f));
		assertEquals("{f:1}",x.postX3(Bean.create()));
		assertEquals("[{f:1}]",x.postX4(new Bean[]{Bean.create()}));
		assertEquals("[{f:1}]",x.postX5(AList.of(Bean.create())));
		assertEquals("{k1:{f:1}}",x.postX6(AMap.of("k1",Bean.create())));
		assertEquals("xxx",x.postX7(new StringReader("xxx")));
		assertEquals("xxx",x.postX8(new StringInputStream("xxx")));
		assertEquals("xxx",x.postX9(new StringEntity("xxx")));
		assertEquals("foo=bar",x.postX10(new NameValuePairs().append("foo","bar")));
	}

	//=================================================================================================================
	// Basic tests - OpenAPI
	//=================================================================================================================

	@Rest
	public static class B implements BasicOpenApiRest {
		@RestMethod
		public Object postX1(@Body int b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return b;
		}

		@RestMethod
		public Object postX2(@Body float b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return b;
		}

		@RestMethod
		public String postX3(@Body Bean b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postX4(@Body Bean[] b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postX5(@Body List<Bean> b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postX6(@Body Map<String,Bean> b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postX7(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/plain",ct);
			return b;
		}

		@RestMethod
		public Object postX8(@Body InputStream b, @Header("Content-Type") String ct) {
			assertEquals("application/octet-stream",ct);
			return b;
		}

		@RestMethod
		public Object postX9(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/plain",ct);
			return b;
		}

		@RestMethod
		public Object postX10(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("application/x-www-form-urlencoded",ct);
			return b;
		}
	}
	@Remote
	public static interface B1 {
		String postX1(@Body int b);
		String postX2(@Body float b);
		String postX3(@Body Bean b);
		String postX4(@Body Bean[] b);
		String postX5(@Body List<Bean> b);
		String postX6(@Body Map<String,Bean> b);
		String postX7(@Body Reader b);
		String postX8(@Body InputStream b);
		String postX9(@Body HttpEntity b);
		String postX10(@Body NameValuePairs b);
	}

	@Test
	public void b01_objectTypes_openApi() throws Exception {
		B1 x = MockRestClient.create(B.class).openApi().contentType(null).build().getRemote(B1.class);
		assertEquals("1",x.postX1(1));
		assertEquals("1.0",x.postX2(1f));
		assertEquals("{f:1}",x.postX3(Bean.create()));
		assertEquals("[{f:1}]",x.postX4(new Bean[]{Bean.create()}));
		assertEquals("[{f:1}]",x.postX5(AList.of(Bean.create())));
		assertEquals("{k1:{f:1}}",x.postX6(AMap.of("k1",Bean.create())));
		assertEquals("xxx",x.postX7(new StringReader("xxx")));
		assertEquals("xxx",x.postX8(new StringInputStream("xxx")));
		assertEquals("xxx",x.postX9(new StringEntity("xxx",org.apache.http.entity.ContentType.create("text/plain"))));
		assertEquals("foo=bar",x.postX10(new NameValuePairs().append("foo","bar")));
	}

	//=================================================================================================================
	// Basic tests - OpenAPI, overridden Content-Type
	//=================================================================================================================

	@Rest
	public static class C {
		@RestMethod
		public Reader postX1(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX2(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX3(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX5(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX6(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX7(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX8(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX9(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestMethod
		public Reader postX10(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
	}
	@Remote
	public static interface C1 {
		String postX1(@Body int b);
		String postX2(@Body float b);
		String postX3(@Body Bean b);
		String postX4(@Body Bean[] b);
		String postX5(@Body List<Bean> b);
		String postX6(@Body Map<String,Bean> b);
		String postX7(@Body Reader b);
		String postX8(@Body InputStream b);
		String postX9(@Body HttpEntity b);
		String postX10(@Body NameValuePairs b);
	}

	@Test
	public void c01_openApi_overriddenContentType() throws Exception {
		C1 x = MockRestClient.create(C.class).parser(JsonParser.class).contentType("text/foo").build().getRemote(C1.class);
		assertEquals("1",x.postX1(1));
		assertEquals("1.0",x.postX2(1f));
		assertEquals("{f:1}",x.postX3(Bean.create()));
		assertEquals("[{f:1}]",x.postX5(AList.of(Bean.create())));
		assertEquals("{k1:{f:1}}",x.postX6(AMap.of("k1",Bean.create())));
		assertEquals("xxx",x.postX7(new StringReader("xxx")));
		assertEquals("xxx",x.postX8(new StringInputStream("xxx")));
		assertEquals("xxx",x.postX9(new StringEntity("xxx",org.apache.http.entity.ContentType.create("text/plain"))));
		assertEquals("foo=bar",x.postX10(new NameValuePairs().append("foo","bar")));
	}
}