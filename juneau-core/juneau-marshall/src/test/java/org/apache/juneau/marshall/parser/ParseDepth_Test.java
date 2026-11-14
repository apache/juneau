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

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.*;

import java.util.*;

/**
 * Tests that the text parsers enforce the shared maximum parse depth.
 */
class ParseDepth_Test extends TestBase {

	private static String repeat(String s, int n) {
		return s.repeat(n);
	}

	private static void assertDepthExceeded(Executable r) {
		var e = assertThrows(ParseException.class, r);
		assertTrue(e.getMessage().contains("Maximum parse depth exceeded"), e.getMessage());
	}

	@Test void a01_jsonDeepArrays() {
		var in = repeat("[", 5000) + repeat("]", 5000);
		assertDepthExceeded(() -> JsonParser.DEFAULT.read(in, Object.class));
	}

	@Test void a02_jsonDeepObjects() {
		var in = repeat("{\"a\":", 5000) + "1" + repeat("}", 5000);
		assertDepthExceeded(() -> JsonParser.DEFAULT.read(in, Object.class));
	}

	@Test void a03_jsonWithinLimit() throws Exception {
		var in = repeat("[", 100) + repeat("]", 100);
		assertNotNull(JsonParser.DEFAULT.read(in, Object.class));
	}

	@Test void a04_uonDeepArrays() {
		var in = repeat("@(", 5000) + repeat(")", 5000);
		assertDepthExceeded(() -> UonParser.DEFAULT.read(in, Object.class));
	}

	@Test void a05_xmlDeepElements() {
		var in = repeat("<array>", 5000) + repeat("</array>", 5000);
		assertDepthExceeded(() -> XmlParser.DEFAULT.read(in, Object.class));
	}

	@Test void a06_yamlDeepArrays() {
		var in = repeat("[", 5000) + repeat("]", 5000);
		assertDepthExceeded(() -> YamlParser.DEFAULT.read(in, Object.class));
	}

	@Test void a07_readIntoMapDeep() {
		var in = "{\"a\":" + repeat("[", 5000) + repeat("]", 5000) + "}";
		assertThrows(ParseException.class, () -> JsonParser.DEFAULT.readIntoMap(in, new HashMap<String,Object>(), String.class, Object.class));
	}
}
