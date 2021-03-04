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
import static org.apache.juneau.http.entity.BasicHttpEntity.*;
import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.http.entity.*;
import org.junit.*;

public class BasicHttpEntity_Test {
	@Test
	public void a01_basic() throws Exception {
		BasicHttpEntity x = of(null);
		File f = File.createTempFile("test", "txt");

		assertNull(x.getContentType());
		assertNull(x.getContent());
		assertNull(x.getContentEncoding());

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


		x = of(()->"foo");
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(()->new StringReader("foo"));
		assertStream(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of(()->"foo".getBytes());
		assertStream(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(()->new ByteArrayInputStream("foo".getBytes()));
		assertStream(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of(()->null);
		assertStream(x.getContent()).asString().doesNotExist();
		assertFalse(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(()->f);
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

		BasicHttpEntity x2 = new BasicHttpEntity(new StringReader("foo")) {
			@Override
			protected byte[] readBytes(Object o) throws IOException {
				throw new IOException("bad");
			}
		};
		x2.cache();
		assertLong(x2.getContentLength()).is(-1l);

		assertThrown(()->x2.writeTo(new ByteArrayOutputStream())).contains("bad");
		assertThrown(()->x2.getContent()).contains("bad");
	}

	@Test
	public void a02_contentType_String() throws Exception {
		BasicHttpEntity x1 = of("foo").contentType("text/plain");
		assertString(x1.getContentType().getValue()).is("text/plain");
		BasicHttpEntity x2 = of("foo").contentType((String)null);
		assertObject(x2.getContentType()).doesNotExist();
	}

	@Test
	public void a03_contentEncoding_String() throws Exception {
		BasicHttpEntity x1 = of("foo").contentEncoding("identity");
		assertString(x1.getContentEncoding().getValue()).is("identity");
		BasicHttpEntity x2 = of("foo").contentEncoding((String)null);
		assertObject(x2.getContentEncoding()).doesNotExist();
	}

	@Test
	public void a04_asString() throws Exception {
		BasicHttpEntity x1 = of(new StringReader("foo"));
		assertString(x1.asString()).is("foo");
		BasicHttpEntity x2 = of((String)null);
		assertString(x2.asString()).doesNotExist();
	}

	@Test
	public void a05_asBytes() throws Exception {
		BasicHttpEntity x1 = of(new StringReader("foo"));
		assertBytes(x1.asBytes()).asSpacedHex().is("66 6F 6F");
		BasicHttpEntity x2 = of((String)null);
		assertBytes(x2.asBytes()).doesNotExist();
	}

	@Test
	public void a06_assertString() throws Exception {
		BasicHttpEntity x1 = of(new StringReader("foo"));
		x1.assertString().is("foo");
		BasicHttpEntity x2 = of((String)null);
		x2.assertString().doesNotExist();
	}

	@Test
	public void a07_assertBytes() throws Exception {
		BasicHttpEntity x1 = of(new StringReader("foo"));
		x1.assertBytes().asSpacedHex().is("66 6F 6F");
		BasicHttpEntity x2 = of((String)null);
		x2.assertBytes().doesNotExist();
	}

	@Test
	public void a08_chunked() throws Exception {
		BasicHttpEntity x1 = of("foo").chunked();
		assertBoolean(x1.isChunked()).isTrue();
		BasicHttpEntity x2 = of("foo");
		assertBoolean(x2.isChunked()).isFalse();
	}

	@Test
	public void a09_chunked_boolean() throws Exception {
		BasicHttpEntity x1 = of("foo").chunked(true);
		assertBoolean(x1.isChunked()).isTrue();
		BasicHttpEntity x2 = of("foo").chunked(false);
		assertBoolean(x2.isChunked()).isFalse();
	}

}
