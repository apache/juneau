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
package org.apache.juneau.ng.http.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;

import org.apache.juneau.ng.http.*;

/**
 * A mutable wrapper around an {@link HttpBody} that allows overriding the content type.
 *
 * <p>
 * Use this class when you need to wrap an existing body and attach (or override) its {@code Content-Type}.
 *
 * <p>
 * Create instances via the static factory methods:
 * <p class='bjava'>
 * 	<jc>// Wrap a byte array body with a specific content type</jc>
 * 	HttpBody <jv>body</jv> = HttpBodyBean.<jsm>of</jsm>(ByteArrayBody.<jsm>of</jsm>(<jv>bytes</jv>), <js>"application/octet-stream"</js>);
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
public final class HttpBodyBean implements HttpBody {

	private final HttpBody delegate;
	private final String contentType;

	private HttpBodyBean(HttpBody delegate, String contentType) {
		this.delegate = assertArgNotNull("delegate", delegate);
		this.contentType = contentType;
	}

	/**
	 * Creates an {@link HttpBodyBean} wrapping the given body with the specified content type.
	 *
	 * @param body The underlying body. Must not be <jk>null</jk>.
	 * @param contentType The content type to report. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpBodyBean of(HttpBody body, String contentType) {
		return new HttpBodyBean(body, contentType);
	}

	/**
	 * Creates an {@link HttpBodyBean} wrapping the given body and retaining its content type.
	 *
	 * @param body The underlying body. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpBodyBean of(HttpBody body) {
		return new HttpBodyBean(body, body.getContentType());
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return delegate.getContentLength();
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return delegate.isRepeatable();
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		delegate.writeTo(out);
	}

	@Override /* Object */
	public String toString() {
		return delegate.toString();
	}
}
