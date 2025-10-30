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
package org.apache.juneau.uon;

import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.io.*;

import org.apache.juneau.parser.*;

/**
 * Same functionality as {@link ParserReader} except automatically decoded <c>%xx</c> escape sequences.
 *
 * <p>
 * Escape sequences are assumed to be encoded UTF-8.  Extended Unicode (&gt;\u10000) is supported.
 *
 * <p>
 * If decoding is enabled, the following character replacements occur so that boundaries are not lost:
 * <ul>
 * 	<li><js>'&amp;'</js> -&gt; <js>'\u0001'</js>
 * 	<li><js>'='</js> -&gt; <js>'\u0002'</js>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UonBasics">UON Basics</a>

 * </ul>
 */
@SuppressWarnings("resource")
public class UonReader extends ParserReader {

	private static int fromHexChar(int c) throws IOException {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return 10 + c - 'a';
		if (c >= 'A' && c <= 'F')
			return 10 + c - 'A';
		throw ioException("Invalid hex character ''{0}'' found in escape pattern.", c);
	}

	private final boolean decodeChars;

	private final char[] buff;

	// Writable properties.
	private int iCurrent, iEnd;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @param decodeChars Whether the input is URL-encoded.
	 * @throws IOException Thrown by underlying stream.
	 */
	public UonReader(ParserPipe pipe, boolean decodeChars) throws IOException {
		super(pipe);
		this.decodeChars = decodeChars;
		if (pipe.isString()) {
			var in = pipe.getInputAsString();
			this.buff = new char[in.length() < 1024 ? in.length() : 1024];
		} else {
			this.buff = new char[1024];
		}
	}

	@Override /* Overridden from Reader */
	public int read(char[] cbuf, int off, int len) throws IOException {

		if (! decodeChars)
			return super.read(cbuf, off, len);

		// Copy any remainder to the beginning of the buffer.
		var remainder = iEnd - iCurrent;
		if (remainder > 0)
			System.arraycopy(buff, iCurrent, buff, 0, remainder);
		iCurrent = 0;

		var expected = buff.length - remainder;

		var x = super.read(buff, remainder, expected);
		if (x == -1 && remainder == 0)
			return -1;

		iEnd = remainder + (x == -1 ? 0 : x);

		var i = 0;
		while (i < len) {
			if (iCurrent >= iEnd)
				return i;
			var c = buff[iCurrent++];
			if (c == '+') {
				cbuf[off + i++] = ' ';
			} else if (c == '&') {
				cbuf[off + i++] = '\u0001';
			} else if (c == '=') {
				cbuf[off + i++] = '\u0002';
			} else if (c != '%') {
				cbuf[off + i++] = c;
			} else {
				var iMark = iCurrent - 1;  // Keep track of current position.

				// Stop if there aren't at least two more characters following '%' in the buffer,
				// or there aren't at least two more positions open in cbuf to handle double-char chars.
				if (iMark + 2 >= iEnd || i + 2 > len) {
					iCurrent--;
					return i;
				}

				var b0 = readEncodedByte();
				int cx;

				// 0xxxxxxx
				if (b0 < 128) {
					cx = b0;

				} else if (b0 < 192) {
					// 10xxxxxx
					throw ioException("Invalid hex value for first escape pattern in UTF-8 sequence: {0}", b0);

				} else if (b0 < 224) {
					// 110xxxxx	10xxxxxx
					// 11000000(192) - 11011111(223)
					cx = readUTF8(b0 - 192, 1);
					if (cx == -1) {
						iCurrent = iMark;
						return i;
					}

				} else if (b0 < 240) {
					// 1110xxxx	10xxxxxx	10xxxxxx
					// 11100000(224) - 11101111(239)
					cx = readUTF8(b0 - 224, 2);
					if (cx == -1) {
						iCurrent = iMark;
						return i;
					}

				} else if (b0 < 248) {
					// 11110xxx	10xxxxxx	10xxxxxx	10xxxxxx
					// 11110000(240) - 11110111(247)
					cx = readUTF8(b0 - 240, 3);
					if (cx == -1) {
						iCurrent = iMark;
						return i;
					}

				} else
					throw ioException("Invalid hex value for first escape pattern in UTF-8 sequence: {0}", b0);

				if (cx < 0x10000)
					cbuf[off + i++] = (char)cx;
				else {
					cx -= 0x10000;
					cbuf[off + i++] = (char)(0xd800 + (cx >> 10));
					cbuf[off + i++] = (char)(0xdc00 + (cx & 0x3ff));
				}
			}
		}
		return i;
	}

	@Override /* Overridden from ParserReader */
	public UonReader unread() throws IOException {
		super.unread();
		return this;
	}

	private int readEncodedByte() throws IOException {
		if (iEnd <= iCurrent + 1)
			throw ioException("Incomplete trailing escape pattern");
		int h = buff[iCurrent++];
		int l = buff[iCurrent++];
		h = fromHexChar(h);
		l = fromHexChar(l);
		return (h << 4) + l;
	}

	private int readHex() throws IOException {
		var c = buff[iCurrent++];
		if (c != '%')
			throw ioException("Did not find expected '%' character in UTF-8 sequence.");
		return readEncodedByte();
	}

	private int readUTF8(int n, final int numBytes) throws IOException {
		if (iCurrent + numBytes * 3 > iEnd)
			return -1;
		for (int i = 0; i < numBytes; i++) {
			n <<= 6;
			n += readHex() - 128;
		}
		return n;
	}
}