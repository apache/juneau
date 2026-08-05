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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Thread-safe, process-scoped default {@link McpClientRegistrationStore} keying registrations by
 * {@code issuer.toString()} (SEP-2352 "separate registration state per authorization server").
 *
 * <p>
 * Holds credential material in memory only; nothing is persisted across process restarts and nothing is logged
 * ({@link #toString()} discloses only the set of stored issuers, never credential material).
 *
 * @since 10.0.0
 */
public class InMemoryMcpClientRegistrationStore implements McpClientRegistrationStore {

	private final ConcurrentMap<String,McpClientRegistration> byIssuer = new ConcurrentHashMap<>();

	@Override /* McpClientRegistrationStore */
	public Optional<McpClientRegistration> find(URI issuer) {
		assertArgNotNull("issuer", issuer);
		return o(byIssuer.get(issuer.toString()));
	}

	@Override /* McpClientRegistrationStore */
	public void put(URI issuer, McpClientRegistration registration) {
		assertArgNotNull("issuer", issuer);
		assertArgNotNull("registration", registration);
		byIssuer.put(issuer.toString(), registration);
	}

	@Override /* McpClientRegistrationStore */
	public void remove(URI issuer) {
		assertArgNotNull("issuer", issuer);
		byIssuer.remove(issuer.toString());
	}

	/**
	 * Returns the number of stored registrations.
	 *
	 * @return The entry count.
	 */
	public int size() {
		return byIssuer.size();
	}

	@Override /* Object */
	public String toString() {
		return "InMemoryMcpClientRegistrationStore[issuers=" + byIssuer.keySet() + "]";
	}
}
