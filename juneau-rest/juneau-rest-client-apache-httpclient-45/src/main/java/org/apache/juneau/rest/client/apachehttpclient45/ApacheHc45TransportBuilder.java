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
package org.apache.juneau.rest.client.apachehttpclient45;


import org.apache.http.impl.client.*;
import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * Fluent builder for {@link ApacheHc45Transport}.
 *
 * <p>
 * Obtain an instance via {@link ApacheHc45Transport#builder()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // Builder methods return Closeables owned by the caller; Eclipse JDT @Owning warning is by design.
})
public final class ApacheHc45TransportBuilder {

	CloseableHttpClient httpClient;

	ApacheHc45TransportBuilder() {}

	/**
	 * Sets the underlying {@link CloseableHttpClient} to use.
	 *
	 * <p>
	 * If not set, the transport creates its own default clients: one for ordinary requests, and one with a separate
	 * connection pool whose connections are all pinned to a policy-allowed address, for SSRF-guard-active
	 * {@code @Remote} requests.
	 *
	 * <p>
	 * A caller-supplied client cannot be given that pin-on-connect pool, so a transport built with one reports
	 * {@link ApacheHc45Transport#supportsUrlPolicy()} as <jk>false</jk>, and {@code RestRequest} refuses to send an
	 * SSRF-guard-active request through it.
	 *
	 * <p>
	 * The transport takes ownership of the client and will close it when {@link ApacheHc45Transport#close()} is called.
	 *
	 * <p>
	 * A legacy {@link AbstractHttpClient} subclass (such as {@code DefaultHttpClient}) is refused: it never sets the request
	 * again just before it is written, so the redirect credential guard cannot strip the credentials HttpClient adds itself
	 * (for example the cookie-store {@code Cookie}) on a cross-origin redirect.  Build the client with {@code HttpClients}.
	 *
	 * @param value The client to use. Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If the client is an {@link AbstractHttpClient}.
	 */
	@SuppressWarnings({
		"deprecation" // AbstractHttpClient is referenced only to refuse the legacy client type; there is no replacement check.
	})
	public ApacheHc45TransportBuilder httpClient(CloseableHttpClient value) {
		if (reqnn("value", value) instanceof AbstractHttpClient)
			throw iaex("A legacy AbstractHttpClient (%s) is not supported: it cannot apply the redirect credential guard.  Build the client with HttpClients instead.", value.getClass().getName());
		httpClient = value;
		return this;
	}

	/**
	 * Builds and returns the {@link ApacheHc45Transport}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public ApacheHc45Transport build() {
		return new ApacheHc45Transport(this);
	}
}
