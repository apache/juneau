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

import static org.apache.juneau.commons.utils.Utils.opt;

import java.util.*;

/**
 * A fluent accessor for a named HTTP response header.
 *
 * <p>
 * Provides convenient typed-access methods for reading header values.
 * Returns {@code null} or empty results when the named header is absent from the response.
 *
 * <p>
 * Obtain instances via {@link NgRestResponse#header(String)}.
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
public final class ResponseHeader {

	private final String name;

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: response is borrowed; caller closes it
	})
	private final NgRestResponse response;

	ResponseHeader(String name, NgRestResponse response) {
		this.name = name;
		this.response = response;
	}

	/**
	 * Returns the header name.
	 *
	 * @return The header name. Never <jk>null</jk>.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns <jk>true</jk> if this header is present in the response.
	 *
	 * @return <jk>true</jk> if present.
	 */
	public boolean isPresent() {
		return response.getFirstHeader(name) != null;
	}

	/**
	 * Returns the first value of this header, or <jk>null</jk> if absent.
	 *
	 * @return The header value, possibly <jk>null</jk>.
	 */
	public String getValue() {
		var h = response.getFirstHeader(name);
		return h != null ? h.value() : null;
	}

	/**
	 * Returns the first value of this header, or the given default if absent.
	 *
	 * @param defaultValue The default value to return when the header is absent.
	 * @return The header value, or {@code defaultValue} if absent.
	 */
	public String orElse(String defaultValue) {
		var value = getValue();
		return value != null ? value : defaultValue;
	}

	/**
	 * Returns the first value of this header as an {@link Optional}.
	 *
	 * @return An optional containing the first header value, or empty if absent.
	 */
	public Optional<String> asOptional() {
		return opt(getValue());
	}

	/**
	 * Returns the first value of this header parsed as an {@code int}, or {@code -1} if absent or not parseable.
	 *
	 * @return The integer value, or {@code -1} if absent or not an integer.
	 */
	public int asInteger() {
		var value = getValue();
		if (value == null)
			return -1;
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Returns the first value of this header parsed as a {@code long}, or {@code -1L} if absent or not parseable.
	 *
	 * @return The long value, or {@code -1L} if absent or not a long.
	 */
	public long asLong() {
		var value = getValue();
		if (value == null)
			return -1L;
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	/**
	 * Returns all values of this header.
	 *
	 * <p>
	 * For repeated headers, this returns each header value in encounter order.
	 *
	 * @return An unmodifiable list of header values. Never <jk>null</jk>, but possibly empty.
	 */
	public List<String> getValues() {
		return response.getHeaders().stream()
			.filter(h -> name.equalsIgnoreCase(h.name()))
			.map(TransportHeader::value)
			.toList();
	}

	/**
	 * Returns the first value as a comma-split list of individual tokens.
	 *
	 * <p>
	 * Useful for headers like {@code Accept} or {@code Allow} that combine multiple values with commas.
	 *
	 * @return A list of trimmed tokens. Never <jk>null</jk>, but possibly empty.
	 */
	public List<String> asCsvList() {
		var value = getValue();
		if (value == null)
			return List.of();
		return Arrays.stream(value.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.toList();
	}

	@Override /* Object */
	public String toString() {
		var value = getValue();
		return value != null ? name + ": " + value : name + ": <absent>";
	}
}
