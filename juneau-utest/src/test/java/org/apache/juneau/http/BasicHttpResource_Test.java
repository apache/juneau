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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.BasicHttpResource.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.junit.*;

public class BasicHttpResource_Test {
	@Test
	public void a01_basic() throws Exception {
		BasicHttpResource x = of(null);
		File f = File.createTempFile("test", "txt");

		assertNull(x.getContentType());
		assertNull(x.getContent());
		assertNull(x.getContentEncoding());
		assertList(x.getHeaders()).isSize(0);

		x = of("foo");
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(new StringReader("foo"));
		assertStream(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of("foo".getBytes());
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(new ByteArrayInputStream("foo".getBytes()));
		assertStream(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of(null);
		assertStream(x.getContent()).asString().doesNotExist();
		assertFalse(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(f);
		assertStream(x.getContent()).asString().isEmpty();
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of("foo").cache();
		assertStream(x.getContent()).asString().is("foo");
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = of(new StringReader("foo")).cache();
		assertStream(x.getContent()).asString().is("foo");
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = of("foo".getBytes()).cache();
		assertStream(x.getContent()).asString().is("foo");
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = of(new ByteArrayInputStream("foo".getBytes())).cache();
		assertStream(x.getContent()).asString().is("foo");
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = of(null).cache();
		assertStream(x.getContent()).asString().doesNotExist();
		assertStream(x.getContent()).asString().doesNotExist();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		x = of(f).cache();
		assertStream(x.getContent()).asString().isEmpty();
		assertStream(x.getContent()).asString().isEmpty();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		assertLong(of("foo").getContentLength()).is(3l);
		assertLong(of("foo".getBytes()).getContentLength()).is(3l);
		assertLong(of(f).getContentLength()).is(0l);

		assertLong(of(new StringReader("foo")).getContentLength()).is(-1l);
		assertLong(of(new StringReader("foo")).contentLength(3).getContentLength()).is(3l);

		x = new BasicHttpResource("foo", contentType("text/plain"), contentEncoding("identity"));
		assertString(x.getContentType().getValue()).is("text/plain");
		assertString(x.getContentEncoding().getValue()).is("identity");

		x = new BasicHttpResource("foo", null, null);
		assertObject(x.getContentType()).doesNotExist();
		assertObject(x.getContentEncoding()).doesNotExist();

		BasicHttpResource x2 = new BasicHttpResource(new StringReader("foo")) {
			@Override
			protected byte[] readBytes(Object o) throws IOException {
				throw new IOException("bad");
			}
		};
		x2.cache();
		assertLong(x2.getContentLength()).is(-1l);
		assertThrown(()->x2.writeTo(new ByteArrayOutputStream())).contains("bad");
	}

	@Test
	public void a02_header_String_Object() throws Exception {
		BasicHttpResource x = of("foo").header("Foo","bar").header("Foo","baz").header(null,"bar").header("foo",null);
		assertString(x.getStringHeader("Foo")).is("baz");
		assertString(x.getStringHeader("Bar")).doesNotExist();
		assertString(x.getFirstHeader("Foo").toString()).is("Foo: bar");
		assertString(x.getLastHeader("Foo").toString()).is("Foo: baz");
		assertObject(x.getFirstHeader("Bar")).doesNotExist();
		assertObject(x.getLastHeader("Bar")).doesNotExist();
		assertObject(x.getHeaders()).asJson().is("['Foo: bar','Foo: baz']");
	}

	@Test
	public void a03_header_Header() throws Exception {
		BasicHttpResource x = of("foo").header(null).header(header("Foo","bar")).header(header("Foo","baz")).header(header(null,"bar")).header(header("Bar",null)).header(null);
		assertString(x.getStringHeader("Foo")).is("baz");
		assertString(x.getStringHeader("Bar")).doesNotExist();
		assertString(x.getFirstHeader("Foo").toString()).is("Foo: bar");
		assertString(x.getLastHeader("Foo").toString()).is("Foo: baz");
		assertObject(x.getFirstHeader("Bar").getValue()).doesNotExist();
		assertObject(x.getLastHeader("Bar").getValue()).doesNotExist();
		assertObject(x.getHeaders()).asJson().is("[null,'Foo: bar','Foo: baz','null: bar','Bar: null',null]");
	}

	@Test
	public void a04_headers_List() throws Exception {
		BasicHttpResource x = of("foo").headers(AList.of(header("Foo","bar"),header("Foo","baz"),header(null,"bar"),header("Bar",null),null));
		assertString(x.getStringHeader("Foo")).is("baz");
		assertString(x.getStringHeader("Bar")).doesNotExist();
		assertString(x.getFirstHeader("Foo").toString()).is("Foo: bar");
		assertString(x.getLastHeader("Foo").toString()).is("Foo: baz");
		assertObject(x.getFirstHeader("Bar").getValue()).doesNotExist();
		assertObject(x.getLastHeader("Bar").getValue()).doesNotExist();
		assertObject(x.getHeaders()).asJson().is("['Foo: bar','Foo: baz','null: bar','Bar: null',null]");
	}

	@Test
	public void a05_headers_array() throws Exception {
		BasicHttpResource x = of("foo").headers(header("Foo","bar"),header("Foo","baz"),header(null,"bar"),header("Bar",null),null);
		assertString(x.getStringHeader("Foo")).is("baz");
		assertString(x.getStringHeader("Bar")).doesNotExist();
		assertString(x.getFirstHeader("Foo").toString()).is("Foo: bar");
		assertString(x.getLastHeader("Foo").toString()).is("Foo: baz");
		assertObject(x.getFirstHeader("Bar").getValue()).doesNotExist();
		assertObject(x.getLastHeader("Bar").getValue()).doesNotExist();
		assertObject(x.getHeaders()).asJson().is("['Foo: bar','Foo: baz','null: bar','Bar: null',null]");
	}


	@Test
	public void a06_chunked() throws Exception {
		BasicHttpResource x1 = of("foo").chunked();
		assertBoolean(x1.isChunked()).isTrue();
		BasicHttpResource x2 = of("foo");
		assertBoolean(x2.isChunked()).isFalse();
	}

	@Test
	public void a07_chunked_boolean() throws Exception {
		BasicHttpResource x1 = of("foo").chunked(true);
		assertBoolean(x1.isChunked()).isTrue();
		BasicHttpResource x2 = of("foo").chunked(false);
		assertBoolean(x2.isChunked()).isFalse();
	}

	@Test
	public void a08_contentType_String() throws Exception {
		BasicHttpResource x1 = of("foo").contentType("text/plain");
		assertString(x1.getContentType().getValue()).is("text/plain");
		BasicHttpResource x2 = of("foo").contentType((String)null);
		assertObject(x2.getContentType()).doesNotExist();
	}

	@Test
	public void a09_contentEncoding_String() throws Exception {
		BasicHttpResource x1 = of("foo").contentEncoding("identity");
		assertString(x1.getContentEncoding().getValue()).is("identity");
		BasicHttpResource x2 = of("foo").contentEncoding((String)null);
		assertObject(x2.getContentEncoding()).doesNotExist();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return new BasicHeader(name, val);
	}
}
