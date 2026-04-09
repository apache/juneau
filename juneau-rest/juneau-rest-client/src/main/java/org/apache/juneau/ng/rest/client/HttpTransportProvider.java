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
 * {@link java.util.ServiceLoader} SPI for auto-discovering {@link HttpTransport} implementations on the classpath.
 *
 * <p>
 * Transport adapter modules (e.g. {@code juneau-ng-rest-client-apache-httpclient-45}) register their provider
 * via {@code META-INF/services/org.apache.juneau.ng.rest.client.HttpTransportProvider}.
 *
 * <p>
 * When no transport is explicitly set, {@code NgRestClient} uses {@link java.util.ServiceLoader} to load all
 * available providers and selects the first one whose {@link #isAvailable()} returns {@code true}.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
public interface HttpTransportProvider {

	/**
	 * Returns the priority of this provider.
	 *
	 * <p>
	 * When multiple providers are available, the one with the <b>lowest</b> priority value is used first.
	 * This allows applications to install a high-priority mock or test transport that takes precedence
	 * over real transports.
	 *
	 * @return The priority. Defaults to {@code 100}.
	 */
	default int getPriority() {
		return 100;
	}

	/**
	 * Returns {@code true} if the required runtime dependencies of this transport are present on the classpath.
	 *
	 * <p>
	 * Providers must check for their dependencies here (e.g. by attempting to load the underlying client class)
	 * so that {@code NgRestClient} can safely skip providers whose jars are absent.
	 *
	 * @return {@code true} if available.
	 */
	boolean isAvailable();

	/**
	 * Creates and returns a new {@link HttpTransport} instance using default configuration.
	 *
	 * @return A new transport. Never <jk>null</jk>.
	 */
	HttpTransport create();
}
