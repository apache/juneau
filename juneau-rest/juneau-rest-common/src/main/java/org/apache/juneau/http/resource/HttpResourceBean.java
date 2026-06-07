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
package org.apache.juneau.http.resource;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;

/**
 * Default immutable implementation of {@link HttpResource}: an {@link HttpBody} bundled with a set of
 * associated headers (such as {@code Content-Type}, {@code Content-Disposition}, or {@code Content-Language}).
 *
 * <p>
 * Useful for static-file delivery, multipart form parts, and other scenarios where headers must travel
 * with the body.
 *
 * <p>
 * Create instances via the static factory methods:
 * <p class='bjava'>
 * 	<jc>// Body with content type header</jc>
 * 	HttpResource <jv>resource</jv> = HttpResourceBean.<jsm>of</jsm>(FileBody.<jsm>of</jsm>(myFile))
 * 		.withHeader(ContentType.<jsm>of</jsm>(<js>"application/pdf"</js>))
 * 		.withHeader(ContentDisposition.<jsm>of</jsm>(<js>"attachment; filename=\"report.pdf\""</js>));
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}). It is not API-frozen: binary- and source-incompatible changes may appear in
 * the <b>next major</b> Juneau release (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class HttpResourceBean implements HttpResource {

	private final HttpBody body;
	private final List<HttpHeader> headers;

	private HttpResourceBean(HttpBody body, List<HttpHeader> headers) {
		this.body = body;
		this.headers = List.copyOf(headers);
	}

	/**
	 * Creates an {@link HttpResourceBean} wrapping the given body with no additional headers.
	 *
	 * @param body The body. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpResourceBean of(HttpBody body) {
		assertArgNotNull("body", body);
		return new HttpResourceBean(body, List.of());
	}

	/**
	 * Creates an {@link HttpResourceBean} wrapping the given body and headers.
	 *
	 * @param body The body. Must not be <jk>null</jk>.
	 * @param headers The associated headers. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpResourceBean of(HttpBody body, List<HttpHeader> headers) {
		assertArgNotNull("body", body);
		assertArgNotNull("headers", headers);
		return new HttpResourceBean(body, headers);
	}

	/**
	 * Returns a new instance with the given header added.
	 *
	 * @param header The header to add. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public HttpResourceBean withHeader(HttpHeader header) {
		assertArgNotNull("header", header);
		var newHeaders = new ArrayList<>(headers);
		newHeaders.add(header);
		return new HttpResourceBean(body, newHeaders);
	}

	/**
	 * Returns a new instance with the given header name and value added.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public HttpResourceBean withHeader(String name, String value) {
		return withHeader(HttpHeaderBean.of(name, value));
	}

	/**
	 * Returns a new instance with the given headers added.
	 *
	 * @param toAdd The headers to add. {@code null} entries are ignored.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public HttpResourceBean withHeaders(HttpHeader...toAdd) {
		if (toAdd == null || toAdd.length == 0)
			return this;
		var newHeaders = new ArrayList<>(headers);
		for (var h : toAdd)
			if (h != null)
				newHeaders.add(h);
		return new HttpResourceBean(body, newHeaders);
	}

	/**
	 * Returns a new instance with the given headers added.
	 *
	 * @param toAdd The headers to add. {@code null} entries are ignored.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public HttpResourceBean withHeaders(List<HttpHeader> toAdd) {
		if (toAdd == null || toAdd.isEmpty())
			return this;
		var newHeaders = new ArrayList<>(headers);
		for (var h : toAdd)
			if (h != null)
				newHeaders.add(h);
		return new HttpResourceBean(body, newHeaders);
	}

	/**
	 * Returns the underlying body.
	 *
	 * @return The body. Never <jk>null</jk>.
	 */
	public HttpBody getBody() {
		return body;
	}

	@Override /* HttpResource */
	public List<HttpHeader> getHeaders() {
		return headers;
	}

	/**
	 * Returns the first header with the given name (case-insensitive), or <jk>null</jk> if absent.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return The first matching header, or <jk>null</jk>.
	 */
	public HttpHeader getFirstHeader(String name) {
		assertArgNotNull("name", name);
		return headers.stream().filter(h -> eqic(h.getName(), name)).findFirst().orElse(null);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// HttpBody delegation
	// ------------------------------------------------------------------------------------------------------------------

	@Override /* HttpBody */
	public String getContentType() {
		var ct = getFirstHeader("Content-Type");
		return ct != null ? ct.getValue() : body.getContentType();
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return body.getContentLength();
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return body.isRepeatable();
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		body.writeTo(out);
	}

	@Override /* Object */
	public String toString() {
		return body.toString();
	}
}
