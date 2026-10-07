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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json5l.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the {@code application/jsonlines} and {@code application/json5lines} media-type aliases through a
 * full in-process REST round trip against a {@link BasicUniversalConfig} resource.
 */
class JsonLinesMediaTypeAliases_Test extends TestBase {

	public static class Item {
		public String name;
		public int age;
		public Item() {}
		public Item(String name, int age) { this.name = name; this.age = age; }
	}

	@Rest(serializers = JsonlSerializer.class, parsers = JsonlParser.class)
	public static class J extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/items")
		public List<Item> items() { return List.of(new Item("a", 1), new Item("b", 2)); }

		@RestPost("/echo")
		public String echo(@Content List<Item> items) { return items.size() + ":" + items.get(0).name; }
	}

	@Rest(serializers = Json5lSerializer.class, parsers = Json5lParser.class)
	public static class K extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/items")
		public List<Item> items() { return List.of(new Item("a", 1), new Item("b", 2)); }

		@RestPost("/echo")
		public String echo(@Content List<Item> items) { return items.size() + ":" + items.get(0).name; }
	}

	@Rest
	public static class U extends RestServlet implements BasicUniversalConfig {
		private static final long serialVersionUID = 1L;
		@RestGet("/items")
		public List<Item> items() { return List.of(new Item("a", 1), new Item("b", 2)); }
	}

	private static final MockRestClient j = MockRestClient.buildLax(J.class);
	private static final MockRestClient k = MockRestClient.buildLax(K.class);
	private static final MockRestClient u = MockRestClient.buildLax(U.class);

	@Test void a01_acceptJsonlines_returnsJsonl() throws Exception {
		var body = j.get("/items").accept("application/jsonlines").run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("application/jsonl")
			.getContent().asString();
		assertEquals("{\"age\":1,\"name\":\"a\"}\n{\"age\":2,\"name\":\"b\"}", body.trim());
	}

	@Test void a02_acceptJson5lines_returnsJson5l() throws Exception {
		var body = k.get("/items").accept("application/json5lines").run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("application/json5l")
			.getContent().asString();
		assertEquals("{\"age\":1,\"name\":\"a\"}\n{\"age\":2,\"name\":\"b\"}", body.trim());
	}

	@Test void a03_contentTypeJsonlines_parsed() throws Exception {
		j.post("/echo", "{\"name\":\"x\",\"age\":1}\n{\"name\":\"y\",\"age\":2}\n")
			.contentType("application/jsonlines").accept("text/plain").run()
			.assertStatus(200)
			.assertContent("2:x");
	}

	@Test void a04_contentTypeJson5lines_parsed() throws Exception {
		k.post("/echo", "{name:'x',age:1}\n{name:'y',age:2}\n")
			.contentType("application/json5lines").accept("text/plain").run()
			.assertStatus(200)
			.assertContent("2:x");
	}

	@Test void a05_universalConfig_acceptsAliases() throws Exception {
		u.get("/items").accept("application/jsonlines").run().assertStatus(200);
		u.get("/items").accept("application/json5lines").run().assertStatus(200);
	}
}
