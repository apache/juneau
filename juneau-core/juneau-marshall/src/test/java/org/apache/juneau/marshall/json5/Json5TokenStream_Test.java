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
package org.apache.juneau.marshall.json5;

import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the public JSON5 token-streaming surface ({@link Json5TokenReader} +
 * the JSON5-flavored token writer obtained via {@link Json5Serializer#writeTokens(Object)}).
 */
@SuppressWarnings({
	"resource" // Token readers/writers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class Json5TokenStream_Test extends TestBase {

	// =================================================================================
	// A. Reader — JSON5 dialect relaxations
	// =================================================================================

	@Nested class A_reader extends TestBase {

		@Test void a01_singleQuotedStrings() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("['a','b']")) {
				assertSequence(r,
					TokenType.START_ARRAY,
					TokenType.VALUE_STRING,
					TokenType.VALUE_STRING,
					TokenType.END_ARRAY,
					TokenType.END_OF_STREAM);
			}
			try (var r = Json5Parser.DEFAULT.readTokens("'hi'")) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hi", r.getString());
			}
		}

		@Test void a02_bareIdentifierStrings() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("[foo, bar, baz]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("foo", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("bar", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("baz", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a03_bareIdentifierKeywords() throws Exception {
			// true/false/null bare-identifiers must still resolve to boolean/null tokens, not strings.
			try (var r = Json5Parser.DEFAULT.readTokens("[true, false, null]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertTrue(r.getBool());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertFalse(r.getBool());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a04_bareFieldNames() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("{foo: 1, bar: 2}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("foo", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("bar", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void a05_singleQuotedFieldNames() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("{'foo': 1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("foo", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void a06_trailingCommaInArray() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("[1,2,3,]")) {
				assertSequence(r,
					TokenType.START_ARRAY,
					TokenType.VALUE_NUMBER,
					TokenType.VALUE_NUMBER,
					TokenType.VALUE_NUMBER,
					TokenType.END_ARRAY,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a07_trailingCommaInObject() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("{a:1, b:2,}")) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME,
					TokenType.VALUE_NUMBER,
					TokenType.FIELD_NAME,
					TokenType.VALUE_NUMBER,
					TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a08_missingValuesAsNull() throws Exception {
			// JSON5 allows [,1,] -> [null, 1, null]
			try (var r = Json5Parser.DEFAULT.readTokens("[,1,]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a09_comments() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("/* leading */ [1, /* mid */ 2 /* trail */]")) {
				assertSequence(r,
					TokenType.START_ARRAY,
					TokenType.VALUE_NUMBER,
					TokenType.VALUE_NUMBER,
					TokenType.END_ARRAY);
			}
		}

		@Test void a10_capability() throws Exception {
			assertInstanceOf(TokenReadable.class, Json5Parser.DEFAULT);
			try (var r = Json5Parser.DEFAULT.readTokens("null")) {
				assertReaderStreaming(r);
			}
		}
	}

	// =================================================================================
	// B. Writer — JSON5 dialect (single-quoted strings, simpleAttrs unquoted field names)
	// =================================================================================

	@Nested class B_writer extends TestBase {

		@Test void b01_defaultQuoteCharIsSingle() throws Exception {
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				w.startObject();
				w.fieldName("a"); w.string("hi");
				w.endObject();
			}
			// JSON5 default: single-quoted strings + simpleAttrs (unquoted bare field names).
			assertEquals("{a:'hi'}", sb.toString());
		}

		@Test void b02_simpleAttrsUnquotedField() throws Exception {
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				w.startObject();
				w.fieldName("user_id"); w.number(42);
				w.endObject();
			}
			assertEquals("{user_id:42}", sb.toString());
		}

		@Test void b03_simpleAttrsQuotesNonIdentifier() throws Exception {
			// Field names with special chars or reserved words still get quoted.
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				w.startObject();
				w.fieldName("not-an-identifier"); w.number(1);
				w.fieldName("class"); w.string("x");  // reserved word
				w.endObject();
			}
			assertEquals("{'not-an-identifier':1,'class':'x'}", sb.toString());
		}

		@Test void b04_capability() throws Exception {
			assertInstanceOf(TokenWritable.class, Json5Serializer.DEFAULT);
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				assertWriterStreaming(w);
			}
		}
	}

	// =================================================================================
	// C. Round-trip via Json5
	// =================================================================================

	@Nested class C_roundTrip extends TestBase {

		@Test void c01_roundTrip() throws Exception {
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				w.startObject();
				w.fieldName("name"); w.string("alice");
				w.fieldName("age"); w.number(30);
				w.endObject();
			}
			var produced = sb.toString();
			assertEquals("{name:'alice',age:30}", produced);

			try (var r = Json5Parser.DEFAULT.readTokens(produced)) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME,
					TokenType.VALUE_STRING,
					TokenType.FIELD_NAME,
					TokenType.VALUE_NUMBER,
					TokenType.END_OBJECT);
			}
		}
	}

	// =================================================================================
	// D. read() bridge
	// =================================================================================

	@Nested class D_read extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void d01_readBeanFromBareIdentifierKeys() throws Exception {
			try (var r = Json5Parser.DEFAULT.readTokens("{name:'alice', age:30}")) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
			}
		}

		@Test void d02_streamArrayOfBeansWithTrailingComma() throws Exception {
			var seen = new java.util.ArrayList<Bean>();
			try (var r = Json5Parser.DEFAULT.readTokens("[{name:'a',age:1},{name:'b',age:2},]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				while (r.canRead())
					seen.add(r.read(Bean.class));
				assertEquals(TokenType.END_ARRAY, r.next());
			}
			assertEquals(2, seen.size());
			assertEquals("a", seen.get(0).name);
			assertEquals("b", seen.get(1).name);
		}
	}

	// =================================================================================
	// E. object() — POJO-walking writer bridge in JSON5 dialect
	// =================================================================================

	@Nested class E_object extends TestBase {

		public static class EBean {
			public String name;
			public int age;
		}

		@Test void e01_objectEmitsJson5() throws Exception {
			var b = new EBean();
			b.name = "alice";
			b.age = 30;
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				w.object(b);
			}
			// JSON5 dialect: single-quoted strings + simpleAttrs unquoted field names.
			// BeanMap iterates fields alphabetically (age before name); confirm against the
			// canonical Json5Serializer for the same bean.
			assertEquals(Json5Serializer.DEFAULT.writeToString(b), sb.toString());
		}

		@Test void e02_objectMatchesSerializer() throws Exception {
			var m = new java.util.LinkedHashMap<String,Object>();
			m.put("a", 1);
			m.put("b", java.util.List.of("x", "y"));
			var sb = new StringWriter();
			try (var w = Json5Serializer.DEFAULT.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals(Json5Serializer.DEFAULT.writeToString(m), sb.toString());
		}
	}
}
