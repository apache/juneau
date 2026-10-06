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
package org.apache.juneau.rest.mock.classic;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.hc.core5.http.*;
import org.apache.hc.client5.http.io.*;
import org.apache.hc.client5.http.routing.*;
import org.apache.hc.core5.http.protocol.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestMock">juneau-rest-mock Basics</a>
 * </ul>
 */
class MockHttpClientConnectionManager implements HttpClientConnectionManager {

	private MockRestClient client;

	public void init(MockRestClient client) { this.client = client; }

	@Override
	public LeaseRequest lease(String id, org.apache.hc.client5.http.HttpRoute route, org.apache.hc.core5.util.Timeout timeout, Object state) {
		return new LeaseRequest() {
			@Override
			public boolean cancel() { return false; }

			@Override
			public ConnectionEndpoint get(org.apache.hc.core5.util.Timeout timeout) {
				return new ConnectionEndpoint() {
					@Override
					public ClassicHttpResponse execute(String id, ClassicHttpRequest request,
						org.apache.hc.core5.http.impl.io.HttpRequestExecutor executor, HttpContext context) throws IOException, HttpException {
						return executor.execute(request, client, context);
					}

					@Override
					public ClassicHttpResponse execute(String id, ClassicHttpRequest request,
						ConnectionEndpoint.RequestExecutor executor, HttpContext context) throws IOException, HttpException {
						return executor.execute(request, client, context);
					}

					@Override
					public boolean isConnected() { return true; }

					@Override
					public void setSocketTimeout(org.apache.hc.core5.util.Timeout timeout) { }

					@Override
					public void close() { }

					@Override
					public void close(org.apache.hc.core5.io.CloseMode mode) { }
				};
			}
		};
	}

	@Override
	public void release(ConnectionEndpoint endpoint, Object state, org.apache.hc.core5.util.TimeValue duration) { }

	@Override
	public void connect(ConnectionEndpoint endpoint, org.apache.hc.core5.util.TimeValue timeout, HttpContext context) { }

	@Override
	public void upgrade(ConnectionEndpoint endpoint, HttpContext context) { }

	@Override
	public void close() { }

	@Override
	public void close(org.apache.hc.core5.io.CloseMode mode) { }

	@Override
	public boolean equals(Object o) { return o instanceof MockHttpClientConnectionManager; }

	@Override
	public int hashCode() { return MockHttpClientConnectionManager.class.hashCode(); }
}
