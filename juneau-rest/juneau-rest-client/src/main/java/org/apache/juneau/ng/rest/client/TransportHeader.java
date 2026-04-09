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

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

/**
 * An HTTP header name/value pair as seen by the transport layer.
 *
 * <p>
 * Unlike {@link org.apache.juneau.ng.http.HttpHeader}, this is a fully-resolved, eagerly-evaluated value;
 * suppliers have already been invoked and {@code null}-valued headers have already been filtered out before
 * a {@link TransportRequest} is constructed.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @param name The header name. Never {@code null}.
 * @param value The header value. Never {@code null}.
 *
 * @since 9.2.1
 */
public record TransportHeader(String name, String value) {

	/**
	 * Compact canonical constructor — validates that neither field is {@code null}.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. Must not be <jk>null</jk>.
	 */
	public TransportHeader {
		assertArgNotNull("name", name);
		assertArgNotNull("value", value);
	}

	/**
	 * Factory method.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static TransportHeader of(String name, String value) {
		return new TransportHeader(name, value);
	}
}
