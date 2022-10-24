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
package org.apache.juneau.http;

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;

import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.http.HttpEntities.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.testutils.pojos.*;

import static org.apache.juneau.assertions.Assertions.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class SerializedHttpEntity_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public String[] checkHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().getAll(req.getHeaderParam("Check").orElse(null)).stream().map(x -> x.getValue()).toArray(String[]::new);
		}
		@RestPost
		public Reader checkBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getReader();
		}
	}

	@Test
	public void a01_basic() throws Exception {
		serializedEntity(ABean.get(),JsonSerializer.DEFAULT).assertString().is("{\"a\":1,\"b\":\"foo\"}");
		serializedEntity(()->ABean.get(),JsonSerializer.DEFAULT).assertString().is("{\"a\":1,\"b\":\"foo\"}");
		serializedEntity(ABean.get(),null).assertString().is("{a:1,b:'foo'}");
		serializedEntity(null,JsonSerializer.DEFAULT).assertString().is("null");
	}

	@Test
	public void a02_schema() throws Exception {
		serializedEntity(alist("foo","bar"),OpenApiSerializer.DEFAULT).setSchema(T_ARRAY_PIPES).assertString().is("foo|bar");
	}

	@Test
	public void a03_serializer_streaming() throws Exception {
		serializedEntity(ABean.get(),MsgPackSerializer.DEFAULT).assertBytes().asSpacedHex().is("82 A1 61 01 A1 62 A3 66 6F 6F");
	}

	@Test
	public void a04_serializer_bad() throws Exception {
		assertThrown(()->serializedEntity(null,OpenApiSerializer.DEFAULT).setSchema(schema().required().build()).asString()).asMessages().isContains("Required value not provided.");
	}

	@Test
	public void a05_writeTo() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializedEntity("foo", null).writeTo(baos);
		assertBytes(baos.toByteArray()).asString().is("foo");
	}

	@Test
	public void a06_isRepeatable() throws Exception {
		assertBoolean(serializedEntity(ABean.get(),null).isRepeatable()).isTrue();
	}

	@Test
	public void a07_getContentLength() throws Exception {
		assertLong(serializedEntity(ABean.get(),null).getContentLength()).is(-1l);
	}

	@Test
	public void a08_getContent() throws Exception {
		assertBytes(serializedEntity("foo",null).getContent()).asString().is("foo");
	}

	@Test
	public void a09_chunked() throws Exception {
		checkHeaderClient("Transfer-Encoding").post("/",serializedEntity(ABean.get(),null).setChunked()).run().assertContent("['chunked']");
	}

	@Test
	public void a10_contentEncoding() throws Exception {
		checkHeaderClient("Content-Encoding").post("/",serializedEntity(ABean.get(),null).setContentEncoding("identity")).run().assertContent("['identity']");
	}

	@Test
	public void a12_contentType() throws Exception {
		checkHeaderClient("Content-Type").post("/",serializedEntity(reader("foo"),null).setContentType("text/foo")).run().assertContent("['text/foo']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private HttpPartSchema.Builder schema() {
		return HttpPartSchema.create();
	}

	private RestClient checkHeaderClient(String header) {
		return MockRestClient.create(A.class).rootUrl("http://localhost/checkHeader").json5().header("Check",header).ignoreErrors().build();
	}
}
