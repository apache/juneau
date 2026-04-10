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

/**
 * A builder interface for configuring and creating {@link HttpTransport} instances.
 *
 * <p>
 * Transport modules (Apache HC, Java HTTP Client, OkHttp, etc.) implement this interface to expose their
 * configuration options in a transport-agnostic way.  The built transport is then passed to
 * {@link NgRestClient.Builder#transport(HttpTransport)}.
 *
 * <p>
 * Example usage:
 * <p class='bjava'>
 * 	HttpTransport <jv>transport</jv> = ApacheHc5Transport.builder()
 * 		.connectTimeout(Duration.ofSeconds(5))
 * 		.readTimeout(Duration.ofSeconds(30))
 * 		.build();
 *
 * 	NgRestClient <jv>client</jv> = NgRestClient.create()
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
public interface HttpTransportBuilder {

	/**
	 * Builds and returns a configured {@link HttpTransport} instance.
	 *
	 * @return A new transport. Never <jk>null</jk>.
	 */
	HttpTransport build();
}
