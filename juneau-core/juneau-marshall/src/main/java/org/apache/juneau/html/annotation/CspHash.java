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
 * Hash algorithms defined for
 * <a href="https://www.w3.org/TR/CSP2/#intro">Content Security Policy </a>
 * elements {@code script} and {@code style}.
 *
 * @see <a href="https://www.w3.org/TR/CSP2/#intro">Content Security Policy
 *      Level 2 (W3C Recommendation, 15 December 2016)</a>
 * @see <a href="https://www.w3.org/TR/CSP2/#source-list-valid-hashes">CSP 2
 *      valid hashes</a>
 * @see <a href="https://www.w3.org/TR/CSP3/#grammardef-hash-algorithm">CSP 3
 *      valid hashes</a>
 * @since 9.0.0
 */
public enum CspHash {

	/**
	 * Don't use a hash algorithm.
	 */
	DEFAULT(""),
	
	/**
	 * SHA-256 hash algorithm.
	 * 
	 * @see <a href="https://www.w3.org/TR/CSP2/#source-list-valid-hashes">Valid
	 *      hashes</a>.
	 */
	SHA256("sha256"),

	/**
	 * SHA-384 hash algorithm.
	 * 
	 * @see <a href="https://www.w3.org/TR/CSP2/#source-list-valid-hashes">Valid
	 *      hashes</a>.
	 */

	SHA384("sha384"),
	/**
	 * SHA-512 hash algorithm.
	 * 
	 * @see <a href="https://www.w3.org/TR/CSP2/#source-list-valid-hashes">Valid
	 *      hashes</a>.
	 */
	SHA512("sha512");

	/** Value in HTML. */
	private final String value;

	/** Constructs a new instance. */
	CspHash(String value) {
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
