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
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * A fluent accessor for an HTTP response body returned by {@link RestResponse}.
 *
 * <p>
 * Provides convenient methods for reading the body as a string, byte array, or stream.
 * The body is read lazily; each method reads and returns the body content once.
 *
 * <p>
 * Obtain instances via {@link RestResponse#body()}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // Eclipse resource analysis: response is borrowed; caller closes it after reading body
})
public final class ResponseBody {

	private final RestResponse response;

	ResponseBody(RestResponse response) {
		this.response = response;
	}

	/**
	 * Returns the response body as a UTF-8 string.
	 *
	 * @return The body string, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	public String asString() throws IOException {
		return response.getBodyAsString();
	}

	/**
	 * Returns the response body decoded with the given charset.
	 *
	 * @param charset The character set to use for decoding. Must not be <jk>null</jk>.
	 * @return The body string, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	public String asString(Charset charset) throws IOException {
		var stream = response.getBodyStream();
		if (stream == null)
			return null;
		return new String(stream.readAllBytes(), charset);
	}

	/**
	 * Returns the response body as a byte array.
	 *
	 * @return The body bytes, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	@SuppressWarnings({
		"java:S1168" // 'null' is a documented sentinel distinguishing 'no response body' (e.g. HTTP 204) from an empty body; callers rely on it (see RestClientFeatures_Test.l04_responseBody_nullBody).
	})
	public byte[] asBytes() throws IOException {
		var stream = response.getBodyStream();
		if (stream == null)
			return null;
		return stream.readAllBytes();
	}

	/**
	 * Returns the raw response body stream.
	 *
	 * <p>
	 * Callers should not close this stream directly; close the parent {@link RestResponse} instead.
	 *
	 * @return The body stream, or <jk>null</jk> if the response has no body.
	 */
	public InputStream asStream() {
		return response.getBodyStream();
	}

	/**
	 * Reads the response body into a byte array and returns it, or returns {@code null} if there is no body.
	 *
	 * <p>
	 * Equivalent to {@link #asBytes()}.
	 *
	 * @return The body bytes, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	public byte[] readAllBytes() throws IOException {
		return asBytes();
	}

	/**
	 * Opens a token/record-streaming cursor over the response body using the parser negotiated from the response
	 * {@code Content-Type}.
	 *
	 * <p>
	 * Equivalent to {@link #asCursor(Parser, Class) asCursor(negotiatedParser, type)} where the parser is selected
	 * from the response {@code Content-Type} header; when no registered parser matches and no default parser is
	 * configured, a <c>415 Unsupported Media Type</c> is thrown.  Use {@link #asCursor(Parser, Class)} to read with an
	 * explicit parser.
	 *
	 * @param <T> The cursor type ({@link RecordReader}, {@link TokenReader}, or a concrete subtype).
	 * @param type The declared cursor type. Must not be <jk>null</jk>.
	 * @return A cursor over the live response body. Never <jk>null</jk>.
	 * @throws IOException If the body is missing, the parser does not support the requested cursor surface, or the
	 * 	produced cursor is not assignable to {@code type}.
	 */
	public <T> T asCursor(Class<T> type) throws IOException {
		return asCursor(negotiatedParser(), type);
	}

	/**
	 * Parses the response body to {@code type} using the parser negotiated from the response {@code Content-Type}.
	 *
	 * <p>
	 * When the header is absent or matches no registered parser and no default parser is configured, a
	 * <c>415 Unsupported Media Type</c> is thrown.  Use {@link #as(Parser, Class)} to force a specific parser,
	 * bypassing content negotiation.
	 *
	 * @param <T> The type to parse to.
	 * @param type The type to parse to. Must not be <jk>null</jk>.
	 * @return The parsed body, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body or the body could not be parsed.
	 */
	public <T> T as(Class<T> type) throws IOException {
		return as(negotiatedParser(), type);
	}

	/**
	 * Parses the response body to {@code type} using the given parser.
	 *
	 * <p>
	 * This forces the supplied parser, bypassing the {@code Content-Type} negotiation performed by {@link #as(Class)}.
	 *
	 * <p>
	 * A parse failure is surfaced strictly as an {@link IOException} wrapping the underlying {@link ParseException};
	 * the malformed body is never returned to the caller.
	 *
	 * @param <T> The type to parse to.
	 * @param parser The parser to use. Must not be <jk>null</jk>.
	 * @param type The type to parse to. Must not be <jk>null</jk>.
	 * @return The parsed body, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body or the body could not be parsed.
	 */
	public <T> T as(Parser parser, Class<T> type) throws IOException {
		assertArgNotNull("parser", parser);
		assertArgNotNull("type", type);
		var body = response.getBodyAsString();
		if (body == null)
			return null;
		try {
			return parser.read(body, type);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	private Parser negotiatedParser() {
		var h = response.getFirstHeader("Content-Type");
		var ct = h == null ? null : h.value();
		return response.getClient().getMatchingParser(ct).orElseThrow(() -> new UnsupportedMediaType(
			"No parser matched the response Content-Type '%s' and no default parser is configured on the client.", ct));
	}

	/**
	 * Opens a token/record-streaming cursor over the response body using the given parser.
	 *
	 * <p>
	 * This forces the supplied parser, bypassing the {@code Content-Type} negotiation performed by
	 * {@link #asCursor(Class)}.
	 *
	 * <p>
	 * The cursor reads directly from the live response stream &mdash; the body is not buffered into memory.  The
	 * caller owns the returned cursor and the parent {@link RestResponse}; close them when done.
	 *
	 * <p>
	 * When {@code type} is (or extends) {@link TokenReader} the parser must implement {@link TokenReadable};
	 * otherwise it must implement {@link RecordReadable}.
	 *
	 * @param <T> The cursor type ({@link RecordReader}, {@link TokenReader}, or a concrete subtype).
	 * @param parser The parser that opens the cursor. Must not be <jk>null</jk>.
	 * @param type The declared cursor type. Must not be <jk>null</jk>.
	 * @return A cursor over the live response body. Never <jk>null</jk>.
	 * @throws IOException If the body is missing, the parser does not support the requested cursor surface, or the
	 * 	produced cursor is not assignable to {@code type}.
	 */
	@SuppressWarnings({
		"unchecked", // The produced cursor is verified assignable to 'type' before the cast.
		"resource"   // The cursor reads from the borrowed response stream; the caller closes the cursor / RestResponse.
	})
	public <T> T asCursor(Parser parser, Class<T> type) throws IOException {
		assertArgNotNull("parser", parser);
		assertArgNotNull("type", type);

		var isToken = TokenReader.class.isAssignableFrom(type);
		var supported = isToken ? parser instanceof TokenReadable : parser instanceof RecordReadable;
		if (! supported)
			throw ioex("Parser '%s' does not support the %s surface.", parser.getClass().getName(), isToken ? "token-reader" : "record-reader");

		var stream = response.getBodyStream();
		if (stream == null)
			throw new IOException("Response has no body to open a cursor over.");

		Object input = parser.isReaderParser() ? new InputStreamReader(stream, StandardCharsets.UTF_8) : stream;
		var cursor = isToken
			? ((TokenReadable) parser).readTokens(input)
			: ((RecordReadable) parser).readRecords(input);

		if (! type.isInstance(cursor))
			throw ioex("Parser '%s' produced cursor type '%s' which is not assignable to the declared type '%s'.", parser.getClass().getName(), cursor == null ? "null" : cursor.getClass().getName(), type.getName());

		return (T) cursor;
	}
}
