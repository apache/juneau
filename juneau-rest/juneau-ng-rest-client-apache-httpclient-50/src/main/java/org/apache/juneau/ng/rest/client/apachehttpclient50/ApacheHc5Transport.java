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
package org.apache.juneau.ng.rest.client.apachehttpclient50;

import java.io.*;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.http.io.support.*;
import org.apache.juneau.ng.rest.client.*;

/**
 * {@link HttpTransport} implementation backed by Apache HttpClient 5.x.
 *
 * <p>
 * This transport is auto-discovered via {@link java.util.ServiceLoader} when
 * {@code org.apache.httpcomponents.client5:httpclient5} is on the classpath.  You can also instantiate it explicitly:
 *
 * <p class='bjava'>
 * 	<jv>transport</jv> = ApacheHc5Transport.<jsm>builder</jsm>()
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
public final class ApacheHc5Transport implements HttpTransport {

	private final CloseableHttpClient httpClient;

	ApacheHc5Transport(ApacheHc5TransportBuilder builder) {
		this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClients.createDefault();
	}

	/**
	 * Returns a new builder for this transport.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static ApacheHc5TransportBuilder builder() {
		return new ApacheHc5TransportBuilder();
	}

	/**
	 * Returns a new instance backed by a default {@link CloseableHttpClient}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ApacheHc5Transport create() {
		return builder().build();
	}

	@Override /* HttpTransport */
	public TransportResponse execute(TransportRequest request) throws TransportException {
		var hcRequest = buildHcRequest(request);
		ClassicHttpResponse hcResponse;
		try {
			hcResponse = httpClient.executeOpen(null, hcRequest, null);
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

	private static ClassicHttpRequest buildHcRequest(TransportRequest request) throws TransportException {
		var builder = ClassicRequestBuilder.create(request.getMethod()).setUri(request.getUri());
		for (var h : request.getHeaders())
			builder.addHeader(h.name(), h.value());
		var body = request.getBody();
		if (body != null)
			builder.setEntity(buildEntity(body));
		try {
			return builder.build();
		} catch (Exception e) {
			throw new TransportException("Failed to build HTTP request: " + e.getMessage(), e);
		}
	}

	private static HttpEntity buildEntity(TransportBody body) {
		var ct = body.getContentType();
		var contentType = ct != null ? ContentType.parse(ct) : ContentType.APPLICATION_OCTET_STREAM;
		return new EntityTemplate(body.getContentLength(), contentType, null, body::writeTo);
	}

	private static TransportResponse buildTransportResponse(ClassicHttpResponse hcResponse) throws TransportException {
		var builder = TransportResponse.builder()
			.statusCode(hcResponse.getCode())
			.reasonPhrase(hcResponse.getReasonPhrase());
		if (hcResponse instanceof Closeable)
			builder.closeCallback((Closeable) hcResponse);
		for (var h : hcResponse.getHeaders())
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
}
