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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.commons.utils.Shorts.*;

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
@SuppressWarnings({
	"java:S4929", // read(byte[],int,int) is intentionally omitted; ParserInputStream reads one byte at a time to maintain accurate position tracking for parse error reporting
	"resource" // is field is owned by the ParserPipe; its lifetime is managed by the caller's session.
})
public class ParserInputStream extends InputStream implements Positionable {

	private final InputStream is;
	int pos = 0;
	// Default cap (16 MiB) for wire-declared lengths; overridable via setMaxLength.
	private int maxLength = 16 * 1024 * 1024;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected ParserInputStream(ParserPipe pipe) throws IOException {
		this.is = pipe.getInputStream();
		pipe.setPositionable(this);
	}

	/**
	 * Sets the maximum allowed wire-declared length (in bytes) for payloads and element counts read off the
	 * stream before they are used to size an allocation or drive a container loop.
	 *
	 * <p>
	 * Guards against malformed input where a small payload declares a huge or negative length that would
	 * otherwise trigger {@link OutOfMemoryError} or {@link NegativeArraySizeException}.
	 *
	 * @param value The maximum length in bytes.  Values &le; 0 disable the cap (only the negative-length check remains).
	 */
	public void setMaxLength(int value) {
		maxLength = value <= 0 ? Integer.MAX_VALUE : value;
	}

	/**
	 * Validates a wire-declared length/count against sane bounds before it is used to size an allocation or
	 * drive a container loop, using this stream's configured maximum.
	 *
	 * @param len The declared length/count read off the wire.
	 * @param what A short description of the field being read (for the error message).
	 * @return The validated length as an int.
	 * @throws IOException If the length is negative (or beyond int range) or exceeds the configured maximum.
	 */
	public int checkLength(long len, String what) throws IOException {
		return checkLength(len, maxLength, what);
	}

	/**
	 * Validates a wire-declared length/count against sane bounds before it is used to size an allocation or
	 * drive a container loop.
	 *
	 * <p>
	 * Shared bounds-check used by the stream-based parsers and by other low-level readers (such as the
	 * Protobuf reader) that do not extend this class but need the same guard.
	 *
	 * @param len The declared length/count read off the wire.
	 * @param maxLength The maximum allowed length.  Values &le; 0 are treated as {@link Integer#MAX_VALUE}
	 * 	(only the negative-length check applies).
	 * @param what A short description of the field being read (for the error message).
	 * @return The validated length as an int.
	 * @throws IOException If the length is negative (or beyond int range) or exceeds the configured maximum.
	 */
	public static int checkLength(long len, long maxLength, String what) throws IOException {
		var max = maxLength <= 0 ? Integer.MAX_VALUE : maxLength;
		if (len < 0)
			throw ioex("Invalid %s length (negative): %s", what, len);
		if (len > max)
			throw ioex("%s length %s exceeds maximum allowed %s", what, len, max);
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