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

import org.apache.hc.client5.http.impl.classic.*;

/**
 * Fluent builder for {@link ApacheHc5Transport}.
 *
 * <p>
 * Obtain an instance via {@link ApacheHc5Transport#builder()}.
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
public final class ApacheHc5TransportBuilder {

	CloseableHttpClient httpClient;

	ApacheHc5TransportBuilder() {}

	/**
	 * Sets the underlying {@link CloseableHttpClient} to use.
	 *
	 * <p>
	 * If not set, a default client is created via {@link HttpClients#createDefault()}.
	 *
	 * <p>
	 * The transport takes ownership of the client and will close it when {@link ApacheHc5Transport#close()} is called.
	 *
	 * @param value The client to use. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ApacheHc5TransportBuilder httpClient(CloseableHttpClient value) {
		httpClient = value;
		return this;
	}

	/**
	 * Builds and returns the {@link ApacheHc5Transport}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public ApacheHc5Transport build() {
		return new ApacheHc5Transport(this);
	}
}
