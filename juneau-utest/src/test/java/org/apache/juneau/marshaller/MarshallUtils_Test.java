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
import static org.apache.juneau.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class MarshallUtils_Test extends TestBase {

	private static final JsonMap MAP = JsonMap.of("a", "b");

	@Test void a01_json() throws Exception {
		assertString("{\"a\":\"b\"}", json(MAP));
	}

	@Test void a02_json5() throws Exception {
		assertString("{a:'b'}", json5(MAP));
	}

	@Test void a03_charFormats_nonNull() throws Exception {
		assertNotNull(jsonl(MAP));
		assertNotNull(jcs(MAP));
		assertNotNull(hjson(MAP));
		assertNotNull(xml(MAP));
		assertNotNull(html(MAP));
		assertNotNull(uon(MAP));
		assertNotNull(urlEncoding(MAP));
		assertNotNull(yaml(MAP));
		assertNotNull(csv("foo"));
		assertNotNull(openApi(MAP));
		assertNotNull(plainText(MAP));
		assertNotNull(markdown(MAP));
		assertNotNull(markdownDoc(MAP));
		assertNotNull(ini(MAP));
		assertNotNull(toml(MAP));
		assertNotNull(hocon(MAP));
		assertNotNull(proto(MAP));
	}

	@Test void a04_binaryFormats() throws Exception {
		assertTrue(msgPack(MAP).length > 0);
		assertTrue(cbor(MAP).length > 0);
		assertTrue(bson(MAP).length > 0);
		assertTrue(parquet(new A04_Bean()).length > 0);
	}

	public static class A04_Bean {
		public String x = "test";
	}

	@Test void b01_parseJson() throws Exception {
		assertString("foo", json(json("foo"), String.class));
	}

	@Test void b02_parseJson5() throws Exception {
		assertString("foo", json5(json5("foo"), String.class));
	}

	@Test void b03_parseCharFormats() throws Exception {
		assertString("foo", jsonl(jsonl("foo"), String.class));
		assertString("foo", jcs(jcs("foo"), String.class));
		assertString("foo", hjson(hjson("foo"), String.class));
		assertString("foo", xml(xml("foo"), String.class));
		assertString("foo", html(html("foo"), String.class));
		assertString("foo", uon(uon("foo"), String.class));
		assertString("foo", urlEncoding(urlEncoding("foo"), String.class));
		assertString("foo", yaml(yaml("foo"), String.class));
		// ini/toml/hocon/proto require a Map or bean at root, not a bare String
		assertEquals("b", ini(ini(MAP), JsonMap.class).getString("a"));
		assertEquals("b", toml(toml(MAP), JsonMap.class).getString("a"));
		assertEquals("b", hocon(hocon(MAP), JsonMap.class).getString("a"));
		assertEquals("b", proto(proto(MAP), JsonMap.class).getString("a"));
	}

	@Test void b04_parseBinaryFormats() throws Exception {
		assertString("foo", msgPack(msgPack("foo"), String.class));
		assertString("foo", cbor(cbor("foo"), String.class));
		assertString("foo", bson(bson("foo"), String.class));
	}

	@Test void b05_parseParquet() throws Exception {
		var a = new A04_Bean();
		List<A04_Bean> parsed = parquet(parquet(a), A04_Bean.class);
		assertEquals(1, parsed.size());
		assertString("test", parsed.get(0).x);
	}
}
