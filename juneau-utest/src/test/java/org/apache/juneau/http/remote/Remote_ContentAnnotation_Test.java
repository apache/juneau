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
package org.apache.juneau.http.remote;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.part.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

/**
 * Tests the @Body annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class Remote_ContentAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}

		@Override
		public String toString() {
			return Json5.of(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests - JSON
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(parsers=JsonParser.class)
	public static class A {
		@RestPost
		public String x1(@Content int b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return String.valueOf(b);
		}

		@RestPost
		public String x2(@Content float b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return String.valueOf(b);
		}

		@RestPost
		public String x3(@Content Bean b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return Json5Serializer.DEFAULT.toString(b);
		}

		@RestPost
		public String x4(@Content Bean[] b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return Json5Serializer.DEFAULT.toString(b);
		}

		@RestPost
		public String x5(@Content List<Bean> b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return Json5Serializer.DEFAULT.toString(b);
		}

		@RestPost
		public String x6(@Content Map<String,Bean> b, @Header("Content-Type") String ct) {
			assertEquals("application/json",ct);
			return Json5Serializer.DEFAULT.toString(b);
		}

		@RestPost
		public String x7(@Content Reader b, @Header("Content-Type") String ct) throws Exception {
			assertEquals("text/plain",ct);
			return read(b);
		}

		@RestPost
		public String x8(@Content InputStream b, @Header("Content-Type") String ct) throws Exception {
			assertEquals("application/octet-stream",ct);
			return read(b);
		}

		@RestPost
		public String x9(@Content Reader b, @Header("Content-Type") String ct) throws Exception {
			assertTrue(ct.startsWith("text/plain"));
			return read(b);
		}

		@RestPost
		public String x10(@Content Reader b, @Header("Content-Type") String ct) throws IOException {
			assertEquals("application/x-www-form-urlencoded",ct);
			return read(b);
		}
	}

	@Remote
	public static interface A1 {
		String postX1(@Content int b);
		String postX2(@Content float b);
		String postX3(@Content Bean b);
		String postX4(@Content Bean[] b);
		String postX5(@Content List<Bean> b);
		String postX6(@Content Map<String,Bean> b);
		String postX7(@Content Reader b);
		String postX8(@Content InputStream b);
		String postX9(@Content HttpEntity b);
		String postX10(@Content PartList b);
	}

	@Test
	public void a01_objectTypes_json() throws Exception {
		A1 x = MockRestClient.create(A.class).serializer(JsonSerializer.class).build().getRemote(A1.class);
		assertEquals("1",x.postX1(1));
		assertEquals("1.0",x.postX2(1f));
		assertEquals("{f:1}",x.postX3(Bean.create()));
		assertEquals("[{f:1}]",x.postX4(new Bean[]{Bean.create()}));
		assertEquals("[{f:1}]",x.postX5(alist(Bean.create())));
		assertEquals("{k1:{f:1}}",x.postX6(map("k1",Bean.create())));
		assertEquals("xxx",x.postX7(reader("xxx")));
		assertEquals("xxx",x.postX8(inputStream("xxx")));
		assertEquals("xxx",x.postX9(new StringEntity("xxx")));
		assertEquals("foo=bar",x.postX10(partList("foo","bar")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests - OpenAPI
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B implements BasicOpenApiConfig {
		@RestPost
		public Object x1(@Content int b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return b;
		}

		@RestPost
		public Object x2(@Content float b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return b;
		}

		@RestPost
		public String x3(@Content Bean b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return Json5.of(b);
		}

		@RestPost
		public Object x4(@Content Bean[] b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return Json5.of(b);
		}

		@RestPost
		public Object x5(@Content List<Bean> b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return Json5.of(b);
		}

		@RestPost
		public Object x6(@Content Map<String,Bean> b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi",ct);
			return Json5.of(b);
		}

		@RestPost
		public Object x7(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/plain",ct);
			return b;
		}

		@RestPost
		public Object x8(@Content InputStream b, @Header("Content-Type") String ct) {
			assertEquals("application/octet-stream",ct);
			return b;
		}

		@RestPost
		public Object x9(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/plain",ct);
			return b;
		}

		@RestPost
		public Object x10(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("application/x-www-form-urlencoded",ct);
			return b;
		}
	}
	@Remote
	public static interface B1 {
		String postX1(@Content int b);
		String postX2(@Content float b);
		String postX3(@Content Bean b);
		String postX4(@Content Bean[] b);
		String postX5(@Content List<Bean> b);
		String postX6(@Content Map<String,Bean> b);
		String postX7(@Content Reader b);
		String postX8(@Content InputStream b);
		String postX9(@Content HttpEntity b);
		String postX10(@Content PartList b);
	}

	@Test
	public void b01_objectTypes_openApi() throws Exception {
		B1 x = MockRestClient.create(B.class).openApi().contentType(null).build().getRemote(B1.class);
		assertEquals("1",x.postX1(1));
		assertEquals("1.0",x.postX2(1f));
		assertEquals("{f:1}",x.postX3(Bean.create()));
		assertEquals("[{f:1}]",x.postX4(new Bean[]{Bean.create()}));
		assertEquals("[{f:1}]",x.postX5(alist(Bean.create())));
		assertEquals("{k1:{f:1}}",x.postX6(map("k1",Bean.create())));
		assertEquals("xxx",x.postX7(reader("xxx")));
		assertEquals("xxx",x.postX8(inputStream("xxx")));
		assertEquals("xxx",x.postX9(new StringEntity("xxx",org.apache.http.entity.ContentType.create("text/plain"))));
		assertEquals("foo=bar",x.postX10(partList("foo","bar")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests - OpenAPI, overridden Content-Type
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestPost
		public Reader x1(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x2(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x3(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x5(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x6(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x7(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x8(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x9(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
		@RestPost
		public Reader x10(@Content Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo",ct);
			return b;
		}
	}
	@Remote
	public static interface C1 {
		String postX1(@Content int b);
		String postX2(@Content float b);
		String postX3(@Content Bean b);
		String postX4(@Content Bean[] b);
		String postX5(@Content List<Bean> b);
		String postX6(@Content Map<String,Bean> b);
		String postX7(@Content Reader b);
		String postX8(@Content InputStream b);
		String postX9(@Content HttpEntity b);
		String postX10(@Content PartList b);
	}

	@Test
	public void c01_openApi_overriddenContentType() throws Exception {
		C1 x = MockRestClient.create(C.class).parser(JsonParser.class).contentType("text/foo").build().getRemote(C1.class);
		assertEquals("1",x.postX1(1));
		assertEquals("1.0",x.postX2(1f));
		assertEquals("{f:1}",x.postX3(Bean.create()));
		assertEquals("[{f:1}]",x.postX5(alist(Bean.create())));
		assertEquals("{k1={f:1}}",x.postX6(map("k1",Bean.create())));
		assertEquals("xxx",x.postX7(reader("xxx")));
		assertEquals("xxx",x.postX8(inputStream("xxx")));
		assertEquals("xxx",x.postX9(new StringEntity("xxx",org.apache.http.entity.ContentType.create("text/plain"))));
		assertEquals("foo=bar",x.postX10(partList("foo","bar")));
	}
}