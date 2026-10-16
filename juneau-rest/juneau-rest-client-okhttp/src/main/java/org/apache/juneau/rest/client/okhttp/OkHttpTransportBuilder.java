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
package org.apache.juneau.rest.client.okhttp;


import java.net.*;

import okhttp3.*;
import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * Fluent builder for {@link OkHttpTransport}.
 *
 * <p>
 * Obtain an instance via {@link OkHttpTransport#builder()}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack.
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
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
public final class OkHttpTransportBuilder {

	OkHttpClient httpClient;

	OkHttpTransportBuilder() {}

	/**
	 * Sets the underlying {@link OkHttpClient} to use.
	 *
	 * <p>
	 * If not set, a default client is created via {@code new OkHttpClient()}.
	 *
	 * <p>
	 * A client with an explicit {@linkplain OkHttpClient#proxy() proxy} makes {@link OkHttpTransport#supportsUrlPolicy()}
	 * report <jk>false</jk> (the proxy resolves the target host itself, bypassing the SSRF guard's address pinning), so
	 * {@code RestRequest} refuses to send an SSRF-guard-active request through it.  A client whose
	 * {@link ProxySelector} routes a guarded request through a proxy is refused at send time.
	 *
	 * <p>
	 * The transport sends through a client derived from this one with {@link OkHttpClient#newBuilder()} (it shares the
	 * connection pool, dispatcher and settings), adding a network interceptor that strips caller-set credential headers
	 * ({@code Authorization}, {@code Cookie} and the rest of {@link org.apache.juneau.rest.client.RedirectSecurity#stripOnCrossOrigin()})
	 * when OkHttp follows a redirect to a different origin.  The client passed in is not modified.
	 *
	 * <p>
	 * Once a hop has been stripped, later hops of the same call stay stripped, including one back to the first origin.  That
	 * state travels on a tag of the request, so a network interceptor or an {@link okhttp3.Authenticator} on the client
	 * that builds a request from scratch (rather than from {@code request.newBuilder()}) must preserve the request's tags,
	 * or the sticky strip is lost.
	 *
	 * <p>
	 * OkHttp recommends sharing a single {@link OkHttpClient} instance across the application.
	 * When sharing, the caller is responsible for lifecycle management; {@link OkHttpTransport#close()}
	 * will still attempt to drain the client's connection pool and executor.
	 *
	 * @param value The client to use. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public OkHttpTransportBuilder httpClient(OkHttpClient value) {
		httpClient = reqnn("value", value);
		return this;
	}

	/**
	 * Builds and returns the {@link OkHttpTransport}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public OkHttpTransport build() {
		return new OkHttpTransport(this);
	}
}
