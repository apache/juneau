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

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.rest.client.assertion.*;

/**
 * An HTTP response returned by {@link RestRequest#run()}.
 *
 * <p>
 * Wraps the transport-layer {@link TransportResponse} and provides higher-level accessors.
 * Callers <b>must</b> close this response (it implements {@link Closeable}) to release transport resources.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack.
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // response is owned and closed via RestResponse.close(); getBodyStream() transfers ownership to caller
})
public final class RestResponse implements Closeable {

	private final TransportResponse response;
	private final RestClient client;
	private final RestRequest request;
	private final Level debugLevel;
	private final InputStream body;
	private byte[] cachedContent;
	private long cachedContentLength = -1;
	private boolean debugEmitted;
	private Duration execTime;

	RestResponse(TransportResponse response, RestClient client) {
		this(response, client, null, null, 0);
	}

	RestResponse(TransportResponse response, RestClient client, RestRequest request, Level debugLevel, int debugBodyCap) {
		this.response = response;
		this.client = client;
		this.request = request;
		this.debugLevel = debugLevel;
		var originalBody = response.getBody();
		if (debugLevel == Level.FINEST && originalBody != null)
			this.body = new BoundedCaptureInputStream(originalBody, debugBodyCap);
		else
			this.body = originalBody;
		if (originalBody == null) {
			cachedContentLength = 0;
		} else {
			var h = response.getFirstHeader("Content-Length");
			if (h != null) {
				try {
					cachedContentLength = Long.parseLong(h.value());
				} catch (NumberFormatException e) {
					cachedContentLength = -1;
				}
			}
		}
	}

	/**
	 * Returns the client that produced this response.
	 *
	 * <p>
	 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack.
	 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
	 * (and possibly earlier).
	 *
	 * @return The client that produced this response. Never <jk>null</jk>.
	 */
	public RestClient getClient() {
		return client;
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
	 * @return The first matching header, or <jk>null</jk> if absent.
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
	public String getBodyAsString() throws IOException {
		var body = this.body;
		if (body == null)
			return null;
		return new String(body.readAllBytes(), StandardCharsets.UTF_8);
	}

	/**
	 * Returns the raw response body stream, or {@code null} if there is no body.
	 *
	 * <p>
	 * Callers must not close this stream directly — close the {@link RestResponse} instead.
	 *
	 * @return The body stream, possibly <jk>null</jk>.
	 */
	public InputStream getBodyStream() {
		return body;
	}

	/**
	 * Asserts that the status code is in the 2xx range.
	 *
	 * @return This object (for chaining).
	 * @throws RestCallException If the status code is not 2xx.
	 */
	public RestResponse assertOk() throws RestCallException {
		var sc = response.getStatusCode();
		if (sc < 200 || sc > 299)
			throw new RestCallException(sc, "Expected 2xx status but got " + sc + " " + response.getReasonPhrase());
		return this;
	}

	/**
	 * Asserts that the status code equals the expected value.
	 *
	 * @param expected The expected status code.
	 * @return This object (for chaining).
	 * @throws RestCallException If the status code does not match.
	 */
	public RestResponse assertStatus(int expected) throws RestCallException {
		var actual = response.getStatusCode();
		if (actual != expected)
			throw new RestCallException(actual, "Expected status " + expected + " but got " + actual);
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
		if (body instanceof BoundedCaptureInputStream bcis) {
			bcis.drain();
			cachedContent = bcis.getCapturedBytes();
			cachedContentLength = bcis.getTotalBytesRead();
		}
		if (!debugEmitted && client != null && debugLevel != null) {
			var thrown = request != null ? request.getException() : null;
			RestClientDebugPipeline.emit(client.debugLogger, client.debugFormatter, debugLevel, request, this, thrown);
			debugEmitted = true;
		}
		response.close();
	}

	void setExecTime(Duration value) {
		execTime = value;
	}

	/**
	 * Returns response body bytes captured for debug logging (up to the configured body cap).
	 *
	 * @return Captured response bytes, or <jk>null</jk> if not captured.
	 */
	public byte[] getCachedContent() {
		return cachedContent;
	}

	/**
	 * Returns the total response body length observed while reading/draining, or {@code -1} if unknown.
	 *
	 * @return The total body length.
	 */
	public long getCachedContentLength() {
		return cachedContentLength;
	}

	Duration getExecTime() {
		return execTime;
	}

	private static final class BoundedCaptureInputStream extends FilterInputStream {
		private final ByteArrayOutputStream capture = new ByteArrayOutputStream();
		private final int cap;
		private long totalBytesRead;

		BoundedCaptureInputStream(InputStream in, int cap) {
			super(in);
			this.cap = cap;
		}

		byte[] getCapturedBytes() {
			return capture.toByteArray();
		}

		long getTotalBytesRead() {
			return totalBytesRead;
		}

		void drain() throws IOException {
			while (read() != -1) {
				// consume
			}
		}

		@Override
		public int read() throws IOException {
			var b = super.read();
			if (b == -1)
				return -1;
			totalBytesRead++;
			if (capture.size() < cap)
				capture.write(b);
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			var count = super.read(b, off, len);
			if (count <= 0)
				return count;
			totalBytesRead += count;
			var remaining = cap - capture.size();
			if (remaining > 0)
				capture.write(b, off, Math.min(count, remaining));
			return count;
		}
	}
}
