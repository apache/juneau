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
package org.apache.juneau.rest.server.auth.saml;

/**
 * SPI for the registry of outstanding SAML authentication request IDs.
 *
 * <p>
 * {@link SamlAssertionValidator#rememberAuthnRequest(String)} records the ID of each request this SP sends, and a
 * response whose confirmation carries a matching {@code InResponseTo} consumes it.  Each ID is accepted once.
 * Multi-node deployments should supply an implementation backed by a store shared across every node, since the
 * response may arrive at a different node than the one that sent the request.
 *
 * <p>
 * Implementations must be thread-safe.
 *
 * @see InMemoryAuthnRequestStore
 * @since 10.0.0
 */
public interface AuthnRequestStore {

	/**
	 * Records an outstanding request ID.
	 *
	 * @param id The {@code ID} attribute of the outgoing request.  Never <jk>null</jk> or blank.
	 * @param expiresAtMs The epoch-millisecond instant after which the ID is no longer accepted.
	 * @param nowMs The current epoch-millisecond instant according to the validator's clock.
	 * @throws IllegalStateException If the store cannot accept more outstanding requests.
	 */
	void remember(String id, long expiresAtMs, long nowMs);

	/**
	 * Atomically removes a request ID and reports whether it was outstanding.
	 *
	 * @param id The {@code InResponseTo} value from the response.  Never <jk>null</jk> or blank.
	 * @param nowMs The current epoch-millisecond instant according to the validator's clock.
	 * @return <jk>true</jk> if the ID was registered, unexpired and not yet consumed.
	 */
	boolean consume(String id, long nowMs);
}
