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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@code juneau-bean-jsonpatch} wire beans.
 */
class JsonPatch_RoundTrip_Test {

	private static final JsonSerializer SER =
		JsonSerializer.create()
			.addBeanTypes()
			.addRootType()
			.typePropertyName(JsonPatchOperation.class, "op")
			.build();

	private static final JsonParser PAR =
		JsonParser.create()
			.typePropertyName(JsonPatchOperation.class, "op")
			.build();

	private static void assertJsonRoundTrip(Object bean, Class<?> type) {
		var j1 = SER.write(bean);
		var copy = PAR.read(j1, type);
		var j2 = SER.write(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	@Nested
	class A_IndividualOps {

		@Test
		void addOp_roundTrip() {
			var op = new AddOp("/a/b/c", JsonMap.of("k", "v"));
			assertJsonRoundTrip(op, JsonPatchOperation.class);
		}

		@Test
		void removeOp_roundTrip() {
			var op = new RemoveOp("/a/b/c");
			assertJsonRoundTrip(op, JsonPatchOperation.class);
		}

		@Test
		void replaceOp_roundTrip() {
			var op = new ReplaceOp("/a/b/c", 42);
			assertJsonRoundTrip(op, JsonPatchOperation.class);
		}

		@Test
		void moveOp_roundTrip() {
			var op = new MoveOp("/a/b/c", "/a/b/d");
			assertJsonRoundTrip(op, JsonPatchOperation.class);
		}

		@Test
		void copyOp_roundTrip() {
			var op = new CopyOp("/a/b/c", "/a/b/e");
			assertJsonRoundTrip(op, JsonPatchOperation.class);
		}

		@Test
		void testOp_roundTrip() {
			var op = new TestOp("/a/b/c", "foo");
			assertJsonRoundTrip(op, JsonPatchOperation.class);
		}
	}

	@Nested
	class B_OpsContainNullValuesAndDefaultConstructors {

		@Test
		void addOp_defaultsAreNull() {
			var op = new AddOp();
			assertNull(op.getPath());
			assertNull(op.getValue());
			op.setValue("v").setPath("/x");
			assertEquals("/x", op.getPath());
			assertEquals("v", op.getValue());
		}

		@Test
		void removeOp_defaultsAreNull() {
			var op = new RemoveOp();
			assertNull(op.getPath());
			op.setPath("/x");
			assertEquals("/x", op.getPath());
		}

		@Test
		void replaceOp_defaultsAreNull() {
			var op = new ReplaceOp();
			assertNull(op.getPath());
			assertNull(op.getValue());
			op.setValue(99).setPath("/x");
			assertEquals("/x", op.getPath());
			assertEquals(99, op.getValue());
		}

		@Test
		void moveOp_defaultsAreNull() {
			var op = new MoveOp();
			assertNull(op.getPath());
			assertNull(op.getFrom());
			op.setFrom("/y").setPath("/x");
			assertEquals("/x", op.getPath());
			assertEquals("/y", op.getFrom());
		}

		@Test
		void copyOp_defaultsAreNull() {
			var op = new CopyOp();
			assertNull(op.getPath());
			assertNull(op.getFrom());
			op.setFrom("/y").setPath("/x");
			assertEquals("/x", op.getPath());
			assertEquals("/y", op.getFrom());
		}

		@Test
		void testOp_defaultsAreNull() {
			var op = new TestOp();
			assertNull(op.getPath());
			assertNull(op.getValue());
			op.setValue("v").setPath("/x");
			assertEquals("/x", op.getPath());
			assertEquals("v", op.getValue());
		}
	}

	@Nested
	class C_PolymorphicDispatch {

		@Test
		void allSixOps_roundTripViaPatchDocument() {
			var patch = new JsonPatch()
				.append(new AddOp("/a/b/c", "foo"))
				.append(new RemoveOp("/a/b/c"))
				.append(new ReplaceOp("/a/b/c", 42))
				.append(new MoveOp("/a/b/c", "/a/b/d"))
				.append(new CopyOp("/a/b/c", "/a/b/e"))
				.append(new TestOp("/a/b/c", "foo"));
			assertJsonRoundTrip(patch, JsonPatch.class);
		}

		@Test
		void parserPicksRightSubclassForEachOp() {
			var patch = new JsonPatch()
				.append(new AddOp("/a", 1))
				.append(new RemoveOp("/b"))
				.append(new ReplaceOp("/c", "x"))
				.append(new MoveOp("/d", "/e"))
				.append(new CopyOp("/f", "/g"))
				.append(new TestOp("/h", true));
			var j = SER.write(patch);
			var back = PAR.read(j, JsonPatch.class);
			assertEquals(6, back.size());
			assertInstanceOf(AddOp.class, back.get(0));
			assertInstanceOf(RemoveOp.class, back.get(1));
			assertInstanceOf(ReplaceOp.class, back.get(2));
			assertInstanceOf(MoveOp.class, back.get(3));
			assertInstanceOf(CopyOp.class, back.get(4));
			assertInstanceOf(TestOp.class, back.get(5));
			assertEquals("/a", back.get(0).getPath());
		}

		@Test
		void wireFormat_isJsonArrayOfOperationObjects() {
			var patch = new JsonPatch().append(new AddOp("/x", 1));
			var j = SER.write(patch);
			assertTrue(j.startsWith("["), () -> "Top-level should be a JSON array: " + j);
			assertTrue(j.contains("\"op\":\"add\""), () -> j);
			assertTrue(j.contains("\"path\":\"/x\""), () -> j);
			assertTrue(j.contains("\"value\":1"), () -> j);
		}
	}

	@Nested
	class D_PatchConstruction {

		@Test
		void varargsConstructor_andAppend() {
			var p = new JsonPatch(new AddOp("/a", 1), new RemoveOp("/b"));
			assertEquals(2, p.size());
			p.append(new TestOp("/c", "v"));
			assertEquals(3, p.size());
		}

		@Test
		void emptyConstructor() {
			var p = new JsonPatch();
			assertTrue(p.isEmpty());
		}

		@Test
		void roundTrips_aListReadFromTheSpec() {
			// Mirrors the JSON Patch RFC 6902 §3 example.
			var p = new JsonPatch().append(
				new TestOp("/a/b/c", "foo"),
				new RemoveOp("/a/b/c"),
				new AddOp("/a/b/c", list("foo", "bar")),
				new ReplaceOp("/a/b/c", 42),
				new MoveOp("/a/b/d", "/a/b/c"),
				new CopyOp("/a/b/e", "/a/b/d")
			);
			assertJsonRoundTrip(p, JsonPatch.class);
		}

		@Test
		void operationToStringDoesNotThrow() {
			assertNotNull(new AddOp("/x", 1).toString());
			assertNotNull(new RemoveOp("/x").toString());
		}
	}
}
