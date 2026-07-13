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
package org.apache.juneau.marshall.jsonl;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the JSONL databind session paths ({@link JsonlParserSession#doParse} /
 * {@link JsonlSerializerSession#doSerialize}) and the {@link JsonlTokenWriter} structural surface
 * and {@code forOutput} type dispatch not reached by the higher-level round-trip tests.
 */
@SuppressWarnings({
	"resource" // Writers wrap in-memory targets; the test closes them where it matters.
})
class JsonlSessionCoverage_Test extends TestBase {

	public static class Bean {
		public int x;

		public Bean() {}
		public Bean(int x) { this.x = x; }
	}

	// =================================================================================
	// A. JsonlParserSession.doParse
	// =================================================================================

	@Nested class A_parse extends TestBase {

		@Test void a01_singleObject() throws Exception {
			var b = JsonlParser.DEFAULT.parse("{\"x\":7}", Bean.class);
			assertBean(b, "x", "7");
		}

		@Test void a02_singleObjectSkipsLeadingBlankLines() throws Exception {
			var b = JsonlParser.DEFAULT.parse("\n   \n{\"x\":9}\n", Bean.class);
			assertBean(b, "x", "9");
		}

		@Test void a03_nullInputYieldsNull() throws Exception {
			assertNull(JsonlParser.DEFAULT.parse((String) null, Bean.class));
		}

		@Test void a04_blankOnlyInputYieldsNull() throws Exception {
			assertNull(JsonlParser.DEFAULT.parse("\n   \n", Bean.class));
		}

		@Test void a05_collectionOfRecords() throws Exception {
			List<?> l = JsonlParser.DEFAULT.parse("{\"x\":1}\n{\"x\":2}", List.class, Bean.class);
			assertEquals(2, l.size());
			assertBean(l.get(0), "x", "1");
			assertBean(l.get(1), "x", "2");
		}

		@Test void a06_arrayOfRecords() throws Exception {
			var a = JsonlParser.DEFAULT.parse("{\"x\":3}\n{\"x\":4}", Bean[].class);
			assertEquals(2, a.length);
			assertBean(a[0], "x", "3");
			assertBean(a[1], "x", "4");
		}

		@Test void a07_collectionSkipsEmbeddedBlankLines() throws Exception {
			List<?> l = JsonlParser.DEFAULT.parse("{\"x\":1}\n\n   \n{\"x\":2}\n", List.class, Bean.class);
			assertEquals(2, l.size());
			assertBean(l.get(0), "x", "1");
			assertBean(l.get(1), "x", "2");
		}

		@Test void a08_malformedSingleObjectPropagatesParseException() {
			// A parse failure unwinds through the outer parser-reader try-with-resources (exception path).
			assertThrows(ParseException.class, () -> JsonlParser.DEFAULT.parse("{bad", Bean.class));
		}

		@Test void a09_malformedCollectionElementPropagatesParseException() {
			assertThrows(ParseException.class, () -> JsonlParser.DEFAULT.parse("{\"x\":1}\n{bad", List.class, Bean.class));
		}
	}

	// =================================================================================
	// B. JsonlSerializerSession.doSerialize
	// =================================================================================

	@Nested class B_serialize extends TestBase {

		@Test void b01_singleObject() throws Exception {
			assertEquals("{\"x\":1}\n", JsonlSerializer.DEFAULT.serializeToString(new Bean(1)));
		}

		@Test void b02_collection() throws Exception {
			var out = JsonlSerializer.DEFAULT.serializeToString(List.of(new Bean(1), new Bean(2)));
			assertEquals("{\"x\":1}\n{\"x\":2}\n", out);
		}

		@Test void b03_array() throws Exception {
			var out = JsonlSerializer.DEFAULT.serializeToString(new Bean[]{new Bean(5), new Bean(6)});
			assertEquals("{\"x\":5}\n{\"x\":6}\n", out);
		}

		@Test void b04_streamable() throws Exception {
			// A Stream is streamable (not a Collection/array) — exercises the forEachStreamableEntry branch.
			Stream<Bean> stream = Stream.of(new Bean(7), new Bean(8));
			var out = JsonlSerializer.DEFAULT.serializeToString(stream);
			assertEquals("{\"x\":7}\n{\"x\":8}\n", out);
		}

		@Test void b05_nullValueUsesScalarBranch() throws Exception {
			// Null has no collection/array/streamable ClassMeta — exercises the else (single-value) branch.
			assertEquals("null\n", JsonlSerializer.DEFAULT.serializeToString(null));
		}
	}

	// =================================================================================
	// C. JsonlTokenWriter — constructor, structural surface, forOutput dispatch
	// =================================================================================

	@Nested class C_writer extends TestBase {

		private static final JsonTokenWriter.Settings SETTINGS = JsonTokenWriter.Settings.DEFAULT;

		@Test void c01_directConstructorAndStructuralEvents() throws Exception {
			var sw = new StringWriter();
			try (var w = new JsonlTokenWriter(sw, SETTINGS)) {
				w.startObject();
				w.fieldName("a"); w.number(1L);
				w.fieldName("b"); w.string("hi");
				w.fieldName("c"); w.bool(true);
				w.fieldName("d"); w.nil();
				w.endObject();
				w.startArray(); w.number(2L); w.endArray();
			}
			// One newline after each top-level value (object, then array).
			assertEquals("{\"a\":1,\"b\":\"hi\",\"c\":true,\"d\":null}\n[2]\n", sw.toString());
		}

		@Test void c01b_nestedContainersOnlyNewlineAtTopLevel() throws Exception {
			// endObject/endArray that return to depth>0 must NOT emit a newline; only the closing
			// of the outermost container (back to depth 0) does.
			var sw = new StringWriter();
			try (var w = new JsonlTokenWriter(sw, SETTINGS)) {
				w.startArray();
				w.startObject();
				w.fieldName("x"); w.number(1L);
				w.endObject();   // depth 2 -> 1: no newline
				w.endArray();    // depth 1 -> 0: newline
			}
			assertEquals("[{\"x\":1}]\n", sw.toString());
		}

		@Test void c02_topLevelScalarsEachGetNewline() throws Exception {
			var sw = new StringWriter();
			try (var w = new JsonlTokenWriter(sw, SETTINGS)) {
				w.number(1L);
				w.number((Number) 2);
				w.number(3.5d);
				w.number(new java.math.BigDecimal("4.5"));
				w.number(new java.math.BigInteger("5"));
				w.string("s");
				w.bool(false);
				w.nil();
				w.binary(new byte[]{1});
			}
			assertEquals("1\n2\n3.5\n4.5\n5\n\"s\"\nfalse\nnull\n\"" + Base64.getEncoder().encodeToString(new byte[]{1}) + "\"\n", sw.toString());
		}

		@Test void c03_forOutputOutputStream() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = JsonlTokenWriter.forOutput(bos, SETTINGS)) {
				w.number(1L);
			}
			assertEquals("1\n", bos.toString(StandardCharsets.UTF_8));
		}

		@Test void c04_forOutputWriter() throws Exception {
			var sw = new StringWriter();
			try (var w = JsonlTokenWriter.forOutput(sw, SETTINGS)) {
				w.number(1L);
			}
			assertEquals("1\n", sw.toString());
		}

		@Test void c05_forOutputStringBuilder() throws Exception {
			var sb = new StringBuilder();
			try (var w = JsonlTokenWriter.forOutput(sb, SETTINGS)) {
				w.bool(true);
			}
			assertEquals("true\n", sb.toString());
		}

		@Test void c06_forOutputFile() throws Exception {
			var f = File.createTempFile("juneau-jltw-", ".jsonl");
			f.deleteOnExit();
			try (var w = JsonlTokenWriter.forOutput(f, SETTINGS)) {
				w.number(42L);
			}
			assertEquals("42\n", new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
		}

		@Test void c07_forOutputNullRejected() {
			assertThrowsWithMessage(IOException.class, "Output cannot be null.", () -> JsonlTokenWriter.forOutput(null, SETTINGS));
		}

		@Test void c08_forOutputUnsupportedTypeRejected() {
			assertThrowsWithMessage(IOException.class, "Cannot convert object of type", () -> JsonlTokenWriter.forOutput(42, SETTINGS));
		}
	}

	@Nested class D_reader extends TestBase {

		@Test void d01_publicSingleArgConstructor() throws Exception {
			// The public (pipe) constructor with no session — exercises the sessionless reader entry
			// point.  The line-delimited stream yields one object per line.
			try (var r = new JsonlTokenReader(new ParserPipe("{\"x\":1}\n{\"x\":2}\n"))) {
				var objs = 0;
				org.apache.juneau.marshall.stream.TokenType t;
				while ((t = r.next()) != org.apache.juneau.marshall.stream.TokenType.END_OF_STREAM)
					if (t == org.apache.juneau.marshall.stream.TokenType.START_OBJECT)
						objs++;
				assertEquals(2, objs);
			}
		}
	}
}
