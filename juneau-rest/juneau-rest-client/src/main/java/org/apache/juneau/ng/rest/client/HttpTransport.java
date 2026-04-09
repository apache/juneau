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
package org.apache.juneau.ng.rest.client;

import java.io.*;

/**
 * SPI for pluggable HTTP transports.
 *
 * <p>
 * An {@link HttpTransport} is responsible for executing a fully-resolved {@link TransportRequest} and returning a
 * {@link TransportResponse}.  Implementations include:
 * <ul>
 * 	<li>{@code ApacheHc45Transport} (module {@code juneau-ng-rest-client-apache-httpclient-45})
 * 	<li>{@code ApacheHc5Transport} (module {@code juneau-ng-rest-client-apache-httpclient-50})
 * 	<li>{@code JavaHttpTransport} (module {@code juneau-ng-rest-client-java-httpclient})
 * 	<li>{@code OkHttpTransport} (module {@code juneau-ng-rest-client-okhttp})
 * 	<li>{@code JettyHttpTransport} (module {@code juneau-ng-rest-client-jetty})
 * 	<li>{@code MockHttpTransport} (module {@code juneau-rest-mock})
 * </ul>
 *
 * <p>
 * Implementations are discovered automatically via {@link HttpTransportProvider} using {@link java.util.ServiceLoader},
 * or set explicitly via {@code NgRestClient.Builder#transport(HttpTransport)}.
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
public interface HttpTransport extends Closeable {

	/**
	 * Executes the given request and returns the response.
	 *
	 * <p>
	 * The caller is responsible for closing the returned {@link TransportResponse} after consuming the body.
	 *
	 * @param request The fully-resolved request. Must not be <jk>null</jk>.
	 * @return The response. Never <jk>null</jk>.
	 * @throws TransportException If a network-level error occurs.
	 */
	TransportResponse execute(TransportRequest request) throws TransportException;

	/**
	 * Releases resources held by this transport (connection pools, threads, etc.).
	 *
	 * <p>
	 * The default implementation is a no-op.
	 */
	@Override
	default void close() throws IOException {
		// no-op by default
	}
}
