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
package org.apache.juneau.rest.mock;

import java.io.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.protocol.*;

/**
 * An implementation of {@link HttpClientConnectionManager} specifically for use in mocked connections using the {@link MockRestClient} class.
 *
 * <p>
 * This class is instantiated by the {@link MockRestClient.Builder} class.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This implementation is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestMockBasics">juneau-rest-mock Basics</a>
 * </ul>
 */
class MockHttpClientConnectionManager implements HttpClientConnectionManager {

	private ConnectionRequest cr;

	@Override /* Overridden from HttpClientConnectionManager */
	public void closeExpiredConnections() {}

	@Override /* Overridden from HttpClientConnectionManager */
	public void closeIdleConnections(long idletime, TimeUnit tunit) {}

	@Override /* Overridden from HttpClientConnectionManager */
	public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		// All MockHttpClientConnectionManagers are considered equal.
		return o != null && o instanceof MockHttpClientConnectionManager;
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return MockHttpClientConnectionManager.class.hashCode();
	}

	/**
	 * Post-creation initialization method.
	 *
	 * @param mockRestClient Allows the connection manager to reference the mock rest client that created it.
	 */
	public void init(MockRestClient mockRestClient) {
		this.cr = new ConnectionRequest() {
			@Override
			public boolean cancel() {
				return false;
			}
			@Override
			public HttpClientConnection get(long timeout, TimeUnit tunit) throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
				return mockRestClient;
			}
		};
	}

	@Override /* Overridden from HttpClientConnectionManager */
	public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {}

	@Override /* Overridden from HttpClientConnectionManager */
	public ConnectionRequest requestConnection(HttpRoute route, Object state) {
		return cr;
	}

	@Override /* Overridden from HttpClientConnectionManager */
	public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {}

	@Override /* Overridden from HttpClientConnectionManager */
	public void shutdown() {}

	@Override /* Overridden from HttpClientConnectionManager */
	public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {}
}