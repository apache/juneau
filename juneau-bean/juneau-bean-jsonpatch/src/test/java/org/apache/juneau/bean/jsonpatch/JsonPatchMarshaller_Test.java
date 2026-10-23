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
package org.apache.juneau.bean.jsonpatch;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Type;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link JsonPatchMarshaller}, the DEFAULT serializer/parser convenience pair for {@code juneau-bean-jsonpatch}.
 */
class JsonPatchMarshaller_Test extends TestBase {

	@Test
	void a01_of_singleOperation() {
		var op = new AddOp("/a/b/c", "foo");
		var j = JsonPatchMarshaller.of(op);
		assertTrue(j.contains("\"op\":\"add\""), () -> j);
		assertTrue(j.contains("\"path\":\"/a/b/c\""), () -> j);
		assertTrue(j.contains("\"value\":\"foo\""), () -> j);
	}

	@Test
	void a02_to_singleOperation_roundTrip() {
		var op = new AddOp("/a/b/c", "foo");
		var j = JsonPatchMarshaller.of(op);
		var back = JsonPatchMarshaller.to(j, JsonPatchOperation.class);
		assertInstanceOf(AddOp.class, back);
		assertEquals("/a/b/c", back.getPath());
		assertEquals("foo", ((AddOp) back).getValue());
	}

	@Test
	void a03_patchDocumentRoundTrip_allSixOps() {
		var patch = new JsonPatch()
			.append(new AddOp("/a", 1))
			.append(new RemoveOp("/b"))
			.append(new ReplaceOp("/c", "x"))
			.append(new MoveOp("/d", "/e"))
			.append(new CopyOp("/f", "/g"))
			.append(new TestOp("/h", true));

		var j = JsonPatchMarshaller.of(patch);
		var back = JsonPatchMarshaller.to(j, JsonPatch.class);

		assertSize(6, back);
		assertInstanceOf(AddOp.class, back.get(0));
		assertInstanceOf(RemoveOp.class, back.get(1));
		assertInstanceOf(ReplaceOp.class, back.get(2));
		assertInstanceOf(MoveOp.class, back.get(3));
		assertInstanceOf(CopyOp.class, back.get(4));
		assertInstanceOf(TestOp.class, back.get(5));
	}

	@Test
	void a04_defaultInstance_matchesStaticShortcuts() {
		var op = new RemoveOp("/x");
		assertEquals(JsonPatchMarshaller.of(op), JsonPatchMarshaller.DEFAULT.write(op));
	}

	@Test
	void a05_readerShortcut() {
		var op = new AddOp("/x", 1);
		var j = JsonPatchMarshaller.of(op);
		var back = JsonPatchMarshaller.to(new StringReader(j), JsonPatchOperation.class);
		assertInstanceOf(AddOp.class, back);
		assertEquals("/x", back.getPath());
	}

	@Test
	void a06_writerShortcut() {
		var op = new AddOp("/x", 1);
		var sw = new StringWriter();
		JsonPatchMarshaller.of(op, sw);
		assertTrue(sw.toString().contains("\"op\":\"add\""));
	}

	@Test
	void a07_customInstance_withOwnSerializerParser() {
		var m = new JsonPatchMarshaller(JsonPatchMarshaller.SERIALIZER, JsonPatchMarshaller.PARSER);
		var op = new TestOp("/x", "foo");
		var j = m.write(op);
		var back = m.read(j, JsonPatchOperation.class);
		assertInstanceOf(TestOp.class, back);
	}

	private static final class FailingReader extends Reader {
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			throw new IOException("read-boom");
		}

		@Override
		public void close() {
			// no-op by design: test stub
		}
	}

	private static final class FailingWriter extends Writer {
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			throw new IOException("write-boom");
		}

		@Override
		public void flush() {
			// no-op by design: test stub
		}

		@Override
		public void close() {
			// no-op by design: test stub
		}
	}

	@Test
	void a08_stringShortcut_typeArgs() {
		var j = JsonPatchMarshaller.of(new JsonPatch(new AddOp("/x", 1), new RemoveOp("/y")));
		var back = JsonPatchMarshaller.<JsonPatch>to(j, (Type)JsonPatch.class);
		assertSize(2, back);
		assertInstanceOf(AddOp.class, back.get(0));
		assertBean(back.get(0), "path,value", "/x,1");
		assertInstanceOf(RemoveOp.class, back.get(1));
		assertBean(back.get(1), "path", "/y");
	}

	@Test
	void a09_readerShortcut_typeArgs() {
		var j = JsonPatchMarshaller.of(new JsonPatch(new MoveOp("/a", "/b")));
		var back = JsonPatchMarshaller.<JsonPatch>to(new StringReader(j), (Type)JsonPatch.class);
		assertSize(1, back);
		assertInstanceOf(MoveOp.class, back.get(0));
		assertBean(back.get(0), "path,from", "/a,/b");
	}

	@Test
	void a10_readerShortcut_ioErrorRethrownAsParseException() {
		var ex = assertThrowsWithMessage(ParseException.class, "read-boom", () -> JsonPatchMarshaller.to(new FailingReader(), JsonPatchOperation.class));
		assertInstanceOf(IOException.class, ex.getCause());
	}

	@Test
	void a11_readerShortcut_typeArgs_ioErrorRethrownAsParseException() {
		var ex = assertThrowsWithMessage(ParseException.class, "read-boom", () -> JsonPatchMarshaller.to(new FailingReader(), (Type)JsonPatch.class));
		assertInstanceOf(IOException.class, ex.getCause());
	}

	@Test
	void a12_writerShortcut_ioErrorRethrownAsSerializeException() {
		var ex = assertThrowsWithMessage(SerializeException.class, "write-boom", () -> JsonPatchMarshaller.of(new AddOp("/x", 1), new FailingWriter()));
		assertInstanceOf(IOException.class, ex.getCause());
	}

	@Test
	void a13_writerShortcut_nullOutputRethrownAsSerializeException() {
		assertThrowsWithMessage(SerializeException.class, "Output cannot be null.", () -> JsonPatchMarshaller.of(new AddOp("/x", 1), (Writer)null));
	}
}
