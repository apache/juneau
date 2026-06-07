/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.header;

import java.util.function.*;

/**
 * Base type for HTTP headers whose value is an opaque string (or lazy {@link Supplier} of string).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class HttpStringHeader extends HttpHeaderBean {

	/**
	 * Creates an {@link HttpStringHeader} with the given name and value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param value Wire value. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpStringHeader of(String name, String value) {
		return new HttpStringHeader(name, value);
	}

	/**
	 * Creates an {@link HttpStringHeader} with the given name and lazy value supplier.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param valueSupplier Lazy wire value supplier. Must not be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpStringHeader of(String name, Supplier<String> valueSupplier) {
		return new HttpStringHeader(name, valueSupplier);
	}

	/**
	 * Parses a header line of the form {@code "Name: value"} (or {@code "Name=value"}) into an
	 * {@link HttpStringHeader}.
	 *
	 * <p>
	 * If the input does not contain {@code ':'} or {@code '='}, the entire string is treated as a header name
	 * with an empty value.
	 *
	 * @param pair The header pair. May be {@code null}.
	 * @return A new instance, or {@code null} if {@code pair} is {@code null}.
	 */
	public static HttpStringHeader ofPair(String pair) {
		if (pair == null)
			return null;
		var i = pair.indexOf(':');
		if (i == -1)
			i = pair.indexOf('=');
		if (i == -1)
			return of(pair, "");
		return of(pair.substring(0, i).trim(), pair.substring(i + 1).trim());
	}

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param value Wire value. May be {@code null}.
	 */
	protected HttpStringHeader(String name, String value) {
		super(name, value);
	}

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param valueSupplier Lazy wire value supplier. Must not be {@code null}.
	 */
	protected HttpStringHeader(String name, Supplier<String> valueSupplier) {
		super(name, valueSupplier);
	}
}
