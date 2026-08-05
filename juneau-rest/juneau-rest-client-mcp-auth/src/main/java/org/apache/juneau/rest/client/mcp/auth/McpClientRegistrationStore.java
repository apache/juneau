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

import java.net.*;
import java.util.*;

/**
 * SEP-2352 optional persistence SPI for Dynamic-Client-Registration / pre-registered credentials, keyed by the
 * authorization server's {@code issuer} identifier.
 *
 * <p>
 * Persistence is <b>SHOULD-level</b> (READY-312f Q8): an MCP client that performs on-demand DCR on every connection
 * with <i>no</i> store is fully compliant.  When a store <i>is</i> used, the SEP-2352 keying MUST bites &mdash;
 * credentials MUST be associated with the specific AS that issued them, and separate registration state MUST be kept per
 * AS ({@link InMemoryMcpClientRegistrationStore} keys by {@code issuer.toString()}).  {@link McpClientRegistrationManager}
 * consults a store (if present) to reuse an issuer-keyed entry and to drive re-registration on AS migration.
 *
 * <h5 class='topic'>Security</h5>
 * <p>
 * A store holds {@code clientSecret} / registration-access-token material (via {@link McpClientRegistration}).  The
 * default {@link InMemoryMcpClientRegistrationStore} is process-scoped; a durable implementation a caller supplies is
 * responsible for at-rest protection and MUST NOT log credential material.
 *
 * @since 10.0.0
 */
public interface McpClientRegistrationStore {

	/**
	 * Returns the registration stored for the given AS issuer, if any.
	 *
	 * @param issuer The authorization server {@code issuer} identifier.  Must not be <jk>null</jk>.
	 * @return The stored registration, or {@link Optional#empty()} if none is stored for that issuer.
	 */
	Optional<McpClientRegistration> find(URI issuer);

	/**
	 * Stores (or replaces) the registration for the given AS issuer.
	 *
	 * @param issuer The authorization server {@code issuer} identifier.  Must not be <jk>null</jk>.
	 * @param registration The registration to store.  Must not be <jk>null</jk>.
	 */
	void put(URI issuer, McpClientRegistration registration);

	/**
	 * Removes any registration stored for the given AS issuer.
	 *
	 * @param issuer The authorization server {@code issuer} identifier.  Must not be <jk>null</jk>.
	 */
	void remove(URI issuer);
}
