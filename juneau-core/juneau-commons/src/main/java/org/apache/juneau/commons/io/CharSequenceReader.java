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
package org.apache.juneau.commons.io;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;

/**
 * A {@link Reader} implementation that reads from any {@link CharSequence} (String, StringBuilder, StringBuffer, etc.).
 *
 * <p>
 * This class extends {@link BufferedReader} and provides efficient reading from any {@link CharSequence}
 * implementation. Unlike {@link StringReader}, which only works with {@link String}, this class can
 * read from {@link StringBuilder}, {@link StringBuffer}, or any other {@link CharSequence} implementation.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Generic CharSequence support - works with String, StringBuilder, StringBuffer, and other CharSequence types
 * 	<li>Efficient reading - optimized for different CharSequence types
 * 	<li>No-op close - close() method does nothing (CharSequence is in-memory)
 * 	<li>No mark support - mark/reset operations are not supported
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Reading from StringBuilder or StringBuffer instances
 * 	<li>Converting CharSequence data to Reader for APIs that require Reader
 * 	<li>Processing character data from various CharSequence sources
 * 	<li>Testing scenarios where you need a Reader from in-memory data
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Read from String</jc>
 * 	CharSequenceReader <jv>reader1</jv> = <jk>new</jk> CharSequenceReader(<js>"Hello World"</js>);
 *
 * 	<jc>// Read from StringBuilder</jc>
 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder(<js>"Dynamic content"</js>);
 * 	CharSequenceReader <jv>reader2</jv> = <jk>new</jk> CharSequenceReader(<jv>sb</jv>);
 *
 * 	<jc>// Read from StringBuffer</jc>
 * 	StringBuffer <jv>sbuf</jv> = <jk>new</jk> StringBuffer(<js>"Buffer content"</js>);
 * 	CharSequenceReader <jv>reader3</jv> = <jk>new</jk> CharSequenceReader(<jv>sbuf</jv>);
 *
 * 	<jc>// Use with APIs that require Reader</jc>
 * 	<jk>try</jk> (CharSequenceReader <jv>reader</jv> = <jk>new</jk> CharSequenceReader(<js>"data"</js>)) {
	 * 		<jc>// Process reader</jc>
	 * 	}
 * </p>
 *
 * <h5 class='section'>Performance:</h5>
 * <p>
 * This class optimizes reading based on the CharSequence type:
 * <ul class='spaced-list'>
 * 	<li>String - uses efficient {@link String#getChars(int, int, char[], int)} method
 * 	<li>StringBuffer - uses efficient {@link StringBuffer#getChars(int, int, char[], int)} method
 * 	<li>StringBuilder - uses efficient {@link StringBuilder#getChars(int, int, char[], int)} method
 * 	<li>Other CharSequence - falls back to character-by-character reading via {@link CharSequence#charAt(int)}
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe. If the underlying CharSequence is modified by another thread
 * while reading, the behavior is undefined. For thread-safe reading, ensure the CharSequence
 * is not modified during reading, or use external synchronization.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class CharSequenceReader extends BufferedReader {

	private final CharSequence cs;
	private String s;
	private StringBuffer sb;
	private StringBuilder sb2;
	private int length;
	private int next;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new CharSequenceReader that reads from the specified CharSequence. If the
	 * CharSequence is <jk>null</jk>, it is treated as an empty string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// From String</jc>
	 * 	CharSequenceReader <jv>reader1</jv> = <jk>new</jk> CharSequenceReader(<js>"Hello"</js>);
	 *
	 * 	<jc>// From StringBuilder</jc>
	 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder(<js>"World"</js>);
	 * 	CharSequenceReader <jv>reader2</jv> = <jk>new</jk> CharSequenceReader(<jv>sb</jv>);
	 *
	 * 	<jc>// Null is treated as empty string</jc>
	 * 	CharSequenceReader <jv>reader3</jv> = <jk>new</jk> CharSequenceReader(<jk>null</jk>);
	 * 	<jk>int</jk> <jv>ch</jv> = <jv>reader3</jv>.read();  <jc>// Returns -1 (EOF)</jc>
	 * </p>
	 *
	 * @param cs The CharSequence to read from. Can be <jk>null</jk> (treated as empty string).
	 */
	public CharSequenceReader(CharSequence cs) {
		super(new StringReader(""), 1);   // Does not actually use a reader.
		if (cs == null)
			cs = "";
		this.cs = cs;
		if (cs instanceof String s2)
			s = s2;
		else if (cs instanceof StringBuffer sb3)
			sb = sb3;
		else if (cs instanceof StringBuilder sb4)
			sb2 = sb4;
		this.length = cs.length();
	}

	@Override /* Overridden from Reader */
	public void close() {
		// no-op
	}

	@Override /* Overridden from Reader */
	public boolean markSupported() {
		return false;
	}

	@Override /* Overridden from Reader */
	public int read() {
		if (next >= length)
			return -1;
		return cs.charAt(next++);
	}

	@Override /* Overridden from Reader */
	public int read(char[] cbuf, int off, int len) {
		assertArgNotNull("cbuf", cbuf);
		if (next >= length)
			return -1;
		int n = Math.min(length - next, len);
		if (nn(s))
			s.getChars(next, next + n, cbuf, off);
		else if (nn(sb))
			sb.getChars(next, next + n, cbuf, off);
		else if (nn(sb2))
			sb2.getChars(next, next + n, cbuf, off);
		else {
			for (var i = 0; i < n; i++)
				cbuf[off + i] = cs.charAt(next + i);
		}
		next += n;
		return n;
	}

	@Override /* Overridden from Reader */
	public long skip(long ns) {
		if (next >= length)
			return 0;
		long n = Math.min(length - next, ns);
		n = Math.max(-next, n);
		next += n;
		return n;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return cs.toString();
	}
}