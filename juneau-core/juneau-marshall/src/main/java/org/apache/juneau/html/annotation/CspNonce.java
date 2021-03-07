// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************

package org.apache.juneau.html.annotation;

/**
 * Nonce algorithms defined for use with
 * <a href="https://www.w3.org/TR/CSP2/#intro">Content Security Policy </a>
 * elements {@code script} and {@code style}.
 *
 * @see <a href="https://www.w3.org/TR/CSP2/#intro">Content Security Policy
 *      Level 2 (W3C Recommendation, 15 December 2016)</a>
 * @see <a href="https://www.w3.org/TR/CSP3/#intro">Content Security Policy
 *      Level 3 (W3C Working Draft, 15 October 2018)</a>
 * @see <a href="https://www.w3.org/TR/CSP3/#security-nonces">Nonce reuse</a>
 * @since 9.0.0
 */
public enum CspNonce {

	/**
	 * Don't use a nonce.
	 */
	DEFAULT(""),

	/**
	 * The secure random algorithm.
	 * 
	 * @see <a href="https://www.w3.org/TR/CSP2/#source-list-valid-hashes">Valid
	 *      hashes</a>.
	 * @see <a href="https://www.w3.org/TR/CSP3/#security-nonces">Nonce reuse</a>
	 */
	// Assumes we are using java.security.SecureRandom as opposed to java.util.Random
	// But leave room for other sources of randomness.
	SECURE_RANDOM("SecureRandom");

	/** Value in HTML. */
	private final String value;

	/** Constructs a new instance. */
	CspNonce(String value) {
		this.value = value;
	}

	/**
	 * Gets the value for HTML.
	 * 
	 * @return the value for HTML.
	 */
	public String value() {
		return value;
	}
}
