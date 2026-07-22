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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

/**
 * Base for single-URI headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160", // equals() on HttpHeaderBean uses name + getValue(); typed state is reflected in getValue()
	"unchecked" // Supplier<?> branches cast to typed suppliers after lazy-mode check
})
public class HttpUriHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_URI = 1;

	private final URI cachedUri;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	/**
	 * Creates an {@link HttpUriHeader} by parsing the given wire-format value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param wireValue Wire value. May be {@code null} or empty.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpUriHeader of(String name, String wireValue) {
		return new HttpUriHeader(name, wireValue);
	}

	/**
	 * Creates an {@link HttpUriHeader} with the given typed value.
	 *
	 * @param name Header name. Must not be {@code null}.
	 * @param typedValue The URI value. May be {@code null}.
	 * @return A new instance. Never {@code null}.
	 */
	public static HttpUriHeader of(String name, URI typedValue) {
		return new HttpUriHeader(name, typedValue);
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Wire value. Can be <jk>null</jk> or empty, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpUriHeader(String name, String value) {
		super(name, value);
		this.cachedUri = ie(value) ? null : URI.create(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value The URI value. Can be <jk>null</jk>, in which case the parsed value is <jk>null</jk>.
	 */
	protected HttpUriHeader(String name, URI value) {
		super(name, s(value));
		this.cachedUri = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	/**
	 * Constructor with lazy value supplier.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param supplier The lazy value supplier. Must not be <jk>null</jk>.
	 * @param lazyMode Either {@link #LAZY_WIRE_STRING} or {@link #LAZY_URI}.
	 */
	protected HttpUriHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? ((Supplier<String>) supplier)::get
			: () -> s(((Supplier<URI>) supplier).get()));
		this.cachedUri = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<URI> asUri() {
		return o(toUri());
	}

	/**
	 * Returns the wire-format value of this header.
	 *
	 * @return The wire value, or <jk>null</jk> if the value is unset.
	 */
	@Override
	public String getValue() {
		return s(toUri());
	}

	/**
	 * Returns the parsed value of this header, or the specified default if unset.
	 *
	 * @param other The default value. Can be <jk>null</jk>.
	 * @return The parsed value, or <c>other</c> if the value is unset. Can be <jk>null</jk> if <c>other</c> is <jk>null</jk>.
	 */
	public URI orElse(URI other) {
		var x = toUri();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the parsed value of this header.
	 *
	 * @return The parsed value, or <jk>null</jk> if the value is unset.
	 */
	public URI toUri() {
		if (lazyMode == LAZY_URI)
			return ((Supplier<URI>) lazySupplier).get();
		if (cachedUri != null)
			return cachedUri;
		var v = super.getValue();
		return ie(v) ? null : URI.create(v);
	}
}
