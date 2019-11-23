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
package org.apache.juneau.rest.client;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Body annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BodyAnnotationTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}
	}

	//=================================================================================================================
	// Basic tests - JSON
	//=================================================================================================================

	@Rest(parsers=JsonParser.class)
	public static class A {
		@RestMethod
		public String postA01(@Body int b, @Header("Content-Type") String ct) {
			assertEquals("application/json", ct);
			return String.valueOf(b);
		}

		@RestMethod
		public String postA02(@Body float b, @Header("Content-Type") String ct) {
			assertEquals("application/json", ct);
			return String.valueOf(b);
		}

		@RestMethod
		public String postA03(@Body Bean b, @Header("Content-Type") String ct) {
			assertEquals("application/json", ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postA04(@Body Bean[] b, @Header("Content-Type") String ct) {
			assertEquals("application/json", ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postA05(@Body List<Bean> b, @Header("Content-Type") String ct) {
			assertEquals("application/json", ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postA06(@Body Map<String,Bean> b, @Header("Content-Type") String ct) {
			assertEquals("application/json", ct);
			return SimpleJsonSerializer.DEFAULT.toString(b);
		}

		@RestMethod
		public String postA07(@Body Reader b, @Header("Content-Type") String ct) throws Exception {
			assertEquals("text/plain", ct);
			return IOUtils.read(b);
		}

		@RestMethod
		public String postA08(@Body InputStream b, @Header("Content-Type") String ct) throws Exception {
			assertEquals("application/octet-stream", ct);
			return IOUtils.read(b);
		}

		@RestMethod
		public String postA09(@Body Reader b, @Header("Content-Type") String ct) throws Exception {
			assertTrue(ct.startsWith("text/plain"));
			return IOUtils.read(b);
		}

		@RestMethod
		public String postA10(@Body Reader b, @Header("Content-Type") String ct) throws IOException {
			assertEquals("application/x-www-form-urlencoded", ct);
			return IOUtils.read(b);
		}
	}

	@RemoteResource
	public static interface A01 {
		String postA01(@Body int b);
		String postA02(@Body float b);
		String postA03(@Body Bean b);
		String postA04(@Body Bean[] b);
		String postA05(@Body List<Bean> b);
		String postA06(@Body Map<String,Bean> b);
		String postA07(@Body Reader b);
		String postA08(@Body InputStream b);
		String postA09(@Body HttpEntity b);
		String postA10(@Body NameValuePairs b);
	}

	private static A01 a01 = MockRemoteResource.create(A01.class, A.class).parser(null).build();

	@Test
	public void a01_int() throws Exception {
		assertEquals("1", a01.postA01(1));
	}
	@Test
	public void a02_float() throws Exception {
		assertEquals("1.0", a01.postA02(1f));
	}
	@Test
	public void a03_Bean() throws Exception {
		assertEquals("{f:1}", a01.postA03(Bean.create()));
	}
	@Test
	public void a04_BeanArray() throws Exception {
		assertEquals("[{f:1}]", a01.postA04(new Bean[]{Bean.create()}));
	}
	@Test
	public void a05_ListOfBeans() throws Exception {
		assertEquals("[{f:1}]", a01.postA05(AList.create(Bean.create())));
	}
	@Test
	public void a06_MapOfBeans() throws Exception {
		assertEquals("{k1:{f:1}}", a01.postA06(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a07_Reader() throws Exception {
		assertEquals("xxx", a01.postA07(new StringReader("xxx")));
	}
	@SuppressWarnings("resource")
	@Test
	public void a08_InputStream() throws Exception {
		assertEquals("xxx", a01.postA08(new StringInputStream("xxx")));
	}
	@Test
	public void a09_HttpEntity() throws Exception {
		assertEquals("xxx", a01.postA09(new StringEntity("xxx")));
	}
	@Test
	public void a10_NameValuePairs() throws Exception {
		assertEquals("foo=bar", a01.postA10(new NameValuePairs().append("foo", "bar")));
	}

	//=================================================================================================================
	// Basic tests - OpenAPI
	//=================================================================================================================

	@Rest(serializers=OpenApiSerializer.class,parsers=OpenApiParser.class,defaultAccept="text/openapi")
	public static class B {
		@RestMethod
		public Object postB01(@Body int b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi", ct);
			return b;
		}

		@RestMethod
		public Object postB02(@Body float b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi", ct);
			return b;
		}

		@RestMethod
		public String postB03(@Body Bean b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi", ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postB04(@Body Bean[] b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi", ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postB05(@Body List<Bean> b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi", ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postB06(@Body Map<String,Bean> b, @Header("Content-Type") String ct) {
			assertEquals("text/openapi", ct);
			return SimpleJson.DEFAULT.toString(b);
		}

		@RestMethod
		public Object postB07(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/plain", ct);
			return b;
		}

		@RestMethod
		public Object postB08(@Body InputStream b, @Header("Content-Type") String ct) {
			assertEquals("application/octet-stream", ct);
			return b;
		}

		@RestMethod
		public Object postB09(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/plain", ct);
			return b;
		}

		@RestMethod
		public Object postB10(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("application/x-www-form-urlencoded", ct);
			return b;
		}
	}
	@RemoteResource
	public static interface B01 {
		String postB01(@Body int b);
		String postB02(@Body float b);
		String postB03(@Body Bean b);
		String postB04(@Body Bean[] b);
		String postB05(@Body List<Bean> b);
		String postB06(@Body Map<String,Bean> b);
		String postB07(@Body Reader b);
		String postB08(@Body InputStream b);
		String postB09(@Body HttpEntity b);
		String postB10(@Body NameValuePairs b);
	}

	private static B01 b01 = MockRemoteResource.create(B01.class, B.class).marshall(OpenApi.DEFAULT).contentType(null).build();

	@Test
	public void b01_int() throws Exception {
		String o = b01.postB01(1);
		assertEquals("1", o);
	}
	@Test
	public void b02_float() throws Exception {
		String o = b01.postB02(1f);
		assertEquals("1.0", o);
	}
	@Test
	public void b03_Bean() throws Exception {
		String o = b01.postB03(Bean.create());
		assertEquals("{f:1}", o);
	}
	@Test
	public void b04_BeanArray() throws Exception {
		String o = b01.postB04(new Bean[]{Bean.create()});
		assertEquals("[{f:1}]", o);
	}
	@Test
	public void b05_ListOfBeans() throws Exception {
		String o = b01.postB05(AList.create(Bean.create()));
		assertEquals("[{f:1}]", o);
	}
	@Test
	public void b06_MapOfBeans() throws Exception {
		String o = b01.postB06(AMap.create("k1",Bean.create()));
		assertEquals("{k1:{f:1}}", o);
	}
	@Test
	public void b07_Reader() throws Exception {
		String o = b01.postB07(new StringReader("xxx"));
		assertEquals("xxx", o);
	}
	@Test
	public void b08_InputStream() throws Exception {
		@SuppressWarnings("resource")
		String o = b01.postB08(new StringInputStream("xxx"));
		assertEquals("xxx", o);
	}
	@Test
	public void b09_HttpEntity() throws Exception {
		String o = b01.postB09(new StringEntity("xxx", ContentType.create("text/plain")));
		assertEquals("xxx", o);
	}
	@Test
	public void b10_NameValuePairs() throws Exception {
		String o = b01.postB10(new NameValuePairs().append("foo", "bar"));
		assertEquals("foo=bar", o);
	}

	//=================================================================================================================
	// Basic tests - OpenAPI, overridden Content-Type
	//=================================================================================================================

	@Rest
	public static class C {
		@RestMethod
		public Reader postC01(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC02(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC03(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC04(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC05(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC06(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC07(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC08(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC09(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
		@RestMethod
		public Reader postC10(@Body Reader b, @Header("Content-Type") String ct) {
			assertEquals("text/foo", ct);
			return b;
		}
	}
	@RemoteResource
	public static interface C01 {
		String postC01(@Body int b);
		String postC02(@Body float b);
		String postC03(@Body Bean b);
		String postC04(@Body Bean[] b);
		String postC05(@Body List<Bean> b);
		String postC06(@Body Map<String,Bean> b);
		String postC07(@Body Reader b);
		String postC08(@Body InputStream b);
		String postC09(@Body HttpEntity b);
		String postC10(@Body NameValuePairs b);
	}

	private static C01 c01 = MockRemoteResource.create(C01.class, C.class).serializer(null).contentType("text/foo").build();

	@Test
	public void c01_int() throws Exception {
		String o = c01.postC01(1);
		assertEquals("1", o);
	}
	@Test
	public void c02_float() throws Exception {
		String o = c01.postC02(1f);
		assertEquals("1.0", o);
	}
	@Test
	public void c03_Bean() throws Exception {
		String o = c01.postC03(Bean.create());
		assertEquals("(f=1)", o);
	}
	@Test
	public void c04_BeanArray() throws Exception {
		String o = c01.postC04(new Bean[]{Bean.create()});
		assertEquals("(f=1)", o);
	}
	@Test
	public void c05_ListOfBeans() throws Exception {
		String o = c01.postC05(AList.create(Bean.create()));
		assertEquals("(f=1)", o);
	}
	@Test
	public void c06_MapOfBeans() throws Exception {
		String o = c01.postC06(AMap.create("k1",Bean.create()));
		assertEquals("(k1=(f=1))", o);
	}
	@Test
	public void c07_Reader() throws Exception {
		String o = c01.postC07(new StringReader("xxx"));
		assertEquals("xxx", o);
	}
	@Test
	public void c08_InputStream() throws Exception {
		@SuppressWarnings("resource")
		String o = c01.postC08(new StringInputStream("xxx"));
		assertEquals("xxx", o);
	}
	@Test
	public void c09_HttpEntity() throws Exception {
		String o = c01.postC09(new StringEntity("xxx", ContentType.create("text/plain")));
		assertEquals("xxx", o);
	}
	@Test
	public void c10_NameValuePairs() throws Exception {
		String o = c01.postC10(new NameValuePairs().append("foo", "bar"));
		assertEquals("foo=bar", o);
	}
}