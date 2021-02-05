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

import java.io.*;

import static org.apache.juneau.httppart.HttpPartSchema.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.pojos.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.SerializedHttpEntity.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class SerializedHttpEntity_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestOp
		public String[] postCheckHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().get(req.getHeader("Check"));
		}
		@RestOp
		public Reader postCheckBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getReader();
		}
	}

	@Test
	public void a01_basic() throws Exception {
		of(ABean.get(),JsonSerializer.DEFAULT).assertString().is("{\"a\":1,\"b\":\"foo\"}");
		of(()->ABean.get(),JsonSerializer.DEFAULT).assertString().is("{\"a\":1,\"b\":\"foo\"}");
		of(ABean.get(),null).assertString().is("{a:1,b:'foo'}");
		of(null,JsonSerializer.DEFAULT).assertString().is("null");
	}

	@Test
	public void a02_schema() throws Exception {
		of(AList.of("foo","bar"),OpenApiSerializer.DEFAULT).schema(T_ARRAY_PIPES).assertString().is("foo|bar");
	}

	@Test
	public void a03_serializer_streaming() throws Exception {
		of(ABean.get(),MsgPackSerializer.DEFAULT).assertBytes().asSpacedHex().is("82 A1 61 01 A1 62 A3 66 6F 6F");
	}

	@Test
	public void a04_serializer_bad() throws Exception {
		assertThrown(()->of(null,OpenApiSerializer.DEFAULT).schema(schema().required().build()).asString()).contains("Required value not provided.");
	}

	@Test
	public void a05_writeTo() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		of(new ByteArrayInputStream("foo".getBytes()), null).writeTo(baos);
		assertBytes(baos.toByteArray()).asString().is("foo");
	}

	@Test
	public void a06_isRepeatable() throws Exception {
		assertBoolean(of(ABean.get(),null).isRepeatable()).isTrue();
		assertBoolean(of(new ByteArrayInputStream("foo".getBytes()),null).isRepeatable()).isFalse();
		assertBoolean(of(new File(""),null).isRepeatable()).isTrue();
		assertBoolean(of(new StringReader("foo"),null).isRepeatable()).isFalse();
	}

	@Test
	public void a07_getContentLength() throws Exception {
		assertLong(of(ABean.get(),null).getContentLength()).is(-1);
		assertLong(of(new StringReader("foo"),null).cache().getContentLength()).is(3);
	}

	@Test
	public void a08_getContent() throws Exception {
		assertStream(of(new StringReader("foo"),null).getContent()).asString().is("foo");

		SerializedHttpEntity x = new SerializedHttpEntity("foo", null) {
			@Override
			public void writeTo(OutputStream os) throws IOException {
				throw new IOException("Bad");
			}
		};
		assertThrown(()->x.getContent()).contains("Bad");
	}

	@Test
	public void a09_chunked() throws Exception {
		checkHeaderClient("Transfer-Encoding").post("/",of(ABean.get(),null).chunked()).run().assertBody().is("['chunked']");
	}

	@Test
	public void a10_contentEncoding() throws Exception {
		checkHeaderClient("Content-Encoding").post("/",of(ABean.get(),null).contentEncoding("identity")).run().assertBody().is("['identity']");
	}

	@Test
	public void a11_contentLength() throws Exception {
		checkHeaderClient("Content-Length").post("/",of(new StringReader("foo"),null).contentLength(3)).run().assertBody().is("['3']");
	}

	@Test
	public void a12_contentType() throws Exception {
		checkHeaderClient("Content-Type").post("/",of(new StringReader("foo"),null).contentType("text/foo")).run().assertBody().is("['text/foo']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private HttpPartSchemaBuilder schema() {
		return HttpPartSchema.create();
	}

	private RestClient checkHeaderClient(String header) {
		return MockRestClient.create(A.class).rootUri("http://localhost/checkHeader").simpleJson().header("Check",header).ignoreErrors().build();
	}
}
