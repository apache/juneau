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

import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Body_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Reader post(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) throws IOException {
			for (RequestHeader e : req.getHeaders())
				res.addHeader("X-" + e.getName(), e.getValue());
			return req.getReader();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_BasicHttpResource() throws Exception {
		HttpResource x1 = stringResource("foo");
		client().build().post("/", x1).run()
			.assertHeader("X-Content-Length").asInteger().is(3)
			.assertHeader("X-Content-Encoding").isNull()
			.assertHeader("X-Content-Type").isNull()
			.assertHeader("X-Transfer-Encoding").isNull()
		;

		HttpResource x2 = stringResource("foo").setContentType("text/plain").setContentEncoding("identity");
		client().build().post("/",x2).run()
			.assertHeader("X-Content-Length").asInteger().is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").isNull()
		;

		HttpResource x3 = stringResource("foo").setContentType(contentType("text/plain")).setContentEncoding(contentEncoding("identity")).setChunked();
		client().build().post("/",x3).run()
			.assertHeader("X-Content-Length").isNull()  // Missing when chunked.
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").is("chunked")
		;

		HttpResource x4 = stringResource("foo", contentType("text/plain")).setContentEncoding("identity");
		client().build().post("/",x4).run()
			.assertHeader("X-Content-Length").asInteger().is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").isNull()
		;

		HttpResource x5 = stringResource("foo").setHeader("Foo","bar").setHeaders(header("Baz","qux"));
		client().build().post("/",x5).run()
			.assertHeader("X-Foo").is("bar")
			.assertHeader("X-Baz").is("qux")
		;

		HttpResource x6 = stringResource("foo").addHeaders(header("Foo","bar"),header("Baz","qux"));
		client().build().post("/",x6).run()
			.assertHeader("X-Foo").is("bar")
			.assertHeader("X-Baz").is("qux")
		;

		HttpResource x7 = readerResource(reader("foo"));
		client().build().post("/",x7).run().assertContent("foo");

		HttpResource x8 = readerResource(reader("foo")).setCached();
		client().build().post("/",x8).run().assertContent("foo");
		client().build().post("/",x8).run().assertContent("foo");
	}

	@Test
	public void a02_StringEntity() throws Exception {
		HttpEntity x1 = stringEntity("foo");
		client().build().post("/", x1).run()
			.assertHeader("X-Content-Length").asInteger().is(3)
			.assertHeader("X-Content-Encoding").isNull()
			.assertHeader("X-Content-Type").isNull()
			.assertHeader("X-Transfer-Encoding").isNull()
		;

		HttpEntity x2 = stringEntity("foo").setContentType("text/plain").setContentEncoding("identity");
		client().build().post("/",x2).run()
			.assertHeader("X-Content-Length").asInteger().is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").isNull()
		;

		HttpEntity x3 = stringEntity("foo").setContentType(contentType("text/plain")).setContentEncoding(contentEncoding("identity")).setChunked();
		client().build().post("/",x3).run()
			.assertHeader("X-Content-Length").isNull()  // Missing when chunked.
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").is("chunked")
		;

		HttpEntity x4 = stringEntity("foo", contentType("text/plain")).setContentEncoding("identity");
		client().build().post("/",x4).run()
			.assertHeader("X-Content-Length").asInteger().is(3)
			.assertHeader("X-Content-Encoding").is("identity")
			.assertHeader("X-Content-Type").is("text/plain")
			.assertHeader("X-Transfer-Encoding").isNull()
		;

		HttpEntity x7 = readerEntity(reader("foo"));
		client().build().post("/",x7).run().assertContent("foo");

		HttpEntity x8 = readerEntity(reader("foo")).setCached();
		client().build().post("/",x8).run().assertContent("foo");
		client().build().post("/",x8).run().assertContent("foo");

		BasicHttpEntity x12 = stringEntity("foo");
		x12.assertString().is("foo");
		x12.assertBytes().asString().is("foo");
	}

	@Test
	public void a03_SerializedHttpEntity() throws Exception {
		Serializer js = JsonSerializer.DEFAULT;

		SerializedEntity x1 = serializedEntity(ABean.get(),null,null);
		client().build().post("/",x1).run()
			.assertHeader("X-Content-Length").isNull()
			.assertHeader("X-Content-Encoding").isNull()
			.assertHeader("X-Content-Type").is("application/json5")
			.assertHeader("X-Transfer-Encoding").is("chunked")  // Because content length is -1.
		;

		SerializedEntity x2 = serializedEntity(ABean.get(),js,null);
		client().build().post("/",x2).run()
			.assertHeader("X-Content-Length").isNull()
			.assertHeader("X-Content-Encoding").isNull()
			.assertHeader("X-Content-Type").is("application/json")
			.assertContent().as(ABean.class).asJson().is("{a:1,b:'foo'}");

		SerializedEntity x3 = serializedEntity(()->ABean.get(),js,null);
		client().build().post("/",x3).run()
			.assertHeader("X-Content-Length").isNull()
			.assertHeader("X-Content-Encoding").isNull()
			.assertHeader("X-Content-Type").is("application/json")
			.assertContent().as(ABean.class).asJson().is("{a:1,b:'foo'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}
}
