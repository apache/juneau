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
package org.apache.juneau.http.part;

import static org.apache.juneau.commons.utils.Utils.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

/**
 * An {@link org.apache.juneau.http.HttpPart} whose value is a URI.
 *
 * <p>
 * Mirrors the semantics of {@code BasicUriPart} from {@code juneau-rest-common-classic} without
 * the Apache HttpCore dependency.
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S2160" // Equality is fully determined by name + getValue() in HttpPartBean; the typed fields are derived from the wire value and add no state to compare
})
public class HttpUriPart extends HttpPartBean {

	/**
	 * Creates an {@link HttpUriPart} with the given name and URI value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The URI value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpUriPart of(String name, URI value) {
		return new HttpUriPart(name, value);
	}

	/**
	 * Creates an {@link HttpUriPart} by parsing the given wire value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpUriPart ofString(String name, String value) {
		return new HttpUriPart(name, value);
	}

	/**
	 * Creates an {@link HttpUriPart} with a lazy URI supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the URI value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpUriPart ofLazy(String name, Supplier<URI> supplier) {
		return new HttpUriPart(name, supplier);
	}

	private final URI typedValue;
	private final Supplier<URI> typedSupplier;

	/**
	 * Constructor accepting a wire-format string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The wire value. May be <jk>null</jk> or empty.
	 */
	public HttpUriPart(String name, String value) {
		super(name, value);
		this.typedValue = e(value) ? null : URI.create(value);
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a {@link URI} value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The URI value. May be <jk>null</jk>.
	 */
	public HttpUriPart(String name, URI value) {
		super(name, value == null ? null : value.toString());
		this.typedValue = value;
		this.typedSupplier = null;
	}

	/**
	 * Constructor accepting a lazy {@link URI} supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param supplier Supplier for the URI value. Must not be <jk>null</jk>.
	 */
	public HttpUriPart(String name, Supplier<URI> supplier) {
		super(name, () -> {
			var v = supplier.get();
			return v == null ? null : v.toString();
		});
		this.typedValue = null;
		this.typedSupplier = supplier;
	}

	/**
	 * Returns the value as an {@link Optional}{@code <URI>}.
	 *
	 * @return The URI value, wrapped in an {@link Optional}. Never <jk>null</jk>.
	 */
	public Optional<URI> asUri() {
		return opt(toUri());
	}

	/**
	 * Returns the value if present, otherwise returns {@code other}.
	 *
	 * @param other The default value.
	 * @return The value or {@code other}.
	 */
	public URI orElse(URI other) {
		var x = toUri();
		return nn(x) ? x : other;
	}

	/**
	 * Returns the value as a {@link URI}, or {@code null} if not set.
	 *
	 * @return The URI value, possibly {@code null}.
	 */
	public URI toUri() {
		if (typedSupplier != null)
			return typedSupplier.get();
		return typedValue;
	}
}
