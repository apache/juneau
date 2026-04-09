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
package org.apache.juneau.ng.rest.client.jetty;

import org.eclipse.jetty.client.*;

/**
 * Fluent builder for {@link JettyHttpTransport}.
 *
 * <p>
 * Obtain an instance via {@link JettyHttpTransport#builder()}.
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
public final class JettyHttpTransportBuilder {

	HttpClient httpClient;
	long responseTimeoutMs = 30_000;

	JettyHttpTransportBuilder() {}

	/**
	 * Sets the underlying {@link HttpClient} to use.
	 *
	 * <p>
	 * If not set, a default client is created via {@code new HttpClient()}.
	 *
	 * <p>
	 * If the provided client has not been started, {@link JettyHttpTransport} will start it automatically.
	 * When {@link JettyHttpTransport#close()} is called, the client will be stopped.
	 *
	 * @param value The client to use. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public JettyHttpTransportBuilder httpClient(HttpClient value) {
		httpClient = value;
		return this;
	}

	/**
	 * Sets the maximum time to wait for the response headers to arrive after the request is sent.
	 *
	 * <p>
	 * This timeout covers the time from when the request is fully sent until the first response line and headers
	 * are received.  It does not limit how long the response body may take to stream.
	 * Defaults to {@code 30_000} ms (30 seconds).
	 *
	 * @param value The timeout in milliseconds. Use {@code 0} or a negative value to wait indefinitely.
	 * @return This object.
	 */
	public JettyHttpTransportBuilder responseTimeoutMs(long value) {
		responseTimeoutMs = value;
		return this;
	}

	/**
	 * Builds and returns the {@link JettyHttpTransport}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 * @throws Exception If the Jetty {@link HttpClient} cannot be started.
	 */
	public JettyHttpTransport build() throws Exception {
		return new JettyHttpTransport(this);
	}
}
