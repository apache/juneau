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
package org.apache.juneau.ng.rest.client;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.ng.rest.client.assertion.*;

/**
 * An HTTP response returned by {@link NgRestRequest#run()}.
 *
 * <p>
 * Wraps the transport-layer {@link TransportResponse} and provides higher-level accessors.
 * Callers <b>must</b> close this response (it implements {@link Closeable}) to release transport resources.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // response is owned and closed via NgRestResponse.close(); getBodyStream() transfers ownership to caller
})
public final class NgRestResponse implements Closeable {

	private final TransportResponse response;

	NgRestResponse(TransportResponse response) {
		this.response = response;
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The status code (e.g. {@code 200}).
	 */
	public int getStatusCode() {
		return response.getStatusCode();
	}

	/**
	 * Returns the reason phrase (e.g. {@code "OK"}), or {@code null} if absent.
	 *
	 * @return The reason phrase, possibly <jk>null</jk>.
	 */
	public String getReasonPhrase() {
		return response.getReasonPhrase();
	}

	/**
	 * Returns all response headers.
	 *
	 * @return An unmodifiable list. Never <jk>null</jk>.
	 */
	public List<TransportHeader> getHeaders() {
		return response.getHeaders();
	}

	/**
	 * Returns the first response header with the given name (case-insensitive), or {@code null} if absent.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return The first matching header, or <jk>null</jk>.
	 */
	public TransportHeader getFirstHeader(String name) {
		return response.getFirstHeader(name);
	}

	/**
	 * Returns the response body as a UTF-8 string, or {@code null} if there is no body.
	 *
	 * @return The body as a string, possibly <jk>null</jk>.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	@SuppressWarnings({
		"resource" // body stream is owned by TransportResponse; caller must close NgRestResponse
	})
	public String getBodyAsString() throws IOException {
		var body = response.getBody();
		if (body == null)
			return null;
		return new String(body.readAllBytes(), StandardCharsets.UTF_8);
	}

	/**
	 * Returns the raw response body stream, or {@code null} if there is no body.
	 *
	 * <p>
	 * Callers must not close this stream directly — close the {@link NgRestResponse} instead.
	 *
	 * @return The body stream, possibly <jk>null</jk>.
	 */
	public InputStream getBodyStream() {
		return response.getBody();
	}

	/**
	 * Asserts that the status code is in the 2xx range.
	 *
	 * @return This object (for chaining).
	 * @throws NgRestCallException If the status code is not 2xx.
	 */
	public NgRestResponse assertOk() throws NgRestCallException {
		var sc = response.getStatusCode();
		if (sc < 200 || sc > 299)
			throw new NgRestCallException(sc, "Expected 2xx status but got " + sc + " " + response.getReasonPhrase());
		return this;
	}

	/**
	 * Asserts that the status code equals the expected value.
	 *
	 * @param expected The expected status code.
	 * @return This object (for chaining).
	 * @throws NgRestCallException If the status code does not match.
	 */
	public NgRestResponse assertStatus(int expected) throws NgRestCallException {
		var actual = response.getStatusCode();
		if (actual != expected)
			throw new NgRestCallException(actual, "Expected status " + expected + " but got " + actual);
		return this;
	}

	/**
	 * Returns a fluent body accessor for this response.
	 *
	 * @return A new body accessor. Never <jk>null</jk>.
	 */
	public ResponseBody body() {
		return new ResponseBody(this);
	}

	/**
	 * Returns a fluent assertion object for this response.
	 *
	 * <p>
	 * Use this for test-style validation of the response in production or test code:
	 * <p class='bjava'>
	 * 	<jv>resp</jv>.assertThat()
	 * 		.statusCode(200)
	 * 		.body().contains(<js>"alice"</js>);
	 * </p>
	 *
	 * @return A new assertion object. Never <jk>null</jk>.
	 */
	public ResponseAssertion assertThat() {
		return new ResponseAssertion(this);
	}

	/**
	 * Returns a fluent header accessor for the named response header.
	 *
	 * @param name The header name (case-insensitive). Must not be <jk>null</jk>.
	 * @return A new header accessor. Never <jk>null</jk>.
	 */
	public ResponseHeader header(String name) {
		return new ResponseHeader(name, this);
	}

	@Override /* Closeable */
	public void close() throws IOException {
		response.close();
	}
}
