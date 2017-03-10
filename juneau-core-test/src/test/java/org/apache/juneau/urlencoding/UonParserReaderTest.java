// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.urlencoding;

import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@SuppressWarnings({"javadoc","resource"})
public class UonParserReaderTest {

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		UonReader r;
		String s, in;
		r = r("f", true);
		assertEquals('f', r.read());
		assertEquals(-1, r.read());

		r = r("%66", true);
		assertEquals('f', r.read());
		assertEquals(-1, r.read());

		r = r("%7D", true);
		assertEquals('}', r.read());
		assertEquals(-1, r.read());

		r = r("%7D%7D", true);
		assertEquals('}', r.read());
		assertEquals('}', r.read());
		assertEquals(-1, r.read());

		r = r("%00%00", true);
		r.mark();
		assertEquals(0, r.read());
		assertEquals(0, r.read());
		assertEquals("\u0000\u0000", r.getMarked());
		assertEquals(-1, r.read());

		in = escape("\u0080");
		r = r(in, true);
		assertEquals('\u0080', r.read());
		assertEquals(-1, r.read());

		in = escape("\u0800");
		r = r(in, true);
		assertEquals('\u0800', r.read());
		assertEquals(-1, r.read());

		in = escape("\uffff");
		r = r(in, true);
		assertEquals('\uffff', r.read());
		assertEquals(-1, r.read());

		// 2-byte codepoint
		s = "¢";
		r = r(escape(s), true);
		assertEquals(s.codePointAt(0), r.read());
		assertEquals(-1, r.read());

		// 3-byte codepoint
		s = "€";
		r = r(escape(s), true);
		assertEquals(s.codePointAt(0), r.readCodePoint());
		assertEquals(-1, r.read());

		// 4-byte codepoint
		s = "𤭢";
		r = r(escape(s), true);
		assertEquals(s.codePointAt(0), r.readCodePoint());
		assertEquals(-1, r.read());

		s = "𤭢𤭢";
		r = r(escape(s), true);
		assertEquals(s.codePointAt(0), r.readCodePoint());
		assertEquals(s.codePointAt(2), r.readCodePoint());
		assertEquals(-1, r.read());

		// Multiple codepoints
		s = "¢€𤭢¢€𤭢";
		in = escape(s);
		r = r(in, true);
		assertEquals(s.codePointAt(0), r.readCodePoint());
		assertEquals(s.codePointAt(1), r.readCodePoint());
		assertEquals(s.codePointAt(2), r.readCodePoint());
		assertEquals(s.codePointAt(4), r.readCodePoint());
		assertEquals(s.codePointAt(5), r.readCodePoint());
		assertEquals(s.codePointAt(6), r.readCodePoint());
		assertEquals(-1, r.read());

		// Multiple codepoints read in small chunks.
		s = "¢€𤭢¢€𤭢";
		String s2;
		int i;
		in = escape(s);
		r = r(in, true);
		char[] buff = new char[2];
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("¢", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("€", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("𤭢", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("¢", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("€", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("𤭢", s2);
		i = r.read(buff, 0, buff.length);
		assertEquals(-1, i);

		// Multiple codepoints read in slightly larger chunks.
		s = "¢€𤭢¢€𤭢";
		in = escape(s);
		r = r(in, true);
		buff = new char[3];
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("¢€", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("𤭢", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("¢€", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("𤭢", s2);
		i = r.read(buff, 0, buff.length);
		assertEquals(-1, i);

		s = "¢€𤭢¢€𤭢";
		in = escape(s);
		r = r(in, true);
		buff = new char[4];
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("¢€𤭢", s2);
		i = r.read(buff, 0, buff.length);
		s2 = new String(buff, 0, i);
		assertEquals("¢€𤭢", s2);
		i = r.read(buff, 0, buff.length);
		assertEquals(-1, i);

		// Reader that only returns 1 character at a time;
		s = "x¢€𤭢x¢€𤭢";
		in = "x" + escape("¢€𤭢") + "x" + escape("¢€𤭢");
		r = new UonReader(new SlowStringReader(in), true);
		assertEquals(s.codePointAt(0), r.readCodePoint());
		assertEquals(s.codePointAt(1), r.readCodePoint());
		assertEquals(s.codePointAt(2), r.readCodePoint());
		assertEquals(s.codePointAt(3), r.readCodePoint());
		assertEquals(s.codePointAt(5), r.readCodePoint());
		assertEquals(s.codePointAt(6), r.readCodePoint());
		assertEquals(s.codePointAt(7), r.readCodePoint());
		assertEquals(s.codePointAt(8), r.readCodePoint());
		assertEquals(-1, r.readCodePoint());
	}

	private String escape(String s) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		byte[] b = s.getBytes("UTF-8");
		for (int i = 0; i < b.length; i++)
			sb.append('%').append(TestUtils.toHex(b[i]));
		return sb.toString();
	}

	private UonReader r(String in, boolean decodeChars) {
		return new UonReader(in, decodeChars);
	}

	private static class SlowStringReader extends Reader {

		String s;
		int i = 0;

		SlowStringReader(String s) {
			this.s = s;
		}

		@Override /* Reader */
		public int read(char[] cbuf, int off, int len) throws IOException {
			if (i >= s.length())
				return -1;
			cbuf[off] = s.charAt(i++);
			return 1;
		}

		@Override /* Reader */
		public void close() throws IOException {
		}

	}
}