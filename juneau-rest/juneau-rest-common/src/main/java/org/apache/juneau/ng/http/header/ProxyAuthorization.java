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
package org.apache.juneau.ng.http.header;

import java.util.function.*;

/**
 * Represents an HTTP <c>Proxy-Authorization</c> header.
 *
 * <p>
 * Authorization credentials for connecting to a proxy.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class ProxyAuthorization extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "Proxy-Authorization";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public ProxyAuthorization(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public ProxyAuthorization(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ProxyAuthorization of(String value) {
		return new ProxyAuthorization(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ProxyAuthorization of(Supplier<String> valueSupplier) {
		return new ProxyAuthorization(valueSupplier);
	}
}
