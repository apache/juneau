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
package org.apache.juneau.marshall.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.toml.*;
import org.junit.jupiter.api.*;

/**
 * Tests that the recursive-descent parsers enforce the shared maximum parse depth.
 */
class RecursiveParsers_ParseDepth_Test extends TestBase {

	private static final int DEEP = 2000;

	private static void assertDepthError(Executable e) {
		var t = assertThrows(Exception.class, e);
		assertTrue(String.valueOf(t.getMessage()).contains("depth"), String.valueOf(t.getMessage()));
	}

	interface Executable extends org.junit.jupiter.api.function.Executable {}

	@Test void a01_hjson() {
		var s = "[".repeat(DEEP) + "]".repeat(DEEP);
		assertDepthError(() -> HjsonParser.DEFAULT.read(s, Object.class));
	}

	@Test void a02_toml() {
		var s = "a = " + "[".repeat(DEEP) + "]".repeat(DEEP);
		assertDepthError(() -> TomlParser.DEFAULT.read(s, Object.class));
	}

	@Test void a03_prototext() {
		var s = "a { ".repeat(DEEP) + "}".repeat(DEEP);
		assertDepthError(() -> PrototextParser.DEFAULT.read(s, Object.class));
	}

	@Test void a04_bson() throws Exception {
		var b = new byte[] { 5, 0, 0, 0, 0 };
		for (var i = 0; i < DEEP; i++) {
			var out = new ByteArrayOutputStream();
			var size = 4 + 1 + 2 + b.length + 1;
			out.write(size); out.write(size >> 8); out.write(size >> 16); out.write(size >> 24);
			out.write(0x03);
			out.write('a'); out.write(0);
			out.write(b);
			out.write(0);
			b = out.toByteArray();
		}
		var bytes = b;
		assertDepthError(() -> BsonParser.DEFAULT.read(bytes, Object.class));
	}
}
