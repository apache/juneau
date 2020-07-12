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
import static org.apache.juneau.http.BasicHttpEntity.*;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

public class BasicHttpEntity_Test {
	@Test
	public void a01_basic() throws Exception {
		BasicHttpEntity x = create();
		File f = File.createTempFile("test", "txt");

		assertNull(x.getContentType());
		assertNull(x.getContent());
		assertNull(x.getContentEncoding());


		x = of("foo");
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(new StringReader("foo"));
		assertStream(x.getContent()).string().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of("foo".getBytes());
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(new ByteArrayInputStream("foo".getBytes()));
		assertStream(x.getContent()).string().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of(null);
		assertStream(x.getContent()).string().doesNotExist();
		assertFalse(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(f);
		assertStream(x.getContent()).string().isEmpty();
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());


		x = of(()->"foo");
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(()->new StringReader("foo"));
		assertStream(x.getContent()).string().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of(()->"foo".getBytes());
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(()->new ByteArrayInputStream("foo".getBytes()));
		assertStream(x.getContent()).string().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = of(()->null);
		assertStream(x.getContent()).string().doesNotExist();
		assertFalse(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = of(()->f);
		assertStream(x.getContent()).string().isEmpty();
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());


		x = of("foo").cache();
		assertStream(x.getContent()).string().is("foo");
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());

		x = of(new StringReader("foo")).cache();
		assertStream(x.getContent()).string().is("foo");
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());

		x = of("foo".getBytes()).cache();
		assertStream(x.getContent()).string().is("foo");
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());

		x = of(new ByteArrayInputStream("foo".getBytes())).cache();
		assertStream(x.getContent()).string().is("foo");
		assertStream(x.getContent()).string().is("foo");
		assertTrue(x.isRepeatable());

		x = of(null).cache();
		assertStream(x.getContent()).string().doesNotExist();
		assertStream(x.getContent()).string().doesNotExist();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		x = of(f).cache();
		assertStream(x.getContent()).string().isEmpty();
		assertStream(x.getContent()).string().isEmpty();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		assertLong(of("foo").getContentLength()).is(3l);
		assertLong(of("foo".getBytes()).getContentLength()).is(3l);
		assertLong(of(f).getContentLength()).is(0l);

		assertLong(of(new StringReader("foo")).getContentLength()).is(-1l);
		assertLong(of(new StringReader("foo")).contentLength(3).getContentLength()).is(3l);

		BasicHttpEntity x2 = new BasicHttpEntity() {
			@Override
			protected byte[] readBytes(Object o) throws IOException {
				throw new IOException("bad");
			}
		};
		x2.cache().content(new StringReader("foo"));
		assertLong(x2.getContentLength()).is(-1l);

		assertThrown(()->x2.writeTo(new ByteArrayOutputStream())).contains("bad");
		assertThrown(()->x2.getContent()).contains("bad");
	}
}
