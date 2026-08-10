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
package org.apache.juneau.parser;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;

/**
 * Input stream meant to be used as input for stream-based parsers.
 *
 * <p>
 * Keeps track of current byte position.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class ParserInputStream extends InputStream implements Positionable {

	private final InputStream is;
	int pos = 0;
	// Default cap (16 MiB) for wire-declared lengths; overridable via setMaxLength.
	private int maxLength = 16 * 1024 * 1024;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected ParserInputStream(ParserPipe pipe) throws IOException {
		this.is = pipe.getInputStream();
		pipe.setPositionable(this);
	}

	/**
	 * Sets the maximum length allowed for a single wire-declared length/count.
	 *
	 * @param value The maximum length in bytes.  Values &le; 0 disable the cap (only the negative-length check remains).
	 */
	public void setMaxLength(int value) {
		maxLength = value <= 0 ? Integer.MAX_VALUE : value;
	}

	/**
	 * Bounds a wire-declared length/count against the configured maximum before it is used to size an allocation.
	 *
	 * @param len The declared length/count read off the wire.
	 * @param what A short description of the field being read (for the error message).
	 * @return The length as an <c>int</c>, guaranteed to be non-negative and within the configured maximum.
	 * @throws IOException If the length is negative (or beyond int range) or exceeds the configured maximum.
	 */
	public int checkLength(long len, String what) throws IOException {
		return checkLength(len, maxLength, what);
	}

	/**
	 * Bounds a wire-declared length/count against the specified maximum before it is used to size an allocation.
	 *
	 * @param len The declared length/count read off the wire.
	 * @param maxLength The maximum allowed length.  Values &le; 0 are treated as {@link Integer#MAX_VALUE}
	 * 	(only the negative-length check applies).
	 * @param what A short description of the field being read (for the error message).
	 * @return The length as an <c>int</c>, guaranteed to be non-negative and within the specified maximum.
	 * @throws IOException If the length is negative (or beyond int range) or exceeds the configured maximum.
	 */
	public static int checkLength(long len, long maxLength, String what) throws IOException {
		var max = maxLength <= 0 ? Integer.MAX_VALUE : maxLength;
		if (len < 0)
			throw ioex("Invalid {0} length (negative): {1}", what, len);
		if (len > max)
			throw ioex("{0} length {1} exceeds maximum allowed {2}", what, len, max);
		return (int)len;
	}

	@Override /* Overridden from Positionable */
	public Position getPosition() { return new Position(pos); }

	@Override /* Overridden from InputStream */
	public int read() throws IOException {
		int i = is.read();
		if (i > 0)
			pos++;
		return i;
	}
}