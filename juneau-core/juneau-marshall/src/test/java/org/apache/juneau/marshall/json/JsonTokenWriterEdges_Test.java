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
package org.apache.juneau.marshall.json;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Edge-branch coverage for {@link JsonTokenWriter}: the public constructors, the JSON5
 * bare-identifier mode, the full string-escape table, non-finite / null number handling, deep
 * container nesting beyond the 32-level bit-packed stack, the depth-underflow guard, and the
 * {@link JsonTokenWriter#forOutput(Object, JsonTokenWriter.Settings)} type dispatch.
 */
@SuppressWarnings({
	"resource" // Writers wrap in-memory targets; the test closes them where it matters.
})
class JsonTokenWriterEdges_Test extends TestBase {

	private static final JsonTokenWriter.Settings DEFAULT = JsonTokenWriter.Settings.DEFAULT;

	@Test void a01_writerOverWriterConstructorDefaultSettings() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			w.startArray().number(1L).endArray();
		}
		assertEquals("[1]", sw.toString());
	}

	@Test void a02_writerOverWriterConstructorExplicitSettings() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw, DEFAULT)) {
			w.bool(true);
		}
		assertEquals("true", sw.toString());
	}

	@Test void a03_simpleAttrsBareIdentifiers() throws Exception {
		// JSON5 "lax attribute" mode: valid bare identifiers are emitted unquoted; reserved words,
		// names that start with a digit, names with non-identifier chars, and empty names stay quoted.
		var settings = new JsonTokenWriter.Settings(false, 100, '"', false, false, true, PojoWalker.Options.DEFAULT, false);
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw, settings)) {
			w.startObject();
			w.fieldName("validName"); w.number(1L);
			w.fieldName("true");      w.number(2L);   // reserved word → quoted
			w.fieldName("9x");        w.number(3L);   // starts with digit → quoted
			w.fieldName("has space"); w.number(4L);   // non-identifier char → quoted
			w.fieldName("");          w.number(5L);   // empty → quoted
			w.fieldName("_ok");       w.number(6L);   // leading underscore → bare
			w.endObject();
		}
		assertEquals("{validName:1,\"true\":2,\"9x\":3,\"has space\":4,\"\":5,_ok:6}", sw.toString());
	}

	@Test void a04_stringEscapeTableNoSolidus() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			w.string("\\\b\f\n\r\t/\u0001");
		}
		// Forward slash stays literal when escapeSolidus is off; 0x01 becomes a \\u escape.
		assertEquals("\"\\\\\\b\\f\\n\\r\\t/\\u0001\"", sw.toString());
	}

	@Test void a05_escapeSolidus() throws Exception {
		var settings = new JsonTokenWriter.Settings(false, 100, '"', true, false, false, PojoWalker.Options.DEFAULT, false);
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw, settings)) {
			w.string("a/b");
		}
		assertEquals("\"a\\/b\"", sw.toString());
	}

	@Test void a06_numberNumberNonNull() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			w.startArray();
			w.number((Number) Integer.valueOf(5));
			w.endArray();
		}
		assertEquals("[5]", sw.toString());
	}

	@Test void a07_nullBoxedNumbersEmitNull() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			w.startArray();
			w.number((Number) null);
			w.number((BigDecimal) null);
			w.number((BigInteger) null);
			w.endArray();
		}
		assertEquals("[null,null,null]", sw.toString());
	}

	@Test void a08_binaryNullEmitsNull() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			w.binary(null);
		}
		assertEquals("null", sw.toString());
	}

	@Test void a09_nonFiniteDoublesRejected() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			assertThrows(IOException.class, () -> w.number(Double.POSITIVE_INFINITY));
			assertThrows(IOException.class, () -> w.number(Double.NEGATIVE_INFINITY));
			assertThrows(IOException.class, () -> w.number(Double.NaN));
		}
	}

	@Test void a10_deepNestingBeyondBitPackedStack() throws Exception {
		// 50 levels exceeds the 32-level bit-packed stack AND forces the int[] overflow array to
		// grow twice (initial size 16, then doubled), exercising the grow + array-copy paths.
		var depth = 50;
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			for (var i = 0; i < depth; i++)
				w.startArray();
			w.number(1L);
			for (var i = 0; i < depth; i++)
				w.endArray();
		}
		var expected = "[".repeat(depth) + "1" + "]".repeat(depth);
		assertEquals(expected, sw.toString());
	}

	@Test void a11_endContainerWithNoStartRejected() throws Exception {
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw)) {
			assertThrowsWithMessage(IllegalStateException.class, "no matching start-container", w::endObject);
		}
	}

	@Test void a12_forOutputWriter() throws Exception {
		var sw = new StringWriter();
		try (var w = JsonTokenWriter.forOutput(sw, DEFAULT)) {
			w.number(1L);
		}
		assertEquals("1", sw.toString());
	}

	@Test void a13_forOutputOutputStream() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = JsonTokenWriter.forOutput(bos, DEFAULT)) {
			w.string("x");
		}
		assertEquals("\"x\"", bos.toString(StandardCharsets.UTF_8));
	}

	@Test void a14_forOutputFile() throws Exception {
		var f = File.createTempFile("juneau-jtw-", ".json");
		f.deleteOnExit();
		try (var w = JsonTokenWriter.forOutput(f, DEFAULT)) {
			w.number(42L);
		}
		assertEquals("42", new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
	}

	@Test void a15_forOutputStringBuilder() throws Exception {
		var sb = new StringBuilder();
		try (var w = JsonTokenWriter.forOutput(sb, DEFAULT)) {
			w.bool(false);
		}
		assertEquals("false", sb.toString());
	}

	@Test void a16_forOutputNullRejected() {
		assertThrowsWithMessage(IOException.class, "Output cannot be null.", () -> JsonTokenWriter.forOutput(null, DEFAULT));
	}

	@Test void a17_forOutputUnsupportedTypeRejected() {
		assertThrowsWithMessage(IOException.class, "Cannot convert object of type", () -> JsonTokenWriter.forOutput(42, DEFAULT));
	}

	@Test void a18_disableObjectThrows() throws Exception {
		// Formats whose serialize() produces non-standard JSON (schema/canonical) set disableObject.
		var settings = new JsonTokenWriter.Settings(false, 100, '"', false, false, false, PojoWalker.Options.DEFAULT, true);
		var sw = new StringWriter();
		try (var w = new JsonTokenWriter(sw, settings)) {
			assertThrows(UnsupportedOperationException.class, () -> w.object(42));
		}
	}

	@Test void a19_endContainerKindMismatchBothDirections() throws Exception {
		// Covers nameOfContext for the OBJECT and ARRAY arms (expected-vs-actual mismatch messages).
		try (var w = new JsonTokenWriter(new StringWriter())) {
			w.startObject();
			assertThrowsWithMessage(IllegalStateException.class, "expected array, got object", w::endArray);
		}
		try (var w = new JsonTokenWriter(new StringWriter())) {
			w.startArray();
			assertThrowsWithMessage(IllegalStateException.class, "expected object, got array", w::endObject);
		}
	}
}
