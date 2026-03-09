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
package org.apache.juneau.marshaller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.cbor.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Cbor} marshaller.
 */
class Cbor_Test extends TestBase {

	public static class Bean {
		public String x = "test";
		public int y = 42;
	}

	@Test
	void a01_of() throws Exception {
		var a = new Bean();
		var bytes = Cbor.of(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a02_to() throws Exception {
		var a = new Bean();
		var bytes = Cbor.of(a);
		var b = Cbor.to(bytes, Bean.class);
		assertEquals(a.x, b.x);
		assertEquals(a.y, b.y);
	}

	@Test
	void a03_roundTrip() throws Exception {
		var a = new Bean();
		var bytes = Cbor.of(a);
		var b = Cbor.to(bytes, Bean.class);
		assertEquals("test", b.x);
		assertEquals(42, b.y);
	}

	@Test
	void f04_ofToOutputStream() throws Exception {
		var a = new Bean();
		var out = new ByteArrayOutputStream();
		Cbor.of(a, out);
		var bytes = out.toByteArray();
		assertTrue(bytes.length > 0);
		var b = Cbor.to(bytes, Bean.class);
		assertEquals(a.x, b.x);
		assertEquals(a.y, b.y);
	}

	@Test
	void f05_toFromInputStream() throws Exception {
		var a = new Bean();
		var bytes = Cbor.of(a);
		try (var is = new ByteArrayInputStream(bytes)) {
			var b = Cbor.to(is, Bean.class);
			assertEquals(a.x, b.x);
			assertEquals(a.y, b.y);
		}
	}
}
