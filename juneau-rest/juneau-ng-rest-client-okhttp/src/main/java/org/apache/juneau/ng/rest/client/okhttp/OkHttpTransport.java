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
package org.apache.juneau.ng.rest.client.okhttp;

import java.io.*;

import okhttp3.*;
import okio.*;

import org.apache.juneau.ng.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by OkHttp 5.x.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code com.squareup.okhttp3:okhttp-jvm} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = OkHttpTransport.<jsm>builder</jsm>()
 * 		.httpClient(<jk>new</jk> OkHttpClient())
 * 		.build();
 *
 * 	<jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
 *
 * <p>
 * OkHttp's {@link RequestBody#writeTo(BufferedSink)} streams the request body directly to the socket
 * without in-memory buffering for bodies with a known content length.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class OkHttpTransport implements HttpTransport {

	private final OkHttpClient httpClient;

	OkHttpTransport(OkHttpTransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : new OkHttpClient();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static OkHttpTransportBuilder builder() {
		return new OkHttpTransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link OkHttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static OkHttpTransport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		var okRequest = buildOkRequest(request);
		Response okResponse;
		try {
			okResponse = httpClient.newCall(okRequest).execute();
		} catch (IOException e) {
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
		return buildTransportResponse(okResponse);
	}

	@Override /* Closeable */
	public void close() {
		httpClient.dispatcher().executorService().shutdown();
		httpClient.connectionPool().evictAll();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static Request buildOkRequest(TransportRequest request) throws TransportException {
		var builder = new Request.Builder().url(request.getUri().toString());
		for (var h : request.getHeaders())
			builder.addHeader(h.name(), h.value());
		builder.method(request.getMethod(), buildOkBody(request.getBody(), request.getMethod()));
		return builder.build();
	}

	private static RequestBody buildOkBody(TransportBody body, String method) {
		if (body == null) {
			// OkHttp requires a non-null body for POST/PUT/PATCH even if empty
			return requiresBody(method) ? RequestBody.create(new byte[0], null) : null;
		}
		var ct = body.getContentType();
		var mediaType = ct != null ? MediaType.parse(ct) : null;
		return new RequestBody() {
			@Override
			public MediaType contentType() {
				return mediaType;
			}

			@Override
			public long contentLength() {
				return body.getContentLength();
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				body.writeTo(sink.outputStream());
			}
		};
	}

	private static boolean requiresBody(String method) {
		return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
	}

	@SuppressWarnings({
		"resource" // okResponse is closed via TransportResponse.closeCallback
	})
	private static TransportResponse buildTransportResponse(Response okResponse) throws TransportException {
		var builder = TransportResponse.builder()
			.statusCode(okResponse.code())
			.reasonPhrase(okResponse.message())
			.closeCallback(okResponse);
		for (var i = 0; i < okResponse.headers().size(); i++)
			builder.header(okResponse.headers().name(i), okResponse.headers().value(i));
		var responseBody = okResponse.body();
		if (responseBody != null) {
			try {
				builder.body(responseBody.byteStream());
			} catch (Exception e) {
				throw new TransportException("Failed to read response body: " + e.getMessage(), e);
			}
		}
		return builder.build();
	}
}
