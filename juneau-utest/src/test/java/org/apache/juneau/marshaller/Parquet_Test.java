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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Parquet} marshaller.
 */
class Parquet_Test extends TestBase {

	public static class Bean {
		public String x = "test";
		public int y = 42;
	}

	@Test
	void a01_of() throws Exception {
		var a = new Bean();
		var bytes = Parquet.of(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a02_to() throws Exception {
		var a = new Bean();
		var bytes = Parquet.of(a);
		var b = Parquet.to(bytes, Bean.class);
		assertBeans(b, "x,y", "test,42");
	}

	@Test
	void a03_roundTrip() throws Exception {
		var a = new Bean();
		var bytes = Parquet.of(a);
		var b = Parquet.to(bytes, Bean.class);
		var bytes2 = Parquet.of(b);
		var c = Parquet.to(bytes2, Bean.class);
		assertBeans(c, "x,y", "test,42");
	}

	@Test
	void a04_ofToOutputStream() throws Exception {
		var a = new Bean();
		var out = new ByteArrayOutputStream();
		Parquet.DEFAULT.write(a, out);
		var bytes = out.toByteArray();
		assertTrue(bytes.length > 0);
		var b = Parquet.to(bytes, Bean.class);
		assertBeans(b, "x,y", "test,42");
	}

	@Test
	void a05_toFromInputStream() throws Exception {
		var a = new Bean();
		var bytes = Parquet.of(a);
		try (var is = new ByteArrayInputStream(bytes)) {
			var b = Parquet.to(is.readAllBytes(), Bean.class);
			assertBeans(b, "x,y", "test,42");
		}
	}

	@Test
	void a06_collectionRoundTrip() throws Exception {
		var a = new Bean();
		a.x = "first";
		a.y = 1;
		var b = new Bean();
		b.x = "second";
		b.y = 2;
		var list = java.util.List.of(a, b);
		var bytes = Parquet.of(list);
		var parsed = Parquet.to(bytes, Bean.class);
		assertBeans(parsed, "x,y", "first,1", "second,2");
	}
}
