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
package org.apache.juneau.internal;

import java.io.*;

/**
 * Similar to {@link StringReader} except reads from a generic {@link CharSequenceReader}.
 *
 * <h5 class='section'>See Also:</h5><ul>

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
	 * @param cs The char sequence to read from.  Can be <jk>null</jk>.
	 */
	public CharSequenceReader(CharSequence cs) {
		super(new StringReader(""), 1);   // Does not actually use a reader.
		if (cs == null)
			cs = "";
		this.cs = cs;
		if (cs instanceof String)
			s = (String)cs;
		else if (cs instanceof StringBuffer)
			sb = (StringBuffer)cs;
		else if (cs instanceof StringBuilder)
			sb2 = (StringBuilder)cs;
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
	public int read(final char[] cbuf, final int off, final int len) {
		if (next >= length)
			return -1;
		int n = Math.min(length - next, len);
		if (s != null)
			s.getChars(next, next + n, cbuf, off);
		else if (sb != null)
			sb.getChars(next, next + n, cbuf, off);
		else if (sb2 != null)
			sb2.getChars(next, next + n, cbuf, off);
		else {
			for (int i = 0; i < n; i++)
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