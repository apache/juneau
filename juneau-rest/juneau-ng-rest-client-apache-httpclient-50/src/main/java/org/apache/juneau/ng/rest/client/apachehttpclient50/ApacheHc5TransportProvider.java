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

import org.apache.juneau.ng.rest.client.*;

/**
 * {@link HttpTransportProvider} implementation that supplies an {@link ApacheHc5Transport}.
 *
 * <p>
 * Registered via {@code META-INF/services/org.apache.juneau.ng.rest.client.HttpTransportProvider} so that
 * {@code NgRestClient} can auto-discover Apache HttpClient 5.x when this module is on the classpath.
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
public final class ApacheHc5TransportProvider implements HttpTransportProvider {

	@Override /* HttpTransportProvider */
	public int getPriority() {
		return 60;
	}

	@Override /* HttpTransportProvider */
	public boolean isAvailable() {
		try {
			Class.forName("org.apache.hc.client5.http.impl.classic.CloseableHttpClient");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@Override /* HttpTransportProvider */
	public HttpTransport create() {
		return ApacheHc5Transport.create();
	}
}
