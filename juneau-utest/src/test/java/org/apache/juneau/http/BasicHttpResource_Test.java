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
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.nio.file.Files;

import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.header.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicHttpResource_Test {
	@Test
	public void a01_basic() throws Exception {
		File f = Files.createTempFile("test","txt").toFile();

		HttpResource x = stringResource((String)null);

		assertNull(x.getContentType());
		assertBytes(x.getContent()).isNotNull().asString().isEmpty();
		assertNull(x.getContentEncoding());
		assertInteger(x.getHeaders().size()).is(0);

		x = stringResource("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = readerResource(reader("foo"));
		assertBytes(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = byteArrayResource("foo".getBytes());
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = streamResource(inputStream("foo"));
		assertBytes(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = fileResource(f);
		assertBytes(x.getContent()).asString().isEmpty();
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = stringResource("foo").setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = readerResource(reader("foo")).setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = byteArrayResource("foo".getBytes()).setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = streamResource(inputStream("foo")).setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = stringResource((String)null).setCached();
		assertBytes(x.getContent()).isExists().asString().isEmpty();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		x = fileResource(f).setCached();
		assertBytes(x.getContent()).asString().isEmpty();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		assertLong(stringResource("foo").getContentLength()).is(3l);
		assertLong(byteArrayResource("foo".getBytes()).getContentLength()).is(3l);
		assertLong(fileResource(f).getContentLength()).is(0l);

		assertLong(readerResource(reader("foo")).getContentLength()).is(-1l);
		assertLong(readerResource(reader("foo")).setContentLength(3).getContentLength()).is(3l);

		x = stringResource("foo", contentType("text/plain")).setContentEncoding("identity");
		assertString(x.getContentType().getValue()).is("text/plain");
		assertString(x.getContentEncoding().getValue()).is("identity");

		x = stringResource("foo", null).setContentEncoding((String)null);
		assertObject(x.getContentType()).isNull();
		assertObject(x.getContentEncoding()).isNull();
	}

	@Test
	public void a02_header_String_Object() throws Exception {
		HeaderList x = stringResource("foo").addHeader("Foo","bar").addHeader("Foo","baz").addHeader(null,"bar").addHeader("foo",null).getHeaders();
		assertString(x.getFirst("Foo").get().toString()).is("Foo: bar");
		assertString(x.getLast("Foo").get().toString()).is("Foo: baz");
		assertOptional(x.getFirst("Bar")).isNull();
		assertOptional(x.getLast("Bar")).isNull();
		assertObject(x.getAll()).asJson().is("['Foo: bar','Foo: baz']");
	}

	@Test
	public void a03_header_Header() throws Exception {
		HeaderList x = stringResource("foo").addHeaders(header("Foo","bar")).addHeaders(header("Foo","baz")).addHeaders(header("Bar",null)).getHeaders();
		assertString(x.getFirst("Foo").get().toString()).is("Foo: bar");
		assertString(x.getLast("Foo").get().toString()).is("Foo: baz");
		assertObject(x.getFirst("Bar").get().getValue()).isNull();
		assertObject(x.getLast("Bar").get().getValue()).isNull();
		assertObject(x.getAll()).asJson().is("['Foo: bar','Foo: baz','Bar: null']");
	}

	@Test
	public void a04_headers_List() throws Exception {
		HeaderList x = stringResource("foo").addHeaders(header("Foo","bar"),header("Foo","baz"),header("Bar",null),null).getHeaders();
		assertString(x.getFirst("Foo").get().toString()).is("Foo: bar");
		assertString(x.getLast("Foo").get().toString()).is("Foo: baz");
		assertObject(x.getFirst("Bar").get().getValue()).isNull();
		assertObject(x.getLast("Bar").get().getValue()).isNull();
		assertObject(x.getAll()).asJson().is("['Foo: bar','Foo: baz','Bar: null']");
	}

	@Test
	public void a05_headers_array() throws Exception {
		HeaderList x = stringResource("foo").addHeaders(header("Foo","bar"),header("Foo","baz"),header("Bar",null),null).getHeaders();
		assertString(x.getFirst("Foo").get().toString()).is("Foo: bar");
		assertString(x.getLast("Foo").get().toString()).is("Foo: baz");
		assertObject(x.getFirst("Bar").get().getValue()).isNull();
		assertObject(x.getLast("Bar").get().getValue()).isNull();
		assertObject(x.getAll()).asJson().is("['Foo: bar','Foo: baz','Bar: null']");
	}


	@Test
	public void a06_chunked() throws Exception {
		StringResource x1 = (StringResource) stringResource("foo").setChunked();
		assertBoolean(x1.isChunked()).isTrue();
		StringResource x2 = stringResource("foo");
		assertBoolean(x2.isChunked()).isFalse();
	}

	@Test
	public void a07_chunked_boolean() throws Exception {
		StringResource x1 = (StringResource) stringResource("foo").setChunked(true);
		assertBoolean(x1.isChunked()).isTrue();
		StringResource x2 = (StringResource) stringResource("foo").setChunked(false);
		assertBoolean(x2.isChunked()).isFalse();
	}

	@Test
	public void a08_contentType_String() throws Exception {
		StringResource x1 = (StringResource) stringResource("foo").setContentType("text/plain");
		assertString(x1.getContentType().getValue()).is("text/plain");
		StringResource x2 = (StringResource) stringResource("foo").setContentType((String)null);
		assertObject(x2.getContentType()).isNull();
	}

	@Test
	public void a09_contentEncoding_String() throws Exception {
		StringResource x1 = (StringResource) stringResource("foo").setContentEncoding("identity");
		assertString(x1.getContentEncoding().getValue()).is("identity");
		StringResource x2 = (StringResource) stringResource("foo").setContentEncoding((String)null);
		assertObject(x2.getContentEncoding()).isNull();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return new BasicHeader(name, val);
	}
}
