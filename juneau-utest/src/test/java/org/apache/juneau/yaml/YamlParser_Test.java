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
package org.apache.juneau.yaml;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class YamlParser_Test extends TestBase {

	@Test void a01_parseString() throws Exception {
		var p = YamlParser.DEFAULT;
		assertEquals("hello", p.parse("hello", String.class));
	}

	@Test void a02_parseQuotedString() throws Exception {
		var p = YamlParser.DEFAULT;
		assertEquals("hello world", p.parse("\"hello world\"", String.class));
		assertEquals("hello world", p.parse("'hello world'", String.class));
	}

	@Test void a03_parseNumber() throws Exception {
		var p = YamlParser.DEFAULT;
		assertEquals(123, p.parse("123", int.class));
		assertEquals(1.5, p.parse("1.5", double.class));
	}

	@Test void a04_parseBoolean() throws Exception {
		var p = YamlParser.DEFAULT;
		assertEquals(true, p.parse("true", boolean.class));
		assertEquals(false, p.parse("false", boolean.class));
	}

	@Test void a05_parseNull() throws Exception {
		var p = YamlParser.DEFAULT;
		assertNull(p.parse("null", String.class));
	}

	@Test void a06_parseFlowMapping() throws Exception {
		var p = YamlParser.DEFAULT;
		JsonMap m = p.parse("{a: 1, b: 2}", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void a07_parseFlowSequence() throws Exception {
		var p = YamlParser.DEFAULT;
		JsonList l = p.parse("[1, 2, 3]", JsonList.class);
		assertEquals(3, l.size());
	}

	@Test void a08_parseBlockMapping() throws Exception {
		var p = YamlParser.DEFAULT;
		JsonMap m = p.parse("a: 1\nb: 2", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void a09_parseBlockSequence() throws Exception {
		var p = YamlParser.DEFAULT;
		JsonList l = p.parse("- a\n- b\n- c", JsonList.class);
		assertEquals(3, l.size());
		assertEquals("a", l.getString(0));
	}

	@Test void a10_parseNestedBlockMapping() throws Exception {
		var p = YamlParser.DEFAULT;
		JsonMap m = p.parse("outer:\n  inner: value", JsonMap.class);
		assertNotNull(m.get("outer"));
	}

	@Test void a11_parseComment() throws Exception {
		var p = YamlParser.DEFAULT;
		assertEquals("hello", p.parse("hello # this is a comment", String.class));
	}

	@Test void a12_parseEmptyInput() throws Exception {
		var p = YamlParser.DEFAULT;
		assertNull(p.parse("", String.class));
	}

	@Test void a13_parseTildeNull() throws Exception {
		var p = YamlParser.DEFAULT;
		assertNull(p.parse("~", String.class));
	}

	public static class DurationBean {
		public java.time.Duration negative;
		public java.time.Duration fractional;
		public java.time.Duration large;
	}

	@Test void a14_parseBeanWithDuration() throws Exception {
		var s = YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = YamlParser.DEFAULT;

		var x = new DurationBean();
		x.negative = java.time.Duration.ofHours(-6);
		x.fractional = java.time.Duration.ofSeconds(20, 345000000);
		x.large = java.time.Duration.ofDays(365);

		String yaml = s.serialize(x);
		var x2 = p.parse(yaml, DurationBean.class);
		assertEquals(x.negative, x2.negative);
		assertEquals(x.fractional, x2.fractional);
		assertEquals(x.large, x2.large);
	}

	public static class ArrayBean {
		public boolean[][] paBoolean;
	}

	@Test void a15_parse2dArrayBean() throws Exception {
		var s = YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = YamlParser.DEFAULT;

		var x = new ArrayBean();
		x.paBoolean = new boolean[][]{{true, false}, {true}};

		String yaml = s.serialize(x);
		System.out.println("2D Array YAML:\n[" + yaml + "]");

		var x2 = p.parse(yaml, ArrayBean.class);
		assertEquals(true, x2.paBoolean[0][0]);
		assertEquals(false, x2.paBoolean[0][1]);
		assertEquals(true, x2.paBoolean[1][0]);
	}
}
