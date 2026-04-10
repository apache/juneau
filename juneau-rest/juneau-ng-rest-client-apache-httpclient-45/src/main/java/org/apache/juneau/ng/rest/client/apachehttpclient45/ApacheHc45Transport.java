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
package org.apache.juneau.ng.rest.client.apachehttpclient45;

import java.io.*;

import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.ng.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by Apache HttpClient 4.5.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code org.apache.httpcomponents:httpclient} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = ApacheHc45Transport.<jsm>builder</jsm>()
 * 		.httpClient(HttpClients.createDefault())
 * 		.build();
 *
 * 	<jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>transport</jv>)
 * 		.build();
 * </p>
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
@SuppressWarnings({
	"resource" // httpClient is owned by this transport and closed in close()
})
public final class ApacheHc45Transport implements HttpTransport {

	private final CloseableHttpClient httpClient;

	ApacheHc45Transport(ApacheHc45TransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClients.createDefault();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static ApacheHc45TransportBuilder builder() {
		return new ApacheHc45TransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link CloseableHttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ApacheHc45Transport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		var hcRequest = buildHcRequest(request);
		CloseableHttpResponse hcResponse;
		try {
			hcResponse = httpClient.execute(hcRequest);
		} catch (IOException e) {
			throw new TransportException("HTTP transport error: " + e.getMessage(), e);
		}
		return buildTransportResponse(hcResponse);
	}

	@Override /* Closeable */
	public void close() throws IOException {
		httpClient.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static HttpUriRequest buildHcRequest(TransportRequest request) throws TransportException {
		var builder = RequestBuilder.create(request.getMethod()).setUri(request.getUri());
		for (var h : request.getHeaders())
			builder.addHeader(h.name(), h.value());
		var body = request.getBody();
		if (body != null)
			builder.setEntity(new TransportBodyEntity(body));
		try {
			return builder.build();
		} catch (Exception e) {
			throw new TransportException("Failed to build HTTP request: " + e.getMessage(), e);
		}
	}

	private static TransportResponse buildTransportResponse(CloseableHttpResponse hcResponse) throws TransportException {
		var statusLine = hcResponse.getStatusLine();
		var builder = TransportResponse.builder()
			.statusCode(statusLine.getStatusCode())
			.reasonPhrase(statusLine.getReasonPhrase())
			.closeCallback(hcResponse);
		for (var h : hcResponse.getAllHeaders())
			builder.header(h.getName(), h.getValue());
		var entity = hcResponse.getEntity();
		if (entity != null) {
			try {
				builder.body(entity.getContent());
			} catch (IOException e) {
				throw new TransportException("Failed to read response body: " + e.getMessage(), e);
			}
		}
		return builder.build();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// TransportBodyEntity — bridges TransportBody to Apache HttpEntity
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Bridges a {@link TransportBody} to Apache HttpClient's {@link AbstractHttpEntity}.
	 */
	private static final class TransportBodyEntity extends AbstractHttpEntity {

		private final TransportBody body;

		TransportBodyEntity(TransportBody body) {
			this.body = body;
			var ct = body.getContentType();
			if (ct != null)
				setContentType(ct);
		}

		@Override /* HttpEntity */
		public boolean isRepeatable() {
			return body.isRepeatable();
		}

		@Override /* HttpEntity */
		public long getContentLength() {
			return body.getContentLength();
		}

		@Override /* HttpEntity */
		public InputStream getContent() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Use writeTo(OutputStream) instead");
		}

		@Override /* HttpEntity */
		public void writeTo(OutputStream out) throws IOException {
			body.writeTo(out);
		}

		@Override /* HttpEntity */
		public boolean isStreaming() {
			return !body.isRepeatable();
		}
	}
}
