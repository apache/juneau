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

import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

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
		var r = (Object)null;

		// Min buff size is 20.
		var pr = createParserReader(t);
		read(pr, 5);
		pr.mark();
		read(pr, 10);
		r = pr.getMarked();
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