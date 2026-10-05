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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.json5l.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.uon.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // ParserReader instances are short-lived test fixtures; closing is irrelevant to these assertions.
})
class ParserReader_Test extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var t = "01234567890123456789012345678901234567890123456789";

		// Min buff size is 20.
		var pr = createParserReader(t);
		var r = read(pr);
		assertEquals(t, r);
		pr.close();

		pr = createParserReader(t);
		pr.read();
		pr.unread();
		r = read(pr);
		assertEquals(t, r);
		pr.close();

		pr = createParserReader(t);
		assertEquals('0', (char)pr.peek());
		assertEquals('0', (char)pr.peek());
		r = read(pr);
		assertEquals(t, r);

		var pr2 = createParserReader(t);
		pr2.read();
		pr2.unread();
		assertThrows(IOException.class, pr2::unread);
	}

	//====================================================================================================
	// testMarking
	//====================================================================================================
	@Test void a02_marking() throws Exception {
		var t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";

		// Min buff size is 20.
		var pr = createParserReader(t);
		read(pr, 5);
		pr.mark();
		read(pr, 10);
		Object r = pr.getMarked();
		assertEquals("56789b1234", r);
		r = read(pr);
		assertEquals("56789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789", r);

		// Force doubling of buffer size
		pr = createParserReader(t);
		read(pr, 5);
		pr.mark();
		read(pr, 20);
		r = pr.getMarked();
		assertEquals("56789b123456789c1234", r);
		r = read(pr);
		assertEquals("56789d123456789e123456789f123456789g123456789h123456789i123456789j123456789", r);
	}

	//====================================================================================================
	// testReadStrings
	//====================================================================================================
	@Test void a03_readStrings() throws Exception {
		var t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";

		// Min buff size is 20.
		var pr = createParserReader(t);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789c123456789", pr.read(20));
		assertEquals("d123456789e123456789f123456789", pr.read(30));
		assertEquals("123456789c123456789d123456789e123456789f12345678", pr.getMarked(1, -1));
		assertEquals("g123456789h123456789i123456789j123456789", pr.read(100));
		assertEquals("", pr.read(100));
		pr.close();
	}

	//====================================================================================================
	// testReplace
	//====================================================================================================
	@Test void a04_replace() throws Exception {
		var t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";

		// Min buff size is 20.
		var pr = createParserReader(t);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.replace('x');
		assertEquals("c123456789", pr.read(10));
		assertEquals("b12345678xc123456789", pr.getMarked());
		pr.close();

		pr = createParserReader(t);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.replace('x', 5);
		assertEquals("c123456789", pr.read(10));
		assertEquals("b1234xc123456789", pr.getMarked());
		pr.close();
	}

	//====================================================================================================
	// testDelete
	//====================================================================================================
	@Test void a05_delete() throws Exception {
		var t = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";

		// Min buff size is 20.
		var pr = createParserReader(t);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.delete();
		assertEquals("c123456789", pr.read(10));
		assertEquals("b12345678c123456789", pr.getMarked());
		pr.close();

		pr = createParserReader(t);
		assertEquals("a123456789", pr.read(10));
		pr.mark();
		assertEquals("b123456789", pr.read(10));
		pr.delete(5);
		assertEquals("c123456789", pr.read(10));
		assertEquals("b1234c123456789", pr.getMarked());
		pr.close();
	}

	//====================================================================================================
	// Utility methods
	//====================================================================================================

	//====================================================================================================
	// Holes created by delete()/replace() are removed by position; real DEL (0x7F) characters survive.
	//====================================================================================================
	@Test void a03_holesPreserveRealDelCharacters() throws Exception {
		var del = "\u007F";

		// replace() with offset > 1 collapses an escape sequence; surrounding DELs are preserved.
		var pr = createParserReader("\"a" + del + "\\u0041" + del + "b\"");
		pr.mark();
		read(pr, 4);                  // "a<DEL>\
		pr.delete();                  // drop the backslash
		read(pr, 5);                  // u0041
		pr.replace('A', 6);           // collapse \u0041 (offset covers 6 chars read so far)
		read(pr, 1);                  // <DEL>
		read(pr, 2);                  // b"
		assertEquals("a" + del + "A" + del + "b", pr.getMarked(1, -1));

		// delete(count) with several holes plus real DELs.
		pr = createParserReader("x" + del + "~~" + del + "y~" + del);
		pr.mark();
		read(pr, 4);
		pr.delete(2);
		read(pr, 3);
		pr.delete(1);
		read(pr, 1);
		assertEquals("x" + del + del + "y" + del, pr.getMarked());

		// Holes do not leak into a later mark.
		pr = createParserReader("~a" + del + "b");
		pr.mark();
		read(pr, 1);
		pr.delete();
		assertEquals("", pr.getMarked());
		pr.mark();
		read(pr, 3);
		assertEquals("a" + del + "b", pr.getMarked());
	}


	//====================================================================================================
	// Parsers that collapse escapes through ParserReader.delete()/replace() keep real DEL (U+007F) characters.
	// (YAML, TOML, Prototext and INI do not use delete()/replace(), so they are not exposed to this.)
	//====================================================================================================
	@Test void a04_delSurvivesEscapesInParsers() throws Exception {
		var del = "\u007F";
		var expected = "a" + del + "b\nc" + del;

		assertEquals(expected, JsonParser.DEFAULT.read("\"a" + del + "b\\nc\\u007f\"", String.class));
		assertEquals(expected, Json5Parser.DEFAULT.read("\"a" + del + "b\\nc\\u007f\"", String.class));
		assertEquals(expected, Json5Parser.DEFAULT.read("'a" + del + "b\\nc" + del + "'", String.class));
		assertEquals(expected, JsonParser.DEFAULT.read("\"a" + del + "b\\nc\\u007f\"", Object.class));
		assertEquals("a" + del + "b'c" + del, UonParser.DEFAULT.read("'a" + del + "b~'c" + del + "'", String.class));

		var jl = JsonlParser.DEFAULT.read("\"a" + del + "b\\nc\\u007f\"", String.class);
		assertEquals(expected, jl);
		var j5l = Json5lParser.DEFAULT.read("'a" + del + "b\\nc" + del + "'", String.class);
		assertEquals(expected, j5l);
	}

	private static String read(ParserReader r) throws IOException {
		return read(r, Integer.MAX_VALUE);
	}

	private static String read(ParserReader r, int length) throws IOException {
		var sb = new StringBuilder();
		for (var i = 0; i < length; i++) {
			int c = r.read();
			if (c == -1)
				return sb.toString();
			sb.append((char)c);
		}
		return sb.toString();
	}

	private static ParserReader createParserReader(Object in) throws Exception {
		return new ParserReader(new ParserPipe(in));
	}
}