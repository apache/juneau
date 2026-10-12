/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.httppart;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Request content dispatch by <c>Content-Type</c> through a real {@link BasicRestServlet}.
 */
class RequestContent_ContentTypeMatching_Test extends TestBase {

	public static class ABean {
		public int a;
	}

	/** A non-bean type that is only convertible from a {@link Reader}. */
	public static class ReaderOnly {
		private final String value;

		public ReaderOnly(Reader in) throws IOException {
			var sb = new StringBuilder();
			var buf = new char[256];
			for (var n = in.read(buf); n != -1; n = in.read(buf))
				sb.append(buf, 0, n);
			value = sb.toString();
		}

		@Override
		public String toString() {
			return "reader:" + value;
		}
	}

	/** A non-bean type that is only convertible from an {@link InputStream}, rejecting the content <js>"bad"</js>. */
	public static class StreamOnly {
		private final String value;

		public StreamOnly(InputStream in) throws IOException {
			value = new String(in.readAllBytes(), UTF_8);
			if (value.equals("bad"))
				throw new IllegalArgumentException("Unconvertible content");
		}

		@Override
		public String toString() {
			return "stream:" + value;
		}
	}

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestPost("/bean")
		public String bean(@Content ABean in) {
			return "a=" + in.a;
		}

		@RestPost("/map")
		public String map(@Content JsonMap in) {
			return "a=" + in.get("a");
		}

		@RestPost("/readerOnly")
		public String readerOnly(@Content ReaderOnly in) {
			return in.toString();
		}

		@RestPost("/streamOnly")
		public String streamOnly(@Content StreamOnly in) {
			return in.toString();
		}
	}

	private static MockRestClient client() {
		return MockRestClient.create(A.class).accept("text/plain").ignoreErrors().noTrace().build();
	}

	@Test void a01_json_returns200() throws Exception {
		client().post("/map", "{\"a\":1}").contentType("application/json").run()
			.assertStatus(200)
			.assertContent("a=1");
	}

	@Test void a02_json5_returns200() throws Exception {
		client().post("/map", "{a:1}").contentType("application/json5").run()
			.assertStatus(200)
			.assertContent("a=1");
	}

	@Test void a03_textJson5_returns200() throws Exception {
		client().post("/map", "{a:1}").contentType("text/json5").run()
			.assertStatus(200)
			.assertContent("a=1");
	}

	@Test void a04_unsupportedContentType_strictJson_usesReaderConstructor() throws Exception {
		// Documented: a Content-Type that matches no parser falls back to the type's Reader constructor.
		client().post("/map", "{\"a\":1}").contentType("application/x-unknown").run()
			.assertStatus(200)
			.assertContent("a=1");
	}

	@Test void a05_unsupportedContentType_nonJsonContent_returns415() throws Exception {
		client().post("/map", "{a:1}").contentType("application/x-unknown").run()
			.assertStatus(415)
			.assertContent().isContains("Unsupported media-type in request header 'Content-Type': 'application/x-unknown'");
	}

	@Test void a06_unsupportedContentType_beanWithoutReaderConstructor_returns415() throws Exception {
		client().post("/bean", "{a:1}").contentType("application/x-unknown").run()
			.assertStatus(415)
			.assertContent().isContains("Unsupported media-type in request header 'Content-Type': 'application/x-unknown'");
	}

	@Test void a07_blankContentType_strictJson_returns200() throws Exception {
		// Blank Content-Type (the mock client always sends one for String content): documented fallback to the
		// type's Reader constructor (strict JSON for JsonMap).
		client().post("/map", "{\"a\":1}").contentType("").run()
			.assertStatus(200)
			.assertContent("a=1");
	}

	@Test void a08_blankContentType_nonJsonContent_keepsExisting500() throws Exception {
		// With a blank Content-Type there is nothing to call unsupported, so a failing Reader constructor keeps its existing 500.
		client().post("/map", "{a:1}").contentType("").run()
			.assertStatus(500);
	}

	@Test void a09_readerOnlyType_unsupportedContentType_usesReaderConstructor() throws Exception {
		// Documented: a non-POJO Reader-convertible type is used when the Content-Type matches no parser.
		client().post("/readerOnly", "xyz").contentType("application/x-unknown").run()
			.assertStatus(200)
			.assertContent("reader:xyz");
	}

	@Test void a10_streamOnlyType_unsupportedContentType_usesInputStreamConstructor() throws Exception {
		// Documented: a non-POJO InputStream-convertible type is used when the Content-Type matches no parser.
		client().post("/streamOnly", "xyz").contentType("application/x-unknown").run()
			.assertStatus(200)
			.assertContent("stream:xyz");
	}

	@Test void a11_streamOnlyType_unsupportedContentType_unconvertible_returns415() throws Exception {
		client().post("/streamOnly", "bad").contentType("application/x-unknown").run()
			.assertStatus(415)
			.assertContent().isContains("Unsupported media-type in request header 'Content-Type': 'application/x-unknown'");
	}
}
