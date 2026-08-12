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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.*;

/**
 * Tests {@link JsonPatchMarshaller}, the DEFAULT serializer/parser convenience pair for {@code juneau-bean-jsonpatch}.
 */
class JsonPatchMarshaller_Test {

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

		assertEquals(6, back.size());
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
}
