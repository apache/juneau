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
package org.apache.juneau.parser;

import java.io.*;

import org.apache.juneau.internal.*;

/**
 * Similar to a {@link java.io.PushbackReader} with a pushback buffer of 1 character.
 *
 * <p>
 * Code is optimized to work with a 1 character buffer.
 *
 * <p>
 * Additionally keeps track of current line and column number, and provides the ability to set mark points and capture
 * characters from the previous mark point.
 *
 * <p>
 * <b>Warning:</b>  Not thread safe.
 */
public class ParserReader extends Reader implements Positionable {

	/** Wrapped reader */
	protected final Reader r;

	private char[] buff;       // Internal character buffer
	private int line = 1;      // Current line number
	private int column;        // Current column number
	private int iCurrent = 0;  // Current pointer into character buffer
	private int iMark = -1;    // Mark position in buffer
	private int iEnd = 0;      // The last good character position in the buffer
	private boolean endReached, holesExist;
	private final boolean unbuffered;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @throws IOException Thrown by underlying stream.
	 */
	public ParserReader(ParserPipe pipe) throws IOException {
		this.unbuffered = pipe.unbuffered;
		if (pipe.isString()) {
			String in = pipe.getInputAsString();
			this.r = new CharSequenceReader(in);
			this.buff = new char[in.length() < 1024 ? in.length() : 1024];
		} else {
			Reader _r = pipe.getReader();
			if (_r instanceof ParserReader)
				this.r = ((ParserReader)_r).r;
			else
				this.r = _r;
			this.buff = new char[1024];
		}
		pipe.setPositionable(this);
	}

	/**
	 * Reads a single character.
	 *
	 * <p>
	 * Note that this method does NOT process extended unicode characters (i.e. characters above 0x10000), but rather
	 * returns them as two <jk>char</jk>s.
	 * Use {@link #readCodePoint()} to ensure proper handling of extended unicode.
	 *
	 * @return The character read, or -1 if the end of the stream has been reached.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	@Override /* Reader */
	public final int read() throws IOException {
		int c = readFromBuff();
		if (c == -1)
			return -1;
		if (c == '\n') {
			line++;
			column = 0;
		} else {
			column++;
		}
		return c;
	}

	/**
	 * Same as {@link #read()} but skips over any whitespace characters.
	 *
	 * @return The first non-whitespace character, or -1 if the end of stream reached.
	 * @throws IOException Thrown by underlying stream.
	 */
	public final int readSkipWs() throws IOException {
		while (true) {
			int c = read();
			if (c == -1 || ! Character.isWhitespace(c))
				return c;
		}
	}

	/**
	 * Same as {@link #read()} but detects and combines extended unicode characters (characters above 0x10000).
	 *
	 * @return The character read, or -1 if the end of the stream has been reached.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final int readCodePoint() throws IOException {
		int c = read();

		// Characters that take up 2 chars.
		if (c >= 0xd800 && c <= 0xdbff) {
			int low = read();
			if (low >= 0xdc00 && low <= 0xdfff)
				c = 0x10000 + ((c - 0xd800) << 10) + (low - 0xdc00);
		}

		return c;
	}

	private final int readFromBuff() throws IOException {
		while (iCurrent >= iEnd) {
			if (endReached)
				return -1;

			// If there's still space at the end of this buffer, fill it.
			// Make sure there's at least 2 character spaces free for extended unicode characters.
			//if (false) {
			if (iEnd+1 < buff.length) {
				int x = read(buff, iCurrent, buff.length-iEnd);
				if (x == -1) {
					endReached = true;
					return -1;
				}
				iEnd += x;

			} else {
				// If we're currently marking, then we want to copy from the current mark point
				// to the beginning of the buffer and then fill in the remainder of buffer.
				if (iMark >= 0) {

					// If we're marking from the beginning of the array, we double the size of the
					// buffer.  This isn't likely to occur often.
					if (iMark == 0) {
						char[] buff2 = new char[buff.length<<1];
						System.arraycopy(buff, 0, buff2, 0, buff.length);
						buff = buff2;

					// Otherwise, we copy what's currently marked to the beginning of the buffer.
					} else {
						int copyBuff = iMark;
						System.arraycopy(buff, copyBuff, buff, 0, buff.length - copyBuff);
						iCurrent -= copyBuff;
						iMark -= copyBuff;
					}
					int expected = buff.length - iCurrent;

					int x = read(buff, iCurrent, expected);
					if (x == -1) {
						endReached = true;
						iEnd = iCurrent;
						return -1;
					}
					iEnd = iCurrent + x;
				} else {
					// Copy the last 10 chars in the buffer to the beginning of the buffer.
					int copyBuff = Math.min(iCurrent, 10);
					System.arraycopy(buff, iCurrent-copyBuff, buff, 0, copyBuff);

					// Number of characters we expect to copy on the next read.
					int expected = buff.length - copyBuff;
					int x = read(buff, copyBuff, expected);
					iCurrent = copyBuff;
					if (x == -1) {
						endReached = true;
						iEnd = iCurrent;
						return -1;
					}
					iEnd = iCurrent + x;
				}
			}
		}
		return buff[iCurrent++];
	}

	/**
	 * Start buffering the calls to read() so that the text can be gathered from the mark point on calling {@code getFromMarked()}.
	 */
	public final void mark() {
		iMark = iCurrent;
	}

	/**
	 * Peeks the next character in the stream.
	 *
	 * <p>
	 * This is equivalent to doing a {@code read()} followed by an {@code unread()}.
	 *
	 * @return The peeked character, or (char)-1 if the end of the stream has been reached.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final int peek() throws IOException {
		int c = read();
		if (c != -1)
			unread();
		return c;
	}

	/**
	 * Same as {@link #peek()} but skips over any whitespace characters.
	 *
	 * <p>
	 * This is equivalent to doing a {@code read()} followed by an {@code unread()}.
	 *
	 * @return The peeked character, or (char)-1 if the end of the stream has been reached.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final int peekSkipWs() throws IOException {
		while(true) {
			int c = read();
			boolean isWs = Character.isWhitespace(c);
			if (c != -1 && ! isWs)
				unread();
			if (! isWs)
				return c;
		}
	}

	/**
	 * Read the specified number of characters off the stream.
	 *
	 * @param num The number of characters to read.
	 * @return The characters packaged as a String.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final String read(int num) throws IOException {
		char[] c = new char[num];
		for (int i = 0; i < num; i++) {
			int c2 = read();
			if (c2 == -1)
				return new String(c, 0, i);
			c[i] = (char)c2;
		}
		return new String(c);
	}

	/**
	 * Pushes the last read character back into the stream.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public ParserReader unread() throws IOException {
		if (iCurrent <= 0)
			throw new IOException("Buffer underflow.");
		iCurrent--;
		if (column == 0)
			line--;
		else
			column--;
		return this;
	}

	/**
	 * No-op.
	 *
	 * <p>
	 * Input readers are closed in the {@link ParserPipe} class.
	 *
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	@Override /* Reader */
	public void close() throws IOException {
		// No-op
	}

	/**
	 * Returns the contents of the reusable character buffer as a string, and resets the buffer for next usage.
	 *
	 * @return The contents of the reusable character buffer as a string.
	 */
	public final String getMarked() {
		return getMarked(0, 0);
	}

	/**
	 * Same as {@link #getMarked()} except allows you to specify offsets into the buffer.
	 *
	 * <p>
	 * For example, to return the marked string, but trim the first and last characters, call the following:
	 * <p class='bcode w800'>
	 * 	getFromMarked(1, -1);
	 * </p>
	 *
	 * @param offsetStart The offset of the start position.
	 * @param offsetEnd The offset of the end position.
	 * @return The contents of the reusable character buffer as a string.
	 */
	public final String getMarked(int offsetStart, int offsetEnd) {
		int offset = 0;

		// Holes are \u00FF 'delete' characters that we need to get rid of now.
		if (holesExist) {
			for (int i = iMark; i < iCurrent; i++) {
				char c = buff[i];
				if (c == 127)
					offset++;
				else
					buff[i-offset] = c;
			}
			holesExist = false;
		}
		int start = iMark + offsetStart, len = iCurrent - iMark + offsetEnd - offsetStart - offset;
		String s = new String(buff, start, len);
		iMark = -1;
		return s;
	}

	/**
	 * Trims off the last character in the marking buffer.
	 *
	 * <p>
	 * Useful for removing escape characters from sequences.
	 *
	 * @return This object (for method chaining).
	 */
	public final ParserReader delete() {
		return delete(1);
	}

	/**
	 * Trims off the specified number of last characters in the marking buffer.
	 * Useful for removing escape characters from sequences.
	 *
	 * @param count The number of characters to delete.
	 * @return This object (for method chaining).
	 */
	public final ParserReader delete(int count) {
		for (int i = 0; i < count; i++)
			buff[iCurrent-i-1] = 127;
		holesExist = true;
		return this;
	}

	/**
	 * Replaces the last character in the marking buffer with the specified character.
	 *
	 * <p>
	 * <c>offset</c> must be at least <c>1</c> for normal characters, and <c>2</c> for extended
	 * unicode characters in order for the replacement to fit into the buffer.
	 *
	 * @param c The new character.
	 * @param offset The offset.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 */
	public final ParserReader replace(int c, int offset) throws IOException {
		if (c < 0x10000) {
			if (offset < 1)
				throw new IOException("Buffer underflow.");
			buff[iCurrent-offset] = (char)c;
		} else {
			if (offset < 2)
				throw new IOException("Buffer underflow.");
			c -= 0x10000;
			buff[iCurrent-offset] = (char)(0xd800 + (c >> 10));
			buff[iCurrent-offset+1] = (char)(0xdc00 + (c & 0x3ff));
			offset--;
		}
		// Fill in the gap with DEL characters.
		for (int i = 1; i < offset; i++)
			buff[iCurrent-i] = 127;
		holesExist |= (offset > 1);
		return this;
	}

	/**
	 * Replace the last read character in the buffer with the specified character.
	 *
	 * @param c The new character.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 */
	public final ParserReader replace(char c) throws IOException {
		return replace(c, 1);
	}

	/**
	 * Subclasses can override this method to provide additional filtering.
	 *
	 * <p>
	 * Default implementation simply calls the same method on the underlying reader.
	 */
	@Override /* Reader */
	public int read(char[] cbuf, int off, int len) throws IOException {
		return unbuffered ? r.read(cbuf, off, 1) : r.read(cbuf, off, len);
	}

	@Override /* Positionable */
	public Position getPosition() {
		return new Position(line, column);
	}
}
