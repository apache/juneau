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

import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Body_Test {

	@Rest
	public static class A extends BasicRest {
		@RestMethod
		public Reader post(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) throws IOException {
			for (Map.Entry<String,String[]> e : req.getHeaders().entrySet()) {
				for (String v : e.getValue())
					res.addHeader("X-" + e.getKey(), v);
			}
			return req.getReader();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_BasicHttpResource() throws Exception {
		BasicHttpResource x1 = httpResource("foo");
		client().build().post("/", x1).run()
			.assertIntegerHeader("X-Content-Length").is(3)
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").doesNotExist()
			.assertHeader("X-Transfer-Encoding").doesNotExist()
		;

		BasicHttpResource x2 = httpResource("foo").contentType("text/plain").contentEncoding("identity");
		client().build().post("/",x2).run()
			.assertIntegerHeader("X-Content-Length").is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").doesNotExist()
		;

		BasicHttpResource x3 = httpResource("foo").contentType(contentType("text/plain")).contentEncoding(contentEncoding("identity")).chunked();
		client().build().post("/",x3).run()
			.assertIntegerHeader("X-Content-Length").doesNotExist()  // Missing when chunked.
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").is("chunked")
		;

		BasicHttpResource x4 = new BasicHttpResource("foo", contentType("text/plain"), contentEncoding("identity"));
		client().build().post("/",x4).run()
			.assertIntegerHeader("X-Content-Length").is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").doesNotExist()
		;

		BasicHttpResource x5 = httpResource("foo").header("Foo","bar").header(header("Baz","qux"));
		client().build().post("/",x5).run()
			.assertHeader("X-Foo").is("bar")
			.assertHeader("X-Baz").is("qux")
		;

		BasicHttpResource x6 = httpResource("foo").headers(Arrays.asList(header("Foo","bar"),header("Baz","qux")));
		client().build().post("/",x6).run()
			.assertHeader("X-Foo").is("bar")
			.assertHeader("X-Baz").is("qux")
		;

		BasicHttpResource x7 = httpResource(new StringReader("foo"));
		client().build().post("/",x7).run().getBody().assertString().is("foo");

		BasicHttpResource x8 = httpResource(new StringReader("foo")).cache();
		client().build().post("/",x8).run().getBody().assertString().is("foo");
		client().build().post("/",x8).run().getBody().assertString().is("foo");

		BasicHttpResource x9 = httpResource(null);
		client().build().post("/",x9).run().getBody().assertString().isEmpty();
	}

	@Test
	public void a02_BasicHttpEntity() throws Exception {
		BasicHttpEntity x1 = httpEntity("foo");
		client().build().post("/", x1).run()
			.assertIntegerHeader("X-Content-Length").is(3)
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").doesNotExist()
			.assertHeader("X-Transfer-Encoding").doesNotExist()
		;

		BasicHttpEntity x2 = httpEntity("foo").contentType("text/plain").contentEncoding("identity");
		client().build().post("/",x2).run()
			.assertIntegerHeader("X-Content-Length").is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").doesNotExist()
		;

		BasicHttpEntity x3 = httpEntity("foo").contentType(contentType("text/plain")).contentEncoding(contentEncoding("identity")).chunked();
		client().build().post("/",x3).run()
			.assertIntegerHeader("X-Content-Length").doesNotExist()  // Missing when chunked.
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").is("chunked")
		;

		BasicHttpEntity x4 = new BasicHttpEntity("foo", contentType("text/plain"), contentEncoding("identity"));
		client().build().post("/",x4).run()
			.assertIntegerHeader("X-Content-Length").is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").doesNotExist()
		;

		BasicHttpEntity x7 = httpEntity(new StringReader("foo"));
		client().build().post("/",x7).run().getBody().assertString().is("foo");

		BasicHttpEntity x8 = httpEntity(new StringReader("foo")).cache();
		client().build().post("/",x8).run().getBody().assertString().is("foo");
		client().build().post("/",x8).run().getBody().assertString().is("foo");

		BasicHttpEntity x9 = httpEntity(null);
		client().build().post("/",x9).run().getBody().assertString().isEmpty();
	}

	@Test
	public void a03_SerializedHttpEntity() throws Exception {
		Serializer js = JsonSerializer.DEFAULT;
		File f = File.createTempFile("test", "txt");

		SerializedHttpEntity x = serializedHttpEntity(ABean.get(),null);
		client().debug().build().post("/",x).run()
			.assertHeader("X-Content-Length").doesNotExist()
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").doesNotExist()
			.assertHeader("X-Transfer-Encoding").is("chunked")  // Because content length is -1.
		;

		x = serializedHttpEntity(ABean.get(),js);
		client().build().post("/",x).run()
			.assertHeader("X-Content-Length").doesNotExist()
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").is("application/json")
			.getBody().assertObject(ABean.class).json().is("{a:1,b:'foo'}");

		x = SerializedHttpEntity.of(()->ABean.get(),js);
		client().build().post("/",x).run()
			.assertHeader("X-Content-Length").doesNotExist()
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").is("application/json")
			.getBody().assertObject(ABean.class).json().is("{a:1,b:'foo'}");

		x = serializedHttpEntity(new StringReader("{a:1,b:'foo'}"),null);
		client().build().post("/",x).run()
			.assertHeader("X-Content-Length").doesNotExist()
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").doesNotExist()
			.getBody().assertObject(ABean.class).json().is("{a:1,b:'foo'}");

		x = serializedHttpEntity(new ByteArrayInputStream("{a:1,b:'foo'}".getBytes()),null);
		client().build().post("/",x).run()
			.assertHeader("X-Content-Length").doesNotExist()
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").doesNotExist()
			.getBody().assertObject(ABean.class).json().is("{a:1,b:'foo'}");

		x = serializedHttpEntity(f,null);
		client().build().post("/",x).run()
			.assertHeader("X-Content-Length").doesNotExist()
			.assertHeader("X-Content-Encoding").doesNotExist()
			.assertHeader("X-Content-Type").doesNotExist()
			.getBody().assertObject(ABean.class).json().is("{a:0}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static BasicHttpResource httpResource(Object val) {
		return BasicHttpResource.of(val);
	}

	private static BasicHttpEntity httpEntity(Object val) {
		return BasicHttpEntity.of(val);
	}

	private static SerializedHttpEntity serializedHttpEntity(Object val, Serializer s) {
		return SerializedHttpEntity.of(val, s);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}

	private static Header header(String name, Object val) {
		return BasicHeader.of(name,val);
	}

	private static ContentType contentType(String val) {
		return ContentType.of(val);
	}

	private static ContentEncoding contentEncoding(String val) {
		return ContentEncoding.of(val);
	}
}
