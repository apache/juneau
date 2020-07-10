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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Body_Test {

	@Rest
	public static class A extends BasicRest {
		@RestMethod
		public Reader post(org.apache.juneau.rest.RestRequest req) throws IOException {
			return new StringReader(req.toString());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_ReaderResource() throws Exception {
		ReaderResource x1 = ReaderResource.create().content("foo");
		client().build().post("/", x1).run()
			.getBody().assertString().contains("foo", "Content-Length: 3")
			.getBody().assertString().doesNotContain("Content-Encoding","Content-Type","Transfer-Encoding")
		;

		ReaderResource x2 = ReaderResource.create().content("foo").contentType("text/plain").contentEncoding("identity");
		client().build().post("/",x2).run()
			.getBody().assertString().contains("foo","Content-Length: 3","Content-Encoding: identity","Content-Type: text/plain")
		;

		ReaderResource x3 = ReaderResource.create().content("foo").contentType(contentType("text/plain")).contentEncoding(contentEncoding("identity")).chunked();
		client().build().post("/",x3).run()
			.getBody().assertString().contains("foo","Content-Encoding: identity","Content-Type: text/plain","Transfer-Encoding: chunked")
		;

		ReaderResource x4 = new ReaderResource(contentType("text/plain"), contentEncoding("identity"), "foo");
		client().build().post("/",x4).run()
			.getBody().assertString().contains("foo","Content-Length: 3","Content-Encoding: identity","Content-Type: text/plain")
		;

		ReaderResource x5 = ReaderResource.of("foo").header("Foo","bar").header(header("Baz","qux"));
		client().build().post("/",x5).run()
			.getBody().assertString().contains("Foo: bar","Baz: qux")
		;

		ReaderResource x6 = ReaderResource.of("foo").headers(Arrays.asList(header("Foo","bar"),header("Baz","qux")));
		client().build().post("/",x6).run()
			.getBody().assertString().contains("Foo: bar","Baz: qux")
		;

		ReaderResource x7 = ReaderResource.of(new StringReader("foo"));
		client().build().post("/",x7).run().getBody().assertString().contains("foo");

		ReaderResource x8 = ReaderResource.of(new StringReader("foo")).cache();
		client().build().post("/",x8).run().getBody().assertString().contains("foo");
		client().build().post("/",x8).run().getBody().assertString().contains("foo");

		ReaderResource x9 = ReaderResource.of(null);
		client().build().post("/",x9).run().getBody().assertString().contains("HTTP POST");
	}

	@Test
	public void a02_StreamResource() throws Exception {
		StreamResource x1 = StreamResource.create().content("foo");
		client().build().post("/", x1).run()
			.getBody().assertString().contains("foo", "Content-Length: 3")
			.getBody().assertString().doesNotContain("Content-Encoding","Content-Type","Transfer-Encoding")
		;

		StreamResource x2 = StreamResource.create().content("foo").contentType("text/plain").contentEncoding("identity");
		client().build().post("/",x2).run()
			.getBody().assertString().contains("foo","Content-Length: 3","Content-Encoding: identity","Content-Type: text/plain")
		;

		StreamResource x3 = StreamResource.create().content("foo").contentType(contentType("text/plain")).contentEncoding(contentEncoding("identity")).chunked();
		client().build().post("/",x3).run()
			.getBody().assertString().contains("foo","Content-Encoding: identity","Content-Type: text/plain","Transfer-Encoding: chunked")
		;

		StreamResource x4 = new StreamResource(contentType("text/plain"), contentEncoding("identity"), "foo");
		client().build().post("/",x4).run()
			.getBody().assertString().contains("foo","Content-Length: 3","Content-Encoding: identity","Content-Type: text/plain")
		;

		StreamResource x5 = StreamResource.of("foo").header("Foo","bar").header(header("Baz","qux"));
		client().build().post("/",x5).run()
			.getBody().assertString().contains("Foo: bar","Baz: qux")
		;

		StreamResource x6 = StreamResource.of("foo").headers(Arrays.asList(header("Foo","bar"),header("Baz","qux")));
		client().build().post("/",x6).run()
			.getBody().assertString().contains("Foo: bar","Baz: qux")
		;

		StreamResource x7 = StreamResource.of(new StringReader("foo"));
		client().build().post("/",x7).run().getBody().assertString().contains("foo");

		StreamResource x8 = StreamResource.of(new StringReader("foo")).cache();
		client().build().post("/",x8).run().getBody().assertString().contains("foo");
		client().build().post("/",x8).run().getBody().assertString().contains("foo");

		StreamResource x9 = StreamResource.of(null);
		client().build().post("/",x9).run().getBody().assertString().contains("HTTP POST");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

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
