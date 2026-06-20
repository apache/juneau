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
package org.apache.juneau.marshall.yaml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class YamlMarshaller_Test extends TestBase {

	@Test void a01_writeAndRead() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		m.put("a", "1");
		m.put("b", 2);

		String yaml = Yaml.DEFAULT.of(m);
		assertNotNull(yaml);

		JsonMap parsed = Yaml.DEFAULT.to(yaml, JsonMap.class);
		assertEquals("1", parsed.getString("a"));
		assertEquals(2, parsed.getInt("b"));
	}

	@Test void a02_roundTripString() throws Exception {
		String original = "hello world";
		String yaml = Yaml.DEFAULT.of(original);
		String parsed = Yaml.DEFAULT.to(yaml, String.class);
		assertEquals(original, parsed);
	}

	@Test void a03_roundTripNumber() throws Exception {
		int original = 42;
		String yaml = Yaml.DEFAULT.of(original);
		int parsed = Yaml.DEFAULT.to(yaml, int.class);
		assertEquals(original, parsed);
	}

	@Test void a04_roundTripBoolean() throws Exception {
		String yaml = Yaml.DEFAULT.of(true);
		boolean parsed = Yaml.DEFAULT.to(yaml, boolean.class);
		assertTrue(parsed);
	}

	@Test void a05_roundTripNull() throws Exception {
		String yaml = Yaml.DEFAULT.of(null);
		assertEquals("null", yaml);
		assertNull(Yaml.DEFAULT.to(yaml, String.class));
	}

	@Test void a06_roundTripList() throws Exception {
		var original = List.of("a", "b", "c");
		String yaml = Yaml.DEFAULT.of(original);
		JsonList parsed = Yaml.DEFAULT.to(yaml, JsonList.class);
		assertEquals(3, parsed.size());
	}
}
