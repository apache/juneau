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
import java.net.http.*;

/**
 * Fluent builder for {@link JavaHttpTransport}.
 *
 * <p>
 * Obtain an instance via {@link JavaHttpTransport#builder()}.
 *
 * <h2 class='section'>See Also:</h2><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">Next-Generation REST Client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // Not owned here; lifecycle is managed by the surrounding context
})
public final class JavaHttpTransportBuilder {

	HttpClient httpClient;

	long maxResponseBytes = JavaHttpTransport.DEFAULT_MAX_RESPONSE_BYTES;

	boolean allowHostHeaderOverride;

	JavaHttpTransportBuilder() {}

	/**
	 * Opts in to sending a caller-controlled {@code Host} header, which pinning a policy-covered HTTP request needs.
	 *
	 * <p>
	 * The JDK {@link HttpClient} rejects a {@code Host} header unless the JVM property {@code jdk.httpclient.allowRestrictedHeaders}
	 * lists it.  Without this opt-in a policy-covered HTTP request that needs a {@code Host} override fails closed.
	 *
	 * <p>
	 * <b>This setting changes a JVM-wide property.</b>  Building the transport adds {@code host} to the system property
	 * {@code jdk.httpclient.allowRestrictedHeaders}, which affects every {@link HttpClient} in the JVM.  The JDK reads the
	 * property once, the first time any code builds a request header, so it only takes effect if that has not happened yet; otherwise
	 * start the JVM with {@code -Djdk.httpclient.allowRestrictedHeaders=host}.
	 *
	 * @return This object.
	 */
	public JavaHttpTransportBuilder allowHostHeaderOverride() {
		allowHostHeaderOverride = true;
		return this;
	}

	/**
	 * Sets the maximum number of response body bytes the transport delivers; reading further fails with an {@link IOException}.
	 *
	 * @param value The cap in bytes.  Zero or negative means unlimited.  Defaults to {@link JavaHttpTransport#DEFAULT_MAX_RESPONSE_BYTES}.
	 * @return This object.
	 */
	public JavaHttpTransportBuilder maxResponseBytes(long value) {
		maxResponseBytes = value;
		return this;
	}

	/**
	 * Sets the underlying {@link HttpClient} to use.
	 *
	 * <p>
	 * If not set, a default client is created via {@link HttpClient#newHttpClient()}.
	 *
	 * @param value The client to use. Can be <jk>null</jk> (a default client is created via {@link HttpClient#newHttpClient()}).
	 * @return This object.
	 */
	public JavaHttpTransportBuilder httpClient(HttpClient value) {
		httpClient = value;
		return this;
	}

	/**
	 * Builds and returns the {@link JavaHttpTransport}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public JavaHttpTransport build() {
		return new JavaHttpTransport(this);
	}
}
