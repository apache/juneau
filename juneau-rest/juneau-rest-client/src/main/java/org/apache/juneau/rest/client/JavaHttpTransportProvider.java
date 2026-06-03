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

/**
 * {@link HttpTransportProvider} implementation that supplies a {@link JavaHttpTransport}.
 *
 * <p>
 * Registered via {@code META-INF/services/org.apache.juneau.rest.client.HttpTransportProvider} so that
 * {@code RestClient} auto-discovers the JDK HTTP client when no higher-priority transport is present.
 * Since the JDK transport ships in the {@code juneau-rest-client} artifact itself, it is always available
 * unless overridden by a sibling transport module with a lower priority value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class JavaHttpTransportProvider implements HttpTransportProvider {

	@Override /* HttpTransportProvider */
	public int getPriority() {
		return 80;
	}

	@Override /* HttpTransportProvider */
	public boolean isAvailable() {
		try {
			Class.forName("java.net.http.HttpClient");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@Override /* HttpTransportProvider */
	@SuppressWarnings({
		"resource" // Not owned here; lifecycle is managed by the surrounding context
	})
	public HttpTransport create() {
		return JavaHttpTransport.create();
	}
}
